package dev.bedrockify.forge.common.features.education;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import static dev.bedrockify.forge.common.features.education.StationSlots.*;

public final class MaterialReducerScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final MaterialReducerBlockEntity reducer;
    private final PropertyDelegate properties;
    public MaterialReducerScreenHandler(int id, PlayerInventory player) { this(id, player, null); }
    public MaterialReducerScreenHandler(int id, PlayerInventory player, MaterialReducerBlockEntity reducer) {
        super(EducationContent.REDUCER_MENU, id);
        this.reducer = reducer;
        inventory = reducer == null ? new SimpleInventory(STATION_SIZE) : reducer.inventory;
        properties = reducer == null ? new ArrayPropertyDelegate(1) : reducer.properties;
        addProperties(properties);
        addSlot(new Slot(inventory, 0, 94, 18) {
            @Override public int getMaxItemCount() { return 1; }
            @Override public boolean canInsert(ItemStack stack) { return !isLocked() && (reducer == null || reducer.accepts(stack)); }
            @Override public boolean canTakeItems(PlayerEntity player) { return !isLocked(); }
        });
        for (int n = 0; n < 9; n++) addSlot(new Slot(inventory, n+1,
                n < 5 ? 42+n*26 : 55+(n-5)*26, n < 5 ? 53 : 78) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity player) {
                if (reducer != null) reducer.refreshPreview();
                return hasStack();
            }
        });
        var layout = ChemistryLayout.REDUCER;
        StationSlots.addPlayerInventory(this::addSlot, player, layout);
    }
    public boolean isLocked() { return properties.get(0) != 0; }
    @Override public void sendContentUpdates() {
        // Recheck after datapack reloads while the sole user's menu is open.
        if (reducer != null) reducer.refreshPreview();
        super.sendContentUpdates();
    }
    @Override public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player) && (reducer == null || reducer.access.allows(player,this)); }
    @Override public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (reducer != null) reducer.access.closed(player, this);
    }
    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        if (index > 0 && index < PLAYER_START && reducer != null) reducer.refreshPreview();
        Slot slot = slots.get(index);
        if (!slot.hasStack() || !slot.canTakeItems(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack before = stack.copy();
        if (index < PLAYER_START) {
            if (!insertItem(stack, PLAYER_START, END, true)) return ItemStack.EMPTY;
            if (index > 0 && reducer != null) reducer.commit();
        } else if (slots.get(0).canInsert(stack) && !slots.get(0).hasStack()) {
            if (!insertItem(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < HOTBAR_START) {
            if (!insertItem(stack, HOTBAR_START, END, false)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, PLAYER_START, HOTBAR_START, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        if (index == 0 && reducer != null) reducer.refreshPreview();
        slot.onTakeItem(player, stack);
        if (reducer != null) reducer.outputChanged();
        return before;
    }
}
