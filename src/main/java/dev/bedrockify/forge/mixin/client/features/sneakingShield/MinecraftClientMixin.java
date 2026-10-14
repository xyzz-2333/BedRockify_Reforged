package dev.bedrockify.forge.mixin.client.features.sneakingShield;

import dev.bedrockify.forge.client.features.sneakingShield.SneakingShield;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Redirect(method="handleInputEvents", at=@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerInteractionManager;stopUsingItem(Lnet/minecraft/entity/player/PlayerEntity;)V"))
    private void bedrockify$keepAutomaticShield(ClientPlayerInteractionManager manager, PlayerEntity player) {
        if (!SneakingShield.shouldKeepUsing((MinecraftClient)(Object)this)) manager.stopUsingItem(player);
    }
}
