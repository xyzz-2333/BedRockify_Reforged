package dev.bedrockify.forge.client.features.creativeInventory;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The catalogue snapshot and grouped rows, independent of vanilla slot mutation. */
public final class CreativeScreenContent {
    private List<ItemStack> source = List.of();
    private List<CreativeGroups.Entry> entries = List.of();
    private boolean hasGroups;
    public List<CreativeGroups.Entry> entries() { return entries; }
    public boolean hasGroups() { return hasGroups; }
    public void select(boolean classic, ItemGroup tab, List<ItemStack> vanillaItems) {
        if (!classic) {
            source = List.of(); entries = List.of(); hasGroups = false;
            return;
        }
        CreativeCatalog.Category category = CreativeCatalog.category(tab);
        source = category == null ? new ArrayList<>(vanillaItems) : CreativeCatalog.contents(category);
    }
    public void searchResults(List<ItemStack> vanillaItems) { source = new ArrayList<>(vanillaItems); }
    public void rebuild(ItemGroup tab, String search, boolean grouping, Set<String> expanded) {
        boolean collapse = grouping && tab.getType() != ItemGroup.Type.SEARCH && (!tab.hasSearchBar() || search.isEmpty());
        CreativeCatalog.Category category = CreativeCatalog.category(tab);
        entries = CreativeGroups.layout(source, expanded, collapse,
                category == CreativeCatalog.Category.CONSTRUCTION || category == CreativeCatalog.Category.EQUIPMENT);
        hasGroups = entries.stream().anyMatch(CreativeGroups.Entry::header);
    }
    public CreativeGroups.Entry entry(int index) { return index >= 0 && index < entries.size() ? entries.get(index) : null; }
}
