package me.juancarloscp52.bedrockify.mixin.client.features.survivalInventory;

import me.juancarloscp52.bedrockify.client.features.survivalInventory.SurvivalScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Inject(method = "removed", at = @At("HEAD"))
    private void bedrockify$restoreSlots(CallbackInfo ci) {
        if ((Object)this instanceof SurvivalScreen screen) screen.bedrockify$survivalLayout().restore();
    }
}
