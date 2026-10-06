package dev.bedrockify.forge.mixin.client.features.loadingScreens;

import dev.bedrockify.forge.client.features.loadingScreens.MenuPanorama;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Unique
    private boolean bedrockify$loadingBackground() {
        Object screen = this;
        return screen instanceof LevelLoadingScreen || screen instanceof DownloadingTerrainScreen
                || screen instanceof ProgressScreen || screen instanceof MessageScreen || screen instanceof ConnectScreen
                || MenuPanorama.optionsScreen((Screen)(Object)this);
    }

    @Inject(method = {"renderBackground", "renderBackgroundTexture"}, at = @At("HEAD"), cancellable = true)
    private void bedrockify$panorama(DrawContext context, CallbackInfo ci) {
        if (!MenuPanorama.enabled() || !bedrockify$loadingBackground()) return;
        MenuPanorama.render((Screen)(Object)this, context, MinecraftClient.getInstance().getLastFrameDuration());
        ci.cancel();
    }
}
