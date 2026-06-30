package com.smp.bounty.tracking;

import com.smp.bounty.BountyPlugin;
import com.smp.bounty.config.ConfigManager;
import com.smp.bounty.data.BountyDataManager;
import com.smp.bounty.data.PlayerBounty;
import com.smp.bounty.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages /track compass sessions. Each active tracker gets exactly one
 * repeating BukkitTask (runTaskTimer, main thread - required since
 * Player#setCompassTarget mutates live player state). There is no
 * per-tick scanning of all players; everything is event/task driven.
 */
public class TrackingManager {

    private final BountyPlugin plugin;
    private final BountyDataManager dataManager;
    private final ConfigManager configManager;

    private final Map<UUID, TrackingSession> sessions = new ConcurrentHashMap<>();

    public TrackingManager(BountyPlugin plugin, BountyDataManager dataManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = configManager;
    }

    public ItemStack createTrackingCompass(String targetName) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("&e&lTracking: &f" + targetName));
            meta.setLore(List.of(
                    ColorUtil.color("&7Points toward your target."),
                    ColorUtil.color("&7Updates automatically.")
            ));
            compass.setItemMeta(meta);
        }
        return compass;
    }

    /** Starts (or restarts) a tracking session for tracker -> target. */
    public void startTracking(Player tracker, UUID targetUuid, String targetName) {
        stopTracking(tracker.getUniqueId());

        tracker.getInventory().addItem(createTrackingCompass(targetName));

        TrackingSession session = new TrackingSession(tracker.getUniqueId(), targetUuid, targetName);
        sessions.put(tracker.getUniqueId(), session);

        long interval = Math.max(1L, configManager.getTrackingUpdateIntervalTicks());
        long startTime = System.currentTimeMillis();
        long durationMillis = configManager.getTrackingDurationSeconds() * 1000L;
        UUID trackerUuid = tracker.getUniqueId();

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Player trackerPlayer = Bukkit.getPlayer(trackerUuid);
            if (trackerPlayer == null || !trackerPlayer.isOnline()) {
                stopTracking(trackerUuid);
                return;
            }

            if (durationMillis > 0 && (System.currentTimeMillis() - startTime) >= durationMillis) {
                trackerPlayer.sendMessage(ColorUtil.color(configManager.getPrefix() +
                        "&7Tracking expired for &f" + targetName + "&7."));
                stopTracking(trackerUuid);
                return;
            }

            updateCompass(trackerPlayer, targetUuid, targetName);
        }, interval, interval);

        session.setTask(task);
    }

    private void updateCompass(Player tracker, UUID targetUuid, String targetName) {
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        Location destination = null;

        if (targetPlayer != null && targetPlayer.isOnline()) {
            if (!configManager.isAllowNetherTracking()
                    && targetPlayer.getWorld().getEnvironment() != tracker.getWorld().getEnvironment()) {
                ColorUtil.actionBar(tracker, "&7Target is in a different dimension.");
                return;
            }
            destination = targetPlayer.getLocation();
        } else {
            PlayerBounty pb = dataManager.get(targetUuid);
            if (pb != null && pb.hasLastLocation()) {
                destination = pb.getLastLocation();
                ColorUtil.actionBar(tracker, "&7" + targetName + " is offline - showing last known location.");
            }
        }

        if (destination == null) {
            ColorUtil.actionBar(tracker, "&cNo location data available for " + targetName + ".");
            return;
        }

        tracker.setCompassTarget(destination);
    }

    public void stopTracking(UUID trackerUuid) {
        TrackingSession session = sessions.remove(trackerUuid);
        if (session != null) {
            session.cancel();
        }
    }

    public boolean isTracking(UUID trackerUuid) {
        return sessions.containsKey(trackerUuid);
    }

    public void shutdown() {
        for (TrackingSession session : sessions.values()) {
            session.cancel();
        }
        sessions.clear();
    }
}
