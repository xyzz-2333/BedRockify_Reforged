package me.juancarloscp52.bedrockify.client.features.survivalInventory;

import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;

/** Implemented on the two vanilla screens, so other menu screens remain untouched. */
public interface SurvivalScreen {
    SurvivalLayout bedrockify$survivalLayout();
    RecipeBookWidget bedrockify$recipeBook();
}
