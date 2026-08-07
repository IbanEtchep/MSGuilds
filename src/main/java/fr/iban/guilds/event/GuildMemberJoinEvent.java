package fr.iban.guilds.event;

import fr.iban.guilds.model.Guild;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player has been added to a guild's members.
 */
public class GuildMemberJoinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Guild guild;
    private final UUID playerId;

    public GuildMemberJoinEvent(Guild guild, UUID playerId) {
        this.guild = guild;
        this.playerId = playerId;
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

    public UUID getPlayerId() {
        return playerId;
    }
}
