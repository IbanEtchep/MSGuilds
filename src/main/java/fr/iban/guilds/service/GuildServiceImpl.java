
package fr.iban.guilds.service;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.bukkitcore.utils.SLocationUtils;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.api.GuildManager;
import fr.iban.guilds.api.service.GuildService;
import fr.iban.guilds.enums.ChatMode;
import fr.iban.guilds.enums.DefaultRank;
import fr.iban.guilds.enums.GuildPermission;
import fr.iban.guilds.event.GuildCreateEvent;
import fr.iban.guilds.event.GuildDisbandEvent;
import fr.iban.guilds.event.GuildPostDisbandEvent;
import fr.iban.guilds.lang.Lang;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildPlayer;
import fr.iban.guilds.model.GuildRank;
import fr.iban.guilds.util.GuildRequestMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GuildServiceImpl implements GuildService {

    private final GuildsPlugin plugin;
    private final GuildManager guildManager;

    public GuildServiceImpl(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.guildManager = plugin.getGuildManager();
    }

    @Override
    public void createGuild(Player player, String name) {
        if (guildManager.getGuildByName(name) != null) {
            player.sendMessage(Lang.GUILD_ALREADY_EXISTS.component("name", name));
            return;
        }

        if (name.contains(" ")) {
            player.sendMessage(Lang.GUILD_NAME_NO_SPACE.component());
            return;
        }

        if (name.length() > 30) {
            player.sendMessage(Lang.GUILD_NAME_TOO_LONG.component());
            return;
        }

        if (guildManager.getGuildPlayer(player.getUniqueId()) != null) {
            player.sendMessage(Lang.ERROR_ALREADY_IN_GUILD.component());
            return;
        }

        Guild guild = new Guild(name);
        guild.setOwnerUUID(player.getUniqueId());

        int order = 0;
        for (DefaultRank defaultRank : DefaultRank.values()) {
            guild.addRank(new GuildRank(UUID.randomUUID(), defaultRank.getName(), order, defaultRank.getPermissions()));
            order++;
        }

        GuildRank ownerRank = guild.getRank(DefaultRank.OWNER.getName());

        GuildPlayer guildPlayer = new GuildPlayer(player.getUniqueId(), guild, ownerRank, ChatMode.PUBLIC);
        guild.getMembers().put(player.getUniqueId(), guildPlayer);

        new GuildCreateEvent(guild).callEvent();
        guildManager.saveGuild(guild);
        guildManager.savePlayer(guildPlayer);
        player.sendMessage(Lang.GUILD_CREATED.component("name", name));
        guildManager.addLog(guild, Lang.LOG_GUILD_CREATED.toString("player", player.getName()));
    }

    @Override
    public void toggleChatMode(Player player) {
        GuildPlayer guildPlayer = guildManager.getGuildPlayer(player.getUniqueId());

        if (guildPlayer == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (guildPlayer.getChatMode() == ChatMode.PUBLIC) {
            guildPlayer.setChatMode(ChatMode.GUILD);
        } else if (guildPlayer.getChatMode() == ChatMode.GUILD) {
            guildPlayer.setChatMode(ChatMode.ALLY);
        } else {
            guildPlayer.setChatMode(ChatMode.PUBLIC);
        }

        player.sendMessage("§fVotre chat est désormais en : §b" + guildPlayer.getChatMode().toString());
        guildManager.savePlayer(guildPlayer);
    }

    @Override
    public void setChatMode(Player player, ChatMode chatMode) {
        GuildPlayer guildPlayer = guildManager.getGuildPlayer(player.getUniqueId());

        if (guildPlayer == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        guildPlayer.setChatMode(chatMode);
        player.sendMessage("§fVotre chat est désormais en : §b" + guildPlayer.getChatMode().toString());
        guildManager.savePlayer(guildPlayer);
    }

    @Override
    public void disbandGuild(Player player) {
        Guild guild = guildManager.getGuildByPlayerId(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(player.getUniqueId());
        if (!guildPlayer.isOwner() && !player.hasPermission("guilds.admin")) {
            player.sendMessage("§cVous devez être fondateur de la guilde pour la dissoudre.");
            return;
        }

        GuildDisbandEvent disbandEvent = new GuildDisbandEvent(guild);
        disbandEvent.callEvent();

        if (disbandEvent.isCancelled()) return;

        guild.getMembers().forEach((uuid, gp) -> {
            gp.sendMessageIfOnline("§cVotre guilde a été dissoute.");
            guildManager.deletePlayer(gp);
        });

        guildManager.addLog(guild, "Dissolution de la guilde " + guild.getName() + " par " + player.getName());
        guildManager.deleteGuild(guild);
        new GuildPostDisbandEvent(guild).callEvent();
    }

    @Override
    public void joinGuild(Player player, Guild guild) {
        UUID uuid = player.getUniqueId();
        if (guildManager.getGuildByPlayerId(uuid) != null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getInvites().contains(uuid) && !player.hasPermission("guilds.bypass")) {
            player.sendMessage(Lang.ERROR_NOT_INVITED.component());
            return;
        }

        GuildPlayer guildPlayer = new GuildPlayer(uuid, guild, guild.getDefautRank(), ChatMode.PUBLIC);
        guild.sendMessageToOnlineMembers(Lang.PLAYER_JOINED_GUILD.toString("player", player.getName()));
        guildManager.addLog(guild, Lang.PLAYER_JOINED_GUILD.toString("player", player.getName()));
        player.sendMessage("§aVous avez rejoint la guilde " + guild.getName() + ".");
        guild.getInvites().remove(uuid);
        guild.getMembers().put(uuid, guildPlayer);
        guildManager.savePlayer(guildPlayer);
    }

    @Override
    public void quitGuild(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(player.getUniqueId());

        if (guild.getOwnerUUID() == player.getUniqueId()) {
            player.sendMessage("§cVous ne pouvez pas quitter la guilde en étant fondateur. Veuillez promouvoir quelqu'un fondateur ou dissoudre la guilde.");
            return;
        }

        guild.getMembers().remove(guildPlayer.getUuid());
        guildManager.deletePlayer(guildPlayer);
        player.sendMessage("§cVous avez quitté votre guilde.");
        guildManager.addLog(guild, Lang.PLAYER_LEFT_GUILD.toString().replace("name", player.getName()));
        guild.sendMessageToOnlineMembers(Lang.PLAYER_LEFT_GUILD.toString("name", player.getName()));
    }

    @Override
    public void invite(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.INVITE_MEMBER)) {
            player.sendMessage("§cVous n'avez pas la permission d'inviter des gens dans la guilde.");
            return;
        }

        if (guild.getInvites().contains(target.getUniqueId())) {
            player.sendMessage("§cVous avez déjà envoyé une invitation à ce joueur.");
            return;
        }

        if (guild.getMember(target.getUniqueId()) != null) {
            player.sendMessage("§cCe joueur est déjà dans votre guilde.");
            return;
        }

        guild.getInvites().add(target.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> guild.getInvites().remove(target.getUniqueId()), 2400L);

        CoreBukkitPlugin core = CoreBukkitPlugin.getInstance();
        core.getMessagingManager().sendMessage(GuildsPlugin.GUILD_INVITE_ADD,
                new GuildRequestMessage(guild.getId(), target.getUniqueId()));
        core.getPlayerManager().sendMessageRawIfOnline(target.getUniqueId(), "[\"\",{\"text\":\"Vous avez reçu une invitation à rejoindre la guilde\",\"color\":\"green\"},{\"text\":\" " + guild.getName() + "\",\"color\":\"dark_green\"},{\"text\":\". Tapez \",\"color\":\"green\"},{\"text\":\"/guild join " + guild.getName() + "\",\"bold\":true,\"color\":\"white\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/guild join " + guild.getName() + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Clic pour accepter\"}},{\"text\":\" ou cliquez\",\"color\":\"green\"},{\"text\":\" pour accepter.\",\"color\":\"green\"}]");
        player.sendMessage("§aVous avez invité " + target.getName() + " à rejoindre votre guilde.");
    }

    @Override
    public void revokeInvite(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.INVITE_MEMBER)) {
            player.sendMessage("§cVous n'avez pas la permission de révoquer une invitation.");
            return;
        }

        if (!guild.getInvites().contains(target.getUniqueId())) {
            player.sendMessage("§cCe joueur n'a pas d'invitation.");
            return;
        }

        guild.getInvites().remove(target.getUniqueId());
        player.sendMessage("§cVous avez révoqué l'invitation envoyée à " + target.getName() + ".");
        CoreBukkitPlugin.getInstance().getPlayerManager().sendMessageIfOnline(target.getUniqueId(),
                "§cL'invitation que vous avez reçu de §2§l" + guild.getName() + "§c a expiré.");
    }

    @Override
    public void teleportHome(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (guild.getHome() == null) {
            player.sendMessage("§cVotre guilde n'a pas de résidence.");
            return;
        }

        CoreBukkitPlugin.getInstance().getTeleportManager().teleport(player, guild.getHome(), 3);
    }

    @Override
    public void setHome(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        guild.setHome(SLocationUtils.getSLocation(player.getLocation()));
        guildManager.addLog(guild, player.getName() + " a redéfini la position de la résidence de la guilde.");
        guildManager.saveGuild(guild);
        player.sendMessage("§aVous avez redéfini la position de la résidence de votre guilde.");
    }

    @Override
    public void delHome(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        guild.setHome(null);
        guildManager.saveGuild(guild);
        guildManager.addLog(guild, player.getName() + " a supprimé la résidence de la guilde.");
        player.sendMessage("§aVous supprimé la résidence de votre guilde.");
    }

    @Override
    public void kick(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage("§cVous ne pouvez pas vous exclure vous même !");
            return;
        }

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.KICK_MEMBER)) {
            player.sendMessage("§cVous n'avez pas la permission d'exclure des membres de la guilde.");
            return;
        }

        GuildPlayer targetGuildPlayer = guild.getMember(target.getUniqueId());
        if (targetGuildPlayer == null) {
            player.sendMessage(Lang.ERROR_PLAYER_NOT_IN_GUILD.toString());
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(player.getUniqueId());
        if (guildPlayer.getRank().getOrder() > targetGuildPlayer.getRank().getOrder()) {
            player.sendMessage("§cVous devez être plus gradé que la personne que vous voulez exclure.");
            return;
        }

        guild.getMembers().remove(targetGuildPlayer.getUuid());
        guild.sendMessageToOnlineMembers("§c" + target.getName() + " a été exclu de la guilde.");
        targetGuildPlayer.sendMessageIfOnline("§cVous avez été exclu de la guilde.");
        guildManager.addLog(guild, target.getName() + " a été exclu de la guilde.");
        guildManager.deletePlayer(targetGuildPlayer);
    }

    @Override
    public void demote(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage("§cVous ne pouvez pas vous rétrograder vous même !");
            return;
        }

        GuildPlayer targetPlayer = guild.getMember(target.getUniqueId());
        if (targetPlayer == null) {
            player.sendMessage(Lang.ERROR_PLAYER_NOT_IN_GUILD.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(player.getUniqueId());

        if((guildPlayer.getRank().getOrder() <= targetPlayer.getRank().getOrder() && !guildPlayer.isOwner())) {
            player.sendMessage("§cVous devez être plus gradé que la personne que vous voulez rétrograder.");
            return;
        }

        if(!guildPlayer.isGranted(GuildPermission.DEMOTE_MEMBER)) {
            player.sendMessage("§cVous n'avez pas la permission de rétrograder des membres.");
            return;
        }

        guildManager.savePlayer(targetPlayer);
        guild.sendMessageToOnlineMembers("§7" + target.getName() + " a été rétrogradé " + targetPlayer.getRank().getName() + ".");
        guildManager.addLog(guild, target.getName() + " a été rétrogradé " + targetPlayer.getRank().getName() + " par " + player.getName());
    }

    @Override
    public void promote(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage("§cVous ne pouvez pas vous promouvoir vous même !");
            return;
        }

        GuildPlayer targetPlayer = guild.getMember(target.getUniqueId());
        if (targetPlayer == null) {
            player.sendMessage(Lang.ERROR_PLAYER_NOT_IN_GUILD.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(player.getUniqueId());

        if((guildPlayer.getRank().getOrder() <= targetPlayer.getRank().getOrder() && !guildPlayer.isOwner())) {
            player.sendMessage("§cVous devez être plus gradé que la personne que vous voulez promouvoir.");
            return;
        }

        if(!guildPlayer.isGranted(GuildPermission.PROMOTE_MEMBER)) {
            player.sendMessage("§cVous n'avez pas la permission de promouvoir des membres.");
            return;
        }

        guildManager.savePlayer(targetPlayer);
        guild.sendMessageToOnlineMembers("§7" + target.getName() + " a été promu " + targetPlayer.getRank().getName() + " par " + player.getName() + ".");
        guildManager.addLog(guild, target.getName() + " a été promu " + targetPlayer.getRank().getName() + " par " + player.getName());
    }

    @Override
    public void transfer(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        GuildPlayer guildOwner = guild.getOwner();
        if ((guildOwner != null && !guildOwner.getUuid().equals(player.getUniqueId())) && !player.hasPermission("guilds.admin")) {
            player.sendMessage("§cIl faut être le fondateur pour transférer la proprieté de la guilde.");
            return;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(Lang.ERROR_ALREADY_GUILD_OWNER.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(target.getUniqueId());
        if (guildPlayer == null) {
            player.sendMessage(Lang.ERROR_PLAYER_NOT_IN_GUILD.component());
            return;
        }

        guild.setOwnerUUID(target.getUniqueId());

        guildManager.saveGuild(guild);
        guild.sendMessageToOnlineMembers("§7§l" + player.getName() + " a transféré la proprieté de la guilde à " + guildPlayer.getName() + ".");
        guildManager.addLog(guild, player.getName() + " a transféré la proprieté de la guilde à " + guildPlayer.getName() + ".");
    }


}
