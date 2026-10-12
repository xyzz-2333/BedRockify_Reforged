package dev.bedrockify.forge.mixin.common.features.mechanics;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ThrownEntity.class)
public interface ThrownEntityAccessor { @Invoker("getGravity") float bedrockify$getGravity(); }
