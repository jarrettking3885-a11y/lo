package com.smp.bounty;

import com.smp.bounty.commands.AdminBountyCommand;
import com.smp.bounty.commands.BountyCommand;
import com.smp.bounty.commands.TrackCommand;
import com.smp.bounty.config.ConfigManager;
import com.smp.bounty.data.BountyDataManager;
import com.smp.bounty.economy.EconomyProvider;
import com.smp.bounty.economy.SimpleEconomyProvider;
import com.smp.bounty.economy.VaultEconomyProvider;
import com.smp.bounty.gui.LeaderboardGUIListener;
import com.smp.bounty.listeners.PlayerConnectionListener;
import com.smp.bounty.listeners.PlayerDeathListener;
import com.smp.bounty.tracking.TrackingManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the Bounty Hunting System.
 *
 * Integration notes for the host SMP plugin:
 *  - Economy: implement {@link EconomyProvider} and swap it in via
 *    {@link #setupEconomy()} if you are not using Vault.
 *  - PlayerData: {@link BountyDataManager} is fully self-contained and keyed
 *    by UUID, so it can run alongside (or be merged into) an existing
 *    PlayerData system without conflicts.
 *  - Combat: hook additional logic (assists, combat tagging, etc.) into
 *    {@link PlayerDeathListener}.
 */
public final class BountyPlugin extends JavaPlugin {

    private static BountyPlugin instance;

    private ConfigManager configManager;
    private BountyDataManager dataManager;
    private EconomyProvider economyProvider;
    private TrackingManager trackingManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        this.dataManager = new BountyDataManager(this);
        this.dataManager.load();

        this.economyProvider = setupEconomy();

        this.trackingManager = new TrackingManager(this, dataManager, configManager);

        registerListeners();
        registerCommands();

        this.dataManager.startAutoSaveTask();
        this.dataManager.startDecayTask();

        getLogger().info("BountySystem enabled. Economy provider: " + economyProvider.getName());
    }

    @Override
    public void onDisable() {
        if (trackingManager != null) {
            trackingManager.shutdown();
        }
        if (dataManager != null) {
            dataManager.shutdownTasks();
            dataManager.saveAllSync();
        }
        getLogger().info("BountySystem disabled, data saved.");
    }

    private EconomyProvider setupEconomy() {
        Plugin vault = getServer().getPluginManager().getPlugin("Vault");
        if (vault != null) {
            VaultEconomyProvider provider = VaultEconomyProvider.tryHook();
            if (provider != null) {
                return provider;
            }
        }
        // Fallback: replace SimpleEconomyProvider with a bridge to your
        // SMP's real economy/PlayerData system if Vault is not used.
        return new SimpleEconomyProvider();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new LeaderboardGUIListener(this), this);
    }

    private void registerCommands() {
        BountyCommand bountyCommand = new BountyCommand(this);
        getCommand("bounty").setExecutor(bountyCommand);
        getCommand("bounty").setTabCompleter(bountyCommand);

        AdminBountyCommand adminCommand = new AdminBountyCommand(this);
        getCommand("adminbounty").setExecutor(adminCommand);
        getCommand("adminbounty").setTabCompleter(adminCommand);

        TrackCommand trackCommand = new TrackCommand(this);
        getCommand("track").setExecutor(trackCommand);
        getCommand("track").setTabCompleter(trackCommand);
    }

    public static BountyPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public BountyDataManager getDataManager() {
        return dataManager;
    }

    public EconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    public TrackingManager getTrackingManager() {
        return trackingManager;
    }
}
