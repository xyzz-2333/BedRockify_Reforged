package dev.bedrockify.forge.common.features.worldGeneration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bedrockify.forge.Bedrockify;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/** Adds the upstream placed features using Forge's biome loading pipeline. */
public record BedrockTreesBiomeModifier(RegistryEntryList<Biome> biomes,
        RegistryEntryList<PlacedFeature> features, boolean dying) implements BiomeModifier {
    public static final Codec<BedrockTreesBiomeModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.REGISTRY_ENTRY_LIST_CODEC.fieldOf("biomes").forGetter(BedrockTreesBiomeModifier::biomes),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(BedrockTreesBiomeModifier::features),
            Codec.BOOL.fieldOf("dying").forGetter(BedrockTreesBiomeModifier::dying)
    ).apply(instance, BedrockTreesBiomeModifier::new));

    @Override
    public void modify(RegistryEntry<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        var settings = Bedrockify.getInstance().settings;
        if (phase == Phase.ADD && biomes.contains(biome) && (dying ? settings.dyingTrees : settings.fallenTrees)) {
            for (var feature : features) builder.getGenerationSettings().addFeature(GenerationStep.Feature.VEGETAL_DECORATION.ordinal(), feature);
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() { return CODEC; }
}
