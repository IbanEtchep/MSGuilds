package fr.iban.guilds.zmenu.button;


import com.destroystokyo.paper.profile.PlayerProfile;
import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildPlayer;
import fr.iban.guilds.util.ChatUtils;
import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.menu.inventory.inventories.InventoryDefault;
import fr.maxlego08.menu.zcore.utils.inventory.Pagination;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class GuildMemberPaginatedButton extends AbstractToggleButton implements PaginateButton {

    public GuildMemberPaginatedButton(Plugin plugin, Material enabledMaterial, Material disabledMaterial) {
        super((GuildsPlugin) plugin, enabledMaterial, disabledMaterial);
    }

    @Override
    public void onRender(Player player, InventoryDefault inventory) {
        Guild guild = menuManager.getMenuData(player).getGuild();
        List<GuildPlayer> members = new ArrayList<>(guild.getMembers().values());

        Pagination<GuildPlayer> pagination = new Pagination<>();
        List<GuildPlayer> paginatedWarps = pagination.paginate(members, this.slots.size(), inventory.getPage());

        for (int i = 0; i < paginatedWarps.size(); i++) {
            int slot = this.slots.get(i);
            GuildPlayer guildPlayer = paginatedWarps.get(i);

            inventory.addItem(slot, this.getPlayerItem(player, guildPlayer));
        }
    }

    @Override
    public int getPaginationSize(Player player) {
        var guildMenuData = menuManager.getMenuData(player);
        return guildMenuData.getGuild().getMembers().size();
    }

    protected ItemStack getPlayerItem(Player player, GuildPlayer guildPlayer) {

        Placeholders placeholders = new Placeholders();
        placeholders.register("rank", ChatUtils.toMiniMessage(guildPlayer.getRank().getDisplayName()));


        ItemStack itemstack = this.getItemStack().build(player, false, placeholders);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(guildPlayer.getUuid());
        profile.complete(true);
        sm.setPlayerProfile(profile);
        head.setItemMeta(sm);

        var meta = head.getItemMeta();
        meta.displayName(itemstack.getItemMeta().displayName());
        head.setItemMeta(meta);

        return head;
    }
}