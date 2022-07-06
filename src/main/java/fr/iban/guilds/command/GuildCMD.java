package fr.iban.guilds.command;

import fr.iban.bukkitcore.commands.annotation.Online;
import fr.iban.guilds.Guild;
import fr.iban.guilds.GuildPlayer;
import fr.iban.guilds.GuildsManager;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.enums.Rank;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.command.CommandActor;

import java.util.List;

@Command({"guild", "g"})
public class GuildCMD {

    private final GuildsPlugin plugin;
    private final GuildsManager guildsManager;

    public GuildCMD(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.guildsManager = plugin.getGuildsManager();
    }

    @Subcommand("help")
    public void help(CommandActor actor) {
        GuildPlayer guildPlayer = guildsManager.getGuildPlayer(actor.getUniqueId());
        actor.reply("");
        actor.reply("§7§lVoici l'aide pour utiliser les guildes :");
        actor.reply("");
        if (guildPlayer == null) {
            actor.reply("§6/guild create §f→ créer une guilde.");
            actor.reply("§6/guild join <nom de la guilde> §f→ accepter l'invitation d'une guilde.");
            actor.reply("§7Davantage de commande seront affichées quand vous serez membre d'une guilde et selon votre rang dans la guilde.");
            return;
        }
        if (guildPlayer.isGranted(Rank.MEMBER)) {
            actor.reply("§f§lCommandes §a§lmembre §f§f:");
            actor.reply("§6/guild info §f→ informations sur la guilde.");
            actor.reply("§6/guild chat §f→ activer/désactiver le chat de guilde.");
            actor.reply("§6/guild logs §f→ voir le journal de sa guilde.");
            actor.reply("§6/guild home §f→ rejoindre la résidence de la guilde.");
            actor.reply("§6/guild bank §f→ voir le solde de la guilde");
            actor.reply("§6/guild bank deposit <montant> §f→ déposer de l'argent dans la banque.");
            actor.reply("§6/guild leave §f→ quitter sa guilde.");
        }
        if (guildPlayer.isGranted(Rank.MODERATOR)) {
            actor.reply("§f§lCommandes §b§lmodérateur §f§l:");
            actor.reply("§6/guild kick §f→ exclure un membre de grade inférieur.");
        }
        if (guildPlayer.isGranted(Rank.ADMIN)) {
            actor.reply("§f§lCommandes §c§ladministrateur §f§l:");
            actor.reply("§6/guild lands §f→ gérer les territoires de la guilde.");
            actor.reply("§6/guild delhome §f→ supprimer la résidence de la guilde.");
            actor.reply("§6/guild promote §f→ promouvoir un membre.");
            actor.reply("§6/guild demote §f→ rétrograder un membre.");
            actor.reply("§6/guild sethome §f→ définir la résidence de la guilde.");
            actor.reply("§6/guild delhome §f→ supprimer la résidence de la guilde.");
            actor.reply("§6/guild bank withdraw <montant> §f→ retirer de l'argent de la banque.");
        }
        if (guildPlayer.isGranted(Rank.OWNER)) {
            actor.reply("§f§lCommandes §4§lfondateur §f§l:");
            actor.reply("§6/guild disband §f→ dissoudre sa guilde.");
        }
        actor.reply("");
    }

    @Subcommand("reload")
    @CommandPermission("guilds.reload")
    public void reload(BukkitCommandActor sender) {
        guildsManager.load();
        sender.reply("§cGuilds reloaded.");
    }

    @Subcommand("create")
    public void create(Player sender, @Named("nom de la guilde") String name) {
        guildsManager.createGuild(sender, name);
    }

    @Subcommand("disband")
    public void disband(Player sender) {
        guildsManager.disbandGuild(sender);
    }

    @Subcommand("chat")
    public void chat(Player sender) {
        GuildPlayer guildPlayer = guildsManager.getGuildPlayer(sender.getUniqueId());
        guildsManager.toggleChatMode(sender);
    }

