package fr.iban.guilds.event;

import fr.iban.guilds.Guild;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GuildCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Guild guild;

    public GuildCreateEvent(Guild guild) {
        this.guild = guild;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public Guild getGuild() {
        return guild;
    }
}
