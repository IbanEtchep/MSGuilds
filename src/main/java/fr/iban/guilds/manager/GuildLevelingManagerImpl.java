package fr.iban.guilds.manager;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.api.GuildLevelingManager;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildLevelInfo;

import java.util.NavigableMap;
import java.util.TreeMap;

public class GuildLevelingManagerImpl implements GuildLevelingManager {

    private final GuildsPlugin plugin;
    private final NavigableMap<Integer, GuildLevelInfo> levels = new TreeMap<>();

    public GuildLevelingManagerImpl(GuildsPlugin plugin) {
        this.plugin = plugin;

        var config = plugin.getConfiguration();

        var section = config.getSection("levels");
        if (section == null) {
            throw new IllegalStateException("No 'levels' section in config.yml");
        }

        for (Object keyObj : section.getKeys()) {
            String key = String.valueOf(keyObj);

            int level;
            try {
                level = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid level key in config: " + key);
                continue;
            }

            long xp = section.getLong(key + ".xp", 0L);
            int maxMembers = section.getInt(key + ".max-members", 0);

            levels.put(level, new GuildLevelInfo(level, xp, maxMembers));

            plugin.getLogger().info("Loaded level " + level + " with " + xp + " XP and " + maxMembers + " max members");
        }

        if (levels.isEmpty()) {
            throw new IllegalStateException("No levels defined in config.yml");
        }
    }

    @Override
    public int getLevel(long xp) {
        int currentLevel = levels.firstKey();

        for (GuildLevelInfo lvl : levels.values()) {
            if (xp >= lvl.requiredXp()) {
                currentLevel = lvl.level();
            } else {
                break;
            }
        }

        return currentLevel;
    }

    @Override
    public long getExpForLevel(int level) throws IllegalArgumentException {
        GuildLevelInfo info = levels.get(level);
        if (info == null) {
            throw new IllegalArgumentException("Level " + level + " does not exist");
        }

        return info.requiredXp();
    }

    @Override
    public GuildLevelInfo getLevelInfo(int level) {
        return levels.get(level);
    }

    @Override
    public GuildLevelInfo getLevelInfo(Guild guild) {
        return getLevelInfo(getLevel(guild));
    }

    @Override
    public int getLevel(Guild guild) {
        return getLevel(guild.getExp());
    }

    @Override
    public void addExp(Guild guild, long xp) {
        if (xp <= 0) return;
        long newXp = guild.getExp() + xp;
        setExp(guild, newXp);
    }

    @Override
    public void removeXp(Guild guild, long xp) {
        if (xp <= 0) return;
        long newXp = Math.max(0, guild.getExp() - xp);
        setExp(guild, newXp);
    }

    @Override
    public void setExp(Guild guild, long xp) {
        if (xp < 0) xp = 0;
        guild.setExp(xp);
        guild.setLevel(getLevel(xp));
        plugin.getGuildManager().saveGuild(guild);
    }

    @Override
    public void setLevel(Guild guild, int level) {
        if (level < levels.firstKey()) {
            level = levels.firstKey();
        } else if (!levels.containsKey(level)) {
            throw new IllegalArgumentException("Level " + level + " does not exist");
        }

        long xpForLevel = getExpForLevel(level);
        setExp(guild, xpForLevel);
    }

}
