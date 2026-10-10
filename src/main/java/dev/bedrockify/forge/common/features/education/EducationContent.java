package dev.bedrockify.forge.common.features.education;

import dev.bedrockify.forge.Bedrockify;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegisterEvent;
import org.joml.Vector3f;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EducationContent {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Block[] ELEMENTS = new Block[119];
    public static final MaterialReducerBlock MATERIAL_REDUCER;
    public static final BlockEntityType<MaterialReducerBlockEntity> REDUCER_ENTITY;
    public static final ScreenHandlerType<MaterialReducerScreenHandler> REDUCER_MENU;
    public static final RecipeType<MaterialReducingRecipe> REDUCING = new RecipeType<>() {
        @Override public String toString() { return "bedrockify:material_reducing"; }
    };
    static {
        for (int n = 0; n < ELEMENTS.length; n++) {
            Block block = new Block(AbstractBlock.Settings.copy(Blocks.STONE).strength(0.0F, 0.0F));
            ELEMENTS[n] = block;
            block("element_" + n, block, new ElementBlockItem(block, n));
        }
        MATERIAL_REDUCER = new MaterialReducerBlock(AbstractBlock.Settings.copy(Blocks.STONE).strength(2.5F, 2.5F));
        block("material_reducer", MATERIAL_REDUCER, new BlockItem(MATERIAL_REDUCER, new Item.Settings()));
        REDUCER_ENTITY = BlockEntityType.Builder.create(MaterialReducerBlockEntity::new, MATERIAL_REDUCER).build(null);
        REDUCER_MENU = IForgeMenuType.create((id, inventory, buffer) -> new MaterialReducerScreenHandler(id, inventory));
        torch("underwater_torch", ParticleTypes.FLAME, true);
        torch("blue_torch", dust(0x4385ef), false);
        torch("red_torch", dust(0xef493c), false);
        torch("purple_torch", dust(0xb361ed), false);
        torch("green_torch", dust(0x63d245), false);
        for (String name : new String[]{"cerium_chloride", "mercuric_chloride", "potassium_chloride", "tungsten_chloride"})
            ITEMS.put(name, new Item(new Item.Settings()));
    }
    private EducationContent() {}
    private static ParticleEffect dust(int color) {
        return new DustParticleEffect(new Vector3f((color >> 16 & 255)/255F, (color >> 8 & 255)/255F, (color & 255)/255F), 1.0F);
    }
    private static void block(String name, Block block, Item item) { BLOCKS.put(name, block); ITEMS.put(name, item); }
    private static void torch(String name, ParticleEffect particle, boolean underwater) {
        AbstractBlock.Settings settings = AbstractBlock.Settings.copy(Blocks.TORCH).luminance(state -> 14);
        Block standing = underwater ? new UnderwaterTorchBlock(settings) : new TorchBlock(settings, particle);
        Block wall = underwater ? new UnderwaterWallTorchBlock(AbstractBlock.Settings.copy(Blocks.WALL_TORCH))
                : new WallTorchBlock(AbstractBlock.Settings.copy(Blocks.WALL_TORCH), particle);
        BLOCKS.put(name, standing);
        BLOCKS.put(name.replace("_torch", "_wall_torch"), wall);
        ITEMS.put(name, new VerticallyAttachableBlockItem(standing, wall, new Item.Settings(), Direction.DOWN));
    }
    public static ItemStack element(int number, int count) { return new ItemStack(ELEMENTS[number], count); }
    public static void register(RegisterEvent event) {
        event.register(RegistryKeys.BLOCK, helper -> BLOCKS.forEach((name, block) -> helper.register(id(name), block)));
        event.register(RegistryKeys.ITEM, helper -> ITEMS.forEach((name, item) -> helper.register(id(name), item)));
        event.register(RegistryKeys.BLOCK_ENTITY_TYPE, helper -> helper.register(id("material_reducer"), REDUCER_ENTITY));
        event.register(RegistryKeys.SCREEN_HANDLER, helper -> helper.register(id("material_reducer"), REDUCER_MENU));
        event.register(RegistryKeys.RECIPE_TYPE, helper -> helper.register(id("material_reducing"), REDUCING));
        event.register(RegistryKeys.RECIPE_SERIALIZER, helper -> helper.register(id("material_reducing"), MaterialReducingRecipe.SERIALIZER));
    }
    public static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ItemGroups.INGREDIENTS))
            ITEMS.forEach((name, item) -> { if (name.startsWith("element_") || name.endsWith("_chloride")) event.accept(() -> item); });
        if (event.getTabKey().equals(ItemGroups.FUNCTIONAL))
            ITEMS.forEach((name, item) -> { if (name.equals("material_reducer") || name.endsWith("_torch")) event.accept(() -> item); });
    }
    private static Identifier id(String name) { return new Identifier(Bedrockify.MOD_ID, name); }
}
