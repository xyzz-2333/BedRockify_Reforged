package dev.bedrockify.forge.tests;

import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.client.features.education.*;
import dev.bedrockify.forge.common.features.education.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/** Opt-in native client/server menu integration. No test code enters release builds. */
@Mod.EventBusSubscriber(modid="bedrockify",value=Dist.CLIENT)
public final class ChemistryUiClientTest {
    private static int ticks;
    private static volatile String failure;
    private static final BlockPos POS=new BlockPos(0,64,0);
    private static void check(boolean ok,String message) { if(!ok)throw new IllegalStateException(message); }
    private static void server(MinecraftClient c,java.util.function.Consumer<net.minecraft.server.network.ServerPlayerEntity> task) {
        var id=c.player.getUuid();c.getServer().execute(()->{try { task.accept(c.getServer().getPlayerManager().getPlayer(id)); }catch(Throwable ex){failure=ex.toString();}});
    }
    private static void open(MinecraftClient c,String kind) {
        server(c,p->{
            var world=p.getServerWorld();var block=EducationContent.BLOCKS.get(kind);var state=block.getDefaultState();
            world.setBlockState(POS,state);
            if(kind.equals("compound_creator")) {
                var s=(ChemistryStationBlockEntity)world.getBlockEntity(POS);s.setStack(0,EducationContent.element(1,4));s.setStack(1,EducationContent.element(8,2));
            }
            if(kind.equals("material_reducer"))((MaterialReducerBlockEntity)world.getBlockEntity(POS)).inventory.setStack(0,new ItemStack(Items.STONE));
            block.onUse(state,world,POS,p,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(POS),Direction.UP,POS,false));
        });
    }
    private static ChemistryStationScreen screen(MinecraftClient c) {
        check(c.currentScreen instanceof ChemistryStationScreen,"Server opens the expected chemistry screen");return (ChemistryStationScreen)c.currentScreen;
    }
    private static ChemistryStationScreenHandler menu(MinecraftClient c) { return (ChemistryStationScreenHandler)c.player.currentScreenHandler; }
    private static void shot(MinecraftClient c,String name) { ScreenshotRecorder.saveScreenshot(c.runDirectory,name+".png",c.getFramebuffer(),message->{}); }
    private static void putAway(MinecraftClient c) {
        var m=c.player.currentScreenHandler;
        for(int i=10;i<m.slots.size();i++)if(!m.getSlot(i).hasStack()) {
            c.interactionManager.clickSlot(m.syncId,i,0,SlotActionType.PICKUP,c.player);return;
        }
        throw new IllegalStateException("No empty player slot for test cursor");
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var c=MinecraftClient.getInstance();
        if(c.currentScreen instanceof AccessibilityOnboardingScreen)c.setScreen(new TitleScreen());
        if(c.player==null||c.world==null)return;
        try {
            if(failure!=null)throw new IllegalStateException(failure);
            ticks++;c.options.pauseOnLostFocus=false;
            if(ticks==1) {
                c.options.getGuiScale().setValue(4);c.options.getViewDistance().setValue(4);c.onResolutionChanged();
                c.getTutorialManager().setStep(net.minecraft.client.tutorial.TutorialStep.NONE);
                server(c,p->{p.changeGameMode(GameMode.CREATIVE);p.getInventory().clear();
                    p.getServerWorld().getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false,c.getServer());
                    p.getServerWorld().getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false,c.getServer());
                    p.getServerWorld().setTimeOfDay(6000);p.getServerWorld().setWeather(6000,0,false,false);
                    for(var pos:BlockPos.iterate(-4,63,-2,4,63,8))p.getServerWorld().setBlockState(pos,net.minecraft.block.Blocks.STONE.getDefaultState());
                    p.teleport(p.getServerWorld(),0,64,6,180,10);
                });
            }
            if(ticks==30)open(c,"element_constructor");
            if(ticks==50) {
                var fields=screen(c).children().stream().filter(w->w instanceof TextFieldWidget).map(w->(TextFieldWidget)w).toList();
                check(fields.size()==3,"All particle fields exist");
                for(var field:fields) {
                    screen(c).mouseClicked(field.getX()+3,field.getY()+5,0);
                    field.setCursorToEnd();screen(c).keyPressed(GLFW.GLFW_KEY_BACKSPACE,0,0);screen(c).charTyped('6',0);
                }
            }
            if(ticks==65) {
                check(menu(c).particle(0)==6 && menu(c).particle(1)==6 && menu(c).particle(2)==6,"Text edits synchronize p/e/n correctly");
                check(menu(c).getSlot(9).getStack().isOf(EducationContent.ELEMENTS[6].asItem()),"Live element preview is carbon");shot(c,"element-constructor");
            }
            if(ticks==70) {
                int left=(screen(c).width-250)/2,top=(screen(c).height-220)/2;
                screen(c).mouseClicked(left+16,top+198,0);screen(c).mouseDragged(left+16,top+70,0,0,-128);screen(c).mouseReleased(left+16,top+70,0);
            }
            if(ticks==80) {
                check(menu(c).particle(0)==118,"Dragging the proton slider reaches the upper bound; actual="+menu(c).particle(0));
                var field=(TextFieldWidget)screen(c).children().stream().filter(w->w instanceof TextFieldWidget).findFirst().orElseThrow();field.setText("6");
            }
            if(ticks==90)c.interactionManager.clickSlot(menu(c).syncId,9,0,SlotActionType.PICKUP,c.player);
            if(ticks==100) { check(menu(c).getCursorStack().isOf(EducationContent.ELEMENTS[6].asItem()),"Native output click claims carbon");putAway(c); }
            if(ticks==110)c.player.closeHandledScreen();
            if(ticks==120)open(c,"compound_creator");
            if(ticks==140) {
                check(menu(c).getSlot(9).getStack().isOf(EducationContent.ITEMS.get("water_compound")),"Network compound preview");
                check(menu(c).getSlot(0).getStack().getCount()==4,"Preview has not consumed hydrogen");shot(c,"compound-creator");
            }
            if(ticks==145)c.interactionManager.clickSlot(menu(c).syncId,9,1,SlotActionType.PICKUP,c.player);
            if(ticks==155) { check(menu(c).getSlot(0).getStack().getCount()==2,"Native pickup consumes one formula");putAway(c); }
            if(ticks==165)c.interactionManager.clickSlot(menu(c).syncId,9,0,SlotActionType.QUICK_MOVE,c.player);
            if(ticks==180) { check(!menu(c).getSlot(0).hasStack()&&!menu(c).getSlot(9).hasStack(),"Shift pickup consumes the remaining formula");c.player.closeHandledScreen(); }
            if(ticks==190)open(c,"lab_table");
            if(ticks==210)shot(c,"lab-table");
            if(ticks==215)server(c,p->{var s=(ChemistryStationBlockEntity)p.getServerWorld().getBlockEntity(POS);s.setStack(0,new ItemStack(EducationContent.ITEMS.get("water_compound"),3));s.setStack(1,new ItemStack(EducationContent.ITEMS.get("sodium_hypochlorite"),3));});
            if(ticks==230) { int left=(screen(c).width-176)/2,top=(screen(c).height-200)/2;screen(c).mouseClicked(left+88,top+99,0); }
            if(ticks==238) { check(menu(c).reaction()==1,"Real server experiment triggers the animation");shot(c,"lab-table-reaction"); }
            if(ticks==250) { check(!menu(c).getSlot(0).hasStack(),"Experiment consumed its inputs");c.player.closeHandledScreen(); }
            if(ticks==260)open(c,"material_reducer");
            if(ticks==280) { check(c.currentScreen instanceof MaterialReducerScreen,"Reducer screen opened through server");check(c.player.currentScreenHandler.getSlot(1).hasStack(),"Reducer preview synchronized");shot(c,"material-reducer"); }
            if(ticks==285)c.interactionManager.clickSlot(c.player.currentScreenHandler.syncId,1,1,SlotActionType.PICKUP,c.player);
            if(ticks==300) { check(((MaterialReducerScreenHandler)c.player.currentScreenHandler).isLocked(),"Claim locks the reducer input");shot(c,"material-reducer-locked");putAway(c); }
            if(ticks==310)c.player.closeHandledScreen();
            if(ticks==325) {
                if(failure!=null)throw new IllegalStateException(failure);
                Bedrockify.LOGGER.info("CHEMISTRY UI CLIENT PASS: four server menus, numeric and slider packets, element/compound pickup, Shift, experiment animation and reducer commit");c.scheduleStop();
            }
            if(ticks>1000)throw new IllegalStateException("Test timed out");
        } catch(Throwable ex) { Bedrockify.LOGGER.error("CHEMISTRY UI CLIENT FAIL at tick "+ticks,ex);c.scheduleStop(); }
    }
}
