package dev.bedrockify.forge.mixin.client.features.survivalInventory;

import dev.bedrockify.forge.client.features.survivalInventory.*;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Inject(method = "removed", at = @At("HEAD"))
    private void bedrockify$restoreSlots(CallbackInfo ci) {
        if ((Object)this instanceof SurvivalScreen screen) screen.bedrockify$survivalLayout().restore();
    }
    private BedrockRecipePanel bedrockify$panel() {
        if (!((Object)this instanceof SurvivalScreen screen) || !screen.bedrockify$survivalLayout().active()
                || !screen.bedrockify$recipeBook().isOpen()) return null;
        return ((SurvivalRecipeBook)screen.bedrockify$recipeBook()).bedrockify$recipePanel();
    }
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void bedrockify$drag(double mx, double my, int button, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        var panel = bedrockify$panel();
        if (panel != null && panel.drag(mx, my)) cir.setReturnValue(true);
    }
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void bedrockify$release(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
        var panel = bedrockify$panel();
        if (panel != null && panel.release()) cir.setReturnValue(true);
    }
}
