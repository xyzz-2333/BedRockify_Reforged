package dev.bedrockify.forge.platform.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Item data operations for the 1.20.1 backend. Callers must not depend on raw NBT:
 * future component-based backends must preserve the same observable semantics.
 * This does not handle block/entity NBT or serialize whole stacks.
 */
public final class ItemStackData {
    // Keep the legacy spelling so existing isotope stacks remain readable.
    private static final String NEUTRONS = "Neutrons";

    private ItemStackData() {}

    public static OptionalInt neutrons(ItemStack stack) {
        NbtCompound data = stack.getNbt();
        return data != null && data.contains(NEUTRONS)
                ? OptionalInt.of(data.getInt(NEUTRONS)) : OptionalInt.empty();
    }

    public static void setNeutrons(ItemStack stack, int neutrons) {
        stack.getOrCreateNbt().putInt(NEUTRONS, neutrons);
    }

    /** Copies all stack data, including other mods' tags, without sharing mutable values. */
    public static void copyData(ItemStack source, ItemStack target) {
        NbtCompound data = source.getNbt();
        target.setNbt(data == null ? null : data.copy());
    }

    public static boolean matches(ItemStack stack, Key key) {
        return key != null && stack.getItem() == key.item && Objects.equals(stack.getNbt(), key.data);
    }

    public static Key key(ItemStack stack) {
        return new Key(stack);
    }

    /**
     * Immutable, count-independent snapshot for catalog deduplication. In a future
     * backend this must include all components, not only minecraft:custom_data.
     * The backing data is deliberately private so callers cannot mutate a set key.
     */
    public static final class Key {
        private final Item item;
        private final NbtCompound data;

        private Key(ItemStack stack) {
            item = stack.getItem();
            NbtCompound tag = stack.getNbt();
            data = tag == null ? null : tag.copy();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof Key key
                    && item == key.item && Objects.equals(data, key.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, data);
        }
    }
}
