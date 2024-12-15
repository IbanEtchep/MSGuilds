package fr.iban.guilds.enums;

import java.util.Set;


public enum DefaultRank {

    MEMBER("<color:#27ae60>Membre", Set.of()),
    MODERATOR("<color:#2980b9>Modérateur", Set.of(
            GuildPermission.KICK_MEMBER)
    ),
    ADMIN("<color:#e74c3c>Administrateur", Set.of(
            GuildPermission.KICK_MEMBER,
            GuildPermission.MANAGE_LANDS,
            GuildPermission.PROMOTE_MEMBER,
            GuildPermission.DEMOTE_MEMBER,
            GuildPermission.MANAGE_HOME,
            GuildPermission.BANK_WITHDRAW,
            GuildPermission.MANAGE_ALLIANCES
    )),
    OWNER("<color:#c0392b>Fondateur", Set.of(
            GuildPermission.KICK_MEMBER,
            GuildPermission.MANAGE_LANDS,
            GuildPermission.PROMOTE_MEMBER,
            GuildPermission.DEMOTE_MEMBER,
            GuildPermission.MANAGE_HOME,
            GuildPermission.BANK_WITHDRAW,
            GuildPermission.MANAGE_ALLIANCES
    ));

    private final String name;
    private final Set<GuildPermission> permissions;

    DefaultRank(String name, Set<GuildPermission> permissions) {
        this.name = name;
        this.permissions = permissions;
    }

    public String getName() {
        return name;
    }

    public Set<GuildPermission> getPermissions() {
        return permissions;
    }
}
