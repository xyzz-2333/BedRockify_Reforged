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
    public static void reactorPatternAndCycle(TestContext c) {
        var world = c.getWorld();
        world.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        BlockPos pos = c.getAbsolutePos(new BlockPos(2, 5, 2));
        for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            Block block;
            if (y == -1) block = Math.abs(x) == 1 && Math.abs(z) == 1 ? Blocks.GOLD_BLOCK : Blocks.COBBLESTONE;
            else if (y == 0) block = x == 0 && z == 0 ? LegacyContent.NETHER_REACTOR_CORE
                    : Math.abs(x) + Math.abs(z) == 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            else block = Math.abs(x) + Math.abs(z) <= 1 ? Blocks.COBBLESTONE : Blocks.AIR;
            world.setBlockState(pos.add(x, y, z), block.getDefaultState());
        }
        c.assertTrue(NetherReactorBlock.validStructure(world, pos), "Classic 14-cobblestone pattern is accepted");
        world.setBlockState(pos.up().north(), Blocks.AIR.getDefaultState());
        c.assertFalse(NetherReactorBlock.validStructure(world, pos), "Incomplete upper cross is rejected");
        world.setBlockState(pos.up().north(), Blocks.COBBLESTONE.getDefaultState());
        var player = FakePlayerFactory.getMinecraft(world);
        player.getAbilities().creativeMode = false;
        player.setPosition(pos.toCenterPos());
        var hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        var core = LegacyContent.NETHER_REACTOR_CORE;
        core.onUse(world.getBlockState(pos), world, pos, player, Hand.MAIN_HAND, hit);
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 1, "Reactor starts");
        c.assertTrue(world.getBlockState(pos.down().north().west()).isOf(LegacyContent.GLOWING_OBSIDIAN), "Gold becomes glowing obsidian");
        for (int i = 0; i < 15; i++) core.scheduledTick(world.getBlockState(pos), world, pos, world.random);
        c.assertTrue(world.getBlockState(pos).get(NetherReactorBlock.PHASE) == 2, "Reactor completes after 900 ticks");
        c.assertTrue(world.getBlockState(pos.down().north().west()).isOf(Blocks.OBSIDIAN), "Glowing frame cools");
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
