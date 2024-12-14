package fr.iban.guilds.lang;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import fr.iban.guilds.GuildsPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class LangManager {

    private final GuildsPlugin plugin;
    private final static String DEFAULT_LANG = "en_EN";

    public LangManager(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            boolean created = langFolder.mkdirs();

            if(created){
                plugin.getLogger().info("Created lang folder.");
            }
        }

        try {
            String currentLang = plugin.getConfiguration().getString("language", DEFAULT_LANG);
            YamlDocument messages = loadLocale(currentLang);
            Lang.setMessages(messages);

        } catch (IOException e) {
            plugin.getLogger().severe("Error loading language files!");
            e.printStackTrace();
        }
    }

    private YamlDocument loadLocale(String locale) throws IOException {
        YamlDocument yamlDocument;

        File langFile = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        InputStream defaultFile = plugin.getResource("lang/" + locale + ".yml");

        if(defaultFile != null) {
            yamlDocument = YamlDocument.create(
                    langFile,
                    defaultFile,
                    GeneralSettings.DEFAULT,
                    LoaderSettings.DEFAULT,
                    DumperSettings.DEFAULT,
                    UpdaterSettings.DEFAULT
            );
        } else {
            yamlDocument = YamlDocument.create(
                    langFile,
                    GeneralSettings.DEFAULT,
                    LoaderSettings.DEFAULT,
                    DumperSettings.DEFAULT,
                    UpdaterSettings.DEFAULT
            );
        }


        for (Lang lang : Lang.values()) {
            String key = lang.getKey();
            if (!yamlDocument.contains(key)) {
                yamlDocument.set(key, "__" + key.toLowerCase().replace(".", "_"));
                plugin.getLogger().warning("Missing translation for key: " + key + " in " + locale + ".yml");
            }
        }

        yamlDocument.save();

        return yamlDocument;
    }
}