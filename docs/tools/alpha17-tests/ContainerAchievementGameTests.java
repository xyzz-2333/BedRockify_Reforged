package dev.bedrockify.forge.tests;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Exercise native slot transactions and event delivery, rather than calling extracted rules directly. */
@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public final class ContainerAchievementGameTests {
    private static ServerPlayerEntity player(TestContext context, String name, BlockPos pos) {
        var world = context.getWorld();
        // Forge deliberately refuses advancement grants to FakePlayer. Use a real owner with discarded packets.
        var player = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), name));
        player.networkHandler = new ServerPlayNetworkHandler(world.getServer(), new ClientConnection(NetworkSide.SERVERBOUND), player) {
            @Override public void sendPacket(Packet<?> packet) {}
            @Override public void sendPacket(Packet<?> packet, PacketCallbacks callbacks) {}
        };
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return player;
    }
    private static boolean done(ServerPlayerEntity player, String name) {
        var achievement = player.server.getAdvancementLoader().get(new Identifier("bedrockify", "bedrock/" + name));
        if (achievement == null) throw new IllegalStateException("Missing test achievement " + name);
        return player.getAdvancementTracker().getProgress(achievement).isDone();
    }

    @GameTest(templateName="empty")
    public static void grindstoneAwardsOnlyAfterActualDisenchantment(TestContext context) {
        var pos = context.getAbsolutePos(new BlockPos(1, 2, 1));
        context.getWorld().setBlockState(pos, Blocks.GRINDSTONE.getDefaultState());
        var player = player(context, "BRGrind", pos);
        var menu = new GrindstoneScreenHandler(1, player.getInventory(), ScreenHandlerContext.create(context.getWorld(), pos));
        player.currentScreenHandler = menu;
        var sword = new ItemStack(Items.IRON_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 1);
        menu.getSlot(0).setStack(sword);
        context.assertTrue(menu.getSlot(2).hasStack(), "Native grindstone publishes a result");
        context.assertFalse(done(player, "disenchanted"), "Preview alone grants nothing");
        menu.setCursorStack(new ItemStack(Items.DIRT, 64));
        menu.onSlotClick(2, 0, SlotActionType.PICKUP, player);
        context.assertFalse(done(player, "disenchanted"), "Blocked result pickup grants nothing");
        menu.setCursorStack(ItemStack.EMPTY);
        menu.onSlotClick(2, 0, SlotActionType.PICKUP, player);
        context.assertTrue(done(player, "disenchanted"), "Successful native disenchantment grants the achievement");

        var other = player(context, "BRCursed", pos);
        var cursedMenu = new GrindstoneScreenHandler(2, other.getInventory(), ScreenHandlerContext.create(context.getWorld(), pos));
        other.currentScreenHandler = cursedMenu;
        var cursed = new ItemStack(Items.IRON_SWORD);
        cursed.addEnchantment(Enchantments.VANISHING_CURSE, 1);
        cursedMenu.getSlot(0).setStack(cursed);
        context.assertTrue(cursedMenu.getSlot(2).hasStack(), "Curse-only result is available");
        cursedMenu.onSlotClick(2, 0, SlotActionType.PICKUP, other);
        context.assertFalse(done(other, "disenchanted"), "Retained curses do not count as removed enchantments");
        context.complete();
    }

    @GameTest(templateName="empty")
    public static void loomAwardsOnlyWhenBannerIngredientsAreConsumed(TestContext context) {
        var pos = context.getAbsolutePos(new BlockPos(1, 2, 1));
        context.getWorld().setBlockState(pos, Blocks.LOOM.getDefaultState());
        var player = player(context, "BRLoom", pos);
        var menu = new LoomScreenHandler(1, player.getInventory(), ScreenHandlerContext.create(context.getWorld(), pos));
        player.currentScreenHandler = menu;
        menu.getSlot(0).setStack(new ItemStack(Items.WHITE_BANNER));
        menu.getSlot(1).setStack(new ItemStack(Items.RED_DYE));
        menu.getSlot(2).setStack(new ItemStack(Items.MOJANG_BANNER_PATTERN));
        menu.onButtonClick(player, 0);
        context.assertTrue(menu.getSlot(3).hasStack(), "Native loom publishes the Mojang banner result");
        context.assertFalse(done(player, "fruit_on_the_loom"), "Loom preview grants nothing");
        menu.setCursorStack(new ItemStack(Items.DIRT, 64));
        menu.onSlotClick(3, 0, SlotActionType.PICKUP, player);
        context.assertFalse(done(player, "fruit_on_the_loom"), "Blocked banner pickup grants nothing");
        menu.setCursorStack(ItemStack.EMPTY);
        menu.onSlotClick(3, 0, SlotActionType.PICKUP, player);
        context.assertTrue(done(player, "fruit_on_the_loom"), "Taking the native banner result grants the achievement");
        context.assertTrue(menu.getSlot(0).getStack().isEmpty() && menu.getSlot(1).getStack().isEmpty()
                && menu.getSlot(2).hasStack(), "Banner and dye are consumed; the pattern remains");
        context.complete();
    }

    @GameTest(templateName="empty")
    public static void furnaceFuelAttributionSurvivesNativeSmeltingAndPickup(TestContext context) {
        var pos = context.getAbsolutePos(new BlockPos(1, 2, 1));
        context.getWorld().setBlockState(pos, Blocks.FURNACE.getDefaultState());
        var furnace = (AbstractFurnaceBlockEntity)context.getWorld().getBlockEntity(pos);
        furnace.setStack(0, new ItemStack(Items.WET_SPONGE));
        furnace.setStack(1, new ItemStack(Items.LAVA_BUCKET));
        for (int tick = 0; tick < 220; tick++)
            AbstractFurnaceBlockEntity.tick(context.getWorld(), pos, context.getWorld().getBlockState(pos), furnace);
        context.assertTrue(furnace.getStack(2).isOf(Items.SPONGE), "Native furnace smelts a wet sponge");
        context.assertTrue(furnace.getStack(1).isOf(Items.WATER_BUCKET), "Wet sponge fills the remaining empty bucket");
        context.assertTrue(furnace.getPersistentData().getString("BedrockifyOutputFuel").equals("lava_bucket"),
                "Tick snapshots attribute the result to lava fuel; actual=" + furnace.getPersistentData());
        var player = player(context, "BRSmelt", pos);
        var menu = new FurnaceScreenHandler(1, player.getInventory(), furnace, new ArrayPropertyDelegate(4));
        player.currentScreenHandler = menu;
        context.assertFalse(done(player, "super_fuel"), "Smelting without pickup does not grant the pickup achievement");
        menu.onSlotClick(2, 0, SlotActionType.PICKUP, player);
        context.assertTrue(done(player, "dry_spell") && done(player, "super_fuel"),
                "Native result pickup delivers the smelting event with its fuel provenance");
        context.complete();
    }

    @GameTest(templateName="empty")
    public static void ordinaryFuelDoesNotGrantSpecialFuelAchievements(TestContext context) {
        var pos = context.getAbsolutePos(new BlockPos(1, 2, 1));
        context.getWorld().setBlockState(pos, Blocks.FURNACE.getDefaultState());
        var furnace = (AbstractFurnaceBlockEntity)context.getWorld().getBlockEntity(pos);
        furnace.setStack(0, new ItemStack(Items.IRON_ORE));
        furnace.setStack(1, new ItemStack(Items.COAL));
        for (int tick = 0; tick < 220; tick++)
            AbstractFurnaceBlockEntity.tick(context.getWorld(), pos, context.getWorld().getBlockState(pos), furnace);
        context.assertTrue(furnace.getStack(2).isOf(Items.IRON_INGOT)
                && furnace.getPersistentData().getString("BedrockifyOutputFuel").equals("other"),
                "Ordinary fuel consumption records its own provenance");
        var player = player(context, "BRCoal", pos);
        var menu = new FurnaceScreenHandler(1, player.getInventory(), furnace, new ArrayPropertyDelegate(4));
        player.currentScreenHandler = menu;
        menu.onSlotClick(2, 0, SlotActionType.PICKUP, player);
        context.assertFalse(done(player, "super_fuel") || done(player, "renewable_energy"),
                "Coal smelting cannot inherit a special fuel achievement");
        context.complete();
    }

    @GameTest(templateName="empty")
    public static void chestThresholdAndInventoryTypeArePreserved(TestContext context) {
        var pos = context.getAbsolutePos(new BlockPos(1, 2, 1));
        context.getWorld().setBlockState(pos, Blocks.CHEST.getDefaultState());
        var chest = (ChestBlockEntity)context.getWorld().getBlockEntity(pos);
        var player = player(context, "BRChest", pos);
        for (int slot = 0; slot < 27; slot++) chest.setStack(slot, new ItemStack(Items.COBBLESTONE, 64));
        chest.getStack(26).decrement(1);
        var menu = GenericContainerScreenHandler.createGeneric9x3(1, player.getInventory(), chest);
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, menu));
        context.assertFalse(done(player, "chestful_of_cobblestone"), "1727 cobblestone is below the threshold");
        chest.getStack(26).increment(1);
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, menu));
        context.assertTrue(done(player, "chestful_of_cobblestone"), "1728 cobblestone in a chest grants the achievement");
        var other = player(context, "BRNotChest", pos);
        var inventory = new SimpleInventory(27);
        for (int slot = 0; slot < 27; slot++) inventory.setStack(slot, new ItemStack(Items.COBBLESTONE, 64));
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(other,
                GenericContainerScreenHandler.createGeneric9x3(2, other.getInventory(), inventory)));
        context.assertFalse(done(other, "chestful_of_cobblestone"), "A generic inventory is not treated as a chest");
        context.complete();
    }
}
