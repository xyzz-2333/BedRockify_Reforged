package dev.bedrockify.forge.common.features.armorStand;

import dev.bedrockify.forge.Bedrockify;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Arm picking uses model-space boxes, without enlarging the physical collision box. */
public final class ArmorStandInteraction {
    private ArmorStandInteraction() {}

    public static boolean enabled(ArmorStandEntity stand) {
        return Bedrockify.getInstance() != null && Bedrockify.getInstance().settings.bedrockArmorStand
                && stand.shouldShowArms() && !stand.isMarker() && !stand.isInvisible();
    }

    private static Box armBox(boolean left) {
        // Vanilla wooden arm cuboid, plus one model pixel for practical targeting.
        return new Box(left ? -1 : -3, -3, -2, left ? 3 : 1, 11, 2);
    }

    private static Quaternionf rotation(ArmorStandEntity stand, boolean left) {
        EulerAngle angle = left ? stand.getLeftArmRotation() : stand.getRightArmRotation();
        float radians = (float) (Math.PI / 180);
        return new Quaternionf().rotationZYX(angle.getRoll() * radians, angle.getYaw() * radians, angle.getPitch() * radians);
    }

    private static Vec3d toArm(ArmorStandEntity stand, Vec3d relative, boolean left) {
        double yaw = Math.toRadians(stand.getYaw()), cos = Math.cos(yaw), sin = Math.sin(yaw);
        double scale = stand.isSmall() ? 0.5 : 1;
        double x = (cos * relative.x + sin * relative.z) / scale;
        double z = (sin * relative.x - cos * relative.z) / scale;
        Vector3f point = new Vector3f((float) (x * 16 - (left ? 5 : -5)),
                (float) ((1.501 - relative.y / scale) * 16 - 2), (float) (z * 16));
        rotation(stand, left).conjugate().transform(point);
        return new Vec3d(point.x, point.y, point.z);
    }

    public static EquipmentSlot slotAt(ArmorStandEntity stand, Vec3d relative) {
        if (!enabled(stand)) return null;
        boolean right = armBox(false).expand(0.02).contains(toArm(stand, relative, false));
        boolean left = armBox(true).expand(0.02).contains(toArm(stand, relative, true));
        if (right && (!left || stand.hasStackEquipped(EquipmentSlot.MAINHAND))) return EquipmentSlot.MAINHAND;
        return left ? EquipmentSlot.OFFHAND : null;
    }

    public static Vec3d raycast(ArmorStandEntity stand, Vec3d start, Vec3d end) {
        if (!enabled(stand)) return null;
        Vec3d nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (boolean left : new boolean[]{false, true}) {
            Vec3d a = toArm(stand, start.subtract(stand.getPos()), left);
            Vec3d b = toArm(stand, end.subtract(stand.getPos()), left);
            Box box = armBox(left);
            var hit = box.contains(a) ? java.util.Optional.of(a) : box.raycast(a, b);
            if (hit.isEmpty() || a.squaredDistanceTo(b) < 1.0e-12) continue;
            double fraction = Math.sqrt(a.squaredDistanceTo(hit.get()) / a.squaredDistanceTo(b));
            Vec3d point = start.lerp(end, fraction);
            double candidate = start.squaredDistanceTo(point);
            if (candidate < distance) { nearest = point; distance = candidate; }
        }
        return nearest;
    }
}
