package fr.iban.guilds.command;

import fr.iban.bukkitcore.commands.annotation.Online;
import fr.iban.bukkitcore.menu.ConfirmMenu;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.api.GuildManager;
import fr.iban.guilds.api.service.GuildAllianceService;
import fr.iban.guilds.api.service.GuildBankService;
import fr.iban.guilds.api.service.GuildService;
import fr.iban.guilds.enums.ChatMode;
import fr.iban.guilds.enums.GuildPermission;
import fr.iban.guilds.lang.Lang;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildPlayer;
import fr.iban.guilds.model.GuildRank;
import fr.iban.guilds.util.ChatUtils;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.List;

@Command({"guild", "g"})
public class GuildCMD {

    private final GuildsPlugin plugin;
    private final GuildManager guildsManager;
    private final GuildBankService guildBankService;
    private final GuildAllianceService guildAllianceService;
    private final GuildService guildService;

    public GuildCMD(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.guildsManager = plugin.getGuildManager();
        this.guildBankService = plugin.getGuildBankService();
        this.guildAllianceService = plugin.getGuildAllianceService();
        this.guildService = plugin.getGuildService();
    }

    @Subcommand("reload")
    @CommandPermission("guilds.reload")
    public void reload(BukkitCommandActor sender) {
        plugin.getLangManager().load();
        plugin.loadConfig();
        sender.reply(Lang.RELOAD.component());
    }

    @Subcommand("help")
    @DefaultFor({"guild", "g"})
    public void help(BukkitCommandActor actor) {
        GuildPlayer guildPlayer = guildsManager.getGuildPlayer(actor.getUniqueId());

        actor.reply(Component.empty());
        actor.reply(Lang.HELP_HEADER.component());
        actor.reply(Component.empty());

        if (guildPlayer == null) {
            actor.reply(Lang.HELP_NO_GUILD.component());
        } else {
            actor.reply(Lang.HELP_MEMBER.component());
        }
    }

    @Subcommand("create")
    public void create(Player sender, @Named("nom de la guilde") String name) {
        guildService.createGuild(sender, name);
    }

    @Subcommand("disband")
    public void disband(Player sender) {
        new ConfirmMenu(sender,
                Lang.GUILD_DISBAND_CONFIRM.toString(),
                result -> {
            if(result){
                guildService.disbandGuild(sender);
            }

            sender.closeInventory();
        }).open();
    }

    @Subcommand("chat")
    public void chat(Player sender, @Optional ChatMode chatMode) {
        if (chatMode == null) {
            guildService.toggleChatMode(sender);
        } else {
            guildService.setChatMode(sender, chatMode);
        }
    }

    @Subcommand("invite")
    public void invite(Player sender, @Online OfflinePlayer player) {
        guildService.invite(sender, player);
    }

    @Subcommand("revoke")
    public void revoke(Player sender, OfflinePlayer player) {
        guildService.revokeInvite(sender, player);
    }

    @Subcommand("promote")
    public void promote(Player sender, OfflinePlayer player) {
        guildService.promote(sender, player);
    }

    @Subcommand("demote")
    public void demote(Player sender, OfflinePlayer player) {
        guildService.demote(sender, player);
    }

    @Subcommand("transfer")
    public void transfer(Player sender, OfflinePlayer player) {
        guildService.transfer(sender, player);
    }

    @Subcommand("join")
    public void join(Player sender, Guild guild) {
        guildService.joinGuild(sender, guild);
    }

    @Subcommand("leave")
    public void leave(Player sender) {
        guildService.quitGuild(sender);
    }

