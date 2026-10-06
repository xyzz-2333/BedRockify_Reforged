package dev.bedrockify.forge.client.features.survivalInventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/** Bounded native crafting clicks, paced by server result updates rather than rendering. */
public final class RecipeQuickCraft {
    private final MinecraftClient client;
    private final AbstractRecipeScreenHandler<?> handler;
    private Recipe<?> recipe;
    private ItemStack batchOutput = ItemStack.EMPTY;
    private boolean batch;
    private boolean waitingForPlacement;
    private int remaining, revision, waitTicks;

    public RecipeQuickCraft(MinecraftClient client, AbstractRecipeScreenHandler<?> handler) {
        this.client = client; this.handler = handler;
    }
    public boolean running() { return recipe != null; }
    public void cancel() { recipe = null; batchOutput = ItemStack.EMPTY; }
    public void start(Recipe<?> recipe, boolean batch) {
        cancel();
        if (!available() || !handler.getCursorStack().isEmpty()) return;
        this.recipe = recipe; this.batch = batch; remaining = -1;
        waitingForPlacement = false;
        if (!matches() || handler.getSlot(handler.getCraftingResultSlotIndex()).getStack().isEmpty()) requestIngredients();
    }
    private boolean available() {
        return client.player != null && client.interactionManager != null
                && client.player.currentScreenHandler == handler;
    }
    @SuppressWarnings("unchecked")
    private boolean matches() {
        return ((AbstractRecipeScreenHandler<RecipeInputInventory>)handler)
                .matches((Recipe<? super RecipeInputInventory>)recipe);
    }
    private void requestIngredients() {
        revision = handler.getRevision(); waitTicks = 0; waitingForPlacement = true;
        // Fill one recipe at a time. Native "craft all" would move more than one stack.
        client.interactionManager.clickRecipe(handler.syncId, recipe, false);
    }
    public void tick(boolean active) {
        if (!running()) return;
        if (!active || !available() || !handler.getCursorStack().isEmpty()) { cancel(); return; }
        // Recipe placement is asynchronous, including on a local integrated server.
        if (waitingForPlacement && handler.getRevision() == revision) {
            if (++waitTicks > 100) cancel();
            return;
        }
        ItemStack output = handler.getSlot(handler.getCraftingResultSlotIndex()).getStack();
        if (output.isEmpty() || !matches()) {
            if (++waitTicks > 100) cancel();
            return;
        }
        if (remaining < 0) {
            batchOutput = output.copy();
            remaining = batch ? Math.max(output.getMaxCount(), output.getCount()) : output.getCount();
        }
        if (!ItemStack.canCombine(batchOutput, output) || output.getCount() > remaining || !fitsInventory(output)) {
            cancel(); return;
        }
        int amount = output.getCount();
        client.interactionManager.clickSlot(handler.syncId, handler.getCraftingResultSlotIndex(), 0,
                SlotActionType.PICKUP, client.player);
        ItemStack cursor = handler.getCursorStack();
        if (!ItemStack.canCombine(batchOutput, cursor) || cursor.getCount() != amount) { cancel(); return; }
        // Only native player inventory slots are destinations. Never touch armor or offhand.
        for (int pass = 0; pass < 2 && !handler.getCursorStack().isEmpty(); pass++) {
            for (Slot slot : handler.slots) {
                cursor = handler.getCursorStack();
                if (cursor.isEmpty()) break;
                ItemStack stack = slot.getStack();
                if (playerSlot(slot) && slot.canInsert(cursor) && slot.isEnabled()
                        && (pass == 0 ? !stack.isEmpty() && ItemStack.canCombine(stack, cursor)
                        && stack.getCount() < slot.getMaxItemCount(cursor) : stack.isEmpty())) {
                    client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
                }
            }
        }
        remaining -= amount;
        if (!handler.getCursorStack().isEmpty() || !batch || remaining < batchOutput.getCount()) { cancel(); return; }
        requestIngredients();
    }
    private boolean playerSlot(Slot slot) {
        return slot.inventory == client.player.getInventory() && slot.getIndex() >= 0 && slot.getIndex() < 36;
    }
    private boolean fitsInventory(ItemStack output) {
        int capacity = 0;
        for (Slot slot : handler.slots) {
            if (!playerSlot(slot) || !slot.isEnabled() || !slot.canInsert(output)) continue;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || ItemStack.canCombine(stack, output)) {
                capacity += Math.max(0, slot.getMaxItemCount(output) - (stack.isEmpty() ? 0 : stack.getCount()));
                if (capacity >= output.getCount()) return true;
            }
        }
        return false;
    }
}
