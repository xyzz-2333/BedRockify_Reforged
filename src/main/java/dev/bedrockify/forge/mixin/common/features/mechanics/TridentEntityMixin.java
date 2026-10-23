package dev.bedrockify.forge.mixin.common.features.mechanics;
import dev.bedrockify.forge.common.features.mechanics.TridentKillerState;
import dev.bedrockify.forge.common.features.mechanics.BedrockTridentDamage;
import dev.bedrockify.forge.Bedrockify;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(TridentEntity.class)
public class TridentEntityMixin implements TridentKillerState {
    @Shadow private ItemStack tridentStack;
    @Unique private boolean bedrockify$killer;
    @Unique private boolean bedrockify$pendingHit;
    @Unique private boolean bedrockify$observedBlocks;
    @Unique private long bedrockify$blocks;
    public void bedrockify$activate() {
        TridentEntity self=(TridentEntity)(Object)this;
        if (!self.getWorld().isClient && Bedrockify.getInstance().settings.bedrockTridentKiller
                && EnchantmentHelper.getLoyalty(tridentStack)==0) {
            bedrockify$killer=true;
            bedrockify$pendingHit=true;
        }
    }
    @Inject(method="tick", at=@At("TAIL"))
    private void bedrockify$kill(CallbackInfo ci) {
        TridentEntity self=(TridentEntity)(Object)this;
        if (self.getWorld().isClient || self.isRemoved()
                || !Bedrockify.getInstance().settings.bedrockTridentKiller || EnchantmentHelper.getLoyalty(tridentStack)>0) return;
        if (!bedrockify$observedBlocks && !bedrockify$killer && self.getVelocity().lengthSquared()>=0.0001) return;
        long current=bedrockify$nearbyBlocks(self);
        if (bedrockify$observedBlocks && current!=bedrockify$blocks) bedrockify$activate();
        bedrockify$blocks=current;
        bedrockify$observedBlocks=true;
        if (!bedrockify$pendingHit) return;
        bedrockify$pendingHit=false;
        Entity owner=self.getOwner();
        if (owner==null || !owner.isAlive()) return;
        for (MobEntity mob:self.getWorld().getEntitiesByClass(MobEntity.class,self.getBoundingBox().expand(0.3),Entity::isAlive)) {
            float damage=BedrockTridentDamage.withBedrockImpaling(tridentStack,mob,
                    8.0F+EnchantmentHelper.getAttackDamage(tridentStack,mob.getGroup()));
            mob.damage(self.getDamageSources().trident(self,owner),damage);
        }
    }
    @Unique private long bedrockify$nearbyBlocks(TridentEntity self) {
        BlockPos center=self.getBlockPos();
        long fingerprint=0;
        for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) for (int z=-1;z<=1;z++) {
            BlockState state=self.getWorld().getBlockState(center.add(x,y,z));
            Block block=state.getBlock();
            if (block instanceof TrapdoorBlock || block instanceof CarpetBlock || block instanceof PistonBlock
                    || state.isOf(Blocks.PISTON_HEAD) || state.isOf(Blocks.MOVING_PISTON)) {
                fingerprint=fingerprint*31+state.hashCode()*31L+(x+1)*9+(y+1)*3+z+1;
            }
        }
        return fingerprint;
    }
    @Redirect(method="onEntityHit", at=@At(value="INVOKE",
            target="Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean bedrockify$directImpaling(Entity target, net.minecraft.entity.damage.DamageSource source, float damage) {
        if (Bedrockify.getInstance().settings.bedrockProjectiles && target instanceof LivingEntity living)
            damage=BedrockTridentDamage.withBedrockImpaling(tridentStack,living,damage);
        return target.damage(source,damage);
    }
    @Inject(method="age", at=@At("HEAD"), cancellable=true)
    private void bedrockify$preserve(CallbackInfo ci) {
        if (Bedrockify.getInstance().settings.bedrockProjectiles) ci.cancel();
    }
    @Inject(method="writeCustomDataToNbt", at=@At("TAIL"))
    private void bedrockify$save(NbtCompound nbt, CallbackInfo ci) { nbt.putBoolean("BedrockifyTridentKiller",bedrockify$killer); }
    @Inject(method="readCustomDataFromNbt", at=@At("TAIL"))
    private void bedrockify$load(NbtCompound nbt, CallbackInfo ci) { bedrockify$killer=nbt.getBoolean("BedrockifyTridentKiller"); }
}
