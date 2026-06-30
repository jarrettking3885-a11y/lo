package com.smp.bounty.listeners;

import com.smp.bounty.BountyPlugin;
import com.smp.bounty.data.PlayerBounty;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final BountyPlugin plugin;

    public PlayerConnectionListener(BountyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerBounty pb = plugin.getDataManager().getOrCreate(player);
        pb.setLastSeenTimestamp(System.currentTimeMillis());
        pb.setName(player.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerBounty pb = plugin.getDataManager().get(player.getUniqueId());
        if (pb != null) {
            pb.setLastSeenTimestamp(System.currentTimeMillis());
            pb.updateLastLocation(player.getLocation());
        }

        // Stop any tracking session this player initiated (target Player ref would go stale).
        plugin.getTrackingManager().stopTracking(player.getUniqueId());

        // Persist data immediately but off the main thread - required per spec ("on player quit").
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getDataManager().saveAllAsyncInternal());
    }
}
