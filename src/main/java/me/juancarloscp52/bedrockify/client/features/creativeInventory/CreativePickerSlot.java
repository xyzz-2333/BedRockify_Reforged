package me.juancarloscp52.bedrockify.client.features.creativeInventory;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.inventory.Inventory;

/** Only populated catalogue cells are interactive; player inventory slots stay native. */
public final class CreativePickerSlot extends CreativeInventoryScreen.LockableSlot {
    public CreativePickerSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override public boolean isEnabled() {
        return hasStack() && super.isEnabled();
    }
}
