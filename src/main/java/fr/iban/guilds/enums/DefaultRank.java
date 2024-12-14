package fr.iban.guilds.enums;

import java.util.Set;


public enum DefaultRank {

    MEMBER("&aMembre", Set.of()),
    MODERATOR("&9&lModérateur", Set.of(
            GuildPermission.KICK_MEMBER)
    ),
    ADMIN("&c&lAdministrateur", Set.of(
            GuildPermission.KICK_MEMBER,
            GuildPermission.MANAGE_LANDS,
            GuildPermission.PROMOTE_MEMBER,
            GuildPermission.DEMOTE_MEMBER,
            GuildPermission.MANAGE_HOME,
            GuildPermission.BANK_WITHDRAW,
            GuildPermission.MANAGE_ALLIANCES
    )),
    OWNER("&4&lFondateur", Set.of(
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
