package me.juancarloscp52.bedrockify.client.features.creativeInventory;

/** Client-only picker geometry. Player inventory slot numbers remain unchanged. */
public interface CreativeGrid {
    void bedrockify$configureGrid(boolean classic, int columns, int rows, boolean rebuild);
    int bedrockify$pickerSize();
    int bedrockify$firstVisibleIndex(float scroll);
}
