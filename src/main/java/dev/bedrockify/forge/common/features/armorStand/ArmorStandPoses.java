package dev.bedrockify.forge.common.features.armorStand;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.EulerAngle;

/** Generated static rotations from the supplied Bedrock vanilla animation file. */
public final class ArmorStandPoses {
    public static final String[] NAMES = {"default", "no", "solemn", "athena", "brandish", "honor", "entertain", "salute", "riposte", "zombie", "cancan_a", "cancan_b", "hero"};
    private static final float[][][] ROTATIONS = {
        {{0F, 0F, 0F}, {0F, 0F, 0F}, {-10F, 0F, -10F}, {-15F, 0F, 10F}, {-1F, 0F, -1F}, {1F, 0F, 1F}},
        {{0F, 0F, 0F}, {0F, 0F, 0F}, {0F, 0F, 0F}, {0F, 0F, 0F}, {0F, 0F, 0F}, {0F, 0F, 0F}},
        {{15F, 0F, 0F}, {0F, 0F, 2F}, {-30F, 15F, 15F}, {-60F, -20F, -10F}, {-1F, 0F, -1F}, {1F, 0F, 1F}},
        {{-5F, 0F, 0F}, {0F, 0F, 2F}, {10F, 0F, -5F}, {-60F, 20F, -10F}, {-3F, -3F, -3F}, {3F, 3F, 3F}},
        {{-15F, 0F, 0F}, {0F, 0F, -2F}, {20F, 0F, -10F}, {-110F, 50F, 0F}, {5F, -3F, -3F}, {-5F, 3F, 3F}},
        {{-15F, 0F, 0F}, {0F, 0F, 0F}, {-110F, 35F, 0F}, {-110F, -35F, 0F}, {5F, -3F, -3F}, {-5F, 3F, 3F}},
        {{-15F, 0F, 0F}, {0F, 0F, 0F}, {-110F, -35F, 0F}, {-110F, 35F, 0F}, {5F, -3F, -3F}, {-5F, 3F, 3F}},
        {{0F, 0F, 0F}, {0F, 0F, 0F}, {10F, 0F, -5F}, {-70F, -40F, 0F}, {-1F, 0F, -1F}, {1F, 0F, 1F}},
        {{16F, 20F, 0F}, {0F, 0F, 0F}, {4F, 8F, 237F}, {246F, 0F, 89F}, {-14F, -18F, -16F}, {8F, 20F, 4F}},
        {{-10F, 0F, -5F}, {0F, 0F, 0F}, {-105F, 0F, 0F}, {-100F, 0F, 0F}, {7F, 0F, 0F}, {-46F, 0F, 0F}},
        {{-5F, 18F, 0F}, {0F, 22F, 0F}, {8F, 0F, -114F}, {0F, 84F, 111F}, {-111F, 55F, 0F}, {0F, 23F, -13F}},
        {{-10F, -20F, 0F}, {0F, -18F, 0F}, {0F, 0F, -112F}, {8F, 90F, 111F}, {0F, 0F, 13F}, {-119F, -42F, 0F}},
        {{-4F, 67F, 0F}, {0F, 8F, 0F}, {16F, 32F, -8F}, {-99F, 63F, 0F}, {0F, -75F, -8F}, {4F, 63F, 8F}}
    };
    private ArmorStandPoses() {}
    public static int normalize(int pose) { return Math.floorMod(pose, NAMES.length); }
    public static int fromPower(int power) { return Math.min(12, Math.max(0, power)); }
    public static void apply(ArmorStandEntity stand, int pose) {
        float[][] p=ROTATIONS[normalize(pose)];
        stand.setHeadRotation(angle(p[0])); stand.setBodyRotation(angle(p[1]));
        stand.setLeftArmRotation(angle(p[2])); stand.setRightArmRotation(angle(p[3]));
        stand.setLeftLegRotation(angle(p[4])); stand.setRightLegRotation(angle(p[5]));
    }
    private static EulerAngle angle(float[] r) { return new EulerAngle(r[0],r[1],r[2]); }
}
