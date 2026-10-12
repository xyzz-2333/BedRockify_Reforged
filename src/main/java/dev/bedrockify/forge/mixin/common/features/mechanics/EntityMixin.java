package dev.bedrockify.forge.mixin.common.features.mechanics;
import dev.bedrockify.forge.common.features.mechanics.TridentKillerState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method="move", at=@At("HEAD"))
    private void bedrockify$piston(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (type == MovementType.PISTON && movement.lengthSquared() > 0 && (Object)this instanceof TridentEntity)
            ((TridentKillerState)this).bedrockify$activate();
    }
}
