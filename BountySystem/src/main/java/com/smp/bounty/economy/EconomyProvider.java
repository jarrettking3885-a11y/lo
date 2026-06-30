package com.smp.bounty.economy;

import java.util.UUID;

/**
 * Abstraction layer so the Bounty System can plug into whatever Economy
 * implementation the host SMP plugin uses. Two implementations are
 * provided out of the box:
 *
 *  - {@link VaultEconomyProvider}: used automatically if Vault + a
 *    registered economy plugin are detected.
 *  - {@link SimpleEconomyProvider}: in-memory fallback used only when
 *    Vault is unavailable.
 *
 * To integrate with a custom/existing Economy system, implement this
 * interface (bridging into your own PlayerData/Economy classes) and
 * return it from {@code BountyPlugin#setupEconomy()}.
 */
public interface EconomyProvider {

    String getName();

    double getBalance(UUID uuid);

    void deposit(UUID uuid, double amount);

    boolean withdraw(UUID uuid, double amount);
}
