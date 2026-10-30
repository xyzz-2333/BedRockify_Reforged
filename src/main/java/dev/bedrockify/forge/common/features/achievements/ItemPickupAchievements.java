package dev.bedrockify.forge.common.features.achievements;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import static dev.bedrockify.forge.common.features.achievements.BedrockAchievements.grant;

/** Item provenance and deferred pickup confirmation for gifts and campfire output. */
final class ItemPickupAchievements {
    private record Source(boolean campfireCod, UUID catOwner) {}
    private record PendingPickup(ItemEntity item, int originalCount, UUID recipient, String achievement) {
        boolean wasPickedUp() { return item.isRemoved() || item.getStack().getCount() < originalCount; }
    }
    private static final Map<ItemEntity, Source> SOURCES = new WeakHashMap<>();
    private static final Map<UUID, List<PendingPickup>> PLAYERS = new HashMap<>();
    private static final ThreadLocal<Boolean> SPAWNING_CAMPFIRE_COD = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<UUID> SPAWNING_CAT_GIFT = new ThreadLocal<>();

    private ItemPickupAchievements() {}
    static void register() {
        MinecraftForge.EVENT_BUS.addListener(ItemPickupAchievements::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(ItemPickupAchievements::onItemPickup);
    }
    static void clear() {
        SOURCES.clear();
        PLAYERS.clear();
        SPAWNING_CAMPFIRE_COD.remove();
        SPAWNING_CAT_GIFT.remove();
    }
    static void logout(UUID player) { PLAYERS.remove(player); }
    static void startCatMorningGift(PlayerEntity owner) {
        SPAWNING_CAT_GIFT.set(owner == null ? null : owner.getUuid());
    }
    static void endCatMorningGift() { SPAWNING_CAT_GIFT.remove(); }
    static void spawnCampfireOutput(World world, double x, double y, double z, ItemStack stack) {
        boolean previous = SPAWNING_CAMPFIRE_COD.get();
        if (stack.isOf(Items.COOKED_COD)) SPAWNING_CAMPFIRE_COD.set(true);
        try {
            ItemScatterer.spawn(world, x, y, z, stack);
        } finally {
            SPAWNING_CAMPFIRE_COD.set(previous);
        }
    }
    static void tick(ServerPlayerEntity receiver) {
        List<PendingPickup> pickups = PLAYERS.remove(receiver.getUuid());
        if (pickups == null) return;
        for (PendingPickup pickup : pickups) {
            if (!pickup.wasPickedUp()) continue;
            ServerPlayerEntity recipient = receiver.getUuid().equals(pickup.recipient()) ? receiver
                    : receiver.server.getPlayerManager().getPlayer(pickup.recipient());
            if (recipient != null) grant(recipient, pickup.achievement());
        }
    }
    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)) return;
        boolean cod = SPAWNING_CAMPFIRE_COD.get() && item.getStack().isOf(Items.COOKED_COD);
        UUID owner = SPAWNING_CAT_GIFT.get();
        if (cod || owner != null) SOURCES.put(item, new Source(cod, owner));
    }
    private static void pending(ServerPlayerEntity receiver, ItemEntity item, UUID recipient, String achievement) {
        PLAYERS.computeIfAbsent(receiver.getUuid(), ignored -> new ArrayList<>())
                .add(new PendingPickup(item, item.getStack().getCount(), recipient, achievement));
    }
    private static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity receiver)) return;
        ItemEntity item = event.getItem();
        Source source = SOURCES.get(item);
        if (source != null) {
            if (source.campfireCod()) pending(receiver, item, receiver.getUuid(), "delicious_fish");
            if (receiver.getUuid().equals(source.catOwner())) pending(receiver, item, receiver.getUuid(), "where_have_you_been");
        }
        String achievement = item.getStack().isOf(Items.DIAMOND) ? "diamonds_to_you"
                : item.getStack().isOf(Items.SUSPICIOUS_STEW) ? "time_for_stew" : null;
        if (achievement == null) return;
        NbtCompound itemData = item.writeNbt(new NbtCompound());
        if (!itemData.containsUuid("Thrower")) return;
        UUID thrower = itemData.getUuid("Thrower");
        if (!receiver.getUuid().equals(thrower)) pending(receiver, item, thrower, achievement);
    }
}
