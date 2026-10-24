package com.github.ibanetchep.artisanguilds.api.event;

import com.github.ibanetchep.artisanguilds.api.Guild;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** A player joined a guild; they are already among its members. Not fired for a guild's creator. */
public class GuildMemberJoinEvent extends GuildEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;

    public GuildMemberJoinEvent(Guild guild, UUID playerId, boolean remote) {
        super(guild, remote);
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
