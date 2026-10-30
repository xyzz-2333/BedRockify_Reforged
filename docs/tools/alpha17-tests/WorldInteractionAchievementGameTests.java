package dev.bedrockify.forge.tests;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ConduitBlockEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public final class WorldInteractionAchievementGameTests {
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
    private static void tick(ServerPlayerEntity player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }
    private static void logout(ServerPlayerEntity player) {
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
    }
    private static void placed(TestContext context, ServerPlayerEntity player, BlockPos pos) {
        var world = context.getWorld();
        MinecraftForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(
                BlockSnapshot.create(world.getRegistryKey(), world, pos), Blocks.AIR.getDefaultState(), player));
    }
    @GameTest(templateName="empty")
    public static void blockEventsCheckMaterialAndEnchantingAccess(TestContext context) {
        var world = context.getWorld();
        var player = player(context, "BRBuilder");
        var table = context.getAbsolutePos(new BlockPos(1, 2, 1));
        var shelf = table.east(2);
        try {
            MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, table, Blocks.STONE.getDefaultState(), player));
            context.assertTrue(!granted(player, "getting_wood"), "Non-log materials do not grant Getting Wood");
            MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, table, Blocks.OAK_LOG.getDefaultState(), player));
            context.assertTrue(granted(player, "getting_wood"), "The log tag grants Getting Wood through the block event");
            world.setBlockState(shelf, Blocks.BOOKSHELF.getDefaultState(), 2);
            placed(context, player, shelf);
            context.assertTrue(!granted(player, "librarian"), "A bookshelf without an enchanting table does not count");
            world.setBlockState(table, Blocks.ENCHANTING_TABLE.getDefaultState(), 2);
            world.setBlockState(table.east(), Blocks.STONE.getDefaultState(), 2);
            placed(context, player, shelf);
            context.assertTrue(!granted(player, "librarian"), "A blocked bookshelf does not supply enchanting power");
            world.setBlockState(table.east(), Blocks.AIR.getDefaultState(), 2);
            placed(context, player, shelf);
            context.assertTrue(granted(player, "librarian"), "An accessible bookshelf grants Librarian");
            context.complete();
        } finally {
            world.setBlockState(table, Blocks.AIR.getDefaultState(), 2);
            world.setBlockState(shelf, Blocks.AIR.getDefaultState(), 2);
            world.setBlockState(table.east(), Blocks.AIR.getDefaultState(), 2);
            logout(player);
        }
    }
    @GameTest(templateName="empty", tickLimit=100)
    public static void conduitActivationIsDeferredAndLogoutClearsPending(TestContext context) {
        var world = context.getWorld();
        var player = player(context, "BRConduit");
        var departed = player(context, "BRLeftConduit");
        var center = context.getAbsolutePos(new BlockPos(3, 4, 3));
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            boolean inner = Math.abs(x) <= 1 && Math.abs(y) <= 1 && Math.abs(z) <= 1;
            world.setBlockState(center.add(x, y, z), (inner ? Blocks.WATER : Blocks.PRISMARINE).getDefaultState(), 2);
        }
        world.setBlockState(center, Blocks.CONDUIT.getDefaultState(), 2);
        placed(context, player, center); placed(context, departed, center);
        tick(player);
        context.assertTrue(!granted(player, "moskstraumen"), "Placing a conduit waits for real activation");
        logout(departed);
        context.runAtTick(45, () -> {
            try {
                context.assertTrue(world.getBlockEntity(center) instanceof ConduitBlockEntity conduit && conduit.isActive(), "The native conduit tick activates the complete underwater frame");
                tick(player); tick(departed);
                context.assertTrue(granted(player, "moskstraumen") && !granted(departed, "moskstraumen"), "Only the player with a pending activation check is credited");
                context.complete();
            } finally {
                for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++)
                    world.setBlockState(center.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                logout(player); logout(departed);
            }
        });
    }
    @GameTest(templateName="empty")
    public static void pearlDistanceUsesRecordedLaunchAndConsumesIt(TestContext context) {
        var world = context.getWorld();
        var player = player(context, "BRPearl");
        var untrackedPlayer = player(context, "BRNoPearl");
        var pearl = new EnderPearlEntity(world, player);
        pearl.setPosition(0, 64, 0);
        MinecraftForge.EVENT_BUS.post(new EntityJoinLevelEvent(pearl, world));
        MinecraftForge.EVENT_BUS.post(new EntityTeleportEvent.EnderPearl(player, 100, 64, 0, pearl, 5, null));
        context.assertTrue(!granted(player, "beam_me_up"), "Exactly 100 blocks does not exceed the original threshold");
        MinecraftForge.EVENT_BUS.post(new EntityTeleportEvent.EnderPearl(player, 101, 64, 0, pearl, 5, null));
        context.assertTrue(!granted(player, "beam_me_up"), "A teleport consumes its recorded launch position");
        MinecraftForge.EVENT_BUS.post(new EntityJoinLevelEvent(pearl, world));
        MinecraftForge.EVENT_BUS.post(new EntityTeleportEvent.EnderPearl(player, 101, 64, 0, pearl, 5, null));
        context.assertTrue(granted(player, "beam_me_up"), "A recorded launch over 100 blocks grants Beam Me Up");
        var untracked = new EnderPearlEntity(world, untrackedPlayer);
        MinecraftForge.EVENT_BUS.post(new EntityTeleportEvent.EnderPearl(untrackedPlayer, 101, 64, 0, untracked, 5, null));
        context.assertTrue(!granted(untrackedPlayer, "beam_me_up"), "Another untracked pearl does not inherit launch state");
        pearl.discard(); untracked.discard(); logout(player); logout(untrackedPlayer);
        context.complete();
    }
}
