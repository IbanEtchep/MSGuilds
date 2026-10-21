package com.github.ibanetchep.artisanguilds;

import com.github.ibanetchep.artisanguilds.api.ArtisanGuildsApi;
import com.github.ibanetchep.artisanguilds.command.GuildCommands;
import com.github.ibanetchep.artisanguilds.command.GuildSuggestions;
import com.github.ibanetchep.artisanguilds.command.PlayerLookup;
import com.github.ibanetchep.artisanguilds.menu.GuildsModule;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.service.AllianceService;
import com.github.ibanetchep.artisanguilds.service.BankService;
import com.github.ibanetchep.artisanguilds.service.GuildEconomy;
import com.github.ibanetchep.artisanguilds.service.GuildLeveling;
import com.github.ibanetchep.artisanguilds.chat.ChatListener;
import com.github.ibanetchep.artisanguilds.chat.GuildChat;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import com.github.ibanetchep.artisanguilds.service.GuildService;
import com.github.ibanetchep.artisanguilds.service.RankService;
import com.github.ibanetchep.artisanguilds.service.VaultEconomy;
import com.github.ibanetchep.artisanguilds.storage.CoreDatabase;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository;
import com.github.ibanetchep.artisanguilds.storage.GuildSchema;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import com.github.ibanetchep.artisanguilds.storage.GuildTables;
import com.github.ibanetchep.artisanguilds.storage.LegacyImport;
import com.github.ibanetchep.artisanguilds.sync.GuildSync;
import net.artisanmc.modules.api.ArtisanAPI;
import net.artisanmc.modules.api.network.NetworkApi;
import net.artisanmc.modules.api.translation.MessageBundle;
import net.artisanmc.modules.api.translation.MessageBundleDeclaration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Entry point. Artisan is required: it lends the database, and will serve the commands, menus
 * and messages. Without a compatible Artisan, or with guilds it cannot import, the plugin
 * disables itself and says why.
 */
public final class ArtisanGuildsPlugin extends JavaPlugin {

    /** Lowest Artisan API this plugin compiles against: {@code getNetwork} (2.13.0), argument suggestions (2.14.0). */
    public static final String MIN_API_VERSION = "2.14.0";

    /** The namespace of the tables: {@code artisan_guilds_*} by default. */
    public static final String TABLES = "guilds";

    private @Nullable ArtisanAPI api;
    private @Nullable Guilds guilds;
    private @Nullable ExecutorService database;

