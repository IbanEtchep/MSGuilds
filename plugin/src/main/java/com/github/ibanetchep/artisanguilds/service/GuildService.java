package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.GuildPlatform;
import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.api.GuildHome;
import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.api.event.GuildCreateEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildDisbandEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildMemberJoinEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildMemberLeaveEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildPostDisbandEvent;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.model.RankData;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import com.github.ibanetchep.artisanguilds.sync.GuildSync;
import net.artisanmc.modules.api.network.NetworkApi;
import net.artisanmc.modules.api.network.NetworkLocation;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.UUID;

/**
 * A player's actions on their guild, as MSGuilds' {@code GuildServiceImpl}: each checks, tells
 * the player why when it refuses, changes memory, then saves and logs. Called on the player's
 * thread.
 */
public final class GuildService {

    public static final String BYPASS = "guilds.bypass";
    public static final String ADMIN = "guilds.admin";

    private final GuildRegistry registry;
    private final GuildStore store;
    private final GuildLeveling leveling;
    private final GuildMessages messages;
    private final GuildPlatform platform;
    private final NetworkApi network;
    private final GuildSync sync;

    public GuildService(GuildRegistry registry, GuildStore store, GuildLeveling leveling, GuildMessages messages,
                        GuildPlatform platform, NetworkApi network, GuildSync sync) {
        this.registry = registry;
        this.store = store;
        this.leveling = leveling;
        this.messages = messages;
        this.platform = platform;
        this.network = network;
        this.sync = sync;
    }

    public void createGuild(Player player, String name) {
        if (registry.byName(name) != null) {
            messages.send(player, Msg.GUILD_ALREADY_EXISTS, "name", name);
            return;
        }
        if (name.contains(" ")) {
            messages.send(player, Msg.GUILD_NAME_NO_SPACE);
            return;
        }
        if (name.length() > 30 || name.length() < 3) {
            messages.send(player, Msg.GUILD_NAME_INVALID_LENGTH);
            return;
        }
        if (registry.member(player.getUniqueId()) != null) {
            messages.send(player, Msg.ERROR_SELF_ALREADY_IN_GUILD);
            return;
        }

        GuildData guild = new GuildData(UUID.randomUUID(), name, player.getUniqueId(), 0, 0, leveling.levelFor(0),
                null, Instant.now(), registry::get);
        for (DefaultRank rank : DefaultRank.values()) {
            guild.addRank(new RankData(UUID.randomUUID(), rank.label(), 0, rank.permissions()));
        }
        MemberData owner = new MemberData(player.getUniqueId(), guild, guild.getRanks().getLast(), ChatMode.PUBLIC);
        owner.setName(player.getName());
        guild.putMember(owner);
        registry.put(guild);

        platform.callEvent(new GuildCreateEvent(guild, false));
        store.saveGuild(guild);
        store.saveMember(owner);
        messages.send(player, Msg.GUILD_CREATED, "name", name);
        store.addLog(guild, messages.plain(Msg.LOG_GUILD_CREATED, "player", player.getName()));
    }

