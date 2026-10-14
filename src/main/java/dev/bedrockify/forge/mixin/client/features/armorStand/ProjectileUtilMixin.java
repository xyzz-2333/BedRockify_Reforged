package dev.bedrockify.forge.mixin.client.features.armorStand;

import dev.bedrockify.forge.common.features.armorStand.ArmorStandInteraction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {
    // Observe the completed raycast instead of taking ownership of its caller's invocation.
    // Shoulder Surfing also uses this method with its own camera ray and reach limit.
    @Inject(method="raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;", at=@At("RETURN"), cancellable=true)
    private static void bedrockify$pickArms(Entity camera, Vec3d start, Vec3d end, Box search,
                                          Predicate<Entity> predicate, double distance,
                                          CallbackInfoReturnable<EntityHitResult> cir) {
        if (!camera.getWorld().isClient || camera != MinecraftClient.getInstance().getCameraEntity()) return;
        EntityHitResult result = cir.getReturnValue();
        if (result != null) distance = start.squaredDistanceTo(result.getPos());
        for (Entity entity : camera.getWorld().getOtherEntities(camera, search.expand(1.5), predicate)) {
            if (!(entity instanceof ArmorStandEntity stand) || entity.getRootVehicle() == camera.getRootVehicle()) continue;
            Vec3d point = ArmorStandInteraction.raycast(stand, start, end);
            if (point != null && start.squaredDistanceTo(point) < distance) {
                distance = start.squaredDistanceTo(point);
                result = new EntityHitResult(stand, point);
            }
        }
        if (result != cir.getReturnValue()) cir.setReturnValue(result);
    }
}
