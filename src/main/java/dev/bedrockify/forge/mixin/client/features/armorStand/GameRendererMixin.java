package dev.bedrockify.forge.mixin.client.features.armorStand;

import dev.bedrockify.forge.common.features.armorStand.ArmorStandInteraction;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Redirect(method="updateTargetedEntity", at=@At(value="INVOKE", target="Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"))
    private EntityHitResult bedrockify$pickArms(Entity camera, Vec3d start, Vec3d end, Box search,
                                               Predicate<Entity> predicate, double distance) {
        EntityHitResult result = ProjectileUtil.raycast(camera, start, end, search, predicate, distance);
        if (result != null) distance = start.squaredDistanceTo(result.getPos());
        for (Entity entity : camera.getWorld().getOtherEntities(camera, search.expand(1.5), predicate)) {
            if (!(entity instanceof ArmorStandEntity stand) || entity.getRootVehicle() == camera.getRootVehicle()) continue;
            Vec3d point = ArmorStandInteraction.raycast(stand, start, end);
            if (point != null && start.squaredDistanceTo(point) < distance) {
                distance = start.squaredDistanceTo(point);
                result = new EntityHitResult(stand, point);
            }
        }
        return result;
    }
}
