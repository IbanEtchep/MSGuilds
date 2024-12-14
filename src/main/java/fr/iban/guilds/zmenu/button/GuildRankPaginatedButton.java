package fr.iban.guilds.zmenu.button;


import fr.iban.bukkitcore.menu.ConfirmMenu;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.model.GuildRank;
import fr.iban.guilds.zmenu.data.GuildMenuData;
import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.menu.inventory.inventories.InventoryDefault;
import fr.maxlego08.menu.zcore.utils.inventory.Pagination;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class GuildRankPaginatedButton extends AbstractGuildMenuButton implements PaginateButton {

    public GuildRankPaginatedButton(Plugin plugin) {
        super((GuildsPlugin) plugin);
    }

    @Override
    public void onRender(Player player, InventoryDefault inventory) {
        GuildMenuData guildMenuData = menuManager.getMenuData(player);
        List<GuildRank> ranks = guildMenuData.getGuild().getRanks();

        Pagination<GuildRank> pagination = new Pagination<>();
        List<GuildRank> paginatedWarps = pagination.paginate(ranks, this.slots.size(), inventory.getPage());

        for (int i = 0; i < paginatedWarps.size(); i++) {
            int slot = this.slots.get(i);
            GuildRank rank = paginatedWarps.get(i);

            inventory.addItem(slot, getRankItem(player, rank)).setClick(e -> {
                switch (e.getClick()) {
                    case RIGHT:
                        player.closeInventory();
                        new ConfirmMenu(player, result -> {
                            if(result){
                                plugin.getGuildRankService().deleteRank(player, rank);
                            }

                            menuManager.openInventory(player, inventory.getMenuInventory());
                        }).open();
                        break;
                    case LEFT:
                        menuManager.openGuildRankManageMenu(player, rank);
                        break;
                    case SHIFT_LEFT:
                        plugin.getGuildRankService().rankMoveDown(player, rank);
                        plugin.getMenuManager().update(player);
                        break;
                    case SHIFT_RIGHT:
                        plugin.getGuildRankService().rankMoveUp(player, rank);
                        plugin.getMenuManager().update(player);
                        break;
                    default:
                        break;
                }
            });
        }
    }

    @Override
    public int getPaginationSize(Player player) {
        var guildMenuData = menuManager.getMenuData(player);
        return guildMenuData.getGuild().getRanks().size();
    }


    protected ItemStack getRankItem(Player player, GuildRank rank) {
        Placeholders placeholders = new Placeholders();
        placeholders.register("name", rank.getName());

        return this.getItemStack().build(player, false, placeholders);
    }
}
