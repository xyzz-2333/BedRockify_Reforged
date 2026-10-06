package dev.bedrockify.forge.client.features.loadingScreens;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bedrockify.forge.client.BedrockifyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screen.option.TelemetryInfoScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

/** Reuses vanilla's cube map, textures and panorama-speed option. */
public final class MenuPanorama {
    private static final Identifier OVERLAY = new Identifier("minecraft", "textures/gui/title/background/panorama_overlay.png");
    private static RotatingCubeMapRenderer renderer;

    private MenuPanorama() {}

    public static boolean enabled() {
        var mod = BedrockifyClient.getInstance();
        return mod != null && mod.settings != null && mod.settings.menuPanorama;
    }

    public static boolean optionsScreen(Screen screen) {
        return screen instanceof OptionsScreen || screen instanceof GameOptionsScreen
                || screen instanceof CreditsAndAttributionScreen || screen instanceof TelemetryInfoScreen
                || screen instanceof PackScreen;
    }

    public static void render(Screen screen, DrawContext context, float delta) {
        context.draw();
        context.setShaderColor(1, 1, 1, 1);
        if (renderer == null) renderer = new RotatingCubeMapRenderer(TitleScreen.PANORAMA_CUBE_MAP);
        renderer.render(delta, 1);
        RenderSystem.enableBlend();
        context.drawTexture(OVERLAY, 0, 0, screen.width, screen.height, 0, 0, 16, 128, 16, 128);
        // Keep loading tips and world names readable without replacing their widgets.
        context.fill(0, 0, screen.width, screen.height, 0x40000000);
        context.setShaderColor(1, 1, 1, 1);
        MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered(screen, context));
    }
}
