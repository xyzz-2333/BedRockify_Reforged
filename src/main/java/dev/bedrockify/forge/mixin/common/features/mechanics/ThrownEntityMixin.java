package dev.bedrockify.forge.mixin.common.features.mechanics;
import dev.bedrockify.forge.common.features.mechanics.MechanicsRules;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ThrownEntity.class)
public class ThrownEntityMixin {
    @ModifyConstant(method="tick", constant=@Constant(floatValue=0.99F))
    private float bedrockify$air(float original) { return pearl() ? 1.0F : original; }
    @ModifyConstant(method="tick", constant=@Constant(floatValue=0.8F))
    private float bedrockify$water(float original) { return pearl() ? 1.0F : original; }
    @Redirect(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/entity/projectile/thrown/ThrownEntity;getGravity()F"))
    private float bedrockify$gravity(ThrownEntity projectile) { return pearl() ? 0.025F : ((ThrownEntityAccessor)projectile).bedrockify$getGravity(); }
    private boolean pearl() { return (Object)this instanceof EnderPearlEntity p && MechanicsRules.projectiles(p.getWorld()); }
}
