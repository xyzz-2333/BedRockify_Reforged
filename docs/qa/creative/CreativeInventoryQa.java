package bedrockifyqa;

import com.google.gson.*;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.creativeInventory.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.inventory.SimpleInventory;
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
    private final Set<String> seenRequests = new HashSet<>();
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
                JsonObject cmd = new JsonParser().parse(Files.readString(COMMAND)).getAsJsonObject();
                Files.delete(COMMAND);
                if(cmd.has("request_id") && !seenRequests.add(cmd.get("request_id").getAsString()))return;
                JsonObject result = execute(cmd);
                result.addProperty("ok", true);
                if(cmd.has("request_id"))result.add("request_id",cmd.get("request_id"));
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
        if(action.equals("close")){client.setScreen(null);JsonObject out=new JsonObject();out.addProperty("screen","none");return out;}
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
        if (action.equals("scroll")) {
            screen.mouseScrolled((int)field(screen,"x")+20,(int)field(screen,"y")+50,cmd.get("amount").getAsDouble());
        } else if (action.equals("tab")) {
            select(screen, Registries.ITEM_GROUP.get(new Identifier(cmd.get("id").getAsString())));
        } else if (action.equals("click")) {
            Method click = CreativeInventoryScreen.class.getDeclaredMethod("onMouseClick", Slot.class, int.class, int.class, SlotActionType.class);
            click.setAccessible(true);
            int slotId = cmd.get("slot").getAsInt();
            Slot slot = slotId < 0 ? null : screen.getScreenHandler().getSlot(slotId);
            click.invoke(screen, slot, slotId, cmd.get("button").getAsInt(), SlotActionType.valueOf(cmd.get("type").getAsString()));
        } else if (action.equals("char")) {
            for(char ch:cmd.get("text").getAsString().toCharArray())screen.charTyped(ch,0);
        } else if (action.equals("key")) {
            screen.keyPressed(cmd.get("code").getAsInt(),0,cmd.has("modifiers")?cmd.get("modifiers").getAsInt():0);
        } else if (action.equals("search")) {
            var box = (net.minecraft.client.gui.widget.TextFieldWidget) field(screen, "searchBox");
            box.setText(cmd.get("text").getAsString());
            Method search = CreativeInventoryScreen.class.getDeclaredMethod("search"); search.setAccessible(true); search.invoke(screen);
        } else if (action.equals("page")) {
            var pages = (List<net.minecraftforge.client.gui.CreativeTabsScreenPage>) field(screen, "pages");
            screen.setCurrentPage(pages.get(cmd.get("index").getAsInt()));
        } else if (action.equals("resize")) {
            screen.resize(client, cmd.get("width").getAsInt(), cmd.get("height").getAsInt());
        } else if (action.equals("mouse")) {
            screen.mouseClicked(cmd.get("x").getAsDouble(),cmd.get("y").getAsDouble(),cmd.get("button").getAsInt());
            screen.mouseReleased(cmd.get("x").getAsDouble(),cmd.get("y").getAsDouble(),cmd.get("button").getAsInt());
        }
        JsonObject out = new JsonObject();
        if (action.equals("probe")) {
            Method hit=net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredMethod("getSlotAt",double.class,double.class);
            hit.setAccessible(true);
            Slot slot=(Slot)hit.invoke(screen,cmd.get("x").getAsDouble(),cmd.get("y").getAsDouble());
            out.addProperty("hit_slot",slot==null?-1:slot.id);
        }
        if (action.equals("seed_hotbars")) {
            int bytes = cmd.has("bytes") ? cmd.get("bytes").getAsInt() : 128 * 1024;
            var storage = client.getCreativeHotbarStorage();
            for (int row = 0; row < 9; row++) {
                var saved = storage.getSavedHotbar(row);
                for (int col = 0; col < 9; col++) {
                    ItemStack stack = new ItemStack(col % 2 == 0 ? SAMPLE.get() : Items.DIAMOND, col + 1);
                    stack.getOrCreateNbt().putInt("qa_row", row);
                    stack.getOrCreateNbt().putInt("qa_col", col);
                    stack.getOrCreateNbt().putByteArray("qa_payload", new byte[bytes]);
                    saved.set(col, stack);
                }
            }
            storage.save();
            for (int row = 0; row < 9; row++) for (int col = 0; col < 9; col++) storage.getSavedHotbar(row).set(col, ItemStack.EMPTY);
            // Force the next selection through the real hotbar.nbt loader rather than a cache.
            Field loaded = storage.getClass().getDeclaredField("loaded");
            loaded.setAccessible(true); loaded.setBoolean(storage, false);
            out.addProperty("saved_payload_bytes", 81L * bytes);
        }
        if (action.equals("stress_hotbars")) {
            int cycles = cmd.has("cycles") ? cmd.get("cycles").getAsInt() : 200;
            select(screen, Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
            assertGrid(screen, false, true);
            assertSavedRows(screen, 0);
            System.gc();
            long before = usedHeap();
            for (int i = 0; i < cycles; i++) {
                select(screen, Registries.ITEM_GROUP.get(ItemGroups.BUILDING_BLOCKS));
                assertGrid(screen, true, false);
                select(screen, Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
                assertGrid(screen, false, true);
                assertSavedRows(screen, 0);
                screen.getScreenHandler().scrollItems(1);
                assertSavedRows(screen, 4);
                select(screen, Registries.ITEM_GROUP.get(ItemGroups.INVENTORY));
                assertGrid(screen, false, false);
                select(screen, Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
                assertGrid(screen, false, true);
                assertSavedRows(screen, 0);
                select(screen, TAB_LIST.get(i % TAB_LIST.size()).get());
                assertGrid(screen, true, false);
            }
            select(screen, Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
            System.gc();
            out.addProperty("cycles", cycles);
            out.addProperty("heap_before_bytes", before);
            out.addProperty("heap_after_bytes", usedHeap());
            out.addProperty("max_heap_bytes", Runtime.getRuntime().maxMemory());
            out.addProperty("native_picker_size", CreativeInventoryScreen.INVENTORY.size());
            out.addProperty("tracked_stacks", ((List<?>)field(screen.getScreenHandler(), "trackedStacks")).size());
            out.addProperty("previous_tracked_stacks", ((List<?>)field(screen.getScreenHandler(), "previousTrackedStacks")).size());
            out.addProperty("private_picker_empty", ((SimpleInventory)field(screen.getScreenHandler(), "bedrockify$picker")).isEmpty());
        }
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
        out.addProperty("x",(int)field(screen,"x"));out.addProperty("y",(int)field(screen,"y"));
        out.addProperty("columns",(int)field(screen,"bedrockify$columns"));out.addProperty("rows",(int)field(screen,"bedrockify$rows"));
        out.addProperty("disabled_picker_cells",handler.slots.stream().filter(s->s instanceof CreativePickerSlot && !s.isEnabled()).count());
        out.addProperty("slots", handler.slots.size());
        out.addProperty("items", handler.itemList.size());
        out.addProperty("classic", (boolean)field(screen, "bedrockify$classic"));
        out.addProperty("source_items", ((List<?>)field(screen, "bedrockify$source")).size());
        out.addProperty("groups", ((List<CreativeGroups.Entry>)field(screen,"bedrockify$entries")).stream().filter(CreativeGroups.Entry::header).count());
        out.addProperty("expanded", field(screen, "bedrockify$expanded").toString());
        out.addProperty("scroll", (float)field(screen, "scrollPosition"));
        out.addProperty("first_visible",((CreativeGrid)handler).bedrockify$firstVisibleIndex((float)field(screen,"scrollPosition")));
        out.addProperty("search_text",((net.minecraft.client.gui.widget.TextFieldWidget)field(screen,"searchBox")).getText());
        var searchBox=(net.minecraft.client.gui.widget.TextFieldWidget)field(screen,"searchBox");
        out.addProperty("search_x",searchBox.getX());out.addProperty("search_y",searchBox.getY());out.addProperty("search_width",searchBox.getWidth());out.addProperty("search_focused",searchBox.isFocused());out.addProperty("search_visible",searchBox.isVisible());
        out.addProperty("cursor", handler.getCursorStack().toString());
        out.addProperty("hotbar0", client.player.getInventory().getStack(0).toString());
        ItemStack hotbar0 = client.player.getInventory().getStack(0);
        out.addProperty("hotbar0_nbt", hotbar0.hasNbt() && hotbar0.getNbt().contains("qa_payload") ?
                "qa_row=" + hotbar0.getNbt().getInt("qa_row") + ",qa_col=" + hotbar0.getNbt().getInt("qa_col") +
                        ",payload_bytes=" + hotbar0.getNbt().getByteArray("qa_payload").length : Objects.toString(hotbar0.getNbt()));
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

    private static long usedHeap() { return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); }

    private static void assertGrid(CreativeInventoryScreen screen, boolean classic, boolean hotbar) throws Exception {
        var handler = screen.getScreenHandler();
        var grid = (CreativeGrid)handler;
        if (CreativeInventoryScreen.INVENTORY.size() != 45) throw new AssertionError("native picker capacity changed");
        if ((boolean)field(screen, "bedrockify$classic") != classic) throw new AssertionError("destination screen mode");
        if (classic) {
            if (grid.bedrockify$pickerInventory() == CreativeInventoryScreen.INVENTORY) throw new AssertionError("shared classic picker");
            if (handler.slots.size() != grid.bedrockify$pickerSize() + 9) throw new AssertionError("classic slot count");
            if (handler.canInsertIntoSlot(handler.getSlot(0)) || handler.canInsertIntoSlot(Items.DIAMOND.getDefaultStack(), handler.getSlot(0))) throw new AssertionError("drag inserts into picker");
        } else {
            if (grid.bedrockify$pickerInventory() != CreativeInventoryScreen.INVENTORY) throw new AssertionError("native picker not restored");
            if (!((SimpleInventory)field(handler, "bedrockify$picker")).isEmpty()) throw new AssertionError("private picker retains old stacks");
            if (!((List<?>)field(screen, "bedrockify$source")).isEmpty() || !((List<?>)field(screen, "bedrockify$entries")).isEmpty()) throw new AssertionError("catalogue retained on native screen");
            if (hotbar && (handler.slots.size() != 54 || handler.itemList.size() != 81)) throw new AssertionError("saved hotbar layout");
            if (hotbar) {
                List<?> originalSlots = (List<?>)field(handler, "bedrockify$nativeSlots");
                for (int i = 0; i < originalSlots.size(); i++) if (handler.getSlot(i) != originalSlots.get(i)) throw new AssertionError("native slot instance replaced");
            }
        }
    }

    private static void assertSavedRows(CreativeInventoryScreen screen, int firstRow) {
        for (int cell = 0; cell < 45; cell++) {
            ItemStack stack = screen.getScreenHandler().getSlot(cell).getStack();
            if (stack.getNbt() == null || stack.getNbt().getInt("qa_row") != firstRow + cell / 9 ||
                    stack.getNbt().getInt("qa_col") != cell % 9 || stack.getNbt().getByteArray("qa_payload").length == 0) throw new AssertionError("saved row/NBT mismatch at " + cell);
        }
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
