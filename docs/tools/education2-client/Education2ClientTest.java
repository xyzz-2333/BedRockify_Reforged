package dev.bedrockify.forge.tests;
import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.common.features.education.*;
import dev.bedrockify.forge.client.features.education.ChemistryStationScreen;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeGroups;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="bedrockify",value=Dist.CLIENT)
public final class Education2ClientTest {
    private static int ticks;
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var client=MinecraftClient.getInstance();
        if(client.currentScreen instanceof AccessibilityOnboardingScreen)client.setScreen(new TitleScreen());
        if(client.player==null || client.world==null)return;
        client.options.pauseOnLostFocus=false;
        if(ticks++==0){
            // Exercise dry/waterlogged floor and wall particles and both TNT flash phases.
            for (String name : new String[]{"underwater_torch", "underwater_wall_torch"}) {
                var block=EducationContent.BLOCKS.get(name);
                for(boolean waterlogged:new boolean[]{false,true})
                    block.randomDisplayTick(block.getDefaultState().with(net.minecraft.state.property.Properties.WATERLOGGED,waterlogged),client.world,client.player.getBlockPos(),client.world.random);
            }
            var renderTnt=new UnderwaterTntEntity(EducationContent.UNDERWATER_TNT_ENTITY,client.world);
            var renderer=client.getEntityRenderDispatcher().getRenderer(renderTnt);
            var buffers=client.getBufferBuilders().getEntityVertexConsumers();
            for(int fuse:new int[]{80,75,4}) {
                renderTnt.setFuse(fuse);
                renderer.render(renderTnt,0,0,new net.minecraft.client.util.math.MatrixStack(),buffers,15728880);
                buffers.draw();
            }
            Bedrockify.LOGGER.info("EDUCATION2 DISPLAY PASS: dry/wet floor/wall particles; TNT flash phases and final swelling rendered");
            check(CreativeGroups.family(new ItemStack(EducationContent.ITEMS.get("element_constructor")))==null,"Element constructor is a workstation, not an element block");
            int items=0;
            for(var entry:EducationContent.ITEMS.entrySet()){
                var model=client.getItemRenderer().getModel(new ItemStack(entry.getValue()),client.world,client.player,0);
                check(model!=client.getBakedModelManager().getMissingModel(),"Missing item model: "+entry.getKey());
                check(!model.getParticleSprite().getContents().getId().equals(MissingSprite.getMissingSpriteId()),"Missing item texture: "+entry.getKey());items++;
            }
            check("hardened_glass".equals(CreativeGroups.family(new ItemStack(EducationContent.ITEMS.get("red_hardened_glass")))),"Glass creative family");
            check("hardened_glass_panes".equals(CreativeGroups.family(new ItemStack(EducationContent.ITEMS.get("blue_hardened_glass_pane")))),"Pane creative family");
            check("compounds".equals(CreativeGroups.family(new ItemStack(EducationContent.ITEMS.get("aluminum_oxide")))),"Compound family");
            Bedrockify.LOGGER.info("EDUCATION2 ASSETS PASS: {} registered item models/textures and new creative groups",items);
            client.getServer().execute(()->{
                var world=client.getServer().getOverworld();var player=client.getServer().getPlayerManager().getPlayer(client.player.getUuid());
                var pos=player.getBlockPos().add(2,0,0);world.setBlockState(pos,EducationContent.BLOCKS.get("red_hardened_glass").getDefaultState());
                var tnt=new UnderwaterTntEntity(EducationContent.UNDERWATER_TNT_ENTITY,world);tnt.setPosition(Vec3d.ofCenter(pos.up()));tnt.setFuse(6000);tnt.setNoGravity(true);world.spawnEntity(tnt);
            });
        }
        String[] stations={"compound_creator","element_constructor","lab_table"};
        for(int i=0;i<3;i++){
            if(ticks==10+i*30){var handler=new ChemistryStationScreenHandler(0,client.player.getInventory(),stations[i]);client.setScreen(new ChemistryStationScreen(handler,client.player.getInventory(),Text.translatable("block.bedrockify."+stations[i])));}
            if(ticks==25+i*30)ScreenshotRecorder.saveScreenshot(client.runDirectory,client.getFramebuffer(),message->{});
        }
        if(ticks==100){client.getServer().execute(()->{
            var world=client.getServer().getOverworld();var player=client.getServer().getPlayerManager().getPlayer(client.player.getUuid());
            var pos=player.getBlockPos().east();world.setBlockState(pos,EducationContent.BLOCKS.get("compound_creator").getDefaultState());
            var station=(ChemistryStationBlockEntity)world.getBlockEntity(pos);station.clear();station.setStack(0,EducationContent.element(1,2));station.setStack(1,EducationContent.element(8,1));
            net.minecraftforge.network.NetworkHooks.openScreen(player,station,buf->buf.writeString(station.kind()));
        });}
        if(ticks==120){check(client.player.currentScreenHandler instanceof ChemistryStationScreenHandler,"Server opens correct client menu");client.interactionManager.clickButton(client.player.currentScreenHandler.syncId,0);}
        if(ticks==140){
            check(client.player.currentScreenHandler.getSlot(9).getStack().isOf(EducationContent.ITEMS.get("water_compound")),"Network combine returns real output");
            check(client.player.currentScreenHandler.getSlot(0).getStack().isEmpty() && client.player.currentScreenHandler.getSlot(1).getStack().isEmpty(),"Network input consumption synchronized");
            ScreenshotRecorder.saveScreenshot(client.runDirectory,client.getFramebuffer(),message->{});
            Bedrockify.LOGGER.info("EDUCATION2 NETWORK PASS: server menu, input sync, combine button and output sync");
        }
        if(ticks==150){
            boolean seen=false;for(var entity:client.world.getEntities())if(entity instanceof UnderwaterTntEntity){seen=true;check(client.getEntityRenderDispatcher().getRenderer(entity)!=null,"TNT renderer");}
            check(seen,"Custom TNT entity synchronized to client");
            Bedrockify.LOGGER.info("EDUCATION2 CLIENT PASS: station screens rendered, custom TNT synchronized, model/texture checks passed");client.scheduleStop();
        }
    }
}
