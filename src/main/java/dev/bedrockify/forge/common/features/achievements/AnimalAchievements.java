package dev.bedrockify.forge.common.features.achievements;

import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static dev.bedrockify.forge.common.features.achievements.BedrockAchievements.grant;
import static dev.bedrockify.forge.common.features.achievements.BedrockAchievements.incrementPersisted;

/** Deferred animal checks and their player-scoped lifecycle. Persistent progress keys are unchanged. */
final class AnimalAchievements {
    private record PendingDolphin(DolphinEntity dolphin, long expiresAt) {}
    private static final class State {
        final List<TameableEntity> tames = new ArrayList<>();
        PendingDolphin dolphin;
    }
    private static final Map<UUID, State> PLAYERS = new HashMap<>();
    private AnimalAchievements() {}
    static void register() {
        MinecraftForge.EVENT_BUS.addListener(AnimalAchievements::onAnimalTame);
        MinecraftForge.EVENT_BUS.addListener(AnimalAchievements::onEntityInteract);
    }
    static void clear() { PLAYERS.clear(); }
    static void logout(UUID player) { PLAYERS.remove(player); }
    private static State pending(ServerPlayerEntity player) { return PLAYERS.computeIfAbsent(player.getUuid(), ignored -> new State()); }
    static void tick(ServerPlayerEntity player) {
        State state = PLAYERS.get(player.getUuid());
        if (state != null) {
            List<TameableEntity> tames = List.copyOf(state.tames);
            state.tames.clear();
            for (TameableEntity animal : tames) {
                if (!player.getUuid().equals(animal.getOwnerUuid())) continue;
                if (animal instanceof WolfEntity) incrementPersisted(player, "BedrockifyWolvesTamed", 5, "leader_of_the_pack");
                else if (animal instanceof CatEntity) incrementPersisted(player, "BedrockifyCatsTamed", 20, "plethora_of_cats");
            }
            PendingDolphin pendingDolphin = state.dolphin;
            if (pendingDolphin != null) {
                DolphinEntity dolphin = pendingDolphin.dolphin();
                if (dolphin.hasFish() && !dolphin.getTreasurePos().equals(BlockPos.ORIGIN)) {
                    grant(player, "echolocation");
                    state.dolphin = null;
                } else if (!dolphin.isAlive() || player.getWorld().getTime() >= pendingDolphin.expiresAt()) state.dolphin = null;
            }
            if (state.tames.isEmpty() && state.dolphin == null) PLAYERS.remove(player.getUuid(), state);
        }
        if (player.age % 20 == 0) checkCaravan(player);
    }
    private static void checkCaravan(ServerPlayerEntity player) {
        int caravan = 0;
        for (LlamaEntity llama : player.getServerWorld().getEntitiesByClass(LlamaEntity.class,
                player.getBoundingBox().expand(32), entity -> true)) {
            LlamaEntity head = llama;
            for (int depth = 0; depth < 10 && head.isFollowing(); depth++)
                head = head.getFollowing();
            if (head.isLeashed() && head.getHoldingEntity() == player) caravan++;
        }
        if (caravan >= 5) grant(player, "so_i_got_that_going_for_me");
    }

    private static void onAnimalTame(AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayerEntity player)
                || !(event.getAnimal() instanceof TameableEntity animal)) return;
        if (animal instanceof WolfEntity || animal instanceof CatEntity)
            pending(player).tames.add(animal);
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        if (!(event.getTarget() instanceof DolphinEntity dolphin)) return;
        if (event.getItemStack().isOf(Items.COD) || event.getItemStack().isOf(Items.SALMON)
                || event.getItemStack().isOf(Items.TROPICAL_FISH)
                || event.getItemStack().isOf(Items.PUFFERFISH))
            pending(player).dolphin = new PendingDolphin(dolphin, player.getWorld().getTime() + 120);
    }
}
