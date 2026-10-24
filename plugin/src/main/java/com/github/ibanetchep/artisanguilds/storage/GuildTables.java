package com.github.ibanetchep.artisanguilds.storage;

/**
 * The names of ArtisanGuilds' tables under the suite's prefix
 * ({@code api.getDatabase().tablePrefix("guilds")}, {@code artisan_guilds_} by default).
 * Concatenated into SQL: the prefix is validated by Artisan (lower-case letters, digits, {@code _}).
 */
public record GuildTables(String prefix) {

    public String guilds() { return prefix + "guilds"; }
    public String ranks() { return prefix + "ranks"; }
    public String members() { return prefix + "members"; }
    public String logs() { return prefix + "logs"; }
    public String alliances() { return prefix + "alliances"; }
    public String schemaVersion() { return prefix + "schema_version"; }
    public String imports() { return prefix + "imports"; }
    public String chests() { return prefix + "chests"; }

    /** Constraint names are unique per database on MySQL: prefixed like the tables. */
    String rankLabelKey() { return prefix + "ranks_label"; }
}
