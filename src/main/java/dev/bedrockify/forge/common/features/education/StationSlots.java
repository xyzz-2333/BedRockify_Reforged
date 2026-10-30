package dev.bedrockify.forge.common.features.education;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import java.util.function.Consumer;

/** Menu indices, distinct from the indices in a player's underlying inventory. */
public final class StationSlots {
    public static final int CHEMISTRY_INPUTS = 9, CHEMISTRY_OUTPUT = 9;
    public static final int STATION_SIZE = 10, PLAYER_START = STATION_SIZE, HOTBAR_START = PLAYER_START + 27,
            END = HOTBAR_START + 9;
    private StationSlots() {}
    public static void addPlayerInventory(Consumer<Slot> add, PlayerInventory player, ChemistryLayout layout) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            add.accept(new Slot(player, column + row * 9 + 9, layout.inventoryX() + column * 18, layout.inventoryY() + row * 18));
        for (int column = 0; column < 9; column++)
            add.accept(new Slot(player, column, layout.inventoryX() + column * 18, layout.hotbarY()));
    }
}
