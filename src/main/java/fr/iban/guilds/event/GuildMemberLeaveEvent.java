package fr.iban.guilds.event;

import fr.iban.guilds.model.Guild;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player has been removed from a guild's members, whether they left on their
 * own or were kicked. Not fired when the guild is disbanded — use {@link GuildDisbandEvent}.
 */
public class GuildMemberLeaveEvent extends Event {

    public enum Reason {
        QUIT,
        KICKED
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private final Guild guild;
    private final UUID playerId;
    private final Reason reason;

    public GuildMemberLeaveEvent(Guild guild, UUID playerId, Reason reason) {
        this.guild = guild;
        this.playerId = playerId;
        this.reason = reason;
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

    public Reason getReason() {
        return reason;
    }
}
