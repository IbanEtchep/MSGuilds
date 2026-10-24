package com.github.ibanetchep.artisanguilds.chest;

import org.bukkit.event.inventory.InventoryAction;

/** Which clicks a member may make in the guild chest, given their deposit and withdraw rights. */
final class ChestAccess {

    private ChestAccess() {}

    /** @param inChest the clicked slot is the chest's (top inventory), not the player's own */
    static boolean allows(InventoryAction action, boolean inChest, boolean canDeposit, boolean canWithdraw) {
        if (!inChest) {
            return switch (action) {
                case MOVE_TO_OTHER_INVENTORY -> canDeposit;
                case COLLECT_TO_CURSOR -> canWithdraw; // gathers matching items from the chest too
                default -> true;
            };
        }
        return switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME -> canDeposit;
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME,
                 DROP_ALL_SLOT, DROP_ONE_SLOT, MOVE_TO_OTHER_INVENTORY, COLLECT_TO_CURSOR -> canWithdraw;
            case SWAP_WITH_CURSOR, HOTBAR_SWAP -> canDeposit && canWithdraw;
            case NOTHING, DROP_ALL_CURSOR, DROP_ONE_CURSOR -> true;
            // Bundles, creative cloning and anything newer: refused rather than guessed.
            default -> false;
        };
    }
}
