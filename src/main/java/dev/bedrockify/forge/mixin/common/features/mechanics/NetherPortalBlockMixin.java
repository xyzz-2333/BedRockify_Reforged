package dev.bedrockify.forge.mixin.common.features.mechanics;
import dev.bedrockify.forge.Bedrockify;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.dimension.NetherPortal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayDeque;
import java.util.HashSet;
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin extends Block {
    @Unique private static final ThreadLocal<Boolean> bedrockify$clearingPortal = ThreadLocal.withInitial(() -> false);
    protected NetherPortalBlockMixin(Settings settings) { super(settings); }

    @Inject(method="getStateForNeighborUpdate", at=@At("HEAD"), cancellable=true)
    private void bedrockify$collapseInvalidPortal(BlockState state, Direction direction, BlockState neighborState,
                                                     WorldAccess world, BlockPos pos, BlockPos neighborPos,
                                                     CallbackInfoReturnable<BlockState> cir) {
        if (bedrockify$clearingPortal.get()) return;
        if (!(world instanceof ServerWorld server) || direction.getAxis().isHorizontal()
                && direction.getAxis() != state.get(NetherPortalBlock.AXIS)
                || neighborState.isOf(Blocks.NETHER_PORTAL)) return;
        if (new NetherPortal(world, pos, state.get(NetherPortalBlock.AXIS)).wasAlreadyValid()) return;

        // Vanilla validates the whole rectangle again for every portal block removed.
        // Gather the connected plane first, then let block notifications run without rescanning it.
        var pending = new ArrayDeque<BlockPos>();
        var connected = new HashSet<BlockPos>();
        pending.add(pos);
        Direction along = state.get(NetherPortalBlock.AXIS) == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        while (!pending.isEmpty()) {
            BlockPos current = pending.removeFirst();
            if (!connected.add(current)) continue;
            if (connected.size() > 512) return; // Oversized command-built portals use vanilla behavior.
            for (Direction next : new Direction[]{Direction.UP, Direction.DOWN, along, along.getOpposite()}) {
                BlockPos adjacent = current.offset(next);
                if (server.isChunkLoaded(adjacent)
                        && server.getBlockState(adjacent).isOf(Blocks.NETHER_PORTAL)
                        && server.getBlockState(adjacent).get(NetherPortalBlock.AXIS) == state.get(NetherPortalBlock.AXIS)
                        && !connected.contains(adjacent)) pending.add(adjacent);
            }
        }
        bedrockify$clearingPortal.set(true);
        try {
            for (BlockPos current : connected) server.setBlockState(current, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        } finally {
            bedrockify$clearingPortal.remove();
        }
        cir.setReturnValue(Blocks.AIR.getDefaultState());
    }
    @Override public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state,world,pos,oldState,notify);
        if (!(world instanceof ServerWorld server) || oldState.isOf(Blocks.NETHER_PORTAL)
                || !Bedrockify.getInstance().settings.bedrockPortalSpawning || world.getRegistryKey()!=World.OVERWORLD
                || !world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)
                || world.random.nextInt(2000)>=world.getDifficulty().getId()) return;
        bedrockify$spawnPortalPiglin(state,server,pos);
    }

    @Inject(method="randomTick", at=@At("HEAD"), cancellable=true)
    private void bedrockify$bedrockRandomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!Bedrockify.getInstance().settings.bedrockPortalSpawning || world.getRegistryKey()!=World.OVERWORLD) return;
        ci.cancel();
        if (world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)
                && random.nextInt(2000)<world.getDifficulty().getId()) {
            bedrockify$spawnPortalPiglin(state,world,pos);
        }
    }

    @Unique private void bedrockify$spawnPortalPiglin(BlockState state, ServerWorld world, BlockPos pos) {
        BlockPos bottom=pos;
        while (bottom.getY()>world.getBottomY() && world.getBlockState(bottom.down()).isOf(Blocks.NETHER_PORTAL)) {
            bottom=bottom.down();
        }
        if (bottom.getY()<0) return;
        Direction side=state.get(NetherPortalBlock.AXIS)==Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        BlockPos spawn=bottom.offset(side);
        if (!world.isChunkLoaded(spawn)) return;
        var pig=EntityType.ZOMBIFIED_PIGLIN.create(world);
        if (pig==null) return;
        pig.refreshPositionAndAngles(spawn.getX()+0.5,spawn.getY(),spawn.getZ()+0.5,world.random.nextFloat()*360,0);
        pig.initialize(world,world.getLocalDifficulty(spawn),SpawnReason.STRUCTURE,null,null);
        pig.resetPortalCooldown();
        world.spawnEntity(pig);
    }
}
