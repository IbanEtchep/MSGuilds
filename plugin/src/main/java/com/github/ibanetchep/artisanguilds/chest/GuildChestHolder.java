package com.github.ibanetchep.artisanguilds.chest;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Marks an inventory as a page of a guild chest; the inventory is shared by all its viewers. */
final class GuildChestHolder implements InventoryHolder {

    final UUID guildId;
    final int page;
    Inventory inventory;

    GuildChestHolder(UUID guildId, int page) {
        this.guildId = guildId;
        this.page = page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
