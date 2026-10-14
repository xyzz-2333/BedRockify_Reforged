package dev.bedrockify.forge.client.features.biomeWaterColors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.mixin.featureManager.MixinFeatureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Client-only palette; never modifies biome registry data or saved worlds. */
public final class BedrockWaterColors implements SynchronousResourceReloader {
    public record Colors(int surface, int fog) {}
    private record LookupCache(Registry<Biome> registry, Map<Biome, Optional<Identifier>> ids) {}
    private static final Identifier RESOURCE = new Identifier("bedrockify", "water_colors.json");
    private static volatile Map<Identifier, Colors> palette = Map.of();
    private static volatile LookupCache lookupCache;

    public static boolean enabled() {
        var mod = BedrockifyClient.getInstance();
        return mod != null && mod.settings != null && mod.settings.bedrockWaterColors
                && MixinFeatureManager.features.getOrDefault("client.features.biomeWaterColors", true)
                && !palette.isEmpty();
    }
    public static Colors lookup(Identifier id) { return id == null ? null : palette.get(id); }
    public static int size() { return palette.size(); }
    public static Colors forBiome(Biome biome) {
        if (!enabled()) return null;
        var world = MinecraftClient.getInstance().world;
        if (world == null) return null;
        var registry = world.getRegistryManager().get(RegistryKeys.BIOME);
        var cache = lookupCache;
        if (cache == null || cache.registry != registry) {
            cache = new LookupCache(registry, new ConcurrentHashMap<>());
            lookupCache = cache;
        }
        // Meshing can query biomes from several worker threads. Cache only IDs so reloads
        // cannot leave old palette values in in-flight cache computations.
        return lookup(cache.ids.computeIfAbsent(biome, key -> Optional.ofNullable(registry.getId(key))).orElse(null));
    }
    public static void refreshWorldColors() {
        var client = MinecraftClient.getInstance();
        if (client.world != null) {
            client.world.reloadColor();
            client.worldRenderer.reload();
        }
    }
    @Override
    public void reload(ResourceManager manager) {
        Map<Identifier, Colors> next = new HashMap<>();
        for (var resource : manager.getAllResources(RESOURCE)) {
            try (var reader = resource.getReader()) {
                Map<Identifier, Colors> candidate = new HashMap<>(next);
                merge(JsonParser.parseReader(reader).getAsJsonObject(), candidate);
                next = candidate;
            } catch (Exception ex) {
                Bedrockify.LOGGER.warn("Ignoring invalid water palette from {}: {}", resource.getResourcePackName(), ex.getMessage());
            }
        }
        palette = Map.copyOf(next);
        Bedrockify.LOGGER.info("Loaded {} biome water color mappings", palette.size());
        MinecraftClient.getInstance().execute(BedrockWaterColors::refreshWorldColors);
    }
    /** A higher-priority resource pack may override fields or remove entries with null. */
    public static void merge(JsonObject root, Map<Identifier, Colors> target) {
        if (!root.has("format") || root.get("format").getAsInt() != 1)
            throw new IllegalArgumentException("Unsupported water palette format");
        for (var entry : root.getAsJsonObject("biomes").entrySet()) {
            var id = new Identifier(entry.getKey());
            if (entry.getValue().isJsonNull()) { target.remove(id); continue; }
            var json = entry.getValue().getAsJsonObject();
            var previous = target.get(id);
            int surface = json.has("surface") ? parseColor(json.get("surface").getAsString()) : requirePrevious(previous).surface;
            int fog = json.has("fog") ? parseColor(json.get("fog").getAsString()) : requirePrevious(previous).fog;
            target.put(id, new Colors(surface, fog));
        }
    }
    private static Colors requirePrevious(Colors previous) {
        if (previous == null) throw new IllegalArgumentException("New biome entry requires surface and fog colors");
        return previous;
    }
    private static int parseColor(String text) {
        if (!text.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Expected #RRGGBB color: " + text);
        return Integer.parseInt(text.substring(1), 16);
    }
}
