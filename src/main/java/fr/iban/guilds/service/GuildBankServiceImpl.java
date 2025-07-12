package fr.iban.guilds.service;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.api.GuildManager;
import fr.iban.guilds.api.service.GuildBankService;
import fr.iban.guilds.enums.GuildPermission;
import fr.iban.guilds.lang.Lang;
import fr.iban.guilds.model.Guild;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class GuildBankServiceImpl implements GuildBankService {

    private final GuildsPlugin plugin;
    private final GuildManager guildManager;

    public GuildBankServiceImpl(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.guildManager = plugin.getGuildManager();
    }

    @Override
    public void showBalance(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player);
        Economy economy = plugin.getEconomy();

        if (economy == null) {
            player.sendMessage(Lang.BANK_ECONOMY_DISABLED.component());
            return;
        }

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        player.sendMessage(Lang.BANK_BALANCE.component("amount", economyFormat(guild.getBalance())));
    }

    @Override
    public void deposit(Player player, double amount) {
        Guild guild = guildManager.getGuildByPlayer(player);
        Economy economy = plugin.getEconomy();

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (economy == null) {
            player.sendMessage(Lang.ERROR_ECONOMY_DISABLED.component());
            return;
        }

        if (amount <= 0) {
            player.sendMessage(Lang.ERROR_NEED_POSITIVE_AMOUNT.component());
            return;
        }

        double playerBalance = economy.getBalance(player);
        if (playerBalance < amount) {
            player.sendMessage(Lang.PLAYER_INSUFFICIENT_FUNDS.component(
                    "amount", economyFormat(amount),
                    "balance", economyFormat(playerBalance)
            ));
            return;
        }

        economy.withdrawPlayer(player, amount);
        guild.setBalance(guild.getBalance() + amount);
        player.sendMessage(Lang.BANK_DEPOSIT_SUCCESS.component(
                "amount", economyFormat(amount),
                "balance", economyFormat(guild.getBalance())
        ));
        guildManager.addLog(guild, Lang.LOG_BANK_DEPOSIT.toString(
                "player", player.getName(),
                "amount", economyFormat(amount),
                "balance", economyFormat(guild.getBalance())
        ));
        guildManager.saveGuild(guild);
    }

    @Override
    public void withdraw(Player player, double amount) {
        Guild guild = guildManager.getGuildByPlayer(player);
        Economy economy = plugin.getEconomy();

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (economy == null) {
            player.sendMessage(Lang.ERROR_ECONOMY_DISABLED.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.BANK_WITHDRAW)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if (amount <= 0) {
            player.sendMessage(Lang.ERROR_NEED_POSITIVE_AMOUNT.component());
            return;
        }

        double currentBalance = guild.getBalance();
        if (currentBalance < amount) {
            player.sendMessage(Lang.BANK_INSUFFICIENT_FUNDS.component());
            return;
        }

        economy.depositPlayer(player, amount);
        guild.setBalance(currentBalance - amount);
        player.sendMessage(Lang.BANK_WITHDRAW_SUCCESS.component(
                "amount", economyFormat(amount),
                "balance", economyFormat(guild.getBalance())
        ));
        guildManager.saveGuild(guild);
        guildManager.addLog(guild, Lang.LOG_BANK_WITHDRAW.toString(
                "player", player.getName(),
                "amount", economyFormat(amount),
                "balance", economyFormat(guild.getBalance())
        ));
    }

    @Override
    public boolean deposit(Guild guild, double amount) {
        double currentBalance = guild.getBalance();

        if (amount <= 0) {
            return false;
        }

        guild.setBalance(currentBalance + amount);
        guildManager.saveGuild(guild);
        return true;
    }

    @Override
    public boolean withdraw(Guild guild, double amount) {
        double currentBalance = guild.getBalance();

        if (currentBalance < amount || amount <= 0) {
            return false;
        }

        guild.setBalance(currentBalance - amount);
        guildManager.saveGuild(guild);
        return true;
    }

    @Override
    public boolean withdraw(Guild guild, double amount, String reason) {
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            return false;
        }

        boolean result = withdraw(guild, amount);
        if (result) {
            guildManager.addLog(guild, Lang.LOG_BANK_WITHDRAW.toString(
                    "reason", reason,
                    "amount", economyFormat(amount),
                    "balance", economyFormat(guild.getBalance())
            ));
        }
        return result;
    }
    
    private String economyFormat(double amount) {
        String legacyMessage = plugin.getEconomy().format(amount);
        Component message = LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
        return MiniMessage.miniMessage().serialize(message);
    }
}
