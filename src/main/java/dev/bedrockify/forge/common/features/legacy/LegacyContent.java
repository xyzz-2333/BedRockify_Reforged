package dev.bedrockify.forge.common.features.legacy;

import dev.bedrockify.forge.Bedrockify;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemGroup;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegisterEvent;

public final class LegacyContent {
    public static final Block OLD_STONECUTTER = new Block(AbstractBlock.Settings.copy(Blocks.STONECUTTER));
    public static final Block GLOWING_OBSIDIAN = new Block(AbstractBlock.Settings.copy(Blocks.OBSIDIAN)
            .strength(35.0F, 1200.0F).luminance(state -> 12).requiresTool());
    public static final NetherReactorBlock NETHER_REACTOR_CORE = new NetherReactorBlock(
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(3.0F, 6.0F));
    public static final Potion DECAY = new Potion(new StatusEffectInstance(StatusEffects.WITHER, 800, 1));

    private LegacyContent() {}

    private static Identifier id(String path) { return new Identifier(Bedrockify.MOD_ID, path); }

    public static void register(RegisterEvent event) {
        event.register(RegistryKeys.BLOCK, helper -> {
            helper.register(id("old_stonecutter"), OLD_STONECUTTER);
            helper.register(id("glowing_obsidian"), GLOWING_OBSIDIAN);
            helper.register(id("nether_reactor_core"), NETHER_REACTOR_CORE);
        });
        event.register(RegistryKeys.ITEM, helper -> {
            helper.register(id("old_stonecutter"), new BlockItem(OLD_STONECUTTER, new Item.Settings()));
            helper.register(id("glowing_obsidian"), new BlockItem(GLOWING_OBSIDIAN, new Item.Settings()));
            helper.register(id("nether_reactor_core"), new BlockItem(NETHER_REACTOR_CORE, new Item.Settings()));
        });
        event.register(RegistryKeys.POTION, helper -> helper.register(id("decay"), DECAY));
    }

    public static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ItemGroups.FUNCTIONAL)) {
            event.accept(() -> OLD_STONECUTTER);
            event.accept(() -> NETHER_REACTOR_CORE);
        }
        if (event.getTabKey().equals(ItemGroups.BUILDING_BLOCKS)) event.accept(() -> GLOWING_OBSIDIAN);
        if (event.getTabKey().equals(ItemGroups.FOOD_AND_DRINK)) {
            event.getEntries().put(potion(Items.POTION), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
            event.getEntries().put(potion(Items.SPLASH_POTION), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
            event.getEntries().put(potion(Items.LINGERING_POTION), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey().equals(ItemGroups.COMBAT)) event.getEntries().put(potion(Items.TIPPED_ARROW), ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
    }

    private static ItemStack potion(Item item) { return PotionUtil.setPotion(new ItemStack(item), DECAY); }
}
