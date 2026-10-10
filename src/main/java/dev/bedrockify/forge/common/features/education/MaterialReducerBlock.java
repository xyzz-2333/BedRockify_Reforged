package dev.bedrockify.forge.common.features.education;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.network.NetworkHooks;

public final class MaterialReducerBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public MaterialReducerBlock(Settings settings) { super(settings); setDefaultState(getDefaultState().with(FACING, Direction.NORTH)); }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getPlacementState(ItemPlacementContext context) { return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite()); }
    @Override public BlockState rotate(BlockState state, BlockRotation rotation) { return state.with(FACING, rotation.rotate(state.get(FACING))); }
    @Override public BlockState mirror(BlockState state, BlockMirror mirror) { return rotate(state, mirror.getRotation(state.get(FACING))); }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new MaterialReducerBlockEntity(pos, state); }
    @Override public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof MaterialReducerBlockEntity reducer)
            NetworkHooks.openScreen(serverPlayer, reducer);
        return ActionResult.success(world.isClient);
    }
    @Override public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient && world.getBlockEntity(pos) instanceof MaterialReducerBlockEntity reducer) reducer.dropContents();
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
