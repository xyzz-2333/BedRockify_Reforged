package dev.bedrockify.forge.common.features.achievements;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import static dev.bedrockify.forge.common.features.achievements.BedrockAchievements.grant;

/** Movement checks own their transient state; the Forge event adapter stays in the facade. */
final class MovementAchievements {
    private static final int TWO_MINUTES = 2 * 60 * 20, GAME_DAY = 24_000;
    private static final Map<EnderPearlEntity, Vec3d> PEARL_STARTS = new WeakHashMap<>();
    private static final Map<UUID, State> PLAYERS = new HashMap<>();
    private record RailRide(UUID cart, Vec3d origin) {}
    private record SlimeBounce(double originY, long expiresAt) {}
    private static final class State {
        int submergedTicks;
        RailRide railRide;
        SlimeBounce bounce;
    }
    private MovementAchievements() {}
    static void logout(UUID player) { PLAYERS.remove(player); }
    static void register() {
        MinecraftForge.EVENT_BUS.addListener(MovementAchievements::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(MovementAchievements::onEnderPearlTeleport);
    }
    static void clear() { PLAYERS.clear(); PEARL_STARTS.clear(); }
    static void slimeBounce(Entity entity) {
        if (entity instanceof ServerPlayerEntity player && player.getVelocity().y > 0.0)
            PLAYERS.computeIfAbsent(player.getUuid(), ignored -> new State()).bounce =
                    new SlimeBounce(player.getY(), player.getWorld().getTime() + 200);
    }

    static void tick(ServerPlayerEntity player) {
        State state = PLAYERS.computeIfAbsent(player.getUuid(), ignored -> new State());
        SlimeBounce bounce = state.bounce;
        if (bounce != null) {
            if (player.getY() - bounce.originY() >= 30.0) {
                grant(player, "trampoline");
                state.bounce = null;
            } else if (player.getWorld().getTime() >= bounce.expiresAt() || player.isOnGround()) {
                state.bounce = null;
            }
        }
        if (player.isUsingRiptide()) grant(player, "do_a_barrel_roll");
        if (player.isFallFlying() && player.getVelocity().lengthSquared() > 4.0
                && fliesThroughOneBlockOpening(player))
            grant(player, "super_sonic");
        if (player.isSubmergedInWater()) {
            int ticks = ++state.submergedTicks;
            if (ticks >= TWO_MINUTES) grant(player, "free_diver");
            if (ticks >= GAME_DAY) grant(player, "sleep_with_the_fishes");
        } else {
            state.submergedTicks = 0;
        }

        if (player.getVehicle() instanceof AbstractMinecartEntity cart) {
            RailRide ride = state.railRide;
            if (ride == null || !ride.cart().equals(cart.getUuid())) {
                ride = new RailRide(cart.getUuid(), player.getPos());
                state.railRide = ride;
            }
            double dx = player.getX() - ride.origin().x;
            double dz = player.getZ() - ride.origin().z;
            if (dx * dx + dz * dz >= 500.0 * 500.0) grant(player, "on_a_rail");
        } else {
            state.railRide = null;
        }

    }

    private static boolean fliesThroughOneBlockOpening(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos center = player.getBlockPos();
        if (!world.getBlockState(center).isAir()) return false;
        Vec3d speed = player.getVelocity();
        double x = Math.abs(speed.x), y = Math.abs(speed.y), z = Math.abs(speed.z);
        if (x >= y && x >= z)
            return enclosedByFullBlocks(world, center.up(), center.down(),
                    center.north(), center.south());
        if (z >= x && z >= y)
            return enclosedByFullBlocks(world, center.up(), center.down(),
                    center.east(), center.west());
        return enclosedByFullBlocks(world, center.north(), center.south(),
                center.east(), center.west());
    }

    private static boolean enclosedByFullBlocks(ServerWorld world, BlockPos a, BlockPos b,
                                                BlockPos c, BlockPos d) {
        return world.getBlockState(a).isOpaqueFullCube(world, a)
                && world.getBlockState(b).isOpaqueFullCube(world, b)
                && world.getBlockState(c).isOpaqueFullCube(world, c)
                && world.getBlockState(d).isOpaqueFullCube(world, d);
    }
    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof EnderPearlEntity pearl && !pearl.getWorld().isClient)
            PEARL_STARTS.put(pearl, pearl.getPos());
    }

    private static void onEnderPearlTeleport(EntityTeleportEvent.EnderPearl event) {
        Vec3d origin = PEARL_STARTS.remove(event.getPearlEntity());
        if (origin == null) return;
        if (origin.squaredDistanceTo(event.getTargetX(), event.getTargetY(), event.getTargetZ()) > 100.0 * 100.0)
            grant(event.getPlayer(), "beam_me_up");
    }
}
