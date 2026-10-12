package dev.bedrockify.forge.tests;
import dev.bedrockify.forge.Bedrockify;
import net.minecraft.test.*;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.gametest.*;
@GameTestHolder("bedrockify")
@PrefixGameTestTemplate(false)
public class MechanicsGameTests {
 @GameTest(templateName="empty")
 public static void portalSpawning(TestContext c) {
  var world=c.getWorld();
  world.getServer().setDifficulty(net.minecraft.world.Difficulty.HARD,true);
  world.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_SPAWNING).set(true,world.getServer());
  var pos=c.getAbsolutePos(new net.minecraft.util.math.BlockPos(2,2,2));
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.setBlockState(pos.down().add(x,0,z),net.minecraft.block.Blocks.OBSIDIAN.getDefaultState());
  var state=net.minecraft.block.Blocks.NETHER_PORTAL.getDefaultState();
  world.setBlockState(pos,state);
  var box=new net.minecraft.util.math.Box(pos).expand(3);
  Bedrockify.getInstance().settings.bedrockPortalSpawning=false;
  for(int i=0;i<4000;i++)state.getBlock().onBlockAdded(state,world,pos,net.minecraft.block.Blocks.AIR.getDefaultState(),false);
  c.assertTrue(world.getEntitiesByClass(net.minecraft.entity.mob.ZombifiedPiglinEntity.class,box,Entity::isAlive).isEmpty(),"Disabled ignition spawning");
  Bedrockify.getInstance().settings.bedrockPortalSpawning=true;
  world.random.setSeed(12345);
  for(int i=0;i<20000;i++)state.getBlock().onBlockAdded(state,world,pos,net.minecraft.block.Blocks.AIR.getDefaultState(),false);
  c.assertFalse(world.getEntitiesByClass(net.minecraft.entity.mob.ZombifiedPiglinEntity.class,box,Entity::isAlive).isEmpty(),"Ignition creates adjacent piglins");
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
