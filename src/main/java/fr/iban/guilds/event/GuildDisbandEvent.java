package fr.iban.guilds.event;

import fr.iban.guilds.model.Guild;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GuildDisbandEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Guild guild;
    private boolean cancelled;

    public GuildDisbandEvent(Guild guild) {
        this.guild = guild;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public Guild getGuild() {
        return guild;
    }

}
