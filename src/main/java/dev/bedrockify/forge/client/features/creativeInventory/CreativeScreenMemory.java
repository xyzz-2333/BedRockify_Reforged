package dev.bedrockify.forge.client.features.creativeInventory;

import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.inventoryMemory.InventoryUiState;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Per-screen memory lifecycle. Callbacks are the only access to vanilla's private tab selection. */
public final class CreativeScreenMemory {
    private final Set<String> expanded = new HashSet<>();
    private final Set<String> expandedView = Collections.unmodifiableSet(expanded);
    private boolean ready, initializing, restoring;
    public Set<String> expanded() { return expandedView; }
    public boolean isExpanded(String group) { return expanded.contains(group); }
    public void toggle(String group) { if (!expanded.remove(group)) expanded.add(group); }
    public void expand(String group) { expanded.add(group); }
    public void collapseAll() { expanded.clear(); }
    public void beginInitialization() { initializing = true; }
    private boolean enabled() {
        var mod = BedrockifyClient.getInstance();
        return mod != null && mod.settings != null && mod.settings.creativeInventory
                && mod.settings.rememberInventoryState && mod.inventoryUiState != null;
    }
    private static ItemGroup resolve(String id) {
        Identifier parsed = Identifier.tryParse(id);
        return parsed == null ? null : Registries.ITEM_GROUP.get(parsed);
    }
    public void restoreTabs(List<CreativeTabsScreenPage> pages, Supplier<CreativeTabsScreenPage> currentPage,
                            Consumer<CreativeTabsScreenPage> selectPage,
                            Consumer<ItemGroup> selectTab) {
        if (enabled()) {
            var state = BedrockifyClient.getInstance().inventoryUiState;
            ItemGroup wanted = resolve(state.creativeTab());
            if (wanted != null) for (var page : pages) {
                if (!page.getVisibleTabs().contains(wanted)) continue;
                restoring = true;
                try { if (currentPage.get() != page) selectPage.accept(page); selectTab.accept(wanted); }
                finally { restoring = false; }
                break;
            }
            ItemGroup anchor = resolve(state.creativePage());
            if (anchor != null) for (var page : pages) {
                if (page.getVisibleTabs().contains(anchor)) { selectPage.accept(page); break; }
            }
        }
        initializing = false;
        ready = true;
        if (enabled()) BedrockifyClient.getInstance().inventoryUiState.creativeView(0);
    }
    public void remember(ItemGroup selected, CreativeTabsScreenPage page, boolean classic,
                         TextFieldWidget search, int firstIndex) {
        if (!ready || initializing || restoring || !enabled()) return;
        var mod = BedrockifyClient.getInstance();
        var state = mod.inventoryUiState;
        state.creativeView(0);
        String id = Registries.ITEM_GROUP.getId(selected).toString();
        state.creativeTab(id);
        if (page != null && !page.getVisibleTabs().isEmpty())
            state.creativePage(Registries.ITEM_GROUP.getId(page.getVisibleTabs().get(0)).toString());
        if (classic) state.remember("creative:" + id, new InventoryUiState.View("",
                selected.hasSearchBar() && search != null ? search.getText() : "", firstIndex, expanded));
        state.save(mod.settings.rememberInventorySearch);
    }
    public InventoryUiState.View select(ItemGroup group, boolean classic) {
        var mod = BedrockifyClient.getInstance();
        InventoryUiState.View view = classic && mod.settings.rememberInventoryState && mod.inventoryUiState != null
                ? mod.inventoryUiState.view("creative:" + Registries.ITEM_GROUP.getId(group), mod.settings.rememberInventorySearch) : null;
        if (mod.settings.rememberInventoryState) {
            expanded.clear();
            if (view != null) expanded.addAll(view.expanded());
        }
        return view;
    }
    public void restoreSearch(InventoryUiState.View view, ItemGroup group, TextFieldWidget search, Runnable refreshSearch) {
        if (view == null || !group.hasSearchBar() || search == null) return;
        search.setText(view.search());
        if (!view.search().isEmpty()) refreshSearch.run();
    }
}
