package me.juancarloscp52.bedrockify.client.features.creativeInventory;

import net.minecraft.inventory.SimpleInventory;

/** Client-only picker geometry. Player inventory slot numbers remain unchanged. */
public interface CreativeGrid {
    void bedrockify$configureGrid(boolean classic, int columns, int rows, boolean rebuild);
    SimpleInventory bedrockify$pickerInventory();
    int bedrockify$pickerSize();
    int bedrockify$firstVisibleIndex(float scroll);
}
