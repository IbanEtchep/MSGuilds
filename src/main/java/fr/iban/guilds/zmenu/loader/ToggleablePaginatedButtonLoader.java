package fr.iban.guilds.zmenu.loader;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.zmenu.button.AbstractTogglePaginatedButton;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;


public class ToggleablePaginatedButtonLoader extends ButtonLoader {

    private final Class<? extends AbstractTogglePaginatedButton> buttonClass;
    private final GuildsPlugin plugin;
    private final String name;

    public ToggleablePaginatedButtonLoader(GuildsPlugin plugin, Class<? extends AbstractTogglePaginatedButton> buttonClass, String name) {
        super(plugin, name);
        this.plugin = plugin;
        this.buttonClass = buttonClass;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public Button load(YamlConfiguration yamlConfiguration, String s, DefaultButtonValue defaultButtonValue) {
        Material enabledMaterial = Material.valueOf(yamlConfiguration.getString(s+"enabledMaterial", "LIME_DYE"));
        Material disabledMaterial = Material.valueOf(yamlConfiguration.getString(s+"disabledMaterial", "GRAY_DYE"));

        try {
            return this.buttonClass
                    .getConstructor(Plugin.class, Material.class, Material.class)
                    .newInstance(this.plugin, enabledMaterial, disabledMaterial);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
