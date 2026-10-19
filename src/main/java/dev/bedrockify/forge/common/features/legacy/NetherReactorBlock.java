package dev.bedrockify.forge.common.features.legacy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

public class NetherReactorBlock extends Block {
    public static final IntProperty PHASE = IntProperty.of("phase", 0, 2);
    public static final IntProperty MINUTE = IntProperty.of("minute", 0, 15);
    private static final int[] LOOT = {0, 1, 2, 3, 4, 5, 6, 7};

    public NetherReactorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(PHASE, 0).with(MINUTE, 0));
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PHASE, MINUTE);
    }

    @Override public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                        Hand hand, BlockHitResult hit) {
        if (state.get(PHASE) != 0 || player.isSpectator() || player.isCreative()
                || !player.getAbilities().allowModifyWorld) return ActionResult.PASS;
        if (world.getRegistryKey() != World.OVERWORLD || !validStructure(world, pos)) return ActionResult.PASS;
        if (world.isClient) return ActionResult.SUCCESS;
        if (pos.getY() < world.getBottomY() + 2 || pos.getY() + 33 >= world.getTopY()) return ActionResult.PASS;
        makeSpire((ServerWorld) world, pos);
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            BlockPos other = pos.add(x, y, z);
            if (world.getBlockState(other).isOf(Blocks.GOLD_BLOCK) || world.getBlockState(other).isOf(Blocks.COBBLESTONE))
                world.setBlockState(other, LegacyContent.GLOWING_OBSIDIAN.getDefaultState(), Block.NOTIFY_ALL);
        }
        world.setBlockState(pos, state.with(PHASE, 1), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, this, 60);
        return ActionResult.SUCCESS;
    }

    public static boolean validStructure(World world, BlockPos center) {
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            Block expected;
            if (y == -1) expected = Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.GOLD_BLOCK : Blocks.COBBLESTONE;
            else if (y == 0) expected = Math.abs(x) + Math.abs(z) == 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            else expected = Math.abs(x) + Math.abs(z) <= 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            if (!world.getBlockState(center.add(x, y, z)).isOf(expected)) return false;
        }
        return true;
    }

    private static void makeSpire(ServerWorld world, BlockPos center) {
        // The original shell is 17 x 17 and 35 blocks high. Only replace air so existing builds survive.
        for (int y = -2; y <= 32; y++) {
            int radius = y < 7 ? 8 : y < 21 ? 7 : y < 29 ? 5 : 3;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) != radius && Math.abs(z) != radius && y != -2 && y != 32) continue;
                BlockPos target = center.add(x, y, z);
                if (world.getBlockState(target).isAir()) world.setBlockState(target, Blocks.NETHERRACK.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
    }

    @Override public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(PHASE) != 1) return;
        int minute = state.get(MINUTE) + 1;
        if (minute >= 15) {
            world.setBlockState(pos, state.with(PHASE, 2).with(MINUTE, 0), Block.NOTIFY_ALL);
            for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos other = pos.add(x, y, z);
                if (world.getBlockState(other).isOf(LegacyContent.GLOWING_OBSIDIAN))
                    world.setBlockState(other, Blocks.OBSIDIAN.getDefaultState(), Block.NOTIFY_ALL);
            }
            return;
        }
        if (world.getDifficulty() != Difficulty.PEACEFUL && minute % 3 == 0) {
            ZombifiedPiglinEntity piglin = EntityType.ZOMBIFIED_PIGLIN.create(world);
            if (piglin != null) {
                piglin.refreshPositionAndAngles(pos.getX() + 0.5 + random.nextInt(7) - 3, pos.getY() + 2,
                        pos.getZ() + 0.5 + random.nextInt(7) - 3, random.nextFloat() * 360F, 0);
                world.spawnEntity(piglin);
            }
        }
        ItemStack drop = switch (LOOT[random.nextInt(LOOT.length)]) {
            case 0 -> new ItemStack(Items.GLOWSTONE_DUST);
            case 1 -> new ItemStack(Items.QUARTZ);
            case 2 -> new ItemStack(Items.GOLD_INGOT);
            case 3 -> new ItemStack(Items.BONE);
            case 4 -> new ItemStack(Items.RED_MUSHROOM);
            case 5 -> new ItemStack(Items.BROWN_MUSHROOM);
            case 6 -> new ItemStack(Items.SUGAR_CANE);
            default -> new ItemStack(Items.CACTUS);
        };
        Block.dropStack(world, pos.up(2), drop);
        world.setBlockState(pos, state.with(MINUTE, minute), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, this, 60);
    }
}
