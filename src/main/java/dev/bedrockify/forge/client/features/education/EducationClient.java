package dev.bedrockify.forge.client.features.education;

import dev.bedrockify.forge.common.features.education.EducationContent;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;

public final class EducationClient {
    private EducationClient() {}
    public static void register() {
        net.minecraft.client.render.entity.EntityRenderers.register(EducationContent.UNDERWATER_TNT_ENTITY, UnderwaterTntRenderer::new);
        EducationContent.BLOCKS.forEach((name,block)-> { if(name.contains("hardened_glass")) RenderLayers.setRenderLayer(block,RenderLayer.getTranslucent()); });
        HandledScreens.register(EducationContent.STATION_MENU,ChemistryStationScreen::new);
        net.minecraft.client.render.entity.EntityRenderers.register(EducationContent.ICE_BOMB_ENTITY,net.minecraft.client.render.entity.FlyingItemEntityRenderer::new);
        HandledScreens.register(EducationContent.REDUCER_MENU, MaterialReducerScreen::new);
        EducationContent.BLOCKS.forEach((name, block) -> { if (name.endsWith("_torch")) RenderLayers.setRenderLayer(block, RenderLayer.getCutout()); });
    }
}
