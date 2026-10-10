package dev.bedrockify.forge.common.features.education;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/** Unclaimed preview stacks are never saved or dropped as owned items. */
public final class MaterialReducerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(10, ItemStack.EMPTY);
    private boolean committed;
    private MaterialReducingRecipe previewRecipe;
    public final Inventory inventory = new ReducerInventory();
    public final PropertyDelegate properties = new PropertyDelegate() {
        @Override public int get(int index) { return committed ? 1 : 0; }
        @Override public void set(int index, int value) {}
        @Override public int size() { return 1; }
    };
    public MaterialReducerBlockEntity(BlockPos pos, BlockState state) { super(EducationContent.REDUCER_ENTITY, pos, state); }
    public boolean isCommitted() { return committed; }
    public boolean accepts(ItemStack stack) {
        return !committed && world != null && !stack.isEmpty()
                && world.getRecipeManager().getFirstMatch(EducationContent.REDUCING, new SimpleInventory(stack), world).isPresent();
    }
    public void refreshPreview() {
        if (committed || world == null || world.isClient) return;
        MaterialReducingRecipe recipe = stacks.get(0).isEmpty() ? null : world.getRecipeManager()
                .getFirstMatch(EducationContent.REDUCING, new SimpleInventory(stacks.get(0)), world).orElse(null);
        if (recipe == previewRecipe) return;
        previewRecipe = recipe;
        for (int n = 1; n < 10; n++) stacks.set(n, ItemStack.EMPTY);
        if (recipe != null) { var outputs = recipe.outputs(); for (int n = 0; n < outputs.size(); n++) stacks.set(n+1, outputs.get(n)); }
    }
    public void commit() {
        if (committed) return;
        // A bucket contributes its contents; return the empty bucket rather than destroying it.
        ItemStack input = stacks.get(0);
        if (!input.isEmpty() && input.getItem().hasRecipeRemainder() && world != null)
            ItemScatterer.spawn(world, pos.getX()+0.5, pos.getY()+1.0, pos.getZ()+0.5, new ItemStack(input.getItem().getRecipeRemainder()));
        stacks.set(0, ItemStack.EMPTY);
        committed = true;
        previewRecipe = null;
        markDirty();
    }
    public void outputChanged() {
        boolean empty = true;
        for (int n = 1; n < 10; n++) if (!stacks.get(n).isEmpty()) { empty = false; break; }
        if (empty && committed) committed = false;
        markDirty();
    }
    public void dropContents() {
        if (world == null || world.isClient) return;
        ItemScatterer.spawn(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stacks.get(0));
        if (committed) for (int n = 1; n < 10; n++) ItemScatterer.spawn(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stacks.get(n));
        stacks.clear(); committed = false;
    }
    @Override public Text getDisplayName() { return Text.translatable("block.bedrockify.material_reducer"); }
    @Override public ScreenHandler createMenu(int id, PlayerInventory playerInventory, PlayerEntity player) {
        refreshPreview();
        return new MaterialReducerScreenHandler(id, playerInventory, this);
    }
    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        DefaultedList<ItemStack> saved = DefaultedList.ofSize(10, ItemStack.EMPTY);
        saved.set(0, stacks.get(0));
        if (committed) for (int n = 1; n < 10; n++) saved.set(n, stacks.get(n));
        Inventories.writeNbt(nbt, saved);
        nbt.putBoolean("Committed", committed);
    }
    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt); stacks.clear(); previewRecipe = null; Inventories.readNbt(nbt, stacks);
        committed = nbt.getBoolean("Committed");
        if (committed) stacks.set(0, ItemStack.EMPTY);
        else for (int n = 1; n < 10; n++) stacks.set(n, ItemStack.EMPTY);
    }
    // Keep this workbench's inventory behind its menu. No implicit hopper extraction of preview items.
    private final class ReducerInventory implements Inventory {
        @Override public int size() { return 10; }
        @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getStack(int slot) { return stacks.get(slot); }
        @Override public ItemStack removeStack(int slot, int count) {
            if (count <= 0 || stacks.get(slot).isEmpty() || slot == 0 && committed) return ItemStack.EMPTY;
            if (slot > 0) commit();
            ItemStack result = Inventories.splitStack(stacks, slot, count);
            if (slot == 0) refreshPreview(); else outputChanged();
            MaterialReducerBlockEntity.this.markDirty(); return result;
        }
        @Override public ItemStack removeStack(int slot) { return removeStack(slot, stacks.get(slot).getCount()); }
        @Override public void setStack(int slot, ItemStack stack) {
            if (slot == 0 && committed) return;
            stacks.set(slot, stack);
            if (slot == 0) refreshPreview(); else outputChanged();
            MaterialReducerBlockEntity.this.markDirty();
        }
        @Override public void markDirty() { outputChanged(); }
        @Override public boolean canPlayerUse(PlayerEntity player) { return Inventory.canPlayerUse(MaterialReducerBlockEntity.this, player); }
        @Override public boolean isValid(int slot, ItemStack stack) { return slot == 0 && accepts(stack); }
        @Override public void clear() { stacks.clear(); committed = false; MaterialReducerBlockEntity.this.markDirty(); }
    }
}
