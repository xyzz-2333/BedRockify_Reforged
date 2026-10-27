package dev.bedrockify.forge.common.features.legacy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

public class NetherReactorBlock extends Block {
    public static final IntProperty PHASE = IntProperty.of("phase", 0, 2);
    // Each value represents one 20-tick level in the 0.11 reactor run.
    public static final IntProperty MINUTE = IntProperty.of("minute", 0, 46);
    private static final int[] DROP_LEVELS = {10, 13, 20, 22, 25, 30, 34, 36, 38, 40};

    public NetherReactorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(PHASE, 0).with(MINUTE, 0));
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PHASE, MINUTE);
    }

    @Override public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                        Hand hand, BlockHitResult hit) {
        if (player.isSpectator()) return ActionResult.PASS;
        if (world.isClient || hand != Hand.MAIN_HAND) return ActionResult.SUCCESS;
        if (state.get(PHASE) == 1) {
            player.sendMessage(Text.translatable("message.bedrockify.nether_reactor.active"), false);
            return ActionResult.SUCCESS;
        }
        if (state.get(PHASE) == 2) {
            player.sendMessage(Text.translatable("message.bedrockify.nether_reactor.completed"), false);
            return ActionResult.SUCCESS;
        }
        if (!validStructure(world, pos)) {
            player.sendMessage(Text.translatable("message.bedrockify.nether_reactor.wrong_pattern"), false);
            return ActionResult.SUCCESS;
        }
        if (world.getRegistryKey() != World.OVERWORLD) {
            player.sendMessage(Text.translatable("message.bedrockify.nether_reactor.overworld_only"), false);
            return ActionResult.SUCCESS;
        }
        for (PlayerEntity other : world.getPlayers()) {
            if (other.isSpectator()) continue;
            if (Math.abs(other.getX() - pos.getX()) > 5 || Math.abs(other.getY() - pos.getY()) > 2
                    || Math.abs(other.getZ() - pos.getZ()) > 5) {
                player.sendMessage(Text.translatable("message.bedrockify.nether_reactor.players_too_far"), false);
                return ActionResult.SUCCESS;
            }
        }
        if (pos.getY() <= 2 || pos.getY() < world.getBottomY() + 2) {
            player.sendMessage(Text.translatable("message.bedrockify.nether_reactor.built_too_low"), false);
            return ActionResult.SUCCESS;
        }
        if (pos.getY() > 100 || pos.getY() + 33 >= world.getTopY()) {
            player.sendMessage(Text.translatable("message.bedrockify.nether_reactor.built_too_high"), false);
            return ActionResult.SUCCESS;
        }
        if (!player.getAbilities().allowModifyWorld) {
            player.sendMessage(Text.translatable("message.bedrockify.nether_reactor.cannot_build"), false);
            return ActionResult.SUCCESS;
        }
        makeSpire((ServerWorld) world, pos);
        world.setBlockState(pos, state.with(PHASE, 1), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, this, 20);
        player.sendMessage(Text.translatable("message.bedrockify.nether_reactor.active"), false);
        return ActionResult.SUCCESS;
    }

    public static boolean validStructure(World world, BlockPos center) {
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            Block expected;
            if (y == -1) expected = Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.GOLD_BLOCK : Blocks.COBBLESTONE;
            else if (y == 0) expected = Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            else expected = Math.abs(x) + Math.abs(z) <= 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            if (!world.getBlockState(center.add(x, y, z)).isOf(expected)) return false;
        }
        return true;
    }

    private static void makeSpire(ServerWorld world, BlockPos center) {
        // MCPE 0.11 buildDome: two solid floors, a four-layer room, a ceiling,
        // then three crooked roof volumes. The room clears existing blocks while
        // keeping the 3x3x3 reactor itself.
        for (int y = -3; y <= 3; y++) {
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                boolean reactor = y >= -1 && y <= 1 && Math.abs(x) <= 1 && Math.abs(z) <= 1;
                if (reactor) continue;
                boolean wall = Math.abs(x) == 8 || Math.abs(z) == 8;
                Block replacement = y <= -2 || y == 3 || wall ? Blocks.NETHERRACK : Blocks.AIR;
                placeDomeBlock(world, center.add(x, y, z), replacement);
            }
        }
        crookedRoof(world, center, 4, 8, 1, false);
        crookedRoof(world, center, 5, 5, 8, true);
        crookedRoof(world, center, 11, 3, 14, false);
    }

    private static void crookedRoof(ServerWorld world, BlockPos center, int baseY,
                                    int radius, int height, boolean reverseSlope) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            int roof = radius + height + (reverseSlope ? (radius - x) / 2 : (x - radius) / 2);
            boolean edge = Math.abs(x) == radius || Math.abs(z) == radius;
            for (int y = 0; y <= roof; y++) {
                if (edge || y == roof)
                    placeDomeBlock(world, center.add(x, baseY + y, z), Blocks.NETHERRACK);
            }
        }
    }

    private static void placeDomeBlock(ServerWorld world, BlockPos pos, Block block) {
        if (!world.getBlockState(pos).isOf(block))
            world.setBlockState(pos, block.getDefaultState(), Block.NOTIFY_ALL);
    }

    @Override public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(PHASE) == 2) {
            if (world.getPlayers().stream().anyMatch(player -> !player.isSpectator()
                    && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 1600)) {
                spawnReactorPiglins(world, pos, random, 3);
            } else {
                world.getEntitiesByClass(ZombifiedPiglinEntity.class, new Box(pos).expand(40),
                        entity -> entity.getCommandTags().contains(reactorTag(pos))).forEach(ZombifiedPiglinEntity::discard);
            }
            world.scheduleBlockTick(pos, this, 20);
            return;
        }
        if (state.get(PHASE) != 1) return;
        int level = state.get(MINUTE) + 1;
        if (level > 46) {
            deteriorateDome(world, pos, random);
            world.setBlockState(pos, state.with(PHASE, 2).with(MINUTE, 0), Block.NOTIFY_ALL);
            world.scheduleBlockTick(pos, this, 20);
            return;
        }
        if (level >= 2 && level <= 4) turnLayer(pos, world, level - 2, Blocks.COBBLESTONE,
                LegacyContent.GLOWING_OBSIDIAN);
        if (level >= 7 && level <= 9) turnLayer(pos, world, level - 7, Blocks.GOLD_BLOCK,
                LegacyContent.GLOWING_OBSIDIAN);
        if (level >= 43 && level <= 45) {
            int y = 1 - (level - 43);
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos target = pos.add(x, y, z);
                if (!world.getBlockState(target).isOf(this))
                    world.setBlockState(target, Blocks.OBSIDIAN.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        int batch = 0;
        for (int i = 0; i < DROP_LEVELS.length; i++) if (DROP_LEVELS[i] == level) batch = i + 1;
        if (batch != 0) {
            int itemCount = batch <= 3 ? 15 : batch <= 7 ? Math.max(0, random.nextInt(42) - 4)
                    : Math.max(0, random.nextInt(27) - 2);
            for (int i = 0; i < itemCount; i++) {
                Vec3d spawn = spawnPosition(pos, random);
                ItemEntity item = new ItemEntity(world, spawn.x, spawn.y, spawn.z, randomDrop(random));
                item.setPickupDelay(10);
                world.spawnEntity(item);
            }
            int enemyCount = batch <= 3 ? 2 : batch <= 5 ? random.nextInt(2) : 0;
            spawnReactorPiglins(world, pos, random, enemyCount);
        }
        world.setBlockState(pos, state.with(MINUTE, level), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, this, level == 46 ? 1 : 20);
    }

    private static void deteriorateDome(ServerWorld world, BlockPos center, Random random) {
        // 0.11 deteriorateDome removes roughly one in three wall blocks and
        // one in four exposed roof blocks when the run finishes.
        for (int y = -1; y <= 3; y++) for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            if ((Math.abs(x) == 8 || Math.abs(z) == 8) && random.nextInt(3) == 0)
                removeDomeBlock(world, center.add(x, y, z));
        }
        deteriorateRoof(world, center, 4, 8, 1, false, random);
        deteriorateRoof(world, center, 5, 5, 8, true, random);
        deteriorateRoof(world, center, 11, 3, 14, false, random);
    }

    private static void deteriorateRoof(ServerWorld world, BlockPos center, int baseY,
                                        int radius, int height, boolean reverseSlope, Random random) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (Math.abs(x) != radius && Math.abs(z) != radius) continue;
            int roof = radius + height + (reverseSlope ? (radius - x) / 2 : (x - radius) / 2);
            for (int y = 0; y <= roof; y++) if (random.nextInt(4) == 0)
                removeDomeBlock(world, center.add(x, baseY + y, z));
        }
    }

    private static void removeDomeBlock(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos).isOf(Blocks.NETHERRACK))
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
    }

    private static void turnLayer(BlockPos center, ServerWorld world, int layer, Block expected, Block replacement) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            Block pattern = layer == 0 ? (Math.abs(x) == 1 && Math.abs(z) == 1
                    ? Blocks.GOLD_BLOCK : Blocks.COBBLESTONE)
                    : layer == 1 ? (Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.COBBLESTONE : Blocks.AIR)
                    : (Math.abs(x) + Math.abs(z) <= 1 ? Blocks.COBBLESTONE : Blocks.AIR);
            if (pattern == expected)
                world.setBlockState(center.add(x, layer - 1, z), replacement.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    private static Vec3d spawnPosition(BlockPos pos, Random random) {
        double radius = 3.0 + random.nextDouble() * 4.0;
        double angle = random.nextDouble() * Math.PI * 2.0;
        return new Vec3d(pos.getX() + Math.cos(angle) * radius,
                pos.getY() - 1.0,
                pos.getZ() + Math.sin(angle) * radius);
    }

    private static void spawnReactorPiglins(ServerWorld world, BlockPos pos, Random random, int desired) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) return;
        int occupied = world.getEntitiesByClass(ZombifiedPiglinEntity.class, new Box(pos).expand(7),
                entity -> entity.isAlive()).size();
        for (int i = 0; i < Math.min(desired, Math.max(0, 3 - occupied)); i++) {
            ZombifiedPiglinEntity piglin = EntityType.ZOMBIFIED_PIGLIN.create(world);
            if (piglin == null) return;
            Vec3d spawn = spawnPosition(pos, random);
            piglin.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, random.nextFloat() * 360F, 0);
            piglin.addCommandTag(reactorTag(pos));
            world.spawnEntity(piglin);
        }
    }

    private static String reactorTag(BlockPos pos) {
        return "bedrockify_reactor_" + pos.asLong();
    }

    private static ItemStack randomDrop(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> new ItemStack(Items.GLOWSTONE_DUST, 3);
            case 1 -> new ItemStack(Items.MELON_SEEDS);
            case 2 -> new ItemStack(Items.BROWN_MUSHROOM);
            case 3 -> new ItemStack(Items.RED_MUSHROOM);
            case 4 -> new ItemStack(Items.SUGAR_CANE);
            case 5 -> new ItemStack(Items.CACTUS);
            case 6 -> new ItemStack(Items.QUARTZ, 4);
            default -> new ItemStack(Items.PUMPKIN_SEEDS);
        };
    }
}
