package com.github.ibanetchep.artisanguilds.api.event;

import com.github.ibanetchep.artisanguilds.api.Guild;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A guild is about to be disbanded on this server; cancelling keeps it. Never remote: the other
 * servers only see {@link GuildPostDisbandEvent}.
 */
public class GuildDisbandEvent extends GuildEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    public GuildDisbandEvent(Guild guild) {
        super(guild, false);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
