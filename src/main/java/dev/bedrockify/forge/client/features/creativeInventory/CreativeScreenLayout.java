package dev.bedrockify.forge.client.features.creativeInventory;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.math.MathHelper;
import dev.bedrockify.forge.client.gui.GridViewport;
import java.util.List;

/** Geometry and control placement, independent of the vanilla screen's private fields. */
public record CreativeScreenLayout(boolean classic, int columns, int rows, int left, int top, int width, int height) {
    public static CreativeScreenLayout calculate(int screenWidth, int screenHeight, boolean classic) {
        int columns = MathHelper.clamp((screenWidth - 70) / 20, 9, 17);
        // Leave room above and below for Forge's native tabs and page controls.
        int rows = MathHelper.clamp((screenHeight - 170) / 20, 3, 7);
        int width = classic ? columns * 20 + 32 : 195;
        int height = classic ? rows * 20 + 70 : 136;
        return new CreativeScreenLayout(classic, columns, rows, (screenWidth - width) / 2,
                (screenHeight - height) / 2, width, height);
    }
    public int viewTabY() { return width >= 250 ? top - 28 : Math.max(2, top - 62); }
    public float restoredScroll(int firstIndex, int entries) {
        int overflow = new GridViewport(columns, rows).maxRow(entries);
        return overflow == 0 ? 0 : MathHelper.clamp((float)(firstIndex / columns) / overflow, 0, 1);
    }
    public boolean scrollbarContains(double x, double y) {
        return x >= left + width - 16 && x < left + width - 6 && y >= top + 28 && y < top + 28 + rows * 20;
    }
    public float draggedScroll(double mouseY) {
        return MathHelper.clamp(((float)mouseY - top - 28 - 8) / (rows * 20 - 16), 0, 1);
    }
    public void positionControls(TextFieldWidget search, ItemGroup selected, List<? extends Element> children) {
        if (search != null) {
            search.setWidth(classic ? width - 124 : selected.getSearchBarWidth());
            search.setX(classic ? left + 12 : left + 171 - search.getWidth());
            search.setY(top + (classic ? 10 : 6));
            search.setEditableColor(classic ? 0xff303030 : 0xffffff);
        }
        for (Element element : children) if (element instanceof ButtonWidget button) {
            String label = button.getMessage().getString();
            if (label.equals("<") || label.equals(">")) {
                button.setX(label.equals("<") ? left : left + width - 20);
                button.setY(top - 50);
            }
        }
    }
}
