package dev.bedrockify.forge.tests;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.bedrockify.forge.common.features.legacy.LegacyContent;
import dev.bedrockify.forge.common.features.legacy.NetherReactorBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public class Reactor011GameTests {
    private static JsonObject fixture() {
        try (var stream = Reactor011GameTests.class.getResourceAsStream(
                "/data/bedrockify/reactor/mcpe011-spire.json")) {
            if (stream == null) throw new IllegalStateException("Missing 0.11 save fixture");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static BlockPos start(TestContext c, int zOffset) {
        var world = c.getWorld();
        world.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        // Keep the long-running interruption test clear of other tests' spires.
        BlockPos pos = c.getAbsolutePos(new BlockPos(2, 70, 2 + zOffset));
        prepareScene(world, pos);
        useCore(world, pos);
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 1, "Reactor activates");
        return pos;
    }

    private static void prepareScene(ServerWorld world, BlockPos pos) {
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            world.setChunkForced((pos.getX() >> 4) + x, (pos.getZ() >> 4) + z, true);
        // GameTest keeps its world between launches and may place templates at new origins.
        // Remove prior towers and nearby cores so their blocks/timers cannot contaminate this scene.
        for (int y = -3; y <= 34; y++) for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
            BlockPos target = pos.add(x, y, z);
            if (target.getY() < world.getBottomY()) continue;
            if (!world.getBlockState(target).isAir())
                world.setBlockState(target, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            Block block;
            if (y == -1) block = Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.GOLD_BLOCK : Blocks.COBBLESTONE;
            else if (y == 0) block = x == 0 && z == 0 ? LegacyContent.NETHER_REACTOR_CORE
                    : Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            else block = Math.abs(x) + Math.abs(z) <= 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            world.setBlockState(pos.add(x, y, z), block.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    private static void useCore(ServerWorld world, BlockPos pos) {
        var player = FakePlayerFactory.getMinecraft(world);
        player.getAbilities().allowModifyWorld = true;
        player.setPosition(Vec3d.ofCenter(pos));
        var core = LegacyContent.NETHER_REACTOR_CORE;
        core.onUse(world.getBlockState(pos), world, pos, player, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
    }

    @GameTest(templateName="empty")
    public static void lowestTwoGoldLayersRefuseActivation(TestContext c) {
        var world = c.getWorld();
        for (int offset = 0; offset <= 1; offset++) {
            BlockPos anchor = c.getAbsolutePos(new BlockPos(2, 0, 322 + offset * 64));
            BlockPos pos = new BlockPos(anchor.getX(), world.getBottomY() + offset + 1, anchor.getZ());
            prepareScene(world, pos);
            c.assertTrue(NetherReactorBlock.validStructure(world, pos), "Boundary test uses a valid structure");
            world.setBlockState(pos.add(8, 0, 0), Blocks.BEDROCK.getDefaultState());
            var before = snapshot(world, pos);
            useCore(world, pos);
            c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 0,
                    "Gold at bottom + " + offset + " must not activate");
            assertSnapshot(c, pos, before, "Rejected low reactor cannot alter surrounding blocks");
        }
        c.complete();
    }

    @GameTest(templateName="empty")
    public static void minimumGoldLayerActivatesAndReplacesBedrock(TestContext c) {
        var world = c.getWorld();
        world.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        BlockPos anchor = c.getAbsolutePos(new BlockPos(2, 0, 450));
        BlockPos pos = new BlockPos(anchor.getX(), world.getBottomY() + 3, anchor.getZ());
        prepareScene(world, pos);
        // Gold is at bottom + 2. Both spire floor layers intersect the world's bedrock floor.
        for (int y = 0; y <= 1; y++) for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
            world.setBlockState(new BlockPos(pos.getX() + x, world.getBottomY() + y, pos.getZ() + z), Blocks.BEDROCK.getDefaultState());
        for (BlockPos target : new BlockPos[]{pos.add(4, 0, 0), pos.add(8, 0, 0), pos.add(0, 3, 0), pos.add(9, 0, 0)})
            world.setBlockState(target, Blocks.BEDROCK.getDefaultState());
        c.assertTrue(NetherReactorBlock.validStructure(world, pos), "Bedrock fixture preserves the activation structure");
        useCore(world, pos);
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 1,
                "Gold at bottom + 2 activates, even when the core has a negative Y coordinate");
        for (int y = 0; y <= 1; y++) for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++)
            c.assertTrue(world.getBlockState(new BlockPos(pos.getX() + x, world.getBottomY() + y, pos.getZ() + z)).isOf(Blocks.NETHERRACK),
                    "The two 17x17 spire floors replace bedrock at bottom + " + y);
        c.assertTrue(world.getBlockState(pos.add(4, 0, 0)).isAir(), "Room generation clears bedrock");
        c.assertTrue(world.getBlockState(pos.add(8, 0, 0)).isOf(Blocks.NETHERRACK), "Spire wall replaces bedrock");
        c.assertTrue(world.getBlockState(pos.add(0, 3, 0)).isOf(Blocks.NETHERRACK), "Spire ceiling replaces bedrock");
        c.assertTrue(world.getBlockState(pos.add(9, 0, 0)).isOf(Blocks.BEDROCK), "Bedrock outside the spire remains intact");
        c.complete();
    }

    private static Map<BlockPos, BlockState> snapshot(ServerWorld world, BlockPos center) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (int y = -3; y <= 34; y++) for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            BlockPos pos = center.add(x, y, z);
            states.put(pos, world.getBlockState(pos));
        }
        return states;
    }

    private static void assertSnapshot(TestContext c, BlockPos center, Map<BlockPos, BlockState> before, String message) {
        var after = snapshot(c.getWorld(), center);
        String differences = before.entrySet().stream().filter(entry -> entry.getValue() != after.get(entry.getKey()))
                .limit(8).map(entry -> entry.getKey().subtract(center) + ": " + entry.getValue() + " -> " + after.get(entry.getKey()))
                .collect(java.util.stream.Collectors.joining("; "));
        c.assertTrue(before.equals(after), message + "; core=" + c.getWorld().getBlockState(center) + "; " + differences);
    }

    @GameTest(templateName="empty")
    public static void spireMatches011SaveAndDeterioratesOnce(TestContext c) {
        var world = c.getWorld();
        BlockPos pos = start(c, 64);
        JsonObject saved = fixture();
        int count = 0;
        for (int y = -3; y <= 34; y++) {
            String[] rows = y <= 30 ? saved.getAsJsonArray("interrupted").get(y + 3).getAsString().split(",") : null;
            for (int z = -8; z <= 8; z++) for (int x = -8; x <= 8; x++) {
                boolean expected = rows != null && (Integer.parseInt(rows[z + 8], 16) & (1 << (x + 8))) != 0;
                // The interrupted save has two adjacent access holes in its east wall.
                if (x == 8 && z == 4 && (y == -1 || y == 0)) expected = true;
                boolean actual = world.getBlockState(pos.add(x, y, z)).isOf(Blocks.NETHERRACK);
                c.assertTrue(actual == expected, "0.11 spire mismatch at " + x + "," + y + "," + z);
                if (actual) count++;
            }
        }
        c.assertTrue(count == 3005, "Intact 0.11 spire contains 3005 netherrack blocks; found " + count);
        var before = snapshot(world, pos);
        // A player replacement in a damageable wall must not be removed as netherrack.
        world.setBlockState(pos.add(8, 0, 0), Blocks.DIAMOND_BLOCK.getDefaultState());
        var core = LegacyContent.NETHER_REACTOR_CORE;
        world.setBlockState(pos, world.getBlockState(pos).with(NetherReactorBlock.MINUTE, 46));
        core.scheduledTick(world.getBlockState(pos), world, pos, Random.create(110));
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 2, "Normal completion changes phase");
        c.assertTrue(world.getBlockState(pos.add(8, 0, 0)).isOf(Blocks.DIAMOND_BLOCK), "Player replacement survives");
        int brokenWalls = 0, brokenRoof = 0;
        for (var entry : before.entrySet()) {
            BlockPos target = entry.getKey();
            int x = target.getX() - pos.getX(), y = target.getY() - pos.getY(), z = target.getZ() - pos.getZ();
            if (!entry.getValue().isOf(Blocks.NETHERRACK) || !world.getBlockState(target).isAir()) continue;
            c.assertTrue(y >= -1, "Completion preserves both floor layers");
            boolean perimeter = Math.abs(x) == 8 || Math.abs(z) == 8
                    || (y >= 5 && (Math.abs(x) == 5 || Math.abs(z) == 5) && Math.abs(x) <= 5 && Math.abs(z) <= 5)
                    || (y >= 11 && (Math.abs(x) == 3 || Math.abs(z) == 3) && Math.abs(x) <= 3 && Math.abs(z) <= 3);
            c.assertTrue(perimeter, "Interior ceiling and slope survive completion at " + x + "," + y + "," + z);
            if (y <= 3) brokenWalls++; else brokenRoof++;
        }
        c.assertTrue(brokenWalls > 0 && brokenRoof > 0, "Normal completion damages both walls and roof perimeter");
        var after = snapshot(world, pos);
        for (int i = 0; i < 3; i++) core.scheduledTick(world.getBlockState(pos), world, pos, world.random);
        c.assertTrue(after.equals(snapshot(world, pos)), "Completed reactor never repeats deterioration");
        c.complete();
    }

    @GameTest(templateName="empty", tickLimit=1100)
    public static void brokenAndReplacedCoresNeverDeteriorateSpire(TestContext c) {
        var world = c.getWorld();
        BlockPos broken = start(c, 128);
        BlockPos replaced = start(c, 192);
        // Leave pending scheduled ticks behind, including one just before completion.
        world.setBlockState(broken, world.getBlockState(broken).with(NetherReactorBlock.MINUTE, 46));
        world.breakBlock(broken, false);
        world.setBlockState(replaced, world.getBlockState(replaced).with(NetherReactorBlock.MINUTE, 46));
        world.breakBlock(replaced, false);
        world.setBlockState(replaced, LegacyContent.NETHER_REACTOR_CORE.getDefaultState());
        var brokenBefore = snapshot(world, broken);
        var replacedBefore = snapshot(world, replaced);
        c.waitAndRun(960, () -> {
            assertSnapshot(c, broken, brokenBefore, "Breaking an active core leaves its spire intact after a full cycle");
            assertSnapshot(c, replaced, replacedBefore, "Pending ticks cannot run a newly placed idle core");
            c.complete();
        });
    }
}
