package dev.bedrockify.forge.client.features.creativeInventory;

import dev.bedrockify.forge.client.gui.BedrockGui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

/** Icons copied unchanged from the Bedrock vanilla UI textures. */
public final class CreativeViewTabs {
    public static final int WIDTH = 80, HEIGHT = 32, STEP = 27;
    private static boolean forceFull;
    public static void openFull(net.minecraft.client.MinecraftClient client) {
        forceFull = true;
        client.setScreen(new net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen(client.player,
                client.player.networkHandler.getEnabledFeatures(), true));
    }
    public static boolean consumeFullRequest() {
        boolean requested = forceFull; forceFull = false; return requested;
    }
    private static final String[] ICONS = {"creative_icon", "recipe_book_icon", "inventory_icon"};
    public static void draw(DrawContext context, int x, int y, int selected) {
        for (int i = 0; i < 3; i++) {
            BedrockGui.frame(context, x + i * STEP, y, 26, HEIGHT, selected == i ? 0xffc6c6c6 : 0xff626262);
            Identifier texture = new Identifier("bedrockify", "textures/gui/inventory_views/" + ICONS[i] + ".png");
            context.drawTexture(texture, x + i * STEP + 4, y + 9, 18, 13, 0, 0, 19, 13, 19, 13);
        }
    }
    public static int hit(double mx, double my, int x, int y) {
        if (my < y || my >= y + HEIGHT || mx < x || mx >= x + WIDTH) return -1;
        int offset = (int)(mx - x);
        return offset % STEP < 26 ? offset / STEP : -1;
    }
    public static void tooltip(DrawContext context, TextRenderer renderer, int mx, int my, int x, int y, boolean holding) {
        int view = hit(mx, my, x, y);
        if (view >= 0) context.drawTooltip(renderer, Text.translatable(holding ? "bedrockify.creative.view.putDown"
                : "bedrockify.creative.view." + view), mx, my);
    }
    private CreativeViewTabs() {}
}
