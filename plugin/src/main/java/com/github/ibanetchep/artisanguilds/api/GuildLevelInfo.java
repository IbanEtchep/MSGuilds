package com.github.ibanetchep.artisanguilds.api;

/**
 * A level of {@code levels} in config.yml: the experience it starts at, the member cap it gives,
 * and the guild chest it opens ({@code chestPages} pages of {@code chestRows} rows; 0 pages: none).
 */
public record GuildLevelInfo(int level, long requiredXp, int maxMembers, int chestRows, int chestPages) {

    /** A level without a guild chest. */
    public GuildLevelInfo(int level, long requiredXp, int maxMembers) {
        this(level, requiredXp, maxMembers, 0, 0);
    }
}
