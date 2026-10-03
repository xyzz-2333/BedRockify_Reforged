package me.juancarloscp52.bedrockify.mixin.client.features.creativeInventory;

import me.juancarloscp52.bedrockify.client.features.creativeInventory.CreativeGrid;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeInventoryScreen.CreativeScreenHandler.class)
public abstract class CreativeScreenHandlerMixin extends ScreenHandler implements CreativeGrid {
    @Shadow public DefaultedList<ItemStack> itemList;
    @Unique private PlayerInventory bedrockify$inventory;
    @Unique private boolean bedrockify$classic;
    @Unique private int bedrockify$columns = 9;
    @Unique private int bedrockify$rows = 5;

    protected CreativeScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) { super(type, syncId); }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bedrockify$rememberInventory(PlayerEntity player, CallbackInfo ci) { bedrockify$inventory = player.getInventory(); }

    @Override
    public void bedrockify$configureGrid(boolean classic, int columns, int rows, boolean rebuild) {
        boolean changed = bedrockify$classic != classic || bedrockify$columns != columns || bedrockify$rows != rows;
        bedrockify$classic = classic;
        bedrockify$columns = columns;
        bedrockify$rows = rows;
        if (!rebuild || (!changed && slots.size() == columns * rows + 9)) return;
        slots.clear();
        trackedStacks.clear();
        previousTrackedStacks.clear();
        for (int row = 0; row < rows; row++) for (int col = 0; col < columns; col++) {
            addSlot(new CreativeInventoryScreen.LockableSlot(CreativeInventoryScreen.INVENTORY, row * columns + col,
                    classic ? 14 + col * 20 : 9 + col * 18, classic ? 30 + row * 20 : 18 + row * 18));
        }
        for (int i = 0; i < 9; i++) {
            int hotbarX = (columns * 20 + 32 - 180) / 2 + 2;
            addSlot(new Slot(bedrockify$inventory, i, classic ? hotbarX + i * 20 : 9 + i * 18,
                    classic ? 44 + rows * 20 : 112));
        }
    }

    @Override public int bedrockify$pickerSize() { return bedrockify$columns * bedrockify$rows; }
    @Override public int bedrockify$firstVisibleIndex(float scroll) {
        return Math.max(0, Math.round(scroll * bedrockify$overflow())) * bedrockify$columns;
    }
    @Unique private int bedrockify$overflow() { return Math.max(0, MathHelper.ceilDiv(itemList.size(), bedrockify$columns) - bedrockify$rows); }

    @Inject(method = "getOverflowRows", at = @At("HEAD"), cancellable = true)
    private void bedrockify$overflow(CallbackInfoReturnable<Integer> cir) {
        if (bedrockify$classic) cir.setReturnValue(bedrockify$overflow());
    }

    @Inject(method = "getScrollPosition(I)F", at = @At("HEAD"), cancellable = true)
    private void bedrockify$finiteScroll(int row, CallbackInfoReturnable<Float> cir) {
        if (bedrockify$classic) cir.setReturnValue(bedrockify$overflow() == 0 ? 0 : MathHelper.clamp((float) row / bedrockify$overflow(), 0, 1));
    }

    @Inject(method = "scrollItems", at = @At("HEAD"), cancellable = true)
    private void bedrockify$scroll(float scroll, CallbackInfo ci) {
        if (!bedrockify$classic) return;
        int first = bedrockify$firstVisibleIndex(scroll);
        for (int cell = 0; cell < bedrockify$pickerSize(); cell++) {
            int index = first + cell;
            CreativeInventoryScreen.INVENTORY.setStack(cell, index < itemList.size() ? itemList.get(index) : ItemStack.EMPTY);
        }
        ci.cancel();
    }

    @Inject(method = "shouldShowScrollbar", at = @At("HEAD"), cancellable = true)
    private void bedrockify$scrollbar(CallbackInfoReturnable<Boolean> cir) {
        if (bedrockify$classic) cir.setReturnValue(itemList.size() > bedrockify$pickerSize());
    }
}
