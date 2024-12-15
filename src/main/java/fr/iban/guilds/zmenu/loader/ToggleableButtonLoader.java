package fr.iban.guilds.zmenu.loader;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.zmenu.button.AbstractToggleButton;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;


public class ToggleableButtonLoader implements ButtonLoader {

    private final Class<? extends AbstractToggleButton> buttonClass;
    private final GuildsPlugin plugin;
    private final String name;

    public ToggleableButtonLoader(GuildsPlugin plugin, Class<? extends AbstractToggleButton> buttonClass, String name) {
        super();
        this.plugin = plugin;
        this.buttonClass = buttonClass;
        this.name = name;
    }


    @Override
    public Class<? extends Button> getButton() {
        return AbstractToggleButton.class;
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
