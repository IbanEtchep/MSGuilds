package com.github.ibanetchep.artisanguilds.api;

import java.util.Set;
import java.util.UUID;

/** A rank of a guild. Ranks are ordered from the lowest (0, given to new members) up. */
public interface GuildRank {

    UUID getId();

    /** As the guild typed it, MiniMessage allowed. */
    String getName();

    int getOrder();

    Set<GuildPermission> getPermissions();

    default boolean hasPermission(GuildPermission permission) {
        return getPermissions().contains(permission);
    }
}
