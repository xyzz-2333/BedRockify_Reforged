package dev.bedrockify.forge.client.features.survivalInventory;

import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeGroups;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.item.*;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;
import dev.bedrockify.forge.client.features.inventoryMemory.InventoryUiState;

import java.util.*;

/** A view over native recipe-book collections. Never synthesizes recipes or item combinations. */
public final class BedrockRecipePanel {
    public enum Category {
        ALL(Items.COMPASS), CONSTRUCTION(Items.BRICKS), EQUIPMENT(Items.IRON_SWORD),
        ITEMS(Items.RED_BED), NATURE(Items.GRASS_BLOCK);
        final ItemStack icon;
        Category(Item item) { icon = item.getDefaultStack(); }
        Text title() { return Text.translatable("bedrockify.survival.category." + name().toLowerCase(Locale.ROOT)); }
    }
    public record Node(Recipe<?> recipe, RecipeResultCollection collection, ItemStack output,
                       String family, Category category, String searchText) {}
    public record Entry(Node node, String family, int count, boolean craftable) {
        public boolean header() { return family != null; }
    }
    public static final int COLS = 8, ROWS = 6, PAGE_SIZE = COLS * ROWS, GRID_Y = 52;
    private static final int CATEGORY_Y = 4, CATEGORY_SIZE = 24, FILTER_Y = 28, FOOTER_Y = SurvivalLayout.HEIGHT - 23;
    private static final TagKey<Item> INGOTS = TagKey.of(RegistryKeys.ITEM, new Identifier("forge", "ingots"));
    private static final TagKey<Item> NUGGETS = TagKey.of(RegistryKeys.ITEM, new Identifier("forge", "nuggets"));
    private static final TagKey<Item> STORAGE = TagKey.of(RegistryKeys.ITEM, new Identifier("forge", "storage_blocks"));
    private final MinecraftClient client;
    private final AbstractRecipeScreenHandler<?> handler;
    private final RecipeQuickCraft quickCraft;
    private Identifier selectedRecipe;
    private String lastQuery = "";
    private final Set<String> expanded = new HashSet<>();
    private List<RecipeResultCollection> source;
    private List<RecipeResultCollection> craftingCollections = List.of();
    private List<Node> nodes = List.of();
    private List<Entry> entries = List.of();
    private Language language;
    private long inputVersion = Long.MIN_VALUE;
    private int page, filteredCount;
    private Category category = Category.ALL;
    private boolean craftableOnly;
    private TextFieldWidget search;
    private SurvivalLayout layout;
    private InventoryUiState.View remembered;
    private boolean restored;
    private final String memoryKey;
    public long matchingPasses, rebuildCount;
    public int paintedCells;

    public BedrockRecipePanel(MinecraftClient client, AbstractRecipeScreenHandler<?> handler) {
        this.client = client; this.handler = handler;
        quickCraft = new RecipeQuickCraft(client, handler);
        memoryKey = handler.getCraftingWidth() == 3 ? "survival:crafting" : "survival:inventory";
        var mod = BedrockifyClient.getInstance();
        if (mod.settings.rememberInventoryState && mod.inventoryUiState != null) {
            remembered = mod.inventoryUiState.view(memoryKey, mod.settings.rememberInventorySearch);
            if (remembered != null) {
                try { category = Category.valueOf(remembered.category()); }
                catch (IllegalArgumentException ignored) { category = Category.ALL; }
                expanded.addAll(remembered.expanded());
            }
        }
    }
    public boolean crafting() { return quickCraft.running(); }
    public void tick(boolean active) { quickCraft.tick(active); }
    public void cancelCrafting() { quickCraft.cancel(); }
    public int category() { return category.ordinal(); }
    public Set<String> expanded() { return Set.copyOf(expanded); }
    public boolean restoreSearch(TextFieldWidget field) {
        if (restored || field == null) return false;
        restored = true;
        if (remembered == null) return false;
        field.setText(remembered.search());
        return true;
    }
    public void remember() {
        var mod = BedrockifyClient.getInstance();
        if (!mod.settings.rememberInventoryState || mod.inventoryUiState == null) return;
        // A book closed before its first initialization has no catalogue or search field.
        if (remembered != null && !restored) return;
        mod.inventoryUiState.remember(memoryKey, new InventoryUiState.View(category.name(),
                search == null ? "" : search.getText(), page * PAGE_SIZE, expanded));
        mod.inventoryUiState.save(mod.settings.rememberInventorySearch);
    }
    public List<Entry> entries() { return entries; }
    public int recipeCount() { return nodes.size(); }
    public int filteredCount() { return filteredCount; }
    public int page() { return page; }
    public int pages() { return Math.max(1, MathHelper.ceilDiv(entries.size(), PAGE_SIZE)); }
    public boolean craftableOnly() { return craftableOnly; }
    public void invalidate() { source = null; inputVersion = Long.MIN_VALUE; }

