package dev.bedrockify.forge.mixin.client.features.snowyLeaves;

import dev.bedrockify.forge.client.features.snowyLeaves.SnowyLeaves;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockColors.class)
public class BlockColorsMixin {
    @Inject(method="getColor(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;I)I",
            at=@At("RETURN"), cancellable=true)
    private void bedrockify$snow(BlockState state, BlockRenderView world, BlockPos pos, int tint,
                                 CallbackInfoReturnable<Integer> cir) {
        if (tint == 0) cir.setReturnValue(SnowyLeaves.tint(state, world, pos, cir.getReturnValue()));
    }
}
