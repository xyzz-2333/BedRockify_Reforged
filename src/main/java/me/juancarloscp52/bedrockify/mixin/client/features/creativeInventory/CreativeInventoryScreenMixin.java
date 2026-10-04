package me.juancarloscp52.bedrockify.mixin.client.features.creativeInventory;

import me.juancarloscp52.bedrockify.client.BedrockifyClient;
import me.juancarloscp52.bedrockify.client.features.creativeInventory.CreativeCatalog;
import me.juancarloscp52.bedrockify.client.features.creativeInventory.CreativeGrid;
import me.juancarloscp52.bedrockify.client.features.creativeInventory.CreativeGroups;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
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
public abstract class CreativeInventoryScreenMixin extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler> {
    @Shadow private static ItemGroup selectedTab;
    @Shadow private float scrollPosition;
    @Shadow private boolean scrolling;
    @Shadow private TextFieldWidget searchBox;
    @Shadow private CreativeTabsScreenPage currentPage;
    @Shadow protected abstract void renderTabIcon(DrawContext context, ItemGroup group);
    @Shadow private int getTabX(ItemGroup group) { throw new AssertionError(); }
    @Shadow private int getTabY(ItemGroup group) { throw new AssertionError(); }

    @Unique private boolean bedrockify$classic;
    @Unique private int bedrockify$columns = 17;
    @Unique private int bedrockify$rows = 7;
    @Unique private List<ItemStack> bedrockify$source = List.of();
    @Unique private List<CreativeGroups.Entry> bedrockify$entries = List.of();
    @Unique private boolean bedrockify$hasGroups;
    @Unique private boolean bedrockify$populating;
    @Unique private final Set<String> bedrockify$expanded = new HashSet<>();

