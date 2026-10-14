package dev.bedrockify.forge.tests;

import dev.bedrockify.forge.client.BedrockifyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in startup check; never included in the distributable JAR. */
@Mod.EventBusSubscriber(modid="bedrockify", value=Dist.CLIENT)
public final class Alpha14ClientSmoke {
    private static int titleTicks;
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var client=MinecraftClient.getInstance();
        if (event.phase!=TickEvent.Phase.END) return;
        if (client.currentScreen instanceof AccessibilityOnboardingScreen) client.setScreen(new TitleScreen());
        if (!(client.currentScreen instanceof TitleScreen)) return;
        if (++titleTicks!=3) return;
        if (!(client.inGameHud instanceof ForgeGui)) throw new IllegalStateException("Forge HUD missing");
        var mod=BedrockifyClient.getInstance();
        if (mod.hudOpacity==null || mod.overlay==null || mod.settingsGUI.getConfigScreen(client.currentScreen,false)==null)
            throw new IllegalStateException("Client features or configuration screen missing");
        dev.bedrockify.forge.Bedrockify.LOGGER.info("ALPHA14 CLIENT SMOKE PASS: Forge HUD mixins, client initialization and configuration screen loaded");
        client.scheduleStop();
    }
}
