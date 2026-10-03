package bedrockifyqa;

import com.google.gson.*;
import me.juancarloscp52.bedrockify.client.BedrockifyClient;
import me.juancarloscp52.bedrockify.client.features.creativeInventory.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.screen.slot.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Separate development-only Forge fixture. Never included in the BedrockIfy release JAR. */
@Mod("bedrockifyqa")
public class CreativeInventoryQa {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "bedrockifyqa");
    private static final RegistryObject<Item> SAMPLE = ITEMS.register("sample", () -> new Item(new Item.Settings()));
    private static final DeferredRegister<ItemGroup> TABS = DeferredRegister.create(RegistryKeys.ITEM_GROUP, "bedrockifyqa");
    private static final List<RegistryObject<ItemGroup>> TAB_LIST = new ArrayList<>();
    private static final Path COMMAND = Path.of("creative-qa-command.json");
    private static final Path RESULT = Path.of("creative-qa-result.json");
    static {
        for (int i = 0; i < 12; i++) {
            int number = i;
            TAB_LIST.add(TABS.register("tab_" + i, () -> ItemGroup.create(ItemGroup.Row.TOP, 0)
                    .displayName(Text.literal("QA Mod " + number)).icon(() -> Items.DIAMOND.getDefaultStack())
                    .withSearchBar().entries((context, entries) -> {
                        entries.add(SAMPLE.get()); entries.add(Items.OAK_PLANKS); entries.add(Items.BIRCH_PLANKS);
                    }).build()));
        }
    }

    public CreativeInventoryQa() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus); TABS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == ItemGroups.BUILDING_BLOCKS) {
                event.add(SAMPLE.get());
                for (int i = 1; i <= 2; i++) {
                    ItemStack stack = Items.DIAMOND.getDefaultStack();
                    stack.getOrCreateNbt().putInt("qa_variant", i);
                    event.add(stack);
                }
            }
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END || MinecraftClient.getInstance().player == null || !Files.exists(COMMAND)) return;
            try {
                JsonObject cmd = JsonParser.parseString(Files.readString(COMMAND)).getAsJsonObject();
                Files.delete(COMMAND);
                JsonObject result = execute(cmd);
                result.addProperty("ok", true);
                Files.writeString(RESULT, new GsonBuilder().setPrettyPrinting().create().toJson(result));
            } catch (Throwable failure) {
                JsonObject result = new JsonObject(); result.addProperty("ok", false); result.addProperty("error", failure.toString());
                try { Files.writeString(RESULT, result.toString()); } catch (Exception ignored) {}
                failure.printStackTrace();
            }
        });
    }

    private static JsonObject execute(JsonObject cmd) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        String action = cmd.get("action").getAsString();
        if (action.equals("config")) {
            client.setScreen(BedrockifyClient.getInstance().settingsGUI.getConfigScreen(client.currentScreen, true));
            JsonObject result = new JsonObject(); result.addProperty("screen", client.currentScreen.getClass().getName()); return result;
        }
        if (action.equals("flat")) {
            BedrockifyClient.getInstance().settings.creativeInventoryGroups = false;
            client.setScreen(null);
            client.setScreen(new CreativeInventoryScreen(client.player, client.player.networkHandler.getEnabledFeatures(), true));
        }
        if (action.equals("open") || action.equals("audit") || action.equals("fallback")) {
            client.setScreen(null);
            BedrockifyClient.getInstance().settings.creativeInventory = !action.equals("fallback");
            client.setScreen(new CreativeInventoryScreen(client.player, client.player.networkHandler.getEnabledFeatures(), true));
        }
        CreativeInventoryScreen screen = (CreativeInventoryScreen) client.currentScreen;
        if (action.equals("tab")) {
            select(screen, Registries.ITEM_GROUP.get(new Identifier(cmd.get("id").getAsString())));
        } else if (action.equals("click")) {
            Method click = CreativeInventoryScreen.class.getDeclaredMethod("onMouseClick", Slot.class, int.class, int.class, SlotActionType.class);
            click.setAccessible(true);
            int slotId = cmd.get("slot").getAsInt();
            Slot slot = slotId < 0 ? null : screen.getScreenHandler().getSlot(slotId);
            click.invoke(screen, slot, slotId, cmd.get("button").getAsInt(), SlotActionType.valueOf(cmd.get("type").getAsString()));
        } else if (action.equals("search")) {
            var box = (net.minecraft.client.gui.widget.TextFieldWidget) field(screen, "searchBox");
            box.setText(cmd.get("text").getAsString());
            Method search = CreativeInventoryScreen.class.getDeclaredMethod("search"); search.setAccessible(true); search.invoke(screen);
        } else if (action.equals("page")) {
            var pages = (List<net.minecraftforge.client.gui.CreativeTabsScreenPage>) field(screen, "pages");
            screen.setCurrentPage(pages.get(cmd.get("index").getAsInt()));
        } else if (action.equals("resize")) {
            screen.resize(client, cmd.get("width").getAsInt(), cmd.get("height").getAsInt());
        }
        JsonObject out = new JsonObject();
        if (action.equals("audit")) {
            JsonArray checks = new JsonArray();
            Set<String> classified = new HashSet<>();
            for (CreativeCatalog.Category category : CreativeCatalog.Category.values()) {
                List<ItemStack> source = CreativeCatalog.contents(category);
                classified.addAll(identities(source));
                var folded = CreativeGroups.layout(source, Set.of(), true, true);
                Set<String> groups = folded.stream().filter(CreativeGroups.Entry::header).map(CreativeGroups.Entry::group).collect(Collectors.toSet());
                var expanded = CreativeGroups.layout(source, groups, true, true);
                List<ItemStack> visible = expanded.stream().filter(e -> !e.header()).map(CreativeGroups.Entry::stack).toList();
                if (visible.size() != source.size() || !identities(source).equals(identities(visible))) throw new AssertionError("lost variants in " + category);
                select(screen, Registries.ITEM_GROUP.get(category.anchor));
                if (screen.getScreenHandler().slots.size() != ((CreativeGrid)screen.getScreenHandler()).bedrockify$pickerSize() + 9) throw new AssertionError("slot geometry");
                checks.add(category + ": " + source.size() + " stacks preserved, " + groups.size() + " groups");
            }
            Set<String> original = new HashSet<>();
            for (var key : List.of(ItemGroups.BUILDING_BLOCKS, ItemGroups.COLORED_BLOCKS, ItemGroups.NATURAL, ItemGroups.FUNCTIONAL,
                    ItemGroups.REDSTONE, ItemGroups.TOOLS, ItemGroups.COMBAT, ItemGroups.FOOD_AND_DRINK, ItemGroups.INGREDIENTS, ItemGroups.SPAWN_EGGS)) {
                original.addAll(identities(new ArrayList<>(Registries.ITEM_GROUP.get(key).getDisplayStacks())));
            }
            if (!classified.equals(original)) throw new AssertionError("four-category coverage differs from Forge source tabs");
            checks.add("All " + original.size() + " source item/NBT identities reachable in four categories");
            var construction = CreativeCatalog.contents(CreativeCatalog.Category.CONSTRUCTION);
            if (construction.stream().noneMatch(s -> s.isOf(SAMPLE.get()))) throw new AssertionError("Forge item addition missing");
            if (construction.stream().filter(s -> s.isOf(Items.DIAMOND) && s.hasNbt()).count() != 2) throw new AssertionError("NBT variants merged");
            var pages = (List<?>)field(screen, "pages");
            if (pages.size() < 2) throw new AssertionError("Forge page missing");
            for (var tab : TAB_LIST) {
                if (net.minecraftforge.common.CreativeModeTabRegistry.getSortedCreativeModeTabs().stream().noneMatch(t -> t == tab.get())) throw new AssertionError("mod tab missing");
                select(screen, tab.get());
                List<ItemStack> source = (List<ItemStack>)field(screen, "bedrockify$source");
                if (source.size() != tab.get().getDisplayStacks().size() || !identities(source).equals(identities(new ArrayList<>(tab.get().getDisplayStacks())))) throw new AssertionError("searchable mod tab lost grouped items");
            }
            checks.add("Forge additions, 2 NBT variants, 12 mod tabs and " + pages.size() + " pages preserved");
            select(screen, Registries.ITEM_GROUP.get(ItemGroups.BUILDING_BLOCKS));
            out.add("checks", checks);
        }
        var handler = screen.getScreenHandler();
        out.addProperty("slots", handler.slots.size());
        out.addProperty("items", handler.itemList.size());
        out.addProperty("classic", (boolean)field(screen, "bedrockify$classic"));
        out.addProperty("source_items", ((List<?>)field(screen, "bedrockify$source")).size());
        out.addProperty("groups", ((List<CreativeGroups.Entry>)field(screen,"bedrockify$entries")).stream().filter(CreativeGroups.Entry::header).count());
        out.addProperty("expanded", field(screen, "bedrockify$expanded").toString());
        out.addProperty("scroll", (float)field(screen, "scrollPosition"));
        out.addProperty("cursor", handler.getCursorStack().toString());
        out.addProperty("hotbar0", client.player.getInventory().getStack(0).toString());
        out.addProperty("hotbar0_nbt", Objects.toString(client.player.getInventory().getStack(0).getNbt()));
        out.addProperty("page_count", ((List<?>)field(screen, "pages")).size());
        out.addProperty("tab", Registries.ITEM_GROUP.getId((ItemGroup)field(screen,"selectedTab")).toString());
        JsonArray tabs = new JsonArray(); for (ItemGroup tab : screen.getCurrentPage().getVisibleTabs()) tabs.add(Registries.ITEM_GROUP.getId(tab).toString()); out.add("tabs", tabs);
        JsonArray first = new JsonArray(); for (int i = 0; i < Math.min(8, handler.itemList.size()); i++) first.add(handler.itemList.get(i).toString()); out.add("first_items", first);
        JsonArray hotbar = new JsonArray(); for (int i = 0; i < 9; i++) hotbar.add(client.player.getInventory().getStack(i).toString()); out.add("hotbar", hotbar);
        if (client.getServer() != null) {
            var players = client.getServer().getPlayerManager().getPlayerList();
            if (!players.isEmpty()) out.addProperty("server_hotbar0", players.get(0).getInventory().getStack(0).toString());
        }
        return out;
    }

    private static Set<String> identities(List<ItemStack> stacks) { return stacks.stream().map(s -> Registries.ITEM.getId(s.getItem()) + ":" + Objects.toString(s.getNbt())).collect(Collectors.toSet()); }
    private static void select(CreativeInventoryScreen screen, ItemGroup tab) throws Exception {
        Method select = CreativeInventoryScreen.class.getDeclaredMethod("setSelectedTab", ItemGroup.class); select.setAccessible(true); select.invoke(screen, tab);
    }
    private static Object field(Object target, String name) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) { try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(target); } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); } }
        throw new NoSuchFieldException(name);
    }
}
