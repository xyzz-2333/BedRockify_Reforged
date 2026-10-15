package dev.bedrockify.forge.common.features.education;
import com.google.gson.*;
import java.util.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/** Counted, unordered inputs. Only the server consumes ingredients and publishes real output. */
public final class ChemistryRecipe implements Recipe<Inventory> {
    public static final RecipeType<ChemistryRecipe> TYPE=new RecipeType<>() {};
    public static final RecipeSerializer<ChemistryRecipe> SERIALIZER=new Serializer();
    private final Identifier id;
    public final String station;
    public final Map<Item,Integer> inputs;
    private final ItemStack output;
    public ChemistryRecipe(Identifier id,String station,Map<Item,Integer> inputs,ItemStack output) {
        if(!Set.of("compound_creator","lab_table").contains(station) || inputs.isEmpty() || inputs.size()>9 || output.isEmpty() || output.getCount()>output.getMaxCount()
                || inputs.entrySet().stream().anyMatch(e->e.getKey()==Items.AIR || e.getValue()<1 || e.getValue()>576)) throw new IllegalArgumentException("Invalid chemistry recipe "+id);
        this.id=id;this.station=station;this.inputs=Map.copyOf(inputs);this.output=output.copy();
    }
    @Override public boolean matches(Inventory inventory,World world) {
        Map<Item,Integer> available=new HashMap<>();
        for(int i=0;i<9;i++) {var stack=inventory.getStack(i);if(!stack.isEmpty()) available.merge(stack.getItem(),stack.getCount(),Integer::sum);}
        if(!available.keySet().equals(inputs.keySet()))return false;
        int batches=-1;
        for(var entry:inputs.entrySet()) {
            int count=available.get(entry.getKey()),needed=entry.getValue();
            if(count%needed!=0 || count<needed)return false;
            int ratio=count/needed;if(batches!=-1 && batches!=ratio)return false;batches=ratio;
        }
        return true;
    }
    public void consume(Inventory inventory) {
        for(var entry:inputs.entrySet()) {
            int remaining=entry.getValue();
            for(int i=0;i<9 && remaining>0;i++) if(inventory.getStack(i).isOf(entry.getKey())) {
                int amount=Math.min(remaining,inventory.getStack(i).getCount());inventory.removeStack(i,amount);remaining-=amount;
            }
        }
    }
    @Override public ItemStack craft(Inventory inventory,DynamicRegistryManager registry) {return output.copy();}
    @Override public ItemStack getOutput(DynamicRegistryManager registry) {return output.copy();}
    @Override public boolean fits(int w,int h) {return true;}
    @Override public boolean isIgnoredInRecipeBook() {return true;}
    @Override public Identifier getId() {return id;}
    @Override public RecipeType<?> getType() {return TYPE;}
    @Override public RecipeSerializer<?> getSerializer() {return SERIALIZER;}
    private static final class Serializer implements RecipeSerializer<ChemistryRecipe> {
        @Override public ChemistryRecipe read(Identifier id,JsonObject json) {
            Map<Item,Integer> inputs=new LinkedHashMap<>();
            for(var e:json.getAsJsonObject("inputs").entrySet()) inputs.put(Registries.ITEM.get(new Identifier(e.getKey())),e.getValue().getAsInt());
            var out=json.getAsJsonObject("result");
            return new ChemistryRecipe(id,json.get("station").getAsString(),inputs,new ItemStack(Registries.ITEM.get(new Identifier(out.get("item").getAsString())),out.has("count")?out.get("count").getAsInt():1));
        }
        @Override public ChemistryRecipe read(Identifier id,PacketByteBuf buf) {
            String station=buf.readString();int count=buf.readVarInt();if(count<1 || count>9) throw new IllegalArgumentException("Invalid chemistry input length");
            Map<Item,Integer> inputs=new LinkedHashMap<>();for(int i=0;i<count;i++) inputs.put(Registries.ITEM.get(buf.readIdentifier()),buf.readVarInt());
            return new ChemistryRecipe(id,station,inputs,buf.readItemStack());
        }
        @Override public void write(PacketByteBuf buf,ChemistryRecipe recipe) {
            buf.writeString(recipe.station);buf.writeVarInt(recipe.inputs.size());recipe.inputs.forEach((item,count)->{buf.writeIdentifier(Registries.ITEM.getId(item));buf.writeVarInt(count);});buf.writeItemStack(recipe.output);
        }
    }
}
