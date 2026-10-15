package dev.bedrockify.forge.tests;
import dev.bedrockify.forge.common.features.education.*;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.explosion.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.util.FakePlayerFactory;
@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public class Education2GameTests {
    private static ChemistryStationBlockEntity station(TestContext c,String kind){
        var pos=c.getAbsolutePos(new BlockPos(1,2,1));c.getWorld().setBlockState(pos,EducationContent.BLOCKS.get(kind).getDefaultState());
        return (ChemistryStationBlockEntity)c.getWorld().getBlockEntity(pos);
    }
    @GameTest(templateName="empty") public static void compoundsAndPersistence(TestContext c){
        var s=station(c,"compound_creator");int recipes=0;
        for(var recipe:c.getWorld().getRecipeManager().listAllOfType(ChemistryRecipe.TYPE)) {
            if(!recipe.station.equals("compound_creator"))continue;s.clear();int i=0;
            for(var entry:recipe.inputs.entrySet())s.setStack(i++,new ItemStack(entry.getKey(),entry.getValue()*2));
            // The largest formula fits two batches below stack size except soap hydrogen.
            if(recipe.inputs.values().stream().anyMatch(n->n*2>64)) {s.clear();i=0;for(var entry:recipe.inputs.entrySet())s.setStack(i++,new ItemStack(entry.getKey(),entry.getValue()));}
            c.assertTrue(s.combine(),"Compound can be made: "+recipe.getId());
            c.assertTrue(ItemStack.areEqual(s.getStack(9),recipe.getOutput(c.getWorld().getRegistryManager())),"Exact output: "+recipe.getId()+" got "+s.getStack(9));
            c.assertTrue(!s.combine(),"Cannot overwrite or duplicate an unclaimed output");recipes++;
        }
        c.assertTrue(recipes==37,"37 compound recipes, got "+recipes);
        s.clear();s.setStack(0,EducationContent.element(1,4));s.setStack(1,EducationContent.element(8,2));
        c.assertTrue(s.combine(),"Water recipe");c.assertTrue(s.getStack(0).getCount()==2 && s.getStack(1).getCount()==1,"Consumes one exact batch");
        var nbt=s.createNbt();var restored=new ChemistryStationBlockEntity(s.getPos(),s.getCachedState());restored.readNbt(nbt);restored.setWorld(c.getWorld());
        c.assertTrue(restored.getStack(9).isOf(EducationContent.ITEMS.get("water_compound")) && !restored.combine(),"Persisted output blocks repeated consumption");
        restored.removeStack(9);c.assertTrue(restored.combine() && restored.getStack(0).isEmpty() && restored.getStack(1).isEmpty(),"Next batch only after output removal");
        c.complete();
    }
    @GameTest(templateName="empty") public static void labsAndMenus(TestContext c){
        var s=station(c,"lab_table");s.setStack(0,new ItemStack(EducationContent.ITEMS.get("water_compound"),3));s.setStack(1,new ItemStack(EducationContent.ITEMS.get("sodium_hypochlorite"),3));
        c.assertTrue(s.combine() && s.getStack(9).isOf(EducationContent.ITEMS.get("bleach")),"Bleach recipe");
        s.removeStack(9);s.setStack(0,new ItemStack(Items.DIRT,5));c.assertTrue(s.combine() && s.getStack(9).isOf(EducationContent.ITEMS.get("garbage")) && s.getStack(0).isEmpty(),"Invalid experiment consumes inputs into garbage");
        var player=FakePlayerFactory.getMinecraft(c.getWorld());player.setPosition(s.getPos().getX(),s.getPos().getY(),s.getPos().getZ());
        var menu=new ChemistryStationScreenHandler(1,player.getInventory(),s,s.kind());
        c.assertTrue(!menu.slots.get(9).canInsert(new ItemStack(Items.DIRT)),"Output rejects insertion");
        menu.quickMove(player,9);c.assertTrue(s.getStack(9).isEmpty(),"Shift click transfers actual output");
        player.setPosition(10000,100,10000);c.assertTrue(!menu.onButtonClick(player,0),"Remote button rejected");c.complete();
    }
    @GameTest(templateName="empty") public static void constructorAndGlass(TestContext c){
        var s=station(c,"element_constructor");s.properties.set(0,8);s.properties.set(1,10);s.properties.set(2,8);
        c.assertTrue(s.combine() && s.getStack(9).isOf(EducationContent.ELEMENTS[8].asItem()),"Constructor chooses atomic number");
        c.assertTrue(s.getStack(9).getNbt().getInt("Neutrons")==10,"Isotope neutron value preserved");s.removeStack(9);s.properties.set(2,7);c.assertTrue(!s.combine(),"Non-neutral atom rejected");
        long count=EducationContent.BLOCKS.keySet().stream().filter(n->n.contains("hardened_glass")).count();c.assertTrue(count==34,"34 glass variants");
        var pos=c.getAbsolutePos(new BlockPos(3,2,3));var pane=EducationContent.BLOCKS.get("red_hardened_glass_pane");c.getWorld().setBlockState(pos,pane.getDefaultState());c.getWorld().setBlockState(pos.east(),pane.getDefaultState());
        c.assertTrue(c.getWorld().getBlockState(pos).get(PaneBlock.EAST),"Pane connects to neighbor");
        c.assertTrue(pane.getBlastResistance()==9F,"Glass blast resistance");c.complete();
    }
    @GameTest(templateName="empty",tickLimit=120) public static void underwaterExplosion(TestContext c){
        var world=c.getWorld();var pos=c.getAbsolutePos(new BlockPos(3,3,3));
        for(var target:BlockPos.iterate(pos.add(-2,-2,-2),pos.add(2,2,2)))world.setBlockState(target,Blocks.WATER.getDefaultState());
        world.setBlockState(pos.east(),Blocks.DIRT.getDefaultState());
        var tnt=new UnderwaterTntEntity(EducationContent.UNDERWATER_TNT_ENTITY,world);tnt.setPosition(Vec3d.ofCenter(pos));tnt.setNoGravity(true);tnt.setFuse(1);world.spawnEntity(tnt);
        c.waitAndRun(3,()->{c.assertTrue(!world.getBlockState(pos.east()).isOf(Blocks.DIRT),"Underwater blast destroys submerged dirt");c.assertTrue(!world.getFluidState(pos).isEmpty(),"Water remains");c.complete();});
    }
    @GameTest(templateName="empty") public static void tntTriggersAndSave(TestContext c){
        var world=c.getWorld();var pos=c.getAbsolutePos(new BlockPos(2,2,2));world.setBlockState(pos.down(),Blocks.REDSTONE_BLOCK.getDefaultState());world.setBlockState(pos,EducationContent.UNDERWATER_TNT.getDefaultState());
        var entities=world.getEntitiesByClass(UnderwaterTntEntity.class,new Box(pos).expand(1),e->true);
        c.assertTrue(entities.size()==1 && !world.getBlockState(pos).isOf(EducationContent.UNDERWATER_TNT),"Redstone primes custom TNT once");
        var tnt=entities.get(0);tnt.setFuse(47);var nbt=new NbtCompound();tnt.saveNbt(nbt);var restored=new UnderwaterTntEntity(EducationContent.UNDERWATER_TNT_ENTITY,world);restored.readNbt(nbt);c.assertTrue(restored.getFuse()==47,"Fuse persists");
        c.complete();
    }
    @GameTest(templateName="empty",tickLimit=100) public static void heatAndFertilizer(TestContext c){
        var world=c.getWorld();var pos=c.getAbsolutePos(new BlockPos(2,3,2));
        world.setBlockState(pos,EducationContent.BLOCKS.get("heat_block").getDefaultState());world.setBlockState(pos.east(),Blocks.ICE.getDefaultState());
        world.setBlockState(pos.west().down(),Blocks.DIRT.getDefaultState());world.setBlockState(pos.west(),Blocks.SNOW.getDefaultState());
        var farm=pos.south(3);world.setBlockState(farm.down(),Blocks.FARMLAND.getDefaultState());world.setBlockState(farm,Blocks.WHEAT.getDefaultState());
        var player=FakePlayerFactory.getMinecraft(world);player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,new ItemStack(EducationContent.ITEMS.get("super_fertilizer"),2));
        EducationContent.ITEMS.get("super_fertilizer").useOnBlock(new ItemUsageContext(player,net.minecraft.util.Hand.MAIN_HAND,new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(farm),Direction.UP,farm,false)));
        c.assertTrue(((CropBlock)Blocks.WHEAT).isMature(world.getBlockState(farm)),"Super fertilizer matures crop");c.assertTrue(player.getMainHandStack().getCount()==1,"Fertilizer consumed once");
        c.waitAndRun(25,()->{c.assertTrue(world.getBlockState(pos.east()).isOf(Blocks.WATER),"Heat melts ice");c.assertTrue(!world.getBlockState(pos.west()).isOf(Blocks.SNOW),"Heat melts snow layer");c.complete();});
    }

    @GameTest(templateName="empty",tickLimit=100) public static void iceBombCollision(TestContext c){
        var world=c.getWorld();var center=c.getAbsolutePos(new BlockPos(3,3,3));
        world.setBlockState(center,Blocks.STONE.getDefaultState());world.setBlockState(center.north(),Blocks.WATER.getDefaultState());
        var bomb=new IceBombEntity(EducationContent.ICE_BOMB_ENTITY,world);bomb.setPosition(center.getX()+0.5,center.getY()+0.5,center.getZ()-2.5);bomb.setNoGravity(true);bomb.setVelocity(0,0,1);world.spawnEntity(bomb);
        c.waitAndRun(8,()->{c.assertTrue(bomb.isRemoved(),"Ice bomb consumed by collision");c.assertTrue(world.getBlockState(center.north()).isOf(Blocks.ICE),"Collision freezes nearby water");c.assertTrue(world.getBlockState(center).isOf(Blocks.STONE),"Ice bomb does not destroy solids");c.complete();});
    }

}
