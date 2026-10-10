package dev.bedrockify.forge.common.features.education;

import com.google.gson.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.List;

/** Exact per-item data, not a scan of crafting recipes or a chemical guess from item names. */
public final class MaterialReducingRecipe implements Recipe<Inventory> {
    public static final RecipeSerializer<MaterialReducingRecipe> SERIALIZER = new Serializer();
    public record Component(int element, int count) {
        public Component { if (element < 0 || element > 118 || count < 1 || count > 100) throw new IllegalArgumentException("Invalid material reducer component"); }
    }
    private final Identifier id;
    private final Ingredient input;
    private final List<Component> components;
    public MaterialReducingRecipe(Identifier id, Ingredient input, List<Component> components) {
        this.id = id; this.input = input; this.components = List.copyOf(components);
        int slots = components.stream().mapToInt(c -> (c.count()+63)/64).sum();
        if (components.isEmpty() || slots > 9) throw new IllegalArgumentException("Material reducer requires 1..9 output stacks");
    }
    public List<ItemStack> outputs() {
        List<ItemStack> result = new ArrayList<>();
        for (Component component : components) for (int left = component.count(); left > 0; left -= 64)
            result.add(EducationContent.element(component.element(), Math.min(64, left)));
        return result;
    }
    @Override public boolean matches(Inventory inventory, World world) { return input.test(inventory.getStack(0)); }
    @Override public ItemStack craft(Inventory inventory, DynamicRegistryManager registry) { return getOutput(registry); }
    @Override public boolean fits(int width, int height) { return width * height >= 1; }
    @Override public ItemStack getOutput(DynamicRegistryManager registry) { return EducationContent.element(components.get(0).element(), Math.min(64, components.get(0).count())); }
    @Override public DefaultedList<Ingredient> getIngredients() { return DefaultedList.copyOf(Ingredient.EMPTY, input); }
    @Override public boolean isIgnoredInRecipeBook() { return true; }
    @Override public Identifier getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<?> getType() { return EducationContent.REDUCING; }
    private static final class Serializer implements RecipeSerializer<MaterialReducingRecipe> {
        @Override public MaterialReducingRecipe read(Identifier id, JsonObject json) {
            List<Component> components = new ArrayList<>();
            for (JsonElement value : json.getAsJsonArray("elements")) {
                JsonObject component = value.getAsJsonObject();
                components.add(new Component(component.get("element").getAsInt(), component.get("count").getAsInt()));
            }
            return new MaterialReducingRecipe(id, Ingredient.fromJson(json.get("ingredient")), components);
        }
        @Override public MaterialReducingRecipe read(Identifier id, PacketByteBuf buffer) {
            Ingredient input = Ingredient.fromPacket(buffer);
            int length = buffer.readVarInt();
            if (length < 1 || length > 9) throw new IllegalArgumentException("Invalid material reducer component count");
            List<Component> components = new ArrayList<>();
            for (int n = 0; n < length; n++) components.add(new Component(buffer.readVarInt(), buffer.readVarInt()));
            return new MaterialReducingRecipe(id, input, components);
        }
        @Override public void write(PacketByteBuf buffer, MaterialReducingRecipe recipe) {
            recipe.input.write(buffer); buffer.writeVarInt(recipe.components.size());
            for (Component component : recipe.components) { buffer.writeVarInt(component.element()); buffer.writeVarInt(component.count()); }
        }
    }
}
