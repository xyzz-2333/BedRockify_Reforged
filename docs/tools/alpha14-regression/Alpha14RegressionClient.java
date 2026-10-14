package dev.bedrockify.forge.tests;

import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.biomeWaterColors.BedrockWaterColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.text.Text;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraftforge.common.util.FakePlayerFactory;
import dev.bedrockify.forge.common.features.armorStand.ArmorStandInteraction;
import dev.bedrockify.forge.common.features.armorStand.ArmorStandPoses;
import net.minecraft.client.render.entity.model.ArmorStandEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Runs only in a disposable copied GameTest world, never in a distributed JAR. */
@Mod.EventBusSubscriber(modid="bedrockify", value=Dist.CLIENT)
public final class Alpha14RegressionClient {
    private static int ticks;
    private static boolean finished;
    private static boolean prepared;
    private static void check(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var client=MinecraftClient.getInstance();
        if (client.currentScreen instanceof AccessibilityOnboardingScreen) client.setScreen(new TitleScreen());
        if (finished || client.player==null || client.world==null) return;
        var player=client.player;
        if (!prepared) {
            prepared=true;
            verifyModelArms(client);
            verifyArmPicking(client);
            if (net.minecraftforge.fml.ModList.get().isLoaded("shouldersurfing")) {
                try {
                    var type=Class.forName("com.github.exopandora.shouldersurfing.client.ShoulderSurfingImpl");
                    var instance=type.getMethod("getInstance").invoke(null);
                    type.getMethod("setShoulderSurfing",boolean.class).invoke(instance,true);
                    check((boolean)type.getMethod("isShoulderSurfing").invoke(instance),"Shoulder camera activated");
                    Bedrockify.LOGGER.info("ALPHA14 SHOULDER CAMERA ACTIVE: rendering regression with shoulder mode enabled");
                } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            }
            verifyCatalog(client);
            client.getServer().execute(()-> {
                var world=client.getServer().getOverworld();
                for(var old:world.getEntitiesByClass(ArmorStandEntity.class,new Box(-4,145,-4,16,160,8),e->e.getName().getString().startsWith("RegressionYaw:"))) old.discard();
                var fake=FakePlayerFactory.getMinecraft(world);
                for (int direction=0;direction<8;direction++) {
                    var floor=new BlockPos(2+direction,149,2);world.setBlockState(floor,Blocks.STONE.getDefaultState());
                    fake.setYaw(direction*45F);fake.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.ARMOR_STAND));
                    Items.ARMOR_STAND.useOnBlock(new ItemUsageContext(fake,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(floor).add(0,0.5,0),Direction.UP,floor,false)));
                    for(var stand:world.getEntitiesByClass(ArmorStandEntity.class,new Box(floor.up()),e->true)) {
                        stand.setNoGravity(true);stand.setCustomName(Text.literal("RegressionYaw:"+MathHelper.wrapDegrees(direction*45F-180)));
                    }
                }
            });
        }
        int stage=ticks/30;
        if (stage>=4) {
            int directions=0;
            for(var entity:client.world.getEntities()) {
                if (!(entity instanceof ArmorStandEntity stand) || !stand.getName().getString().startsWith("RegressionYaw:")) continue;
                float expected=Float.parseFloat(stand.getName().getString().substring("RegressionYaw:".length()));
                check(Math.abs(MathHelper.wrapDegrees(stand.getYaw()-expected))<0.1,"Network yaw "+expected+" became "+stand.getYaw());
                check(Math.abs(MathHelper.wrapDegrees(stand.bodyYaw-expected))<0.1,"Rendered body yaw "+expected+" became "+stand.bodyYaw);
                directions++;
            }
            check(directions==8,"All eight networked stands observed: "+directions);
            Bedrockify.LOGGER.info("ALPHA14 DIRECTION PASS: eight server-placed stands retain client yaw and rendered body yaw after 120 ticks");
            finished=true;
            client.options.forwardKey.setPressed(false);client.options.leftKey.setPressed(false);
            client.options.sneakKey.setPressed(false);client.options.useKey.setPressed(false);
            BedrockifyClient.getInstance().settings.sneakingShield=true;
            check(BedrockWaterColors.size()==64,"Merged water palette loaded");
            Bedrockify.LOGGER.info("ALPHA14 REGRESSION PASS: actual movement input, crouching shield 0.3, standing shield 0.2, crouching food 0.06, disabled feature 0.06, merged water palette");
            client.scheduleStop();return;
        }
        if (event.phase==TickEvent.Phase.START) {
            client.setScreen(null);
            client.options.pauseOnLostFocus=false;
            client.options.forwardKey.setPressed(true);client.options.leftKey.setPressed(true);
            client.options.sneakKey.setPressed(stage!=1);client.options.useKey.setPressed(true);
            player.getAbilities().flying=false;player.setNoGravity(true);player.setVelocity(Vec3d.ZERO);
            player.setPosition(0,150,0);
            BedrockifyClient.getInstance().settings.sneakingShield=stage!=3;
            var item=stage==2 ? Items.HONEY_BOTTLE : Items.SHIELD;
            if (!player.getMainHandStack().isOf(item)) { player.clearActiveItem(); player.setStackInHand(Hand.MAIN_HAND,new ItemStack(item)); }
            player.setCurrentHand(Hand.MAIN_HAND);
        } else {
            if (ticks%30>10) {
                float expected=stage==1 ? 0.2F : stage==0 ? 0.3F : 0.06F;
                if (net.minecraftforge.fml.ModList.get().isLoaded("shouldersurfing")) {
                    // Shoulder mode rotates the input vector to match its camera; compare speed, not axes.
                    check(Math.abs(Math.hypot(player.input.movementForward,player.input.movementSideways)-expected*Math.sqrt(2))<0.0001,
                            "Shoulder movement magnitude stage "+stage);
                } else {
                    check(Math.abs(player.input.movementForward-expected)<0.0001,"Forward slowdown stage "+stage);
                    check(Math.abs(player.input.movementSideways-expected)<0.0001,"Sideways slowdown stage "+stage);
                }
            }
            ticks++;
        }
    }

    private static void verifyArmPicking(MinecraftClient client) {
        var stand=new ArmorStandEntity(EntityType.ARMOR_STAND,client.world);
        stand.setPosition(100,200,100);stand.setShowArms(true);
        stand.setLeftArmRotation(new EulerAngle(0,0,0));stand.setRightArmRotation(new EulerAngle(0,0,0));
        client.world.addEntity(190000001,stand);
        boolean enabled=Bedrockify.getInstance().settings.bedrockArmorStand;
        try {
            Bedrockify.getInstance().settings.bedrockArmorStand=true;
            for(int angle=0;angle<360;angle+=45) {
                stand.setYaw(angle);
                double yaw=Math.toRadians(angle),c=Math.cos(yaw),s=Math.sin(yaw);
                Vec3d start=stand.getPos().add(c*0.4+s*2,1.3,s*0.4-c*2);
                Vec3d end=stand.getPos().add(c*0.4-s*2,1.3,s*0.4+c*2);
                var box=new Box(start,end).expand(0.1);
                var camera=client.getCameraEntity();
                var hit=net.minecraft.entity.projectile.ProjectileUtil.raycast(camera,start,end,box,e->e==stand,16);
                check(hit!=null && hit.getEntity()==stand,"Arm ray at yaw "+angle);
                check(net.minecraft.entity.projectile.ProjectileUtil.raycast(camera,start,end,box,e->e==stand,1)==null,"Occlusion/reach limit at yaw "+angle);
                check(net.minecraft.entity.projectile.ProjectileUtil.raycast(camera,start,end,box,e->false,16)==null,"Caller predicate respected");
                Bedrockify.getInstance().settings.bedrockArmorStand=false;
                check(net.minecraft.entity.projectile.ProjectileUtil.raycast(camera,start,end,box,e->e==stand,16)==null,"Disabled arm extension");
                Bedrockify.getInstance().settings.bedrockArmorStand=true;
            }
            Bedrockify.LOGGER.info("ALPHA14 ARM PICK PASS: eight directions, caller ray, distance cap, predicate and disabled setting");
        } finally {
            Bedrockify.getInstance().settings.bedrockArmorStand=enabled;
            client.world.removeEntity(stand.getId(),net.minecraft.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    private static void verifyModelArms(MinecraftClient client) {
        var stand=new ArmorStandEntity(EntityType.ARMOR_STAND,client.world);
        var root=client.getEntityModelLoader().getModelPart(EntityModelLayers.ARMOR_STAND);
        var model=new ArmorStandEntityModel(root);
        for(boolean small:new boolean[]{false,true}) {
            var nbt=new net.minecraft.nbt.NbtCompound();stand.writeCustomDataToNbt(nbt);nbt.putBoolean("Small",small);stand.readCustomDataFromNbt(nbt);
            for(int pose=0;pose<13;pose++) for(int direction=0;direction<8;direction++) {
                stand.setYaw(direction*45F);ArmorStandPoses.apply(stand,pose);
                stand.prevYaw=stand.getYaw();
                model.animateModel(stand,0,0,0.5F);
                check(root.getChild("base_plate").yaw==0,"Base plate follows stand instead of counter-rotating");
                model.setAngles(stand,0,0,0,0,0);
                for(boolean left:new boolean[]{false,true}) {
                    var matrices=new MatrixStack();
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180-stand.getYaw()));
                    matrices.scale(-1,-1,1);matrices.translate(0,-1.501,0);
                    if(small) {matrices.scale(0.5F,0.5F,0.5F);matrices.translate(0,1.5,0);}
                    (left?model.leftArm:model.rightArm).rotate(matrices);
                    var point=matrices.peek().getPositionMatrix().transformPosition(new Vector3f((left?1F:-1F)/16,6F/16,0));
                    var hit=new Vec3d(point.x,point.y,point.z);
                    check(ArmorStandInteraction.slotAt(stand,hit)!=null,"Actual model arm not targetable: pose "+pose+", direction "+direction+", small "+small+", left "+left);
                }
            }
        }
        boolean saved=Bedrockify.getInstance().settings.bedrockArmorStand;
        try {
            Bedrockify.getInstance().settings.bedrockArmorStand=false;stand.setYaw(45);stand.prevYaw=45;
            model.animateModel(stand,0,0,1);
            check(Math.abs(root.getChild("base_plate").yaw+(float)Math.PI/4)<0.001,"Disabled feature restores vanilla base plate");
        } finally {Bedrockify.getInstance().settings.bedrockArmorStand=saved;}
        Bedrockify.LOGGER.info("ALPHA14 MODEL PASS: both actual model arms, 13 poses, eight rotations, normal and small stands");
    }

    private static void verifyCatalog(MinecraftClient client) {
        net.minecraft.item.ItemGroups.updateDisplayContext(client.world.getEnabledFeatures(),true,client.world.getRegistryManager());
        var expected=java.util.Map.of("copper_blocks",32,"goat_horns",8,"paintings",2,"infested_blocks",7,"banner_patterns",6,"pottery_sherds",20,"armor_trim_templates",16);
        for(var entry:expected.entrySet()) {
            String family=entry.getKey();
            var category=family.equals("copper_blocks") ? dev.bedrockify.forge.client.features.creativeInventory.CreativeCatalog.Category.CONSTRUCTION
                    : family.equals("goat_horns") ? dev.bedrockify.forge.client.features.creativeInventory.CreativeCatalog.Category.EQUIPMENT
                    : dev.bedrockify.forge.client.features.creativeInventory.CreativeCatalog.Category.ITEMS;
            var members=dev.bedrockify.forge.client.features.creativeInventory.CreativeCatalog.contents(category).stream()
                    .filter(stack->family.equals(dev.bedrockify.forge.client.features.creativeInventory.CreativeGroups.family(stack))).toList();
            check(family.equals("paintings") ? members.size()>=entry.getValue() : members.size()==entry.getValue(),family+" count "+members.size());
            var collapsed=dev.bedrockify.forge.client.features.creativeInventory.CreativeGroups.layout(members,java.util.Set.of(),true,true);
            check(collapsed.size()==1 && collapsed.get(0).header() && collapsed.get(0).count()==members.size(),family+" collapsed header");
            var expanded=dev.bedrockify.forge.client.features.creativeInventory.CreativeGroups.layout(members,java.util.Set.of(family),true,true);
            check(expanded.size()==members.size()+1,family+" expansion count");
            for(int i=0;i<members.size();i++) check(ItemStack.areEqual(members.get(i),expanded.get(i+1).stack()),family+" variant NBT retained");
            Bedrockify.LOGGER.info("ALPHA14 GROUP PASS: {} = {} variants",family,members.size());
        }
        check(dev.bedrockify.forge.client.features.creativeInventory.CreativeGroups.family(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE))==null,"Netherite upgrade remains independent");
        check(!"copper_blocks".equals(dev.bedrockify.forge.client.features.creativeInventory.CreativeGroups.family(new ItemStack(Items.COPPER_ORE))),"Copper ore is not a building copper block");
        check(dev.bedrockify.forge.client.features.creativeInventory.CreativeGroups.family(new ItemStack(Items.RAW_COPPER_BLOCK))==null,"Raw copper remains independent");
        boolean saved=Bedrockify.getInstance().settings.bedrockSharpness;
        try {
            Bedrockify.getInstance().settings.bedrockSharpness=true;
            dev.bedrockify.forge.common.features.mechanics.MechanicsRules.acceptServerRules(true,false);
            check(net.minecraft.enchantment.Enchantments.SHARPNESS.getAttackDamage(5,net.minecraft.entity.EntityGroup.DEFAULT)==3F,"Remote server overrides local Sharpness");
            dev.bedrockify.forge.common.features.mechanics.MechanicsRules.acceptServerRules(true,true);
            check(net.minecraft.enchantment.Enchantments.SHARPNESS.getAttackDamage(5,net.minecraft.entity.EntityGroup.DEFAULT)==6.25F,"Server Sharpness enabled");
            dev.bedrockify.forge.common.features.mechanics.MechanicsRules.clearServerRule();
            Bedrockify.getInstance().settings.bedrockSharpness=false;
            check(net.minecraft.enchantment.Enchantments.SHARPNESS.getAttackDamage(5,net.minecraft.entity.EntityGroup.DEFAULT)==3F,"Disconnect clears remote Sharpness");
        } finally {Bedrockify.getInstance().settings.bedrockSharpness=saved;dev.bedrockify.forge.common.features.mechanics.MechanicsRules.clearServerRule();}
    }
}
