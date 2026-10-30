package dev.bedrockify.forge.tests;

import com.mojang.authlib.GameProfile;
import dev.bedrockify.forge.common.features.achievements.BedrockAchievements;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public final class SurvivalAchievementGameTests {
    private static ServerPlayerEntity player(TestContext context, String name) {
        var world = context.getWorld();
        return new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), name));
    }
    private static boolean granted(ServerPlayerEntity player, String path) {
        var advancement = player.server.getAdvancementLoader().get(new Identifier("bedrockify", "bedrock/" + path));
        return player.getAdvancementTracker().getProgress(advancement).isDone();
    }
    private static void tick(ServerPlayerEntity player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }
    private static void finish(ServerPlayerEntity player, net.minecraft.item.Item item) {
        MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, new ItemStack(item), 0, ItemStack.EMPTY));
    }
    private static void logout(ServerPlayerEntity player) {
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
    }
    @GameTest(templateName="empty")
    public static void foodSnapshotsKelpPersistenceAndLogout(TestContext context) {
        var first = player(context, "BRFood");
        var other = player(context, "BROtherFood");
        first.getHungerManager().setFoodLevel(0);
        MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Start(first, new ItemStack(Items.ROTTEN_FLESH), 32));
        finish(other, Items.ROTTEN_FLESH);
        context.assertTrue(!granted(other, "iron_belly"), "Another player cannot inherit the starving-at-start snapshot");
        first.getHungerManager().setFoodLevel(4);
        finish(first, Items.ROTTEN_FLESH);
        context.assertTrue(granted(first, "iron_belly"), "Food completion uses the starting hunger level");
        other.getHungerManager().setFoodLevel(0);
        MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Start(other, new ItemStack(Items.ROTTEN_FLESH), 32));
        logout(other); finish(other, Items.ROTTEN_FLESH);
        context.assertTrue(!granted(other, "iron_belly"), "Logout clears the eating-start snapshot");
        finish(first, Items.DRIED_KELP);
        var data = first.getPersistentData().getCompound("PlayerPersisted");
        long started = data.getLong("BedrockifyKelpOnlySince");
        finish(first, Items.DRIED_KELP);
        context.assertTrue(first.getPersistentData().getCompound("PlayerPersisted").getLong("BedrockifyKelpOnlySince") == started, "Kelp-only progress retains its existing start time");
        data.putLong("BedrockifyKelpOnlySince", context.getWorld().getTime() - 72_000);
        first.getPersistentData().put("PlayerPersisted", data);
        tick(first);
        context.assertTrue(granted(first, "castaway"), "Three game days of persisted kelp progress grant Castaway");
        finish(first, Items.BREAD);
        context.assertTrue(!first.getPersistentData().getCompound("PlayerPersisted").contains("BedrockifyKelpOnlySince"), "Other food resets the existing kelp progress key");
        logout(first); logout(other); context.complete();
    }
    @GameTest(templateName="empty")
    public static void equipmentTimeAndDyeProgress(TestContext context) {
        var player = player(context, "BRProgress");
        player.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        player.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        tick(player);
        context.assertTrue(!granted(player, "iron_man"), "An incomplete iron set does not grant Iron Man");
        player.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        player.getStatHandler().setStat(player, Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME), 2_400_000);
        tick(player);
        context.assertTrue(granted(player, "iron_man") && granted(player, "passing_the_time"), "Full iron equipment and one hundred days grant their achievements");
        BedrockAchievements.leatherArmorDyedInCauldron(player, new ItemStack(Items.LEATHER_HELMET));
        BedrockAchievements.leatherArmorDyedInCauldron(player, new ItemStack(Items.LEATHER_HELMET));
        BedrockAchievements.leatherArmorDyedInCauldron(player, new ItemStack(Items.LEATHER_CHESTPLATE));
        BedrockAchievements.leatherArmorDyedInCauldron(player, new ItemStack(Items.LEATHER_LEGGINGS));
        context.assertTrue(!granted(player, "tie_dye_outfit"), "Repeating one armor part cannot substitute for the missing part");
        BedrockAchievements.leatherArmorDyedInCauldron(player, new ItemStack(Items.LEATHER_BOOTS));
        context.assertTrue(granted(player, "tie_dye_outfit") && player.getPersistentData().getCompound("PlayerPersisted").getInt("BedrockifyCauldronDyedArmor") == 15, "The public Mixin entry retains the existing armor bit mask");
        logout(player); context.complete();
    }
}
