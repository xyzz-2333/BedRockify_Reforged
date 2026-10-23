package dev.bedrockify.forge.mixin.common.features.achievements;

import dev.bedrockify.forge.common.features.achievements.BedrockAchievements;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.passive.CatEntity$SleepWithOwnerGoal")
public abstract class CatMorningGiftMixin {
    @Shadow private PlayerEntity owner;

    @Inject(method = "dropMorningGifts", at = @At("HEAD"))
    private void bedrockify$startMorningGift(CallbackInfo info) {
        BedrockAchievements.startCatMorningGift(owner);
    }

    @Inject(method = "dropMorningGifts", at = @At("TAIL"))
    private void bedrockify$endMorningGift(CallbackInfo info) {
        BedrockAchievements.endCatMorningGift();
    }
}
