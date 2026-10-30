package dev.bedrockify.forge.client.features.creativeInventory;

import dev.bedrockify.forge.client.gui.BedrockGui;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemGroup;
import net.minecraft.text.Text;

/** Classic creative-view painting; the Mixin retains native tab order and input hooks. */
public final class CreativeScreenRenderer {
    private CreativeScreenRenderer() {}
    public static void background(DrawContext context, CreativeScreenLayout layout, TextFieldWidget searchBox,
                                  int first, int itemCount, boolean scrollbar, float scrollPosition) {
        int x = layout.left(), y = layout.top(), backgroundWidth = layout.width();
        int mainHeight = 38 + layout.rows() * 20;
        int hotbarLeft = (backgroundWidth - 180) / 2;
        panel(context, x, y, backgroundWidth, mainHeight, 0xffc6c6c6);
        panel(context, x + hotbarLeft - 8, y + mainHeight - 2, 196, 34, 0xffc6c6c6);
        context.fill(x + 8, y + 26, x + backgroundWidth - 8, y + mainHeight - 4, 0xff626262);
        for (int cell = 0; cell < layout.columns() * layout.rows(); cell++) {
            if (first + cell >= itemCount) break;
            slot(context, x + 12 + cell % layout.columns() * 20, y + 28 + cell / layout.columns() * 20);
        }
        for (int i = 0; i < 9; i++) slot(context, x + hotbarLeft + i * 20, y + 42 + layout.rows() * 20);
        if (searchBox.isVisible()) {
            context.fill(searchBox.getX() - 3, searchBox.getY() - 3, searchBox.getX() + searchBox.getWidth() + 3, searchBox.getY() + 12, 0xff8b8b8b);
            context.fill(searchBox.getX() - 2, searchBox.getY() - 2, searchBox.getX() + searchBox.getWidth() + 2, searchBox.getY() + 11, 0xfff0f0f0);
        }
        if (scrollbar) {
            int left = x + backgroundWidth - 16;
            context.fill(left, y + 28, left + 10, y + 28 + layout.rows() * 20, 0xff373737);
            int top = y + 28 + Math.round((layout.rows() * 20 - 16) * scrollPosition);
            panel(context, left, top, 10, 16, 0xffc6c6c6);
        }
    }
    public static void foreground(DrawContext context, TextRenderer textRenderer, CreativeScreenLayout layout,
                                  ItemGroup selectedTab, TextFieldWidget searchBox, CreativeScreenContent content,
                                  CreativeScreenMemory memory, int first) {
        int backgroundWidth = layout.width();
        CreativeCatalog.Category category = CreativeCatalog.category(selectedTab);
        Text title = category == null ? selectedTab.getDisplayName() : category.title();
        int titleWidth = searchBox.isVisible() ? (content.hasGroups() ? 54 : 98) : backgroundWidth - 80;
        if (textRenderer.getWidth(title) > titleWidth) title = Text.literal(textRenderer.trimToWidth(title.getString(), titleWidth - 10) + "…");
        int titleX = searchBox.isVisible() ? backgroundWidth - textRenderer.getWidth(title) - (content.hasGroups() ? 60 : 12) : 12;
        context.drawText(textRenderer, title, titleX, 10, 0xff404040, false);
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300);
        for (int cell = 0; cell < layout.columns() * layout.rows() && first + cell < content.entries().size(); cell++) {
            CreativeGroups.Entry entry = content.entries().get(first + cell);
            if (!entry.header()) continue;
            int left = 12 + cell % layout.columns() * 20 + 10;
            int top = 28 + cell / layout.columns() * 20 + 10;
            String marker = memory.isExpanded(entry.group()) ? "−" : "+";
            // Outline the glyph itself; Bedrock does not put a black tile behind group markers.
            context.drawText(textRenderer, marker, left + 1, top, 0xff000000, false);
            context.drawText(textRenderer, marker, left + 3, top, 0xff000000, false);
            context.drawText(textRenderer, marker, left + 2, top - 1, 0xff000000, false);
            context.drawText(textRenderer, marker, left + 2, top + 1, 0xff000000, false);
            context.drawText(textRenderer, marker, left + 2, top, 0xffffffff, false);
        }
        if (content.hasGroups()) {
            panel(context, backgroundWidth - 54, 6, 18, 16, 0xff8b8b8b);
            panel(context, backgroundWidth - 34, 6, 18, 16, 0xff8b8b8b);
            context.drawText(textRenderer, "+", backgroundWidth - 48, 9, 0xffffffff, false);
            context.drawText(textRenderer, "−", backgroundWidth - 28, 9, 0xffffffff, false);
        }
        context.getMatrices().pop();
    }
    public static void tab(DrawContext context, ItemGroup group, boolean selected, int left, int top) {
        panel(context, left, top, 26, 32, selected ? 0xffc6c6c6 : 0xff626262);
        CreativeCatalog.Category category = CreativeCatalog.category(group);
        context.drawItem(category == null ? group.getIcon() : category.icon, left + 5, top + 8);
    }
    public static void search(DrawContext context, TextFieldWidget searchBox, int mouseX, int mouseY, float delta) {
        // Render once, above the panel and item passes. Buffered text in drawBackground
        // could be covered by later GUI batches, leaving only fragments of its shadow.
        context.draw();
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 200);
        context.setShaderColor(1, 1, 1, 1);
        context.enableScissor(searchBox.getX() - 1, searchBox.getY() - 1,
                searchBox.getX() + searchBox.getWidth() + 1, searchBox.getY() + 12);
        searchBox.render(context, mouseX, mouseY, delta);
        context.draw();
        context.disableScissor();
        context.getMatrices().pop();
        }
    private static void panel(DrawContext context, int x, int y, int w, int h, int color) {
        BedrockGui.frame(context, x, y, w, h, color);
    }
    private static void slot(DrawContext context, int x, int y) { BedrockGui.cell(context, x, y, false); }
}