    /** Public, then guild, then alliance, then public again. */
    public void toggleChatMode(Player player) {
        MemberData member = registry.member(player.getUniqueId());
        if (member == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        setChatMode(player, switch (member.getChatMode()) {
            case PUBLIC -> ChatMode.GUILD;
            case GUILD -> ChatMode.ALLY;
            case ALLY -> ChatMode.PUBLIC;
        });
    }

    public void setChatMode(Player player, ChatMode chatMode) {
        MemberData member = registry.member(player.getUniqueId());
        if (member == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        member.setChatMode(chatMode);
        messages.send(player, Msg.CHAT_MODE_CHANGED, "mode", chatMode.name());
        store.saveMember(member);
    }

    public void disbandGuild(Player player) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        if (!guild.getMember(player.getUniqueId()).isOwner() && !player.hasPermission(ADMIN)) {
            messages.send(player, Msg.ERROR_NOT_GUILD_OWNER);
            return;
        }
        GuildDisbandEvent event = new GuildDisbandEvent(guild);
        platform.callEvent(event);
        if (event.isCancelled()) return;

        messages.sendToMembers(guild, Msg.GUILD_DISBANDED);
        registry.remove(guild.getId());
        // Its members, ranks, logs and alliance rows go with it; the allies here forget it.
        store.deleteGuild(guild.getId());
        platform.callEvent(new GuildPostDisbandEvent(guild, false));
    }

    public void joinGuild(Player player, GuildData guild) {
        UUID id = player.getUniqueId();
        boolean bypass = player.hasPermission(BYPASS);
        if (!guild.invites().contains(id) && !bypass) {
            messages.send(player, Msg.ERROR_NOT_INVITED);
            return;
        }
        if (registry.member(id) != null) {
            messages.send(player, Msg.ERROR_SELF_ALREADY_IN_GUILD);
            return;
        }
        if (guild.getMembers().size() >= leveling.info(guild).maxMembers() && !bypass) {
            messages.send(player, Msg.ERROR_GUILD_FULL);
            return;
        }

        MemberData member = new MemberData(id, guild, guild.defaultRank(), ChatMode.PUBLIC);
        member.setName(player.getName());
        messages.send(player, Msg.JOIN_SUCCESS, "guild", guild.getName());
        messages.sendToMembers(guild, Msg.MEMBER_JOINED, "player", player.getName());
        store.addLog(guild, messages.plain(Msg.MEMBER_JOINED, "player", player.getName()));
        guild.invites().remove(id);
        guild.putMember(member);
        store.saveMember(member);
        platform.callEvent(new GuildMemberJoinEvent(guild, id, false));
    }

    public void quitGuild(Player player) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        if (player.getUniqueId().equals(guild.getOwnerId())) {
            messages.send(player, Msg.ERROR_LEAVE_OWNER);
            return;
        }
        guild.removeMember(player.getUniqueId());
        store.deleteMember(player.getUniqueId(), GuildMemberLeaveEvent.Reason.QUIT.name());
        messages.send(player, Msg.LEAVE_SUCCESS);
        store.addLog(guild, messages.plain(Msg.MEMBER_LEFT, "player", player.getName()));
        messages.sendToMembers(guild, Msg.MEMBER_LEFT, "player", player.getName());
        platform.callEvent(new GuildMemberLeaveEvent(guild, player.getUniqueId(), GuildMemberLeaveEvent.Reason.QUIT, false));
    }

