package dev.bedrockify.forge.mixin.client.core.snowyLeafTextures;

import com.mojang.serialization.Codec;
import net.minecraft.client.texture.atlas.AtlasSource;
import net.minecraft.client.texture.atlas.AtlasSourceManager;
import net.minecraft.client.texture.atlas.AtlasSourceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Atlas decoding must remain available even when the optional weather effect is disabled. */
@Mixin(AtlasSourceManager.class)
public interface AtlasSourceManagerInvoker {
    @Invoker("register")
    static AtlasSourceType bedrockify$register(String id, Codec<? extends AtlasSource> codec) {
        throw new AssertionError();
    }
}
