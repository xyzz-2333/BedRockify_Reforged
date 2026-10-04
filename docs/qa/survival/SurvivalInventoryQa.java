package bedrockifysurvivalqa;

import com.google.gson.*;
import me.juancarloscp52.bedrockify.client.BedrockifyClient;
import me.juancarloscp52.bedrockify.client.features.survivalInventory.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Development fixture only; no fixture classes enter the release JAR. */
@Mod("bedrockifysurvivalqa")
public final class SurvivalInventoryQa {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "bedrockifysurvivalqa");
    private static final Path COMMAND = Path.of("survival-qa-command.json"), RESULT = Path.of("survival-qa-result.json");
    static {
        for (int i = 0; i < 100; i++) {
            for (String type : List.of("log", "planks", "stairs", "slab", "door", "fence"))
                ITEMS.register("wood_" + i + "_" + type, () -> new Item(new Item.Settings()));
            for (String type : List.of("ingot", "nugget", "block"))
                ITEMS.register("metal_" + i + "_" + type, () -> new Item(new Item.Settings()));
        }
    }
    public SurvivalInventoryQa() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END || MinecraftClient.getInstance().player == null || !Files.exists(COMMAND)) return;
            JsonObject result; JsonObject cmd = null;
            try {
                cmd = new JsonParser().parse(Files.readString(COMMAND)).getAsJsonObject(); Files.delete(COMMAND);
                result = execute(cmd); result.addProperty("ok", true);
            } catch (Throwable e) { result = new JsonObject(); result.addProperty("ok", false); result.addProperty("error", e.toString()); e.printStackTrace(); }
            if (cmd != null && cmd.has("request_id")) result.add("request_id", cmd.get("request_id"));
            try { Path tmp = RESULT.resolveSibling("survival-qa-result.tmp"); Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(result)); Files.move(tmp, RESULT, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); } catch (Exception ignored) {}
        });
    }
    private static JsonObject execute(JsonObject cmd) throws Exception {
        MinecraftClient c = MinecraftClient.getInstance(); String action = cmd.get("action").getAsString();
        if (action.equals("command")) {
            c.player.networkHandler.sendChatCommand(cmd.get("text").getAsString()); return state();
        }
        if (action.equals("setup")) {
            c.getServer().execute(() -> {
                var player = c.getServer().getPlayerManager().getPlayer(c.player.getUuid());
                player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
                player.unlockRecipes(c.getServer().getRecipeManager().values());
                var inventory = player.getInventory(); inventory.clear();
                inventory.setStack(0, new ItemStack(Items.OAK_LOG, 64));
                inventory.setStack(1, new ItemStack(Items.OAK_PLANKS, 64));
                inventory.setStack(2, new ItemStack(Items.IRON_INGOT, 64));
                inventory.setStack(3, new ItemStack(Items.DIAMOND, 64));
                inventory.setStack(4, new ItemStack(Registries.ITEM.get(new Identifier("bedrockifysurvivalqa", "wood_0_log")), 64));
                inventory.setStack(5, new ItemStack(Registries.ITEM.get(new Identifier("bedrockifysurvivalqa", "wood_0_planks")), 64));
                inventory.setStack(6, new ItemStack(Registries.ITEM.get(new Identifier("bedrockifysurvivalqa", "metal_0_ingot")), 64));
                inventory.setStack(7, new ItemStack(Items.LEATHER_CHESTPLATE));
                inventory.setStack(8, new ItemStack(Items.SHIELD));
                player.playerScreenHandler.sendContentUpdates();
            }); return state();
        }
        if (action.equals("inventory")) { c.player.closeHandledScreen(); c.setScreen(new InventoryScreen(c.player)); }
        if (action.equals("crafting")) {
            c.player.closeHandledScreen(); c.setScreen(null);
            c.getServer().execute(() -> {
                var player = c.getServer().getPlayerManager().getPlayer(c.player.getUuid());
                var pos = player.getBlockPos().down(); var world = player.getServerWorld();
                world.setBlockState(pos, Blocks.CRAFTING_TABLE.getDefaultState());
                player.openHandledScreen(Blocks.CRAFTING_TABLE.getDefaultState().createScreenHandlerFactory(world, pos));
            }); return state();
        }
        if (action.equals("close")) { c.player.closeHandledScreen(); c.setScreen(null); return state(); }
        if (action.equals("toggle_setting")) {
            BedrockifyClient.getInstance().settings.survivalInventory = cmd.get("enabled").getAsBoolean(); return state();
        }
        if (action.equals("creative")) { c.player.closeHandledScreen(); c.setScreen(new CreativeInventoryScreen(c.player,c.player.networkHandler.getEnabledFeatures(),true)); return state(); }
        if (action.equals("jei_recipes")) {
            Object runtime=Class.forName("mezz.jei.common.Internal").getMethod("getJeiRuntime").invoke(null);
            Object gui=Class.forName("mezz.jei.api.runtime.IJeiRuntime").getMethod("getRecipesGui").invoke(runtime);
            // Open the real JEI recipes screen without a compile-time dependency.
            var recipeType=Class.forName("mezz.jei.api.recipe.RecipeType");
            Object crafting=recipeType.getMethod("create",String.class,String.class,Class.class).invoke(null,"minecraft","crafting",net.minecraft.recipe.CraftingRecipe.class);
            Class.forName("mezz.jei.api.runtime.IRecipesGui").getMethod("showTypes",List.class).invoke(gui,List.of(crafting));
            return state();
        }
        if (action.equals("return_screen")) { c.currentScreen.close(); return state(); }
        if (!(c.currentScreen instanceof HandledScreen<?> screen) || !(screen instanceof SurvivalScreen access)) return state();
        var panel = ((SurvivalRecipeBook)access.bedrockify$recipeBook()).bedrockify$recipePanel();
        if (action.equals("user_toggle_book")) {
            var layout=access.bedrockify$survivalLayout();
            screen.mouseClicked(layout.left(access.bedrockify$recipeBook().isOpen())+SurvivalLayout.WIDTH-20,layout.top()+17,0);
        }
        if (action.equals("remove_recipe_controls")) {
            Method remove=Screen.class.getDeclaredMethod("remove",Element.class);remove.setAccessible(true);
            for(var child:List.copyOf(screen.children()))
                if(child instanceof RecipeBookToggle || child instanceof net.minecraft.client.gui.widget.TexturedButtonWidget b && b.getWidth()==20 && b.getHeight()==18)
                    remove.invoke(screen,child);
        }
        if (action.equals("transient_close")) { if(access.bedrockify$recipeBook().isOpen())access.bedrockify$recipeBook().toggleOpen(); }
        if (action.equals("redisplay") || action.equals("reuse_without_init") || action.equals("disable_redisplay")) {
            if(cmd.has("close_book")&&cmd.get("close_book").getAsBoolean()&&access.bedrockify$recipeBook().isOpen())access.bedrockify$recipeBook().toggleOpen();
            c.setScreen(new Screen(Text.literal("QA temporary screen")){});
            if(action.equals("disable_redisplay"))BedrockifyClient.getInstance().settings.survivalInventory=false;
            if(action.equals("reuse_without_init"))c.currentScreen=screen;else c.setScreen(screen);
            return state();
        }
        if (action.equals("resize")) screen.resize(c, cmd.get("width").getAsInt(), cmd.get("height").getAsInt());
        if (action.equals("search")) {
            TextFieldWidget box = (TextFieldWidget)field(access.bedrockify$recipeBook(), "searchField");
            box.setText(cmd.get("text").getAsString()); invoke(access.bedrockify$recipeBook(), "refreshSearchResults");
        }
        if (action.equals("category")) panel.category(cmd.get("index").getAsInt());
        if (action.equals("expand")) panel.expandAll(cmd.get("enabled").getAsBoolean());
        if (action.equals("toggle_book")) access.bedrockify$recipeBook().toggleOpen();
        if (action.equals("filter")) {
            var l=access.bedrockify$survivalLayout(); screen.mouseClicked(l.recipeLeft()+169,l.top()+50,0);
        }
        if (action.equals("recipe")) {
            String id = cmd.get("id").getAsString(); int index=-1;
            for(int i=0;i<panel.entries().size();i++) if(!panel.entries().get(i).header() && panel.entries().get(i).node().recipe().getId().toString().equals(id)){index=i;break;}
            if(index<0) throw new AssertionError("missing recipe "+id);
            var l=access.bedrockify$survivalLayout();
            while(panel.page()!=index/48) panel.scroll(l.recipeLeft()+20,l.top()+80,panel.page()<index/48?-1:1);
            screen.mouseClicked(l.recipeLeft()+18+index%8*20,l.top()+78+(index%48)/8*20,0);
        }
        if (action.equals("slot")) {
            int id=cmd.get("id").getAsInt(),button=cmd.get("button").getAsInt();
            SlotActionType type=SlotActionType.valueOf(cmd.get("type").getAsString());
            Method method=HandledScreen.class.getDeclaredMethod("onMouseClick", net.minecraft.screen.slot.Slot.class,int.class,int.class,SlotActionType.class);
            method.setAccessible(true); method.invoke(screen,id<0?null:screen.getScreenHandler().getSlot(id),id,button,type);
        }
        if (action.equals("mouse")) screen.mouseClicked(cmd.get("x").getAsDouble(),cmd.get("y").getAsDouble(),cmd.get("button").getAsInt());
        if (action.equals("key")) screen.keyPressed(cmd.get("code").getAsInt(),0,0);
        if (action.equals("char")) for(char ch:cmd.get("text").getAsString().toCharArray()) screen.charTyped(ch,0);
        JsonObject result=state();
        if (action.equals("jei_area")) {
            Class<?> cls=Class.forName("mezz.jei.library.plugins.vanilla.gui.RecipeBookGuiHandler");
            var areas=(List<?>)cls.getMethod("getGuiExtraAreas",HandledScreen.class).invoke(cls.getConstructor().newInstance(),screen);
            JsonArray bounds=new JsonArray();for(var area:areas){var rect=(net.minecraft.client.util.math.Rect2i)area;JsonObject b=new JsonObject();b.addProperty("x",rect.getX());b.addProperty("y",rect.getY());b.addProperty("width",rect.getWidth());b.addProperty("height",rect.getHeight());bounds.add(b);}result.add("jei_areas",bounds);
        }
        if (action.equals("verify_variants")) {
            Set<Integer> variants = new HashSet<>(); Set<Identifier> ids = new HashSet<>();
            for (var entry : panel.entries()) {
                var node = entry.node(); String path = node.recipe().getId().getPath();
                if (entry.header() || !path.startsWith("stress_")) continue;
                int expected = Integer.parseInt(path.substring(7));
                if (!node.output().hasNbt() || node.output().getNbt().getInt("qa_variant") != expected)
                    throw new AssertionError("wrong output NBT for " + node.recipe().getId());
                variants.add(expected); ids.add(node.recipe().getId());
            }
            int count = cmd.get("count").getAsInt();
            if (variants.size() != count || ids.size() != count) throw new AssertionError("variant/recipe identity loss");
            result.addProperty("verified_variants", variants.size());
        }
        if (action.equals("stress_filters")) {
            System.gc();long before=used();long passes=panel.matchingPasses;long start=System.nanoTime();
            int cycles=cmd.get("cycles").getAsInt();
            for(int i=0;i<cycles;i++){panel.category(i%5);panel.expandAll(i%2==0);}
            panel.category(0);panel.expandAll(false);System.gc();
            if(panel.matchingPasses!=passes) throw new AssertionError("filtering reran ingredient matching");
            result.addProperty("cycles",cycles);result.addProperty("elapsed_ms",(System.nanoTime()-start)/1e6);
            result.addProperty("heap_before",before);result.addProperty("heap_after",used());result.addProperty("matching_passes_added",panel.matchingPasses-passes);
        }
        if (action.equals("stress_open")) {
            c.setScreen(null);System.gc();long before=used();int cycles=cmd.get("cycles").getAsInt();List<Double> times=new ArrayList<>();
            var handler=c.player.playerScreenHandler;List<net.minecraft.screen.slot.Slot> slots=List.copyOf(handler.slots);
            int[][] xy=new int[slots.size()][2];for(int i=0;i<slots.size();i++){xy[i][0]=slots.get(i).x;xy[i][1]=slots.get(i).y;}
            for(int i=0;i<cycles;i++){
                long start=System.nanoTime();c.setScreen(new InventoryScreen(c.player));times.add((System.nanoTime()-start)/1e6);c.setScreen(null);
                for(int j=0;j<slots.size();j++)if(handler.getSlot(j)!=slots.get(j)||slots.get(j).x!=xy[j][0]||slots.get(j).y!=xy[j][1])throw new AssertionError("slot identity/position changed after closing");
            }
            System.gc();Collections.sort(times);result.addProperty("cycles",cycles);result.addProperty("median_ms",times.get(times.size()/2));result.addProperty("max_ms",Collections.max(times));
            result.addProperty("heap_before",before);result.addProperty("heap_after",used());result.addProperty("max_heap",Runtime.getRuntime().maxMemory());c.setScreen(new InventoryScreen(c.player));
        }
        if (action.equals("bench_match")) {
            List<Double> times=new ArrayList<>();long before=panel.matchingPasses;
            for(int i=0;i<20;i++){long start=System.nanoTime();access.bedrockify$recipeBook().reset();times.add((System.nanoTime()-start)/1e6);}
            Collections.sort(times);result.addProperty("median_ms",times.get(10));result.addProperty("max_ms",Collections.max(times));result.addProperty("matching_passes_added",panel.matchingPasses-before);
        }
        return result;
    }
    private static long used(){return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();}
    private static Object field(Object obj,String name)throws Exception{for(Class<?> c=obj.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(obj);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void invoke(Object obj,String name)throws Exception{Method m=obj.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(obj);}
    private static JsonObject state()throws Exception{
        MinecraftClient c=MinecraftClient.getInstance();JsonObject out=new JsonObject();
        out.addProperty("screen",c.currentScreen==null?"none":c.currentScreen.getClass().getName());
        out.addProperty("max_heap",Runtime.getRuntime().maxMemory());
        out.addProperty("recipe_open_preference",BedrockifyClient.getInstance().settings.survivalRecipeBookOpen);
        if(c.player!=null){out.addProperty("cursor",c.player.currentScreenHandler.getCursorStack().toString());JsonArray hotbar=new JsonArray();for(int i=0;i<9;i++)hotbar.add(c.player.getInventory().getStack(i).toString());out.add("hotbar",hotbar);}
        if(c.currentScreen instanceof HandledScreen<?> screen){
            out.addProperty("x",(int)field(screen,"x"));out.addProperty("y",(int)field(screen,"y"));out.addProperty("width",screen.width);out.addProperty("height",screen.height);
            out.addProperty("background_width",(int)field(screen,"backgroundWidth"));out.addProperty("background_height",(int)field(screen,"backgroundHeight"));
            var handler=screen.getScreenHandler();out.addProperty("slot_count",handler.slots.size());out.addProperty("sync_id",handler.syncId);
            JsonArray buttons=new JsonArray();for(var element:screen.children())if(element instanceof net.minecraft.client.gui.widget.ClickableWidget widget){JsonObject b=new JsonObject();b.addProperty("class",widget.getClass().getName());b.addProperty("x",widget.getX());b.addProperty("y",widget.getY());b.addProperty("width",widget.getWidth());b.addProperty("height",widget.getHeight());b.addProperty("message",widget.getMessage().getString());buttons.add(b);}out.add("buttons",buttons);
            JsonArray slots=new JsonArray();for(var slot:handler.slots){JsonObject s=new JsonObject();s.addProperty("id",slot.id);s.addProperty("x",slot.x);s.addProperty("y",slot.y);s.addProperty("item",slot.getStack().toString());if(slot.hasStack()&&slot.getStack().hasNbt())s.addProperty("nbt",slot.getStack().getNbt().toString());slots.add(s);}out.add("slots",slots);
            if(screen instanceof SurvivalScreen access){out.addProperty("classic",access.bedrockify$survivalLayout().active());out.addProperty("book_open",access.bedrockify$recipeBook().isOpen());
                out.addProperty("recovery_toggle",access.bedrockify$recipeToggle()!=null);
                var panel=((SurvivalRecipeBook)access.bedrockify$recipeBook()).bedrockify$recipePanel();if(panel!=null){out.addProperty("recipe_count",panel.recipeCount());out.addProperty("filtered_count",panel.filteredCount());out.addProperty("entry_count",panel.entries().size());out.addProperty("matching_passes",panel.matchingPasses);out.addProperty("rebuilds",panel.rebuildCount);out.addProperty("painted_cells",panel.paintedCells);out.addProperty("page",panel.page());out.addProperty("pages",panel.pages());out.addProperty("craftable_only",panel.craftableOnly());
                    JsonArray groups=new JsonArray();for(var e:panel.entries())if(e.header()){JsonObject g=new JsonObject();g.addProperty("family",e.family());g.addProperty("count",e.count());groups.add(g);}out.add("groups",groups);
                }}
        }
        return out;
    }
}
