package com.github.ibanetchep.artisanguilds.api.event;

import com.github.ibanetchep.artisanguilds.api.Guild;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A player left a guild, on their own or kicked; they are no longer among its members. Not fired
 * when the guild is disbanded: see {@link GuildPostDisbandEvent}.
 */
public class GuildMemberLeaveEvent extends GuildEvent {

    public enum Reason {
        QUIT,
        KICKED,
        /** Learned from another server, which did not say why. */
        UNKNOWN
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final Reason reason;

    public GuildMemberLeaveEvent(Guild guild, UUID playerId, Reason reason, boolean remote) {
        super(guild, remote);
        this.playerId = playerId;
        this.reason = reason;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Reason getReason() {
        return reason;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
