package com.smp.bounty.economy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback economy provider used only when no Vault economy plugin is
 * present. In production on a real SMP, this should be replaced by a
 * bridge to the existing Economy/PlayerData system - see
 * {@link EconomyProvider} for the integration point.
 */
public class SimpleEconomyProvider implements EconomyProvider {

    private final ConcurrentHashMap<UUID, Double> balances = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "InternalFallbackEconomy";
    }

    @Override
    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0);
    }

    @Override
    public void deposit(UUID uuid, double amount) {
        if (amount <= 0) return;
        balances.merge(uuid, amount, Double::sum);
    }

    @Override
    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return true;
        double bal = getBalance(uuid);
        if (bal < amount) return false;
        balances.put(uuid, bal - amount);
        return true;
    }
}
