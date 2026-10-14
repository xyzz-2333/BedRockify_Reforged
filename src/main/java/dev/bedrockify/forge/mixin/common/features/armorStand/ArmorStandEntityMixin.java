package dev.bedrockify.forge.mixin.common.features.armorStand;

import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.common.features.armorStand.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ArmorStandEntity.class)
public class ArmorStandEntityMixin implements ArmorStandState {
    @Shadow private boolean isSlotDisabled(EquipmentSlot slot) { throw new AssertionError(); }
    @Shadow private boolean equip(PlayerEntity player, EquipmentSlot slot, ItemStack stack, Hand hand) { throw new AssertionError(); }
    @Unique private int bedrockify$pose;
    @Unique private int bedrockify$lastPower = -1;
    @Unique private boolean bedrockify$managedPose;
    @Unique private static boolean bedrockify$enabled() {
        return Bedrockify.getInstance()!=null && Bedrockify.getInstance().settings.bedrockArmorStand;
    }
    @Inject(method="<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V",at=@At("TAIL"))
    private void bedrockify$arms(EntityType<?> type, World world, CallbackInfo ci) {
        if (bedrockify$enabled()) ((ArmorStandEntity)(Object)this).setShowArms(true);
    }
    @Inject(method="interactAt",at=@At("HEAD"),cancellable=true)
    private void bedrockify$cycle(PlayerEntity player, Vec3d hit, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ArmorStandEntity stand=(ArmorStandEntity)(Object)this;
        if (!bedrockify$enabled() || stand.isMarker() || player.isSpectator()
                || player.getStackInHand(hand).isOf(Items.NAME_TAG)) return;
        if (!player.isSneaking()) {
            EquipmentSlot slot = ArmorStandInteraction.slotAt(stand, hit);
            if (slot == null) return;
            if (stand.getWorld().isClient) { cir.setReturnValue(ActionResult.CONSUME); return; }
            // Retain vanilla item counts, creative handling and all DisabledSlots lock bits.
            if (isSlotDisabled(slot)) { cir.setReturnValue(ActionResult.FAIL); return; }
            equip(player, slot, player.getStackInHand(hand), hand);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }
        if (hand!=Hand.MAIN_HAND) { cir.setReturnValue(ActionResult.CONSUME); return; }
        if (!stand.getWorld().isClient) {
            bedrockify$pose=ArmorStandPoses.normalize(bedrockify$pose+1);
            bedrockify$managedPose=true;
            stand.setShowArms(true);
            ArmorStandPoses.apply(stand,bedrockify$pose);
            player.sendMessage(Text.translatable("bedrockify.armorStand.pose",Text.translatable("bedrockify.armorStand.pose."+ArmorStandPoses.NAMES[bedrockify$pose])),true);
        }
        cir.setReturnValue(ActionResult.success(stand.getWorld().isClient));
    }
    @Inject(method="tick",at=@At("TAIL"))
    private void bedrockify$redstone(CallbackInfo ci) {
        ArmorStandEntity stand=(ArmorStandEntity)(Object)this;
        if (stand.getWorld().isClient || !bedrockify$enabled() || stand.isMarker()) return;
        var world=stand.getWorld();var pos=stand.getBlockPos();
        int power=world.getReceivedRedstonePower(pos.down());
        if (Bedrockify.getInstance().settings.armorStandExtendedRedstone)
            power=Math.max(power,Math.max(world.getReceivedRedstonePower(pos),world.getReceivedRedstonePower(pos.up())));
        if (power>0 && power!=bedrockify$lastPower) {
            bedrockify$pose=ArmorStandPoses.fromPower(power);
            bedrockify$managedPose=true;
            ArmorStandPoses.apply(stand,bedrockify$pose);
        }
        bedrockify$lastPower=power;
    }
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void bedrockify$save(NbtCompound nbt,CallbackInfo ci) {
        if (bedrockify$managedPose) nbt.putInt("BedrockifyPose",bedrockify$pose);
        nbt.putInt("BedrockifyLastPower",bedrockify$lastPower);
    }
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void bedrockify$load(NbtCompound nbt,CallbackInfo ci) {
        bedrockify$managedPose=nbt.contains("BedrockifyPose",3);
        bedrockify$pose=bedrockify$managedPose?ArmorStandPoses.normalize(nbt.getInt("BedrockifyPose")):0;
        bedrockify$lastPower=nbt.contains("BedrockifyLastPower",3)?Math.max(-1,Math.min(15,nbt.getInt("BedrockifyLastPower"))):-1;
        // Vanilla Pose and explicit ShowArms remain authoritative when loading a saved stand.
        if (bedrockify$enabled() && !nbt.contains("ShowArms",1)) ((ArmorStandEntity)(Object)this).setShowArms(true);
    }
    public int bedrockify$getPose() { return bedrockify$pose; }
}
