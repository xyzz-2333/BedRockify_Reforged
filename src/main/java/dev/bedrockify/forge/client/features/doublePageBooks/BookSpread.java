package dev.bedrockify.forge.client.features.doublePageBooks;

import dev.bedrockify.forge.client.BedrockifyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Two 160x207 pages, matching Bedrock's book_screen.json; Java's text area stays 114x128. */
public final class BookSpread {
    public static final int WIDTH = 320, HEIGHT = 207, PAGE_WIDTH = 160, TEXT_WIDTH = 114, TEXT_HEIGHT = 128;
    private static final Identifier BACK = texture("book_back");
    private static final Identifier SPINE = texture("book_spine");
    private static final Identifier LEFT = texture("book_pagecrease_left");
    private static final Identifier RIGHT = texture("book_pagecrease_right");

    private BookSpread() {}
    private static Identifier texture(String name) { return new Identifier("bedrockify", "textures/gui/book/" + name + ".png"); }
    public static boolean enabled(int width, int height) {
        var mod = BedrockifyClient.getInstance();
        return mod != null && mod.settings != null && mod.settings.doublePageBooks && width >= 340 && height >= 250;
    }
    public static int firstPage(int active) { return active / 2 * 2; }
    public static int left(int width) { return (width - WIDTH) / 2; }
    public static int top(int height) { return Math.max(8, (height - HEIGHT - 28) / 2); }
    public static int textX(int width, int side) { return left(width) + 23 + side * PAGE_WIDTH; }
    public static int textY(int height) { return top(height) + 32; }
    public static int sideAt(int width, int height, double x, double y) {
        if (y < textY(height) || y >= textY(height) + TEXT_HEIGHT) return -1;
        for (int side = 0; side < 2; side++)
            if (x >= textX(width, side) && x < textX(width, side) + TEXT_WIDTH) return side;
        return -1;
    }
    public static void draw(DrawContext context, int width, int height) {
        int x = left(width), y = top(height);
        nineSlice(context, BACK, x, y, WIDTH, HEIGHT, 28, 28, 13, 13);
        nineSlice(context, LEFT, x + 16, y + 6, 128, HEIGHT - 14, 110, 14, 7, 6);
        nineSlice(context, RIGHT, x + 176, y + 6, 128, HEIGHT - 14, 110, 14, 7, 6);
        context.drawTexture(SPINE, x + WIDTH / 2 - 4, y + 1, 8, HEIGHT - 2, 0, 0, 8, 1, 8, 1);
    }

    private static void nineSlice(DrawContext c, Identifier texture, int x, int y, int w, int h,
                                  int sourceW, int sourceH, int borderX, int borderY) {
        int[] xs = {0, borderX, w - borderX, w}, ys = {0, borderY, h - borderY, h};
        int[] us = {0, borderX, sourceW - borderX, sourceW}, vs = {0, borderY, sourceH - borderY, sourceH};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            c.drawTexture(texture, x + xs[col], y + ys[row], xs[col + 1] - xs[col], ys[row + 1] - ys[row],
                    us[col], vs[row], us[col + 1] - us[col], vs[row + 1] - vs[row], sourceW, sourceH);
    }
}
