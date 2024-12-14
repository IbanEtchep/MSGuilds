package fr.iban.guilds.zmenu.button;

import fr.iban.guilds.GuildsPlugin;
import org.bukkit.Material;

public abstract class AbstractToggleButton extends AbstractGuildMenuButton {

    protected Material enabledMaterial;
    protected Material disabledMaterial;

    public AbstractToggleButton(GuildsPlugin plugin, Material enabledMaterial, Material disabledMaterial) {
        super(plugin);
        this.enabledMaterial = enabledMaterial;
        this.disabledMaterial = disabledMaterial;
    }
}
