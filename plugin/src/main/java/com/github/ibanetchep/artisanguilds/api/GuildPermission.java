package com.github.ibanetchep.artisanguilds.api;

/** What a rank allows. The owner holds them all; {@code guilds.bypass} too. Same constants as MSGuilds. */
public enum GuildPermission {

    // Moderator
    KICK_MEMBER("Expulser un membre"),
    INVITE_MEMBER("Inviter un membre"),

    // Administrator
    MANAGE_LANDS("Gérer les territoires"),
    PROMOTE_MEMBER("Promouvoir un membre"),
    DEMOTE_MEMBER("Rétrograder un membre"),
    MANAGE_HOME("Gérer le home"),
    BANK_WITHDRAW("Retirer de l'argent de la banque"),
    MANAGE_ALLIANCES("Gérer les alliances"),
    MANAGE_RANKS("Gérer les grades");

    private final String name;

    GuildPermission(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
