package com.github.ibanetchep.artisanguilds.api.event;

import com.github.ibanetchep.artisanguilds.api.Guild;
import org.bukkit.event.Event;

/**
 * An event about a guild. Fired on the server's global thread. {@link #isRemote()}: the change
 * was made on another server of the network and this server just learned it; the guild is
 * already updated here.
 */
public abstract class GuildEvent extends Event {

    private final Guild guild;
    private final boolean remote;

    protected GuildEvent(Guild guild, boolean remote) {
        this.guild = guild;
        this.remote = remote;
    }

    public Guild getGuild() {
        return guild;
    }

    public boolean isRemote() {
        return remote;
    }
}
