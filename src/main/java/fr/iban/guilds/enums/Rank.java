package fr.iban.guilds.enums;

public enum Rank {

    OWNER("Fondateur"),
    ADMIN("Administrateur"),
    MODERATOR("Modérateur"),
    MEMBER("Membre");

    private final String name;

    Rank(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
