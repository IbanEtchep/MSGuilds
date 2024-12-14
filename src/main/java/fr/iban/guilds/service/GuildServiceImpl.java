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

        player.sendMessage(Lang.CHAT_MODE_CHANGED.component("mode", guildPlayer.getChatMode().toString()));
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
        player.sendMessage(Lang.CHAT_MODE_CHANGED.component("mode", chatMode.toString()));
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
            player.sendMessage(Lang.ERROR_NOT_GUILD_OWNER.component());
            return;
        }

        GuildDisbandEvent disbandEvent = new GuildDisbandEvent(guild);
        disbandEvent.callEvent();

        if (disbandEvent.isCancelled()) return;

        guild.getMembers().forEach((uuid, gp) -> {
            gp.sendMessageIfOnline(Lang.GUILD_DISBANDED.toString());
            guildManager.deletePlayer(gp);
        });

        guildManager.addLog(guild, Lang.LOG_GUILD_DISBANDED.toString("player", player.getName()));
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
        guild.sendMessageToOnlineMembers(Lang.MEMBER_JOINED.toString("player", player.getName()));
        guildManager.addLog(guild, Lang.MEMBER_JOINED.toString("player", player.getName()));
        player.sendMessage(Lang.MEMBER_JOINED.component("guild", guild.getName()));
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
            player.sendMessage(Lang.ERROR_LEAVE_OWNER.component());
            return;
        }

        guild.getMembers().remove(guildPlayer.getUuid());
        guildManager.deletePlayer(guildPlayer);
        player.sendMessage(Lang.LEAVE_SUCCESS.component());
        guildManager.addLog(guild, Lang.MEMBER_LEFT.toString("player", player.getName()));
        guild.sendMessageToOnlineMembers(Lang.MEMBER_LEFT.toString("player", player.getName()));
    }

    @Override
    public void invite(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.INVITE_MEMBER)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if (guild.getInvites().contains(target.getUniqueId())) {
            player.sendMessage(Lang.MEMBER_INVITED.component());
            return;
        }

        if (guild.getMember(target.getUniqueId()) != null) {
            player.sendMessage(Lang.ERROR_ALREADY_IN_GUILD.component());
            return;
        }

        guild.getInvites().add(target.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> guild.getInvites().remove(target.getUniqueId()), 2400L);

        CoreBukkitPlugin core = CoreBukkitPlugin.getInstance();
        core.getMessagingManager().sendMessage(GuildsPlugin.GUILD_INVITE_ADD,
                new GuildRequestMessage(guild.getId(), target.getUniqueId()));
        core.getPlayerManager().sendMessageRawIfOnline(target.getUniqueId(), Lang.ALLIANCE_REQUEST_RECEIVED.toString("guild", guild.getName()));
        player.sendMessage(Lang.MEMBER_INVITED.component("player", target.getName()));
    }

    @Override
    public void revokeInvite(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.INVITE_MEMBER)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        if (!guild.getInvites().contains(target.getUniqueId())) {
            player.sendMessage(Lang.ERROR_PLAYER_NOT_INVITED.component());
            return;
        }

        guild.getInvites().remove(target.getUniqueId());
        player.sendMessage(Lang.MEMBER_INVITE_REVOKED.component("player", target.getName()));
        CoreBukkitPlugin.getInstance().getPlayerManager().sendMessageIfOnline(target.getUniqueId(), 
            Lang.ERROR_NOT_INVITED.toString("guild", guild.getName()));
    }

    @Override
    public void teleportHome(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (guild.getHome() == null) {
            player.sendMessage(Lang.HOME_NOT_SET.component());
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
        guildManager.addLog(guild, Lang.LOG_HOME_SET.toString("player", player.getName()));
        guildManager.saveGuild(guild);
        player.sendMessage(Lang.HOME_SET.component());
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
        guildManager.addLog(guild, Lang.LOG_HOME_DELETE.toString("player", player.getName()));
        player.sendMessage(Lang.HOME_DELETE.component());
    }

    @Override
    public void kick(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(Lang.ERROR_KICK_SELF.component());
            return;
        }

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.KICK_MEMBER)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        GuildPlayer targetGuildPlayer = guild.getMember(target.getUniqueId());
        if (targetGuildPlayer == null) {
            player.sendMessage(Lang.ERROR_PLAYER_NOT_IN_GUILD.toString());
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(player.getUniqueId());
        if (guildPlayer.getRank().getOrder() > targetGuildPlayer.getRank().getOrder()) {
            player.sendMessage(Lang.ERROR_KICK_RANK.component());
            return;
        }

        guild.getMembers().remove(targetGuildPlayer.getUuid());
        guild.sendMessageToOnlineMembers(Lang.KICK_SUCCESS.toString("player", target.getName()));
        targetGuildPlayer.sendMessageIfOnline(Lang.KICK_TARGET.toString());
        guildManager.addLog(guild, Lang.KICK_SUCCESS.toString("player", target.getName()));
        guildManager.deletePlayer(targetGuildPlayer);
    }

    @Override
    public void demote(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        GuildPlayer targetPlayer = guild.getMember(target.getUniqueId());
        if (targetPlayer == null) {
            player.sendMessage(Lang.ERROR_PLAYER_NOT_IN_GUILD.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(player.getUniqueId());

        if((guildPlayer.getRank().getOrder() <= targetPlayer.getRank().getOrder() && !guildPlayer.isOwner())) {
            player.sendMessage(Lang.ERROR_RANK_TOO_HIGH.component());
            return;
        }

        if(!guildPlayer.isGranted(GuildPermission.DEMOTE_MEMBER)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        GuildRank previousRank = guild.getPreviousRank(targetPlayer.getRank());
        if(previousRank == null) {
            player.sendMessage(Lang.ERROR_ALREADY_LOWEST_RANK.component());
            return;
        }

        targetPlayer.setRank(previousRank);

        guildManager.savePlayer(targetPlayer);
        guild.sendMessageToOnlineMembers(Lang.DEMOTE_SUCCESS.toString(
            "player", target.getName(),
            "rank", targetPlayer.getRank().getName(),
            "by", player.getName()
        ));
        guildManager.addLog(guild, Lang.DEMOTE_SUCCESS.toString(
            "player", target.getName(),
            "rank", targetPlayer.getRank().getName(),
            "by", player.getName()
        ));
    }

    @Override
    public void promote(Player player, OfflinePlayer target) {
        Guild guild = guildManager.getGuildByPlayer(player);

        if (guild == null) {
            player.sendMessage(Lang.ERROR_NOT_GUILD_MEMBER.component());
            return;
        }

        GuildPlayer targetPlayer = guild.getMember(target.getUniqueId());
        if (targetPlayer == null) {
            player.sendMessage(Lang.ERROR_PLAYER_NOT_IN_GUILD.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(player.getUniqueId());

        if((guildPlayer.getRank().getOrder() <= targetPlayer.getRank().getOrder() && !guildPlayer.isOwner())) {
            player.sendMessage(Lang.ERROR_TARGET_RANK_TOO_HIGH.component());
            return;
        }

        if(!guildPlayer.isGranted(GuildPermission.PROMOTE_MEMBER)) {
            player.sendMessage(Lang.ERROR_INSUFFICIENT_RANK.component());
            return;
        }

        GuildRank nextRank = guild.getNextRank(targetPlayer.getRank());
        if(nextRank == null) {
            player.sendMessage(Lang.ERROR_ALREADY_HIGHEST_RANK.component());
            return;
        }

        targetPlayer.setRank(nextRank);

        guildManager.savePlayer(targetPlayer);
        guild.sendMessageToOnlineMembers(Lang.PROMOTE_SUCCESS.toString(
            "player", target.getName(),
            "rank", targetPlayer.getRank().getName(),
            "by", player.getName()
        ));
        guildManager.addLog(guild, Lang.PROMOTE_SUCCESS.toString(
            "player", target.getName(),
            "rank", targetPlayer.getRank().getName(),
            "by", player.getName()
        ));
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
            player.sendMessage(Lang.ERROR_TRANSFER_OWNER.component());
            return;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(Lang.ERROR_TRANSFER_SELF.component());
            return;
        }

        GuildPlayer guildPlayer = guild.getMember(target.getUniqueId());
        if (guildPlayer == null) {
            player.sendMessage(Lang.ERROR_PLAYER_NOT_IN_GUILD.component());
            return;
        }

        guild.setOwnerUUID(target.getUniqueId());

        guildManager.saveGuild(guild);
        guild.sendMessageToOnlineMembers(Lang.TRANSFER_SUCCESS.toString(
            "player", player.getName(),
            "target", guildPlayer.getName()
        ));
        guildManager.addLog(guild, Lang.TRANSFER_SUCCESS.toString(
            "player", player.getName(),
            "target", guildPlayer.getName()
        ));
    }


}
