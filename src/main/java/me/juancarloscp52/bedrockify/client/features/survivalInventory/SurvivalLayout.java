package me.juancarloscp52.bedrockify.client.features.survivalInventory;

import me.juancarloscp52.bedrockify.client.BedrockifyClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.IdentityHashMap;
import java.util.Map;

public final class SurvivalLayout {
    public static final int WIDTH = 218, HEIGHT = 226, RECIPE_WIDTH = 190, GAP = 30;
    private final Map<Slot, int[]> original = new IdentityHashMap<>();
    private boolean active, crafting;
    private int screenWidth, screenHeight;
    private ClickableWidget toggle;

    public boolean active() { return active; }
    public boolean crafting() { return crafting; }
    public int top() { return (screenHeight - HEIGHT) / 2; }
    public int left(boolean bookOpen) {
        return bookOpen ? (screenWidth - WIDTH - RECIPE_WIDTH - GAP) / 2 + RECIPE_WIDTH + GAP
                : (screenWidth - WIDTH) / 2;
    }
    public int recipeLeft() { return (screenWidth - WIDTH - RECIPE_WIDTH - GAP) / 2; }

    public void prepare(Screen screen, ScreenHandler handler, boolean crafting) {
        restore();
        this.screenWidth = screen.width;
        this.screenHeight = screen.height;
        this.crafting = crafting;
        var mod = BedrockifyClient.getInstance();
        active = mod != null && mod.settings != null && mod.settings.survivalInventory
                && screen.width >= WIDTH + RECIPE_WIDTH + GAP + 32 && screen.height >= HEIGHT + 24
                && handler.slots.size() == 46
                && screen.getClass() == (crafting ? CraftingScreen.class : InventoryScreen.class);
        if (!active) return;
        // Keep every native Slot and its ID. Only its client-side screen position changes.
        for (Slot slot : handler.slots) original.put(slot, new int[]{slot.x, slot.y});
        if (crafting) {
            move(handler, 0, 164, 54);
            for (int i = 0; i < 9; i++) move(handler, 1 + i, 48 + i % 3 * 20, 34 + i / 3 * 20);
            inventory(handler, 10, 37);
        } else {
            move(handler, 0, 182, 54);
            for (int i = 0; i < 4; i++) move(handler, 1 + i, 122 + i % 2 * 20, 44 + i / 2 * 20);
            for (int i = 0; i < 4; i++) move(handler, 5 + i, 14, 32 + i * 20);
            move(handler, 45, 90, 98);
            inventory(handler, 9, 36);
        }
    }

    private static void inventory(ScreenHandler handler, int first, int hotbar) {
        for (int i = 0; i < 27; i++) move(handler, first + i, 18 + i % 9 * 20, 130 + i / 9 * 20);
        for (int i = 0; i < 9; i++) move(handler, hotbar + i, 18 + i * 20, 198);
    }
    private static void move(ScreenHandler handler, int id, int x, int y) {
        Slot slot = handler.getSlot(id); slot.x = x; slot.y = y;
    }
    public void restore() {
        original.forEach((slot, xy) -> { slot.x = xy[0]; slot.y = xy[1]; });
        original.clear();
        active = false;
        toggle = null;
    }
    public void toggle(ClickableWidget widget) { toggle = widget; }
    public void positionToggle(boolean open) {
        if (toggle != null && active) {
            toggle.setX(left(open) + WIDTH - 30);
            toggle.setY(top() + 8);
        }
    }
    public static SurvivalLayout current(Screen screen) {
        return screen instanceof SurvivalScreen access && access.bedrockify$survivalLayout().active()
                ? access.bedrockify$survivalLayout() : null;
    }
}
