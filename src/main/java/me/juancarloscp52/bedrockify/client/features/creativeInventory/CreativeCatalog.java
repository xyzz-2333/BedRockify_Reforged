package me.juancarloscp52.bedrockify.client.features.creativeInventory;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

/** Reads already-built Forge tabs; never replaces their global contents or search trees. */
public final class CreativeCatalog {
    public enum Category {
        CONSTRUCTION(ItemGroups.BUILDING_BLOCKS, Items.BRICKS.getDefaultStack()),
        EQUIPMENT(ItemGroups.COMBAT, Items.DIAMOND_SWORD.getDefaultStack()),
        ITEMS(ItemGroups.INGREDIENTS, Items.RED_BED.getDefaultStack()),
        NATURE(ItemGroups.NATURAL, Items.GRASS_BLOCK.getDefaultStack());

        public final RegistryKey<ItemGroup> anchor;
        public final ItemStack icon;
        Category(RegistryKey<ItemGroup> anchor, ItemStack icon) { this.anchor = anchor; this.icon = icon; }
        public Text title() { return Text.translatable("bedrockify.creative.category." + name().toLowerCase(Locale.ROOT)); }
    }

    private static final List<RegistryKey<ItemGroup>> SOURCES = List.of(
            ItemGroups.BUILDING_BLOCKS, ItemGroups.COLORED_BLOCKS, ItemGroups.NATURAL,
            ItemGroups.FUNCTIONAL, ItemGroups.REDSTONE, ItemGroups.TOOLS, ItemGroups.COMBAT,
            ItemGroups.FOOD_AND_DRINK, ItemGroups.INGREDIENTS, ItemGroups.SPAWN_EGGS);

    public static Category category(ItemGroup tab) {
        for (Category category : Category.values()) if (Registries.ITEM_GROUP.get(category.anchor) == tab) return category;
        return null;
    }

    public static List<ItemGroup> visibleTabs(List<ItemGroup> forgeTabs) {
        List<ItemGroup> result = new ArrayList<>();
        for (Category category : Category.values()) {
            ItemGroup tab = Registries.ITEM_GROUP.get(category.anchor);
            if (forgeTabs.contains(tab)) result.add(tab);
        }
        for (ItemGroup tab : forgeTabs) {
            // Preserve mod tabs (including their Forge ordering) and the operator tab.
            if (!SOURCES.stream().anyMatch(key -> Registries.ITEM_GROUP.get(key) == tab)) result.add(tab);
        }
        return result;
    }

    public static List<ItemStack> contents(Category wanted) {
        List<ItemStack> result = new ArrayList<>();
        Set<StackKey> seen = new HashSet<>();
        for (RegistryKey<ItemGroup> source : SOURCES) {
            ItemGroup group = Registries.ITEM_GROUP.get(source);
            if (group == null) continue;
            // getDisplayStacks includes BuildCreativeModeTabContentsEvent additions and feature filtering.
            for (ItemStack stack : group.getDisplayStacks()) {
                if (classify(stack, source) == wanted && seen.add(new StackKey(stack))) result.add(stack);
            }
        }
        return result;
    }

    private static Category classify(ItemStack stack, RegistryKey<ItemGroup> source) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        String path = id.getPath();
        if (id.getNamespace().equals("minecraft")) {
            String family = CreativeGroups.family(stack);
            if (family != null && Set.of("logs", "stripped_logs", "wood", "stripped_wood", "leaves", "saplings",
                    "flowers", "coral", "coral_fans", "coral_blocks", "ores", "seeds", "spawn_eggs", "raw_food", "dyes").contains(family)) return Category.NATURE;
            if (family != null && Set.of("beds", "candles", "banners", "shulker_boxes", "chests", "signs", "hanging_signs",
                    "anvils", "music_discs", "buttons", "pressure_plates", "rails", "skulls").contains(family)) return Category.ITEMS;
            if (Set.of("stone", "granite", "diorite", "andesite", "deepslate", "tuff", "calcite", "dripstone_block",
                    "netherrack", "end_stone", "obsidian", "crying_obsidian", "gravel", "sand", "red_sand").contains(path)) return Category.NATURE;
            if (path.endsWith("_door") || path.endsWith("_trapdoor") || path.equals("iron_bars") ||
                    path.equals("ladder") || path.equals("scaffolding")) return Category.CONSTRUCTION;
            if (path.contains("bucket") || path.endsWith("_boat") || path.endsWith("_raft") ||
                    path.endsWith("_minecart") || path.equals("minecart")) return Category.ITEMS;
            if (path.endsWith("_spawn_egg") || path.equals("bone_meal") || path.equals("bone") ||
                    path.equals("egg") || path.equals("sugar_cane") || path.equals("wheat") ||
                    path.equals("wheat_seeds") || path.endsWith("_seeds") || path.equals("cocoa_beans") ||
                    path.equals("rotten_flesh") || path.equals("spider_eye")) return Category.NATURE;
            if (Set.of("beef", "porkchop", "chicken", "mutton", "rabbit", "cod", "salmon").contains(path)) return Category.NATURE;
        }
        if (source == ItemGroups.BUILDING_BLOCKS || source == ItemGroups.COLORED_BLOCKS) return Category.CONSTRUCTION;
        if (source == ItemGroups.NATURAL || source == ItemGroups.SPAWN_EGGS) return Category.NATURE;
        if (source == ItemGroups.TOOLS || source == ItemGroups.COMBAT || source == ItemGroups.FOOD_AND_DRINK) return Category.EQUIPMENT;
        return Category.ITEMS;
    }

    /** NBT is part of identity: potion, enchantment and mod variants must not disappear. */
    private record StackKey(net.minecraft.item.Item item, net.minecraft.nbt.NbtCompound nbt) {
        StackKey(ItemStack stack) { this(stack.getItem(), stack.getNbt() == null ? null : stack.getNbt().copy()); }
    }

    private CreativeCatalog() {}
}