    public void refresh(RecipeMatcher finder, ClientRecipeBook book, TextFieldWidget search, long version, boolean resetPage) {
        this.search = search;
        var current = book.getOrderedResults();
        if (source != current || language != Language.getInstance()) {
            source = current; language = Language.getInstance();
            List<Node> all = new ArrayList<>();
            List<RecipeResultCollection> collections = new ArrayList<>();
            Set<Identifier> seen = new HashSet<>();
            for (var collection : current) {
                boolean crafting = false;
                for (var recipe : collection.getAllRecipes()) {
                    if (recipe.getType() != RecipeType.CRAFTING || !seen.add(recipe.getId())) continue;
                    crafting = true;
                    ItemStack output = recipe.getOutput(client.world.getRegistryManager());
                    String family = family(output);
                    all.add(new Node(recipe, collection, output, family, category(output, family),
                            (output.getName().getString() + " " + Registries.ITEM.getId(output.getItem())
                                    + " " + recipe.getId()).toLowerCase(Locale.ROOT)));
                }
                if (crafting) collections.add(collection);
            }
            nodes = List.copyOf(all); craftingCollections = List.copyOf(collections);
            inputVersion = Long.MIN_VALUE;
        }
        if (inputVersion != version) {
            for (var collection : craftingCollections)
                collection.computeCraftables(finder, handler.getCraftingWidth(), handler.getCraftingHeight(), book);
            inputVersion = version; matchingPasses++;
        }
        craftableOnly = book.isFilteringCraftable(handler);
        rebuild(resetPage);
    }

