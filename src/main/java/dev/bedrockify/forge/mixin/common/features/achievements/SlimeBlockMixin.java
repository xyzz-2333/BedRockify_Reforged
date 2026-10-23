package dev.bedrockify.forge.mixin.common.features.achievements;

import dev.bedrockify.forge.common.features.achievements.BedrockAchievements;
import net.minecraft.block.SlimeBlock;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeBlock.class)
public abstract class SlimeBlockMixin {
    @Inject(method = "onEntityLand", at = @At("TAIL"))
    private void bedrockify$onSlimeBounce(BlockView world, Entity entity, CallbackInfo info) {
        BedrockAchievements.slimeBounce(entity);
    }
}
