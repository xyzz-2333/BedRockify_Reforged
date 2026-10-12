package dev.bedrockify.forge.common.features.mechanics;

import dev.bedrockify.forge.Bedrockify;
import net.minecraft.entity.player.PlayerEntity;

/** Server rules are authoritative; a remote server's attack rule is temporary. */
public final class MechanicsRules {
    private static volatile Boolean remoteAttackRule;
    private static volatile Boolean remoteProjectileRule;
    public static boolean projectiles(net.minecraft.world.World world) {
        return world.isClient && remoteProjectileRule != null ? remoteProjectileRule : Bedrockify.getInstance().settings.bedrockProjectiles;
    }
    public static void acceptProjectileRule(boolean enabled) { remoteProjectileRule = enabled; }
    private MechanicsRules() {}
    public static boolean noAttackCooldown(PlayerEntity player) {
        if (player.getWorld().isClient && remoteAttackRule != null) return remoteAttackRule;
        return Bedrockify.getInstance().settings.bedrockNoAttackCooldown;
    }
    public static void acceptServerRule(boolean enabled) { remoteAttackRule = enabled; }
    public static void clearServerRule() { remoteAttackRule = null; remoteProjectileRule = null; }
    public static int regenerationInterval() {
        return Math.max(1, Math.min(1200, Bedrockify.getInstance().settings.regenerationIntervalTicks));
    }
}
