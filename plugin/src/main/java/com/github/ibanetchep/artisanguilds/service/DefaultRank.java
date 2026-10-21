package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.api.GuildPermission;

import java.util.Set;

/** The ranks a new guild starts with, the lowest first. */
enum DefaultRank {

    MEMBER("<color:#27ae60>Membre", Set.of()),
    MODERATOR("<color:#2980b9>Modérateur", Set.of(GuildPermission.KICK_MEMBER)),
    ADMIN("<color:#e74c3c>Administrateur", Set.of(
            GuildPermission.KICK_MEMBER,
            GuildPermission.MANAGE_LANDS,
            GuildPermission.PROMOTE_MEMBER,
            GuildPermission.DEMOTE_MEMBER,
            GuildPermission.MANAGE_HOME,
            GuildPermission.BANK_WITHDRAW,
            GuildPermission.MANAGE_ALLIANCES,
            GuildPermission.MANAGE_RANKS,
            GuildPermission.INVITE_MEMBER)),
    OWNER("<color:#c0392b>Fondateur", Set.of(GuildPermission.values()));

    private final String label;
    private final Set<GuildPermission> permissions;

    DefaultRank(String label, Set<GuildPermission> permissions) {
        this.label = label;
        this.permissions = permissions;
    }

    String label() {
        return label;
    }

    Set<GuildPermission> permissions() {
        return permissions;
    }
}
