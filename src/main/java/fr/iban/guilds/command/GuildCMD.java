package fr.iban.guilds.command;

import fr.iban.guilds.GuildPlayer;
import fr.iban.guilds.GuildsManager;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.enums.Rank;
import fr.iban.guilds.exception.AlreadyGuildMemberException;
import fr.iban.guilds.exception.GuildAlreadyExistsException;
import fr.iban.guilds.exception.InsufficientPermissionException;
import fr.iban.guilds.exception.NotGuildMemberException;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.command.CommandActor;

@Command({"guild", "g"})
public class GuildCMD {

    private final GuildsPlugin plugin;
    private final GuildsManager guildsManager;

    public GuildCMD(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.guildsManager = plugin.getGuildsManager();
    }

    @Subcommand("help")
    public void help(CommandActor actor, @Default("1") int page) {
        GuildPlayer guildPlayer = guildsManager.getGuildPlayer(actor.getUniqueId());
        actor.reply("");
        actor.reply("§7§lVoici l'aide pour utiliser les guildes :");
        actor.reply("");
        if(guildPlayer == null) {
            actor.reply("§6/guild create §f→ créer une guilde.");
            actor.reply("§6/guild join <nom de la guilde> §f→ accepter l'invitation d'une guilde.");
            actor.reply("§7Davantage de commande seront affichées quand vous serez membre d'une guilde et selon votre rang dans la guilde.");
            return;
        }
        Rank rank = guildPlayer.getRank();
        if(guildPlayer.isGranted(Rank.MEMBER)) {
            actor.reply("§f§lCommandes §a§lmembre §f§f:");
            actor.reply("§6/guild info §f→ informations sur la guilde.");
            actor.reply("§6/guild chat §f→ activer/désactiver le chat de guilde.");
            actor.reply("§6/guild logs §f→ voir le journal de sa guilde.");
            actor.reply("§6/guild home §f→ rejoindre la résidence de la guilde.");
            actor.reply("§6/guild bank §f→ voir le solde de la guilde");
            actor.reply("§6/guild bank <deposit> §f→ déposer de l'argent dans la banque.");
            actor.reply("§6/guild leave §f→ quitter sa guilde.");
        }
        if (guildPlayer.isGranted(Rank.MODERATOR)) {
            actor.reply("§f§lCommandes §b§lmodérateur §f§l:");
            actor.reply("§6/guild kick §f→ exclure un membre de grade inférieur.");
        }
        if(guildPlayer.isGranted(Rank.ADMIN)) {
            actor.reply("§f§lCommandes §c§ladministrateur §f§l:");
            actor.reply("§6/guild lands §f→ gérer les territoires de la guilde.");
            actor.reply("§6/guild delhome §f→ supprimer la résidence de la guilde.");
            actor.reply("§6/guild promote §f→ promouvoir un membre.");
            actor.reply("§6/guild demote §f→ rétrograder un membre.");
            actor.reply("§6/guild sethome §f→ définir la résidence de la guilde.");
            actor.reply("§6/guild delhome §f→ supprimer la résidence de la guilde.");
            actor.reply("§6/guild bank <take> §f→ retirer de l'argent de la banque.");
        }
        if(guildPlayer.isGranted(Rank.OWNER)) {
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
        try {
            guildsManager.createGuild(name, sender.getUniqueId());
            sender.sendMessage("§aVous avez crée une guilde au nom de " + name + ".");
        } catch (GuildAlreadyExistsException | AlreadyGuildMemberException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
    }

    @Subcommand("disband")
    public void disband(Player sender) {
        try {
            guildsManager.disbandGuild(sender.getUniqueId());
        } catch (NotGuildMemberException | InsufficientPermissionException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
    }

    @Subcommand("chat")
    public void chat(Player sender) {
        try {
            GuildPlayer guildPlayer = guildsManager.getGuildPlayer(sender.getUniqueId());
            guildsManager.toggleChatMode(sender.getUniqueId());
            sender.sendMessage("§fVotre chat est désormais en : §b" + guildPlayer.getChatMode().toString());
        } catch (NotGuildMemberException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
    }

    @Subcommand("invite")
    public void invite(Player sender, Player player) {
    }

    public void join() {

    }

    @Subcommand("info")
    public void info(Player sender) {
        GuildPlayer guildPlayer = guildsManager.getGuildPlayer(sender.getUniqueId());

    }
}