    @Override
    public void onEnable() {
        api = resolveArtisan();
        if (api == null) {
            disable();
            return;
        }

        try {
            DataSource dataSource = api.getDatabase().dataSource(this);
            GuildTables tables = new GuildTables(api.getDatabase().tablePrefix(TABLES));
            GuildSchema.Result schema = new GuildSchema(dataSource, api.getDatabase().getDialect(), tables).migrate();
            if (schema.created()) getLogger().info("Guild tables created (" + tables.prefix() + "*).");
            if (schema.upgraded()) getLogger().info("Guild tables upgraded from version " + schema.fromVersion()
                    + " to " + schema.toVersion() + ".");
            importLegacy(dataSource, tables);
            guilds = start(api, dataSource, tables);
        } catch (LegacyImport.LegacySchemaException e) {
            getLogger().severe("ArtisanGuilds is disabled: " + e.getMessage());
            disable();
            return;
        } catch (SQLException | IllegalStateException | IllegalArgumentException e) {
            getLogger().log(Level.SEVERE, "The database is not available, ArtisanGuilds is disabled. "
                    + "Check plugins/Artisan/environment.yml.", e);
            disable();
            return;
        }

        getLogger().info(guilds.registry().all().size() + " guilds loaded (Artisan API " + api.getApiVersion() + ").");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (api != null && api.getModules().has(GuildsModule.ID)) api.getModules().unregister(GuildsModule.ID);
        if (database != null) {
            // Queued writes finish before the pool closes (Artisan waits 5 seconds).
            database.shutdown();
            try {
                if (!database.awaitTermination(4, TimeUnit.SECONDS)) {
                    getLogger().warning("Some guild changes were still being saved at shutdown.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        database = null;
        guilds = null;
        api = null;
    }

    /** The API other plugins use; null while ArtisanGuilds is disabled. Same as {@link ArtisanGuildsApi#get()}. */
    public static @Nullable ArtisanGuildsApi api() {
        return ArtisanGuildsApi.get();
    }

    /** The running guilds, null while disabled: for the commands and menus. */
    public @Nullable Guilds guilds() {
        return guilds;
    }

    /** Reads the guilds, wires the services and serves {@link ArtisanGuildsApi}. */
    private Guilds start(ArtisanAPI api, DataSource dataSource, GuildTables tables) throws SQLException {
        copyLegacyFiles();
        saveDefaultConfig();
        reloadConfig();

        NetworkApi network = api.getNetwork();
        var scheduler = api.getScheduler().forPlugin(this);
        PaperPlatform platform = new PaperPlatform(scheduler, network);
        GuildData.online = platform::isOnline;

        database = Executors.newSingleThreadExecutor(r -> new Thread(r, "ArtisanGuilds-database"));
        GuildRepository repository = new GuildRepository(dataSource, tables);
        GuildStore store = new GuildStore(repository, database, network, getLogger());
        GuildRegistry registry = new GuildRegistry();
        registry.load(repository.loadAll());

        MessageBundle bundle = api.getTranslations().register(MessageBundleDeclaration.files(this, TABLES, "en", "fr")
                .withDefaultLang(getConfig().getString("language")));
        GuildMessages messages = new GuildMessages(bundle, api.getText(), platform);
        GuildEconomy economy = getServer().getPluginManager().getPlugin("Vault") != null
                ? new VaultEconomy(api.getText()) : VaultEconomy.NONE;
        GuildLeveling leveling = new GuildLeveling(
                GuildLeveling.read(getConfig().getConfigurationSection("levels"), getLogger()), store);

        GuildSync sync = new GuildSync(registry, repository, database, platform, getLogger());
        sync.subscribe(this, network);

        BankService bank = new BankService(registry, store, economy, messages);
        Guilds running = new Guilds(registry, store, leveling, sync,
                new GuildService(registry, store, leveling, messages, platform, network, sync), bank,
                new RankService(registry, store, messages, api.getText()),
                new AllianceService(registry, store, messages, network, sync));
        getServer().getPluginManager().registerEvents(new ChatListener(registry, store,
                new GuildChat(registry, messages, getLogger()), messages), this);
        getServer().getServicesManager().register(ArtisanGuildsApi.class,
                new GuildsApiImpl(registry, store, bank, leveling), this, ServicePriority.Normal);
        // Removed by Artisan when this plugin disables.
        api.getPlayerCommands().register(new GuildCommands(running, messages, api.getText(),
                PlayerLookup.of(network.getPresence(), getServer()),
                new GuildSuggestions(registry, () -> network.getPresence().getOnline(),
                        id -> getServer().getOfflinePlayer(id).getName()),
                () -> reload(messages, leveling), player -> player.performCommand("lands guild"), getLogger())
                .declaration());
        // The ranks and disband menus those commands open, with their data sources and actions.
        try {
            api.getModules().register(new GuildsModule(running, messages, getLogger()));
        } catch (RuntimeException e) {
            getLogger().log(Level.WARNING, "The guild menus are not available", e);
        }
        return running;
    }

    /** {@code /guild reload}: config.yml's levels and the lang files, read again. */
    private void reload(GuildMessages messages, GuildLeveling leveling) {
        reloadConfig();
        messages.reload();
        try {
            leveling.replace(GuildLeveling.read(getConfig().getConfigurationSection("levels"), getLogger()));
        } catch (IllegalStateException e) {
            getLogger().warning("config.yml defines no level: the previous levels stay.");
        }
    }

    /**
     * MSGuilds' config.yml and edited lang files, copied once into this plugin's folder when it
     * has none: the levels and messages a server set keep applying.
     */
    private void copyLegacyFiles() {
        Path legacy = getDataFolder().toPath().resolveSibling("MSGuilds");
        Map<String, String> files = Map.of("config.yml", "config.yml",
                "lang/en_EN.yml", "lang/en.yml", "lang/fr_FR.yml", "lang/fr.yml");
        files.forEach((from, to) -> {
            Path source = legacy.resolve(from);
            Path target = getDataFolder().toPath().resolve(to);
            if (!Files.isRegularFile(source) || Files.exists(target)) return;
            try {
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
                getLogger().info("Copied plugins/MSGuilds/" + from + " to plugins/" + getName() + "/" + to + ".");
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Could not copy plugins/MSGuilds/" + from, e);
            }
        });
    }

    /**
     * The guilds of MSGuilds come from Core's database when Core is enabled, else from Artisan's
     * own database when MSGuilds' tables are there. Once only; an error stops the start, so that
     * players never play on an empty copy.
     */
    private void importLegacy(DataSource target, GuildTables tables) throws SQLException {
        DataSource core = CoreDatabase.find(getServer().getPluginManager().getPlugin("Core"));
        DataSource source = core != null ? core : target;
        String label = core != null ? "the Core plugin's database" : "Artisan's database";
        LegacyImport.Result result = new LegacyImport(tables).run(source, label, target);
        switch (result.outcome()) {
            case IMPORTED -> getLogger().info("MSGuilds data imported from " + label + ": " + result.counts()
                    + ". MSGuilds' tables were left as they are.");
            case TARGET_NOT_EMPTY -> getLogger().warning("MSGuilds tables found in " + label + ", but ArtisanGuilds "
                    + "already holds guilds: nothing imported. Empty " + tables.prefix() + "* to import them.");
            case NO_SOURCE, ALREADY_IMPORTED -> { }
        }
    }

    private void disable() {
        getServer().getPluginManager().disablePlugin(this);
    }

    private @Nullable ArtisanAPI resolveArtisan() {
        try {
            ArtisanAPI found = getServer().getServicesManager().load(ArtisanAPI.class);
            if (found == null) {
                getLogger().severe("Artisan is required but its API is not available. "
                        + "Install Artisan (API " + MIN_API_VERSION + "+) next to ArtisanGuilds.");
                return null;
            }
            if (!found.isCompatibleWith(MIN_API_VERSION)) {
                getLogger().severe("Artisan API " + found.getApiVersion() + " is too old: ArtisanGuilds needs "
                        + MIN_API_VERSION + "+ (same major). Update Artisan.");
                return null;
            }
            return found;
        } catch (LinkageError e) {
            getLogger().log(Level.SEVERE, "Installed Artisan is incompatible: ArtisanGuilds needs API "
                    + MIN_API_VERSION + "+.", e);
            return null;
        }
    }
}
