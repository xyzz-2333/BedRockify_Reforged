package dev.bedrockify.forge.client.features.education;

import dev.bedrockify.forge.common.features.education.EducationContent;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;

public final class EducationClient {
    private EducationClient() {}
    public static void register() {
        HandledScreens.register(EducationContent.REDUCER_MENU, MaterialReducerScreen::new);
        EducationContent.BLOCKS.forEach((name, block) -> { if (name.endsWith("_torch")) RenderLayers.setRenderLayer(block, RenderLayer.getCutout()); });
    }
}
