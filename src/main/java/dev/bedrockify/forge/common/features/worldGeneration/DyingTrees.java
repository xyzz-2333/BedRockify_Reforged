package dev.bedrockify.forge.common.features.worldGeneration;

import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.treedecorator.TreeDecoratorType;


public class DyingTrees {

    public static TreeDecoratorType<FullTrunkVineTreeDecorator> VINE_DECORATOR;

    public static final RegistryKey<ConfiguredFeature<?, ?>> DYING_OAK_TREE = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("bedrockify", "dying_oak_tree"));
    public static final RegistryKey<ConfiguredFeature<?, ?>> DYING_BIRCH_TREE = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("bedrockify", "dying_birch_tree"));
    public static final RegistryKey<ConfiguredFeature<?, ?>> DYING_SPRUCE_TREE = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("bedrockify", "dying_spruce_tree"));
    public static final RegistryKey<ConfiguredFeature<?, ?>> DYING_PINE_TREE = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("bedrockify", "dying_pine_tree"));
    public static final RegistryKey<ConfiguredFeature<?, ?>> DYING_DARK_OAK_TREE = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("bedrockify", "dying_dark_oak_tree"));


    public static final RegistryKey<PlacedFeature> DYING_BIRCH_TREE_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("bedrockify", "dying_birch_tree"));
    public static final RegistryKey<PlacedFeature> DYING_OAK_TREE_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("bedrockify", "dying_oak_tree"));
    public static final RegistryKey<PlacedFeature> DYING_OAK_TREE_PLAINS_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("bedrockify", "dying_oak_tree_plains"));
    public static final RegistryKey<PlacedFeature> DYING_SPRUCE_TREE_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("bedrockify", "dying_spruce_tree"));
    public static final RegistryKey<PlacedFeature> DYING_PINE_TREE_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("bedrockify", "dying_pine_tree"));
    public static final RegistryKey<PlacedFeature> DYING_DARK_OAK_TREE_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("bedrockify", "dying_dark_oak_tree"));


    public static final RegistryKey<ConfiguredFeature<?, ?>> FALLEN_OAK_TREE_CONFIGURED = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("bedrockify", "fallen_oak_tree_c"));
    public static final RegistryKey<ConfiguredFeature<?, ?>> FALLEN_BIRCH_TREE_CONFIGURED = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("bedrockify", "fallen_birch_tree_c"));
    public static final RegistryKey<ConfiguredFeature<?, ?>> FALLEN_SPRUCE_TREE_CONFIGURED = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("bedrockify", "fallen_spruce_tree_c"));

    public static final Feature<DefaultFeatureConfig> FALLEN_OAK_TREE = new FallenTreeFeature(DefaultFeatureConfig.CODEC, Blocks.OAK_LOG);
    public static final Feature<DefaultFeatureConfig> FALLEN_BIRCH_TREE = new FallenTreeFeature(DefaultFeatureConfig.CODEC, Blocks.BIRCH_LOG);
    public static final Feature<DefaultFeatureConfig> FALLEN_SPRUCE_TREE = new FallenTreeFeature(DefaultFeatureConfig.CODEC, Blocks.SPRUCE_LOG);

    public static final RegistryKey<PlacedFeature> FALLEN_OAK_TREE_PLACED = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("bedrockify", "fallen_oak_tree_placed"));
    public static final RegistryKey<PlacedFeature> FALLEN_OAK_TREE_PLAINS_PLACED = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("bedrockify", "fallen_oak_tree_plains_placed"));

    public static final RegistryKey<PlacedFeature> FALLEN_BIRCH_TREE_PLACED = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("bedrockify", "fallen_birch_tree_placed"));
    public static final RegistryKey<PlacedFeature> FALLEN_SPRUCE_TREE_PLACED = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("bedrockify", "fallen_spruce_tree_placed"));

    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(RegistryKeys.TREE_DECORATOR_TYPE)) {
            VINE_DECORATOR = TreeDecoratorType.register("bedrockify:vinedecorator", FullTrunkVineTreeDecorator.CODEC);
        }
        event.register(RegistryKeys.FEATURE, helper -> {
            helper.register(new Identifier("bedrockify", "fallen_oak_tree"), FALLEN_OAK_TREE);
            helper.register(new Identifier("bedrockify", "fallen_birch_tree"), FALLEN_BIRCH_TREE);
            helper.register(new Identifier("bedrockify", "fallen_spruce_tree"), FALLEN_SPRUCE_TREE);
        });
        event.register(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, helper ->
                helper.register(new Identifier("bedrockify", "trees"), BedrockTreesBiomeModifier.CODEC));
    }
}
