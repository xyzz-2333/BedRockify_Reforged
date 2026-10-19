package dev.bedrockify.forge.mixin.common.features.mechanics;

import dev.bedrockify.forge.common.features.mechanics.MechanicsRules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {
    @ModifyVariable(method = "shootAll", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static float bedrockify$playerArrowSpeed(float original, World world, LivingEntity user,
            net.minecraft.util.Hand hand, ItemStack crossbow, float speed, float divergence) {
        return user instanceof PlayerEntity
                && MechanicsRules.projectiles(world)
                && !CrossbowItem.hasProjectile(crossbow, Items.FIREWORK_ROCKET)
                && Math.abs(original - 3.15F) < 0.0001F ? 5.0F : original;
    }
}
