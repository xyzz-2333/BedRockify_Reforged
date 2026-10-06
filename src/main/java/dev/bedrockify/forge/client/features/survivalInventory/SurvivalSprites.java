package dev.bedrockify.forge.client.features.survivalInventory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;

/** Classic pixel geometry, using the original resource-pack-overridable atlas. */
public final class SurvivalSprites {
    public static final Identifier TEXTURE = new Identifier("bedrockify", "textures/gui/survival.png");
    private SurvivalSprites() {}

    public static void panel(DrawContext context, int x, int y, int width, int height, boolean dark) {
        if (dark) {
            context.drawTexture(TEXTURE, x, y, width, height, 18, 2, 12, 12, 64, 64);
            sample(context, x, y, width, 1, 1, 21);
            sample(context, x, y, 1, height, 1, 21);
            return;
        }
        // Stepped corners and a single bright rim, rather than nested square bevels.
        sample(context, x + 2, y, width - 4, height, 0, 0);
        sample(context, x, y + 2, width, height - 4, 0, 0);
        sample(context, x + 1, y + 1, width - 2, height - 2, 0, 0);
        context.drawTexture(TEXTURE, x + 2, y + 2, width - 4, height - 4, 2, 2, 12, 12, 64, 64);
        sample(context, x + 2, y + 1, width - 4, 1, 1, 1);
        sample(context, x + 1, y + 2, 1, height - 4, 1, 1);
        sample(context, x + 2, y + height - 3, width - 4, 2, 4, 14);
        sample(context, x + width - 3, y + 2, 2, height - 4, 14, 4);
    }
    public static void cell(DrawContext context, int x, int y, boolean unavailable) {
        int u = unavailable ? 20 : 0;
        context.drawTexture(TEXTURE, x + 1, y + 1, 18, 18, u + 2, 22, 16, 16, 64, 64);
        sample(context, x, y, 20, 1, u + 1, 21);
        sample(context, x, y, 1, 20, u + 1, 21);
        sample(context, x + 1, y + 19, 19, 1, 1, 1);
        sample(context, x + 19, y + 1, 1, 19, 1, 1);
    }
    public static void button(DrawContext context, int x, int y, int width, int height, boolean selected) {
        tile(context, x, y, width, height, selected ? 16 : 0, 44);
    }
    private static void sample(DrawContext c, int x, int y, int w, int h, int u, int v) {
        c.drawTexture(TEXTURE, x, y, w, h, u, v, 1, 1, 64, 64);
    }
    public static void frame(DrawContext c, int x, int y, int w, int h, int face) {
        c.fill(x + 2, y, x + w - 2, y + h, 0xff151515);
        c.fill(x, y + 2, x + w, y + h - 2, 0xff151515);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xff151515);
        c.fill(x + 2, y + 2, x + w - 2, y + h - 2, face);
        c.fill(x + 2, y + 1, x + w - 2, y + 2, 0xffffffff);
        c.fill(x + 1, y + 2, x + 2, y + h - 2, 0xffffffff);
        c.fill(x + 2, y + h - 3, x + w - 2, y + h - 1, 0xff555555);
        c.fill(x + w - 3, y + 2, x + w - 1, y + h - 2, 0xff555555);
    }
    public static void pageArrow(DrawContext c, int x, int y, boolean right, boolean enabled) {
        c.getMatrices().push();
        c.getMatrices().translate(0, 0, 200);
        int color = enabled ? 0xfff0f0f0 : 0xff303030;
        for (int col = 0; col < 5; col++) {
            int dx = right ? col : 4 - col;
            c.fill(x + 7 + dx, y + 5 + col, x + 8 + dx, y + 15 - col, color);
        }
        c.getMatrices().pop();
    }
    public static void craftingArrow(DrawContext c, int x, int y, int width, int height) {
        int half = height / 2, head = width - half, color = 0xff8b8b8b;
        c.fill(x, y + half - 2, x + head + 1, y + half + 2, color);
        for (int row = 0; row < height; row++) {
            int length = Math.min(row, height - row - 1) + 1;
            c.fill(x + head, y + row, x + head + length, y + row + 1, color);
        }
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
