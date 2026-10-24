package com.github.ibanetchep.artisanguilds.settings;

import com.github.ibanetchep.artisanguilds.api.GuildLevelInfo;
import com.github.ibanetchep.artisanguilds.service.GuildLeveling;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NavigableMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildSettingsTest {

    private final Logger logger = Logger.getLogger("GuildSettingsTest");

    @TempDir
    Path folder;

    private InputStreamSource jar() {
        return path -> getClass().getClassLoader().getResourceAsStream(path);
    }

    private interface InputStreamSource extends java.util.function.Function<String, java.io.InputStream> {}

    @Test
    void aNewServerGetsTheJarsSettingsAndModel() throws Exception {
        assertFalse(GuildSettings.ensure(folder.toFile(), null, jar(), logger));

        assertEquals("model: guilds:settings\n", Files.readString(folder.resolve(GuildSettings.MODEL_PATH)));
        YamlConfiguration settings = GuildSettings.load(folder.toFile(), jar(), logger);
        assertEquals("en", settings.getString("language"));
        NavigableMap<Integer, GuildLevelInfo> levels = GuildLeveling.read(settings.getConfigurationSection("levels"), logger);
        assertEquals(11, levels.size());
        assertEquals(new GuildLevelInfo(2, 2, 7, 3, 1), levels.get(2));
        assertEquals(new GuildLevelInfo(10, 10, 30, 6, 5), levels.get(10));
    }

    @Test
    void aConfigWithLevelsMovesItsLanguageAndLevelsEachSpelledOut() throws Exception {
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.loadFromString("language: fr_FR\nlevels:\n  0: {xp: 0, max-members: 4}\n"
                + "  1: {xp: 10, max-members: 8, chest-rows: 3, chest-pages: 1}\n  2: {xp: 20, max-members: 9}\n");

        assertTrue(GuildSettings.ensure(folder.toFile(), legacy, jar(), logger));

        YamlConfiguration settings = GuildSettings.load(folder.toFile(), jar(), logger);
        assertEquals("fr_FR", settings.getString("language"));
        assertEquals(3, settings.getInt("levels.2.chest-rows"), "the inherited chest is written out");
        assertEquals(GuildLeveling.read(legacy.getConfigurationSection("levels"), logger),
                GuildLeveling.read(settings.getConfigurationSection("levels"), logger));
    }

    @Test
    void anExistingFileIsTheAdminsAndAMissingOneFallsBackToTheJar() throws Exception {
        File data = folder.resolve("data").toFile();
        data.mkdirs();
        Files.writeString(data.toPath().resolve("settings.yaml"), "language: fr\nlevels:\n  0: {xp: 0, max-members: 3}\n");
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.loadFromString("levels:\n  0: {xp: 0, max-members: 9}\n");

        assertFalse(GuildSettings.ensure(folder.toFile(), legacy, jar(), logger));
        assertFalse(Files.exists(folder.resolve(GuildSettings.PATH)), "the .yaml twin is the file");
        assertEquals(3, GuildSettings.load(folder.toFile(), jar(), logger).getInt("levels.0.max-members"));
        assertEquals("en", GuildSettings.load(null, jar(), logger).getString("language"));
    }
}
