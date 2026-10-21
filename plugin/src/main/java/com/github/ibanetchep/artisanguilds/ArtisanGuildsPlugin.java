package com.github.ibanetchep.artisanguilds;

import com.github.ibanetchep.artisanguilds.storage.CoreDatabase;
import com.github.ibanetchep.artisanguilds.storage.GuildSchema;
import com.github.ibanetchep.artisanguilds.storage.GuildTables;
import com.github.ibanetchep.artisanguilds.storage.LegacyImport;
import net.artisanmc.modules.api.ArtisanAPI;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Entry point. Artisan is required: it lends the database, and will serve the commands, menus
 * and messages. Without a compatible Artisan, or with guilds it cannot import, the plugin
 * disables itself and says why.
 */
public final class ArtisanGuildsPlugin extends JavaPlugin {

    /** Lowest Artisan API this plugin compiles against: {@code tablePrefix} (2.9.0). */
    public static final String MIN_API_VERSION = "2.9.0";

    /** The namespace of the tables: {@code artisan_guilds_*} by default. */
    public static final String TABLES = "guilds";

    private @Nullable ArtisanAPI api;

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

        getLogger().info("Enabled (Artisan API " + api.getApiVersion() + ").");
    }

    @Override
    public void onDisable() {
        api = null;
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
