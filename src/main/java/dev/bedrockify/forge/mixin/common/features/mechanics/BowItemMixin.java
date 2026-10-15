package dev.bedrockify.forge.mixin.common.features.mechanics;

import dev.bedrockify.forge.common.features.mechanics.MechanicsRules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BowItem.class)
public class BowItemMixin {
    @ModifyConstant(method = "onStoppedUsing", constant = @Constant(floatValue = 3.0F))
    private float bedrockify$playerArrowSpeed(float original, ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        return user instanceof PlayerEntity && MechanicsRules.projectiles(world) ? 5.0F : original;
    }
}
