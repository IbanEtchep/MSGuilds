package fr.iban.guilds.enums;

import org.bukkit.ChatColor;

public enum Rank {

    OWNER("Fondateur", ChatColor.DARK_RED),
    ADMIN("Administrateur", ChatColor.RED),
    MODERATOR("Modérateur", ChatColor.GREEN),
    MEMBER("Membre", ChatColor.AQUA);

    private final String name;
    private final ChatColor color;

    Rank(String name, ChatColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }
}
