package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import org.bukkit.entity.Player;

/** The guild's bank: players move their money in and out; other plugins pay from it. */
public final class BankService {

    private final GuildRegistry registry;
    private final GuildStore store;
    private final GuildEconomy economy;
    private final GuildMessages messages;

    public BankService(GuildRegistry registry, GuildStore store, GuildEconomy economy, GuildMessages messages) {
        this.registry = registry;
        this.store = store;
        this.economy = economy;
        this.messages = messages;
    }

    public void showBalance(Player player) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (!economy.available()) {
            messages.send(player, Msg.BANK_ECONOMY_DISABLED);
            return;
        }
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        messages.send(player, Msg.BANK_BALANCE, "amount", economy.format(guild.getBalance()));
    }

    public void deposit(Player player, double amount) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        if (!economy.available()) {
            messages.send(player, Msg.ERROR_ECONOMY_DISABLED);
            return;
        }
        if (amount <= 0) {
            messages.send(player, Msg.ERROR_NEED_POSITIVE_AMOUNT);
            return;
        }
        double balance = economy.balance(player);
        if (balance < amount || !economy.withdraw(player, amount)) {
            messages.send(player, Msg.PLAYER_INSUFFICIENT_FUNDS, "amount", economy.format(amount), "balance", economy.format(balance));
            return;
        }
        guild.setBalance(guild.getBalance() + amount);
        messages.send(player, Msg.BANK_DEPOSIT_SUCCESS, "amount", economy.format(amount), "balance", economy.format(guild.getBalance()));
        store.addLog(guild, messages.plain(Msg.LOG_BANK_DEPOSIT, "player", player.getName(),
                "amount", economy.format(amount), "balance", economy.format(guild.getBalance())));
        store.saveGuild(guild);
    }

    public void withdraw(Player player, double amount) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        if (!economy.available()) {
            messages.send(player, Msg.ERROR_ECONOMY_DISABLED);
            return;
        }
        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.BANK_WITHDRAW)) {
            messages.send(player, Msg.ERROR_INSUFFICIENT_RANK);
            return;
        }
        if (amount <= 0) {
            messages.send(player, Msg.ERROR_NEED_POSITIVE_AMOUNT);
            return;
        }
        if (guild.getBalance() < amount) {
            messages.send(player, Msg.BANK_INSUFFICIENT_FUNDS);
            return;
        }
        guild.setBalance(guild.getBalance() - amount);
        economy.deposit(player, amount);
        messages.send(player, Msg.BANK_WITHDRAW_SUCCESS, "amount", economy.format(amount), "balance", economy.format(guild.getBalance()));
        store.saveGuild(guild);
        store.addLog(guild, messages.plain(Msg.LOG_BANK_WITHDRAW, "player", player.getName(),
                "amount", economy.format(amount), "balance", economy.format(guild.getBalance())));
    }

    /** False for an amount not above zero. */
    public boolean deposit(GuildData guild, double amount) {
        if (amount <= 0) return false;
        guild.setBalance(guild.getBalance() + amount);
        store.saveGuild(guild);
        return true;
    }

    /** False when the bank lacks the amount, the amount is not above zero, or there is no economy. */
    public boolean withdraw(GuildData guild, double amount, String reason) {
        if (!economy.available() || amount <= 0 || guild.getBalance() < amount) return false;
        guild.setBalance(guild.getBalance() - amount);
        store.saveGuild(guild);
        store.addLog(guild, messages.plain(Msg.LOG_BANK_WITHDRAW, "player", reason, "reason", reason,
                "amount", economy.format(amount), "balance", economy.format(guild.getBalance())));
        return true;
    }
}
