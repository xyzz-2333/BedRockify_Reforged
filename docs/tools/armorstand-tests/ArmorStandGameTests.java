package dev.bedrockify.forge.tests;

import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.common.features.armorStand.ArmorStandState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.hit.BlockHitResult;
import dev.bedrockify.forge.common.features.armorStand.ArmorStandInteraction;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("bedrockify")
@PrefixGameTestTemplate(false)
public class ArmorStandGameTests {
    @GameTest(templateName="empty")
    public static void poseAndEquipment(TestContext c) {
        var stand=c.spawnEntity(EntityType.ARMOR_STAND,2,2,2);stand.setNoGravity(true);
        c.assertTrue(stand.shouldShowArms(),"New stands show arms");
        var player=FakePlayerFactory.getMinecraft(c.getWorld());
        player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.TORCH,3));player.setSneaking(true);
        stand.interactAt(player,new Vec3d(0,0.5,0),Hand.MAIN_HAND);
        c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==1,"Sneaking selects neutral pose");
        c.assertTrue(player.getMainHandStack().getCount()==3 && stand.getMainHandStack().isEmpty(),"Posing does not move items");
        for(int i=0;i<12;i++)stand.interactAt(player,new Vec3d(0,0.5,0),Hand.MAIN_HAND);
        c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==0,"13 poses wrap to default");
        player.setSneaking(false);
        stand.interactAt(player,new Vec3d(0,0.5,0),Hand.MAIN_HAND);
        c.assertTrue(stand.getMainHandStack().isOf(Items.TORCH) && stand.getMainHandStack().getCount()==1,"Equip one held item");
        c.assertTrue(player.getMainHandStack().getCount()==2,"Survival inventory decremented once");
        player.setSneaking(true);
        stand.interactAt(player,new Vec3d(0,0.5,0),Hand.MAIN_HAND);
        stand.interactAt(player,new Vec3d(0,0.5,0),Hand.OFF_HAND);
        c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==1,"Offhand does not cycle a second time");
        player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.NAME_TAG));
        stand.interactAt(player,new Vec3d(0,0.5,0),Hand.MAIN_HAND);
        c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==1,"Name tags do not cycle poses");
        var nbt=new NbtCompound();stand.writeCustomDataToNbt(nbt);
        var restored=new ArmorStandEntity(EntityType.ARMOR_STAND,c.getWorld());restored.readCustomDataFromNbt(nbt);
        c.assertTrue(((ArmorStandState)restored).bedrockify$getPose()==1 && restored.getMainHandStack().isOf(Items.TORCH),"Non-default pose and equipment survive NBT roundtrip");
        c.assertTrue(restored.getLeftArmRotation().equals(stand.getLeftArmRotation()),"Saved limb rotations restored");
        player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
        player.setSneaking(false);
        c.complete();
    }
    @GameTest(templateName="empty")
    public static void redstoneAndCustomPose(TestContext c) {
        var stand=c.spawnEntity(EntityType.ARMOR_STAND,2,2,2);stand.setNoGravity(true);
        stand.tick();var pos=stand.getBlockPos();
        c.getWorld().setBlockState(pos.down().east(),Blocks.REDSTONE_BLOCK.getDefaultState());stand.tick();
        c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==12,"Signal 15 selects hero pose");
        c.assertTrue(stand.getRightArmRotation().getPitch()==-99,"Hero rotation applied");
        c.getWorld().setBlockState(pos.down().east(),Blocks.AIR.getDefaultState());stand.tick();
        c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==12,"Power off retains previous pose");
        var nbt=new NbtCompound();stand.writeCustomDataToNbt(nbt);nbt.remove("BedrockifyPose");
        var head=new NbtList();head.add(NbtFloat.of(44));head.add(NbtFloat.of(0));head.add(NbtFloat.of(0));
        var pose=new NbtCompound();pose.put("Head",head);nbt.put("Pose",pose);nbt.putBoolean("ShowArms",false);
        stand.readCustomDataFromNbt(nbt);stand.tick();
        c.assertTrue(stand.getHeadRotation().getPitch()==44 && !stand.shouldShowArms(),"Unpowered saved custom pose and explicit arms preserved");
        c.complete();
    }
    @GameTest(templateName="empty")
    public static void disabledAndSlots(TestContext c) {
        Bedrockify.getInstance().settings.bedrockArmorStand=false;
        try {
            var stand=new ArmorStandEntity(EntityType.ARMOR_STAND,c.getWorld());
            c.assertFalse(stand.shouldShowArms(),"Disabled feature restores vanilla constructor");
        } finally {Bedrockify.getInstance().settings.bedrockArmorStand=true;}
        var stand=c.spawnEntity(EntityType.ARMOR_STAND,2,2,2);stand.setNoGravity(true);
        var nbt=new NbtCompound();stand.writeCustomDataToNbt(nbt);nbt.putInt("DisabledSlots",1);stand.readCustomDataFromNbt(nbt);
        var player=FakePlayerFactory.getMinecraft(c.getWorld());player.setSneaking(false);player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.TORCH,3));
        stand.interactAt(player,new Vec3d(0,0.5,0),Hand.MAIN_HAND);
        c.assertTrue(stand.getMainHandStack().isEmpty() && player.getMainHandStack().getCount()==3,"Disabled hand slots cannot transfer items");
        player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
        c.complete();
    }

    @GameTest(templateName="empty")
    public static void armsWithArmor(TestContext c) {
        var stand=c.spawnEntity(EntityType.ARMOR_STAND,2,2,2); stand.setNoGravity(true);
        stand.setYaw(0); stand.setRightArmRotation(new EulerAngle(0,0,0)); stand.setLeftArmRotation(new EulerAngle(0,0,0));
        stand.equipStack(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
        stand.equipStack(EquipmentSlot.LEGS,new ItemStack(Items.DIAMOND_LEGGINGS));
        stand.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
        stand.equipStack(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
        var player=FakePlayerFactory.getMinecraft(c.getWorld()); player.setSneaking(false);
        player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
        Vec3d right=new Vec3d(-0.375,1.1,0),left=new Vec3d(0.375,1.1,0);
        c.assertTrue(ArmorStandInteraction.slotAt(stand,right)==EquipmentSlot.MAINHAND,"Right arm targets main hand");
        stand.interactAt(player,right,Hand.MAIN_HAND);
        c.assertTrue(player.getMainHandStack().isOf(Items.DIAMOND_SWORD) && stand.getMainHandStack().isEmpty(),"Chest and leggings do not intercept main hand removal");
        player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
        stand.interactAt(player,left,Hand.MAIN_HAND);
        c.assertTrue(player.getMainHandStack().isOf(Items.SHIELD) && stand.getOffHandStack().isEmpty(),"Left arm removes offhand");
        c.assertTrue(stand.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.DIAMOND_CHESTPLATE) && stand.getEquippedStack(EquipmentSlot.LEGS).isOf(Items.DIAMOND_LEGGINGS),"Armor remains equipped");
        var start=stand.getPos().add(-0.375,1.1,-2);var end=start.add(0,0,4);
        c.assertTrue(stand.getBoundingBox().raycast(start,end).isEmpty(),"Test ray misses vanilla physical box");
        c.assertTrue(ArmorStandInteraction.raycast(stand,start,end)!=null,"Arm outside physical box is targetable");
        c.assertTrue(ArmorStandInteraction.raycast(stand,start.add(-0.5,0,0),end.add(-0.5,0,0))==null,"Empty space outside arm is not targetable");
        stand.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Items.TORCH));
        var nbt=new NbtCompound();stand.writeCustomDataToNbt(nbt);nbt.putInt("DisabledSlots",1<<8);stand.readCustomDataFromNbt(nbt);
        player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);stand.interactAt(player,right,Hand.MAIN_HAND);
        c.assertTrue(player.getMainHandStack().isEmpty() && stand.getMainHandStack().isOf(Items.TORCH),"Hand removal lock is preserved");
        stand.setYaw(45);
        c.assertTrue(ArmorStandInteraction.slotAt(stand,new Vec3d(-0.375/Math.sqrt(2),1.1,-0.375/Math.sqrt(2)))==EquipmentSlot.MAINHAND,"Diagonal rotation preserves arm selection");
        c.complete();
    }

    @GameTest(templateName="empty")
    public static void eightDirectionPlacement(TestContext c) {
        var player=FakePlayerFactory.getMinecraft(c.getWorld()); player.setSneaking(false);
        var floor=c.getAbsolutePos(new BlockPos(2,1,2));
        c.getWorld().setBlockState(floor,Blocks.STONE.getDefaultState());
        for(int direction=0;direction<8;direction++) {
            float yaw=direction*45F;player.setYaw(yaw);player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.ARMOR_STAND));
            var hit=new BlockHitResult(Vec3d.ofCenter(floor).add(0,0.5,0),Direction.UP,floor,false);
            var result=Items.ARMOR_STAND.useOnBlock(new ItemUsageContext(player,Hand.MAIN_HAND,hit));
            c.assertTrue(result.isAccepted(),"Placement accepted at yaw "+yaw);
            var stands=c.getWorld().getEntitiesByClass(ArmorStandEntity.class,new Box(floor.up()).expand(0.25),e->true);
            c.assertTrue(stands.size()==1,"Exactly one stand placed");var stand=stands.get(0);
            float expected=MathHelper.wrapDegrees(yaw-180);
            c.assertTrue(Math.abs(MathHelper.wrapDegrees(stand.getYaw()-expected))<0.01,"Placed yaw preserves eighth direction "+yaw);
            for(int tick=0;tick<5;tick++)stand.tick();
            c.assertTrue(Math.abs(MathHelper.wrapDegrees(stand.getYaw()-expected))<0.01,"Ticks preserve eighth direction "+yaw);
            var saved=new NbtCompound();stand.writeNbt(saved);var loaded=new ArmorStandEntity(EntityType.ARMOR_STAND,c.getWorld());loaded.readNbt(saved);
            c.assertTrue(Math.abs(MathHelper.wrapDegrees(loaded.getYaw()-expected))<0.01,"Reload preserves eighth direction "+yaw);
            stand.discard();
        }
        player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);c.complete();
    }

    @GameTest(templateName="empty")
    public static void redstoneStrengthAndRange(TestContext c) {
        var settings=Bedrockify.getInstance().settings;boolean saved=settings.armorStandExtendedRedstone;
        try {
            settings.armorStandExtendedRedstone=false;
            var stand=c.spawnEntity(EntityType.ARMOR_STAND,2,2,2);stand.setNoGravity(true);stand.tick();
            var pos=stand.getBlockPos();var support=pos.down();
            c.getWorld().setBlockState(pos.east(),Blocks.REDSTONE_BLOCK.getDefaultState());stand.tick();
            c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==0,"Default mode ignores power beside stand itself");
            settings.armorStandExtendedRedstone=true;stand.tick();
            c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==12,"Extended mode accepts power beside stand");
            settings.armorStandExtendedRedstone=false;stand.tick();
            c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==12,"Returning to zero input retains pose");
            c.getWorld().setBlockState(pos.east(),Blocks.AIR.getDefaultState());
            var plate=Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.getDefaultState();
            var power=net.minecraft.state.property.Properties.POWER;
            c.getWorld().setBlockState(support.west(),plate.with(power,3),2);
            c.getWorld().setBlockState(support.east(),plate.with(power,8),2);stand.tick();
            c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==8,"Multiple sources choose highest power");
            c.getWorld().setBlockState(support.east(),plate.with(power,5),2);stand.tick();
            c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==5,"8 to 5 updates without power off");
            c.getWorld().setBlockState(support.east(),Blocks.AIR.getDefaultState(),2);stand.tick();
            c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==3,"Removing highest source falls back to remaining source");
            c.getWorld().setBlockState(support.west(),Blocks.AIR.getDefaultState(),2);stand.tick();
            c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==3,"Power off retains last pose");
            c.getWorld().setBlockState(support.west(),plate.with(power,7),2);stand.tick();
            c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==7,"Power restored updates pose");
            var player=FakePlayerFactory.getMinecraft(c.getWorld());player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);player.setSneaking(true);
            stand.interactAt(player,new Vec3d(0,0.5,0),Hand.MAIN_HAND);player.setSneaking(false);stand.tick();
            c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==8,"Unchanged input does not overwrite manual pose");
            c.getWorld().setBlockState(support.west(),plate.with(power,6),2);stand.tick();
            c.assertTrue(((ArmorStandState)stand).bedrockify$getPose()==6,"Next changed input updates manual pose");
            c.complete();
        } finally {settings.armorStandExtendedRedstone=saved;}
    }

    @GameTest(templateName="empty")
    public static void sharpnessDamage(TestContext c) {
        var settings=Bedrockify.getInstance().settings;boolean saved=settings.bedrockSharpness;
        var player=FakePlayerFactory.getMinecraft(c.getWorld());
        var attribute=player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
        double oldBase=attribute.getBaseValue();
        try {
            settings.bedrockSharpness=true;
            for(int level=1;level<=5;level++) {
                float value=net.minecraft.enchantment.Enchantments.SHARPNESS.getAttackDamage(level,EntityGroup.DEFAULT);
                c.assertTrue(value==1.25F*level,"Sharpness level "+level+" damage");
            }
            c.assertTrue(net.minecraft.enchantment.Enchantments.SMITE.getAttackDamage(5,EntityGroup.UNDEAD)==12.5F,"Smite unchanged");
            c.assertTrue(net.minecraft.enchantment.Enchantments.BANE_OF_ARTHROPODS.getAttackDamage(5,EntityGroup.ARTHROPOD)==12.5F,"Bane unchanged");
            var weapon=new ItemStack(Items.STICK);weapon.addEnchantment(net.minecraft.enchantment.Enchantments.SHARPNESS,5);
            player.setStackInHand(Hand.MAIN_HAND,weapon);player.setOnGround(true);player.fallDistance=0;player.setSprinting(false);
            attribute.setBaseValue(7);
            var target=c.spawnEntity(EntityType.COW,2,2,2);target.setAiDisabled(true);
            target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(40);target.setHealth(40);
            player.attack(target);
            c.assertTrue(Math.abs(target.getHealth()-26.75F)<0.001,"Actual attack applies 7 + 6.25 damage, remaining health: "+target.getHealth());
            settings.bedrockSharpness=false;
            c.assertTrue(net.minecraft.enchantment.Enchantments.SHARPNESS.getAttackDamage(5,EntityGroup.DEFAULT)==3F,"Disabled restores Java Sharpness");
            c.complete();
        } finally {settings.bedrockSharpness=saved;attribute.setBaseValue(oldBase);player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);}
    }
}
