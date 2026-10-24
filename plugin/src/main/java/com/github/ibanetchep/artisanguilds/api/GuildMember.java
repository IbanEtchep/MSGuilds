package com.github.ibanetchep.artisanguilds.api;

import java.util.UUID;

/** A player of a guild. */
public interface GuildMember {

    UUID getPlayerId();

    /** The player's last known name; their id when the server never saw them. */
    String getName();

    Guild getGuild();

    GuildRank getRank();

    ChatMode getChatMode();

    boolean isOwner();

    /**
     * True when the rank grants {@code permission}, for the owner, and for a player online on this
     * server with {@code guilds.bypass}.
     */
    boolean isGranted(GuildPermission permission);
}
