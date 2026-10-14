package dev.bedrockify.forge.mixin.client.features.biomeWaterColors;

import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.biomeWaterColors.BedrockWaterColors;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public class BiomeMixin {
    @Inject(method="getWaterColor", at=@At("RETURN"), cancellable=true)
    private void bedrockify$surface(CallbackInfoReturnable<Integer> cir) {
        var colors = BedrockWaterColors.forBiome((Biome)(Object)this);
        if (colors != null) cir.setReturnValue(colors.surface());
    }
    @Inject(method="getWaterFogColor", at=@At("RETURN"), cancellable=true)
    private void bedrockify$fog(CallbackInfoReturnable<Integer> cir) {
        var colors = BedrockWaterColors.forBiome((Biome)(Object)this);
        if (colors != null && BedrockifyClient.getInstance().settings.bedrockWaterFogColor)
            cir.setReturnValue(colors.fog());
    }
}
