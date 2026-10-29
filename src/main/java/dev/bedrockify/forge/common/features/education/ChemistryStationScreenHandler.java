package dev.bedrockify.forge.common.features.education;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public final class ChemistryStationScreenHandler extends ScreenHandler {
    public final String kind;
    public final ChemistryLayout layout;
    private final Inventory inventory;
    private final ChemistryStationBlockEntity station;
    private final PropertyDelegate properties;
    private final SimpleInventory result = new SimpleInventory(1);
    public ChemistryStationScreenHandler(int id, PlayerInventory player, String kind) { this(id, player, null, kind); }
    public ChemistryStationScreenHandler(int id, PlayerInventory player, ChemistryStationBlockEntity station, String kind) {
        super(EducationContent.STATION_MENU, id);
        this.station = station; this.kind = kind; layout = ChemistryLayout.of(kind);
        inventory = station == null ? new SimpleInventory(10) : station;
        properties = station == null ? new ArrayPropertyDelegate(4) : station.properties;
        addProperties(properties);
        for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i,
                kind.equals("lab_table") ? 5+i*18 : 54+i%3*18,
                kind.equals("lab_table") ? 69 : 25+i/3*18) {
            @Override public boolean canInsert(ItemStack stack) {
                return !kind.equals("element_constructor") && (!kind.equals("compound_creator") || stack.getItem() instanceof ElementBlockItem);
            }
            @Override public boolean isEnabled() { return !kind.equals("element_constructor"); }
        });
        int outputX = kind.equals("element_constructor") ? 212 : kind.equals("compound_creator") ? 138 : 80;
        int outputY = kind.equals("element_constructor") ? 90 : kind.equals("compound_creator") ? 43 : 40;
        addSlot(new Slot(result, 0, outputX, outputY) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity player) { refreshResult(); return canUse(player) && hasStack(); }
            @Override public boolean isEnabled() { return !kind.equals("lab_table") || hasStack(); }
            @Override public void onTakeItem(PlayerEntity player, ItemStack taken) { claim(taken.getCount()); super.onTakeItem(player, taken); }
        });
        for (int row=0; row<3; row++) for (int col=0; col<9; col++)
            addSlot(new Slot(player, col+row*9+9, layout.inventoryX()+col*18, layout.inventoryY()+row*18));
        for (int col=0; col<9; col++) addSlot(new Slot(player, col, layout.inventoryX()+col*18, layout.hotbarY()));
        refreshResult();
    }
    private void refreshResult() {
        if (station == null) return;
        ItemStack preview=station.previewOutput();
        if (!ItemStack.areEqual(result.getStack(0),preview)) result.setStack(0,preview);
    }
    /** Materialize once, then remove only the quantity transferred by the native slot transaction. */
    private void claim(int count) {
        if (station == null || count <= 0) return;
        if (station.getStack(9).isEmpty() && !station.combine()) throw new IllegalStateException("Chemistry result became invalid during its slot transaction");
        station.removeStack(9, count);
    }
    @Override public void sendContentUpdates() { refreshResult(); super.sendContentUpdates(); }
    public int particle(int n) { return properties.get(n); }
    public int reaction() { return properties.get(3); }
    public void displayParticle(int n, int value) { properties.set(n, value); }
    public boolean canCombine() {
        if (!kind.equals("lab_table") || slots.get(9).hasStack()) return false;
        for (int i=0; i<9; i++) if (slots.get(i).hasStack()) return true;
        return false;
    }
    @Override public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player) && (station == null || station.access.allows(player,this)); }
    @Override public void onClosed(PlayerEntity player) { super.onClosed(player); if (station != null) station.access.closed(player, this); }
    @Override public void onSlotClick(int index,int button,SlotActionType action,PlayerEntity player) {
        // A constructor has unlimited preview output; one Shift-click requests one batch.
        if(index==9 && action==SlotActionType.QUICK_MOVE)quickMove(player,index);
        else super.onSlotClick(index,button,action,player);
    }
    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        if (station == null || !canUse(player)) return false;
        if (id == 0) { boolean combined = kind.equals("lab_table") ? station.runExperiment() : station.combine(); refreshResult(); return combined; }
        if (!kind.equals("element_constructor")) return false;
        if (id >= 16 && id < 16+3*256) {
            int n = (id-16)/256, value = (id-16)%256;
            if (value > ChemistryLayout.particleLimit(n)) return false;
            properties.set(n, value); refreshResult(); return true;
        }
        if (id < 1 || id > 6) return false;
        int n = (id-1)/2;
        properties.set(n, properties.get(n)+(id%2==0 ? 1 : -1)); refreshResult(); return true;
    }
    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        if (index < 0 || index >= slots.size() || !canUse(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.canTakeItems(player) || !slot.hasStack() || !slot.isEnabled()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), before = stack.copy();
        if (index < 10) { if (!insertItem(stack, 10, 46, true)) return ItemStack.EMPTY; }
        else if (!kind.equals("element_constructor") && slots.get(0).canInsert(stack)) {
            if (!insertItem(stack, 0, 9, false)) return ItemStack.EMPTY;
        } else if (index < 37) { if (!insertItem(stack, 37, 46, false)) return ItemStack.EMPTY; }
        else if (!insertItem(stack, 10, 37, false)) return ItemStack.EMPTY;
        int transferred = before.getCount()-stack.getCount();
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        if (index == 9) claim(transferred); else slot.onTakeItem(player, stack);
        return before;
    }
}
