package fr.iban.guilds.zmenu.button;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.zmenu.ZMenuManager;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.PaginateButton;

public abstract class AbstractGuildPaginatedButton extends PaginateButton {

    protected final ZMenuManager menuManager;
    protected final GuildsPlugin plugin;

    public AbstractGuildPaginatedButton(GuildsPlugin plugin) {
        this.plugin = plugin;
        this.menuManager = plugin.getMenuManager();
    }
}
