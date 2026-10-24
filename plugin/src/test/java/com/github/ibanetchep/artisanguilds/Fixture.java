package com.github.ibanetchep.artisanguilds;

import com.github.ibanetchep.artisanguilds.api.GuildLevelInfo;
import com.github.ibanetchep.artisanguilds.chest.GuildChestService;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.service.BankService;
import com.github.ibanetchep.artisanguilds.service.GuildEconomy;
import com.github.ibanetchep.artisanguilds.service.GuildLeveling;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import com.github.ibanetchep.artisanguilds.service.GuildService;
import com.github.ibanetchep.artisanguilds.service.Msg;
import com.github.ibanetchep.artisanguilds.storage.GuildChestRepository;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository;
import com.github.ibanetchep.artisanguilds.storage.GuildSchema;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import com.github.ibanetchep.artisanguilds.storage.GuildTables;
import com.github.ibanetchep.artisanguilds.sync.GuildSync;
import net.artisanmc.modules.api.database.DatabaseDialect;
import net.artisanmc.modules.api.network.NetworkApi;
import net.artisanmc.modules.api.network.NetworkLocation;
import net.artisanmc.modules.api.network.NetworkMessage;
import net.artisanmc.modules.api.network.NetworkMessageHandler;
import net.artisanmc.modules.api.network.NetworkPresence;
import net.artisanmc.modules.api.network.NetworkServer;
import net.artisanmc.modules.api.network.NetworkSubscription;
import net.artisanmc.modules.api.network.TeleportResult;
import net.artisanmc.modules.api.text.TextApi;
import net.artisanmc.modules.api.translation.MessageBundle;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Everything wired as on a server, on an H2 database, with the writes run where they are asked
 * and the server faked: events, messages and network messages are recorded.
 */
public final class Fixture {

    public static final GuildTables TABLES = new GuildTables("artisan_guilds_");

    public final DataSource dataSource;
    public final GuildRepository repository;
    public final GuildRegistry registry = new GuildRegistry();
    public final FakePlatform platform = new FakePlatform();
    public final RecordingNetwork network = new RecordingNetwork();
    public final RecordingBundle bundle = new RecordingBundle();
    public final FakeEconomy economy = new FakeEconomy();
    public final GuildStore store;
    public final GuildSync sync;
    public final GuildLeveling leveling;
    public final GuildMessages messages;
    public final GuildService guilds;
    public final BankService bank;
    public final GuildChestService chest;
    /** What the disband check finds in every guild's chest. */
    public boolean chestEmpty = true;

    public Fixture() throws SQLException {
        this(h2());
    }

    /** Another server on the same database. */
    public Fixture(DataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
        new GuildSchema(dataSource, DatabaseDialect.H2, TABLES).migrate();
        repository = new GuildRepository(dataSource, TABLES);
        Logger logger = Logger.getLogger("ArtisanGuildsTest");
        store = new GuildStore(repository, Runnable::run, network, logger);
        sync = new GuildSync(registry, repository, Runnable::run, platform, logger);
        TreeMap<Integer, GuildLevelInfo> levels = new TreeMap<>();
        levels.put(1, new GuildLevelInfo(1, 0, 2));
        levels.put(2, new GuildLevelInfo(2, 100, 5));
        levels.put(3, new GuildLevelInfo(3, 300, 10));
        leveling = new GuildLeveling(levels, store);
        messages = new GuildMessages(bundle, TextApi.NOOP, platform);
        guilds = new GuildService(registry, store, leveling, messages, platform, network, sync, id -> chestEmpty);
        bank = new BankService(registry, store, economy, messages);
        // Disabled: it reads the database only, Paper's items being out of reach here.
        chest = new GuildChestService(registry, leveling, messages, store, new GuildChestRepository(dataSource, TABLES),
                false, logger);
        MemberData.names = id -> "player-" + id.toString().substring(0, 4);
    }

    private static DataSource h2() {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:guilds-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        return h2;
    }

