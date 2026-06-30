package com.smp.bounty.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/**
 * Bridges the Bounty System into Vault's Economy API, which most SMP
 * plugins already use. Requires the VaultAPI dependency at compile time
 * (see pom.xml) - this is just the API jar, not the Vault plugin itself,
 * so it compiles fine even on servers that don't run Vault.
 *
 * Note: Bukkit#getOfflinePlayer(UUID) is a safe, non-blocking lookup
 * (no network call), unlike the String-name overload.
 */
public class VaultEconomyProvider implements EconomyProvider {

    private final Economy economy;

    private VaultEconomyProvider(Economy economy) {
        this.economy = economy;
    }

    public static VaultEconomyProvider tryHook() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return null;
        Economy econ = rsp.getProvider();
        if (econ == null) return null;
        return new VaultEconomyProvider(econ);
    }

    @Override
    public String getName() {
        return "Vault(" + economy.getName() + ")";
    }

    @Override
    public double getBalance(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return economy.getBalance(player);
    }

    @Override
    public void deposit(UUID uuid, double amount) {
        if (amount <= 0) return;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        economy.depositPlayer(player, amount);
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return true;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (economy.getBalance(player) < amount) return false;
        economy.withdrawPlayer(player, amount);
        return true;
    }
}
