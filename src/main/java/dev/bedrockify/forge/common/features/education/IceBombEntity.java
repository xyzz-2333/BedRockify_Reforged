package dev.bedrockify.forge.common.features.education;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public final class IceBombEntity extends ThrownItemEntity {
    public IceBombEntity(EntityType<? extends IceBombEntity> type,World world){super(type,world);}
    @Override protected Item getDefaultItem(){return EducationContent.ITEMS.get("ice_bomb");}
    @Override protected void onCollision(HitResult hit){
        super.onCollision(hit);if(getWorld().isClient)return;
        BlockPos center=BlockPos.ofFloored(hit.getPos());
        for(BlockPos pos:BlockPos.iterate(center.add(-1,-1,-1),center.add(1,1,1)))
            if(getWorld().isChunkLoaded(pos) && getWorld().getBlockState(pos).isOf(Blocks.WATER))getWorld().setBlockState(pos,Blocks.ICE.getDefaultState());
        getWorld().sendEntityStatus(this,(byte)3);discard();
    }
}
