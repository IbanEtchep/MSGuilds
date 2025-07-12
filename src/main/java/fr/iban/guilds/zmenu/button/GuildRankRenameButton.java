package fr.iban.guilds.zmenu.button;


import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.zmenu.data.GuildMenuData;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

public class GuildRankRenameButton extends AbstractGuildMenuButton {

    public GuildRankRenameButton(Plugin plugin) {
        super((GuildsPlugin) plugin);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        super.onClick(player, event, inventory, slot, placeholders);

        GuildMenuData guildMenuData = menuManager.getMenuData(player);

        player.closeInventory();
        CoreBukkitPlugin core = CoreBukkitPlugin.getInstance();
        player.sendMessage("§aEntrez le nouveau nom du rang, ou tapez \"annuler\".");
        core.getTextInputs().put(player.getUniqueId(), text -> {
            plugin.getGuildRankService().renameRank(player, guildMenuData.getCurrentRank(), text);
            plugin.getMenuManager().openInventory(player, inventory.getMenuInventory());
            core.getTextInputs().remove(player.getUniqueId());
        });
    }
}
