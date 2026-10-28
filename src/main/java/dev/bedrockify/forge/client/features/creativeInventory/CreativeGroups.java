package dev.bedrockify.forge.client.features.creativeInventory;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public final class CreativeGroups {
    public record Entry(ItemStack stack, String group, int count) {
        public boolean header() { return group != null; }
        public Text title() { return Text.translatable("bedrockify.creative.group." + group); }
    }

    // Specific families precede general suffixes: chest boats, stained panes and stripped logs stay distinct.
    private static final List<String> ORDER = List.of("planks", "walls", "fences", "fence_gates", "stairs", "doors",
            "trapdoors", "glass", "glass_panes", "slabs", "stone_bricks", "sandstone", "wool", "carpet", "concrete", "concrete_powder",
            "terracotta", "glazed_terracotta", "logs", "stripped_logs", "wood", "stripped_wood", "leaves", "saplings",
            "flowers", "coral", "coral_fans", "coral_blocks", "ores", "seeds", "spawn_eggs", "helmets", "chestplates",
            "leggings", "boots", "swords", "axes", "pickaxes", "shovels", "hoes", "cooked_food", "other_food", "crops", "suspicious_stew", "raw_food", "mushrooms", "banners",
            "beds", "candles", "chests", "shulker_boxes", "signs", "hanging_signs", "boats", "chest_boats", "music_discs",
            "anvils", "potions", "splash_potions", "lingering_potions", "tipped_arrows", "enchanted_books", "dyes",
            "buttons", "pressure_plates", "rails", "skulls", "copper_blocks", "goat_horns", "paintings",
            "infested_blocks", "banner_patterns", "pottery_sherds", "armor_trim_templates", "hardened_glass", "hardened_glass_panes", "elements", "chlorides", "compounds");

    public static String family(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id.getNamespace().equals("bedrockify")) {
            if (id.getPath().endsWith("hardened_glass_pane")) return "hardened_glass_panes";
            if (id.getPath().endsWith("hardened_glass")) return "hardened_glass";
            if (id.getPath().matches("element_[0-9]+")) return "elements";
            if (id.getPath().endsWith("_chloride")) return "chlorides";
            if (dev.bedrockify.forge.common.features.education.EducationContent.COMPOUNDS.containsKey(id.getPath())) return "compounds";
        }
        // Unknown mod families remain fully visible instead of guessing from their display names.
        if (!id.getNamespace().equals("minecraft")) return null;
        String p = id.getPath();
        if (Set.of("sandstone", "chiseled_sandstone", "cut_sandstone", "smooth_sandstone",
                "red_sandstone", "chiseled_red_sandstone", "cut_red_sandstone", "smooth_red_sandstone").contains(p)) return "sandstone";
        if (Set.of("stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks",
                "end_stone_bricks", "prismarine_bricks", "polished_blackstone_bricks", "cracked_polished_blackstone_bricks",
                "chiseled_polished_blackstone", "deepslate_bricks", "cracked_deepslate_bricks", "deepslate_tiles",
                "cracked_deepslate_tiles", "chiseled_deepslate", "mud_bricks").contains(p)) return "stone_bricks";
        if (Set.of("bread", "baked_potato", "mushroom_stew", "rabbit_stew", "beetroot_soup", "cookie",
                "pumpkin_pie", "cake", "dried_kelp").contains(p)) return "other_food";
        if (Set.of("apple", "golden_apple", "enchanted_golden_apple", "melon_slice", "sweet_berries", "glow_berries",
                "carrot", "golden_carrot", "potato", "beetroot").contains(p)) return "crops";
        if (p.equals("suspicious_stew")) return "suspicious_stew";
        if (Set.of("brown_mushroom", "red_mushroom", "brown_mushroom_block", "red_mushroom_block",
                "mushroom_stem", "crimson_fungus", "warped_fungus").contains(p)) return "mushrooms";
        String copper=p.startsWith("waxed_") ? p.substring(6) : p;
        if (Set.of("copper_block", "exposed_copper", "weathered_copper", "oxidized_copper").contains(copper)
                || copper.matches("(?:(?:exposed|weathered|oxidized)_)?cut_copper(?:_stairs|_slab)?")) return "copper_blocks";
        if (p.equals("goat_horn")) return "goat_horns";
        if (p.equals("painting")) return "paintings";
        if (p.startsWith("infested_")) return "infested_blocks";
        if (p.endsWith("_banner_pattern")) return "banner_patterns";
        if (p.endsWith("_pottery_sherd")) return "pottery_sherds";
        if (p.endsWith("_armor_trim_smithing_template")) return "armor_trim_templates";
        if (p.endsWith("_hanging_sign")) return "hanging_signs";
        if (p.endsWith("_sign")) return "signs";
        if (p.endsWith("_chest_boat") || p.equals("bamboo_chest_raft")) return "chest_boats";
        if (p.endsWith("_boat") || p.equals("bamboo_raft")) return "boats";
        if (p.endsWith("_fence_gate")) return "fence_gates";
        if (p.endsWith("_trapdoor")) return "trapdoors";
        if (p.endsWith("_door")) return "doors";
        if (p.endsWith("_glass_pane") || p.equals("glass_pane")) return "glass_panes";
        if (p.endsWith("_glass") || p.equals("glass")) return "glass";
        if (p.endsWith("_glazed_terracotta")) return "glazed_terracotta";
        if (p.endsWith("_terracotta") || p.equals("terracotta")) return "terracotta";
        if (p.startsWith("stripped_") && (p.endsWith("_log") || p.endsWith("_stem"))) return "stripped_logs";
        if (p.startsWith("stripped_") && (p.endsWith("_wood") || p.endsWith("_hyphae"))) return "stripped_wood";
        if (p.endsWith("_log") || p.endsWith("_stem")) return "logs";
        if (p.endsWith("_wood") || p.endsWith("_hyphae")) return "wood";
        if (p.endsWith("_coral_block")) return "coral_blocks";
        if (p.endsWith("_coral_fan")) return "coral_fans";
        if (p.endsWith("_coral")) return "coral";
        if (p.startsWith("music_disc_")) return "music_discs";
        if (p.equals("shulker_box") || p.endsWith("_shulker_box")) return "shulker_boxes";
        if (p.equals("anvil") || p.endsWith("_anvil")) return "anvils";
        if (p.equals("candle") || p.endsWith("_candle")) return "candles";
        if (Set.of("chest", "trapped_chest", "ender_chest").contains(p)) return "chests";
        if (p.equals("rail") || p.endsWith("_rail")) return "rails";
        if (p.endsWith("_skull") || p.endsWith("_head")) return "skulls";
        if (p.equals("potion")) return "potions";
        if (p.equals("splash_potion")) return "splash_potions";
        if (p.equals("lingering_potion")) return "lingering_potions";
        if (p.equals("tipped_arrow")) return "tipped_arrows";
        if (p.equals("enchanted_book")) return "enchanted_books";
        if (p.startsWith("cooked_")) return "cooked_food";
        if (Set.of("beef", "porkchop", "chicken", "mutton", "rabbit", "cod", "salmon", "pufferfish", "tropical_fish").contains(p)) return "raw_food";
        if (Set.of("dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip",
                "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose",
                "sunflower", "lilac", "rose_bush", "peony", "torchflower", "pitcher_plant").contains(p)) return "flowers";
        for (String suffix : List.of("planks", "wall", "fence", "stairs", "slab", "wool", "carpet", "concrete_powder",
                "concrete", "leaves", "sapling", "ore", "seeds", "spawn_egg", "helmet", "chestplate", "leggings",
                "boots", "sword", "pickaxe", "axe", "shovel", "hoe", "banner", "bed", "dye", "button", "pressure_plate")) {
            if (p.endsWith("_" + suffix)) return switch (suffix) {
                case "wall" -> "walls"; case "fence" -> "fences"; case "slab" -> "slabs";
                case "sapling" -> "saplings"; case "ore" -> "ores"; case "spawn_egg" -> "spawn_eggs";
                case "helmet" -> "helmets"; case "chestplate" -> "chestplates"; case "sword" -> "swords";
                case "pickaxe" -> "pickaxes"; case "axe" -> "axes"; case "shovel" -> "shovels"; case "hoe" -> "hoes";
                case "banner" -> "banners"; case "bed" -> "beds"; case "dye" -> "dyes"; case "button" -> "buttons";
                case "pressure_plate" -> "pressure_plates"; default -> suffix;
            };
        }
        return null;
    }

    public static List<Entry> layout(List<ItemStack> stacks, Set<String> expanded, boolean collapse, boolean bedrockOrder) {
        if (!collapse) return stacks.stream().map(s -> new Entry(s, null, 0)).toList();
        Map<String, List<ItemStack>> families = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            String group = family(stack);
            if (group != null) families.computeIfAbsent(group, key -> new ArrayList<>()).add(stack);
        }
        List<Entry> result = new ArrayList<>();
        Set<String> added = new HashSet<>();
        if (bedrockOrder) for (String group : ORDER) append(group, families, expanded, added, result);
        for (ItemStack stack : stacks) {
            String group = family(stack);
            if (group == null || families.get(group).size() < 2) result.add(new Entry(stack, null, 0));
            else append(group, families, expanded, added, result);
        }
        return result;
    }

    private static void append(String group, Map<String, List<ItemStack>> families, Set<String> expanded,
                               Set<String> added, List<Entry> result) {
        List<ItemStack> members = families.get(group);
        if (members == null || members.size() < 2 || !added.add(group)) return;
        ItemStack icon = group.equals("cooked_food") ? members.stream()
                .filter(s -> s.isOf(net.minecraft.item.Items.COOKED_CHICKEN)).findFirst().orElse(members.get(0)) : members.get(0);
        result.add(new Entry(icon, group, members.size()));
        if (expanded.contains(group)) for (ItemStack stack : members) result.add(new Entry(stack, null, 0));
    }

    private CreativeGroups() {}
}
