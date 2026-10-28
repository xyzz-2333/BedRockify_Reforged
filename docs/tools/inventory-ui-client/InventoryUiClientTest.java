package dev.bedrockify.forge.tests;

import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.creativeInventory.*;
import dev.bedrockify.forge.client.features.survivalInventory.*;
import dev.bedrockify.forge.client.features.loadingScreens.MenuPanorama;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.world.GameMode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Field;
import java.util.*;

/** Opt-in rendering and native packet regression in a disposable copied world. */
@Mod.EventBusSubscriber(modid="bedrockify", value=Dist.CLIENT)
public final class InventoryUiClientTest {
    private static int ticks;
    private static volatile String failure;
    private static void check(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
    private static Object field(Object object, String name) throws Exception {
        Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    private static void screenshot(MinecraftClient c, String name) {
        ScreenshotRecorder.saveScreenshot(c.runDirectory, name + ".png", c.getFramebuffer(), message -> {});
    }
    private static void pointer(MinecraftClient c, double x, double y) {
        var window=c.getWindow();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(window.getHandle(),x*window.getWidth()/window.getScaledWidth(),y*window.getHeight()/window.getScaledHeight());
    }
    private static void server(MinecraftClient c, java.util.function.Consumer<net.minecraft.server.network.ServerPlayerEntity> task) {
        UUID id=c.player.getUuid();
        c.getServer().execute(() -> { try { task.accept(c.getServer().getPlayerManager().getPlayer(id)); }
            catch (Throwable e) { failure=e.toString(); } });
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var c=MinecraftClient.getInstance();
        if (c.currentScreen instanceof AccessibilityOnboardingScreen) c.setScreen(new TitleScreen());
        if (c.player==null || c.world==null) return;
        try {
            if (failure!=null) throw new IllegalStateException(failure);
            ticks++; c.options.pauseOnLostFocus=false;
            if(ticks==1) {
                c.options.getGuiScale().setValue(2); c.onResolutionChanged();
                var mod=BedrockifyClient.getInstance();mod.settings.rememberInventoryState=true;mod.settings.rememberInventorySearch=true;
                mod.inventoryUiState.creativeView(0);
                mod.inventoryUiState.remember("creative:split",new dev.bedrockify.forge.client.features.inventoryMemory.InventoryUiState.View("CONSTRUCTION","",0,Set.of()));
                server(c,p -> { p.changeGameMode(GameMode.CREATIVE); p.getInventory().clear(); p.getInventory().setStack(p.getInventory().selectedSlot,new ItemStack(Items.DIAMOND,3)); p.playerScreenHandler.setCursorStack(ItemStack.EMPTY); p.playerScreenHandler.sendContentUpdates(); });
            }
            if(ticks==30) {
                CreativeViewTabs.openFull(c);
                var equipment=CreativeCatalog.contents(CreativeCatalog.Category.EQUIPMENT);
                var groups=CreativeGroups.layout(equipment,Set.of(),true,true);
                check(groups.stream().anyMatch(e -> "cooked_food".equals(e.group()) && e.stack().isOf(Items.COOKED_CHICKEN)),"Cooked chicken represents cooked food");
                check(groups.stream().anyMatch(e -> "other_food".equals(e.group()) && e.count()==9),"Nine other foods collapse together");
                check(groups.stream().anyMatch(e -> "crops".equals(e.group()) && e.count()==10),"Ten crop foods collapse together");
                check(groups.stream().anyMatch(e -> "suspicious_stew".equals(e.group())),"Suspicious stew NBT variants collapse together");
                check(CreativeGroups.family(Items.CHORUS_FRUIT.getDefaultStack())==null,"Chorus fruit stays outside crops");
                var building=CreativeGroups.layout(CreativeCatalog.contents(CreativeCatalog.Category.CONSTRUCTION),Set.of(),true,true);
                check(building.stream().anyMatch(e -> "sandstone".equals(e.group()) && e.count()==8),"Eight sandstone blocks collapse together");
                check(building.stream().anyMatch(e -> "stone_bricks".equals(e.group())),"Stone bricks collapsed");
                var nature=CreativeCatalog.contents(CreativeCatalog.Category.NATURE);
                check(nature.stream().anyMatch(s -> s.isOf(Items.PUFFERFISH)) && nature.stream().anyMatch(s -> s.isOf(Items.TROPICAL_FISH)),"Fish moved to nature");
                check(CreativeGroups.layout(nature,Set.of(),true,false).stream().anyMatch(e -> "mushrooms".equals(e.group()) && e.count()==7),"Mushrooms, fungi and mushroom blocks collapse together");
                Bedrockify.LOGGER.info("INVENTORY UI CATALOGUE PASS");
            }
            if(ticks==40) screenshot(c,"creative-catalogue");
            if(ticks==50) c.setScreen(new BedrockCreativeScreen(c.player,true));
            if(ticks==55) pointer(c,(int)field(c.currentScreen,"catalogX")+18,(c.currentScreen.height-198)/2+38);
            if(ticks==60) screenshot(c,"creative-split");
            if(ticks==70) {
                var screen=(BedrockCreativeScreen)c.currentScreen;
                var search=(TextFieldWidget)field(screen,"search");
                search.setFocused(true);screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_E,0,0);screen.charTyped('e',0);
                check(c.currentScreen==screen && search.getText().equals("e"),"Search typing does not trigger inventory close");
                search.setText("minecraft:oak_planks");
                screen.mouseClicked((int)field(screen,"catalogX")+14,(c.currentScreen.height-198)/2+32,0);
            }
            if(ticks==72) pointer(c,10,10);
            if(ticks==74) screenshot(c,"creative-selected-item");
            if(ticks==75) c.currentScreen.mouseClicked((int)field(c.currentScreen,"catalogX")+14,(c.currentScreen.height-198)/2+32,1);
            if(ticks==80) server(c,p -> check(p.playerScreenHandler.getCursorStack().isOf(Items.OAK_PLANKS)
                    && p.playerScreenHandler.getCursorStack().getCount()==1,"Repeated same-item pick replaces the cursor count"));
            if(ticks==81) c.currentScreen.mouseClicked((int)field(c.currentScreen,"catalogX")+14,(c.currentScreen.height-198)/2+32,0);
            if(ticks==85) server(c,p -> { check(p.playerScreenHandler.getCursorStack().isOf(Items.OAK_PLANKS)
                    && p.playerScreenHandler.getCursorStack().getCount()==64,"Creative picker synchronizes the server cursor");
                check(p.getInventory().getStack(p.getInventory().selectedSlot).isOf(Items.DIAMOND)
                    && p.getInventory().getStack(p.getInventory().selectedSlot).getCount()==3,"Creative picker preserves an occupied staging slot"); });
            if(ticks==90) for(int i=1;i<=4;i++) {
                var slot=c.player.playerScreenHandler.getSlot(i);
                int left=(c.currentScreen.width-172-198)/2+172, top=Math.max(30,(c.currentScreen.height-198)/2);
                c.currentScreen.mouseClicked(left+slot.x+4,top+slot.y+4,1);
                c.currentScreen.mouseReleased(left+slot.x+4,top+slot.y+4,1);
            }
            if(ticks==110) server(c,p -> check(p.playerScreenHandler.getSlot(0).getStack().isOf(Items.CRAFTING_TABLE),"Server computes native 2x2 recipe"));
            if(ticks==115) c.interactionManager.clickSlot(0,9,0,SlotActionType.PICKUP,c.player);
            if(ticks==125) c.interactionManager.clickSlot(0,0,0,SlotActionType.PICKUP,c.player);
            if(ticks==140) {
                server(c,p -> { check(p.playerScreenHandler.getCursorStack().isOf(Items.CRAFTING_TABLE),"Native crafting result pickup");
                    for(int i=1;i<=4;i++) check(p.playerScreenHandler.getSlot(i).getStack().isEmpty(),"Crafting consumes real inputs"); });
                screenshot(c,"creative-split-crafted");
            }
            if(ticks==150) {
                Bedrockify.LOGGER.info("INVENTORY UI CREATIVE NETWORK PASS");
                c.interactionManager.clickSlot(0,10,0,SlotActionType.PICKUP,c.player);
                c.setScreen(new OptionsScreen(c.currentScreen,c.options));
                check(!MenuPanorama.optionsScreen(c.currentScreen),"In-world options must not use menu panorama");
            }
            if(ticks==160) screenshot(c,"options-in-world");
            if(ticks==170) { c.setScreen(null); server(c,p -> { p.changeGameMode(GameMode.SURVIVAL); p.getInventory().clear(); p.unlockRecipes(c.getServer().getRecipeManager().values()); p.playerScreenHandler.sendContentUpdates(); }); }
            if(ticks==200) {
                BedrockifyClient.getInstance().settings.survivalRecipeBookOpen=true;
                c.setScreen(new InventoryScreen(c.player));
            }
            if(ticks==210) {
                var layout=((SurvivalScreen)c.currentScreen).bedrockify$survivalLayout();
                pointer(c,layout.recipeLeft()+20,layout.top()+BedrockRecipePanel.GRID_Y+10);
            }
            if(ticks==220) {
                screenshot(c,"survival-recipes");
                var screen=(SurvivalScreen)c.currentScreen;
                var panel=((SurvivalRecipeBook)screen.bedrockify$recipeBook()).bedrockify$recipePanel();
                var layout=screen.bedrockify$survivalLayout();
                check(panel!=null,"Survival panel initialized");
                int before=panel.page(); panel.scroll(layout.recipeLeft()+20,layout.top()+70,-1);
                check(panel.page()==Math.min(before+1,panel.pages()-1),"Wheel advances one recipe row");
                int sx=layout.recipeLeft()+SurvivalLayout.RECIPE_WIDTH-14, sy=layout.top()+BedrockRecipePanel.GRID_Y+8;
                panel.mouseClicked(sx,sy,0,()->{},()->{});
                c.currentScreen.mouseDragged(sx,sy+150,0,0,150); c.currentScreen.mouseReleased(sx,sy+150,0);
                check(panel.page()==panel.pages()-1,"Scrollbar drag reaches last recipe row");
                Bedrockify.LOGGER.info("INVENTORY UI SURVIVAL SCROLL PASS");
            }
            if(ticks==230) screenshot(c,"survival-scrolled");
            if(ticks==240) { c.setScreen(null); server(c,p -> p.changeGameMode(GameMode.CREATIVE)); }
            if(ticks==250) check(c.player.playerScreenHandler.getSlot(9).x==8 && c.player.playerScreenHandler.getSlot(9).y==84,"Closing survival restores native slot positions");
            if(ticks==260) c.setScreen(new BedrockCreativeScreen(c.player,false));
            if(ticks==270) screenshot(c,"creative-inventory-only");
            if(ticks==275) {
                c.setScreen(new BedrockCreativeScreen(c.player,true));
                var screen=c.currentScreen;int left=(int)field(screen,"catalogX"),top=(screen.height-198)/2;
                screen.mouseClicked(left+30,top-12,0);
                ((TextFieldWidget)field(screen,"search")).setText("");
                screen.mouseClicked(left+18,top+38,0);
                screen.mouseScrolled(left+18,top+60,-1);
                c.setScreen(null);
            }
            if(ticks==280) c.setScreen(new CreativeInventoryScreen(c.player,c.player.networkHandler.getEnabledFeatures(),true));
            if(ticks==282) {
                check(c.currentScreen instanceof BedrockCreativeScreen,"Reopening creative inventory restores split mode");
                check(field(c.currentScreen,"category")==CreativeCatalog.Category.EQUIPMENT,"Category remembered");
                check((int)field(c.currentScreen,"firstRow")==1,"Scroll row remembered");
                check(!((Set<?>)field(c.currentScreen,"expanded")).isEmpty(),"Expanded groups remembered");
                pointer(c,(int)field(c.currentScreen,"catalogX")+18,(c.currentScreen.height-198)/2+38);
                Bedrockify.LOGGER.info("INVENTORY UI MEMORY PASS");
            }
            if(ticks==284) screenshot(c,"creative-memory-restored");
            if(ticks==286) {
                int left=(c.currentScreen.width-172-198)/2+172,top=(c.currentScreen.height-198)/2;
                c.currentScreen.mouseClicked(left+198-81+8,top-18,0);
            }
            if(ticks==290) check(c.currentScreen instanceof CreativeInventoryScreen,"Explicit full view bypasses split restore");
            if(ticks==295) screenshot(c,"creative-toolbar-aligned");
            if(ticks==305) { Bedrockify.LOGGER.info("INVENTORY UI CLIENT PASS"); c.scheduleStop(); }
        } catch(Throwable e) { Bedrockify.LOGGER.error("INVENTORY UI CLIENT FAILED at tick "+ticks,e); c.scheduleStop(); }
    }
}
