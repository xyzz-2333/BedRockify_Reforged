package dev.bedrockify.forge.mixin.common.features.achievements;

import dev.bedrockify.forge.common.features.achievements.BedrockAchievements;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {
    @Redirect(method = "litServerTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/ItemScatterer;spawn(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V"))
    private static void bedrockify$markCookedFish(World world, double x, double y, double z, ItemStack stack) {
        BedrockAchievements.spawnCampfireOutput(world, x, y, z, stack);
    }
}
