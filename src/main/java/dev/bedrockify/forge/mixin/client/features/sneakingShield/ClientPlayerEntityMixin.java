package dev.bedrockify.forge.mixin.client.features.sneakingShield;

import dev.bedrockify.forge.client.BedrockifyClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @ModifyConstant(method="tickMovement", constant=@Constant(floatValue=0.2F), require=2, allow=2)
    private float bedrockify$singleShieldSlowdown(float original) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        var mod = BedrockifyClient.getInstance();
        // Input.tick already applied crouching (including Swift Sneak). Do not multiply it again.
        return mod != null && mod.settings.isSneakingShieldEnabled() && player.shouldSlowDown()
                && player.isSneaking() && player.getActiveItem().getItem() instanceof ShieldItem ? 1F : original;
    }
}
