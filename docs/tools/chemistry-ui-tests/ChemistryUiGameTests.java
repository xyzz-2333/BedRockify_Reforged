package dev.bedrockify.forge.tests;

import com.mojang.authlib.GameProfile;
import dev.bedrockify.forge.common.features.education.*;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.test.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public final class ChemistryUiGameTests {
    private static ChemistryStationBlockEntity station(TestContext c,String kind) {
        var pos=c.getAbsolutePos(new BlockPos(1,2,1));
        c.getWorld().setBlockState(pos,EducationContent.BLOCKS.get(kind).getDefaultState());
        return (ChemistryStationBlockEntity)c.getWorld().getBlockEntity(pos);
    }
    private static net.minecraft.server.network.ServerPlayerEntity player(TestContext c,BlockPos pos) {
        var p=FakePlayerFactory.get(c.getWorld(),new GameProfile(UUID.randomUUID(),"ChemistryTest"));
        p.setPosition(pos.getX(),pos.getY(),pos.getZ());return p;
    }
    private static ChemistryStationScreenHandler menu(TestContext c,ChemistryStationBlockEntity station) {
        var p=player(c,station.getPos());
        var menu=new ChemistryStationScreenHandler(1,p.getInventory(),station,station.kind());
        p.currentScreenHandler=menu;return menu;
    }
    @GameTest(templateName="empty") public static void previewPickupAndShift(TestContext c) {
        var s=station(c,"compound_creator");
        s.setStack(0,EducationContent.element(1,4));s.setStack(1,EducationContent.element(8,2));
        var p=player(c,s.getPos());var m=new ChemistryStationScreenHandler(1,p.getInventory(),s,s.kind());p.currentScreenHandler=m;
        c.assertTrue(m.getSlot(9).hasStack() && s.getStack(9).isEmpty(),"Preview is not an owned block output");
        c.assertTrue(s.getStack(0).getCount()==4 && s.getStack(1).getCount()==2,"Preview consumes nothing");
        var saved=s.createNbt();var restored=new ChemistryStationBlockEntity(s.getPos(),s.getCachedState());restored.readNbt(saved);
        c.assertTrue(restored.getStack(9).isEmpty(),"Preview is absent from saved NBT");
        m.onSlotClick(9,1,SlotActionType.PICKUP,p);
        c.assertTrue(m.getCursorStack().isOf(EducationContent.ITEMS.get("water_compound")),"Native right-click claims the result");
        c.assertTrue(s.getStack(0).getCount()==2 && s.getStack(1).getCount()==1,"Claim consumes one batch");
        m.setCursorStack(ItemStack.EMPTY);m.sendContentUpdates();
        m.onSlotClick(9,0,SlotActionType.QUICK_MOVE,p);m.sendContentUpdates();
        c.assertTrue(p.getInventory().count(EducationContent.ITEMS.get("water_compound"))==1,"Shift claim transfers one result");
        c.assertTrue(s.getStack(0).isEmpty() && s.getStack(1).isEmpty() && !m.getSlot(9).hasStack(),"Inputs exhausted; no ghost output");
        m.onSlotClick(9,0,SlotActionType.PICKUP,p);
        c.assertTrue(m.getCursorStack().isEmpty(),"Empty recipe cannot duplicate output");c.complete();
    }
    @GameTest(templateName="empty") public static void fullInventoryAndLegacyOutput(TestContext c) {
        var s=station(c,"compound_creator");s.setStack(0,EducationContent.element(1,4));s.setStack(1,EducationContent.element(8,2));
        var p=player(c,s.getPos());for(int i=0;i<36;i++)p.getInventory().setStack(i,new ItemStack(Items.STONE,64));
        var m=new ChemistryStationScreenHandler(1,p.getInventory(),s,s.kind());p.currentScreenHandler=m;
        m.onSlotClick(9,0,SlotActionType.QUICK_MOVE,p);
        c.assertTrue(s.getStack(0).getCount()==4 && s.getStack(1).getCount()==2,"Full inventory does not consume a preview");
        c.assertTrue(s.combine(),"Create an old-format committed output");m.sendContentUpdates();
        p.getInventory().setStack(0,ItemStack.EMPTY);m.onSlotClick(9,0,SlotActionType.QUICK_MOVE,p);
        c.assertTrue(s.getStack(9).isEmpty() && s.getStack(0).getCount()==2,"Legacy output is claimed without consuming another batch");c.complete();
    }
    @GameTest(templateName="empty") public static void constructorBoundsAndSingleBatch(TestContext c) {
        var s=station(c,"element_constructor");var p=player(c,s.getPos());
        var m=new ChemistryStationScreenHandler(1,p.getInventory(),s,s.kind());p.currentScreenHandler=m;
        c.assertTrue(m.onButtonClick(p,ChemistryLayout.particleButton(0,8)),"Absolute proton input accepted");
        m.onButtonClick(p,ChemistryLayout.particleButton(2,8));m.onButtonClick(p,ChemistryLayout.particleButton(1,10));
        c.assertTrue(!m.onButtonClick(p,ChemistryLayout.particleButton(0,119)) && !m.onButtonClick(p,Integer.MAX_VALUE),"Invalid parameter packet rejected");
        m.onSlotClick(9,0,SlotActionType.QUICK_MOVE,p);
        c.assertTrue(p.getInventory().count(EducationContent.ELEMENTS[8].asItem())==1,"Infinite constructor preview gives one batch per Shift-click");
        c.assertTrue(s.getStack(9).isEmpty(),"No stored copy of the claimed element");
        p.setPosition(10000,100,10000);
        c.assertTrue(!m.onButtonClick(p,ChemistryLayout.particleButton(0,6)),"Remote parameter updates rejected");c.complete();
    }
    @GameTest(templateName="empty") public static void experimentAndAccess(TestContext c) {
        var s=station(c,"lab_table");var p=player(c,s.getPos());
        var m=new ChemistryStationScreenHandler(1,p.getInventory(),s,s.kind());p.currentScreenHandler=m;s.access.opened(p,m);
        c.assertTrue(s.access.inUse(),"One active menu occupies the station");
        var other=player(c,s.getPos());s.access.closed(other,m);
        c.assertTrue(s.access.inUse(),"Another user cannot release the owner");
        var hit=new net.minecraft.util.hit.BlockHitResult(net.minecraft.util.math.Vec3d.ofCenter(s.getPos()),net.minecraft.util.math.Direction.UP,s.getPos(),false);
        s.getCachedState().getBlock().onUse(s.getCachedState(),c.getWorld(),s.getPos(),other,net.minecraft.util.Hand.MAIN_HAND,hit);
        c.assertTrue(other.currentScreenHandler==other.playerScreenHandler && s.access.inUse(),"Busy workstation refuses a second menu");
        var blocked=new ChemistryStationScreenHandler(2,other.getInventory(),s,s.kind());
        c.assertTrue(!blocked.canUse(other),"The occupied station rejects a second handler");
        c.assertTrue(!m.onButtonClick(p,0),"Empty experiment rejected");
        s.setStack(0,new ItemStack(EducationContent.ITEMS.get("water_compound"),3));s.setStack(1,new ItemStack(EducationContent.ITEMS.get("sodium_hypochlorite"),3));
        c.assertTrue(m.onButtonClick(p,0) && m.reaction()==1,"A real experiment triggers feedback");
        c.assertTrue(s.getStack(9).isEmpty(),"Experiment output is delivered above the table");
        var drops=c.getWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class,new net.minecraft.util.math.Box(s.getPos()).expand(2),e->e.getStack().isOf(EducationContent.ITEMS.get("bleach")));
        c.assertTrue(drops.size()==1 && drops.get(0).getStack().getCount()==1,"Exactly one bleach result spawned");
        m.onClosed(p);c.assertTrue(!s.access.inUse(),"Closing releases occupation");
        p.currentScreenHandler=m;s.access.opened(p,m);p.currentScreenHandler=p.playerScreenHandler;
        c.assertTrue(!s.access.inUse(),"Stale menu releases occupation");c.complete();
    }
}
