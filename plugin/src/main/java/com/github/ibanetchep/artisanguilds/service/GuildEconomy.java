package com.github.ibanetchep.artisanguilds.service;

import org.bukkit.entity.Player;

/** The players' money, through Vault when an economy is installed. */
public interface GuildEconomy {

    boolean available();

    double balance(Player player);

    boolean withdraw(Player player, double amount);

    void deposit(Player player, double amount);

    /** As MiniMessage. */
    String format(double amount);
}
