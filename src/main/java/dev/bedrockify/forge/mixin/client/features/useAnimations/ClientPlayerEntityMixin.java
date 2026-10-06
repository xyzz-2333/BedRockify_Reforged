package dev.bedrockify.forge.mixin.client.features.useAnimations;

import com.mojang.authlib.GameProfile;
import dev.bedrockify.forge.client.features.useAnimations.AnimationsHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    /**
     * Drop animation for Hotbar item.
     */
    @Inject(method = "dropSelectedItem", at = @At("HEAD"))
    private void bedrockify$animateHotbarItemDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (entireStack) {
            return;
        }

        AnimationsHelper.doBobbingAnimation(this.getMainHandStack());
    }
}
