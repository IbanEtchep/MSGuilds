package com.github.ibanetchep.artisanguilds.chest;

import org.junit.jupiter.api.Test;

import static org.bukkit.event.inventory.InventoryAction.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestAccessTest {

    @Test
    void depositOnlyPutsInAndTakesNothingOut() {
        assertTrue(ChestAccess.allows(PLACE_ALL, true, true, false));
        assertTrue(ChestAccess.allows(MOVE_TO_OTHER_INVENTORY, false, true, false));
        assertFalse(ChestAccess.allows(PICKUP_ALL, true, true, false));
        assertFalse(ChestAccess.allows(MOVE_TO_OTHER_INVENTORY, true, true, false));
        assertFalse(ChestAccess.allows(COLLECT_TO_CURSOR, false, true, false), "would gather from the chest");
        assertFalse(ChestAccess.allows(SWAP_WITH_CURSOR, true, true, false));
    }

    @Test
    void withdrawOnlyTakesOutAndPutsNothingIn() {
        assertTrue(ChestAccess.allows(PICKUP_HALF, true, false, true));
        assertTrue(ChestAccess.allows(DROP_ONE_SLOT, true, false, true));
        assertFalse(ChestAccess.allows(PLACE_ONE, true, false, true));
        assertFalse(ChestAccess.allows(MOVE_TO_OTHER_INVENTORY, false, false, true));
    }

    @Test
    void theOwnInventoryStaysFreeAndUnknownActionsAreRefused() {
        assertTrue(ChestAccess.allows(PICKUP_ALL, false, false, false));
        assertTrue(ChestAccess.allows(HOTBAR_SWAP, true, true, true));
        assertFalse(ChestAccess.allows(CLONE_STACK, true, true, true));
    }
}
