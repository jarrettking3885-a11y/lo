package com.smp.bounty.data;

import com.smp.bounty.BountyPlugin;
import com.smp.bounty.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Owns the in-memory bounty cache and all persistence (playerdata.yml).
 *
 * Threading model:
 *  - The cache is a ConcurrentHashMap, safe to read/write from any thread.
 *  - Disk I/O (load/save) never touches live Bukkit world/player objects,
 *    so it is safe to run fully asynchronously.
 *  - Only methods that resolve a Location back from a world name
 *    (PlayerBounty#getLastLocation) must be called on the main thread.
 */
public class BountyDataManager {

    private final BountyPlugin plugin;
    private final File dataFile;
    private final Object fileLock = new Object();

    private final Map<UUID, PlayerBounty> cache = new ConcurrentHashMap<>();

    private BukkitTask autoSaveTask;
    private BukkitTask decayTask;

    public BountyDataManager(BountyPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
    }

    /** Synchronous load, intended to run once during onEnable. */
    public void load() {
        synchronized (fileLock) {
            if (!dataFile.exists()) {
                try {
                    dataFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create playerdata.yml", e);
                    return;
                }
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection playersSection = yaml.getConfigurationSection("players");
            cache.clear();
            if (playersSection != null) {
                for (String key : playersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        ConfigurationSection section = playersSection.getConfigurationSection(key);
                        if (section != null) {
                            cache.put(uuid, PlayerBounty.deserialize(uuid, section));
                        }
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Skipping invalid UUID entry in playerdata.yml: " + key);
                    }
                }
            }
            plugin.getLogger().info("Loaded bounty data for " + cache.size() + " players.");
        }
    }

    /** Blocking save - only use during onDisable. */
    public void saveAllSync() {
        writeToDisk();
    }

    /** Safe to call from an async task. */
    public void saveAllAsyncInternal() {
        writeToDisk();
    }

    private void writeToDisk() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection playersSection = yaml.createSection("players");

        for (Map.Entry<UUID, PlayerBounty> entry : cache.entrySet()) {
            ConfigurationSection section = playersSection.createSection(entry.getKey().toString());
            entry.getValue().serialize(section);
        }

        synchronized (fileLock) {
            try {
                yaml.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save playerdata.yml", e);
            }
        }
    }

    public void startAutoSaveTask() {
        long intervalTicks = Math.max(20L, plugin.getConfigManager().getAutoSaveIntervalSeconds() * 20L);
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin, this::saveAllAsyncInternal, intervalTicks, intervalTicks);
    }

    public void startDecayTask() {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.isDecayEnabled() || cfg.getDecayRate() <= 0) {
            return;
        }
        long intervalTicks = Math.max(20L * 60L, cfg.getDecayIntervalMinutes() * 60L * 20L);
        decayTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            long inactivityMillis = cfg.getDecayInactivityMinutes() * 60L * 1000L;
            for (PlayerBounty pb : cache.values()) {
                if (pb.getBounty() <= 0) continue;
                if ((now - pb.getLastSeenTimestamp()) >= inactivityMillis) {
                    pb.addBounty(-cfg.getDecayRate());
                }
            }
        }, intervalTicks, intervalTicks);
    }

    public void shutdownTasks() {
        if (autoSaveTask != null) autoSaveTask.cancel();
        if (decayTask != null) decayTask.cancel();
    }

    public PlayerBounty getOrCreate(UUID uuid, String name) {
        PlayerBounty pb = cache.computeIfAbsent(uuid, id -> new PlayerBounty(id, name));
        if (name != null && !name.equals(pb.getName())) {
            pb.setName(name);
        }
        return pb;
    }

    public PlayerBounty getOrCreate(Player player) {
        return getOrCreate(player.getUniqueId(), player.getName());
    }

    public PlayerBounty get(UUID uuid) {
        return cache.get(uuid);
    }

    public boolean has(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /** Case-insensitive lookup by last-known name, avoids blocking Mojang UUID lookups. */
    public PlayerBounty findByName(String name) {
        for (PlayerBounty pb : cache.values()) {
            if (pb.getName().equalsIgnoreCase(name)) {
                return pb;
            }
        }
        return null;
    }

    /**
     * Resolves a UUID for a player name without ever performing a blocking
     * network call: checks online players first, then falls back to the
     * locally cached bounty records. Returns null if unknown.
     */
    public UUID resolveUuid(String name) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) {
            return online.getUniqueId();
        }
        PlayerBounty pb = findByName(name);
        return pb != null ? pb.getUuid() : null;
    }

    public List<PlayerBounty> getTop(int count) {
        List<PlayerBounty> list = new ArrayList<>(cache.values());
        list.sort(Comparator.comparingLong(PlayerBounty::getBounty).reversed());
        if (list.size() > count) {
            return list.subList(0, count);
        }
        return list;
    }

    public Map<UUID, PlayerBounty> getCache() {
        return cache;
    }
}
