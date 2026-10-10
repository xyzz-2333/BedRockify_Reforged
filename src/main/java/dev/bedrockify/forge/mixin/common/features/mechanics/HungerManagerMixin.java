package dev.bedrockify.forge.mixin.common.features.mechanics;

import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.common.features.mechanics.MechanicsRules;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HungerManager.class)
public class HungerManagerMixin {
    // Skip only saturation-based fast healing. Keep gamerule, exhaustion and starvation intact.
    @Redirect(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;canFoodHeal()Z", ordinal = 0))
    private boolean bedrockify$slowHealing(PlayerEntity player) {
        return !Bedrockify.getInstance().settings.bedrockSlowRegeneration && player.canFoodHeal();
    }
    @ModifyConstant(method = "update", constant = @Constant(intValue = 80, ordinal = 0))
    private int bedrockify$healingInterval(int original) {
        return Bedrockify.getInstance().settings.bedrockSlowRegeneration ? MechanicsRules.regenerationInterval() : original;
    }
}
