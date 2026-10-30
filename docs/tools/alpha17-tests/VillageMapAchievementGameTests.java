package dev.bedrockify.forge.tests;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.Arrays;
import java.util.UUID;

@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public final class VillageMapAchievementGameTests {
    private static ServerPlayerEntity player(TestContext context, String name) {
        var world = context.getWorld();
        var player = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), name));
        player.age = 1;
        return player;
    }
    private static boolean granted(ServerPlayerEntity player, String path) {
        var advancement = player.server.getAdvancementLoader().get(new Identifier("bedrockify", "bedrock/" + path));
        return player.getAdvancementTracker().getProgress(advancement).isDone();
    }
    private static NbtCompound progress(ServerPlayerEntity player) {
        return player.getPersistentData().getCompound("PlayerPersisted");
    }
    private static void trade(ServerPlayerEntity player, VillagerEntity villager, ItemStack first, ItemStack second, ItemStack sold) {
        MinecraftForge.EVENT_BUS.post(new TradeWithVillagerEvent(player, new TradeOffer(first, second, sold, 10, 1, 0), villager));
    }
    private static void tick(ServerPlayerEntity player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }
    private static void logout(ServerPlayerEntity player) {
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
    }
    @GameTest(templateName="empty")
    public static void tradeTotalsThresholdsAndIsolation(TestContext context) {
        var first = player(context, "BRTrade");
        var other = player(context, "BROtherTrade");
        var villager = EntityType.VILLAGER.create(context.getWorld());
        var persisted = new NbtCompound();
        persisted.putInt("BedrockifyEmeraldsEarned", 998);
        persisted.putInt("BedrockifyEmeraldsTraded", 28);
        persisted.putString("UnrelatedProgress", "preserved");
        first.getPersistentData().put("PlayerPersisted", persisted);
        trade(first, villager, new ItemStack(Items.WHEAT, 20), ItemStack.EMPTY, new ItemStack(Items.EMERALD));
        context.assertTrue(!granted(first, "master_trader") && !granted(first, "the_haggler"), "Below-threshold sales do not grant achievements");
        trade(first, villager, new ItemStack(Items.WHEAT, 20), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 4));
        context.assertTrue(granted(first, "master_trader") && granted(first, "the_haggler"), "Crossing both thresholds grants both achievements");
        context.assertTrue(progress(first).getInt("BedrockifyEmeraldsEarned") == 1000
                && progress(first).getInt("BedrockifyEmeraldsTraded") == 30, "Progress saturates at its thresholds");
        context.assertTrue(progress(first).getString("UnrelatedProgress").equals("preserved"), "Trade progress retains unrelated persisted data");
        trade(other, villager, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.EMERALD, 3), new ItemStack(Items.BREAD));
        context.assertTrue(progress(other).getInt("BedrockifyEmeraldsTraded") == 5
                && progress(other).getInt("BedrockifyEmeraldsEarned") == 0, "Both buy slots count as spent, independently of earned progress");
        context.assertTrue(!granted(other, "buy_low_sell_high") && !granted(other, "master_trader"), "Other player does not inherit achievements");
        trade(other, villager, new ItemStack(Items.EMERALD), new ItemStack(Items.WHEAT), new ItemStack(Items.BREAD));
        context.assertTrue(!granted(other, "buy_low_sell_high"), "One emerald plus another payment is not a one-emerald deal");
        trade(other, villager, new ItemStack(Items.EMERALD), ItemStack.EMPTY, new ItemStack(Items.BREAD));
        context.assertTrue(granted(other, "buy_low_sell_high"), "A one-emerald deal grants the discount achievement");
        logout(first); logout(other);
        context.complete();
    }

    @GameTest(templateName="empty")
    public static void cartographerMapProvenanceAndDeduplication(TestContext context) {
        var first = player(context, "BRMapBuyer");
        var other = player(context, "BROtherBuyer");
        var villager = EntityType.VILLAGER.create(context.getWorld());
        var map = FilledMapItem.createMap(context.getWorld(), 0, 0, (byte)0, true, false);
        MapState.addDecorationsNbt(map, new BlockPos(320, 64, -256), "+", MapIcon.Type.MONUMENT);
        trade(first, villager, new ItemStack(Items.EMERALD, 2), ItemStack.EMPTY, map);
        context.assertTrue(!progress(first).contains("BedrockifyExplorerMaps"), "Non-cartographers do not record explorer-map provenance");
        villager.setVillagerData(villager.getVillagerData().withProfession(VillagerProfession.CARTOGRAPHER));
        trade(first, villager, new ItemStack(Items.EMERALD, 2), ItemStack.EMPTY, map);
        trade(first, villager, new ItemStack(Items.EMERALD, 2), ItemStack.EMPTY, map);
        var maps = progress(first).getList("BedrockifyExplorerMaps", NbtElement.COMPOUND_TYPE);
        context.assertTrue(maps.size() == 1, "Buying a duplicate map does not duplicate provenance");
        var record = maps.getCompound(0);
        context.assertTrue(record.getInt("id") == FilledMapItem.getMapId(map)
                && record.getDouble("x") == 320 && record.getDouble("z") == -256
                && record.getByte("type") == MapIcon.Type.MONUMENT.getId()
                && record.getString("dimension").equals(context.getWorld().getRegistryKey().getValue().toString()), "Map ID, destination, type and dimension retain the existing save format");
        var ordinary = FilledMapItem.createMap(context.getWorld(), 0, 0, (byte)0, true, false);
        trade(first, villager, new ItemStack(Items.EMERALD, 2), ItemStack.EMPTY, ordinary);
        context.assertTrue(progress(first).getList("BedrockifyExplorerMaps", NbtElement.COMPOUND_TYPE).size() == 1
                && !progress(other).contains("BedrockifyExplorerMaps"), "Ordinary maps are ignored and buyers remain isolated");
        logout(first); logout(other);
        context.complete();
    }

    private static void interact(ServerPlayerEntity player, ItemFrameEntity frame) {
        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.FILLED_MAP));
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, Hand.MAIN_HAND, frame));
    }
    @GameTest(templateName="empty")
    public static void mapRoomRequiresCompleteUniqueAdjacentMapsAndClearsPending(TestContext context) {
        var world = context.getWorld();
        var first = player(context, "BRMapRoom");
        var other = player(context, "BROtherRoom");
        var departed = player(context, "BRLeftRoom");
        var origin = context.getAbsolutePos(new BlockPos(1, 2, 1));
        var frames = new ItemFrameEntity[9];
        var maps = new ItemStack[9];
        try {
            for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                maps[index] = FilledMapItem.createMap(world, col * 128, row * 128, (byte)0, true, false);
                Arrays.fill(FilledMapItem.getMapState(maps[index], world).colors, (byte)1);
                frames[index] = new ItemFrameEntity(world, origin.add(col, row, 0), Direction.SOUTH);
                frames[index].setHeldItemStack(maps[index]);
                world.spawnEntity(frames[index]);
            }
            var last = FilledMapItem.getMapState(maps[8], world);
            last.colors[0] = 0;
            interact(first, frames[4]); tick(first);
            context.assertTrue(!granted(first, "map_room"), "One unexplored map pixel prevents completion");
            last.colors[0] = 1;
            frames[8].setHeldItemStack(maps[0]);
            interact(first, frames[4]); tick(first);
            context.assertTrue(!granted(first, "map_room"), "Repeated map centers cannot complete a map room");
            var distant = FilledMapItem.createMap(world, 1024, 1024, (byte)0, true, false);
            Arrays.fill(FilledMapItem.getMapState(distant, world).colors, (byte)1);
            frames[8].setHeldItemStack(distant);
            interact(first, frames[4]); tick(first);
            context.assertTrue(!granted(first, "map_room"), "Nine explored maps with gaps between centers do not complete a map room");
            var scaled = FilledMapItem.createMap(world, 256, 256, (byte)1, true, false);
            Arrays.fill(FilledMapItem.getMapState(scaled, world).colors, (byte)1);
            frames[8].setHeldItemStack(scaled);
            interact(first, frames[4]); tick(first);
            context.assertTrue(!granted(first, "map_room"), "Mixed map scales do not complete a map room");
            frames[8].setHeldItemStack(maps[8]);
            tick(first);
            context.assertTrue(!granted(first, "map_room"), "A failed pending check is consumed until the next interaction");
            interact(departed, frames[4]); logout(departed); tick(departed);
            context.assertTrue(!granted(departed, "map_room"), "Logout discards pending map-room checks");
            interact(first, frames[4]);
            context.assertTrue(!granted(first, "map_room"), "The interaction waits for frame contents to settle");
            tick(first); tick(other);
            context.assertTrue(granted(first, "map_room") && !granted(other, "map_room"), "A complete room grants only to its interacting player");
            context.complete();
        } finally {
            for (var frame : frames) if (frame != null) frame.discard();
            logout(first); logout(other); logout(departed);
        }
    }
}
