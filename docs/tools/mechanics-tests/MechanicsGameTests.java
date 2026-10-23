package dev.bedrockify.forge.tests;
import dev.bedrockify.forge.Bedrockify;
import net.minecraft.test.*;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import dev.bedrockify.forge.common.features.mechanics.BedrockTridentDamage;
@GameTestHolder("bedrockify")
@PrefixGameTestTemplate(false)
public class MechanicsGameTests {
 @GameTest(templateName="empty")
 public static void largePortalBreak(TestContext c) {
  var world=c.getWorld();
  var portal=c.getAbsolutePos(new net.minecraft.util.math.BlockPos(30,70,30));
  int width=21,height=21;
  for(int x=-1;x<=width;x++) for(int y=-1;y<=height;y++) {
   if(x==-1 || x==width || y==-1 || y==height)
    world.setBlockState(portal.add(x,y,0),Blocks.OBSIDIAN.getDefaultState(),Block.NOTIFY_LISTENERS);
  }
  var portalState=Blocks.NETHER_PORTAL.getDefaultState()
   .with(net.minecraft.block.NetherPortalBlock.AXIS,net.minecraft.util.math.Direction.Axis.X);
  for(int x=0;x<width;x++) for(int y=0;y<height;y++)
   world.setBlockState(portal.add(x,y,0),portalState,Block.NOTIFY_LISTENERS);
  long start=System.nanoTime();
  world.setBlockState(portal.add(width/2,height/2,0),Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);
  long millis=(System.nanoTime()-start)/1_000_000;
  System.out.println("BRP_PORTAL_BREAK_21x21_MS="+millis);
  for(int x=0;x<width;x++) for(int y=0;y<height;y++)
   c.assertTrue(world.getBlockState(portal.add(x,y,0)).isAir(),"Broken portal has no surviving blocks");
  c.complete();
 }

