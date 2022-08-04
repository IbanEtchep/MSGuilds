package fr.iban.guilds.event;

import fr.iban.guilds.Guild;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GuildPostDisbandEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Guild guild;

    public GuildPostDisbandEvent(Guild guild) {
        this.guild = guild;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Guild getGuild() {
        return guild;
    }

}
