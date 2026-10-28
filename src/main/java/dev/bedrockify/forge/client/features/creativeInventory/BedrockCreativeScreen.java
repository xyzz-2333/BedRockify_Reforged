package dev.bedrockify.forge.client.features.creativeInventory;

import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.survivalInventory.SurvivalLayout;
import dev.bedrockify.forge.client.features.survivalInventory.SurvivalSprites;
import dev.bedrockify.forge.client.features.inventoryMemory.InventoryUiState;
import dev.bedrockify.forge.client.features.inventoryMemory.RememberedInventory;
import dev.bedrockify.forge.platform.item.ItemStackData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.*;

/** Creative catalogue beside the real player container, including its native crafting slots. */
public final class BedrockCreativeScreen extends HandledScreen<PlayerScreenHandler> implements CreativeSearchStyle, RememberedInventory {
    private static final int CATALOG_WIDTH = 172, COLS = 7, ROWS = 8, GRID_Y = 28, GAP = 0;
    private final Map<Slot, int[]> original = new IdentityHashMap<>();
    private final Set<String> expanded = new HashSet<>();
    private CreativeCatalog.Category category = CreativeCatalog.Category.CONSTRUCTION;
    private List<CreativeGroups.Entry> entries = List.of();
    private final boolean split;
    private int catalogX, firstRow;
    private boolean draggingScrollbar;
    private TextFieldWidget search;
    private ItemStackData.Key selectedStack;
    private boolean initialized, restoring;

    public BedrockCreativeScreen(PlayerEntity player, boolean split) {
        super(player.playerScreenHandler, player.getInventory(), Text.translatable("container.inventory"));
        this.split = split;
        backgroundWidth = SurvivalLayout.WIDTH;
        backgroundHeight = SurvivalLayout.HEIGHT;
    }

