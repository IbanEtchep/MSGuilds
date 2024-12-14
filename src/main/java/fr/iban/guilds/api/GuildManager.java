package fr.iban.guilds.api;

import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface GuildManager {

    @Nullable Guild getGuildByPlayerId(UUID uuid);
    @Nullable Guild getGuildByPlayer(Player player);
    @Nullable Guild getGuildByName(String name);
    @Nullable Guild getGuildById(UUID uuid);
    @Nullable GuildPlayer getGuildPlayer(UUID uuid);
    List<String> getGuildNames();
    List<Guild> getOnlineGuilds();
    void saveGuild(Guild guild);
    void savePlayer(GuildPlayer guildPlayer);
    void deleteGuild(Guild guild);
    void deletePlayer(GuildPlayer guildPlayer);
    void addLog(Guild guild, String log);
    CompletableFuture<List<String>> getLogsAsync(Guild guild);
    void reloadGuildFromDB(UUID guildId);
    void reloadGuildPlayerFromDB(UUID uuid);
}
