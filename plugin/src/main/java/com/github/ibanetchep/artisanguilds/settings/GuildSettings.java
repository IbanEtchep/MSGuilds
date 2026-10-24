package com.github.ibanetchep.artisanguilds.settings;

import com.github.ibanetchep.artisanguilds.api.GuildLevelInfo;
import com.github.ibanetchep.artisanguilds.service.GuildLeveling;
import net.artisanmc.modules.api.model.ModelDeclaration;
import net.artisanmc.modules.api.model.ModelField;
import net.artisanmc.modules.api.model.ModelFieldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The guild settings shared by the network's content: {@code data/settings.yml} of the module's
 * folder, typed by the model {@code guilds:settings} and edited in the editor's Settings screen.
 * The language and the levels lived in config.yml before; what belongs to one server
 * ({@code chest.enabled}) stays there.
 */
public final class GuildSettings {

    public static final String PATH = "data/settings.yml";
    public static final String MODEL_PATH = "models/settings.yml";
    public static final String MODEL = "guilds:settings";
    public static final String SCREEN = "guilds:settings";
    /** The table the screen edits: {@link #PATH} without its extension. */
    public static final String TABLE = "data/settings";

    private GuildSettings() {}

    /**
     * Writes {@link #PATH} and {@link #MODEL_PATH} when the folder lacks them. The settings come
     * from {@code legacy} (the config.yml that still holds {@code levels}), else from the jar's
     * defaults. Returns true when the settings were taken from {@code legacy}.
     */
    public static boolean ensure(File folder, @Nullable ConfigurationSection legacy,
                                 Function<String, @Nullable InputStream> resource, Logger logger) {
        try {
            if (!exists(folder, MODEL_PATH)) write(folder, MODEL_PATH, read(resource, "module/" + MODEL_PATH));
            if (exists(folder, PATH)) return false;
            if (legacy != null && legacy.isConfigurationSection("levels")) {
                write(folder, PATH, fromLegacy(legacy, logger));
                return true;
            }
            write(folder, PATH, read(resource, "module/" + PATH));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not write the guild settings in " + folder.getPath(), e);
        }
        return false;
    }

    /**
     * {@link #PATH} as read; the jar's defaults without the module's folder, or when the file is
     * missing or broken (named in the log).
     */
    public static YamlConfiguration load(@Nullable File folder, Function<String, @Nullable InputStream> resource,
                                         Logger logger) {
        File file = folder != null ? file(folder, PATH) : null;
        if (file != null && file.isFile()) {
            try {
                return parse(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            } catch (IOException | InvalidConfigurationException e) {
                logger.warning(file.getPath() + " cannot be read, the default guild settings apply: " + e.getMessage());
            }
        }
        try {
            return parse(read(resource, "module/" + PATH));
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalStateException("The jar's guild settings cannot be read", e);
        }
    }

    private static YamlConfiguration parse(String yaml) throws InvalidConfigurationException {
        YamlConfiguration settings = new YamlConfiguration();
        settings.loadFromString(yaml);
        return settings;
    }

    /** The YAML of the settings a config.yml held: its language and its levels, each one spelled out. */
    static String fromLegacy(ConfigurationSection legacy, Logger logger) {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("language", legacy.getString("language", "en"));
        NavigableMap<Integer, GuildLevelInfo> levels = GuildLeveling.read(legacy.getConfigurationSection("levels"), logger);
        for (Map.Entry<Integer, GuildLevelInfo> e : levels.entrySet()) {
            String key = "levels." + e.getKey();
            settings.set(key + ".xp", e.getValue().requiredXp());
            settings.set(key + ".max-members", e.getValue().maxMembers());
            settings.set(key + ".chest-rows", e.getValue().chestRows());
            settings.set(key + ".chest-pages", e.getValue().chestPages());
        }
        return "# Moved from plugins/ArtisanGuilds/config.yml. Edited in the Artisan editor (the guilds\n"
                + "# folder, Settings).\n" + settings.saveToString();
    }

    /** The shape of {@link #PATH}, declared to the editor so the Settings screen renders a form. */
    public static ModelDeclaration declaration() {
        ModelField level = new ModelField("levels", ModelFieldType.map(ModelFieldType.object(List.of(
                field("xp", "XP needed", "The level is the highest one whose XP the guild has reached.", 0),
                field("max-members", "Members at most", null, 5),
                field("chest-rows", "Chest rows", "1 to 6.", 0).withRange(0d, 6d),
                field("chest-pages", "Chest pages", "0: no guild chest at this level.", 0)))))
                .withLabel("Levels")
                .withHelp("Keyed by level number, from 0. Earned through MSQuests rankup quests or "
                        + "/guild admin setlevel.");
        ModelField language = new ModelField("language", ModelFieldType.string())
                .withLabel("Language")
                .withHelp("The messages' language when a player's is not offered (en, fr). Applies at the next start.")
                .withDefault("en");
        return new ModelDeclaration(MODEL, "id", List.of(language, level)).withName("Guild settings");
    }

    private static ModelField field(String name, String label, @Nullable String help, int defaultValue) {
        return new ModelField(name, ModelFieldType.integer()).withLabel(label).withHelp(help)
                .withDefault(defaultValue).withRange(0d, null);
    }

    private static boolean exists(File folder, String path) {
        return file(folder, path).isFile();
    }

    /** The {@code .yml} file, or its {@code .yaml} twin when only that one exists. */
    private static File file(File folder, String path) {
        File yml = new File(folder, path);
        File yaml = new File(folder, path.replace(".yml", ".yaml"));
        return !yml.isFile() && yaml.isFile() ? yaml : yml;
    }

    private static String read(Function<String, @Nullable InputStream> resource, String path) throws IOException {
        try (InputStream in = resource.apply(path)) {
            if (in == null) throw new IOException(path + " is missing from the jar");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void write(File folder, String path, String content) throws IOException {
        File target = new File(folder, path);
        Files.createDirectories(target.getParentFile().toPath());
        Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
    }
}
