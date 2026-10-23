package dev.bedrockify.forge.mixin.common.features.mechanics;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PowderSnowBucketItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBucketItem.class)
public class PowderSnowBucketItemMixin {
    @Inject(method = "placeFluid", at = @At("HEAD"))
    private void bedrockify$replacePortalFromDispenser(PlayerEntity player, World world, BlockPos pos,
                                                         BlockHitResult hit, CallbackInfoReturnable<Boolean> cir) {
        if (player == null && world instanceof ServerWorld && world.isInBuildLimit(pos)
                && world.getBlockState(pos).isOf(Blocks.NETHER_PORTAL)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }
}