    @Override protected void init() {
        if (initialized) bedrockify$rememberInventory();
        super.init();
        if (width < backgroundWidth + 16 || height < backgroundHeight + 32) {
            CreativeViewTabs.openFull(client);
            return;
        }
        client.player.currentScreenHandler = handler;
        boolean dual = split && width >= CATALOG_WIDTH + GAP + backgroundWidth + 16;
        catalogX = (width - CATALOG_WIDTH - GAP - backgroundWidth) / 2;
        x = dual ? catalogX + CATALOG_WIDTH + GAP : (width - backgroundWidth) / 2;
        y = Math.max(30, (height - backgroundHeight) / 2);
        if (original.isEmpty()) for (Slot slot : handler.slots) original.put(slot, new int[]{slot.x, slot.y});
        move(0, 174, 44);
        for (int i = 0; i < 4; i++) move(1 + i, 114 + i % 2 * 20, 34 + i / 2 * 20);
        for (int i = 0; i < 4; i++) move(5 + i, 12, 12 + i * 20);
        move(45, 92, 72);
        for (int i = 0; i < 27; i++) move(9 + i, 12 + i % 9 * 20, 104 + i / 9 * 20);
        for (int i = 0; i < 9; i++) move(36 + i, 12 + i * 20, 172);
        if (dual) {
            var mod = BedrockifyClient.getInstance();
            var remembered = mod.settings.rememberInventoryState && mod.inventoryUiState != null
                    ? mod.inventoryUiState.view("creative:split", mod.settings.rememberInventorySearch) : null;
            if (remembered != null) {
                try { category = CreativeCatalog.Category.valueOf(remembered.category()); }
                catch (IllegalArgumentException ignored) { category = CreativeCatalog.Category.CONSTRUCTION; }
            }
            search = addSelectableChild(new TextFieldWidget(textRenderer, catalogX + 10, y + 9, CATALOG_WIDTH - 20, 12, Text.translatable("itemGroup.search")));
            search.setDrawsBackground(false);
            search.setEditableColor(0xff303030);
            search.setChangedListener(value -> { if (!restoring) { selectedStack = null; rebuild(true); } });
            restoreCategory(remembered);
        } else search = null;
        initialized = true;
        if (BedrockifyClient.getInstance().settings.rememberInventoryState && BedrockifyClient.getInstance().inventoryUiState != null)
            BedrockifyClient.getInstance().inventoryUiState.creativeView(split ? 1 : 2);
    }
    @Override public boolean bedrockify$flatSearch(TextFieldWidget field) { return field == search; }
    @Override public void bedrockify$rememberInventory() {
        var mod = BedrockifyClient.getInstance();
        if (!initialized || !mod.settings.rememberInventoryState || mod.inventoryUiState == null) return;
        mod.inventoryUiState.creativeView(split ? 1 : 2);
        if (search != null) {
            var view = new InventoryUiState.View(category.name(), search.getText(), firstRow * COLS, expanded);
            mod.inventoryUiState.remember("creative:split", view);
            mod.inventoryUiState.remember("creative:split:" + category.name(), view);
        }
        mod.inventoryUiState.save(mod.settings.rememberInventorySearch);
    }
    private void restoreCategory(InventoryUiState.View view) {
        restoring = true;
        expanded.clear();
        if (view != null) expanded.addAll(view.expanded());
        search.setText(view == null ? "" : view.search());
        search.setPlaceholder(category.title().copy().styled(style -> style.withColor(0x404040)));
        firstRow = view == null ? 0 : view.first() / COLS;
        selectedStack = null;
        restoring = false;
        rebuild(false);
    }
    private void move(int id, int sx, int sy) { Slot slot = handler.getSlot(id); slot.x = sx; slot.y = sy; }
    private int maxRow() { return Math.max(0, MathHelper.ceilDiv(entries.size(), COLS) - ROWS); }
    private void rebuild(boolean reset) {
        String query = search == null ? "" : search.getText().strip().toLowerCase(Locale.ROOT);
        List<ItemStack> source = new ArrayList<>();
        for (CreativeCatalog.Category candidate : CreativeCatalog.Category.values()) {
            if (query.isEmpty() && candidate != category) continue;
            for (ItemStack stack : CreativeCatalog.contents(candidate)) {
                if (query.isEmpty() || stack.getName().getString().toLowerCase(Locale.ROOT).contains(query)
                        || Registries.ITEM.getId(stack.getItem()).toString().contains(query)) source.add(stack);
            }
        }
        entries = CreativeGroups.layout(source, expanded,
                query.isEmpty() && BedrockifyClient.getInstance().settings.creativeInventoryGroups,
                category == CreativeCatalog.Category.CONSTRUCTION || category == CreativeCatalog.Category.EQUIPMENT);
        firstRow = reset ? 0 : Math.min(firstRow, maxRow());
    }
    private int entryAt(double mx, double my) {
        int col = (int)Math.floor((mx - catalogX - 10) / 20);
        int row = (int)Math.floor((my - y - GRID_Y) / 20);
        int index = (firstRow + row) * COLS + col;
        return search != null && col >= 0 && col < COLS && row >= 0 && row < ROWS && index < entries.size() ? index : -1;
    }
    @Override protected void drawBackground(DrawContext context, float delta, int mx, int my) {
        SurvivalSprites.inventory(context, handler, x, y);
        context.fill(x + 30, y + 10, x + 90, y + 90, 0xff000000);
        InventoryScreen.drawEntity(context, x + 60, y + 86, 35, x + 60 - mx, y + 45 - my, client.player);
        SurvivalSprites.craftingArrow(context, x + 156, y + 44, 14, 16);
        CreativeViewTabs.draw(context, x + backgroundWidth - 81, y - 28, split ? 1 : 2);
        if (search == null) return;
        SurvivalSprites.panel(context, catalogX, y, CATALOG_WIDTH, backgroundHeight, false);
        context.fill(catalogX + 8, y + 26, catalogX + CATALOG_WIDTH - 8, y + backgroundHeight - 8, 0xff626262);
        int tab = 0;
        for (var candidate : CreativeCatalog.Category.values()) {
            SurvivalSprites.frame(context, catalogX + tab * 26, y - 28, 24, 28, category == candidate ? 0xffc6c6c6 : 0xff626262);
            context.drawItem(candidate.icon, catalogX + tab * 26 + 4, y - 22); tab++;
        }
        for (int local = 0; local < COLS * ROWS && firstRow * COLS + local < entries.size(); local++) {
            var entry = entries.get(firstRow * COLS + local);
            int sx = catalogX + 10 + local % COLS * 20, sy = y + GRID_Y + local / COLS * 20;
            SurvivalSprites.cell(context, sx, sy, false);
            if (firstRow * COLS + local == entryAt(mx, my) || ItemStackData.key(entry.stack()).equals(selectedStack))
                SurvivalSprites.highlight(context, sx, sy);
            context.drawItem(entry.stack(), sx + 2, sy + 2);
            if (entry.header()) {
                context.getMatrices().push(); context.getMatrices().translate(0, 0, 200);
                context.drawText(textRenderer, expanded.contains(entry.group()) ? "−" : "+", sx + 12, sy + 10, 0xffffffff, true);
                context.getMatrices().pop();
            }
        }
        int sx = catalogX + CATALOG_WIDTH - 18;
        context.fill(sx, y + GRID_Y, sx + 10, y + GRID_Y + ROWS * 20, 0xff373737);
        int thumb = maxRow() == 0 ? 0 : Math.round((ROWS * 20 - 16) * (float)firstRow / maxRow());
        SurvivalSprites.frame(context, sx, y + GRID_Y + thumb, 10, 16, 0xffc6c6c6);
    }
    @Override protected void drawForeground(DrawContext context, int mx, int my) {
        context.drawText(textRenderer, Text.translatable("container.crafting"), 114, 20, 0xff303030, false);
    }
    @Override public void render(DrawContext context, int mx, int my, float delta) {
        renderBackground(context);
        super.render(context, mx, my, delta);
        if (search != null) {
            context.draw();
            context.getMatrices().push(); context.getMatrices().translate(0, 0, 300);
            context.setShaderColor(1, 1, 1, 1);
            search.render(context, mx, my, delta);
            context.draw(); context.getMatrices().pop();
        }
        drawMouseoverTooltip(context, mx, my);
        int index = entryAt(mx, my);
        if (index >= 0) {
            var entry = entries.get(index);
            if (entry.header()) context.drawTooltip(textRenderer, entry.title(), mx, my);
            else context.drawItemTooltip(textRenderer, entry.stack(), mx, my);
        }
        CreativeViewTabs.tooltip(context, textRenderer, mx, my, x + backgroundWidth - 81, y - 28, !handler.getCursorStack().isEmpty());
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        int view = CreativeViewTabs.hit(mx, my, x + backgroundWidth - 81, y - 28);
        if (view >= 0 && button == 0) {
            if (handler.getCursorStack().isEmpty()) {
                if (view == 0) CreativeViewTabs.openFull(client);
                else if ((view == 1) != split) client.setScreen(new BedrockCreativeScreen(client.player, view == 1));
            }
            return true;
        }
        if (search != null) {
            if (button == 0 && my >= y - 28 && my < y && mx >= catalogX && mx < catalogX + 104) {
                bedrockify$rememberInventory();
                category = CreativeCatalog.Category.values()[(int)(mx - catalogX) / 26];
                var mod = BedrockifyClient.getInstance();
                restoreCategory(mod.settings.rememberInventoryState && mod.inventoryUiState != null
                        ? mod.inventoryUiState.view("creative:split:" + category.name(), mod.settings.rememberInventorySearch) : null);
                return true;
            }
            if (button == 0 && mx >= catalogX + CATALOG_WIDTH - 18 && mx < catalogX + CATALOG_WIDTH - 8
                    && my >= y + GRID_Y && my < y + GRID_Y + ROWS * 20) {
                draggingScrollbar = true; dragScrollbar(my); return true;
            }
            int index = entryAt(mx, my);
            if (index >= 0) {
                var entry = entries.get(index);
                if (entry.header()) {
                    if (button == 0 && handler.getCursorStack().isEmpty()) {
                        if (!expanded.add(entry.group())) expanded.remove(entry.group()); rebuild(false);
                    }
                } else if (button <= 2) {
                    selectedStack = ItemStackData.key(entry.stack());
                    pick(entry.stack(), button == 1 ? 1 : entry.stack().getMaxCount());
                }
                return true;
            }
            if (mx >= catalogX && mx < catalogX + CATALOG_WIDTH && my >= y && my < y + backgroundHeight)
                return search.mouseClicked(mx, my, button);
            search.setFocused(false);
        }
        return super.mouseClicked(mx, my, button);
    }
    private void pick(ItemStack template, int count) {
        // Stage through a real hotbar slot, then use the native container click. This
        // gives the server the same cursor as the client, so crafting remains authoritative.
        int index = client.player.getInventory().selectedSlot, slotId = 36 + index;
        ItemStack saved = client.player.getInventory().getStack(index).copy();
        if (!handler.getCursorStack().isEmpty()) {
            // Empty the cursor through the native container before picking. Otherwise
            // picking the same item would merge into the staging slot instead of swapping.
            client.player.getInventory().setStack(index, ItemStack.EMPTY);
            client.interactionManager.clickCreativeStack(ItemStack.EMPTY, slotId);
            client.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, client.player);
        }
        ItemStack picked = template.copy(); picked.setCount(count);
        client.player.getInventory().setStack(index, picked);
        client.interactionManager.clickCreativeStack(picked, slotId);
        client.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, client.player);
        client.player.getInventory().setStack(index, saved);
        client.interactionManager.clickCreativeStack(saved, slotId);
    }
    @Override protected boolean isClickOutsideBounds(double mx, double my, int left, int top, int button) {
        if (search != null && mx >= catalogX && mx < catalogX + CATALOG_WIDTH && my >= y - 28 && my < y + backgroundHeight) return false;
        return super.isClickOutsideBounds(mx, my, left, top, button);
    }
    private void dragScrollbar(double my) {
        firstRow = Math.round(MathHelper.clamp((float)(my - y - GRID_Y - 8) / (ROWS * 20 - 16), 0, 1) * maxRow());
    }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScrollbar) { dragScrollbar(my); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (draggingScrollbar) { draggingScrollbar = false; return true; }
        return super.mouseReleased(mx, my, button);
    }
    @Override public boolean mouseScrolled(double mx, double my, double amount) {
        if (search != null && mx >= catalogX && mx < catalogX + CATALOG_WIDTH) {
            firstRow = MathHelper.clamp(firstRow - (int)Math.signum(amount), 0, maxRow()); return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (search != null && search.isFocused() && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)
            return search.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }
    @Override protected void handledScreenTick() {
        if (!client.interactionManager.hasCreativeInventory()) close();
    }
    @Override public boolean charTyped(char c, int modifiers) {
        return search != null && search.charTyped(c, modifiers) || super.charTyped(c, modifiers);
    }
    @Override public void removed() {
        bedrockify$rememberInventory();
        original.forEach((slot, xy) -> { slot.x = xy[0]; slot.y = xy[1]; });
        original.clear(); super.removed();
    }
}
