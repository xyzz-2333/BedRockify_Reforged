package dev.bedrockify.forge.mixin.common.features.mechanics;
import dev.bedrockify.forge.common.features.mechanics.MechanicsRules;
import net.minecraft.item.TridentItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(TridentItem.class)
public class TridentItemMixin {
    @ModifyConstant(method="onStoppedUsing", constant=@Constant(floatValue=2.5F))
    private float bedrockify$speed(float original, ItemStack stack, World world, LivingEntity user, int remaining) {
        return MechanicsRules.projectiles(world) ? 4.0F : original;
    }
}
