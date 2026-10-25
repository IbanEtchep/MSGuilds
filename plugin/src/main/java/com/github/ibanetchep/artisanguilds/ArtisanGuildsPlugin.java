package com.github.ibanetchep.artisanguilds;

import com.github.ibanetchep.artisanguilds.api.ArtisanGuildsApi;
import com.github.ibanetchep.artisanguilds.chest.GuildChestService;
import com.github.ibanetchep.artisanguilds.command.GuildCommands;
import com.github.ibanetchep.artisanguilds.command.GuildSuggestions;
import com.github.ibanetchep.artisanguilds.command.PlayerLookup;
import com.github.ibanetchep.artisanguilds.placeholder.GuildPlaceholders;
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
import com.github.ibanetchep.artisanguilds.storage.GuildChestRepository;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository;
import com.github.ibanetchep.artisanguilds.storage.GuildSchema;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import com.github.ibanetchep.artisanguilds.storage.GuildTables;
import com.github.ibanetchep.artisanguilds.storage.LegacyImport;
import com.github.ibanetchep.artisanguilds.sync.GuildSync;
import net.artisanmc.modules.api.ArtisanAPI;
import net.artisanmc.modules.api.network.NetworkApi;
import net.artisanmc.modules.api.playercommand.PlayerCommandDeclaration;
import net.artisanmc.modules.api.translation.MessageBundle;
import net.artisanmc.modules.api.translation.MessageBundleDeclaration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.io.File;
import java.util.NavigableMap;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import org.bukkit.configuration.ConfigurationSection;
import com.github.ibanetchep.artisanguilds.api.GuildLevelInfo;
import com.github.ibanetchep.artisanguilds.settings.GuildSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
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

    /**
     * Lowest Artisan API this plugin compiles against: {@code getNetwork} (2.13.0), argument
     * suggestions (2.14.0), messages in the module's folder (2.17.0).
     */
    public static final String MIN_API_VERSION = "2.17.0";

    /** The namespace of the tables: {@code artisan_guilds_*} by default. */
    public static final String TABLES = "guilds";

    private @Nullable ArtisanAPI api;
    /** plugins/Artisan/modules/guilds/, null when Artisan gives none. */
    private @Nullable File moduleFolder;
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
        // Its open pages are written before the writer stops.
        if (guilds != null) guilds.chest().shutdown();
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
        File moduleFolder = moduleFolder(api);
        this.moduleFolder = moduleFolder;
        copyLegacyFiles(moduleFolder);
        saveDefaultConfig();
        reloadConfig();
        if (moduleFolder != null) moveSettings(moduleFolder);
        ConfigurationSection settings = settings();

        NetworkApi network = api.getNetwork();
        var scheduler = api.getScheduler().forPlugin(this);
        PaperPlatform platform = new PaperPlatform(scheduler, network);
        GuildData.online = platform::isOnline;

        database = Executors.newSingleThreadExecutor(r -> new Thread(r, "ArtisanGuilds-database"));
        GuildRepository repository = new GuildRepository(dataSource, tables);
        GuildStore store = new GuildStore(repository, database, network, getLogger());
        GuildRegistry registry = new GuildRegistry();
        for (GuildData guild : registry.load(repository.loadAll())) {
            getLogger().warning("Guild " + guild.getName() + " had no rank: the default ranks were given to it.");
            repository.saveGuild(guild);
            for (UUID id : guild.getMembers().keySet()) repository.saveMember(guild.getMember(id));
        }

        // The edited lang files of plugins/ArtisanGuilds/lang/ move to the module's folder once.
        MessageBundleDeclaration messageFiles = MessageBundleDeclaration.files(this, TABLES, "en", "fr")
                .withDefaultLang(settings.getString("language"));
        MessageBundle bundle = api.getTranslations().register(
                moduleFolder != null ? messageFiles.inFolder(moduleFolder) : messageFiles);
        GuildMessages messages = new GuildMessages(bundle, api.getText(), platform);
        GuildEconomy economy = getServer().getPluginManager().getPlugin("Vault") != null
                ? new VaultEconomy(api.getText()) : VaultEconomy.NONE;
        GuildLeveling leveling = new GuildLeveling(
                GuildLeveling.read(settings.getConfigurationSection("levels"), getLogger()), store);

        GuildSync sync = new GuildSync(registry, repository, database, platform, getLogger());
        sync.subscribe(this, network);

        BankService bank = new BankService(registry, store, economy, messages);
        GuildChestService chest = new GuildChestService(registry, leveling, messages, store,
                new GuildChestRepository(dataSource, tables), getConfig().getBoolean("chest.enabled", true), getLogger());
        getServer().getPluginManager().registerEvents(chest, this);
        Guilds running = new Guilds(registry, store, leveling, sync,
                new GuildService(registry, store, leveling, messages, platform, network, sync, chest::isEmpty), bank,
                new RankService(registry, store, messages, api.getText()),
                new AllianceService(registry, store, messages, network, sync), chest);
        getServer().getPluginManager().registerEvents(new ChatListener(registry, store,
                new GuildChat(registry, messages, getLogger()), messages), this);
        getServer().getServicesManager().register(ArtisanGuildsApi.class,
                new GuildsApiImpl(registry, store, bank, leveling), this, ServicePriority.Normal);
        PlayerCommandDeclaration commands = new GuildCommands(running, messages, api.getText(),
                PlayerLookup.of(network.getPresence(), getServer()),
                new GuildSuggestions(registry, () -> network.getPresence().getOnline(),
                        id -> getServer().getOfflinePlayer(id).getName()),
                () -> reload(messages, leveling), player -> player.performCommand("lands guild"),
                player -> api.getMenus().open(player, GuildCommands.MAIN_MENU), getLogger())
                .declaration();
        // %msguilds_…% as MSGuilds named them, for PlaceholderAPI too; removed by Artisan on disable.
        try {
            api.getPlaceholders().register(GuildPlaceholders.NAMESPACE, new GuildPlaceholders(registry, messages), true);
        } catch (IllegalStateException e) {
            getLogger().warning("%" + GuildPlaceholders.NAMESPACE + "_…% not registered: " + e.getMessage()
                    + " (is MSGuilds still installed?)");
        }
        // The guild, members, ranks and disband menus those commands open, with their data sources and actions.
        // /guild too: registered by the module, it is a file of the module's folder, edited in the editor.
        try {
            api.getModules().register(new GuildsModule(running, messages, commands, () -> reloadLevels(leveling), getLogger()));
        } catch (RuntimeException e) {
            getLogger().log(Level.WARNING, "The guild menus are not available", e);
        }
        // Without the module, /guild still works, as declared: Artisan removes it when this plugin disables.
        if (api.getPlayerCommands().find(GuildCommands.ROOT) == null) api.getPlayerCommands().register(commands);
        return running;
    }

    /** {@code /guild reload}: config.yml, the levels and the lang files, read again. */
    private void reload(GuildMessages messages, GuildLeveling leveling) {
        reloadConfig();
        messages.reload();
        reloadLevels(leveling);
    }

    /** The levels of data/settings.yml, read again: {@code /guild reload} and each Deploy. */
    private void reloadLevels(GuildLeveling leveling) {
        NavigableMap<Integer, GuildLevelInfo> levels =
                GuildLeveling.read(settings().getConfigurationSection("levels"), getLogger());
        if (levels.isEmpty()) {
            getLogger().warning(GuildSettings.PATH + " defines no level: the previous levels stay.");
            return;
        }
        leveling.replace(levels);
    }

    /** data/settings.yml of the module's folder, else the jar's. */
    private ConfigurationSection settings() {
        return GuildSettings.load(moduleFolder, this::getResource, getLogger());
    }

    /**
     * The language and the levels of a config.yml from before, moved once to the module's
     * data/settings.yml; config.yml is then written again with what stays on this server.
     */
    private void moveSettings(File folder) {
        boolean legacy = getConfig().isConfigurationSection("levels");
        if (!GuildSettings.ensure(folder, legacy ? getConfig() : null, this::getResource, getLogger())) {
            if (legacy) getLogger().warning("config.yml still holds levels, but plugins/Artisan/modules/"
                    + GuildsModule.FOLDER + "/" + GuildSettings.PATH + " is the one read: remove them from config.yml.");
            return;
        }
        boolean chest = getConfig().getBoolean("chest.enabled", true);
        try (InputStream in = getResource("config.yml")) {
            if (in == null) return;
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!chest) text = text.replace("  enabled: true", "  enabled: false");
            Files.writeString(new File(getDataFolder(), "config.yml").toPath(), text, StandardCharsets.UTF_8);
            reloadConfig();
            getLogger().info("The language and the levels of config.yml moved to plugins/Artisan/modules/"
                    + GuildsModule.FOLDER + "/" + GuildSettings.PATH + ", edited in the Artisan editor.");
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not write config.yml again: its levels are no longer read", e);
        }
    }

    /**
     * plugins/Artisan/modules/guilds/, where the messages live beside the menus; null when Artisan
     * gives none, the messages then staying in this plugin's folder.
     */
    private @Nullable File moduleFolder(ArtisanAPI api) {
        try {
            return api.getProjectRoots().moduleFolder(GuildsModule.FOLDER, GuildsModule.ID, null);
        } catch (RuntimeException e) {
            getLogger().log(Level.WARNING, "No folder plugins/Artisan/modules/" + GuildsModule.FOLDER
                    + ": the messages stay in plugins/" + getName() + "/lang/", e);
            return null;
        }
    }

    /**
     * MSGuilds' config.yml and edited lang files, copied once when this plugin has none: the
     * levels and messages a server set keep applying. A lang file goes to the data folder, from
     * where the bundle moves it to the module's folder; one the module's folder holds is not
     * copied again.
     */
    private void copyLegacyFiles(@Nullable File moduleFolder) {
        Path legacy = getDataFolder().toPath().resolveSibling("MSGuilds");
        Map<String, String> files = Map.of("config.yml", "config.yml",
                "lang/en_EN.yml", "lang/en.yml", "lang/fr_FR.yml", "lang/fr.yml");
        files.forEach((from, to) -> {
            Path source = legacy.resolve(from);
            Path target = getDataFolder().toPath().resolve(to);
            boolean moved = moduleFolder != null && to.startsWith("lang/") && (Files.exists(moduleFolder.toPath().resolve(to))
                    || Files.exists(moduleFolder.toPath().resolve(to.replace(".yml", ".yaml"))));
            if (!Files.isRegularFile(source) || Files.exists(target) || moved) return;
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
