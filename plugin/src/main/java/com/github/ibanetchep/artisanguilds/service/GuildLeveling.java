package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.api.GuildLevelInfo;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;

/** The levels of config.yml: a guild's level follows its experience. */
public final class GuildLeveling {

    private final NavigableMap<Integer, GuildLevelInfo> levels = new TreeMap<>();
    private final GuildStore store;

    public GuildLeveling(NavigableMap<Integer, GuildLevelInfo> levels, GuildStore store) {
        if (levels.isEmpty()) throw new IllegalStateException("No levels defined in config.yml");
        this.levels.putAll(levels);
        this.store = store;
    }

    /** {@code levels: {1: {xp: 0, max-members: 50}, ...}}; invalid keys are logged and skipped. */
    public static NavigableMap<Integer, GuildLevelInfo> read(@Nullable ConfigurationSection section, Logger logger) {
        NavigableMap<Integer, GuildLevelInfo> levels = new TreeMap<>();
        if (section == null) return levels;
        for (String key : section.getKeys(false)) {
            int level;
            try {
                level = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                logger.warning("Invalid level key in config.yml: " + key);
                continue;
            }
            levels.put(level, new GuildLevelInfo(level, section.getLong(key + ".xp", 0L), section.getInt(key + ".max-members", 0)));
        }
        return levels;
    }

    public int levelFor(long exp) {
        int level = levels.firstKey();
        for (GuildLevelInfo info : levels.values()) {
            if (exp < info.requiredXp()) break;
            level = info.level();
        }
        return level;
    }

    public @Nullable GuildLevelInfo info(int level) {
        return levels.get(level);
    }

    /** The guild's level as its experience gives it. */
    public GuildLevelInfo info(GuildData guild) {
        return levels.get(levelFor(guild.getExp()));
    }

    public void addExp(GuildData guild, long exp) {
        if (exp > 0) setExp(guild, guild.getExp() + exp);
    }

    public void removeExp(GuildData guild, long exp) {
        if (exp > 0) setExp(guild, Math.max(0, guild.getExp() - exp));
    }

    public void setExp(GuildData guild, long exp) {
        long value = Math.max(0, exp);
        guild.setExp(value, levelFor(value));
        store.saveGuild(guild);
    }

    /** Below the first level: the first. A level config.yml lacks: {@link IllegalArgumentException}. */
    public void setLevel(GuildData guild, int level) {
        int target = Math.max(level, levels.firstKey());
        GuildLevelInfo info = levels.get(target);
        if (info == null) throw new IllegalArgumentException("Level " + level + " does not exist");
        setExp(guild, info.requiredXp());
    }
}
