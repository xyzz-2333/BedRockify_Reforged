package dev.bedrockify.forge.common.features.achievements;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.VillagerProfession;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import static dev.bedrockify.forge.common.features.achievements.BedrockAchievements.grant;
import static dev.bedrockify.forge.common.features.achievements.BedrockAchievements.incrementPersisted;

/** Villager trading and alarm confirmation, with world and player session state. */
final class VillageAchievements {
    private static final int ALARM_WINDOW = 1200;
    private static final int ALARM_RADIUS = 32;
    private static final Map<ServerWorld, List<HurtVillager>> HURT_VILLAGERS = new WeakHashMap<>();
    private static final Map<UUID, PendingBell> PENDING_BELLS = new HashMap<>();
    private record HurtVillager(BlockPos pos, long tick) {}
    private record PendingBell(ServerWorld world, BlockPos pos) {}
    private VillageAchievements() {}
    static void register() {
        MinecraftForge.EVENT_BUS.addListener(VillageAchievements::onTrade);
        MinecraftForge.EVENT_BUS.addListener(VillageAchievements::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(VillageAchievements::onRightClickBlock);
    }
    static void clear() { HURT_VILLAGERS.clear(); PENDING_BELLS.clear(); }
    static void logout(UUID player) { PENDING_BELLS.remove(player); }
    static void tick(ServerPlayerEntity player) {
        PendingBell bell = PENDING_BELLS.remove(player.getUuid());
        if (bell != null && bell.world().getBlockEntity(bell.pos()) instanceof BellBlockEntity bellEntity
                && bellEntity.ringing && bell.world().isNearOccupiedPointOfInterest(bell.pos())) {
            List<HurtVillager> hurt = HURT_VILLAGERS.get(bell.world());
            if (hurt != null && hurt.stream().anyMatch(entry ->
                    bell.world().getTime() - entry.tick() <= ALARM_WINDOW
                            && entry.pos().isWithinDistance(bell.pos(), ALARM_RADIUS)))
                grant(player, "sound_the_alarm");
        }
    }
    private static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().getWorld().isClient) return;
        if (event.getEntity() instanceof VillagerEntity villager
                && villager.getWorld() instanceof ServerWorld world && event.getAmount() > 0) {
            List<HurtVillager> hurt = HURT_VILLAGERS.computeIfAbsent(world, ignored -> new ArrayList<>());
            hurt.removeIf(entry -> world.getTime() - entry.tick() > ALARM_WINDOW);
            hurt.add(new HurtVillager(villager.getBlockPos(), world.getTime()));
        }
    }

    private static void onTrade(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        var offer = event.getMerchantOffer();
        if (event.getAbstractVillager() instanceof VillagerEntity villager
                && villager.getVillagerData().getProfession() == VillagerProfession.CARTOGRAPHER)
            MapAchievements.recordExplorerMap(player, offer.getSellItem());
        int earned = offer.getSellItem().isOf(Items.EMERALD) ? offer.getSellItem().getCount() : 0;
        int spent = (offer.getAdjustedFirstBuyItem().isOf(Items.EMERALD)
                ? offer.getAdjustedFirstBuyItem().getCount() : 0)
                + (offer.getSecondBuyItem().isOf(Items.EMERALD)
                ? offer.getSecondBuyItem().getCount() : 0);
        if ((offer.getAdjustedFirstBuyItem().isOf(Items.EMERALD)
                && offer.getAdjustedFirstBuyItem().getCount() == 1
                && offer.getSecondBuyItem().isEmpty())
                || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE))
            grant(player, "buy_low_sell_high");
        if (earned > 0) incrementPersisted(player, "BedrockifyEmeraldsEarned", earned, 1000, "master_trader");
        if (earned + spent > 0) incrementPersisted(player, "BedrockifyEmeraldsTraded", earned + spent, 30, "the_haggler");
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayerEntity player
                && event.getLevel() instanceof ServerWorld world
                && world.getBlockState(event.getPos()).isOf(Blocks.BELL))
            PENDING_BELLS.put(player.getUuid(), new PendingBell(world, event.getPos().toImmutable()));
    }
}
