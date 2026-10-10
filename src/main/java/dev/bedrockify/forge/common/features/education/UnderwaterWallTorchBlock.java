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

public final class UnderwaterWallTorchBlock extends WallTorchBlock implements Waterloggable {
    public UnderwaterWallTorchBlock(Settings settings) { super(settings, ParticleTypes.FLAME); setDefaultState(getDefaultState().with(Properties.WATERLOGGED, false)); }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { super.appendProperties(builder); builder.add(Properties.WATERLOGGED); }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.with(Properties.WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).getFluid() == Fluids.WATER);
    }
    @Override public FluidState getFluidState(BlockState state) { return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state); }
    @Override public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(Properties.WATERLOGGED)) world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        return super.getStateForNeighborUpdate(state, direction, neighbor, world, pos, neighborPos);
    }
    @Override public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(Properties.WATERLOGGED)) {
            Direction direction = state.get(FACING).getOpposite();
            world.addParticle(ParticleTypes.BUBBLE, pos.getX()+0.5+direction.getOffsetX()*0.27, pos.getY()+0.92, pos.getZ()+0.5+direction.getOffsetZ()*0.27, 0, 0.03, 0);
        } else super.randomDisplayTick(state, world, pos, random);
    }
}
