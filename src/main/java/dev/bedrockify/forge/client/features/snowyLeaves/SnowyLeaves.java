package dev.bedrockify.forge.client.features.snowyLeaves;

import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.mixin.featureManager.MixinFeatureManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Weather-dependent client tint and frost stage; no block states, packets or world-save changes. */
public final class SnowyLeaves {
    public static final int STEPS = 8;
    // A complete frost/thaw takes 30 seconds. Exact Bedrock timing needs live calibration.
    public static final int TRANSITION_TICKS = 600;
    private static final ColorResolver SNOW_MASK = (biome, x, z) -> supportsSnow(biome) ? 0xffffff : 0;
    private static final Set<Long> sections = ConcurrentHashMap.newKeySet();
    private static ClientWorld world;
    private static int progress;
    private static volatile int step;

    private SnowyLeaves() {}

    public static void registerColorResolvers(net.minecraftforge.client.event.RegisterColorHandlersEvent.ColorResolvers event) {
        event.register(SNOW_MASK);
    }

    public static boolean supportsSnow(Biome biome) {
        // Base climate, not altitude-adjusted temperature: mountains in warm biomes stay green.
        return biome.hasPrecipitation() && biome.getTemperature() < 0.15F;
    }

    public static boolean supported(BlockState state) {
        return state.isOf(Blocks.OAK_LEAVES) || state.isOf(Blocks.SPRUCE_LEAVES)
                || state.isOf(Blocks.BIRCH_LEAVES) || state.isOf(Blocks.JUNGLE_LEAVES)
                || state.isOf(Blocks.ACACIA_LEAVES) || state.isOf(Blocks.DARK_OAK_LEAVES)
                || state.isOf(Blocks.MANGROVE_LEAVES);
    }

    public static boolean enabled() {
        var mod = BedrockifyClient.getInstance();
        return mod != null && mod.settings != null && mod.settings.snowyLeaves
                && MixinFeatureManager.features.getOrDefault("client.features.snowyLeaves", true);
    }

    public static int tint(BlockState state, BlockRenderView view, BlockPos pos, int original) {
        if (view == null || pos == null || original < 0 || !supported(state)) return original;
        // Uses the render region's biome blend/cache, safe for chunk meshing workers.
        int mask = view.getColor(pos, SNOW_MASK) & 255;
        if (mask == 0) return original;
        sections.add(ChunkSectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4));
        if (!enabled() || MinecraftClient.getInstance().options.getGraphicsMode().getValue() == GraphicsMode.FAST)
            return original;
        return blend(original, step * mask, STEPS * 255);
    }

    public static int frostStage(BlockState state, BlockRenderView view, BlockPos pos) {
        if (!supported(state) || view == null || pos == null || !enabled()
                || MinecraftClient.getInstance().options.getGraphicsMode().getValue() == GraphicsMode.FAST) return 0;
        int mask = view.getColor(pos, SNOW_MASK) & 255;
        if (mask == 0) return 0;
        sections.add(ChunkSectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4));
        return step * mask / 255;
    }

    public static int blend(int color, int amount, int maximum) {
        amount = Math.max(0, Math.min(maximum, amount));
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        return (r + (255 - r) * amount / maximum) << 16
                | (g + (255 - g) * amount / maximum) << 8
                | b + (255 - b) * amount / maximum;
    }

    public static void tick(MinecraftClient client) {
        if (client.world != world) {
            world = client.world;
            sections.clear();
            // Joining an existing storm should not repaint newly loaded leaves green.
            progress = world != null && world.isRaining() ? TRANSITION_TICKS : 0;
            step = progress * STEPS / TRANSITION_TICKS;
        }
        if (world == null || client.isPaused()) return;
        progress = Math.max(0, Math.min(TRANSITION_TICKS, progress + (world.isRaining() ? 1 : -1)));
        int next = progress * STEPS / TRANSITION_TICKS;
        if (next != step) {
            step = next;
            if (enabled()) refreshSections(client);
        }
    }

    private static void refreshSections(MinecraftClient client) {
        if (client.player == null) return;
        int x = client.player.getBlockX() >> 4, z = client.player.getBlockZ() >> 4;
        int distance = client.options.getViewDistance().getValue() + 1;
        sections.removeIf(key -> Math.abs(ChunkSectionPos.unpackX(key) - x) > distance
                || Math.abs(ChunkSectionPos.unpackZ(key) - z) > distance);
        for (long key : sections) {
            int sx = ChunkSectionPos.unpackX(key) << 4, sy = ChunkSectionPos.unpackY(key) << 4,
                    sz = ChunkSectionPos.unpackZ(key) << 4;
            client.worldRenderer.scheduleBlockRenders(sx, sy, sz, sx + 15, sy + 15, sz + 15);
        }
    }

    public static void refresh() {
        var client = MinecraftClient.getInstance();
        if (client.world != null) refreshSections(client);
    }

    public static int currentStep() { return step; }
}