    /** {@code target} anywhere: a player known by name to this server or online on the network. */
    public void invite(Player player, UUID targetId, String targetName) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.INVITE_MEMBER)) {
            messages.send(player, Msg.ERROR_INSUFFICIENT_RANK);
            return;
        }
        if (guild.invites().contains(targetId)) {
            messages.send(player, Msg.MEMBER_INVITED, "player", targetName);
            return;
        }
        if (guild.getMember(targetId) != null) {
            messages.send(player, Msg.ERROR_ALREADY_IN_GUILD);
            return;
        }
        if (guild.getMembers().size() >= leveling.info(guild).maxMembers()) {
            messages.send(player, Msg.ERROR_GUILD_FULL);
            return;
        }

        sync.expiring(guild.invites(), targetId);
        network.publish(GuildSync.INVITE, GuildSync.pair(guild.getId(), targetId));
        messages.sendTo(targetId, Msg.MEMBER_INVITE_RECEIVED, "guild", guild.getName());
        messages.send(player, Msg.MEMBER_INVITED, "player", targetName);
    }

    public void revokeInvite(Player player, UUID targetId, String targetName) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.INVITE_MEMBER)) {
            messages.send(player, Msg.ERROR_INSUFFICIENT_RANK);
            return;
        }
        if (!guild.invites().remove(targetId)) {
            messages.send(player, Msg.ERROR_PLAYER_NOT_INVITED);
            return;
        }
        network.publish(GuildSync.INVITE_REVOKE, GuildSync.pair(guild.getId(), targetId));
        messages.send(player, Msg.MEMBER_INVITE_REVOKED, "player", targetName);
        messages.sendTo(targetId, Msg.ERROR_NOT_INVITED, "guild", guild.getName());
    }

    /** Three seconds without moving, then to the home, on whichever server it is. */
    public void teleportHome(Player player) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        GuildHome home = guild.getHome();
        if (home == null) {
            messages.send(player, Msg.HOME_NOT_SET);
            return;
        }
        network.teleport(player, home.toLocation(), 3);
    }

    public void setHome(Player player) {
        GuildData guild = managedHome(player);
        if (guild == null) return;
        guild.setHome(GuildHome.of(NetworkLocation.of(network.getServerName(), player.getLocation())));
        store.addLog(guild, messages.plain(Msg.LOG_HOME_SET, "player", player.getName()));
        store.saveGuild(guild);
        messages.send(player, Msg.HOME_SET);
    }

    public void delHome(Player player) {
        GuildData guild = managedHome(player);
        if (guild == null) return;
        guild.setHome(null);
        store.saveGuild(guild);
        store.addLog(guild, messages.plain(Msg.LOG_HOME_DELETE, "player", player.getName()));
        messages.send(player, Msg.HOME_DELETE);
    }

    private GuildData managedHome(Player player) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return null;
        }
        if (!guild.getMember(player.getUniqueId()).isGranted(GuildPermission.MANAGE_HOME)) {
            messages.send(player, Msg.ERROR_INSUFFICIENT_RANK);
            return null;
        }
        return guild;
    }

    /** Only a member of a lower rank. */
    public void kick(Player player, UUID targetId) {
        if (player.getUniqueId().equals(targetId)) {
            messages.send(player, Msg.ERROR_KICK_SELF);
            return;
        }
        GuildData guild = registry.byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return;
        }
        MemberData self = guild.getMember(player.getUniqueId());
        if (!self.isGranted(GuildPermission.KICK_MEMBER)) {
            messages.send(player, Msg.ERROR_INSUFFICIENT_RANK);
            return;
        }
        MemberData target = guild.getMember(targetId);
        if (target == null) {
            messages.send(player, Msg.ERROR_PLAYER_NOT_IN_GUILD);
            return;
        }
        if (target.isOwner() || (self.getRank().getOrder() <= target.getRank().getOrder() && !self.isOwner())) {
            messages.send(player, Msg.ERROR_KICK_RANK);
            return;
        }

        guild.removeMember(targetId);
        messages.sendToMembers(guild, Msg.KICK_SUCCESS, "player", target.getName());
        messages.sendTo(targetId, Msg.KICK_TARGET);
        store.addLog(guild, messages.plain(Msg.KICK_SUCCESS, "player", target.getName()));
        store.deleteMember(targetId, GuildMemberLeaveEvent.Reason.KICKED.name());
        platform.callEvent(new GuildMemberLeaveEvent(guild, targetId, GuildMemberLeaveEvent.Reason.KICKED, false));
    }

    public void promote(Player player, UUID targetId) {
        changeRank(player, targetId, true);
    }

    public void demote(Player player, UUID targetId) {
        changeRank(player, targetId, false);
    }

    private void changeRank(Player player, UUID targetId, boolean up) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        MemberData target = guild == null ? null : guild.getMember(targetId);
        if (target == null) {
            messages.send(player, Msg.ERROR_PLAYER_NOT_IN_GUILD);
            return;
        }
        MemberData self = guild.getMember(player.getUniqueId());
        if (self.getRank().getOrder() <= target.getRank().getOrder() && !self.isOwner()) {
            messages.send(player, up ? Msg.ERROR_TARGET_RANK_TOO_HIGH : Msg.ERROR_RANK_TOO_HIGH);
            return;
        }
        if (!self.isGranted(up ? GuildPermission.PROMOTE_MEMBER : GuildPermission.DEMOTE_MEMBER)) {
            messages.send(player, Msg.ERROR_INSUFFICIENT_RANK);
            return;
        }
        RankData next = up ? guild.rankAbove(target.getRank()) : guild.rankBelow(target.getRank());
        if (next == null) {
            messages.send(player, up ? Msg.ERROR_ALREADY_HIGHEST_RANK : Msg.ERROR_ALREADY_LOWEST_RANK);
            return;
        }

        target.setRank(next);
        store.saveMember(target);
        Msg msg = up ? Msg.PROMOTE_SUCCESS : Msg.DEMOTE_SUCCESS;
        Object[] placeholders = {"player", target.getName(), "rank", next.getName(), "by", player.getName()};
        messages.sendToMembers(guild, msg, placeholders);
        store.addLog(guild, messages.plain(msg, placeholders));
    }

    /** The owner (or an admin) gives the guild to another member. */
    public void transfer(Player player, UUID targetId) {
        GuildData guild = registry.byPlayer(player.getUniqueId());
        MemberData target = guild == null ? null : guild.getMember(targetId);
        if (target == null) {
            messages.send(player, Msg.ERROR_PLAYER_NOT_IN_GUILD);
            return;
        }
        if (!player.getUniqueId().equals(guild.getOwnerId()) && !player.hasPermission(ADMIN)) {
            messages.send(player, Msg.ERROR_TRANSFER_OWNER);
            return;
        }
        if (player.getUniqueId().equals(targetId)) {
            messages.send(player, Msg.ERROR_TRANSFER_SELF);
            return;
        }

        guild.setOwnerId(targetId);
        store.saveGuild(guild);
        messages.sendToMembers(guild, Msg.TRANSFER_SUCCESS, "player", player.getName(), "target", target.getName());
        store.addLog(guild, messages.plain(Msg.TRANSFER_SUCCESS, "player", player.getName(), "target", target.getName()));
    }
}
