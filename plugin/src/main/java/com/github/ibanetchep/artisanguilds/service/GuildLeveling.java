package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.api.GuildLevelInfo;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;

/** The levels of data/settings.yml: a guild's level follows its experience. */
public final class GuildLeveling {

    /** Replaced whole by {@link #replace}: readers on any thread see one table or the other. */
    private volatile NavigableMap<Integer, GuildLevelInfo> levels;
    private final GuildStore store;

    public GuildLeveling(NavigableMap<Integer, GuildLevelInfo> levels, GuildStore store) {
        this.levels = copy(levels);
        this.store = store;
    }

    /** The levels of a reloaded data/settings.yml; the guilds keep their experience. */
    public void replace(NavigableMap<Integer, GuildLevelInfo> levels) {
        this.levels = copy(levels);
    }

    private static NavigableMap<Integer, GuildLevelInfo> copy(NavigableMap<Integer, GuildLevelInfo> levels) {
        if (levels.isEmpty()) throw new IllegalStateException("No levels defined in data/settings.yml");
        return java.util.Collections.unmodifiableNavigableMap(new TreeMap<>(levels));
    }

    /**
     * {@code levels: {0: {xp: 0, max-members: 5}, 2: {xp: 2, chest-rows: 3, chest-pages: 1}, ...}};
     * invalid keys are logged and skipped. The chest is cumulative: a level that does not set
     * {@code chest-rows} or {@code chest-pages} keeps the previous level's; rows are 0 to 6.
     */
    public static NavigableMap<Integer, GuildLevelInfo> read(@Nullable ConfigurationSection section, Logger logger) {
        NavigableMap<Integer, GuildLevelInfo> levels = new TreeMap<>();
        if (section == null) return levels;
        NavigableMap<Integer, String> keys = new TreeMap<>();
        for (String key : section.getKeys(false)) {
            try {
                keys.put(Integer.parseInt(key), key);
            } catch (NumberFormatException e) {
                logger.warning("Invalid level key in config.yml: " + key);
            }
        }
        int rows = 0;
        int pages = 0;
        for (Map.Entry<Integer, String> e : keys.entrySet()) {
            String key = e.getValue();
            rows = section.getInt(key + ".chest-rows", rows);
            pages = section.getInt(key + ".chest-pages", pages);
            levels.put(e.getKey(), new GuildLevelInfo(e.getKey(), section.getLong(key + ".xp", 0L),
                    section.getInt(key + ".max-members", 0), Math.max(0, Math.min(6, rows)), Math.max(0, pages)));
        }
        return levels;
    }

    public int levelFor(long exp) {
        NavigableMap<Integer, GuildLevelInfo> levels = this.levels;
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

    /** The highest level config.yml defines. */
    public int maxLevel() {
        return levels.lastKey();
    }

    /** The first level with a guild chest; -1 when none has one. */
    public int chestUnlockLevel() {
        return levels.values().stream().filter(l -> l.chestPages() > 0).mapToInt(GuildLevelInfo::level).findFirst().orElse(-1);
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
        NavigableMap<Integer, GuildLevelInfo> levels = this.levels;
        int target = Math.max(level, levels.firstKey());
        GuildLevelInfo info = levels.get(target);
        if (info == null) throw new IllegalArgumentException("Level " + level + " does not exist");
        setExp(guild, info.requiredXp());
    }
}
