package dev.bedrockify.forge.client.features.snowyLeaves;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Model data carries the position-specific frost stage; inventory/model geometry stays native. */
public final class SnowyLeafModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> FROST = new ModelProperty<>();
    private record QuadKey(BakedQuad quad, int stage) {}
    private final Map<QuadKey, BakedQuad> quads = new ConcurrentHashMap<>();

    private SnowyLeafModel(BakedModel original) { super(original); }

    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<BakedModel, BakedModel> wrappers = new IdentityHashMap<>();
        event.getModels().replaceAll((id, model) -> {
            boolean leaf = id.getNamespace().equals("minecraft")
                    && SnowyLeafAtlasSource.LEAVES.stream().anyMatch(name -> id.getPath().equals(name + "_leaves"));
            return leaf ? wrappers.computeIfAbsent(model, SnowyLeafModel::new) : model;
        });
    }

    @Override public ModelData getModelData(BlockRenderView view, BlockPos pos, BlockState state, ModelData data) {
        ModelData original = super.getModelData(view, pos, state, data);
        return original.derive().with(FROST, SnowyLeaves.frostStage(state, view, pos)).build();
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, Random random, ModelData data, RenderLayer layer) {
        var original = super.getQuads(state, side, random, data, layer);
        Integer stage = data.get(FROST);
        if (stage == null || stage == 0 || !SnowyLeaves.enabled()) return original;
        return original.stream().map(quad -> quads.computeIfAbsent(new QuadKey(quad, stage), this::frostQuad)).toList();
    }

    private BakedQuad frostQuad(QuadKey key) {
        BakedQuad original = key.quad;
        Sprite source = original.getSprite();
        var id = source.getContents().getId();
        String path = id.getPath();
        if (!id.getNamespace().equals("minecraft") || !path.startsWith("block/") || !path.endsWith("_leaves")) return original;
        String leaf = path.substring(6, path.length() - 7);
        if (!SnowyLeafAtlasSource.LEAVES.contains(leaf)) return original;
        Sprite target = MinecraftClient.getInstance().getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(SnowyLeafAtlasSource.spriteId(leaf, key.stage));
        if (!target.getContents().getId().equals(SnowyLeafAtlasSource.spriteId(leaf, key.stage))) return original;
        int[] vertices = original.getVertexData().clone();
        int stride = vertices.length / 4;
        if (stride < 6 || vertices.length % 4 != 0) return original;
        for (int i = 0; i < 4; i++) {
            int offset = i * stride;
            float u = Float.intBitsToFloat(vertices[offset + 4]), v = Float.intBitsToFloat(vertices[offset + 5]);
            u = target.getMinU() + (u - source.getMinU()) / (source.getMaxU() - source.getMinU()) * (target.getMaxU() - target.getMinU());
            v = target.getMinV() + (v - source.getMinV()) / (source.getMaxV() - source.getMinV()) * (target.getMaxV() - target.getMinV());
            vertices[offset + 4] = Float.floatToRawIntBits(u);
            vertices[offset + 5] = Float.floatToRawIntBits(v);
        }
        return new BakedQuad(vertices, original.getColorIndex(), original.getFace(), target, original.hasShade(), original.hasAmbientOcclusion());
    }
}