    /** A player online on this server, without permissions. */
    public Player player(String name) {
        UUID id = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn(name);
        platform.players.put(id, player);
        return player;
    }

    public <E extends Event> List<E> events(Class<E> type) {
        return platform.events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public static final class FakePlatform implements GuildPlatform {
        public final Map<UUID, Player> players = new HashMap<>();
        public final List<Event> events = new ArrayList<>();
        public final List<Runnable> later = new ArrayList<>();

        @Override public Player player(UUID id) { return players.get(id); }
        @Override public boolean isOnline(UUID id) { return players.containsKey(id); }
        @Override public void sendRemote(UUID id, String miniMessage) { }
        @Override public void callEvent(Event event) { events.add(event); }
        @Override public void global(Runnable task) { task.run(); }
        @Override public void later(long ticks, Runnable task) { later.add(task); }
    }

    public record Sent(CommandSender to, String key) {}

    /** Keeps what was sent; a message's text is its key. */
    public static final class RecordingBundle implements MessageBundle {
        public final List<Sent> sent = new ArrayList<>();

        public List<String> keysSentTo(CommandSender to) {
            return sent.stream().filter(s -> s.to() == to).map(Sent::key).toList();
        }

        public boolean got(CommandSender to, Msg msg) {
            return keysSentTo(to).contains(msg.key());
        }

        @Override public String getNamespace() { return "guilds"; }
        @Override public Set<String> getLanguages() { return Set.of("en"); }
        @Override public String getDefaultLang() { return "en"; }
        @Override public String langOf(CommandSender viewer) { return "en"; }
        @Override public String find(String lang, String key) { return key; }
        @Override public String raw(String lang, String key) { return key; }
        @Override public String format(String template, Map<String, ?> placeholders) { return template; }
        @Override public void send(CommandSender viewer, String key, Map<String, ?> placeholders) { sent.add(new Sent(viewer, key)); }
        @Override public String legacy(String lang, String key, Map<String, ?> placeholders) { return key; }
        @Override public Component get(String lang, String key, Map<String, ?> placeholders) { return Component.text(key); }
        @Override public void reload() { }
    }

    public record Published(String channel, String payload) {}

    public static final class RecordingNetwork implements NetworkApi {
        public final List<Published> published = new ArrayList<>();
        public final Map<String, NetworkMessageHandler> handlers = new HashMap<>();

        public void deliver(String channel, String payload) {
            handlers.get(channel).handle(new NetworkMessage(channel, "other", payload));
        }

        @Override public String getServerName() { return "here"; }
        @Override public List<NetworkServer> getServers() { return List.of(); }
        @Override public NetworkSubscription subscribe(Plugin owner, String channel, NetworkMessageHandler handler) {
            handlers.put(channel, handler);
            return () -> handlers.remove(channel);
        }
        @Override public void publish(String channel, String payload, boolean includeSelf) { published.add(new Published(channel, payload)); }
        @Override public NetworkPresence getPresence() { return NetworkApi.NOOP.getPresence(); }
        @Override public CompletableFuture<TeleportResult> teleport(Player player, NetworkLocation location, int delaySeconds) {
            return CompletableFuture.completedFuture(TeleportResult.FAILED);
        }
        @Override public CompletableFuture<TeleportResult> connect(Player player, String server, int delaySeconds) {
            return CompletableFuture.completedFuture(TeleportResult.FAILED);
        }
    }

    public static final class FakeEconomy implements GuildEconomy {
        public final Map<Player, Double> balances = new HashMap<>();

        @Override public boolean available() { return true; }
        @Override public double balance(Player player) { return balances.getOrDefault(player, 0.0); }
        @Override public boolean withdraw(Player player, double amount) {
            if (balance(player) < amount) return false;
            balances.put(player, balance(player) - amount);
            return true;
        }
        @Override public void deposit(Player player, double amount) { balances.put(player, balance(player) + amount); }
        @Override public String format(double amount) { return String.valueOf(amount); }
    }
}
