package dev.bedrockify.forge.mixin.common.features.mechanics;
import dev.bedrockify.forge.Bedrockify;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin extends Block {
    protected NetherPortalBlockMixin(Settings settings) { super(settings); }
    @Override public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state,world,pos,oldState,notify);
        if (!(world instanceof ServerWorld server) || oldState.isOf(Blocks.NETHER_PORTAL)
                || !Bedrockify.getInstance().settings.bedrockPortalSpawning || world.getRegistryKey()!=World.OVERWORLD
                || !world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)
                || world.random.nextInt(2000)>=world.getDifficulty().getId()) return;
        BlockPos floor=pos;
        while (floor.getY()>world.getBottomY() && world.getBlockState(floor).isOf(Blocks.NETHER_PORTAL)) floor=floor.down();
        Direction side=state.get(NetherPortalBlock.AXIS)==Direction.Axis.X ? Direction.NORTH : Direction.EAST;
        if (world.random.nextBoolean()) side=side.getOpposite();
        for (int i=0;i<2;i++,side=side.getOpposite()) {
            BlockPos spawn=floor.up().offset(side);
            if (!world.isChunkLoaded(spawn) || !world.getBlockState(spawn.down()).isSideSolidFullSquare(world,spawn.down(),Direction.UP)) continue;
            var pig=EntityType.ZOMBIFIED_PIGLIN.create(server);
            if (pig==null) return;
            pig.refreshPositionAndAngles(spawn.getX()+0.5,spawn.getY(),spawn.getZ()+0.5,world.random.nextFloat()*360,0);
            if (!world.isSpaceEmpty(pig)) continue;
            pig.initialize(server,world.getLocalDifficulty(spawn),SpawnReason.STRUCTURE,null,null);
            pig.resetPortalCooldown();
            server.spawnEntity(pig);
            return;
        }
    }
}
