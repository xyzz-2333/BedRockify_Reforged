package dev.bedrockify.forge.common.features.education;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
public final class HeatBlock extends Block {
    public HeatBlock(Settings settings){super(settings);}
    @Override public void onBlockAdded(BlockState state,World world,BlockPos pos,BlockState old,boolean notify){if(!world.isClient)world.scheduleBlockTick(pos,this,20);}
    @Override public void scheduledTick(BlockState state,ServerWorld world,BlockPos pos,Random random){
        for(BlockPos target:BlockPos.iterate(pos.add(-1,-1,-1),pos.add(1,1,1))){
            if(!world.isChunkLoaded(target))continue;var block=world.getBlockState(target);
            if(block.isOf(Blocks.ICE))world.setBlockState(target,world.getDimension().ultrawarm()?Blocks.AIR.getDefaultState():Blocks.WATER.getDefaultState());
            else if(block.isOf(Blocks.SNOW))world.removeBlock(target,false);
        }
        world.scheduleBlockTick(pos,this,20);
    }
}
