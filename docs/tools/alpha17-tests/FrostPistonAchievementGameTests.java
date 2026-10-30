package dev.bedrockify.forge.tests;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.FrostWalkerEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public final class FrostPistonAchievementGameTests {
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
    @GameTest(templateName="empty")
    public static void frostWalkerNativeFreezeCancellationAndLogout(TestContext context) {
        var world = context.getWorld();
        var origin = context.getAbsolutePos(new BlockPos(1, 3, 1));
        var chunk = world.getWorldChunk(origin);
        var water = new BlockPos(chunk.getPos().getStartX()+8, origin.getY(), chunk.getPos().getStartZ()+8);
        var sampler = world.getChunkManager().getNoiseConfig().getMultiNoiseSampler();
        var saved = new HashMap<Long, RegistryEntry<Biome>>();
        for(int bx=chunk.getPos().getStartX()>>2; bx<(chunk.getPos().getStartX()>>2)+4; bx++)
            for(int bz=chunk.getPos().getStartZ()>>2; bz<(chunk.getPos().getStartZ()>>2)+4; bz++)
                for(int by=world.getBottomY()>>2; by<world.getTopY()>>2; by++)
                    saved.put(BlockPos.asLong(bx,by,bz), chunk.getBiomeForNoiseGen(bx,by,bz));
        var deepOcean = world.getRegistryManager().get(RegistryKeys.BIOME).entryOf(BiomeKeys.DEEP_OCEAN);
        var first = player(context, "BRFrost");
        var cancelled = player(context, "BRFrostCancel");
        var departed = player(context, "BRFrostLeft");
        var bare = player(context, "BRFrostBare");
        Consumer<BlockEvent.EntityPlaceEvent> cancel = event -> { if (event.getEntity() == cancelled) event.setCanceled(true); };
        MinecraftForge.EVENT_BUS.addListener(cancel);
        var oldWater = world.getBlockState(water);
        var oldAbove = world.getBlockState(water.up());
        try {
            chunk.populateBiomes((bx,by,bz,noise) -> deepOcean, sampler);
            world.setBlockState(water.up(), Blocks.AIR.getDefaultState(), 2);
            for(var player : new ServerPlayerEntity[]{bare, cancelled, departed, first}) {
                world.setBlockState(water, Blocks.WATER.getDefaultState(), 2);
                player.setPosition(water.getX()+0.5, water.getY()+1, water.getZ()+0.5);
                player.setOnGround(true);
                if (player != bare) {
                    var boots = new ItemStack(Items.DIAMOND_BOOTS);
                    boots.addEnchantment(Enchantments.FROST_WALKER, 1);
                    player.equipStack(EquipmentSlot.FEET, boots);
                }
                FrostWalkerEnchantment.freezeWater(player, world, water.up(), 1);
                context.assertTrue(!granted(player, "let_it_go"), "Freeze placement defers the achievement until the player tick");
                if (player == cancelled)
                    context.assertTrue(world.getBlockState(water).isOf(Blocks.WATER), "A cancelled native placement leaves the water unfrozen");
                else context.assertTrue(world.getBlockState(water).isOf(Blocks.FROSTED_ICE), "The native freezing path changed the world to frosted ice");
                if (player == departed) logout(player);
                tick(player);
                context.assertTrue(granted(player, "let_it_go") == (player == first), "Only an equipped player with a completed, retained freeze check receives Let It Go");
            }
            context.complete();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
            chunk.populateBiomes((bx,by,bz,noise) -> saved.get(BlockPos.asLong(bx,by,bz)), sampler);
            world.setBlockState(water, oldWater, 2); world.setBlockState(water.up(), oldAbove, 2);
            logout(first); logout(cancelled); logout(departed); logout(bare);
        }
    }

    @GameTest(templateName="empty", tickLimit=100)
    @SuppressWarnings("unchecked")
    public static void poweredPistonPushAndStickyPullGrantInception(TestContext context) throws Exception {
        var world = context.getWorld();
        var player = player(context, "BRPiston");
        player.networkHandler = new net.minecraft.server.network.ServerPlayNetworkHandler(world.getServer(),
                new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND), player);
        // Supply only the nearby-player query and online UUID lookup; no Forge login pipeline is involved.
        var playersField = net.minecraft.server.world.ServerWorld.class.getDeclaredField("players");
        playersField.setAccessible(true);
        var nearbyPlayers = (java.util.List<ServerPlayerEntity>)playersField.get(world);
        var onlineField = net.minecraft.server.PlayerManager.class.getDeclaredField("playerMap");
        onlineField.setAccessible(true);
        var online = (java.util.Map<UUID, ServerPlayerEntity>)onlineField.get(world.getServer().getPlayerManager());
        nearbyPlayers.add(player); online.put(player.getUuid(), player);
        var other = player(context, "BRNoPiston");
        var base = context.getAbsolutePos(new BlockPos(1, 3, 1));
        player.setPosition(base.getX()+0.5, base.getY()+2, base.getZ()+2.5);
        world.setBlockState(base, Blocks.PISTON.getDefaultState().with(PistonBlock.FACING, Direction.EAST), 3);
        world.setBlockState(base.east(), Blocks.STICKY_PISTON.getDefaultState().with(PistonBlock.FACING, Direction.WEST), 3);
        world.setBlockState(base.west(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        context.runAtTick(5, () -> {
            context.assertTrue(world.getBlockState(base.east(2)).isOf(Blocks.STICKY_PISTON), "The powered native piston pushed the opposing sticky piston");
            context.assertTrue(!granted(player, "inception"), "The initial push alone does not grant Inception");
            world.setBlockState(base.west(), Blocks.AIR.getDefaultState(), 3);
        });
        context.runAtTick(10, () -> world.setBlockState(base.east(3), Blocks.REDSTONE_BLOCK.getDefaultState(), 3));
        context.runAtTick(15, () -> world.setBlockState(base.east(3), Blocks.AIR.getDefaultState(), 3));
        context.runAtTick(22, () -> {
            try {
                context.assertTrue(world.getBlockState(base).isAir() && world.getBlockState(base.east()).isOf(Blocks.PISTON), "The sticky piston pulled the original piston into its adjacent cell");
                tick(player); tick(other);
                context.assertTrue(granted(player, "inception") && !granted(other, "inception"), "The player associated with the original push receives Inception");
                context.complete();
            } finally {
                for(int x=-1;x<=3;x++) world.setBlockState(base.east(x), Blocks.AIR.getDefaultState(), 3);
                logout(player); logout(other);
                nearbyPlayers.remove(player); online.remove(player.getUuid());
            }
        });
    }
}
