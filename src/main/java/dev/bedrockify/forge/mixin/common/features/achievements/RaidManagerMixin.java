package dev.bedrockify.forge.mixin.common.features.achievements;

import dev.bedrockify.forge.common.features.achievements.BedrockAchievements;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.raid.Raid;
import net.minecraft.village.raid.RaidManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RaidManager.class)
public abstract class RaidManagerMixin {
    @Inject(method = "startRaid", at = @At("RETURN"))
    private void bedrockify$raidStarted(ServerPlayerEntity player, CallbackInfoReturnable<Raid> result) {
        if (result.getReturnValue() != null) BedrockAchievements.raidStarted(player);
    }
}