    @Subcommand("bank balance")
    @DefaultFor({"g bank", "guild bank"})
    public void bankBalance(Player sender) {
        Guild guild = guildsManager.getGuildByPlayer(sender);
        Economy economy = plugin.getEconomy();

        if (economy == null) {
            sender.sendMessage(Lang.BANK_ECONOMY_DISABLED.component());
            return;
        }

        if (guild == null) {
            sender.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        sender.sendMessage(Lang.BANK_BALANCE.component("amount", economy.format(guild.getBalance())));
    }

    @Subcommand("bank deposit")
    public void bankDeposit(Player sender, @Named("le montant") double amount) {
        guildBankService.deposit(sender, amount);
    }

    @Subcommand("bank withdraw")
    public void bankWithdraw(Player sender, @Named("le montant") double amount) {
        guildBankService.withdraw(sender, amount);
    }

    @Subcommand("home")
    public void home(Player sender) {
        guildService.teleportHome(sender);
    }

    @Subcommand("delhome")
    public void delhome(Player sender) {
        guildService.delHome(sender);
    }

    @Subcommand("sethome")
    public void sethome(Player sender) {
        guildService.setHome(sender);
    }

    @Subcommand("kick")
    public void kick(Player sender, OfflinePlayer target) {
        guildService.kick(sender, target);
    }

    @Subcommand("lands")
    public void lands(Player sender) {
        sender.performCommand("lands guild");
    }

    @Subcommand("info")
    public void info(BukkitCommandActor sender, @Optional Guild guild) {
        if (guild == null) {
            Player player = sender.getAsPlayer();
            if (player != null) {
                guild = guildsManager.getGuildByPlayer(player);
                if (guild != null) {
                    sendGuildInfo(sender, guild);
                    return;
                }
            }
            sender.reply(Lang.ERROR_SPECIFY_GUILD.component());
        } else {
            sendGuildInfo(sender, guild);
        }
    }

    private void sendGuildInfo(BukkitCommandActor actor, Guild guild) {
        actor.reply(Lang.INFO_HEADER.component("guild", guild.getName()));
        actor.reply(Lang.INFO_CREATED.component("date", guild.getDate()));

        for (GuildRank rank : guild.getRanks()) {
            List<GuildPlayer> rankMembers = guild.getMembersByRank(rank);
            if (!rankMembers.isEmpty()) {
                actor.reply(Lang.INFO_RANK_FORMAT.component(
                        "rank", rank.getName(),
                        "players", String.join(", ", rankMembers.stream().map(GuildPlayer::getName).toList())
                ));
            }
        }

        List<String> alliances = guild.getAlliances().stream().map(Guild::getName).toList();
        if (!alliances.isEmpty()) {
            actor.reply(Lang.INFO_ALLIANCES.component("alliances", String.join(", ", alliances)));
        }
    }

    @Subcommand("list")
    public void list(BukkitCommandActor actor, @Default("1") @Range(min = 1) @Named("page") int page) {
        List<Guild> onlineGuilds = guildsManager.getOnlineGuilds();
        int maxpages = (int) Math.ceil(onlineGuilds.size() / 10D);

        if (page > maxpages) {
            actor.reply(Lang.LIST_INVALID_PAGE.component("maxpage", String.valueOf(maxpages)));
            return;
        }

        actor.reply(Lang.LIST_HEADER.component(
                "page", String.valueOf(page),
                "maxpage", String.valueOf(maxpages)
        ));

        onlineGuilds.stream()
                .skip((page - 1) * 10L)
                .limit(10)
                .forEach(guild -> actor.reply(Lang.LIST_FORMAT.component(
                        "guild", ChatUtils.legacyToMiniMessage(guild.getName()),
                        "online", String.valueOf(guild.getOnlinePlayerAmount()),
                        "total", String.valueOf(guild.getMembers().size())
                )));

        actor.reply(Lang.LIST_FOOTER.component());
    }

    @Subcommand("logs")
    public void logs(BukkitCommandActor sender, @Default("1") @Range(min = 1) @Named("page") int page, @Optional Guild guild) {
        if (guild == null && sender.isPlayer()) {
            Player player = sender.getAsPlayer();
            if (player != null) {
                guild = guildsManager.getGuildByPlayer(player);
            }
        }

        if (guild == null) {
            sender.reply(Lang.ERROR_SPECIFY_GUILD.component());
            return;
        }

        guildsManager.getLogsAsync(guild).thenAccept(logs -> {
            int maxpages = (int) Math.ceil(logs.size() / 10D);

            if (page > maxpages) {
                sender.reply(Lang.LIST_INVALID_PAGE.component("maxpage", String.valueOf(maxpages)));
                return;
            }

            sender.reply(Lang.LOGS_HEADER.component(
                    "page", String.valueOf(page),
                    "maxpage", String.valueOf(maxpages)
            ));

            logs.stream()
                    .skip((page - 1) * 10L)
                    .limit(10)
                    .forEach(log -> sender.reply(Lang.LOGS_ENTRY.component("log", log)));

            sender.reply(Lang.LOGS_FOOTER.component());
        });
    }

    @Subcommand("alliance invite")
    public void allianceInvite(Player sender, Guild guild) {
        guildAllianceService.sendAllianceRequest(sender, guild);
    }

    @Subcommand("alliance accept")
    public void allianceAccept(Player sender, Guild guild) {
        guildAllianceService.acceptAllianceRequest(sender, guild);
    }

    @Subcommand("alliance remove")
    public void allianceRevoke(Player sender, Guild guild) {
        guildAllianceService.revokeAlliance(sender, guild);
    }

    @Subcommand("ranks")
    public void rankList(Player sender) {
        GuildPlayer guildPlayer = guildsManager.getGuildPlayer(sender.getUniqueId());

        if (guildPlayer == null) {
            sender.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if(!guildPlayer.isGranted(GuildPermission.MANAGE_RANKS)) {
            sender.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        plugin.getMenuManager().openGuildRanksMenu(sender, guildPlayer.getGuild());
    }
}