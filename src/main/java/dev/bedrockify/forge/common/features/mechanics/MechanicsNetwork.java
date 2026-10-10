package dev.bedrockify.forge.common.features.mechanics;

import dev.bedrockify.forge.Bedrockify;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;
import java.util.function.Supplier;

public final class MechanicsNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new Identifier(Bedrockify.MOD_ID, "mechanics"), () -> "1", "1"::equals, "1"::equals);
    private static Boolean lastBroadcast;
    private MechanicsNetwork() {}
    public static void register() {
        CHANNEL.registerMessage(0, AttackRule.class, AttackRule::encode, AttackRule::decode,
                AttackRule::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayerEntity player)
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), current());
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) return;
            boolean value = current().enabled();
            if (lastBroadcast == null || lastBroadcast != value) {
                CHANNEL.send(PacketDistributor.ALL.noArg(), new AttackRule(value));
                lastBroadcast = value;
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> lastBroadcast = null);
    }
    private static AttackRule current() {
        return new AttackRule(Bedrockify.getInstance().settings.bedrockNoAttackCooldown
                && dev.bedrockify.forge.mixin.featureManager.MixinFeatureManager.features
                .getOrDefault("common.features.mechanics", true));
    }
    private record AttackRule(boolean enabled) {
        private void encode(PacketByteBuf buffer) { buffer.writeBoolean(enabled); }
        private static AttackRule decode(PacketByteBuf buffer) { return new AttackRule(buffer.readBoolean()); }
        private void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> MechanicsRules.acceptServerRule(enabled));
            context.setPacketHandled(true);
        }
    }
}
