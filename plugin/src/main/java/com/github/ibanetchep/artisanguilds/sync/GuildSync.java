package com.github.ibanetchep.artisanguilds.sync;

import com.github.ibanetchep.artisanguilds.GuildPlatform;
import com.github.ibanetchep.artisanguilds.api.event.GuildCreateEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildMemberJoinEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildMemberLeaveEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildPostDisbandEvent;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository.GuildRow;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository.MemberRow;
import net.artisanmc.modules.api.network.NetworkApi;
import net.artisanmc.modules.api.network.NetworkMessage;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * What another server changed, applied here: a guild or a player is read again from the
 * database (on the store's thread, after the writes this server queued), then applied on the
 * global thread, where the same events as a local change fire with {@code remote} set.
 * Invitations live in memory only and travel as messages.
 */
public final class GuildSync {

    public static final String GUILD = "guilds:sync";
    public static final String PLAYER = "guilds:player";
    public static final String INVITE = "guilds:invite";
    public static final String INVITE_REVOKE = "guilds:invite_revoke";
    public static final String ALLIANCE_REQUEST = "guilds:alliance_request";

    /** Invitations and alliance requests expire after two minutes, as in MSGuilds. */
    public static final long INVITE_TICKS = 2400L;

    private final GuildRegistry registry;
    private final GuildRepository repository;
    private final Executor reader;
    private final GuildPlatform platform;
    private final Logger logger;

    public GuildSync(GuildRegistry registry, GuildRepository repository, Executor reader, GuildPlatform platform, Logger logger) {
        this.registry = registry;
        this.repository = repository;
        this.reader = reader;
        this.platform = platform;
        this.logger = logger;
    }

    public void subscribe(Plugin owner, NetworkApi network) {
        network.subscribe(owner, GUILD, m -> read(m, () -> {
            UUID id = UUID.fromString(m.getPayload());
            GuildRow row = repository.load(id);
            platform.global(() -> applyGuild(id, row));
        }));
        network.subscribe(owner, PLAYER, m -> read(m, () -> {
            String[] parts = m.getPayload().split(";", 2);
            UUID playerId = UUID.fromString(parts[0]);
            MemberRow row = repository.loadMember(playerId);
            GuildMemberLeaveEvent.Reason reason = parts.length > 1 ? reason(parts[1]) : GuildMemberLeaveEvent.Reason.UNKNOWN;
            platform.global(() -> applyMember(playerId, row, reason));
        }));
        network.subscribe(owner, INVITE, m -> pair(m, (guild, target) -> expiring(guild.invites(), target)));
        network.subscribe(owner, INVITE_REVOKE, m -> pair(m, (guild, target) -> guild.invites().remove(target)));
        network.subscribe(owner, ALLIANCE_REQUEST, m -> pair(m, (guild, target) -> expiring(guild.allianceInvites(), target)));
    }

    /** {@code playerId} or {@code playerId;REASON}. */
    public static String player(UUID playerId, @Nullable String reason) {
        return reason == null ? playerId.toString() : playerId + ";" + reason;
    }

    public static String pair(UUID guildId, UUID target) {
        return guildId + ";" + target;
    }

    /** Adds {@code id}, removed again after {@link #INVITE_TICKS}. */
    public void expiring(Set<UUID> set, UUID id) {
        if (set.add(id)) platform.later(INVITE_TICKS, () -> set.remove(id));
    }

    public void applyGuild(UUID id, @Nullable GuildRow row) {
        GuildData known = registry.get(id);
        if (row == null) {
            if (known != null) {
                registry.remove(id);
                platform.callEvent(new GuildPostDisbandEvent(known, true));
            }
            return;
        }
        if (known == null) {
            GuildData guild = registry.create(row);
            registry.put(guild);
            // A player still listed in another guild here moved: their own message follows.
            platform.callEvent(new GuildCreateEvent(guild, true));
            return;
        }
        GuildRegistry.Diff diff = registry.apply(known, row);
        diff.joined().forEach(p -> platform.callEvent(new GuildMemberJoinEvent(known, p, true)));
        diff.left().forEach(p -> platform.callEvent(new GuildMemberLeaveEvent(known, p, GuildMemberLeaveEvent.Reason.UNKNOWN, true)));
    }

    public void applyMember(UUID playerId, @Nullable MemberRow row, GuildMemberLeaveEvent.Reason reason) {
        GuildData before = registry.byPlayer(playerId);
        GuildData after = row == null ? null : registry.get(row.guildId());
        if (row != null && after == null) {
            // A guild this server has not read yet: its own message brings the member.
            return;
        }
        if (before != null && !before.equals(after)) {
            before.removeMember(playerId);
            platform.callEvent(new GuildMemberLeaveEvent(before, playerId, reason, true));
        }
        if (after == null) return;
        var rank = row.rankId() != null ? after.getRank(row.rankId()) : null;
        if (rank == null) rank = after.defaultRank();
        MemberData member = after.getMember(playerId);
        if (member != null) {
            member.setRank(rank);
            member.setChatMode(row.chatMode());
            return;
        }
        after.putMember(new MemberData(playerId, after, rank, row.chatMode()));
        after.invites().remove(playerId);
        platform.callEvent(new GuildMemberJoinEvent(after, playerId, true));
    }

    private static GuildMemberLeaveEvent.Reason reason(String name) {
        try {
            return GuildMemberLeaveEvent.Reason.valueOf(name);
        } catch (IllegalArgumentException e) {
            return GuildMemberLeaveEvent.Reason.UNKNOWN;
        }
    }

    @FunctionalInterface
    private interface Read {
        void run() throws SQLException;
    }

    private void read(NetworkMessage message, Read read) {
        reader.execute(() -> {
            try {
                read.run();
            } catch (SQLException | RuntimeException e) {
                logger.log(Level.WARNING, "[ArtisanGuilds] Change from " + message.getSourceServer()
                        + " not applied (" + message.getChannel() + " " + message.getPayload() + ")", e);
            }
        });
    }

    private void pair(NetworkMessage message, java.util.function.BiConsumer<GuildData, UUID> apply) {
        String[] parts = message.getPayload().split(";", 2);
        if (parts.length != 2) return;
        try {
            UUID guildId = UUID.fromString(parts[0]);
            UUID target = UUID.fromString(parts[1]);
            platform.global(() -> {
                GuildData guild = registry.get(guildId);
                if (guild != null) apply.accept(guild, target);
            });
        } catch (IllegalArgumentException e) {
            logger.warning("[ArtisanGuilds] Malformed " + message.getChannel() + " from " + message.getSourceServer());
        }
    }
}
