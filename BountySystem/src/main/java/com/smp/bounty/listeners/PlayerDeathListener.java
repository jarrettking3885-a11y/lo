package com.smp.bounty.listeners;

import com.smp.bounty.BountyPlugin;
import com.smp.bounty.config.ConfigManager;
import com.smp.bounty.data.BountyDataManager;
import com.smp.bounty.data.PlayerBounty;
import com.smp.bounty.economy.EconomyProvider;
import com.smp.bounty.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Core PvP hook. PlayerDeathEvent fires exactly once per death, so as long
 * as we do all bounty/economy mutation synchronously within this single
 * handler call (no async re-entry), there is no risk of double-rewarding
 * a kill.
 */
public class PlayerDeathListener implements Listener {

    private final BountyPlugin plugin;

    public PlayerDeathListener(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        BountyDataManager data = plugin.getDataManager();
        ConfigManager cfg = plugin.getConfigManager();
        EconomyProvider economy = plugin.getEconomyProvider();

        PlayerBounty victimBounty = data.getOrCreate(victim);
        // Always refresh last-known location/activity on death, used by tracking + decay.
        victimBounty.updateLastLocation(victim.getLocation());
        victimBounty.setLastSeenTimestamp(System.currentTimeMillis());

        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            // Not a PvP kill (environment/PvE/self): just break the victim's streak.
            victimBounty.resetKillStreak();
            return;
        }

        PlayerBounty killerBounty = data.getOrCreate(killer);

        // --- 1. Killer is rewarded with the victim's current bounty as coins ---
        long victimCurrentBounty = victimBounty.getBounty();
        double rewardCoins = victimCurrentBounty * cfg.getRewardMultiplier();
        if (rewardCoins > 0) {
            economy.deposit(killer.getUniqueId(), rewardCoins);
        }

        // --- 2. Victim loses bounty (partially retained based on config) ---
        long retained = Math.round(victimCurrentBounty * (cfg.getVictimRetainPercent() / 100.0));
        victimBounty.setBounty(retained);
        victimBounty.resetKillStreak();

        // --- 3. Killer's own bounty value rises (kill_gain + streak bonus) ---
        killerBounty.incrementKillStreak();
        int streak = killerBounty.getKillStreak();
        long gain = cfg.getKillGain();
        if (streak > 1) {
            gain += (long) cfg.getKillStreakBonus() * (streak - 1);
        }
        killerBounty.addBounty(gain);
        killerBounty.setLastKillTimestamp(System.currentTimeMillis());
        killerBounty.setLastSeenTimestamp(System.currentTimeMillis());
        killerBounty.updateLastLocation(killer.getLocation());

        // --- 4. Notify both parties ---
        StringBuilder killerMsg = new StringBuilder()
                .append(cfg.getPrefix())
                .append("&aYou killed &f").append(victim.getName())
                .append("&a! Bounty gained: &e+").append(gain);
        if (rewardCoins > 0) {
            killerMsg.append(" &7| &aCoins: &e+").append(String.format("%.2f", rewardCoins));
        }
        if (streak > 1) {
            killerMsg.append(" &7| &6Kill streak: &e").append(streak);
        }
        killer.sendMessage(ColorUtil.color(killerMsg.toString()));

        victim.sendMessage(ColorUtil.color(cfg.getPrefix() +
                "&cYou were killed by &f" + killer.getName() +
                "&c. Your bounty dropped to &e" + victimBounty.getBounty() + "&c."));
    }
}
