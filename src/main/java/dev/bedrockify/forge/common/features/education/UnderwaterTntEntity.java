package dev.bedrockify.forge.common.features.education;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.explosion.*;

public final class UnderwaterTntEntity extends TntEntity {
    private UUID igniter;
    public UnderwaterTntEntity(EntityType<? extends TntEntity> type,World world) { super(type,world); }
    public void setIgniter(LivingEntity entity) { igniter=entity==null?null:entity.getUuid(); }
    @Override public LivingEntity getOwner() {
        if(igniter!=null && getWorld() instanceof ServerWorld world && world.getEntity(igniter) instanceof LivingEntity entity) return entity;
        return null;
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);if(igniter!=null) nbt.putUuid("Igniter",igniter);
    }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);igniter=nbt.containsUuid("Igniter")?nbt.getUuid("Igniter"):null;
    }
    public static final ExplosionBehavior WATER_RESISTANCE = new ExplosionBehavior() {
        @Override public Optional<Float> getBlastResistance(Explosion explosion,BlockView world,BlockPos pos,BlockState state,FluidState fluid) {
            if(fluid.isIn(FluidTags.WATER)) return state.isAir() || state.getBlock() instanceof net.minecraft.block.FluidBlock
                    ? Optional.empty() : Optional.of(state.getBlock().getBlastResistance());
            return super.getBlastResistance(explosion,world,pos,state,fluid);
        }
        @Override public boolean canDestroyBlock(Explosion explosion,BlockView world,BlockPos pos,BlockState state,float power) {
            // Water transmits the blast, but is not itself removed by it.
            return !(state.getBlock() instanceof net.minecraft.block.FluidBlock) && super.canDestroyBlock(explosion,world,pos,state,power);
        }
    };
    @Override protected void explode() {
        getWorld().createExplosion(this,null,WATER_RESISTANCE,getX(),getBodyY(0.0625),getZ(),4F,false,World.ExplosionSourceType.TNT);
    }
}
