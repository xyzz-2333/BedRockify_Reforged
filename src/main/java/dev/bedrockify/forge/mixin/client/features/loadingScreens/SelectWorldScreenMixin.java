package dev.bedrockify.forge.mixin.client.features.loadingScreens;

import dev.bedrockify.forge.client.features.loadingScreens.MenuPanorama;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void bedrockify$panorama(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MenuPanorama.enabled()) MenuPanorama.render((Screen)(Object)this, context, delta);
    }
}
