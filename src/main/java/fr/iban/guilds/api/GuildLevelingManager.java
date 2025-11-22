package fr.iban.guilds.api;

import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildLevelInfo;

public interface GuildLevelingManager {

    long getExpForLevel(int level);
    int getLevel(long xp);
    GuildLevelInfo getLevelInfo(int level);
    GuildLevelInfo getLevelInfo(Guild guild);
    int getLevel(Guild guild);
    void addExp(Guild guild, long xp);
    void removeXp(Guild guild, long xp);
    void setExp(Guild guild, long xp);
    void setLevel(Guild guild, int level);

}
