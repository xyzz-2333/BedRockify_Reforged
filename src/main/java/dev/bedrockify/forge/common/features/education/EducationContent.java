package dev.bedrockify.forge.common.features.education;

import dev.bedrockify.forge.Bedrockify;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.util.DyeColor;
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
    public static final Map<String,Item> COMPOUNDS=new LinkedHashMap<>();
    public static final EntityType<IceBombEntity> ICE_BOMB_ENTITY;
    public static final BlockEntityType<ChemistryStationBlockEntity> STATION_ENTITY;
    public static final ScreenHandlerType<ChemistryStationScreenHandler> STATION_MENU;
    public static final Block UNDERWATER_TNT;
    public static final EntityType<UnderwaterTntEntity> UNDERWATER_TNT_ENTITY;
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
        for (int color=-1;color<16;color++) {
            DyeColor dye=color<0?null:DyeColor.byId(color);
            String name=dye==null?"hardened_glass":dye.getName()+"_hardened_glass";
            var settings=AbstractBlock.Settings.copy(Blocks.GLASS).strength(10F,9F);
            Block glass=dye==null?new GlassBlock(settings):new StainedGlassBlock(dye,settings);
            Block pane=dye==null?new PaneBlock(AbstractBlock.Settings.copy(Blocks.GLASS_PANE).strength(10F,9F))
                    :new StainedGlassPaneBlock(dye,AbstractBlock.Settings.copy(Blocks.GLASS_PANE).strength(10F,9F));
            block(name,glass,new BlockItem(glass,new Item.Settings()));
            block(name+"_pane",pane,new BlockItem(pane,new Item.Settings()));
        }
        UNDERWATER_TNT=new UnderwaterTntBlock(AbstractBlock.Settings.copy(Blocks.TNT));
        block("underwater_tnt",UNDERWATER_TNT,new BlockItem(UNDERWATER_TNT,new Item.Settings()));
        UNDERWATER_TNT_ENTITY=EntityType.Builder.<UnderwaterTntEntity>create(UnderwaterTntEntity::new,SpawnGroup.MISC)
                .setDimensions(0.98F,0.98F).maxTrackingRange(10).trackingTickInterval(10).build("bedrockify:underwater_tnt");
        MATERIAL_REDUCER = new MaterialReducerBlock(AbstractBlock.Settings.copy(Blocks.STONE).strength(2.5F, 2.5F));
        block("material_reducer", MATERIAL_REDUCER, new BlockItem(MATERIAL_REDUCER, new Item.Settings()));
        REDUCER_ENTITY = BlockEntityType.Builder.create(MaterialReducerBlockEntity::new, MATERIAL_REDUCER).build(null);
        REDUCER_MENU = IForgeMenuType.create((id, inventory, buffer) -> new MaterialReducerScreenHandler(id, inventory));
        torch("underwater_torch", UnderwaterTorchBlock.WHITE_PARTICLE, true);
        torch("blue_torch", dust(0x4385ef), false);
        torch("red_torch", dust(0xef493c), false);
        torch("purple_torch", dust(0xb361ed), false);
        torch("green_torch", dust(0x63d245), false);
        for (String name : new String[]{"cerium_chloride", "mercuric_chloride", "potassium_chloride", "tungsten_chloride"})
            ITEMS.put(name, new Item(new Item.Settings()));
        for(String name:new String[]{"salt","sodium_oxide","sodium_hydroxide","magnesium_nitrate","iron_sulfide","lithium_hydride","sodium_hydride","calcium_bromide","magnesium_oxide","sodium_acetate","luminol","charcoal_compound","sugar_compound","aluminum_oxide","boron_trioxide","soap","polyethylene","magnesium_salts","sulfate","barium_sulfate","potassium_chloride","mercuric_chloride","cerium_chloride","tungsten_chloride","calcium_chloride","water_compound","glue","hypochlorite","crude_oil","latex","potassium_iodide","sodium_fluoride","benzene","ink_compound","hydrogen_peroxide","ammonia","sodium_hypochlorite"}) {
            Item item=ITEMS.computeIfAbsent(name,key->new Item(new Item.Settings()));COMPOUNDS.put(name,item);
        }
        ITEMS.put("garbage",new Item(new Item.Settings()));ITEMS.put("bleach",new Item(new Item.Settings()));
        ITEMS.put("super_fertilizer",new SuperFertilizerItem(new Item.Settings()));ITEMS.put("ice_bomb",new IceBombItem(new Item.Settings().maxCount(16)));
        ICE_BOMB_ENTITY=EntityType.Builder.<IceBombEntity>create(IceBombEntity::new,SpawnGroup.MISC).setDimensions(0.25F,0.25F).maxTrackingRange(4).trackingTickInterval(10).build("bedrockify:ice_bomb");
        Block heat=new HeatBlock(AbstractBlock.Settings.copy(Blocks.STONE).strength(2.5F));block("heat_block",heat,new BlockItem(heat,new Item.Settings()));
        Block[] stations=new Block[3];int index=0;
        for(String name:new String[]{"element_constructor","compound_creator","lab_table"}) {
            Block station=new ChemistryStationBlock(name,AbstractBlock.Settings.copy(Blocks.STONE).strength(2.5F));stations[index++]=station;block(name,station,new BlockItem(station,new Item.Settings()));
        }
        STATION_ENTITY=BlockEntityType.Builder.create(ChemistryStationBlockEntity::new,stations).build(null);
        STATION_MENU=IForgeMenuType.create((id,inventory,buffer)->new ChemistryStationScreenHandler(id,inventory,buffer.readString()));
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
        event.register(RegistryKeys.BLOCK_ENTITY_TYPE,helper->helper.register(id("chemistry_station"),STATION_ENTITY));
        event.register(RegistryKeys.SCREEN_HANDLER,helper->helper.register(id("chemistry_station"),STATION_MENU));
        event.register(RegistryKeys.RECIPE_TYPE,helper->helper.register(id("chemistry"),ChemistryRecipe.TYPE));
        event.register(RegistryKeys.RECIPE_SERIALIZER,helper->helper.register(id("chemistry"),ChemistryRecipe.SERIALIZER));
        event.register(RegistryKeys.ENTITY_TYPE,helper->helper.register(id("ice_bomb"),ICE_BOMB_ENTITY));
        event.register(RegistryKeys.ENTITY_TYPE, helper -> helper.register(id("underwater_tnt"), UNDERWATER_TNT_ENTITY));
        event.register(RegistryKeys.BLOCK, helper -> BLOCKS.forEach((name, block) -> helper.register(id(name), block)));
        event.register(RegistryKeys.ITEM, helper -> ITEMS.forEach((name, item) -> helper.register(id(name), item)));
        event.register(RegistryKeys.BLOCK_ENTITY_TYPE, helper -> helper.register(id("material_reducer"), REDUCER_ENTITY));
        event.register(RegistryKeys.SCREEN_HANDLER, helper -> helper.register(id("material_reducer"), REDUCER_MENU));
        event.register(RegistryKeys.RECIPE_TYPE, helper -> helper.register(id("material_reducing"), REDUCING));
        event.register(RegistryKeys.RECIPE_SERIALIZER, helper -> helper.register(id("material_reducing"), MaterialReducingRecipe.SERIALIZER));
    }
    public static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ItemGroups.INGREDIENTS))
            ITEMS.forEach((name, item) -> { if (item instanceof ElementBlockItem || COMPOUNDS.containsKey(name) || java.util.Set.of("garbage","bleach","super_fertilizer","ice_bomb").contains(name)) event.accept(() -> item); });
        if (event.getTabKey().equals(ItemGroups.BUILDING_BLOCKS))
            ITEMS.forEach((name,item)-> { if(name.contains("hardened_glass")) event.accept(()->item); });
        if (event.getTabKey().equals(ItemGroups.FUNCTIONAL))
            ITEMS.forEach((name, item) -> { if (BLOCKS.get(name) instanceof ChemistryStationBlock || name.equals("heat_block") || name.equals("underwater_tnt") || name.equals("material_reducer") || name.endsWith("_torch")) event.accept(() -> item); });
    }
    private static Identifier id(String name) { return new Identifier(Bedrockify.MOD_ID, name); }
}
