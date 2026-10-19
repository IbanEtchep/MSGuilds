package com.github.ibanetchep.artisanguilds;

import com.github.ibanetchep.artisanguilds.storage.GuildSchema;
import net.artisanmc.modules.api.ArtisanAPI;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Entry point. Artisan is required: it lends the database, and will serve the commands, menus
 * and messages. Without a compatible Artisan, or with tables it cannot take over, the plugin
 * disables itself and says why.
 */
public final class ArtisanGuildsPlugin extends JavaPlugin {

    /** Lowest Artisan API this plugin compiles against: {@code moduleFolder} (2.8.0). */
    public static final String MIN_API_VERSION = "2.8.0";

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
            GuildSchema.Result result = new GuildSchema(dataSource, api.getDatabase().getDialect()).migrate();
            logSchema(result);
        } catch (GuildSchema.LegacySchemaException e) {
            getLogger().severe("ArtisanGuilds is disabled: " + e.getMessage());
            disable();
            return;
        } catch (SQLException | IllegalStateException e) {
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

    private void logSchema(GuildSchema.Result result) {
        switch (result.outcome()) {
            case CREATED -> getLogger().info("Guild tables created (schema version " + result.toVersion() + ").");
            case ADOPTED -> getLogger().info("MSGuilds tables taken over as they are"
                    + (result.addedColumns().isEmpty() ? "" : ", columns added: " + result.addedColumns())
                    + " (schema version " + result.toVersion() + ").");
            case UPGRADED -> getLogger().info("Guild tables upgraded from version " + result.fromVersion()
                    + " to " + result.toVersion() + ".");
            case UP_TO_DATE -> { }
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
