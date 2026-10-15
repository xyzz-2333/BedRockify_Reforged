package dev.bedrockify.forge.common.features.education;

import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;

public final class UnderwaterTntBlock extends TntBlock {
    public UnderwaterTntBlock(Settings settings) { super(settings); }
    @Override public void onCaughtFire(BlockState state, World world, BlockPos pos, Direction face, LivingEntity owner) {
        if (world.isClient) return;
        spawn(world,pos,owner,false);
        world.playSound(null,pos,SoundEvents.ENTITY_TNT_PRIMED,SoundCategory.BLOCKS,1,1);
        world.emitGameEvent(owner,GameEvent.PRIME_FUSE,pos);
    }
    @Override public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        if (!world.isClient) spawn(world,pos,explosion.getCausingEntity(),true);
    }
    private void spawn(World world,BlockPos pos,LivingEntity owner,boolean chained) {
        var entity=new UnderwaterTntEntity(EducationContent.UNDERWATER_TNT_ENTITY,world);
        entity.setPosition(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);
        double angle=world.random.nextDouble()*Math.PI*2;
        entity.setVelocity(-Math.sin(angle)*0.02,0.2,-Math.cos(angle)*0.02);
        entity.setIgniter(owner);
        if(chained) entity.setFuse(world.random.nextInt(20)+10);
        world.spawnEntity(entity);
    }
}
