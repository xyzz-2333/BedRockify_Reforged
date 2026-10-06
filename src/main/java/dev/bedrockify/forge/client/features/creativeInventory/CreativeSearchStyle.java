package dev.bedrockify.forge.client.features.creativeInventory;

import net.minecraft.client.gui.widget.TextFieldWidget;

/** Identifies only this screen's light-background search field. */
public interface CreativeSearchStyle {
    boolean bedrockify$flatSearch(TextFieldWidget field);
}
