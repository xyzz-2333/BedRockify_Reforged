package dev.bedrockify.forge.mixin.common.features.mechanics;

import dev.bedrockify.forge.common.features.mechanics.MechanicsRules;
import dev.bedrockify.forge.common.features.mechanics.BedrockTridentDamage;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "getAttackCooldownProgress", at = @At("HEAD"), cancellable = true)
    private void bedrockify$chargedAttack(float tickDelta, CallbackInfoReturnable<Float> callback) {
        if (MechanicsRules.noAttackCooldown((PlayerEntity) (Object) this)) callback.setReturnValue(1.0F);
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/enchantment/EnchantmentHelper;getAttackDamage(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityGroup;)F", ordinal = 0))
    private float bedrockify$meleeImpaling(ItemStack stack, EntityGroup group, Entity target) {
        float vanillaBonus = EnchantmentHelper.getAttackDamage(stack, group);
        if (stack.isOf(Items.TRIDENT) && target instanceof LivingEntity living
                && MechanicsRules.projectiles(((PlayerEntity) (Object) this).getWorld()))
            return BedrockTridentDamage.withBedrockImpaling(stack, living, vanillaBonus);
        return vanillaBonus;
    }
}
