package dev.bedrockify.forge.mixin.common.features.mechanics;
import dev.bedrockify.forge.common.features.mechanics.TridentKillerState;
import dev.bedrockify.forge.Bedrockify;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(TridentEntity.class)
public class TridentEntityMixin implements TridentKillerState {
    @Shadow private ItemStack tridentStack;
    @Unique private boolean bedrockify$killer;
    public void bedrockify$activate() {
        TridentEntity self=(TridentEntity)(Object)this;
        if (!self.getWorld().isClient && Bedrockify.getInstance().settings.bedrockTridentKiller
                && EnchantmentHelper.getLoyalty(tridentStack)==0) bedrockify$killer=true;
    }
    @Inject(method="tick", at=@At("TAIL"))
    private void bedrockify$kill(CallbackInfo ci) {
        TridentEntity self=(TridentEntity)(Object)this;
        if (self.getWorld().isClient || !bedrockify$killer || self.isRemoved() || self.age%2!=0
                || !Bedrockify.getInstance().settings.bedrockTridentKiller || EnchantmentHelper.getLoyalty(tridentStack)>0) return;
        Entity owner=self.getOwner();
        if (owner==null || !owner.isAlive()) return;
        for (MobEntity mob:self.getWorld().getEntitiesByClass(MobEntity.class,self.getBoundingBox().expand(0.3),Entity::isAlive)) {
            int previous=mob.timeUntilRegen;
            mob.timeUntilRegen=0;
            float damage=8.0F+EnchantmentHelper.getAttackDamage(tridentStack,mob.getGroup());
            if (!mob.damage(self.getDamageSources().trident(self,owner),damage)) mob.timeUntilRegen=previous;
        }
    }
    @Inject(method="age", at=@At("HEAD"), cancellable=true)
    private void bedrockify$preserve(CallbackInfo ci) {
        if (bedrockify$killer && Bedrockify.getInstance().settings.bedrockTridentKiller && EnchantmentHelper.getLoyalty(tridentStack)==0) ci.cancel();
    }
    @Inject(method="writeCustomDataToNbt", at=@At("TAIL"))
    private void bedrockify$save(NbtCompound nbt, CallbackInfo ci) { nbt.putBoolean("BedrockifyTridentKiller",bedrockify$killer); }
    @Inject(method="readCustomDataFromNbt", at=@At("TAIL"))
    private void bedrockify$load(NbtCompound nbt, CallbackInfo ci) { bedrockify$killer=nbt.getBoolean("BedrockifyTridentKiller"); }
}
