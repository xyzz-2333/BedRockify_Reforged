package dev.bedrockify.forge.common.features.achievements;

import dev.bedrockify.forge.Bedrockify;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class AchievementsNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new Identifier(Bedrockify.MOD_ID, "achievements"), () -> "1", "1"::equals, "1"::equals);

    private AchievementsNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, InventoryOpened.class, InventoryOpened::encode,
                InventoryOpened::decode, InventoryOpened::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void inventoryOpened() {
        CHANNEL.sendToServer(new InventoryOpened());
    }

    private record InventoryOpened() {
        private void encode(PacketByteBuf ignored) {}

        private static InventoryOpened decode(PacketByteBuf ignored) {
            return new InventoryOpened();
        }

        private void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayerEntity sender = context.getSender();
            if (sender != null)
                context.enqueueWork(() -> BedrockAchievements.inventoryOpened(sender));
            context.setPacketHandled(true);
        }
    }
}
