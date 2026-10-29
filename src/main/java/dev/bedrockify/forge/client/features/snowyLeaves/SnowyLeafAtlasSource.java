package dev.bedrockify.forge.client.features.snowyLeaves;

import com.mojang.serialization.Codec;
import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.mixin.client.core.snowyLeafTextures.AtlasSourceManagerInvoker;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.atlas.AtlasSource;
import net.minecraft.client.texture.atlas.AtlasSourceType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.List;

/** Produces frost sprites from the active pack's leaf pixels, including animation and alpha. */
public final class SnowyLeafAtlasSource implements AtlasSource {
    public static final List<String> LEAVES = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove");
    private static final Codec<SnowyLeafAtlasSource> CODEC = Codec.unit(SnowyLeafAtlasSource::new);
    private static AtlasSourceType type;

    public static void register() {
        type = AtlasSourceManagerInvoker.bedrockify$register("bedrockify:snowy_leaves", CODEC);
    }

    public static Identifier spriteId(String leaf, int stage) {
        return new Identifier("bedrockify", "block/snowy_leaves/" + leaf + "_" + stage);
    }

    @Override public AtlasSourceType getType() { return type; }

    @Override public void load(ResourceManager manager, SpriteRegions regions) {
        for (String leaf : LEAVES) {
            var source = manager.getResource(new Identifier("minecraft", "textures/block/" + leaf + "_leaves.png"));
            if (source.isEmpty()) continue;
            for (int stage = 1; stage <= SnowyLeaves.STEPS; stage++) {
                int level = stage;
                Identifier id = spriteId(leaf, level);
                regions.add(id, () -> create(id, source.get(), level));
            }
        }
    }

    private static SpriteContents create(Identifier id, Resource resource, int stage) {
        NativeImage image = null;
        try (var stream = resource.getInputStream()) {
            image = NativeImage.read(stream);
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
                image.setColor(x, y, frostPixel(image.getColor(x, y), stage));
            var animation = resource.getMetadata().decode(AnimationResourceMetadata.READER).orElse(AnimationResourceMetadata.EMPTY);
            return new SpriteContents(id, animation.getSize(image.getWidth(), image.getHeight()), image, animation);
        } catch (IOException | RuntimeException ex) {
            if (image != null) image.close();
            Bedrockify.LOGGER.warn("Cannot create snowy leaf sprite {}", id, ex);
            return null;
        }
    }

    /** NativeImage stores ABGR. Snow brightens the surface, rather than only removing green tint. */
    public static int frostPixel(int pixel, int stage) {
        if ((pixel >>> 24) == 0) return pixel;
        int r = pixel & 255, g = pixel >> 8 & 255, b = pixel >> 16 & 255;
        // Preserve the native jungle-leaf berries, visible as yellow spots in Bedrock's frosted leaves.
        if (r > g && g - b > 32 && r - b > 64) return pixel;
        int level = Math.max(0, Math.min(SnowyLeaves.STEPS, stage));
        r += (Math.min(255, r * 2) - r) * level / SnowyLeaves.STEPS;
        g += (Math.min(255, g * 2) - g) * level / SnowyLeaves.STEPS;
        b += (Math.min(255, b * 2) - b) * level / SnowyLeaves.STEPS;
        return pixel & 0xff000000 | b << 16 | g << 8 | r;
    }
}