    private static String family(ItemStack stack) {
        if (stack.isIn(ItemTags.PLANKS)) return "planks";
        if (stack.isIn(ItemTags.LOGS)) return "logs";
        if (stack.isIn(ItemTags.WOODEN_STAIRS)) return "stairs";
        if (stack.isIn(ItemTags.WOODEN_SLABS)) return "slabs";
        if (stack.isIn(ItemTags.WOODEN_FENCES)) return "fences";
        if (stack.isIn(ItemTags.WOODEN_DOORS)) return "doors";
        if (stack.isIn(ItemTags.WOODEN_TRAPDOORS)) return "trapdoors";
        if (stack.isIn(ItemTags.WOODEN_BUTTONS)) return "buttons";
        if (stack.isIn(ItemTags.WOODEN_PRESSURE_PLATES)) return "pressure_plates";
        if (stack.isIn(INGOTS)) return "ingots";
        if (stack.isIn(NUGGETS)) return "nuggets";
        if (stack.isIn(STORAGE)) return "storage_blocks";
        return CreativeGroups.family(stack);
    }
    private static Category category(ItemStack stack, String family) {
        if (Set.of("ingots", "nuggets", "storage_blocks").contains(family == null ? "" : family)) return Category.ITEMS;
        if (Set.of("planks", "stairs", "slabs", "fences", "fence_gates", "doors", "trapdoors", "walls",
                "buttons", "pressure_plates", "glass", "glass_panes").contains(family == null ? "" : family)) return Category.CONSTRUCTION;
        if (Set.of("logs", "wood", "stripped_logs", "stripped_wood", "leaves", "saplings", "flowers",
                "seeds", "ores", "coral", "coral_fans", "coral_blocks").contains(family == null ? "" : family)) return Category.NATURE;
        Item item = stack.getItem();
        if (item instanceof ToolItem || item instanceof ArmorItem || item instanceof SwordItem
                || item instanceof BowItem || item instanceof CrossbowItem || item instanceof ShieldItem
                || item instanceof TridentItem || stack.isFood()) return Category.EQUIPMENT;
        if (item instanceof BlockItem && !Set.of("beds", "chests", "shulker_boxes", "anvils", "signs",
                "hanging_signs", "banners", "candles").contains(family == null ? "" : family)) return Category.CONSTRUCTION;
        return Category.ITEMS;
    }
    public void category(int ordinal) {
        selectedRecipe = null; quickCraft.cancel();
        category = Category.values()[MathHelper.clamp(ordinal, 0, Category.values().length - 1)]; rebuild(true);
    }
    public void expandAll(boolean expand) {
        selectedRecipe = null; quickCraft.cancel();
        expanded.clear();
        if (expand) for (Node node : nodes) if (node.family != null) expanded.add(node.family);
        rebuild(true);
    }
    public void rebuild(boolean resetPage) {
        if (client.player == null) return;
        String query = search == null ? "" : search.getText().strip().toLowerCase(Locale.ROOT);
        if (!query.equals(lastQuery)) { selectedRecipe = null; quickCraft.cancel(); lastQuery = query; }
        List<Node> filtered = new ArrayList<>();
        boolean tagSearch = query.startsWith("#");
        String tagQuery = tagSearch ? query.substring(1) : "";
        for (Node node : nodes) {
            if (!client.player.getRecipeBook().contains(node.recipe)
                    || !node.recipe.fits(handler.getCraftingWidth(), handler.getCraftingHeight())
                    || category != Category.ALL && node.category != category
                    || craftableOnly && !node.collection.isCraftable(node.recipe)) continue;
            if (!query.isEmpty()) {
                boolean match = tagSearch ? node.output.streamTags().anyMatch(tag -> tag.id().toString().contains(tagQuery))
                        : node.searchText.contains(query);
                if (!match) continue;
            }
            filtered.add(node);
        }
        filteredCount = filtered.size();
        boolean collapse = BedrockifyClient.getInstance().settings.survivalRecipeGroups && query.isEmpty();
        Map<String, List<Node>> families = new LinkedHashMap<>();
        if (collapse) for (Node node : filtered) if (node.family != null)
            families.computeIfAbsent(node.family, key -> new ArrayList<>()).add(node);
        List<Entry> visible = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (Node node : filtered) {
            List<Node> family = collapse && node.family != null ? families.get(node.family) : null;
            if (family == null || family.size() < 2) visible.add(new Entry(node, null, 1, node.collection.isCraftable(node.recipe)));
            else if (added.add(node.family)) {
                Node representative = family.stream().filter(n -> n.collection.isCraftable(n.recipe)).findFirst().orElse(node);
                visible.add(new Entry(representative, node.family, family.size(), representative.collection.isCraftable(representative.recipe)));
                if (expanded.contains(node.family)) for (Node member : family)
                    visible.add(new Entry(member, null, 1, member.collection.isCraftable(member.recipe)));
            }
        }
        entries = List.copyOf(visible);
        if (remembered != null && restored) {
            page = MathHelper.clamp(remembered.first() / PAGE_SIZE, 0, pages() - 1);
            remembered = null;
        } else page = resetPage ? 0 : MathHelper.clamp(page, 0, pages() - 1);
        rebuildCount++;
    }
    public void position(SurvivalLayout layout, TextFieldWidget search) {
        this.layout = layout; this.search = search;
        if (search != null && layout != null) {
            search.setX(layout.recipeLeft() + 10); search.setY(layout.top() + FILTER_Y + 2);
            search.setWidth(142); search.setEditableColor(0xffffff);
            search.setPlaceholder(Text.translatable("bedrockify.survival.search"));
        }
    }
    public boolean contains(double x, double y) {
        return layout != null && x >= layout.recipeLeft() && x < layout.recipeLeft() + SurvivalLayout.RECIPE_WIDTH
                && y >= layout.top() && y < layout.top() + SurvivalLayout.HEIGHT;
    }
    private int hovered(double x, double y) {
        if (layout == null) return -1;
        int col = (int)Math.floor((x - layout.recipeLeft() - 10) / 20);
        int row = (int)Math.floor((y - layout.top() - GRID_Y) / 20);
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return -1;
        int i = page * PAGE_SIZE + row * COLS + col;
        return i < entries.size() ? i : -1;
    }
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (layout == null) return;
        int x = layout.recipeLeft(), y = layout.top();
        SurvivalSprites.panel(context, x, y, SurvivalLayout.RECIPE_WIDTH, SurvivalLayout.HEIGHT, false);
        for (int i = 0; i < Category.values().length; i++) {
            int tx = x + 10 + i * 32;
            SurvivalSprites.button(context, tx, y + CATEGORY_Y, CATEGORY_SIZE, CATEGORY_SIZE, category.ordinal() == i);
            context.drawItem(Category.values()[i].icon, tx + 4, y + CATEGORY_Y + 4);
        }
        SurvivalSprites.button(context, x + 158, y + FILTER_Y, 22, 22, craftableOnly);
        context.drawItem(Items.CRAFTING_TABLE.getDefaultStack(), x + 161, y + FILTER_Y + 3);
        if (search != null) search.render(context, mouseX, mouseY, delta);
        SurvivalSprites.panel(context, x + 8, y + GRID_Y - 2, 174, SurvivalLayout.HEIGHT - 24 - (GRID_Y - 2), true);
        int hover = hovered(mouseX, mouseY); paintedCells = 0;
        for (int i = page * PAGE_SIZE; i < Math.min(entries.size(), (page + 1) * PAGE_SIZE); i++) {
            Entry entry = entries.get(i); int local = i % PAGE_SIZE;
            int sx = x + 10 + local % COLS * 20, sy = y + GRID_Y + local / COLS * 20;
            SurvivalSprites.cell(context, sx, sy, !entry.craftable);
            context.drawItem(entry.node.output, sx + 2, sy + 2);
            if (entry.header()) marker(context, expanded.contains(entry.family) ? "−" : "+", sx + 12, sy + 10);
            else context.drawItemInSlot(client.textRenderer, entry.node.output, sx + 2, sy + 2);
            if (i == hover || !entry.header() && entry.node.recipe.getId().equals(selectedRecipe)) {
                context.fill(sx, sy, sx + 20, sy + 1, 0xffffffff); context.fill(sx, sy + 19, sx + 20, sy + 20, 0xffffffff);
                context.fill(sx, sy, sx + 1, sy + 20, 0xffffffff); context.fill(sx + 19, sy, sx + 20, sy + 20, 0xffffffff);
            }
            paintedCells++;
        }
        if (entries.isEmpty()) context.drawCenteredTextWithShadow(client.textRenderer,
                Text.translatable("bedrockify.survival.empty"), x + 95, y + 100, 0xffffff);
        context.drawText(client.textRenderer, Text.translatable("bedrockify.survival.count", filteredCount), x + 10, y + FOOTER_Y + 5, 0x303030, false);
        context.drawText(client.textRenderer, (page + 1) + "/" + pages(), x + 88, y + FOOTER_Y + 5, 0x303030, false);
        SurvivalSprites.button(context, x + 136, y + FOOTER_Y, 20, 20, false);
        SurvivalSprites.button(context, x + 160, y + FOOTER_Y, 20, 20, false);
        SurvivalSprites.pageArrow(context, x + 136, y + FOOTER_Y, false, page > 0);
        SurvivalSprites.pageArrow(context, x + 160, y + FOOTER_Y, true, page + 1 < pages());
    }
    private void marker(DrawContext c, String text, int x, int y) {
        c.getMatrices().push();
        c.getMatrices().translate(0, 0, 300);
        c.drawText(client.textRenderer, text, x - 1, y, 0xff000000, false);
        c.drawText(client.textRenderer, text, x + 1, y, 0xff000000, false);
        c.drawText(client.textRenderer, text, x, y - 1, 0xff000000, false);
        c.drawText(client.textRenderer, text, x, y + 1, 0xff000000, false);
        c.drawText(client.textRenderer, text, x, y, 0xffffffff, false);
        c.getMatrices().pop();
    }
    public boolean mouseClicked(double mx, double my, int button, Runnable toggleFilter, Runnable clearGhost) {
        quickCraft.cancel();
        if (!contains(mx, my)) { if (search != null) search.setFocused(false); return false; }
        if (search != null && search.mouseClicked(mx, my, button)) {
            search.setFocused(true);
            return true;
        }
        if (search != null) search.setFocused(false);
        int x = layout.recipeLeft(), y = layout.top();
        if (button == 0 && my >= y + CATEGORY_Y && my < y + CATEGORY_Y + CATEGORY_SIZE && mx >= x + 10 && mx < x + 170) {
            int i = (int)(mx - x - 10) / 32;
            if ((int)(mx - x - 10) % 32 < CATEGORY_SIZE) category(i);
            return true;
        }
        if (button == 0 && mx >= x + 158 && mx < x + 180 && my >= y + FILTER_Y && my < y + FILTER_Y + 22) {
            toggleFilter.run(); return true;
        }
        if (button == 0 && my >= y + FOOTER_Y && my < y + FOOTER_Y + 20) {
            if (mx >= x + 136 && mx < x + 156) page = Math.max(0, page - 1);
            if (mx >= x + 160 && mx < x + 180) page = Math.min(pages() - 1, page + 1);
            return true;
        }
        int i = hovered(mx, my);
        if (i >= 0 && (button == 0 || button == 1)) {
            Entry entry = entries.get(i);
            if (entry.header() && button == 0) {
                selectedRecipe = null;
                if (!expanded.add(entry.family)) expanded.remove(entry.family);
                rebuild(false);
            } else if (client.interactionManager != null) {
                clearGhost.run();
                boolean repeat = entry.node.recipe.getId().equals(selectedRecipe);
                selectedRecipe = entry.node.recipe.getId();
                if (button == 0 && entry.craftable && (repeat || Screen.hasShiftDown()))
                    quickCraft.start(entry.node.recipe, Screen.hasShiftDown());
                else client.interactionManager.clickRecipe(handler.syncId, entry.node.recipe, false);
            }
        }
        return true;
    }
    public boolean scroll(double x, double y, double amount) {
        if (!contains(x, y)) return false;
        quickCraft.cancel();
        page = MathHelper.clamp(page - (int)Math.signum(amount), 0, pages() - 1); return true;
    }
    public void tooltip(DrawContext context, int mx, int my) {
        int i = hovered(mx, my);
        if (i >= 0) {
            Entry entry = entries.get(i);
            if (entry.header()) context.drawTooltip(client.textRenderer,
                    Text.translatable("bedrockify.survival.group", familyName(entry.family), entry.count), mx, my);
            else context.drawItemTooltip(client.textRenderer, entry.node.output, mx, my);
        } else if (layout != null && contains(mx, my)) {
            int x = layout.recipeLeft(), y = layout.top();
            if (my >= y + CATEGORY_Y && my < y + CATEGORY_Y + CATEGORY_SIZE && mx >= x + 10 && mx < x + 170)
                context.drawTooltip(client.textRenderer, Category.values()[(mx - x - 10) / 32].title(), mx, my);
            if (my >= y + FILTER_Y && my < y + FILTER_Y + 22 && mx >= x + 158 && mx < x + 180)
                context.drawTooltip(client.textRenderer, Text.translatable(craftableOnly ? "bedrockify.survival.craftable" : "bedrockify.survival.allRecipes"), mx, my);
            if (my >= y + FOOTER_Y && my < y + FOOTER_Y + 20) {
                if (mx >= x + 136 && mx < x + 156)
                    context.drawTooltip(client.textRenderer, Text.translatable("bedrockify.survival.previousPage"), mx, my);
                else if (mx >= x + 160 && mx < x + 180)
                    context.drawTooltip(client.textRenderer, Text.translatable("bedrockify.survival.nextPage"), mx, my);
            }
        }
    }
    private static Text familyName(String family) {
        return Text.translatable((Set.of("ingots", "nuggets", "storage_blocks").contains(family)
                ? "bedrockify.survival.group." : "bedrockify.creative.group.") + family);
    }
}
