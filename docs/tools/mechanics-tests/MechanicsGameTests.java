package dev.bedrockify.forge.tests;
import dev.bedrockify.forge.Bedrockify;
import net.minecraft.test.*;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.util.FakePlayerFactory;
@GameTestHolder("bedrockify")
@PrefixGameTestTemplate(false)
public class MechanicsGameTests {
 @GameTest(templateName="empty")
 public static void portalSpawning(TestContext c) {
  var world=c.getWorld();
  world.getServer().setDifficulty(net.minecraft.world.Difficulty.HARD,true);
  world.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_SPAWNING).set(true,world.getServer());
  var anchor=c.getAbsolutePos(new net.minecraft.util.math.BlockPos(2,2,2));
  var pos=new net.minecraft.util.math.BlockPos(anchor.getX(),5,anchor.getZ());
  var state=net.minecraft.block.Blocks.NETHER_PORTAL.getDefaultState().with(net.minecraft.block.NetherPortalBlock.AXIS,net.minecraft.util.math.Direction.Axis.X);
  world.setBlockState(pos,state);
  var box=new net.minecraft.util.math.Box(pos).expand(3);
  Bedrockify.getInstance().settings.bedrockPortalSpawning=false;
  for(int i=0;i<4000;i++)state.getBlock().onBlockAdded(state,world,pos,net.minecraft.block.Blocks.AIR.getDefaultState(),false);
  c.assertTrue(world.getEntitiesByClass(net.minecraft.entity.mob.ZombifiedPiglinEntity.class,box,Entity::isAlive).isEmpty(),"Disabled ignition spawning");
  world.setBlockState(pos.south(),net.minecraft.block.Blocks.OBSIDIAN.getDefaultState());
  Bedrockify.getInstance().settings.bedrockPortalSpawning=true;
  world.random.setSeed(12345);
  for(int i=0;i<20000;i++)state.getBlock().onBlockAdded(state,world,pos,net.minecraft.block.Blocks.AIR.getDefaultState(),false);
  var pigs=world.getEntitiesByClass(net.minecraft.entity.mob.ZombifiedPiglinEntity.class,box,Entity::isAlive);
  c.assertFalse(pigs.isEmpty(),"Ignition creates piglins without free space or support");
  c.assertTrue(pigs.stream().allMatch(p->Math.abs(p.getX()-(pos.getX()+0.5))<0.01 && Math.abs(p.getZ()-(pos.getZ()+1.5))<0.01),"Axis X spawns only south");
  c.assertTrue(pigs.stream().allMatch(p->p.getPortalCooldown()==300),"Portal piglins wait 300 ticks before teleporting");
  pigs.forEach(Entity::discard);
  world.random.setSeed(12345);
  for(int i=0;i<20000;i++)state.getBlock().randomTick(state,world,pos,world.random);
  pigs=world.getEntitiesByClass(net.minecraft.entity.mob.ZombifiedPiglinEntity.class,box,Entity::isAlive);
  c.assertFalse(pigs.isEmpty(),"Random ticks use Bedrock portal spawning");
  pigs.forEach(Entity::discard);
  var eastState=state.with(net.minecraft.block.NetherPortalBlock.AXIS,net.minecraft.util.math.Direction.Axis.Z);
  world.setBlockState(pos,eastState);
  world.random.setSeed(12345);
  for(int i=0;i<20000;i++)eastState.getBlock().onBlockAdded(eastState,world,pos,net.minecraft.block.Blocks.AIR.getDefaultState(),false);
  pigs=world.getEntitiesByClass(net.minecraft.entity.mob.ZombifiedPiglinEntity.class,box,Entity::isAlive);
  c.assertFalse(pigs.isEmpty(),"Axis Z ignition creates piglins");
  c.assertTrue(pigs.stream().allMatch(p->Math.abs(p.getX()-(pos.getX()+1.5))<0.01 && Math.abs(p.getZ()-(pos.getZ()+0.5))<0.01),"Axis Z spawns only east");
  pigs.forEach(Entity::discard);
  var below=new net.minecraft.util.math.BlockPos(pos.getX(),-1,pos.getZ());
  world.setBlockState(below,eastState);
  world.random.setSeed(12345);
  for(int i=0;i<20000;i++)eastState.getBlock().onBlockAdded(eastState,world,below,net.minecraft.block.Blocks.AIR.getDefaultState(),false);
  c.assertTrue(world.getEntitiesByClass(net.minecraft.entity.mob.ZombifiedPiglinEntity.class,new net.minecraft.util.math.Box(below).expand(2),Entity::isAlive).isEmpty(),"No portal piglins below Y=0");
  c.complete();
 }
 @GameTest(templateName="empty")
 public static void pearlPhysics(TestContext c) {
  var p=new EnderPearlEntity(EntityType.ENDER_PEARL,c.getWorld());
  p.setPosition(0,200,0); p.setVelocity(1,0,0); p.tick();
  c.assertTrue(Math.abs(p.getVelocity().x-1)<1e-6,"Pearl air drag");
  c.assertTrue(Math.abs(p.getVelocity().y+0.025)<1e-6,"Pearl gravity");
  Bedrockify.getInstance().settings.bedrockProjectiles=false;
  try { p.setVelocity(1,0,0);p.tick();c.assertTrue(Math.abs(p.getVelocity().x-0.99)<1e-6,"Disabled pearl drag"); }
  finally {Bedrockify.getInstance().settings.bedrockProjectiles=true;}
  var t=new TridentEntity(EntityType.TRIDENT,c.getWorld());
  t.setPosition(0,200,0);t.setVelocity(1,0,0);t.tick();
  c.assertTrue(Math.abs(t.getVelocity().y+0.1)<1e-6,"Trident gravity");
  c.complete();
 }
 @GameTest(templateName="empty")
 public static void playerArrowSpeed(TestContext c) {
  var world=c.getWorld();
  var player=FakePlayerFactory.getMinecraft(world);
  player.setPosition(c.getAbsolutePos(new net.minecraft.util.math.BlockPos(2,5,2)).toCenterPos());
  player.getAbilities().creativeMode=true;
  var box=player.getBoundingBox().expand(4);
  var bow=new ItemStack(Items.BOW);
  Items.BOW.onStoppedUsing(bow,world,player,Items.BOW.getMaxUseTime(bow)-20);
  var arrows=world.getEntitiesByClass(ArrowEntity.class,box,Entity::isAlive);
  c.assertTrue(arrows.size()==1,"Bow spawned one arrow");
  c.assertTrue(Math.abs(arrows.get(0).getVelocity().length()-5)<0.12,"Player bow speed is near 5, actual="+arrows.get(0).getVelocity().length());
  arrows.get(0).discard();
  var crossbow=new ItemStack(Items.CROSSBOW);
  var loaded=new NbtList();
  loaded.add(new ItemStack(Items.ARROW).writeNbt(new NbtCompound()));
  crossbow.getOrCreateNbt().put("ChargedProjectiles",loaded);
  CrossbowItem.setCharged(crossbow,true);
  CrossbowItem.shootAll(world,player,net.minecraft.util.Hand.MAIN_HAND,crossbow,3.15F,1.0F);
  arrows=world.getEntitiesByClass(ArrowEntity.class,box,Entity::isAlive);
  c.assertTrue(arrows.size()==1,"Crossbow spawned one arrow");
  c.assertTrue(Math.abs(arrows.get(0).getVelocity().length()-5)<0.12,"Player crossbow speed is near 5, actual="+arrows.get(0).getVelocity().length());
  arrows.get(0).discard();
  Bedrockify.getInstance().settings.bedrockProjectiles=false;
  try {
   Items.BOW.onStoppedUsing(bow,world,player,Items.BOW.getMaxUseTime(bow)-20);
   arrows=world.getEntitiesByClass(ArrowEntity.class,box,Entity::isAlive);
   c.assertTrue(arrows.size()==1,"Disabled bow spawned one arrow");
   c.assertTrue(Math.abs(arrows.get(0).getVelocity().length()-3)<0.12,"Disabled bow retains Java speed");
   arrows.get(0).discard();
   crossbow=new ItemStack(Items.CROSSBOW);
   loaded=new NbtList();
   loaded.add(new ItemStack(Items.ARROW).writeNbt(new NbtCompound()));
   crossbow.getOrCreateNbt().put("ChargedProjectiles",loaded);
   CrossbowItem.setCharged(crossbow,true);
   CrossbowItem.shootAll(world,player,net.minecraft.util.Hand.MAIN_HAND,crossbow,3.15F,1.0F);
   arrows=world.getEntitiesByClass(ArrowEntity.class,box,Entity::isAlive);
   c.assertTrue(arrows.size()==1,"Disabled crossbow spawned one arrow");
   c.assertTrue(Math.abs(arrows.get(0).getVelocity().length()-3.15)<0.12,"Disabled crossbow retains Java speed");
   arrows.get(0).discard();
  } finally { Bedrockify.getInstance().settings.bedrockProjectiles=true; }
  c.complete();
 }
 @GameTest(templateName="empty")
 public static void tridentKiller(TestContext c) {
  var owner=c.spawnMob(EntityType.COW,0,2,0);
  var mob=c.spawnMob(EntityType.ZOMBIE,2,2,2);mob.setAiDisabled(true);mob.setNoGravity(true);
  var t=new TridentEntity(c.getWorld(),owner,new ItemStack(Items.TRIDENT));
  t.setPosition(mob.getPos());t.setNoGravity(true);t.setVelocity(Vec3d.ZERO);
  t.move(MovementType.PISTON,new Vec3d(0.01,0,0));
  var nbt=new NbtCompound();t.writeCustomDataToNbt(nbt);
  c.assertTrue(nbt.getBoolean("BedrockifyTridentKiller"),"Piston activation persisted");
  t.readCustomDataFromNbt(nbt);
  t.age=1;t.tick();
  c.assertTrue(mob.getHealth()==20,"Odd tick does not apply killer damage");
  for(int i=0;i<3;i++){t.age=2;t.setPosition(mob.getPos());t.setVelocity(Vec3d.ZERO);t.tick();}
  c.assertTrue(mob.getHealth()<=0,"Three 8-damage strikes bypass only killer hurt interval");
  var loyal=new ItemStack(Items.TRIDENT);loyal.addEnchantment(Enchantments.LOYALTY,1);
  var l=new TridentEntity(c.getWorld(),owner,loyal);l.setPosition(mob.getPos());
  l.move(MovementType.PISTON,new Vec3d(0.01,0,0));var ln=new NbtCompound();l.writeCustomDataToNbt(ln);
  c.assertFalse(ln.getBoolean("BedrockifyTridentKiller"),"Loyalty excluded");
  c.complete();
 }
}
