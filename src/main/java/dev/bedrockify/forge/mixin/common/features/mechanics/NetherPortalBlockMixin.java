package dev.bedrockify.forge.mixin.common.features.mechanics;
import dev.bedrockify.forge.Bedrockify;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin extends Block {
    protected NetherPortalBlockMixin(Settings settings) { super(settings); }
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
