package com.github.ibanetchep.artisanguilds.api.event;

import com.github.ibanetchep.artisanguilds.api.Guild;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A guild was disbanded: it is no longer listed. {@link #getGuild()} is its last state. */
public class GuildPostDisbandEvent extends GuildEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public GuildPostDisbandEvent(Guild guild, boolean remote) {
        super(guild, remote);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
