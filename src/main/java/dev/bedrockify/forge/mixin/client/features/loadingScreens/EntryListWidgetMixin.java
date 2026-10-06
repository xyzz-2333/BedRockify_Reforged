package dev.bedrockify.forge.mixin.client.features.loadingScreens;

import dev.bedrockify.forge.client.features.loadingScreens.MenuPanorama;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntryListWidget.class)
public abstract class EntryListWidgetMixin {
    @Shadow private boolean renderBackground;
    @Shadow private boolean renderHorizontalShadows;
    @Unique private boolean bedrockify$customized;
    @Unique private boolean bedrockify$originalBackground, bedrockify$originalShadows;

    @Inject(method = "render", at = @At("HEAD"))
    private void bedrockify$transparentWorldList(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        boolean eligible = (Object)this instanceof WorldListWidget
                || MenuPanorama.optionsScreen(MinecraftClient.getInstance().currentScreen);
        if (!eligible && !bedrockify$customized) return;
        if (eligible && MenuPanorama.enabled()) {
            if (!bedrockify$customized) {
                bedrockify$originalBackground = renderBackground;
                bedrockify$originalShadows = renderHorizontalShadows;
                bedrockify$customized = true;
            }
            renderBackground = false; renderHorizontalShadows = false;
        } else if (bedrockify$customized) {
            renderBackground = bedrockify$originalBackground;
            renderHorizontalShadows = bedrockify$originalShadows;
            bedrockify$customized = false;
        }
    }
}
