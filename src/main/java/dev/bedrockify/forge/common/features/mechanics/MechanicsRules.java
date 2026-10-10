package dev.bedrockify.forge.common.features.mechanics;

import dev.bedrockify.forge.Bedrockify;
import net.minecraft.entity.player.PlayerEntity;

/** Server rules are authoritative; a remote server's attack rule is temporary. */
public final class MechanicsRules {
    private static volatile Boolean remoteAttackRule;
    private MechanicsRules() {}
    public static boolean noAttackCooldown(PlayerEntity player) {
        if (player.getWorld().isClient && remoteAttackRule != null) return remoteAttackRule;
        return Bedrockify.getInstance().settings.bedrockNoAttackCooldown;
    }
    public static void acceptServerRule(boolean enabled) { remoteAttackRule = enabled; }
    public static void clearServerRule() { remoteAttackRule = null; }
    public static int regenerationInterval() {
        return Math.max(1, Math.min(1200, Bedrockify.getInstance().settings.regenerationIntervalTicks));
    }
}
