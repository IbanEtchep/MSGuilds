package fr.iban.guilds;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.guilds.enums.ChatMode;
import fr.iban.guilds.enums.Rank;
import fr.iban.guilds.event.GuildCreateEvent;
import fr.iban.guilds.event.GuildDisbandEvent;
import fr.iban.guilds.event.GuildPostDisbandEvent;
import fr.iban.guilds.exception.AlreadyGuildMemberException;
import fr.iban.guilds.exception.GuildAlreadyExistsException;
import fr.iban.guilds.exception.InsufficientPermissionException;
import fr.iban.guilds.exception.NotGuildMemberException;
import fr.iban.guilds.storage.SqlStorage;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuildsManager {

    private final GuildsPlugin plugin;
    private final SqlStorage storage;

    private final Map<UUID, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<UUID, GuildPlayer> guildPlayers = new ConcurrentHashMap<>();

    public GuildsManager(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.storage = new SqlStorage();
        load();
    }

    public Guild getGuild(UUID uuid) {
        GuildPlayer guildPlayer = getGuildPlayer(uuid);
        if (guildPlayer != null) {
            return guilds.get(guildPlayer.getGuildId());
        }
        return null;
    }

    public GuildPlayer getGuildPlayer(UUID uuid) {
        return guildPlayers.get(uuid);
    }

    public boolean hasGuild(UUID uuid) {
        return guildPlayers.containsKey(uuid);
    }

    public void createGuild(String name, UUID owner) throws GuildAlreadyExistsException, AlreadyGuildMemberException {
        if (guilds.values().stream().anyMatch(g -> g.getName().equalsIgnoreCase(name))) {
            throw new GuildAlreadyExistsException("Une guilde existe déjà au nom de " + name + ".");
        }

        if (hasGuild(owner)) {
            throw new AlreadyGuildMemberException("Vous êtes déjà dans une guilde !");
        }

        Guild guild = new Guild(name);
        GuildPlayer guildPlayer = new GuildPlayer(owner, guild.getId(), Rank.OWNER, ChatMode.PUBLIC);
        guild.getMembers().put(owner, guildPlayer);
        storage.saveGuild(guild);
        storage.saveGuildPlayer(guildPlayer);
        guilds.put(guild.getId(), guild);
        guildPlayers.put(owner, guildPlayer);
        Bukkit.getPluginManager().callEvent(new GuildCreateEvent(guild));
    }

    public void toggleChatMode(UUID uuid) throws NotGuildMemberException {
        GuildPlayer guildPlayer = getGuildPlayer(uuid);

        if (guildPlayer == null) {
            throw new NotGuildMemberException("Vous n'avez pas de guilde !");
        }

        if (guildPlayer.getChatMode() == ChatMode.PUBLIC) {
            guildPlayer.setChatMode(ChatMode.GUILD);
        } else {
            guildPlayer.setChatMode(ChatMode.PUBLIC);
        }

        saveGuildPlayerToDB(guildPlayer);
    }

    public void disbandGuild(UUID ownerID) throws NotGuildMemberException, InsufficientPermissionException {
        GuildPlayer guildPlayer = getGuildPlayer(ownerID);

        if (guildPlayer == null) {
            throw new NotGuildMemberException("Vous n'avez pas de guilde !");
        }

        if (guildPlayer.isGranted(Rank.OWNER)) {
            throw new InsufficientPermissionException("Vous devez être fondateur de la guilde pour la dissoudre.");
        }

        Guild guild = getGuild(ownerID);

        GuildDisbandEvent disbandEvent = new GuildDisbandEvent(guild);
        Bukkit.getPluginManager().callEvent(disbandEvent);

        if(!disbandEvent.isCancelled()) {
            guild.getMembers().forEach((uuid, gp) -> {
                gp.sendMessageIfOnline("§cVotre guilde a été dissoute.");
                guildPlayers.remove(uuid);
                deleteGuildPlayerFromDB(uuid);
            });

            guilds.remove(guild.getId());
            deleteGuildFromDB(guildPlayer.getGuildId());
            Bukkit.getPluginManager().callEvent(new GuildPostDisbandEvent(guild));
        }
    }

    public void joinGuild(UUID uuid, UUID guildId) {

    }

    public void quitGuild(GuildPlayer guildPlayer) {
        Guild guild = guilds.get(guildPlayer.getGuildId());
        guild.getMembers().remove(guildPlayer.getUuid());
        guildPlayers.remove(guildPlayer.getUuid());
        deleteGuildPlayerFromDB(guildPlayer.getUuid());
    }

    private void saveGuildToDB(Guild guild) {
        plugin.runAsyncQueued(() -> {
            storage.saveGuild(guild);
            syncGuild(guild.getId());
        });
    }

    private void deleteGuildFromDB(UUID guildID) {
        plugin.runAsyncQueued(() -> {
            storage.deleteGuild(guildID);
            syncGuild(guildID);
        });
    }

    private void saveGuildPlayerToDB(GuildPlayer guildPlayer) {
        plugin.runAsyncQueued(() -> {
            storage.saveGuildPlayer(guildPlayer);
            syncGuildPlayer(guildPlayer.getUuid());
        });
    }

    private void deleteGuildPlayerFromDB(UUID uuid) {
        plugin.runAsyncQueued(() -> {
            storage.deleteGuildPlayer(uuid);
            syncGuildPlayer(uuid);
        });
    }

    /*
    SYNC
     */

    public void syncGuild(UUID guildID) {
        CoreBukkitPlugin core = CoreBukkitPlugin.getInstance();
        core.getMessagingManager().sendMessageAsync(GuildsPlugin.GUILD_SYNC_CHANNEL, guildID.toString());
    }

    public void syncGuildPlayer(UUID uuid) {
        CoreBukkitPlugin core = CoreBukkitPlugin.getInstance();
        core.getMessagingManager().sendMessageAsync(GuildsPlugin.GUILD_PLAYER_SYNC_CHANNEL, uuid.toString());
    }

    public void reloadGuildFromDB(UUID guildId) {
        Guild oldGuild = guilds.get(guildId);
        Guild newGuild = storage.getGuild(guildId);

        if (oldGuild != null && newGuild == null) {
            //Suppression d'une guilde
            for (UUID uuid : oldGuild.getMembers().keySet()) {
                guildPlayers.remove(uuid);
            }
            guilds.remove(guildId);
        } else {
            //Mise à jour de la guilde
            guilds.put(guildId, newGuild);
            for (GuildPlayer guildMember : storage.getGuildMembers(guildId)) {
                newGuild.getMembers().put(guildMember.getUuid(), guildMember);
                guildPlayers.put(guildMember.getUuid(), guildMember);
            }
        }
    }

    public void reloadGuildPlayerFromDB(UUID uuid) {
        GuildPlayer oldGuildPlayer = guildPlayers.get(uuid);
        GuildPlayer newGuildPlayer = storage.getGuildPlayer(uuid);
        if (oldGuildPlayer != null && newGuildPlayer == null) {
            //Le joueur a quitté la guilde
            guilds.get(oldGuildPlayer.getGuildId()).getMembers().remove(uuid);
            guildPlayers.remove(uuid);
        } else {
            //Le joueur a rejoint une guilde
            guildPlayers.put(uuid, newGuildPlayer);
            guilds.get(newGuildPlayer.getGuildId()).getMembers().put(uuid, newGuildPlayer);
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
        plugin.getLogger().info(guilds.size() + " guildes chargées en " + (System.currentTimeMillis() - start) + "ms.");
    }

    private void loadGuildPlayers() {
        long start = System.currentTimeMillis();
        for (GuildPlayer guildPlayer : storage.getGuildPlayers()) {
            UUID uuid = guildPlayer.getUuid();
            guildPlayers.put(uuid, guildPlayer);
            guilds.get(guildPlayer.getGuildId()).getMembers().put(uuid, guildPlayer);
        }
        plugin.getLogger().info(guildPlayers.size() + " joueurs de guilde chargées en " + (System.currentTimeMillis() - start) + "ms.");
    }

    public void load() {
        guilds.clear();
        guildPlayers.clear();
        loadGuilds();
        loadGuildPlayers();
    }

}
