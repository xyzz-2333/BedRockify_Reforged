package dev.bedrockify.forge.tests;

import dev.bedrockify.forge.common.features.legacy.LegacyContent;
import dev.bedrockify.forge.common.features.legacy.NetherReactorBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.test.TestContext;
import net.minecraft.test.GameTest;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public class Alpha16GameTests {
    @GameTest(templateName="empty")
    public static void creativeReactorClick(TestContext c) {
        var world = c.getWorld();
        BlockPos pos = c.getAbsolutePos(new BlockPos(2, 70, 2));
        var core = LegacyContent.NETHER_REACTOR_CORE;
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            world.setBlockState(pos.add(x, y, z), Blocks.AIR.getDefaultState());
        world.setBlockState(pos, core.getDefaultState());
        var player = FakePlayerFactory.getMinecraft(world);
        player.getAbilities().creativeMode = true;
        player.getAbilities().allowModifyWorld = true;
        player.setPosition(pos.toCenterPos());
        var hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        c.assertTrue(core.onUse(world.getBlockState(pos), world, pos, player, Hand.MAIN_HAND, hit) == ActionResult.SUCCESS,
                "Invalid pattern consumes the click to give feedback");
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 0,
                "Invalid pattern does not activate");
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            Block block;
            if (y == -1) block = Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.GOLD_BLOCK : Blocks.COBBLESTONE;
            else if (y == 0) block = Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            else block = Math.abs(x) + Math.abs(z) <= 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            world.setBlockState(pos.add(x, y, z), block.getDefaultState());
        }
        world.setBlockState(pos.add(4, 0, 0), Blocks.STONE.getDefaultState());
        world.setBlockState(pos.add(8, 0, 0), Blocks.STONE.getDefaultState());
        c.assertTrue(core.onUse(world.getBlockState(pos), world, pos, player, Hand.MAIN_HAND, hit) == ActionResult.SUCCESS,
                "Creative activation consumes the click");
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 1,
                "Creative mode activates a correct structure at y=" + pos.getY()
                        + ", pattern=" + NetherReactorBlock.validStructure(world, pos)
                        + ", modify=" + player.getAbilities().allowModifyWorld
                        + ", players=" + world.getPlayers().size());
        c.assertTrue(world.getBlockState(pos.add(4, 0, 0)).isAir(),
                "Original bottom room clears existing blocks");
        c.assertTrue(world.getBlockState(pos.add(8, 0, 0)).isOf(Blocks.NETHERRACK),
                "Original room wall replaces existing blocks");
        c.assertTrue(world.getBlockState(pos.add(0, 3, 0)).isOf(Blocks.NETHERRACK),
                "Original room has a solid ceiling");
        c.complete();
    }

    @GameTest(templateName="empty")
    public static void reactorPatternAndCycle(TestContext c) {
        var world = c.getWorld();
        world.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        BlockPos pos = c.getAbsolutePos(new BlockPos(2, 70, 2));
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            Block block;
            if (y == -1) block = Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.GOLD_BLOCK : Blocks.COBBLESTONE;
            else if (y == 0) block = x == 0 && z == 0 ? LegacyContent.NETHER_REACTOR_CORE
                    : Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            else block = Math.abs(x) + Math.abs(z) <= 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            world.setBlockState(pos.add(x, y, z), block.getDefaultState());
        }
        c.assertTrue(NetherReactorBlock.validStructure(world, pos), "Classic 14-cobblestone pattern is accepted");
        world.setBlockState(pos.north(), Blocks.COBBLESTONE.getDefaultState());
        c.assertFalse(NetherReactorBlock.validStructure(world, pos), "Middle edge must remain open");
        world.setBlockState(pos.north(), Blocks.AIR.getDefaultState());
        world.setBlockState(pos.north().west(), Blocks.AIR.getDefaultState());
        c.assertFalse(NetherReactorBlock.validStructure(world, pos), "Middle corner needs cobblestone");
        world.setBlockState(pos.north().west(), Blocks.COBBLESTONE.getDefaultState());
        world.setBlockState(pos.up().north(), Blocks.AIR.getDefaultState());
        c.assertFalse(NetherReactorBlock.validStructure(world, pos), "Incomplete upper cross is rejected");
        world.setBlockState(pos.up().north(), Blocks.COBBLESTONE.getDefaultState());
        world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class,
                new net.minecraft.util.math.Box(pos).expand(9), item -> item.isAlive())
                .forEach(net.minecraft.entity.ItemEntity::discard);
        var player = FakePlayerFactory.getMinecraft(world);
        player.getAbilities().creativeMode = false;
        player.setPosition(pos.toCenterPos());
        var hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        var core = LegacyContent.NETHER_REACTOR_CORE;
        core.onUse(world.getBlockState(pos), world, pos, player, Hand.MAIN_HAND, hit);
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 1,
                "Reactor starts at y=" + pos.getY() + ", pattern=" + NetherReactorBlock.validStructure(world, pos)
                        + ", modify=" + player.getAbilities().allowModifyWorld
                        + ", players=" + world.getPlayers().size());
        c.assertTrue(world.getBlockState(pos.down().north().west()).isOf(Blocks.GOLD_BLOCK), "Gold remains until its transformation level");
        for (int i = 0; i < 2; i++) core.scheduledTick(world.getBlockState(pos), world, pos, world.random);
        c.assertTrue(world.getBlockState(pos.down()).isOf(LegacyContent.GLOWING_OBSIDIAN), "Bottom cobblestone changes at level 2");
        for (int i = 2; i < 7; i++) core.scheduledTick(world.getBlockState(pos), world, pos, world.random);
        c.assertTrue(world.getBlockState(pos.down().north().west()).isOf(LegacyContent.GLOWING_OBSIDIAN), "Gold changes at level 7");
        for (int i = 7; i < 9; i++) core.scheduledTick(world.getBlockState(pos), world, pos, world.random);
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.MINUTE) == 9, "Reactor advances in 20-tick levels");
        core.scheduledTick(world.getBlockState(pos), world, pos, world.random);
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.MINUTE) == 10, "First batch occurs at level 10");
        var drops = world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class,
                new net.minecraft.util.math.Box(pos).expand(9), item -> item.isAlive());
        c.assertTrue(drops.size() == 15, "First native batch contains 15 item entities; found " + drops.size());
        c.assertTrue(drops.stream().allMatch(item -> Math.abs(item.getY() - (pos.getY() - 1)) < 0.01),
                "Native reactor drops spawn one block below the core");
        for (int i = 10; i < 43; i++) core.scheduledTick(world.getBlockState(pos), world, pos, world.random);
        c.assertTrue(world.getBlockState(pos.up().north().west()).isOf(Blocks.OBSIDIAN), "Cooling fills the top layer at level 43");
        for (int i = 43; i < 46; i++) core.scheduledTick(world.getBlockState(pos), world, pos, world.random);
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 1, "Reactor remains active through tick 920");
        c.assertTrue(world.getBlockState(pos.north()).isOf(Blocks.OBSIDIAN), "Cooling fills the middle layer");
        core.scheduledTick(world.getBlockState(pos), world, pos, world.random);
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 2, "Reactor completes after tick 920");
        c.assertTrue(world.getBlockState(pos.down().north().west()).isOf(Blocks.OBSIDIAN), "Glowing frame cools");
        int brokenWallBlocks = 0;
        for (int y = -1; y <= 3; y++) for (int z = -8; z <= 8; z++)
            if (world.getBlockState(pos.add(8, y, z)).isAir()) brokenWallBlocks++;
        c.assertTrue(brokenWallBlocks > 0, "Reactor completion breaks part of the spire wall");
        c.complete();
    }

    @GameTest(templateName="empty")
    public static void decayPotion(TestContext c) {
        var stack = PotionUtil.setPotion(new ItemStack(Items.POTION), LegacyContent.DECAY);
        c.assertTrue(PotionUtil.getPotion(stack) == LegacyContent.DECAY, "Decay survives potion NBT");
        var effect = LegacyContent.DECAY.getEffects().get(0);
        c.assertTrue(effect.getEffectType() == StatusEffects.WITHER && effect.getAmplifier() == 1 && effect.getDuration() == 800,
                "Wither II lasts 40 seconds");
        c.complete();
    }
}
