package fr.iban.guilds.api.service;

import fr.iban.guilds.model.Guild;
import org.bukkit.entity.Player;

public interface GuildBankService {
    void showBalance(Player player);
    void deposit(Player player, double amount);
    void withdraw(Player player, double amount);
    boolean deposit(Guild guild, double amount);
    boolean withdraw(Guild guild, double amount);
    boolean withdraw(Guild guild, double amount, String reason);
}
