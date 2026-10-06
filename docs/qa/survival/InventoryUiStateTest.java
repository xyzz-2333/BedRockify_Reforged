import dev.bedrockify.forge.client.features.inventoryMemory.InventoryUiState;
import java.nio.file.*;
import java.util.*;

/** Standalone checks of the actual persistence class, without a game or replacement stubs. */
public class InventoryUiStateTest {
    private static int assertions;
    private static void check(boolean value) { assertions++; if (!value) throw new AssertionError(assertions); }
    public static void main(String[] args) throws Exception {
        Path file = Files.createTempDirectory("bedrockify-ui-test").resolve("ui.json");
        List<String> warnings = new ArrayList<>();
        InventoryUiState state = new InventoryUiState(file, warnings::add);
        check(state.view("survival:inventory", true) == null);
        state.creativeTab("test:tab_11"); state.creativePage("test:tab_7");
        state.remember("survival:inventory", new InventoryUiState.View("NATURE", "木板", 96, Set.of("planks", "logs")));
        state.remember("survival:crafting", new InventoryUiState.View("ITEMS", "ingot", 48, Set.of("ingots")));
        state.save(true);
        InventoryUiState reloaded = new InventoryUiState(file, warnings::add);
        check(reloaded.creativeTab().equals("test:tab_11")); check(reloaded.creativePage().equals("test:tab_7"));
        var inventory = reloaded.view("survival:inventory", true);
        check(inventory.category().equals("NATURE")); check(inventory.search().equals("木板"));
        check(inventory.first() == 96 && inventory.expanded().equals(Set.of("planks", "logs")));
        check(reloaded.view("survival:crafting", true).first() == 48);
        check(reloaded.view("survival:inventory", false).search().isEmpty());
        check(reloaded.view("survival:inventory", false).first() == 0);
        reloaded.save(false);
        check(new InventoryUiState(file, warnings::add).view("survival:inventory", true).search().isEmpty());
        String before = Files.readString(file); var stamp = Files.getLastModifiedTime(file);
        reloaded.save(false); check(Files.readString(file).equals(before)); check(Files.getLastModifiedTime(file).equals(stamp));
        check(warnings.isEmpty());
        for (int i = 0; i < 200; i++) reloaded.remember("creative:test:" + i, new InventoryUiState.View(null, null, -1, null));
        reloaded.save(true);
        check(reloaded.view("creative:test:0", true) == null); check(reloaded.view("creative:test:199", true).first() == 0);
        Files.writeString(file, "{broken");
        InventoryUiState damaged = new InventoryUiState(file, warnings::add);
        check(!warnings.isEmpty() && damaged.view("survival:inventory", true) == null);
        damaged.remember("test", new InventoryUiState.View("invalid", "x".repeat(300), Integer.MAX_VALUE, Set.of("planks")));
        damaged.save(true);
        check(new InventoryUiState(file, warnings::add).view("test", true).search().length() == 256);
        check(damaged.view("test", true).first() == 1_000_000);
        Files.writeString(file, "{\"version\":1,\"creativeTab\":null,\"views\":null}");
        check(new InventoryUiState(file, warnings::add).creativeTab().isEmpty());
        System.out.println("PASS: " + assertions + " persistence assertions");
    }
}
