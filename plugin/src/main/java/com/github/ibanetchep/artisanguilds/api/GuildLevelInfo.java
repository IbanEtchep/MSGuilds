package com.github.ibanetchep.artisanguilds.api;

/** A level of {@code levels} in config.yml: the experience it starts at and the member cap it gives. */
public record GuildLevelInfo(int level, long requiredXp, int maxMembers) {}
