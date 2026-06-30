package com.smp.bounty.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

/**
 * Holds all persistent state for a single player's bounty.
 * Pure data object - no Bukkit scheduling logic lives here.
 */
public class PlayerBounty {

    private final UUID uuid;
    private String name;
    private long bounty;
    private int killStreak;
    private long lastKillTimestamp;
    private long lastSeenTimestamp;

    private String lastWorldName;
    private double lastX;
    private double lastY;
    private double lastZ;

    public PlayerBounty(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name == null ? "Unknown" : name;
        this.bounty = 0L;
        this.killStreak = 0;
        this.lastKillTimestamp = 0L;
        this.lastSeenTimestamp = System.currentTimeMillis();
    }

    public static PlayerBounty deserialize(UUID uuid, ConfigurationSection section) {
        PlayerBounty pb = new PlayerBounty(uuid, section.getString("name", "Unknown"));
        pb.bounty = section.getLong("bounty", 0L);
        pb.killStreak = section.getInt("kill_streak", 0);
        pb.lastKillTimestamp = section.getLong("last_kill_timestamp", 0L);
        pb.lastSeenTimestamp = section.getLong("last_seen_timestamp", System.currentTimeMillis());
        pb.lastWorldName = section.getString("last_location.world", null);
        pb.lastX = section.getDouble("last_location.x", 0.0);
        pb.lastY = section.getDouble("last_location.y", 0.0);
        pb.lastZ = section.getDouble("last_location.z", 0.0);
        return pb;
    }

    public void serialize(ConfigurationSection section) {
        section.set("name", name);
        section.set("bounty", bounty);
        section.set("kill_streak", killStreak);
        section.set("last_kill_timestamp", lastKillTimestamp);
        section.set("last_seen_timestamp", lastSeenTimestamp);
        if (lastWorldName != null) {
            section.set("last_location.world", lastWorldName);
            section.set("last_location.x", lastX);
            section.set("last_location.y", lastY);
            section.set("last_location.z", lastZ);
        }
    }

    public UUID getUuid() { return uuid; }

    public String getName() { return name; }
    public void setName(String name) { if (name != null) this.name = name; }

    public long getBounty() { return bounty; }

    public void setBounty(long bounty) {
        this.bounty = Math.max(0L, bounty);
    }

    public void addBounty(long amount) {
        setBounty(this.bounty + amount);
    }

    public int getKillStreak() { return killStreak; }
    public void setKillStreak(int killStreak) { this.killStreak = killStreak; }
    public void incrementKillStreak() { this.killStreak++; }
    public void resetKillStreak() { this.killStreak = 0; }

    public long getLastKillTimestamp() { return lastKillTimestamp; }
    public void setLastKillTimestamp(long ts) { this.lastKillTimestamp = ts; }

    public long getLastSeenTimestamp() { return lastSeenTimestamp; }
    public void setLastSeenTimestamp(long ts) { this.lastSeenTimestamp = ts; }

    /** Updates the cached last-known location, used as a tracking fallback when offline. */
    public void updateLastLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        this.lastWorldName = loc.getWorld().getName();
        this.lastX = loc.getX();
        this.lastY = loc.getY();
        this.lastZ = loc.getZ();
    }

    public boolean hasLastLocation() {
        return lastWorldName != null;
    }

    /** Resolves the cached location. Only call from the main thread (touches World/Bukkit). */
    public Location getLastLocation() {
        if (lastWorldName == null) return null;
        World world = Bukkit.getWorld(lastWorldName);
        if (world == null) return null;
        return new Location(world, lastX, lastY, lastZ);
    }
}
