package me.juancarloscp52.bedrockify.client.features.survivalInventory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;

/** Original nine-slice textures, intentionally overridable by resource packs. */
public final class SurvivalSprites {
    public static final Identifier TEXTURE = new Identifier("bedrockify", "textures/gui/survival.png");
    private SurvivalSprites() {}

    public static void panel(DrawContext context, int x, int y, int width, int height, boolean dark) {
        tile(context, x, y, width, height, dark ? 16 : 0, 0);
    }
    public static void cell(DrawContext context, int x, int y, boolean unavailable) {
        context.drawTexture(TEXTURE, x, y, unavailable ? 20 : 0, 20, 20, 20, 64, 64);
    }
    public static void button(DrawContext context, int x, int y, int width, int height, boolean selected) {
        tile(context, x, y, width, height, selected ? 16 : 0, 44);
    }
    private static void tile(DrawContext c, int x, int y, int w, int h, int u, int v) {
        int b = 3, n = 16;
        c.drawTexture(TEXTURE, x, y, b, b, u, v, b, b, 64, 64);
        c.drawTexture(TEXTURE, x + w - b, y, b, b, u + n - b, v, b, b, 64, 64);
        c.drawTexture(TEXTURE, x, y + h - b, b, b, u, v + n - b, b, b, 64, 64);
        c.drawTexture(TEXTURE, x + w - b, y + h - b, b, b, u + n - b, v + n - b, b, b, 64, 64);
        c.drawTexture(TEXTURE, x + b, y, w - b * 2, b, u + b, v, n - b * 2, b, 64, 64);
        c.drawTexture(TEXTURE, x + b, y + h - b, w - b * 2, b, u + b, v + n - b, n - b * 2, b, 64, 64);
        c.drawTexture(TEXTURE, x, y + b, b, h - b * 2, u, v + b, b, n - b * 2, 64, 64);
        c.drawTexture(TEXTURE, x + w - b, y + b, b, h - b * 2, u + n - b, v + b, b, n - b * 2, 64, 64);
        c.drawTexture(TEXTURE, x + b, y + b, w - b * 2, h - b * 2, u + b, v + b, n - b * 2, n - b * 2, 64, 64);
    }
    public static void inventory(DrawContext context, ScreenHandler handler, int x, int y) {
        panel(context, x, y, SurvivalLayout.WIDTH, SurvivalLayout.HEIGHT, false);
        for (var slot : handler.slots) cell(context, x + slot.x - 2, y + slot.y - 2, false);
    }
}
