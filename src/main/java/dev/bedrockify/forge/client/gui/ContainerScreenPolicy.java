package dev.bedrockify.forge.client.gui;

import net.minecraft.client.gui.screen.Screen;

/** Eligibility for a layout that relies on one exact screen and slot topology. */
public record ContainerScreenPolicy(Class<? extends Screen> screenType, int expectedSlots,
                                    int minimumWidth, int minimumHeight) {
    public enum Decision { CUSTOM, DISABLED, FOREIGN_SCREEN, EXTRA_SLOTS, SMALL_WINDOW }

    /** slotCount must describe the native layout, before adding any picker slots. */
    public Decision evaluate(Screen screen, int slotCount, boolean enabled) {
        return evaluate(screen.getClass(), slotCount, enabled, screen.width, screen.height);
    }

    /** Opening events run before Screen.init, so use the window's scaled dimensions. */
    public Decision evaluate(Class<?> actualType, int slotCount, boolean enabled, int width, int height) {
        if (!enabled) return Decision.DISABLED;
        if (actualType != screenType) return Decision.FOREIGN_SCREEN;
        if (slotCount != expectedSlots) return Decision.EXTRA_SLOTS;
        if (width < minimumWidth || height < minimumHeight) return Decision.SMALL_WINDOW;
        return Decision.CUSTOM;
    }

    public boolean supports(Screen screen, int slotCount, boolean enabled) {
        return evaluate(screen, slotCount, enabled) == Decision.CUSTOM;
    }
}
