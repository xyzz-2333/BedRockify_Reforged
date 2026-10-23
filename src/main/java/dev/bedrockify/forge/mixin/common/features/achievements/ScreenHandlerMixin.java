package dev.bedrockify.forge.mixin.common.features.achievements;

import dev.bedrockify.forge.common.features.achievements.BedrockAchievements;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void bedrockify$beforeFuelSlotClick(int slot, int button, SlotActionType action,
                                                 PlayerEntity player, CallbackInfo info) {
        BedrockAchievements.beforeFurnaceSlotClick((ScreenHandler)(Object)this);
        BedrockAchievements.beforeGrindstoneSlotClick((ScreenHandler)(Object)this, slot);
        BedrockAchievements.beforeLoomSlotClick((ScreenHandler)(Object)this, slot);
    }

    @Inject(method = "onSlotClick", at = @At("TAIL"))
    private void bedrockify$afterFuelSlotClick(int slot, int button, SlotActionType action,
                                                PlayerEntity player, CallbackInfo info) {
        BedrockAchievements.afterFurnaceSlotClick((ScreenHandler)(Object)this, player);
        BedrockAchievements.afterGrindstoneSlotClick((ScreenHandler)(Object)this, player);
        BedrockAchievements.afterLoomSlotClick((ScreenHandler)(Object)this, player);
    }
}
