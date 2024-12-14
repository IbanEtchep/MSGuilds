package fr.iban.guilds.zmenu;

import fr.iban.guilds.GuildsPlugin;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildRank;
import fr.iban.guilds.zmenu.button.GuildPermissionPaginatedButton;
import fr.iban.guilds.zmenu.button.GuildRankPaginatedButton;
import fr.iban.guilds.zmenu.button.GuildRankRenameButton;
import fr.iban.guilds.zmenu.data.GuildMenuData;
import fr.iban.guilds.zmenu.loader.ToggleableButtonLoader;
import fr.maxlego08.menu.api.ButtonManager;
import fr.maxlego08.menu.api.Inventory;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.event.events.ButtonLoaderRegisterEvent;
import fr.maxlego08.menu.button.loader.NoneLoader;
import fr.maxlego08.menu.exceptions.InventoryException;
import fr.maxlego08.menu.inventory.inventories.InventoryDefault;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZMenuManager implements Listener {

    private final GuildsPlugin plugin;
    private ButtonManager buttonManager;
    private InventoryManager inventoryManager;
    private final Map<UUID, GuildMenuData> guildMenuDataMap = new HashMap<>();

    private Inventory guildRankMenu;
    private Inventory guildRankManageMenu;

    public ZMenuManager(GuildsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onButtonLoad(ButtonLoaderRegisterEvent event) {
        this.buttonManager = event.getButtonManager();
        this.inventoryManager = event.getInventoryManager();

        this.registerButtons();
        this.loadInventories();
    }

    public void loadZMenu() {
        inventoryManager = getProvider(InventoryManager.class);
        buttonManager = getProvider(ButtonManager.class);

        if (inventoryManager == null || buttonManager == null) {
            plugin.getLogger().warning("ZMenu is not installed !");
            return;
        }

        this.registerButtons();
        this.loadInventories();
    }

    public void unloadZMenu() {
        plugin.getLogger().info("Unloading ZMenu...");
        buttonManager.unregisters(plugin);
        inventoryManager.deleteInventories(plugin);
    }

    private void registerButtons() {
        buttonManager.register(new NoneLoader(plugin, GuildRankPaginatedButton.class, "guild_rank_pagination"));
        buttonManager.register(new ToggleableButtonLoader(plugin, GuildPermissionPaginatedButton.class, "guild_rank_permission_pagination"));
        buttonManager.register(new NoneLoader(plugin, GuildRankRenameButton.class, "guild_rank_rename"));
    }

    private void loadInventories() {
        try {
            guildRankMenu = this.inventoryManager.loadInventoryOrSaveResource(this.plugin, "inventories/guild_rank_menu.yml");
            guildRankManageMenu = this.inventoryManager.loadInventoryOrSaveResource(this.plugin, "inventories/guild_rank_manage_menu.yml");
        } catch (InventoryException exception) {
            exception.printStackTrace();
        }
    }

    private <T> @Nullable T getProvider(Class<T> classProvider) {
        RegisteredServiceProvider<T> provider = Bukkit.getServer().getServicesManager().getRegistration(classProvider);
        return provider == null ? null : provider.getProvider();
    }

    public void openGuildRanksMenu(Player player, Guild guild) {
        GuildMenuData guildMenuData = new GuildMenuData(guild);
        this.guildMenuDataMap.put(player.getUniqueId(), guildMenuData);
        openInventory(player, guildRankMenu);
    }

    public void openGuildRankManageMenu(Player player, GuildRank rank) {
        GuildMenuData guildMenuData = getMenuData(player);
        guildMenuData.setCurrentRank(rank);
        openInventory(player, guildRankManageMenu);
    }

    public void update(Player player) {
        this.inventoryManager.updateInventory(player);
    }

    public GuildMenuData getMenuData(Player player) {
        return guildMenuDataMap.get(player.getUniqueId());
    }

    public void clearMenuData(Player player) {
        guildMenuDataMap.remove(player.getUniqueId());
    }

    public void openInventory(Player player, Inventory inventory) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof InventoryDefault) {
            this.inventoryManager.openInventoryWithOldInventories(player, inventory, 0);
        } else {
            this.inventoryManager.openInventory(player, inventory);
        }
    }
}
