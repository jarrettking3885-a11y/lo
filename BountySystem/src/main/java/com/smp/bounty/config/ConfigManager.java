package com.smp.bounty.config;

import com.smp.bounty.BountyPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Wraps config.yml values so the rest of the plugin never touches
 * FileConfiguration directly. Call {@link #load()} again to support
 * a future /bountyreload command if desired.
 */
public class ConfigManager {

    private final BountyPlugin plugin;

    private int killGain;
    private int killStreakBonus;
    private double rewardMultiplier;
    private int decayRate;
    private int decayIntervalMinutes;
    private int decayInactivityMinutes;
    private int victimRetainPercent;
    private boolean decayEnabled;

    private long trackingUpdateIntervalTicks;
    private boolean allowNetherTracking;
    private int trackingDurationSeconds; // 0 = unlimited

    private int autoSaveIntervalSeconds;

    private boolean leaderboardGuiEnabled;

    private String prefix;

    public ConfigManager(BountyPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        this.killGain = cfg.getInt("bounty.kill_gain", 10);
        this.killStreakBonus = cfg.getInt("bounty.kill_streak_bonus", 5);
        this.rewardMultiplier = cfg.getDouble("bounty.reward_multiplier", 1.0);
        this.decayRate = cfg.getInt("bounty.decay_rate", 1);
        this.decayEnabled = cfg.getBoolean("bounty.decay_enabled", true);
        this.decayIntervalMinutes = cfg.getInt("bounty.decay_interval_minutes", 30);
        this.decayInactivityMinutes = cfg.getInt("bounty.decay_inactivity_minutes", 60);
        this.victimRetainPercent = cfg.getInt("bounty.victim_retain_percent", 0);

        this.trackingUpdateIntervalTicks = cfg.getLong("tracking.update_interval_ticks", 40L);
        this.allowNetherTracking = cfg.getBoolean("tracking.allow_nether_tracking", true);
        this.trackingDurationSeconds = cfg.getInt("tracking.duration_seconds", 0);

        this.autoSaveIntervalSeconds = cfg.getInt("storage.autosave_interval_seconds", 90);

        this.leaderboardGuiEnabled = cfg.getBoolean("leaderboard.gui_enabled", true);

        this.prefix = cfg.getString("messages.prefix", "&6&lBounty &8\u00bb &r");
    }

    public int getKillGain() { return killGain; }
    public int getKillStreakBonus() { return killStreakBonus; }
    public double getRewardMultiplier() { return rewardMultiplier; }
    public int getDecayRate() { return decayRate; }
    public boolean isDecayEnabled() { return decayEnabled; }
    public int getDecayIntervalMinutes() { return decayIntervalMinutes; }
    public int getDecayInactivityMinutes() { return decayInactivityMinutes; }
    public int getVictimRetainPercent() { return victimRetainPercent; }

    public long getTrackingUpdateIntervalTicks() { return trackingUpdateIntervalTicks; }
    public boolean isAllowNetherTracking() { return allowNetherTracking; }
    public int getTrackingDurationSeconds() { return trackingDurationSeconds; }

    public int getAutoSaveIntervalSeconds() { return autoSaveIntervalSeconds; }

    public boolean isLeaderboardGuiEnabled() { return leaderboardGuiEnabled; }

    public String getPrefix() { return prefix; }
}
