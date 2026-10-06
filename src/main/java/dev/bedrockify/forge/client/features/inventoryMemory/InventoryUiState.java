package dev.bedrockify.forge.client.features.inventoryMemory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Only view identifiers and offsets are persisted; no inventories or recipe catalogues. */
public final class InventoryUiState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_VIEWS = 128, MAX_GROUPS = 256;
    public record View(String category, String search, int first, Set<String> expanded) {
        public View {
            category = text(category, 128);
            search = text(search, 256);
            first = Math.max(0, Math.min(1_000_000, first));
            Set<String> groups = new TreeSet<>();
            if (expanded != null) for (String group : expanded) {
                if (group != null && !group.isEmpty()) groups.add(text(group, 128));
                if (groups.size() >= MAX_GROUPS) break;
            }
            expanded = Collections.unmodifiableSet(groups);
        }
        public View withoutSearch() {
            return search.isEmpty() ? this : new View(category, "", 0, expanded);
        }
    }
    private static final class Data {
        int version = 1;
        String creativeTab = "";
        String creativePage = "";
        Map<String, View> views = new LinkedHashMap<>();
    }
    private final Path file;
    private final Consumer<String> warning;
    private Data data = new Data();
    private String lastWritten = "";

    public InventoryUiState(Path file, Consumer<String> warning) {
        this.file = file; this.warning = warning;
        if (!Files.exists(file)) return;
        try {
            if (Files.size(file) > 1_048_576) throw new IOException("UI state exceeds 1 MiB");
            Data loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
            if (loaded == null || loaded.version != 1) return;
            data.creativeTab = text(loaded.creativeTab, 128);
            data.creativePage = text(loaded.creativePage, 128);
            if (loaded.views != null) for (var entry : loaded.views.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) remember(entry.getKey(), entry.getValue());
            }
            lastWritten = GSON.toJson(data);
        } catch (IOException | JsonParseException | IllegalArgumentException e) {
            data = new Data();
            warning.accept("Could not load inventory UI state: " + e.getMessage());
        }
    }
    private static String text(String value, int limit) {
        return value == null ? "" : value.substring(0, Math.min(value.length(), limit));
    }
    public View view(String key, boolean keepSearch) {
        View view = data.views.get(key);
        return view == null || keepSearch ? view : view.withoutSearch();
    }
    public String creativeTab() { return data.creativeTab; }
    public void creativeTab(String id) { data.creativeTab = text(id, 128); }
    public String creativePage() { return data.creativePage; }
    public void creativePage(String id) { data.creativePage = text(id, 128); }
    public void remember(String key, View view) {
        key = text(key, 160);
        if (key.isEmpty() || view == null) return;
        data.views.remove(key);
        data.views.put(key, new View(view.category, view.search, view.first, view.expanded));
        while (data.views.size() > MAX_VIEWS) data.views.remove(data.views.keySet().iterator().next());
    }
    /** Called on screen transitions, never on rendering or each typed character. */
    public void save(boolean keepSearch) {
        if (!keepSearch) data.views.replaceAll((key, view) -> view.withoutSearch());
        String json = GSON.toJson(data);
        if (json.equals(lastWritten)) return;
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            lastWritten = json;
        } catch (IOException e) {
            warning.accept("Could not save inventory UI state: " + e.getMessage());
        }
    }
}