    @Subcommand("invite")
    public void invite(Player sender, @Online OfflinePlayer player) {
        guildsManager.invite(sender, player);
    }

    @Subcommand("revoke")
    public void revoke(Player sender, OfflinePlayer player) {
        guildsManager.revokeInvite(sender, player);
    }

    @Subcommand("promote")
    public void promote(Player sender, OfflinePlayer player) {
        guildsManager.demote(sender, player);
    }

    @Subcommand("demote")
    public void demote(Player sender, OfflinePlayer player) {
        guildsManager.promote(sender, player);
    }

    @Subcommand("transfer")
    public void transfer(Player sender, OfflinePlayer player) {
        guildsManager.transfer(sender, player);
    }

    @Subcommand("join")
    public void join(Player sender, Guild guild) {
        guildsManager.joinGuild(sender, guild);
    }

    @Subcommand("quit")
    public void leave(Player sender) {
        guildsManager.quitGuild(sender);
    }

    @Subcommand({"bank", "bank balance"})
    public void bankBalance(Player sender) {
        Guild guild = guildsManager.getGuildByPlayer(sender);
        Economy economy = plugin.getEconomy();

        if(economy == null) {
            sender.sendMessage("§cLa banque n'est pas accessible.");
            return;
        }

        if (guild == null) {
            sender.sendMessage("§cVous n'avez pas de guilde !");
            return;
        }

        sender.sendMessage("§aVotre guilde possède : §f§l" + economy.format(guild.getBalance()));
    }

    @Subcommand("bank deposit")
    public void bankDeposit(Player sender, double amount) {
        guildsManager.guildDeposit(sender, amount);
    }

    @Subcommand("bank withdraw")
    public void bankWithdraw(Player sender, double amount) {
        guildsManager.guildWithdraw(sender, amount);
    }

    @Subcommand("home")
    public void home(Player sender) {
        guildsManager.teleportHome(sender);
    }

    @Subcommand("delhome")
    public void delhome(Player sender) {
        guildsManager.delHome(sender);
    }

    @Subcommand("sethome")
    public void sethome(Player sender) {
        guildsManager.setHome(sender);
    }

    @Subcommand("kick")
    public void kick(Player sender, OfflinePlayer target) {
        guildsManager.kick(sender, target);
    }

    @Subcommand("info")
    public void info(BukkitCommandActor sender, @Optional Guild guild) {
        if (guild == null) {
            Player player = sender.getAsPlayer();
            if (player != null) {
                guild = guildsManager.getGuildByPlayer(player);
                if (guild != null) {
                    sender.reply(getInfo(guild));
                    return;
                }
            }
            sender.reply("§cVeuillez spécifier le nom d'une guilde.");
        } else {
            sender.reply(getInfo(guild));
        }
    }

    private String getInfo(Guild guild) {
        String info = "§7§lInformations sur §6§l" + guild.getName() + "\n";
        info += "§8Créé le " + guild.getDate() + "\n";
        List<String> admins = guild.getMembers().values().stream()
                .filter(gp -> gp.getRank() == Rank.ADMIN)
                .map(GuildPlayer::getName).toList();
        List<String> mods = guild.getMembers().values().stream()
                .filter(gp -> gp.getRank() == Rank.MODERATOR)
                .map(GuildPlayer::getName).toList();
        List<String> members = guild.getMembers().values().stream()
                .filter(gp -> gp.getRank() == Rank.MEMBER)
                .map(GuildPlayer::getName).toList();
        info += "§4§lFondateur : §f" + guild.getOwner().getName();
        if (!admins.isEmpty()) {
            info += "§c§lAdministrateurs : §f" + String.join(", ", admins) + "\n";
        }
        if (!mods.isEmpty()) {
            info += "§b§lModérateurs : §f" + String.join(", ", mods) + "\n";
        }
        if (!members.isEmpty()) {
            info += "§b§lMembres : §f" + String.join(", ", members) + "\n";
        }
        return info;
    }
}