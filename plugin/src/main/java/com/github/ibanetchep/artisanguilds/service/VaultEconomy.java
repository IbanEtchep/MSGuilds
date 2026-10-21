package com.github.ibanetchep.artisanguilds.service;

import net.artisanmc.modules.api.text.TextApi;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Vault's economy, looked up at each call: one registered after ArtisanGuilds started is found
 * without a listener. Only loaded when Vault is installed.
 */
public final class VaultEconomy implements GuildEconomy {

    private final TextApi text;

    public VaultEconomy(TextApi text) {
        this.text = text;
    }

    private @Nullable Economy economy() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        return provider == null ? null : provider.getProvider();
    }

    @Override
    public boolean available() {
        return economy() != null;
    }

    @Override
    public double balance(Player player) {
        Economy economy = economy();
        return economy == null ? 0 : economy.getBalance(player);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        Economy economy = economy();
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public void deposit(Player player, double amount) {
        Economy economy = economy();
        if (economy != null) economy.depositPlayer(player, amount);
    }

    @Override
    public String format(double amount) {
        Economy economy = economy();
        return economy == null ? String.format(Locale.ROOT, "%.2f", amount) : text.fromLegacy(economy.format(amount));
    }

    /** No economy: banks stay readable, deposits and withdrawals are refused. */
    public static final GuildEconomy NONE = new GuildEconomy() {
        @Override public boolean available() { return false; }
        @Override public double balance(Player player) { return 0; }
        @Override public boolean withdraw(Player player, double amount) { return false; }
        @Override public void deposit(Player player, double amount) { }
        @Override public String format(double amount) { return String.format(Locale.ROOT, "%.2f", amount); }
    };
}
