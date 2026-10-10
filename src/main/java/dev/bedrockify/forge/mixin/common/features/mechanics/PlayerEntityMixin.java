package dev.bedrockify.forge.mixin.common.features.mechanics;

import dev.bedrockify.forge.common.features.mechanics.MechanicsRules;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "getAttackCooldownProgress", at = @At("HEAD"), cancellable = true)
    private void bedrockify$chargedAttack(float tickDelta, CallbackInfoReturnable<Float> callback) {
        if (MechanicsRules.noAttackCooldown((PlayerEntity) (Object) this)) callback.setReturnValue(1.0F);
    }
}