    protected CreativeInventoryScreenMixin(CreativeInventoryScreen.CreativeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Unique private boolean bedrockify$enabled() {
        BedrockifyClient mod = BedrockifyClient.getInstance();
        return mod != null && mod.settings != null && mod.settings.creativeInventory;
    }

    @Unique private boolean bedrockify$useClassic(ItemGroup group) {
        return bedrockify$enabled() && width >= 260 && height >= 230 && group.getType() != ItemGroup.Type.INVENTORY && group.getType() != ItemGroup.Type.HOTBAR;
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/CreativeModeTabRegistry;getSortedCreativeModeTabs()Ljava/util/List;", remap = false))
    private List<ItemGroup> bedrockify$fourCategories() {
        List<ItemGroup> tabs = CreativeModeTabRegistry.getSortedCreativeModeTabs();
        return bedrockify$enabled() && width >= 260 && height >= 230 ? CreativeCatalog.visibleTabs(tabs) : tabs;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void bedrockify$initialGeometry(CallbackInfo ci) { bedrockify$geometry(selectedTab); }

    @Inject(method = "refreshSelectedTab", at = @At("HEAD"))
    private void bedrockify$beginPopulate(CallbackInfo ci) { bedrockify$populating = true; }

    @Inject(method = "setSelectedTab", at = @At("HEAD"))
    private void bedrockify$beginSelect(ItemGroup group, CallbackInfo ci) {
        bedrockify$populating = true;
        bedrockify$geometry(group);
        // Release the old catalogue before vanilla loads saved hotbar NBT. Leave inventory
        // wrappers intact until vanilla restores its cached picker slots on the way out.
        if (!bedrockify$classic) bedrockify$sourceFromTab();
        if (selectedTab.getType() != ItemGroup.Type.INVENTORY) bedrockify$configureGrid(true);
    }

    @Inject(method = "setSelectedTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/CreativeInventoryScreen$CreativeScreenHandler;scrollItems(F)V"))
    private void bedrockify$prepareScroll(ItemGroup group, CallbackInfo ci) {
        // The first population must already use the destination grid, including 9 x 5 for
        // saved hotbars. Configuring only at TAIL exposed the old 17 x 7 grid to mod hooks.
        bedrockify$configureGrid(group.getType() != ItemGroup.Type.INVENTORY);
    }

    @Unique private void bedrockify$configureGrid(boolean rebuild) {
        ((CreativeGrid) handler).bedrockify$configureGrid(bedrockify$classic,
                bedrockify$classic ? bedrockify$columns : 9, bedrockify$classic ? bedrockify$rows : 5, rebuild);
    }

    @Inject(method = "setSelectedTab", at = @At("TAIL"))
    private void bedrockify$select(ItemGroup group, CallbackInfo ci) {
        bedrockify$sourceFromTab();
        bedrockify$rebuild(true);
        bedrockify$positionControls();
        bedrockify$populating = false;
    }

    @Unique private void bedrockify$geometry(ItemGroup group) {
        bedrockify$classic = bedrockify$useClassic(group);
        bedrockify$columns = MathHelper.clamp((width - 70) / 20, 9, 17);
        // Leave 50 GUI pixels above and below for native Forge tabs and page controls.
        bedrockify$rows = MathHelper.clamp((height - 170) / 20, 3, 7);
        backgroundWidth = bedrockify$classic ? bedrockify$columns * 20 + 32 : 195;
        backgroundHeight = bedrockify$classic ? bedrockify$rows * 20 + 70 : 136;
        x = (width - backgroundWidth) / 2;
        y = (height - backgroundHeight) / 2;
    }

    @Unique private void bedrockify$positionControls() {
        if (searchBox != null && bedrockify$classic) {
            searchBox.setX(x + 12);
            searchBox.setY(y + 10);
            searchBox.setWidth(backgroundWidth - 124);
            searchBox.setEditableColor(0x303030);
        } else if (searchBox != null) {
            searchBox.setWidth(selectedTab.getSearchBarWidth());
            searchBox.setX(x + 171 - searchBox.getWidth());
            searchBox.setY(y + 6);
            searchBox.setEditableColor(0xffffff);
        }
        for (Element element : children()) {
            if (element instanceof ButtonWidget button) {
                String label = button.getMessage().getString();
                if (label.equals("<") || label.equals(">")) {
                    button.setX(label.equals("<") ? x : x + backgroundWidth - 20);
                    button.setY(y - 50);
                }
            }
        }
    }

    @Unique private void bedrockify$sourceFromTab() {
        if (!bedrockify$classic) {
            bedrockify$source = List.of();
            bedrockify$entries = List.of();
            bedrockify$hasGroups = false;
            return;
        }
        CreativeCatalog.Category category = CreativeCatalog.category(selectedTab);
        bedrockify$source = category == null ? new ArrayList<>(handler.itemList) : CreativeCatalog.contents(category);
    }

    @Unique private void bedrockify$rebuild(boolean resetScroll) {
        if (!bedrockify$classic) return;
        int firstVisible = ((CreativeGrid) handler).bedrockify$firstVisibleIndex(scrollPosition);
        boolean collapse = BedrockifyClient.getInstance().settings.creativeInventoryGroups &&
                selectedTab.getType() != ItemGroup.Type.SEARCH && (!selectedTab.hasSearchBar() || searchBox.getText().isEmpty());
        CreativeCatalog.Category category = CreativeCatalog.category(selectedTab);
        bedrockify$entries = CreativeGroups.layout(bedrockify$source, bedrockify$expanded, collapse,
                category == CreativeCatalog.Category.CONSTRUCTION || category == CreativeCatalog.Category.EQUIPMENT);
        bedrockify$hasGroups = bedrockify$entries.stream().anyMatch(CreativeGroups.Entry::header);
        handler.itemList.clear();
        for (CreativeGroups.Entry entry : bedrockify$entries) handler.itemList.add(entry.stack());
        int overflow = Math.max(0, MathHelper.ceilDiv(bedrockify$entries.size(), bedrockify$columns) - bedrockify$rows);
        scrollPosition = resetScroll || overflow == 0 ? 0 : (float) (firstVisible / bedrockify$columns) / overflow;
        scrollPosition = Float.isFinite(scrollPosition) ? MathHelper.clamp(scrollPosition, 0, 1) : 0;
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
        if (!bedrockify$classic || bedrockify$populating) return;
        bedrockify$source = new ArrayList<>(handler.itemList);
        bedrockify$rebuild(true);
    }

    @ModifyConstant(method = "onMouseClick", constant = @Constant(intValue = 45))
    private int bedrockify$hotbarOffset(int original) {
        return bedrockify$classic ? ((CreativeGrid) handler).bedrockify$pickerSize() : original;
    }

    @Redirect(method = {"onMouseClick", "isCreativeInventorySlot"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ingame/CreativeInventoryScreen;INVENTORY:Lnet/minecraft/inventory/SimpleInventory;"))
    private SimpleInventory bedrockify$pickerForClicks() {
        return ((CreativeGrid) handler).bedrockify$pickerInventory();
    }

    @Unique private CreativeGroups.Entry bedrockify$entry(Slot slot) {
        if (!bedrockify$classic || slot == null || slot.inventory != ((CreativeGrid) handler).bedrockify$pickerInventory()) return null;
        int index = ((CreativeGrid) handler).bedrockify$firstVisibleIndex(scrollPosition) + slot.getSlotIndex();
        return index >= 0 && index < bedrockify$entries.size() ? bedrockify$entries.get(index) : null;
    }

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
    private void bedrockify$toggleGroup(Slot slot, int slotId, int button, SlotActionType action, CallbackInfo ci) {
        CreativeGroups.Entry entry = bedrockify$entry(slot);
        if (entry == null || !entry.header() || button != 0 || (action != SlotActionType.PICKUP && action != SlotActionType.QUICK_MOVE)) return;
        // Do not disturb a held stack or an in-progress drag when a header is clicked.
        if (handler.getCursorStack().isEmpty()) {
            if (!bedrockify$expanded.remove(entry.group())) bedrockify$expanded.add(entry.group());
            bedrockify$rebuild(false);
        }
        ci.cancel();
    }

    @Inject(method = "isClickInScrollbar", at = @At("HEAD"), cancellable = true)
    private void bedrockify$scrollbarHitbox(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (bedrockify$classic) cir.setReturnValue(mouseX >= x + backgroundWidth - 16 && mouseX < x + backgroundWidth - 6 && mouseY >= y + 28 && mouseY < y + 28 + bedrockify$rows * 20);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void bedrockify$dragScrollbar(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (bedrockify$classic && scrolling) {
            scrollPosition = MathHelper.clamp(((float) mouseY - y - 28 - 8) / (bedrockify$rows * 20 - 16), 0, 1);
            handler.scrollItems(scrollPosition);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void bedrockify$allGroups(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!bedrockify$classic || button != 0 || !bedrockify$hasGroups) return;
        if (mouseY >= y + 6 && mouseY < y + 22 && mouseX >= x + backgroundWidth - 54 && mouseX < x + backgroundWidth - 14) {
            if (mouseX < x + backgroundWidth - 34) {
                for (CreativeGroups.Entry entry : bedrockify$entries) if (entry.header()) bedrockify$expanded.add(entry.group());
            } else bedrockify$expanded.clear();
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
        if (!bedrockify$classic) return;
        for (ItemGroup group : currentPage.getVisibleTabs()) if (group != selectedTab) renderTabIcon(context, group);
        int mainHeight = 38 + bedrockify$rows * 20;
        int hotbarLeft = (backgroundWidth - 180) / 2;
        bedrockify$panel(context, x, y, backgroundWidth, mainHeight, 0xffc6c6c6);
        bedrockify$panel(context, x + hotbarLeft - 8, y + mainHeight - 2, 196, 34, 0xffc6c6c6);
        context.fill(x + 8, y + 26, x + backgroundWidth - 8, y + mainHeight - 4, 0xff5c5c5c);
        int first = ((CreativeGrid) handler).bedrockify$firstVisibleIndex(scrollPosition);
        for (int cell = 0; cell < bedrockify$columns * bedrockify$rows; cell++) {
            if (first + cell >= handler.itemList.size()) break;
            bedrockify$slot(context, x + 12 + cell % bedrockify$columns * 20, y + 28 + cell / bedrockify$columns * 20);
        }
        for (int i = 0; i < 9; i++) bedrockify$slot(context, x + hotbarLeft + i * 20, y + 42 + bedrockify$rows * 20);
        if (searchBox.isVisible()) {
            context.fill(searchBox.getX() - 3, searchBox.getY() - 3, searchBox.getX() + searchBox.getWidth() + 3, searchBox.getY() + 12, 0xff8b8b8b);
            context.fill(searchBox.getX() - 2, searchBox.getY() - 2, searchBox.getX() + searchBox.getWidth() + 2, searchBox.getY() + 11, 0xfff0f0f0);
        }
        searchBox.render(context, mouseX, mouseY, delta);
        if (selectedTab.hasScrollbar() && handler.shouldShowScrollbar()) {
            int left = x + backgroundWidth - 16;
            context.fill(left, y + 28, left + 10, y + 28 + bedrockify$rows * 20, 0xff373737);
            int top = y + 28 + Math.round((bedrockify$rows * 20 - 16) * scrollPosition);
            bedrockify$panel(context, left, top, 10, 16, 0xffc6c6c6);
        }
        if (currentPage.getVisibleTabs().contains(selectedTab)) renderTabIcon(context, selectedTab);
        ci.cancel();
    }

    @Inject(method = "renderTabIcon", at = @At("HEAD"), cancellable = true)
    private void bedrockify$tabStyle(DrawContext context, ItemGroup group, CallbackInfo ci) {
        if (!bedrockify$classic) return;
        boolean selected = group == selectedTab;
        int left = x + getTabX(group);
        int top = y + getTabY(group) + (currentPage.isTop(group) ? 4 : -4);
        bedrockify$panel(context, left, top, 26, 32, selected ? 0xffc6c6c6 : 0xff5c5c5c);
        CreativeCatalog.Category category = CreativeCatalog.category(group);
        context.drawItem(category == null ? group.getIcon() : category.icon, left + 5, top + 8);
        ci.cancel();
    }

    @Inject(method = "renderTabTooltipIfHovered", at = @At("HEAD"), cancellable = true)
    private void bedrockify$tabTitle(DrawContext context, ItemGroup group, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        CreativeCatalog.Category category = CreativeCatalog.category(group);
        if (!bedrockify$classic || category == null) return;
        if (isPointWithinBounds(getTabX(group) + 3, getTabY(group) + 3, 21, 27, mouseX, mouseY)) {
            context.drawTooltip(textRenderer, category.title(), mouseX, mouseY);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true)
    private void bedrockify$foreground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!bedrockify$classic) return;
        CreativeCatalog.Category category = CreativeCatalog.category(selectedTab);
        Text title = category == null ? selectedTab.getDisplayName() : category.title();
        int titleWidth = searchBox.isVisible() ? (bedrockify$hasGroups ? 54 : 98) : backgroundWidth - 80;
        if (textRenderer.getWidth(title) > titleWidth) title = Text.literal(textRenderer.trimToWidth(title.getString(), titleWidth - 10) + "…");
        int titleX = searchBox.isVisible() ? backgroundWidth - textRenderer.getWidth(title) - (bedrockify$hasGroups ? 60 : 12) : 12;
        context.drawText(textRenderer, title, titleX, 10, 0xff404040, false);
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300);
        int first = ((CreativeGrid) handler).bedrockify$firstVisibleIndex(scrollPosition);
        for (int cell = 0; cell < bedrockify$columns * bedrockify$rows && first + cell < bedrockify$entries.size(); cell++) {
            CreativeGroups.Entry entry = bedrockify$entries.get(first + cell);
            if (!entry.header()) continue;
            int left = 12 + cell % bedrockify$columns * 20 + 10;
            int top = 28 + cell / bedrockify$columns * 20 + 10;
            String marker = bedrockify$expanded.contains(entry.group()) ? "−" : "+";
            // Outline the glyph itself; Bedrock does not put a black tile behind group markers.
            context.drawText(textRenderer, marker, left + 1, top, 0xff000000, false);
            context.drawText(textRenderer, marker, left + 3, top, 0xff000000, false);
            context.drawText(textRenderer, marker, left + 2, top - 1, 0xff000000, false);
            context.drawText(textRenderer, marker, left + 2, top + 1, 0xff000000, false);
            context.drawText(textRenderer, marker, left + 2, top, 0xffffffff, false);
        }
        if (bedrockify$hasGroups) {
            bedrockify$panel(context, backgroundWidth - 54, 6, 18, 16, 0xff8b8b8b);
            bedrockify$panel(context, backgroundWidth - 34, 6, 18, 16, 0xff8b8b8b);
            context.drawText(textRenderer, "+", backgroundWidth - 48, 9, 0xffffffff, false);
            context.drawText(textRenderer, "−", backgroundWidth - 28, 9, 0xffffffff, false);
        }
        context.getMatrices().pop();
        ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void bedrockify$controlsTooltip(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!bedrockify$classic || !bedrockify$hasGroups) return;
        if (mouseY >= y + 6 && mouseY < y + 22 && mouseX >= x + backgroundWidth - 54 && mouseX < x + backgroundWidth - 14) {
            context.drawTooltip(textRenderer, Text.translatable(mouseX < x + backgroundWidth - 34 ? "bedrockify.creative.expandAll" : "bedrockify.creative.collapseAll"), mouseX, mouseY);
        }
    }

    @Unique private static void bedrockify$panel(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + h, 0xff171717);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xfff0f0f0);
        context.fill(x + 2, y + 2, x + w - 2, y + h - 2, color);
        context.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xff555555);
        context.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xff555555);
    }

    @Unique private static void bedrockify$slot(DrawContext context, int x, int y) {
        context.fill(x, y, x + 20, y + 20, 0xff424242);
        context.fill(x + 1, y + 1, x + 20, y + 20, 0xffd0d0d0);
        context.fill(x + 1, y + 1, x + 19, y + 19, 0xff8b8b8b);
    }
}
