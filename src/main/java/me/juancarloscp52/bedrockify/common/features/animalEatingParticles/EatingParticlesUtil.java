package me.juancarloscp52.bedrockify.common.features.animalEatingParticles;

import me.juancarloscp52.bedrockify.Bedrockify;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class EatingParticlesUtil {

    public static void spawnItemParticles(PlayerEntity player, ItemStack stack, AnimalEntity entity) {
        if(player.getWorld().isClient)
            return;
        int count = 16;
        for (int i = 0; i < count; ++i) {
            Vec3d vec3d = new Vec3d(((double)entity.getRandom().nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, 0.0);
            vec3d = vec3d.rotateX(-entity.getPitch() * ((float)Math.PI / 180));
            vec3d = vec3d.rotateY(-entity.getYaw() * ((float)Math.PI / 180));
            double d = (double)(-entity.getRandom().nextFloat()) * 0.6 - 0.3;
            Vec3d vec3d2 = new Vec3d(((double)entity.getRandom().nextFloat()- 0.5) * 0.3, d, 0.6);
            vec3d2 = vec3d2.rotateX(-entity.getPitch() * ((float)Math.PI / 180));
            vec3d2 = vec3d2.rotateY(-entity.getHeadYaw() * ((float)Math.PI / 180));
            vec3d2 = vec3d2.add(entity.getX(), entity.getEyeY(), entity.getZ());
            ((ServerWorld) player.getWorld()).spawnParticles(
                    new ItemStackParticleEffect(ParticleTypes.ITEM, stack),
                    vec3d2.x, vec3d2.y, vec3d2.z, 0,
                    vec3d.x, vec3d.y + 0.05, vec3d.z, 1.0);

        }
    }

}
