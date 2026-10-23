package dev.bedrockify.forge.mixin.common.features.achievements;

import dev.bedrockify.forge.common.features.achievements.BedrockAchievements;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private static void bedrockify$beforeFuel(World world, BlockPos pos, BlockState state,
                                                AbstractFurnaceBlockEntity furnace, CallbackInfo info) {
        BedrockAchievements.beforeFurnaceTick(furnace);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private static void bedrockify$afterFuel(World world, BlockPos pos, BlockState state,
                                               AbstractFurnaceBlockEntity furnace, CallbackInfo info) {
        BedrockAchievements.afterFurnaceTick(furnace);
    }
}
