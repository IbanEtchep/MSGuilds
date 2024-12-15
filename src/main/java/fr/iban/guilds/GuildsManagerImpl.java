package fr.iban.guilds;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.guilds.api.GuildManager;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildPlayer;
import fr.iban.guilds.model.GuildRank;
import fr.iban.guilds.model.dto.GuildPlayerDTO;
import fr.iban.guilds.storage.SqlStorage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GuildsManagerImpl implements GuildManager {

    private final GuildsPlugin plugin;
    private final CoreBukkitPlugin corePlugin;
    private final SqlStorage storage;
    private final Map<UUID, Guild> guilds = new HashMap<>();

    public GuildsManagerImpl(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.storage = new SqlStorage();
        this.corePlugin = CoreBukkitPlugin.getInstance();
        load();
    }

    public Map<UUID, Guild> getGuilds() {
        return guilds;
    }

    @Override
    public @Nullable Guild getGuildByPlayerId(UUID uuid) {
        return guilds.values().stream().filter(guild -> guild.getMember(uuid) != null).findFirst().orElse(null);
    }

    @Override
    public @Nullable Guild getGuildByPlayer(Player player) {
        return getGuildByPlayerId(player.getUniqueId());
    }

    @Override
    public @Nullable Guild getGuildByName(String name) {
        return guilds.values().stream().filter(guild -> guild.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    @Override
    public @Nullable Guild getGuildById(UUID uuid) {
        return guilds.get(uuid);
    }

    @Override
    public @Nullable GuildPlayer getGuildPlayer(UUID uuid) {
        Guild guild = getGuildByPlayerId(uuid);
        return guild == null ? null : guild.getMember(uuid);
    }

    @Override
    public List<String> getGuildNames() {
        return guilds.values().stream().map(Guild::getName).toList();
    }

    @Override
    public List<Guild> getOnlineGuilds() {
        return guilds.values().stream().filter(guild -> !guild.getOnlinePlayers().isEmpty())
                .sorted(Comparator.comparingInt(Guild::getOnlinePlayerAmount).reversed()).toList();
    }

    @Override
    public void saveGuild(Guild guild) {
        guilds.put(guild.getId(), guild);

        plugin.runAsyncQueued(() -> {
            storage.saveGuild(guild);
            syncGuild(guild.getId());
        });
    }

    @Override
    public void savePlayer(GuildPlayer guildPlayer) {
        plugin.runAsyncQueued(() -> {
            storage.saveGuildPlayer(guildPlayer);
            syncGuildPlayer(guildPlayer.getUuid());
        });
    }

    @Override
    public void deleteGuild(Guild guild) {
        UUID guildID = guild.getId();

        guilds.remove(guildID);

        plugin.runAsyncQueued(() -> {
            storage.deleteGuild(guildID);
            syncGuild(guildID);
        });
    }

    @Override
    public void deletePlayer(GuildPlayer guildPlayer) {
        UUID uuid = guildPlayer.getUuid();

        plugin.runAsyncQueued(() -> {
            storage.deleteGuildPlayer(uuid);
            syncGuildPlayer(uuid);
        });
    }

    @Override
    public void addLog(Guild guild, String log) {
        guild.invalidateLogs();
        plugin.runAsyncQueued(() -> storage.addLog(guild, log));
    }

    @Override
    public CompletableFuture<List<String>> getLogsAsync(Guild guild) {
        return CompletableFuture.supplyAsync(() -> {
            if (guild.getCachedLogs() == null) {
                guild.setCachedLogs(storage.getLogs(guild.getId()));
            }
            return guild.getCachedLogs();
        });
    }

    /*
    SYNC
     */
    public void syncGuild(UUID guildID) {
        corePlugin.getMessagingManager().sendMessage(GuildsPlugin.GUILD_SYNC_CHANNEL, guildID.toString());
    }

    public void syncGuildPlayer(UUID uuid) {
        corePlugin.getMessagingManager().sendMessage(GuildsPlugin.GUILD_PLAYER_SYNC_CHANNEL, uuid.toString());
    }

    @Override
    public void reloadGuildFromDB(UUID guildId) {
        Guild oldGuild = guilds.get(guildId);
        Guild newGuild = storage.getGuild(guildId);

        if (oldGuild != null && newGuild == null) {
            //Suppression d'une guilde
            guilds.remove(guildId);
        } else {
            //Ajout ou maj à jour de la guilde
            guilds.put(guildId, newGuild);
            for (GuildPlayerDTO guildPlayerDTO : storage.getGuildPlayerDTOs(guildId)) {
                loadGuildPlayerFromDTO(guildPlayerDTO);
            }

            for (UUID uuid : storage.getAlliances(newGuild)) {
                Guild alliance = guilds.get(uuid);
                if (alliance != null) {
                    newGuild.getAlliances().add(alliance);
                }
            }
        }
    }

    @Override
    public void reloadGuildPlayerFromDB(UUID uuid) {
        GuildPlayer oldGuildPlayer = getGuildPlayer(uuid);

        GuildPlayerDTO updatedGuildPlayerDTO = storage.getGuildPlayerDto(uuid);
        if (oldGuildPlayer != null && updatedGuildPlayerDTO == null) {
            //Le joueur a quitté la guilde
            Guild guild = getGuildById(oldGuildPlayer.getGuild().getId());
            if (guild != null) {
                guild.getMembers().remove(uuid);
            }
        } else {
            loadGuildPlayerFromDTO(updatedGuildPlayerDTO);
        }
    }

    /*
    LOAD
     */

    private void loadGuilds() {
        long start = System.currentTimeMillis();
        for (Guild guild : storage.getGuilds()) {
            guilds.put(guild.getId(), guild);
        }

        for (Guild guild : guilds.values()) {
            for (UUID uuid : storage.getAlliances(guild)) {
                Guild alliance = guilds.get(uuid);
                if (alliance != null) {
                    guild.getAlliances().add(alliance);
                }
            }
        }

        plugin.getLogger().info(guilds.size() + " guildes chargées en " + (System.currentTimeMillis() - start) + "ms.");
    }

    private void loadGuildPlayers() {
        long start = System.currentTimeMillis();
        List<GuildPlayerDTO> guildPlayers = storage.getGuildPlayerDTOs();
        for (GuildPlayerDTO guildPlayerDTO : guildPlayers) {
            loadGuildPlayerFromDTO(guildPlayerDTO);
        }
        plugin.getLogger().info(guildPlayers.size() + " joueurs de guilde chargées en " + (System.currentTimeMillis() - start) + "ms.");
    }

    public void load() {
        guilds.clear();
        loadGuilds();
        loadGuildPlayers();
    }

    private void loadGuildPlayerFromDTO(GuildPlayerDTO dto) {
        Guild guild = getGuildById(dto.guildId());

        if (guild == null) {
            throw new IllegalArgumentException("Guild not found for player " + dto.uuid());
        }

        GuildRank rank = guild.getRank(dto.rank());
        GuildPlayer guildPlayer = new GuildPlayer(dto.uuid(), guild, rank, dto.chatMode());

        guild.getMembers().put(dto.uuid(), guildPlayer);
    }
}