 @GameTest(templateName="empty")
 public static void dispenserPowderSnowBreaksPortal(TestContext c) {
  var world=c.getWorld();
  world.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
  var portal=c.getAbsolutePos(new net.minecraft.util.math.BlockPos(10,70,10));
  for(int x=-1;x<=2;x++) for(int y=-1;y<=3;y++) {
   if(x==-1 || x==2 || y==-1 || y==3)
    world.setBlockState(portal.add(x,y,0),Blocks.OBSIDIAN.getDefaultState());
  }
  var portalState=Blocks.NETHER_PORTAL.getDefaultState()
   .with(net.minecraft.block.NetherPortalBlock.AXIS,net.minecraft.util.math.Direction.Axis.X);
  for(int x=0;x<=1;x++) for(int y=0;y<=2;y++)
   world.setBlockState(portal.add(x,y,0),portalState,Block.NOTIFY_LISTENERS);
  var dispenser=portal.north();
  var dispenserState=Blocks.DISPENSER.getDefaultState()
   .with(DispenserBlock.FACING,net.minecraft.util.math.Direction.SOUTH);
  world.setBlockState(dispenser,dispenserState);
  var inventory=(DispenserBlockEntity)world.getBlockEntity(dispenser);
  inventory.setStack(0,new ItemStack(Items.POWDER_SNOW_BUCKET));
  Blocks.DISPENSER.scheduledTick(dispenserState,world,dispenser,world.random);
  c.assertTrue(world.getBlockState(portal).isOf(Blocks.POWDER_SNOW),
   "Dispenser places powder snow into the portal");
  c.assertTrue(world.getBlockState(portal.up()).isAir(),
   "Breaking one portal block collapses the connected portal");
  c.assertTrue(inventory.getStack(0).isOf(Items.BUCKET),
   "Successful dispensing returns the empty bucket");
  c.complete();
 }

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
  var owner=FakePlayerFactory.getMinecraft(c.getWorld());
  var sword=new ItemStack(Items.DIAMOND_SWORD);sword.addEnchantment(Enchantments.LOOTING,3);
  owner.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,sword);
  var mob=c.spawnMob(EntityType.ZOMBIE,2,2,2);mob.setAiDisabled(true);mob.setNoGravity(true);
  var stack=new ItemStack(Items.TRIDENT);stack.setDamage(7);
  var t=new TridentEntity(c.getWorld(),owner,stack);
  t.setPosition(mob.getPos());t.setNoGravity(true);t.setVelocity(Vec3d.ZERO);
  c.assertTrue(EnchantmentHelper.getLooting(owner)==3,"Owner's held Looting is available at kill time");
  t.move(MovementType.PISTON,new Vec3d(0.01,0,0));
  var nbt=new NbtCompound();t.writeCustomDataToNbt(nbt);
  c.assertTrue(nbt.getBoolean("BedrockifyTridentKiller"),"Piston activation persisted");
  t.readCustomDataFromNbt(nbt);t.setOwner(owner);
  t.tick();
  float firstHealth=mob.getHealth();
  c.assertTrue(firstHealth<20 && firstHealth>10,"Piston pulse applies one trident hit");
  t.tick();t.tick();
  c.assertTrue(mob.getHealth()==firstHealth,"Stationary trident does not repeatedly damage");
  var trapdoor=t.getBlockPos().down();
  c.getWorld().setBlockState(trapdoor,Blocks.OAK_TRAPDOOR.getDefaultState());
  t.tick();
  c.assertTrue(mob.getHealth()==firstHealth,"Placing a horizontal trapdoor respects damage immunity");
  mob.timeUntilRegen=0;
  c.getWorld().setBlockState(trapdoor,Blocks.OAK_TRAPDOOR.getDefaultState().with(TrapdoorBlock.OPEN,true));
  t.tick();
  c.assertTrue(mob.getHealth()<firstHealth,"Opening a horizontal trapdoor reactivates the trident");
  float openedHealth=mob.getHealth();
  c.getWorld().setBlockState(trapdoor,Blocks.OAK_TRAPDOOR.getDefaultState().with(TrapdoorBlock.OPEN,false));
  t.tick();
  c.assertTrue(mob.getHealth()==openedHealth,"Closing cannot bypass the target's damage interval");
  var afterHits=new NbtCompound();t.writeCustomDataToNbt(afterHits);
  c.assertTrue(afterHits.getCompound("Trident").getCompound("tag").getInt("Damage")==7,"Killer hits do not consume durability");
  var carpetMob=c.spawnMob(EntityType.ZOMBIE,2,2,2);carpetMob.setAiDisabled(true);carpetMob.setNoGravity(true);
  t.setPosition(carpetMob.getPos());t.tick();
  var carpet=t.getBlockPos().south();
  c.getWorld().setBlockState(carpet,Blocks.WHITE_CARPET.getDefaultState());
  t.tick();
  float carpetHealth=carpetMob.getHealth();
  c.assertTrue(carpetHealth<20,"Carpet state change reactivates trident");
  c.getWorld().setBlockState(carpet,Blocks.AIR.getDefaultState());
  c.getWorld().setBlockState(carpet.west(),Blocks.WHITE_CARPET.getDefaultState());
  t.tick();
  c.assertTrue(carpetMob.getHealth()==carpetHealth,"Moving carpet cannot bypass the damage interval");
  carpetMob.timeUntilRegen=0;
  c.getWorld().setBlockState(carpet.west(),Blocks.AIR.getDefaultState());
  t.tick();
  c.assertTrue(carpetMob.getHealth()<carpetHealth,"Carpet changes can hit again after the damage interval");
  var impaling=new ItemStack(Items.TRIDENT);impaling.addEnchantment(Enchantments.IMPALING,5);
  c.assertTrue(BedrockTridentDamage.withBedrockImpaling(impaling,mob,20)==20,"Dry non-aquatic target receives no Impaling bonus");
  var guardian=c.spawnMob(EntityType.GUARDIAN,5,2,5);guardian.setNoGravity(true);
  c.assertTrue(BedrockTridentDamage.withBedrockImpaling(impaling,guardian,20)==7.5F,"Dry aquatic target loses Java-only Impaling bonus");
  var wet=c.spawnMob(EntityType.ZOMBIE,6,2,6);wet.setAiDisabled(true);wet.setNoGravity(true);
  c.getWorld().setBlockState(wet.getBlockPos(),Blocks.WATER.getDefaultState());wet.tick();
  c.assertTrue(wet.isTouchingWaterOrRain(),"Wet target setup");
  c.assertTrue(BedrockTridentDamage.withBedrockImpaling(impaling,wet,20)==32.5F,"Wet non-aquatic target gains Bedrock Impaling bonus");
  var ground=new TridentEntity(c.getWorld(),owner,new ItemStack(Items.TRIDENT));
  for(int i=0;i<1300;i++)ground.age();
  c.assertFalse(ground.isRemoved(),"Ordinary grounded trident does not despawn");
  var loyal=new ItemStack(Items.TRIDENT);loyal.addEnchantment(Enchantments.LOYALTY,1);
  var l=new TridentEntity(c.getWorld(),owner,loyal);l.setPosition(carpetMob.getPos());
  l.move(MovementType.PISTON,new Vec3d(0.01,0,0));var ln=new NbtCompound();l.writeCustomDataToNbt(ln);
  c.assertFalse(ln.getBoolean("BedrockifyTridentKiller"),"Loyalty excluded");
  c.complete();
 }
}
