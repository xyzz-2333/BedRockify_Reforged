package dev.bedrockify.forge.common.features.achievements;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.bedrockify.forge.common.features.achievements.BedrockAchievements.grant;

/** Food, equipment and play-time progress; owns the eating-start snapshot. */
final class SurvivalAchievements {
    private static final int GAME_DAY = 24_000;
    private static final int ONE_HUNDRED_DAYS = 100 * GAME_DAY;
    private static final Map<UUID, Boolean> STARVING_WHEN_EATING = new HashMap<>();
    private SurvivalAchievements() {}
    static void register() {
        MinecraftForge.EVENT_BUS.addListener(SurvivalAchievements::onItemUseStart);
        MinecraftForge.EVENT_BUS.addListener(SurvivalAchievements::onItemUseFinish);
    }
    static void clear() { STARVING_WHEN_EATING.clear(); }
    static void logout(UUID player) { STARVING_WHEN_EATING.remove(player); }
    static void tick(ServerPlayerEntity player) {
        if (player.age % 20 == 0) {
            if (player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.IRON_HELMET)
                    && player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.IRON_CHESTPLATE)
                    && player.getEquippedStack(EquipmentSlot.LEGS).isOf(Items.IRON_LEGGINGS)
                    && player.getEquippedStack(EquipmentSlot.FEET).isOf(Items.IRON_BOOTS))
                grant(player, "iron_man");
            if (player.getStatHandler().getStat(Stats.CUSTOM, Stats.PLAY_TIME) >= ONE_HUNDRED_DAYS)
                grant(player, "passing_the_time");
            NbtCompound castaway = player.getPersistentData().getCompound("PlayerPersisted");
            long kelpStarted = castaway.getLong("BedrockifyKelpOnlySince");
            if (castaway.contains("BedrockifyKelpOnlySince")
                    && player.getWorld().getTime() - kelpStarted >= 3L * GAME_DAY)
                grant(player, "castaway");
        }
    }

    static void leatherArmorDyedInCauldron(PlayerEntity player, ItemStack original) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        int bit = original.isOf(Items.LEATHER_HELMET) ? 1
                : original.isOf(Items.LEATHER_CHESTPLATE) ? 2
                : original.isOf(Items.LEATHER_LEGGINGS) ? 4
                : original.isOf(Items.LEATHER_BOOTS) ? 8 : 0;
        if (bit == 0) return;
        NbtCompound playerData = serverPlayer.getPersistentData();
        NbtCompound persisted = playerData.getCompound("PlayerPersisted");
        int mask = persisted.getInt("BedrockifyCauldronDyedArmor") | bit;
        persisted.putInt("BedrockifyCauldronDyedArmor", mask);
        playerData.put("PlayerPersisted", persisted);
        if (mask == 15) grant(serverPlayer, "tie_dye_outfit");
    }
    private static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            STARVING_WHEN_EATING.put(player.getUuid(),
                    event.getItem().isOf(Items.ROTTEN_FLESH)
                            && player.getHungerManager().getFoodLevel() == 0);
        }
    }
    private static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            if (event.getItem().isOf(Items.ROTTEN_FLESH)
                    && STARVING_WHEN_EATING.getOrDefault(player.getUuid(), false))
                grant(player, "iron_belly");
            if (event.getItem().isFood()) {
                NbtCompound playerData = player.getPersistentData();
                NbtCompound persisted = playerData.getCompound("PlayerPersisted");
                if (event.getItem().isOf(Items.DRIED_KELP)) {
                    if (!persisted.contains("BedrockifyKelpOnlySince"))
                        persisted.putLong("BedrockifyKelpOnlySince", player.getWorld().getTime());
                } else {
                    persisted.remove("BedrockifyKelpOnlySince");
                }
                playerData.put("PlayerPersisted", persisted);
            }
        }
        STARVING_WHEN_EATING.remove(event.getEntity().getUuid());
    }
}
