package fr.iban.guilds.event;

import fr.iban.guilds.Guild;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GuildDisbandEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Guild guild;

    public GuildDisbandEvent(Guild guild) {
        this.guild = guild;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return null;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void setCancelled(boolean cancel) {

    }

    public Guild getGuild() {
        return guild;
    }
}
