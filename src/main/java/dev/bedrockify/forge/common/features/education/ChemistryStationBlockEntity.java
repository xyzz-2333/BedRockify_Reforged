package dev.bedrockify.forge.common.features.education;

import dev.bedrockify.forge.platform.item.ItemStackData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import static dev.bedrockify.forge.common.features.education.StationSlots.*;

public final class ChemistryStationBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
    public final StationAccess access = new StationAccess();
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(STATION_SIZE, ItemStack.EMPTY);
    private final int[] particles = {1, 0, 1};
    private int reaction;
    public final PropertyDelegate properties = new PropertyDelegate() {
        @Override public int size() { return 4; }
        @Override public int get(int index) { return index == 3 ? reaction : particles[index]; }
        @Override public void set(int index, int value) {
            if (index == 3) reaction = value;
            else particles[index] = Math.max(0, Math.min(ChemistryLayout.particleLimit(index), value));
            markDirty();
        }
    };

    public ChemistryStationBlockEntity(BlockPos pos, BlockState state) { super(EducationContent.STATION_ENTITY, pos, state); }
    public String kind() { return ((ChemistryStationBlock) getCachedState().getBlock()).kind; }
    public ChemistryStationKind type() { return ChemistryStationKind.fromId(kind()); }

    /** An ephemeral result, never cached across input changes or datapack reloads. */
    private record Reaction(ItemStack output, ChemistryRecipe recipe, boolean discardInputs) {}

    private Reaction resolveReaction() {
        if (world == null || world.isClient) return null;
        if (type() == ChemistryStationKind.ELEMENT_CONSTRUCTOR) {
            if (particles[0] < 1 || particles[0] != particles[2]) return null;
            ItemStack output = EducationContent.element(particles[0], 1);
            ItemStackData.setNeutrons(output, particles[1]);
            return new Reaction(output, null, false);
        }
        var recipe = world.getRecipeManager().getAllMatches(ChemistryRecipe.TYPE, this, world).stream()
                .filter(candidate -> candidate.station.equals(kind())).findFirst();
        if (recipe.isPresent()) return new Reaction(recipe.get().craft(this, world.getRegistryManager()), recipe.get(), false);
        if (type() != ChemistryStationKind.LAB_TABLE) return null;
        for (int index = 0; index < CHEMISTRY_INPUTS; index++)
            if (!getStack(index).isEmpty()) return new Reaction(new ItemStack(EducationContent.ITEMS.get("garbage")), null, true);
        return null;
    }

    /** Disposable menu preview: no consumption and no fake output in block saves. */
    public ItemStack previewOutput() {
        if (world == null || world.isClient) return ItemStack.EMPTY;
        if (!getStack(CHEMISTRY_OUTPUT).isEmpty()) return getStack(CHEMISTRY_OUTPUT).copy();
        if (type() == ChemistryStationKind.LAB_TABLE) return ItemStack.EMPTY;
        Reaction resolved = resolveReaction();
        return resolved == null ? ItemStack.EMPTY : resolved.output();
    }

    public boolean combine() {
        if (world == null || world.isClient || !getStack(CHEMISTRY_OUTPUT).isEmpty()) return false;
        // Resolve again when claiming, rather than trusting a potentially stale menu preview.
        Reaction resolved = resolveReaction();
        if (resolved == null) return false;
        if (resolved.recipe() != null) resolved.recipe().consume(this);
        if (resolved.discardInputs()) for (int index = 0; index < CHEMISTRY_INPUTS; index++) stacks.set(index, ItemStack.EMPTY);
        stacks.set(CHEMISTRY_OUTPUT, resolved.output());
        markDirty();
        return true;
    }

    public boolean runExperiment() {
        if (type() != ChemistryStationKind.LAB_TABLE || !combine()) return false;
        ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, removeStack(CHEMISTRY_OUTPUT));
        reaction = (reaction + 1) & 32767;
        return true;
    }

    @Override public int size() { return STATION_SIZE; }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return stacks.get(slot); }
    @Override public ItemStack removeStack(int slot, int count) {
        ItemStack removed = Inventories.splitStack(stacks, slot, count);
        if (!removed.isEmpty()) markDirty();
        return removed;
    }
    @Override public ItemStack removeStack(int slot) {
        ItemStack removed = Inventories.removeStack(stacks, slot);
        markDirty();
        return removed;
    }
    @Override public void setStack(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        stack.setCount(Math.min(stack.getCount(), getMaxCountPerStack()));
        markDirty();
    }
    @Override public void clear() { stacks.clear(); markDirty(); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return Inventory.canPlayerUse(this, player); }
    @Override public boolean isValid(int slot, ItemStack stack) { return slot < CHEMISTRY_INPUTS && type().accepts(stack); }
    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        stacks.clear();
        Inventories.readNbt(nbt, stacks);
        for (int index = 0; index < particles.length; index++) properties.set(index, nbt.getInt("Particle" + index));
    }
    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, stacks);
        for (int index = 0; index < particles.length; index++) nbt.putInt("Particle" + index, particles[index]);
    }
    @Override public Text getDisplayName() { return getCachedState().getBlock().getName(); }
    @Override public ScreenHandler createMenu(int id, PlayerInventory inventory, PlayerEntity player) {
        return new ChemistryStationScreenHandler(id, inventory, this, kind());
    }
    public void dropContents() { ItemScatterer.spawn(world, pos, this); clear(); }
}
