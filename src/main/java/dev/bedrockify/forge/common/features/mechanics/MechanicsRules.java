package dev.bedrockify.forge.common.features.mechanics;

import dev.bedrockify.forge.Bedrockify;
import net.minecraft.entity.player.PlayerEntity;

/** Server rules are authoritative; a remote server's attack rule is temporary. */
public final class MechanicsRules {
    private static volatile Boolean remoteAttackRule;
    private static volatile Boolean remoteSharpnessRule;
    private MechanicsRules() {}
    public static boolean noAttackCooldown(PlayerEntity player) {
        if (player.getWorld().isClient && remoteAttackRule != null) return remoteAttackRule;
        return Bedrockify.getInstance().settings.bedrockNoAttackCooldown;
    }
    public static void acceptServerRule(boolean enabled) { remoteAttackRule = enabled; }
    public static boolean bedrockSharpness() {
        if (net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient() && remoteSharpnessRule!=null)
            return remoteSharpnessRule;
        return Bedrockify.getInstance().settings.bedrockSharpness;
    }
    public static void acceptServerRules(boolean attack, boolean sharpness) {
        remoteAttackRule=attack; remoteSharpnessRule=sharpness;
    }
    public static void clearServerRule() { remoteAttackRule = null; remoteSharpnessRule=null; }
    public static int regenerationInterval() {
        return Math.max(1, Math.min(1200, Bedrockify.getInstance().settings.regenerationIntervalTicks));
    }
}
