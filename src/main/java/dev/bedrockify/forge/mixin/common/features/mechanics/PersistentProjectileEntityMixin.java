package dev.bedrockify.forge.mixin.common.features.mechanics;
import dev.bedrockify.forge.common.features.mechanics.MechanicsRules;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(PersistentProjectileEntity.class)
public class PersistentProjectileEntityMixin {
    @ModifyConstant(method="tick", constant=@Constant(doubleValue=0.05000000074505806D))
    private double bedrockify$gravity(double original) {
        return (Object)this instanceof TridentEntity t && MechanicsRules.projectiles(t.getWorld()) ? 0.1D : original;
    }
}
