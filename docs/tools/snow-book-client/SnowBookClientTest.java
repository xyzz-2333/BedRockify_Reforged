package dev.bedrockify.forge.tests;

import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.doublePageBooks.BookSpread;
import dev.bedrockify.forge.client.features.snowyLeaves.SnowyLeaves;
import dev.bedrockify.forge.client.features.snowyLeaves.SnowyLeafAtlasSource;
import dev.bedrockify.forge.client.features.snowyLeaves.SnowyLeafModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.List;

/** Opt-in integrated client tests, native network writes and actual rendered screenshots. */
@Mod.EventBusSubscriber(modid="bedrockify", value=Dist.CLIENT)
public final class SnowBookClientTest {
    private static int ticks;
    private static volatile String failure;
    private static BookEditScreen book;
    private static String editedLeft, editedRight;
    private static final BlockPos LEAF = new BlockPos(0, 66, 0);
    private static int original;
    private static int lastStep;
    private static String clipboard;
    private static int thawTicks;
    private static void check(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
    private static Object field(Object o, String name) throws Exception {
        Field f=BookEditScreen.class.getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
    @SuppressWarnings("unchecked") private static List<String> pages() throws Exception { return (List<String>)field(book,"pages"); }
    private static void shot(MinecraftClient c,String name) {
        ScreenshotRecorder.saveScreenshot(c.runDirectory,name+".png",c.getFramebuffer(),m->{});
    }
    private static ItemStack writable() {
        ItemStack stack=new ItemStack(Items.WRITABLE_BOOK);
        NbtList pages=new NbtList();
        for(String text:new String[]{"Left page: native editing.","Right page: 中文输入。","Third page.","Fourth page."}) pages.add(NbtString.of(text));
        stack.getOrCreateNbt().put("pages",pages);return stack;
    }
    private static void server(MinecraftClient c, java.util.function.Consumer<net.minecraft.server.network.ServerPlayerEntity> action) {
        var id=c.player.getUuid();c.getServer().execute(()->{try{action.accept(c.getServer().getPlayerManager().getPlayer(id));}catch(Throwable e){failure=e.toString();}});
    }
    private static void weather(MinecraftClient c,boolean rain) {
        c.world.setRainGradient(rain?1:0);
        server(c,p->{p.getServerWorld().setWeather(rain?0:6000,rain?6000:0,rain,false);p.getServerWorld().setRainGradient(rain?1:0);});
    }
    private static int color(MinecraftClient c) { return c.getBlockColors().getColor(Blocks.OAK_LEAVES.getDefaultState(),c.world,LEAF,0); }
    private static BakedQuad renderedQuad(MinecraftClient c) {
        var state=Blocks.OAK_LEAVES.getDefaultState();
        var model=c.getBlockRenderManager().getModel(state);
        check(model instanceof SnowyLeafModel,"World leaf model uses the frost-aware wrapper");
        var data=model.getModelData(c.world,LEAF,state,ModelData.EMPTY);
        return model.getQuads(state,Direction.NORTH,Random.create(0),data,RenderLayer.getCutoutMipped()).get(0);
    }
    private static void checkFrostPixels(MinecraftClient c) {
        var atlas=c.getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        Sprite original=atlas.getSprite(new net.minecraft.util.Identifier("minecraft","block/oak_leaves"));
        Sprite frosted=renderedQuad(c).getSprite();
        check(frosted.getContents().getId().equals(SnowyLeafAtlasSource.spriteId("oak",8)),"Actual world quads select a frosted sprite, not just white tint");
        var source=original.getContents().getOriginalImage();var snow=frosted.getContents().getOriginalImage();
        long before=0,after=0;int pixels=0;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++) {
            int a=source.getColor(x,y),b=snow.getColor(x,y);
            check((a>>>24)==(b>>>24),"Frost preserves each transparent hole and alpha value");
            if((a>>>24)==0)continue;
            before+=(a&255)+(a>>8&255)+(a>>16&255);after+=(b&255)+(b>>8&255)+(b>>16&255);pixels++;
        }
        double base=before/(3.0*pixels),bright=after/(3.0*pixels);
        check(bright>base+60 && bright>225,"Frost increases surface pixels to snowy whites: "+base+" -> "+bright);
        check(SnowyLeafAtlasSource.frostPixel(0xff5fc2de,8)==0xff5fc2de,"Yellow jungle-leaf details retain their original RGB");
        check(SnowyLeafAtlasSource.frostPixel(0x00123456,8)==0x00123456,"Transparent RGB/alpha are untouched");
        for (String leaf : SnowyLeafAtlasSource.LEAVES) {
            var nativeImage=atlas.getSprite(new net.minecraft.util.Identifier("minecraft","block/"+leaf+"_leaves")).getContents().getOriginalImage();
            var frostImage=atlas.getSprite(SnowyLeafAtlasSource.spriteId(leaf,8)).getContents().getOriginalImage();
            for(int y=0;y<nativeImage.getHeight();y++)for(int x=0;x<nativeImage.getWidth();x++) {
                int a=nativeImage.getColor(x,y),b=frostImage.getColor(x,y);
                check((a>>>24)==(b>>>24),leaf+" preserves alpha at "+x+","+y);
                int r=a&255,g=a>>8&255,blue=a>>16&255;
                if(leaf.equals("jungle") && r>g && g-blue>32 && r-blue>64)
                    check(a==b,"Real jungle texture keeps its yellow details");
            }
        }
        Bedrockify.LOGGER.info("SNOW TEXTURE PIXELS PASS: opaque RGB mean {} -> {}, alpha preserved",base,bright);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var c=MinecraftClient.getInstance();
        if(c.currentScreen instanceof AccessibilityOnboardingScreen)c.setScreen(new TitleScreen());
        if(c.player==null || c.world==null)return;
        try {
            if(failure!=null)throw new IllegalStateException(failure);
            ticks++;c.options.pauseOnLostFocus=false;
            if(ticks>110 && c.currentScreen!=null)c.setScreen(null);
            if(ticks==1) {
                c.options.getGuiScale().setValue(2);c.options.getViewDistance().setValue(4);c.options.getGraphicsMode().setValue(GraphicsMode.FANCY);c.onResolutionChanged();
                clipboard=c.keyboard.getClipboard();
                check(SnowyLeaves.blend(0x123456,0,8)==0x123456 && SnowyLeaves.blend(0x123456,8,8)==0xffffff,"Tint endpoints");
                var registry=c.world.getRegistryManager().get(RegistryKeys.BIOME);
                check(SnowyLeaves.supportsSnow(registry.get(BiomeKeys.SNOWY_PLAINS)),"Snowy plains climate");
                check(!SnowyLeaves.supportsSnow(registry.get(BiomeKeys.WINDSWEPT_HILLS)),"Altitude snowfall does not frost a warm base biome");
                check(!SnowyLeaves.supported(Blocks.CHERRY_LEAVES.getDefaultState()) && !SnowyLeaves.supported(Blocks.AZALEA_LEAVES.getDefaultState()),"Untinted leaves excluded");
                server(c,p->{
                    p.changeGameMode(GameMode.CREATIVE);p.getServerWorld().getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false,c.getServer());
                    p.getServerWorld().getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,c.getServer());
                    p.getServerWorld().getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false,c.getServer());
                    p.getServerWorld().setTimeOfDay(6000);
                    var commands=c.getServer().getCommandManager();var source=c.getServer().getCommandSource();
                    commands.executeWithPrefix(source,"fill -16 63 -8 16 90 20 minecraft:air");
                    commands.executeWithPrefix(source,"fill -16 63 -8 16 63 20 minecraft:snow_block");
                    commands.executeWithPrefix(source,"fillbiome -16 60 -8 16 80 20 minecraft:snowy_plains");
                    var types=List.of(Blocks.OAK_LEAVES,Blocks.SPRUCE_LEAVES,Blocks.BIRCH_LEAVES,Blocks.JUNGLE_LEAVES,Blocks.ACACIA_LEAVES,Blocks.DARK_OAK_LEAVES,Blocks.MANGROVE_LEAVES,Blocks.CHERRY_LEAVES);
                    for(int i=0;i<types.size();i++) for(int x=0;x<2;x++) for(int y=65;y<=67;y++)
                        p.getServerWorld().setBlockState(new BlockPos((i-4)*3+x,y,0),types.get(i).getDefaultState().with(LeavesBlock.PERSISTENT,true));
                    p.teleport(p.getServerWorld(),0,65,14,180,0);p.getInventory().selectedSlot=0;p.getInventory().setStack(0,writable());p.playerScreenHandler.sendContentUpdates();
                });weather(c,false);
            }
            if(ticks==35) {
                check(c.player.getMainHandStack().isOf(Items.WRITABLE_BOOK),"Writable book synchronized");
                book=new BookEditScreen(c.player,c.player.getMainHandStack(),Hand.MAIN_HAND);c.setScreen(book);
                book.charTyped('L',0);
                book.mouseClicked(BookSpread.textX(book.width,1)+6,BookSpread.textY(book.height)+4,0);
                check((int)field(book,"currentPage")==1,"Right page receives focus");book.charTyped('R',0);
                editedLeft=pages().get(0);editedRight=pages().get(1);
                check(editedLeft.length()=="Left page: native editing.".length()+1 && editedRight.length()=="Right page: 中文输入。".length()+1,"Both native page buffers edited independently");
            }
            if(ticks==40)shot(c,"book-two-pages");
            if(ticks==42) {
                book.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN,0,0);check((int)field(book,"currentPage")==2,"Next spread advances two pages");
                book.keyPressed(GLFW.GLFW_KEY_PAGE_UP,0,0);check((int)field(book,"currentPage")==0,"Previous spread advances two pages");
                SelectionManager selection=(SelectionManager)field(book,"currentPageSelectionManager");
                String before=pages().get(0);selection.insert("x".repeat(2000));check(pages().get(0).equals(before),"Native length and visual height validation retained");
                book.resize(c,320,240);check(!BookSpread.enabled(book.width,book.height),"Small-window native fallback");
                book.resize(c,c.getWindow().getScaledWidth(),c.getWindow().getScaledHeight());
                check(pages().get(0).equals(editedLeft) && pages().get(1).equals(editedRight),"Resize retains both page contents");
                ((ButtonWidget)field(book,"signButton")).onPress();check((boolean)field(book,"signing"),"Native signing mode");
                for(char ch:"Frost Test".toCharArray())book.charTyped(ch,0);
                ((ButtonWidget)field(book,"finalizeButton")).onPress();
            }
            if(ticks==65)server(c,p->{
                var stack=p.getMainHandStack();check(stack.isOf(Items.WRITTEN_BOOK),"Native signing packet accepted by server");
                var saved=stack.getNbt().getList("pages",8);
                check(saved.size()==4 && saved.getString(0).contains(editedLeft) && saved.getString(1).contains(editedRight),"Signed JSON pages retain both edits");
                p.getInventory().setStack(40,writable());p.playerScreenHandler.sendContentUpdates();
            });
            if(ticks==85) {
                book=new BookEditScreen(c.player,c.player.getOffHandStack(),Hand.OFF_HAND);c.setScreen(book);
                book.mouseClicked(BookSpread.textX(book.width,1)+6,BookSpread.textY(book.height)+4,0);book.charTyped('Z',0);
                ((ButtonWidget)field(book,"doneButton")).onPress();
            }
            if(ticks==110) {
                server(c,p->check(p.getOffHandStack().getNbt().getList("pages",8).getString(1).contains("Z"),"Offhand native save packet targets the offhand book"));
                c.keyboard.setClipboard(clipboard);c.setScreen(null);
                check(SnowyLeaves.supportsSnow(c.world.getBiome(LEAF).value()),"Test scene's cold biome synchronized to the client");
                original=color(c);check(original!=0xffffff,"Clear-weather original leaf tint");shot(c,"leaves-clear");
                check(renderedQuad(c).getSprite().getContents().getId().getNamespace().equals("minecraft"),"Clear weather retains original leaf sprite");
                weather(c,true);
                Bedrockify.LOGGER.info("SNOW BOOK: native editing, spread navigation, resize, signing and offhand save PASS");
            }
            if(ticks>150 && ticks<730) {
                int step=SnowyLeaves.currentStep();check(step>=lastStep,"Frost step never goes backwards during snowfall");lastStep=step;
            }
            if(ticks==420) { check(SnowyLeaves.currentStep()>0 && SnowyLeaves.currentStep()<8,"Snowfall has intermediate stages");shot(c,"leaves-transition"); }
            if(ticks==740) {
                var types=List.of(Blocks.OAK_LEAVES,Blocks.SPRUCE_LEAVES,Blocks.BIRCH_LEAVES,Blocks.JUNGLE_LEAVES,Blocks.ACACIA_LEAVES,Blocks.DARK_OAK_LEAVES,Blocks.MANGROVE_LEAVES,Blocks.CHERRY_LEAVES);
                for(int i=0;i<types.size();i++) for(int x=0;x<2;x++) for(int y=65;y<=67;y++)
                    check(c.world.getBlockState(new BlockPos((i-4)*3+x,y,0)).isOf(types.get(i)),"All scene leaves remain present during snowfall");
                checkFrostPixels(c);
                check(SnowyLeaves.currentStep()==8 && color(c)==0xffffff,"Snowfall reaches full frost: step="+SnowyLeaves.currentStep()+", color="+Integer.toHexString(color(c)));shot(c,"leaves-frosted");
                var settings=BedrockifyClient.getInstance().settings;settings.snowyLeaves=false;check(color(c)==original,"Master switch restores original tint");settings.snowyLeaves=true;
                c.options.getGraphicsMode().setValue(GraphicsMode.FAST);check(color(c)==original,"Fast graphics keeps green leaves");c.options.getGraphicsMode().setValue(GraphicsMode.FANCY);
                check(c.getBlockColors().getColor(Blocks.OAK_LEAVES.getDefaultState(),null,null,0)!=0xffffff,"Inventory tint unaffected");
                weather(c,false);
            }
            if(ticks>740 && !c.isPaused() && !c.world.isRaining())thawTicks++;
            if(thawTicks==610) {
                check(SnowyLeaves.currentStep()==0 && color(c)==original,"Leaves thaw back to their original tint: step="+SnowyLeaves.currentStep()+", color="+Integer.toHexString(color(c))+", original="+Integer.toHexString(original));shot(c,"leaves-thawed");
                check(renderedQuad(c).getSprite().getContents().getId().getNamespace().equals("minecraft"),"Thaw restores the original world sprite");
                Bedrockify.LOGGER.info("SNOW BOOK CLIENT PASS: 8-step frost/thaw, climate/species filtering, switches, native two-page editor, signing and offhand server saves");
                c.scheduleStop();
            }
            if(ticks>3000)throw new IllegalStateException("Test timeout: rain="+c.world.isRaining()+", thawTicks="+thawTicks);
        }catch(Throwable e){Bedrockify.LOGGER.error("SNOW BOOK CLIENT FAIL at tick "+ticks,e);c.scheduleStop();}
    }
}
