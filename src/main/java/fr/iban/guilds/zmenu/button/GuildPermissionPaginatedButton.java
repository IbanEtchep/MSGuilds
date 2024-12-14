package fr.iban.guilds.zmenu.button;


import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.enums.GuildPermission;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildRank;
import fr.maxlego08.menu.MenuItemStack;
import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.menu.inventory.inventories.InventoryDefault;
import fr.maxlego08.menu.zcore.utils.inventory.Pagination;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class GuildPermissionPaginatedButton extends AbstractToggleButton implements PaginateButton {

    public GuildPermissionPaginatedButton(Plugin plugin, Material enabledMaterial, Material disabledMaterial) {
        super((GuildsPlugin) plugin, enabledMaterial, disabledMaterial);
    }

    @Override
    public void onRender(Player player, InventoryDefault inventory) {
        List<GuildPermission> permissions = Arrays.stream(GuildPermission.values()).toList();

        Pagination<GuildPermission> pagination = new Pagination<>();
        List<GuildPermission> paginatedWarps = pagination.paginate(permissions, this.slots.size(), inventory.getPage());

        for (int i = 0; i < paginatedWarps.size(); i++) {
            int slot = this.slots.get(i);
            GuildPermission guildPermission = paginatedWarps.get(i);

            inventory.addItem(slot, getRankItem(player, guildPermission)).setClick(e -> {
                GuildRank rank = menuManager.getMenuData(player).getCurrentRank();
                if (rank.hasPermission(guildPermission)) {
                    rank.removePermission(guildPermission);
                } else {
                    rank.addPermission(guildPermission);
                }

                plugin.getMenuManager().update(player);
            });
        }
    }

    @Override
    public int getPaginationSize(Player player) {
        var guildMenuData = menuManager.getMenuData(player);
        return guildMenuData.getGuild().getRanks().size();
    }

    protected ItemStack getRankItem(Player player, GuildPermission guildPermission) {
        GuildRank rank = menuManager.getMenuData(player).getCurrentRank();
        Placeholders placeholders = new Placeholders();
        String prefix = rank.hasPermission(guildPermission) ? "§a" : "§c";

        placeholders.register("name", prefix + guildPermission.getName());

        MenuItemStack menuItemStack = this.getItemStack();
        menuItemStack.setMaterial(String.valueOf(rank.hasPermission(guildPermission) ? enabledMaterial : disabledMaterial));

    return menuItemStack
                .build(player, false, placeholders);
    }

    @Override
    public void onInventoryClose(Player player, InventoryDefault inventory) {
        super.onInventoryClose(player, inventory);
        Guild guild = menuManager.getMenuData(player).getGuild();
        plugin.getGuildManager().saveGuild(guild);
    }
}