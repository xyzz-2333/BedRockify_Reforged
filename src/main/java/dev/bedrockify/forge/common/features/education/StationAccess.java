package dev.bedrockify.forge.common.features.education;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;

/** Transient single-user access. A closed, invalid or disconnected menu releases itself. */
public final class StationAccess {
    private PlayerEntity user;
    private ScreenHandler menu;
    public boolean inUse() {
        if (user != null && (user.isRemoved() || user.currentScreenHandler != menu || !menu.canUse(user))) { user = null; menu = null; }
        return user != null;
    }
    public void opened(PlayerEntity player, ScreenHandler handler) {
        if (handler != player.playerScreenHandler) { user = player; menu = handler; }
    }
    public boolean allows(PlayerEntity player, ScreenHandler handler) { return user == null || user == player && menu == handler; }
    public void closed(PlayerEntity player, ScreenHandler handler) {
        if (user == player && menu == handler) { user = null; menu = null; }
    }
}
