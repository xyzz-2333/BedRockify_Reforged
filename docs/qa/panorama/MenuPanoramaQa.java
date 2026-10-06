package bedrockifypanoramaqa;

import com.google.gson.*;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.loadingScreens.MenuPanorama;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Isolated development fixture; never included in a production JAR. */
@Mod("bedrockifypanoramaqa")
public final class MenuPanoramaQa {
    private final Set<String> seen=new HashSet<>();
    private final Map<String,Integer> frames=new LinkedHashMap<>();
    private static final Path COMMAND=Path.of("panorama-qa-command.json"),RESULT=Path.of("panorama-qa-result.json");
    public MenuPanoramaQa() {
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.BackgroundRendered e)->frames.merge(e.getScreen().getClass().getSimpleName(),1,Integer::sum));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e)->{
            if(e.phase!=TickEvent.Phase.END||!Files.exists(COMMAND))return;
            JsonObject cmd=null,result;
            try {
                cmd=JsonParser.parseString(Files.readString(COMMAND)).getAsJsonObject();Files.delete(COMMAND);
                if(!seen.add(cmd.get("request_id").getAsString()))return;
                result=execute(cmd);result.addProperty("ok",true);
            }catch(Throwable ex){result=new JsonObject();result.addProperty("ok",false);result.addProperty("error",ex.toString());ex.printStackTrace();}
            if(cmd!=null)result.add("request_id",cmd.get("request_id"));
            try {Path tmp=RESULT.resolveSibling("panorama-qa-result.tmp");Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().create().toJson(result));Files.move(tmp,RESULT,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception ignored){}
        });
    }
    private JsonObject execute(JsonObject cmd)throws Exception {
        var c=MinecraftClient.getInstance();var mod=BedrockifyClient.getInstance();String action=cmd.get("action").getAsString();
        if(action.equals("select_world"))c.setScreen(new SelectWorldScreen(new TitleScreen()));
        if(action.equals("title"))c.setScreen(new TitleScreen());
        if(action.equals("toggle"))mod.settings.menuPanorama=cmd.get("enabled").getAsBoolean();
        if(action.equals("widget_mode"))mod.settings.loadingScreen=cmd.get("enabled").getAsBoolean();
        if(action.equals("speed"))c.options.getPanoramaSpeed().setValue(cmd.get("value").getAsDouble());
        if(action.equals("resize"))c.currentScreen.resize(c,cmd.get("width").getAsInt(),cmd.get("height").getAsInt());
        if(action.equals("loading_probe"))c.setScreen(new LevelLoadingScreen(new WorldGenerationProgressTracker(5)));
        if(action.equals("terrain_probe"))c.setScreen(new DownloadingTerrainScreen());
        if(action.equals("progress_probe")){var s=new ProgressScreen(true);s.setTitle(Text.translatable("menu.generatingLevel"));s.setTask(Text.literal("QA loading progress"));s.progressStagePercentage(50);c.setScreen(s);}
        if(action.equals("message_probe"))c.setScreen(new MessageScreen(Text.translatable("menu.generatingLevel")));
        if(action.equals("options_probe"))c.setScreen(new OptionsScreen(new TitleScreen(),c.options));
        if(action.equals("language_probe"))c.setScreen(new LanguageOptionsScreen(new OptionsScreen(new TitleScreen(),c.options),c.options,c.getLanguageManager()));
        if(action.equals("video_probe"))c.setScreen(new VideoOptionsScreen(c.currentScreen,c.options));
        if(action.equals("controls_probe"))c.setScreen(new ControlsOptionsScreen(new OptionsScreen(new TitleScreen(),c.options),c.options));
        if(action.equals("keys_probe"))c.setScreen(new KeybindsScreen(new OptionsScreen(new TitleScreen(),c.options),c.options));
        if(action.equals("sound_probe"))c.setScreen(new SoundOptionsScreen(new OptionsScreen(new TitleScreen(),c.options),c.options));
        if(action.equals("chat_probe"))c.setScreen(new ChatOptionsScreen(new OptionsScreen(new TitleScreen(),c.options),c.options));
        if(action.equals("accessibility_probe"))c.setScreen(new AccessibilityOptionsScreen(new OptionsScreen(new TitleScreen(),c.options),c.options));
        if(action.equals("key"))c.currentScreen.keyPressed(cmd.get("code").getAsInt(),0,0);
        if(action.equals("char"))for(char ch:cmd.get("text").getAsString().toCharArray())c.currentScreen.charTyped(ch,0);
        if(action.equals("select_first")||action.equals("load_first")) {
            WorldListWidget list=(WorldListWidget)field(c.currentScreen,SelectWorldScreen.class,"levelList");
            var entry=list.children().get(0);list.setSelected(entry);
            if(action.equals("load_first"))entry.getClass().getMethod("play").invoke(entry);
        }
        if(action.equals("mouse")) {c.currentScreen.mouseClicked(cmd.get("x").getAsDouble(),cmd.get("y").getAsDouble(),0);c.currentScreen.mouseReleased(cmd.get("x").getAsDouble(),cmd.get("y").getAsDouble(),0);}
        return state();
    }
    private static Object field(Object obj,Class<?> type,String name)throws Exception {Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    private JsonObject state()throws Exception {
        var c=MinecraftClient.getInstance();JsonObject out=new JsonObject();out.addProperty("screen",c.currentScreen==null?"none":c.currentScreen.getClass().getSimpleName());out.addProperty("world_ready",c.world!=null&&c.player!=null);out.addProperty("enabled",MenuPanorama.enabled());out.addProperty("widget_enabled",BedrockifyClient.getInstance().settings.loadingScreen);out.add("frames",new Gson().toJsonTree(frames));
        Object renderer=field(null,MenuPanorama.class,"renderer");if(renderer!=null)out.addProperty("rotation",((Number)field(renderer,renderer.getClass(),"pitch")).floatValue());
        if(c.currentScreen!=null) {out.addProperty("width",c.currentScreen.width);out.addProperty("height",c.currentScreen.height);JsonArray widgets=new JsonArray();int i=0;for(var e:c.currentScreen.children())if(e instanceof ClickableWidget w){JsonObject b=new JsonObject();b.addProperty("index",i++);b.addProperty("class",w.getClass().getSimpleName());b.addProperty("message",w.getMessage().getString());b.addProperty("active",w.active);b.addProperty("x",w.getX());b.addProperty("y",w.getY());b.addProperty("width",w.getWidth());b.addProperty("height",w.getHeight());widgets.add(b);}out.add("widgets",widgets);
            JsonArray lists=new JsonArray();for(var e:c.currentScreen.children())if(e instanceof EntryListWidget<?> list){JsonObject b=new JsonObject();b.addProperty("class",list.getClass().getSimpleName());b.addProperty("dirt",(Boolean)field(list,EntryListWidget.class,"renderBackground"));b.addProperty("shadows",(Boolean)field(list,EntryListWidget.class,"renderHorizontalShadows"));lists.add(b);}out.add("lists",lists);}
        if(c.currentScreen instanceof SelectWorldScreen s) {
            WorldListWidget list=(WorldListWidget)field(s,SelectWorldScreen.class,"levelList");out.addProperty("dirt",(Boolean)field(list,EntryListWidget.class,"renderBackground"));out.addProperty("shadows",(Boolean)field(list,EntryListWidget.class,"renderHorizontalShadows"));out.addProperty("world_count",list.children().size());out.addProperty("selected",list.getSelectedOrNull()!=null);
            var search=(net.minecraft.client.gui.widget.TextFieldWidget)field(s,SelectWorldScreen.class,"searchBox");out.addProperty("search",search.getText());
        }
        return out;
    }
}
