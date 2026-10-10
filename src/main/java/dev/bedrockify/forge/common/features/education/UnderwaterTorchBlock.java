package dev.bedrockify.forge.common.features.education;

import net.minecraft.block.*;
import net.minecraft.fluid.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;

public final class UnderwaterTorchBlock extends TorchBlock implements Waterloggable {
    public UnderwaterTorchBlock(Settings settings) { super(settings, ParticleTypes.FLAME); setDefaultState(getDefaultState().with(Properties.WATERLOGGED, false)); }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(Properties.WATERLOGGED); }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(Properties.WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).getFluid() == Fluids.WATER);
    }
    @Override public FluidState getFluidState(BlockState state) { return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state); }
    @Override public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(Properties.WATERLOGGED)) world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        return super.getStateForNeighborUpdate(state, direction, neighbor, world, pos, neighborPos);
    }
    @Override public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(Properties.WATERLOGGED)) world.addParticle(ParticleTypes.BUBBLE, pos.getX()+0.5, pos.getY()+0.7, pos.getZ()+0.5, 0, 0.03, 0);
        else super.randomDisplayTick(state, world, pos, random);
    }
}
