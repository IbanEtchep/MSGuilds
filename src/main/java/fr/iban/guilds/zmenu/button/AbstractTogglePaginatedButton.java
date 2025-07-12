package fr.iban.guilds.zmenu.button;

import fr.iban.guilds.GuildsPlugin;
import org.bukkit.Material;

public abstract class AbstractTogglePaginatedButton extends AbstractGuildPaginatedButton {

    protected Material enabledMaterial;
    protected Material disabledMaterial;

    public AbstractTogglePaginatedButton(GuildsPlugin plugin, Material enabledMaterial, Material disabledMaterial) {
        super(plugin);
        this.enabledMaterial = enabledMaterial;
        this.disabledMaterial = disabledMaterial;
    }
}
