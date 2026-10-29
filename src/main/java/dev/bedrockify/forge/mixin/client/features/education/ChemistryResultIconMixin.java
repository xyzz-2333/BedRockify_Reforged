package dev.bedrockify.forge.mixin.client.features.education;

import dev.bedrockify.forge.client.features.education.ChemistryStationScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class ChemistryResultIconMixin {
    @Inject(method="drawSlot",at=@At("HEAD"),cancellable=true)
    private void bedrockify$elementFace(DrawContext context,Slot slot,CallbackInfo ci) {
        if((Object)this instanceof ChemistryStationScreen screen && screen.drawElementResult(context,slot))ci.cancel();
    }
}
