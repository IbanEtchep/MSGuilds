package com.github.ibanetchep.artisanguilds.api.event;

import com.github.ibanetchep.artisanguilds.api.Guild;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A guild was created, its owner already a member. */
public class GuildCreateEvent extends GuildEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public GuildCreateEvent(Guild guild, boolean remote) {
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
