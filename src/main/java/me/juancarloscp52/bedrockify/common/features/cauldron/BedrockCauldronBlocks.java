package me.juancarloscp52.bedrockify.common.features.cauldron;

import me.juancarloscp52.bedrockify.Bedrockify;
import me.juancarloscp52.bedrockify.common.block.ColoredWaterCauldronBlock;
import me.juancarloscp52.bedrockify.common.block.PotionCauldronBlock;
import me.juancarloscp52.bedrockify.common.block.entity.WaterCauldronBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;

public final class BedrockCauldronBlocks {
    public static final Block POTION_CAULDRON = new PotionCauldronBlock(AbstractBlock.Settings.copy(Blocks.CAULDRON).emissiveLighting((state, world, pos) -> true));
    public static final Block COLORED_WATER_CAULDRON = new ColoredWaterCauldronBlock(AbstractBlock.Settings.copy(Blocks.CAULDRON));

    public static final BlockEntityType<WaterCauldronBlockEntity> WATER_CAULDRON_ENTITY = BlockEntityType.Builder.create(WaterCauldronBlockEntity::new, POTION_CAULDRON, COLORED_WATER_CAULDRON).build(null);

    public static void register(RegisterEvent event) {
        event.register(net.minecraft.registry.RegistryKeys.BLOCK, helper -> helper.register(new Identifier(Bedrockify.MOD_ID, "potion_cauldron"), POTION_CAULDRON));
        event.register(net.minecraft.registry.RegistryKeys.BLOCK, helper -> helper.register(new Identifier(Bedrockify.MOD_ID, "colored_water_cauldron"), COLORED_WATER_CAULDRON));

        event.register(net.minecraft.registry.RegistryKeys.BLOCK_ENTITY_TYPE, helper -> helper.register(new Identifier(Bedrockify.MOD_ID, "water_cauldron_entity"), WATER_CAULDRON_ENTITY));
    }
}
