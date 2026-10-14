package dev.bedrockify.forge.mixin.common.features.mechanics;

import dev.bedrockify.forge.common.features.mechanics.MechanicsRules;
import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageEnchantment.class)
public class DamageEnchantmentMixin {
    @Inject(method="getAttackDamage", at=@At("HEAD"), cancellable=true)
    private void bedrockify$sharpness(int level, EntityGroup group, CallbackInfoReturnable<Float> cir) {
        if ((Object)this==Enchantments.SHARPNESS && MechanicsRules.bedrockSharpness())
            cir.setReturnValue(1.25F*Math.max(0,level));
    }
}
