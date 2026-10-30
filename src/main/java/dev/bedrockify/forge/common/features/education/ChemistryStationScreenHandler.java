package dev.bedrockify.forge.common.features.education;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import static dev.bedrockify.forge.common.features.education.StationSlots.*;

public final class ChemistryStationScreenHandler extends ScreenHandler {
    public final String kind;
    public final ChemistryStationKind type;
    public final ChemistryLayout layout;
    private final Inventory inventory;
    private final ChemistryStationBlockEntity station;
    private final PropertyDelegate properties;
    private final SimpleInventory result = new SimpleInventory(1);

    public ChemistryStationScreenHandler(int id, PlayerInventory player, String kind) { this(id, player, null, kind); }
    public ChemistryStationScreenHandler(int id, PlayerInventory player, ChemistryStationBlockEntity station, String kind) {
        super(EducationContent.STATION_MENU, id);
        this.station = station;
        this.kind = kind;
        type = ChemistryStationKind.fromId(kind);
        layout = type.layout();
        inventory = station == null ? new SimpleInventory(STATION_SIZE) : station;
        properties = station == null ? new ArrayPropertyDelegate(4) : station.properties;
        addProperties(properties);
        for (int index = 0; index < CHEMISTRY_INPUTS; index++) addSlot(new Slot(inventory, index,
                layout.inputSlotX(index), layout.inputSlotY(index)) {
            @Override public boolean canInsert(ItemStack stack) { return type.accepts(stack); }
            @Override public boolean isEnabled() { return type != ChemistryStationKind.ELEMENT_CONSTRUCTOR; }
        });
        addSlot(new OutputSlot());
        StationSlots.addPlayerInventory(this::addSlot, player, layout);
        refreshResult();
    }

    private final class OutputSlot extends Slot {
        private OutputSlot() { super(result, 0, layout.outputX(), layout.outputY()); }
        @Override public boolean canInsert(ItemStack stack) { return false; }
        @Override public boolean canTakeItems(PlayerEntity player) { refreshResult(); return canUse(player) && hasStack(); }
        @Override public boolean isEnabled() { return type != ChemistryStationKind.LAB_TABLE || hasStack(); }
        @Override public void onTakeItem(PlayerEntity player, ItemStack taken) {
            claimOutput(taken.getCount());
            super.onTakeItem(player, taken);
        }
    }

    private void refreshResult() {
        if (station == null) return;
        ItemStack preview = station.previewOutput();
        if (!ItemStack.areEqual(result.getStack(0), preview)) result.setStack(0, preview);
    }
    /** Materialize once, then remove only the quantity transferred by the native slot transaction. */
    private void claimOutput(int count) {
        if (station == null || count <= 0) return;
        if (station.getStack(CHEMISTRY_OUTPUT).isEmpty() && !station.combine())
            throw new IllegalStateException("Chemistry result became invalid during its slot transaction");
        station.removeStack(CHEMISTRY_OUTPUT, count);
    }
    @Override public void sendContentUpdates() { refreshResult(); super.sendContentUpdates(); }
    public int particle(int index) { return properties.get(index); }
    public int reaction() { return properties.get(3); }
    public void displayParticle(int index, int value) { properties.set(index, value); }
    public boolean canCombine() {
        if (type != ChemistryStationKind.LAB_TABLE || slots.get(CHEMISTRY_OUTPUT).hasStack()) return false;
        for (int index = 0; index < CHEMISTRY_INPUTS; index++) if (slots.get(index).hasStack()) return true;
        return false;
    }
    @Override public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player) && (station == null || station.access.allows(player, this));
    }
    @Override public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (station != null) station.access.closed(player, this);
    }
    @Override public void onSlotClick(int index, int button, SlotActionType action, PlayerEntity player) {
        // A constructor has unlimited preview output; one Shift-click requests one batch.
        if (index == CHEMISTRY_OUTPUT && action == SlotActionType.QUICK_MOVE) quickMove(player, index);
        else super.onSlotClick(index, button, action, player);
    }
    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        if (station == null || !canUse(player)) return false;
        if (id == 0) {
            boolean combined = type == ChemistryStationKind.LAB_TABLE ? station.runExperiment() : station.combine();
            refreshResult();
            return combined;
        }
        if (type != ChemistryStationKind.ELEMENT_CONSTRUCTOR) return false;
        if (id >= 16 && id < 16 + 3 * 256) {
            int index = (id - 16) / 256, value = (id - 16) % 256;
            if (value > ChemistryLayout.particleLimit(index)) return false;
            properties.set(index, value);
        } else {
            if (id < 1 || id > 6) return false;
            int index = (id - 1) / 2;
            properties.set(index, properties.get(index) + (id % 2 == 0 ? 1 : -1));
        }
        refreshResult();
        return true;
    }
    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        if (index < 0 || index >= slots.size() || !canUse(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.canTakeItems(player) || !slot.hasStack() || !slot.isEnabled()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), before = stack.copy();
        if (index < PLAYER_START) {
            if (!insertItem(stack, PLAYER_START, END, true)) return ItemStack.EMPTY;
        } else if (type != ChemistryStationKind.ELEMENT_CONSTRUCTOR && slots.get(0).canInsert(stack)) {
            if (!insertItem(stack, 0, CHEMISTRY_INPUTS, false)) return ItemStack.EMPTY;
        } else if (index < HOTBAR_START) {
            if (!insertItem(stack, HOTBAR_START, END, false)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, PLAYER_START, HOTBAR_START, false)) return ItemStack.EMPTY;
        int transferred = before.getCount() - stack.getCount();
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        // Shift transfer already mutated the preview stack; charge the actual moved quantity.
        if (index == CHEMISTRY_OUTPUT) claimOutput(transferred); else slot.onTakeItem(player, stack);
        return before;
    }
}
