package fr.iban.guilds.zmenu.button;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.zmenu.ZMenuManager;
import fr.maxlego08.menu.api.button.Button;

public abstract class AbstractGuildMenuButton extends Button {

    protected final ZMenuManager menuManager;
    protected final GuildsPlugin plugin;

    public AbstractGuildMenuButton(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.menuManager = plugin.getMenuManager();
    }
}
