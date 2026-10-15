package dev.bedrockify.forge.common.features.education;
import net.minecraft.item.*;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
public final class SuperFertilizerItem extends Item {
    public SuperFertilizerItem(Settings settings){super(settings);}
    @Override public ActionResult useOnBlock(ItemUsageContext context){
        var world=context.getWorld();var pos=context.getBlockPos();var state=world.getBlockState(pos);
        if(!(state.getBlock() instanceof Fertilizable plant) || !plant.isFertilizable(world,pos,state,world.isClient))return ActionResult.PASS;
        if(world instanceof ServerWorld server){
            if(state.getBlock() instanceof CropBlock crop)world.setBlockState(pos,crop.withAge(crop.getMaxAge()),3);
            else for(int i=0;i<4;i++){var current=world.getBlockState(pos);if(current.getBlock()!=state.getBlock() || !plant.isFertilizable(world,pos,current,false))break;plant.grow(server,world.random,pos,current);}
            if(context.getPlayer()==null || !context.getPlayer().isCreative())context.getStack().decrement(1);
            world.syncWorldEvent(1505,pos,0);
        }
        return ActionResult.success(world.isClient);
    }
}
