package dev.bedrockify.forge.mixin.client.features.creativeInventory;

import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeCatalog;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeScreenLayout;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeScreenMemory;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeGrid;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeGroups;
import dev.bedrockify.forge.client.features.creativeInventory.CreativePickerSlot;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeSearchStyle;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeViewTabs;
import dev.bedrockify.forge.client.features.creativeInventory.BedrockCreativeScreen;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeScreenContent;
import dev.bedrockify.forge.client.features.creativeInventory.CreativeScreenRenderer;
import dev.bedrockify.forge.client.gui.ContainerScreenPolicy;
import dev.bedrockify.forge.client.features.inventoryMemory.InventoryUiState;
import dev.bedrockify.forge.client.features.inventoryMemory.RememberedInventory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import net.minecraftforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler> implements RememberedInventory, CreativeSearchStyle {
    @Shadow private static ItemGroup selectedTab;
    @Shadow private float scrollPosition;
    @Shadow private boolean scrolling;
    @Shadow private TextFieldWidget searchBox;
    @Shadow private CreativeTabsScreenPage currentPage;
    @Shadow @org.spongepowered.asm.mixin.Final private List<CreativeTabsScreenPage> pages;
    @Shadow(remap = false) public abstract void setCurrentPage(CreativeTabsScreenPage page);
    @Shadow private void setSelectedTab(ItemGroup group) { throw new AssertionError(); }
    @Shadow private void search() { throw new AssertionError(); }
    @Shadow protected abstract void renderTabIcon(DrawContext context, ItemGroup group);
    @Shadow private int getTabX(ItemGroup group) { throw new AssertionError(); }
    @Shadow private int getTabY(ItemGroup group) { throw new AssertionError(); }

    @Unique private static final ContainerScreenPolicy BEDROCKIFY_POLICY =
            new ContainerScreenPolicy(CreativeInventoryScreen.class, 54, 260, 230);
    @Unique private final CreativeScreenContent bedrockify$content = new CreativeScreenContent();
    @Unique private boolean bedrockify$populating;
    @Unique private CreativeScreenLayout bedrockify$layout = CreativeScreenLayout.calculate(0, 0, false);
    @Unique private final CreativeScreenMemory bedrockify$memory = new CreativeScreenMemory();

    protected CreativeInventoryScreenMixin(CreativeInventoryScreen.CreativeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Unique private int bedrockify$viewTabY() { return bedrockify$layout.viewTabY(); }

    @Unique private boolean bedrockify$enabled() {
        BedrockifyClient mod = BedrockifyClient.getInstance();
        return mod != null && mod.settings != null && mod.settings.creativeInventory;
    }

    @Override public boolean bedrockify$flatSearch(TextFieldWidget field) {
        return bedrockify$layout.classic() && searchBox == field;
    }

    @Unique private boolean bedrockify$supported() {
        return BEDROCKIFY_POLICY.supports(this, ((CreativeGrid) handler).bedrockify$nativeSlotCount(),
                bedrockify$enabled()) && BedrockCreativeScreen.supportsPlayerInventory(client.player);
    }

    @Unique private boolean bedrockify$useClassic(ItemGroup group) {
        return bedrockify$supported() && group.getType() != ItemGroup.Type.INVENTORY && group.getType() != ItemGroup.Type.HOTBAR;
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/CreativeModeTabRegistry;getSortedCreativeModeTabs()Ljava/util/List;", remap = false))
    private List<ItemGroup> bedrockify$fourCategories() {
        List<ItemGroup> tabs = CreativeModeTabRegistry.getSortedCreativeModeTabs();
        return bedrockify$supported() ? CreativeCatalog.visibleTabs(tabs) : tabs;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void bedrockify$initialGeometry(CallbackInfo ci) {
        bedrockify$rememberInventory();
        bedrockify$memory.beginInitialization();
        bedrockify$geometry(selectedTab);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void bedrockify$restoreTab(CallbackInfo ci) {
        bedrockify$memory.restoreTabs(pages, () -> currentPage,
                page -> setCurrentPage(page), group -> setSelectedTab(group));
    }

    @Override public void bedrockify$rememberInventory() {
        bedrockify$memory.remember(selectedTab, currentPage, bedrockify$layout.classic(), searchBox,
                ((CreativeGrid) handler).bedrockify$firstVisibleIndex(scrollPosition));
    }

    @Inject(method = "refreshSelectedTab", at = @At("HEAD"))
    private void bedrockify$beginPopulate(CallbackInfo ci) { bedrockify$populating = true; }

    @Inject(method = "setSelectedTab", at = @At("HEAD"))
    private void bedrockify$beginSelect(ItemGroup group, CallbackInfo ci) {
        bedrockify$rememberInventory();
        bedrockify$populating = true;
        bedrockify$geometry(group);
        // Release the old catalogue before vanilla loads saved hotbar NBT. Leave inventory
        // wrappers intact until vanilla restores its cached picker slots on the way out.
        if (!bedrockify$layout.classic()) bedrockify$sourceFromTab();
        if (selectedTab.getType() != ItemGroup.Type.INVENTORY) bedrockify$configureGrid(true);
    }

    @Inject(method = "setSelectedTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/CreativeInventoryScreen$CreativeScreenHandler;scrollItems(F)V"))
    private void bedrockify$prepareScroll(ItemGroup group, CallbackInfo ci) {
        // The first population must already use the destination grid, including 9 x 5 for
        // saved hotbars. Configuring only at TAIL exposed the old 17 x 7 grid to mod hooks.
        bedrockify$configureGrid(group.getType() != ItemGroup.Type.INVENTORY);
    }

    @Unique private void bedrockify$configureGrid(boolean rebuild) {
        ((CreativeGrid) handler).bedrockify$configureGrid(bedrockify$layout.classic(),
                bedrockify$layout.classic() ? bedrockify$layout.columns() : 9, bedrockify$layout.classic() ? bedrockify$layout.rows() : 5, rebuild);
    }

    @Inject(method = "setSelectedTab", at = @At("TAIL"))
    private void bedrockify$select(ItemGroup group, CallbackInfo ci) {
        InventoryUiState.View view = bedrockify$memory.select(group, bedrockify$layout.classic());
        bedrockify$sourceFromTab();
        bedrockify$rebuild(true);
        bedrockify$positionControls();
        bedrockify$populating = false;
        bedrockify$memory.restoreSearch(view, group, searchBox, () -> search());
        if (view != null) {
            scrollPosition = bedrockify$layout.restoredScroll(view.first(), handler.itemList.size());
            handler.scrollItems(scrollPosition);
        }
    }

    @Unique private void bedrockify$geometry(ItemGroup group) {
        bedrockify$layout = CreativeScreenLayout.calculate(width, height, bedrockify$useClassic(group));
        backgroundWidth = bedrockify$layout.width(); backgroundHeight = bedrockify$layout.height();
        x = bedrockify$layout.left(); y = bedrockify$layout.top();
    }

    @Unique private void bedrockify$positionControls() {
        bedrockify$layout.positionControls(searchBox, selectedTab, children());
    }

    @Unique private void bedrockify$sourceFromTab() {
        bedrockify$content.select(bedrockify$layout.classic(), selectedTab, handler.itemList);
    }

    @Unique private void bedrockify$rebuild(boolean resetScroll) {
        if (!bedrockify$layout.classic()) return;
        int firstVisible = ((CreativeGrid) handler).bedrockify$firstVisibleIndex(scrollPosition);
        bedrockify$content.rebuild(selectedTab, searchBox == null ? "" : searchBox.getText(),
                BedrockifyClient.getInstance().settings.creativeInventoryGroups, bedrockify$memory.expanded());
        handler.itemList.clear();
        for (CreativeGroups.Entry entry : bedrockify$content.entries()) handler.itemList.add(entry.stack());
        scrollPosition = resetScroll ? 0 : bedrockify$layout.restoredScroll(firstVisible, handler.itemList.size());
        handler.scrollItems(scrollPosition);
    }

    @Inject(method = "refreshSelectedTab", at = @At("TAIL"))
    private void bedrockify$refresh(Collection<ItemStack> stacks, CallbackInfo ci) {
        bedrockify$sourceFromTab();
        bedrockify$rebuild(false);
        bedrockify$populating = false;
    }

    @Inject(method = "search", at = @At("TAIL"))
    private void bedrockify$searchResults(CallbackInfo ci) {
        if (!bedrockify$layout.classic() || bedrockify$populating) return;
        bedrockify$content.searchResults(handler.itemList);
        bedrockify$rebuild(true);
    }

    @ModifyConstant(method = "onMouseClick", constant = @Constant(intValue = 45))
    private int bedrockify$hotbarOffset(int original) {
        return bedrockify$layout.classic() ? ((CreativeGrid) handler).bedrockify$pickerSize() : original;
    }

    @Redirect(method = {"onMouseClick", "isCreativeInventorySlot"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ingame/CreativeInventoryScreen;INVENTORY:Lnet/minecraft/inventory/SimpleInventory;"))
    private SimpleInventory bedrockify$pickerForClicks() {
        return ((CreativeGrid) handler).bedrockify$pickerInventory();
    }

    @Unique private CreativeGroups.Entry bedrockify$entry(Slot slot) {
        if (!bedrockify$layout.classic() || slot == null || slot.inventory != ((CreativeGrid) handler).bedrockify$pickerInventory()) return null;
        int index = ((CreativeGrid) handler).bedrockify$firstVisibleIndex(scrollPosition) + slot.getSlotIndex();
        return bedrockify$content.entry(index);
    }

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
    private void bedrockify$toggleGroup(Slot slot, int slotId, int button, SlotActionType action, CallbackInfo ci) {
        if (slot instanceof CreativePickerSlot && !slot.isEnabled()) { ci.cancel(); return; }
        CreativeGroups.Entry entry = bedrockify$entry(slot);
        if (entry == null || !entry.header() || button != 0 || (action != SlotActionType.PICKUP && action != SlotActionType.QUICK_MOVE)) return;
        // Do not disturb a held stack or an in-progress drag when a header is clicked.
        if (handler.getCursorStack().isEmpty()) {
            bedrockify$memory.toggle(entry.group());
            bedrockify$rebuild(false);
        }
        ci.cancel();
    }

    @Inject(method = "isClickInScrollbar", at = @At("HEAD"), cancellable = true)
    private void bedrockify$scrollbarHitbox(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (bedrockify$layout.classic()) cir.setReturnValue(bedrockify$layout.scrollbarContains(mouseX, mouseY));
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void bedrockify$dragScrollbar(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (bedrockify$layout.classic() && scrolling) {
            scrollPosition = bedrockify$layout.draggedScroll(mouseY);
            handler.scrollItems(scrollPosition);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void bedrockify$allGroups(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!bedrockify$layout.classic() || button != 0) return;
        int view = CreativeViewTabs.hit(mouseX, mouseY, x + backgroundWidth - 135, bedrockify$viewTabY());
        if (view >= 0) {
            if (view != 0 && handler.getCursorStack().isEmpty()) client.setScreen(new BedrockCreativeScreen(client.player, view == 1));
            cir.setReturnValue(true); return;
        }
        if (!bedrockify$content.hasGroups()) return;
        if (mouseY >= y + 6 && mouseY < y + 22 && mouseX >= x + backgroundWidth - 54 && mouseX < x + backgroundWidth - 14) {
            if (mouseX < x + backgroundWidth - 34) {
                for (CreativeGroups.Entry entry : bedrockify$content.entries()) if (entry.header()) bedrockify$memory.expand(entry.group());
            } else bedrockify$memory.collapseAll();
            bedrockify$rebuild(true);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true)
    private void bedrockify$groupTooltip(ItemStack stack, CallbackInfoReturnable<List<Text>> cir) {
        CreativeGroups.Entry entry = bedrockify$entry(focusedSlot);
        if (entry == null || !entry.header()) return;
        List<Text> tooltip = new ArrayList<>(cir.getReturnValue());
        tooltip.add(0, Text.translatable("bedrockify.creative.groupCount", entry.title(), entry.count()).formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("bedrockify.creative.groupHint").formatted(Formatting.GRAY));
        cir.setReturnValue(tooltip);
    }

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void bedrockify$background(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!bedrockify$layout.classic()) return;
        for (ItemGroup group : currentPage.getVisibleTabs()) if (group != selectedTab) renderTabIcon(context, group);
        CreativeScreenRenderer.background(context, bedrockify$layout, searchBox,
                ((CreativeGrid) handler).bedrockify$firstVisibleIndex(scrollPosition), handler.itemList.size(),
                selectedTab.hasScrollbar() && handler.shouldShowScrollbar(), scrollPosition);
        if (currentPage.getVisibleTabs().contains(selectedTab)) renderTabIcon(context, selectedTab);
        CreativeViewTabs.draw(context, x + backgroundWidth - 135, bedrockify$viewTabY(), 0);
        ci.cancel();
    }

    @Inject(method = "renderTabIcon", at = @At("HEAD"), cancellable = true)
    private void bedrockify$tabStyle(DrawContext context, ItemGroup group, CallbackInfo ci) {
        if (!bedrockify$layout.classic()) return;
        boolean selected = group == selectedTab;
        int left = x + getTabX(group);
        int top = y + getTabY(group) + (currentPage.isTop(group) ? 4 : -4);
        CreativeScreenRenderer.tab(context, group, selected, left, top);
        ci.cancel();
    }

    @Inject(method = "renderTabTooltipIfHovered", at = @At("HEAD"), cancellable = true)
    private void bedrockify$tabTitle(DrawContext context, ItemGroup group, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        CreativeCatalog.Category category = CreativeCatalog.category(group);
        if (!bedrockify$layout.classic() || category == null) return;
        if (isPointWithinBounds(getTabX(group) + 3, getTabY(group) + 3, 21, 27, mouseX, mouseY)) {
            context.drawTooltip(textRenderer, category.title(), mouseX, mouseY);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true)
    private void bedrockify$foreground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!bedrockify$layout.classic()) return;
        CreativeScreenRenderer.foreground(context, textRenderer, bedrockify$layout, selectedTab, searchBox,
                bedrockify$content, bedrockify$memory, ((CreativeGrid) handler).bedrockify$firstVisibleIndex(scrollPosition));
        ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void bedrockify$refreshCompatibility(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (client.currentScreen == (Object) this && bedrockify$layout.classic() != bedrockify$useClassic(selectedTab)) clearAndInit();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void bedrockify$controlsTooltip(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!bedrockify$layout.classic()) return;
        CreativeViewTabs.tooltip(context, textRenderer, mouseX, mouseY, x + backgroundWidth - 135,
                bedrockify$viewTabY(), !handler.getCursorStack().isEmpty());
        if (!bedrockify$content.hasGroups()) return;
        if (mouseY >= y + 6 && mouseY < y + 22 && mouseX >= x + backgroundWidth - 54 && mouseX < x + backgroundWidth - 14) {
            context.drawTooltip(textRenderer, Text.translatable(mouseX < x + backgroundWidth - 34 ? "bedrockify.creative.expandAll" : "bedrockify.creative.collapseAll"), mouseX, mouseY);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void bedrockify$searchField(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!bedrockify$layout.classic() || searchBox == null || !searchBox.isVisible()) return;
        CreativeScreenRenderer.search(context, searchBox, mouseX, mouseY, delta);
    }

}
