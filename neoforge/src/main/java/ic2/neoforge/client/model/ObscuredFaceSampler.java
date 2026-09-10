package ic2.neoforge.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client counterpart of ItemObscurator.getRenderInfo: samples the rendered face of a reference
 * block state into flat texture rectangles (sprite-space UV corners) for the wall overlays. Regular
 * unit faces are preferred; otherwise every opaque quad contributes its first diagonal.
 */
public final class ObscuredFaceSampler {
    public record Sample(
            TextureAtlasSprite sprite,
            ChunkSectionLayer layer,
            RenderType itemRenderType,
            float uStart,
            float vStart,
            float uEnd,
            float vEnd) {}

    public static final class RenderInfo {
        public final Sample[] samples;

        RenderInfo(Sample[] samples) {
            this.samples = samples;
        }
    }

    private static final float EPSILON = 1.0e-4f;

    private ObscuredFaceSampler() {}

    public static @Nullable RenderInfo renderInfo(BlockState state, Direction side) {
        BlockStateModel model =
                Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) quads.addAll(part.getQuads(side));
        if (quads.isEmpty()) return null;

        List<Sample> regular = new ArrayList<>();
        for (BakedQuad quad : quads) {
            Sample sample = regularFaceSample(quad, side);
            if (sample != null) regular.add(sample);
        }
        if (!regular.isEmpty()) return new RenderInfo(regular.toArray(Sample[]::new));

        List<Sample> fallback = new ArrayList<>();
        for (BakedQuad quad : quads) {
            if (quad.materialInfo().layer() == ChunkSectionLayer.TRANSLUCENT) return null;
            fallback.add(cornerSample(quad, 0));
        }
        return fallback.isEmpty() ? null : new RenderInfo(fallback.toArray(Sample[]::new));
    }

    /**
     * Legacy regular-face check: a unit square lying in the face plane contributes its diagonal.
     */
    private static @Nullable Sample regularFaceSample(BakedQuad quad, Direction side) {
        if (quad.materialInfo().layer() == ChunkSectionLayer.TRANSLUCENT) return null;
        float xShift = (side.getStepX() + 1) / 2.0f;
        float yShift = (side.getStepY() + 1) / 2.0f;
        float zShift = (side.getStepZ() + 1) / 2.0f;
        int firstVertex = -1;
        for (int vertex = 0; vertex < 4; vertex++) {
            if (near(quad.position(vertex).x(), xShift)
                    && near(quad.position(vertex).y(), yShift)
                    && near(quad.position(vertex).z(), zShift)) {
                firstVertex = vertex;
                break;
            }
        }
        if (firstVertex == -1) return null;
        Vector3fc v1 =
                quad.position((firstVertex + 1) % 4)
                        .sub(quad.position(firstVertex), new Vector3f());
        if (!near(v1.lengthSquared(), 1.0f)) return null;
        Vector3fc v4 =
                quad.position((firstVertex + 3) % 4)
                        .sub(quad.position(firstVertex), new Vector3f());
        if (!near(v4.lengthSquared(), 1.0f)) return null;
        Vector3fc v3 =
                quad.position((firstVertex + 2) % 4)
                        .sub(quad.position((firstVertex + 3) % 4), new Vector3f());
        if (!near(v3.add(v1, new Vector3f()).lengthSquared(), 0.0f)) return null;
        Vector3fc normal = v1.cross(v4, new Vector3f());
        if (!near(normal.x() - side.getStepX(), 0.0f)
                || !near(normal.y() - side.getStepY(), 0.0f)
                || !near(normal.z() - side.getStepZ(), 0.0f)) {
            return null;
        }
        return cornerSample(quad, firstVertex);
    }

    private static Sample cornerSample(BakedQuad quad, int firstVertex) {
        TextureAtlasSprite sprite = quad.materialInfo().sprite();
        float uOrigin = sprite.getU0();
        float vOrigin = sprite.getV0();
        float uWidth = sprite.getU1() - uOrigin;
        float vWidth = sprite.getV1() - vOrigin;
        int opposite = (firstVertex + 2) % 4;
        float uStart = atlasToSpriteU(quad.packedUV(firstVertex), uOrigin, uWidth);
        float vStart = atlasToSpriteV(quad.packedUV(firstVertex), vOrigin, vWidth);
        float uEnd = atlasToSpriteU(quad.packedUV(opposite), uOrigin, uWidth);
        float vEnd = atlasToSpriteV(quad.packedUV(opposite), vOrigin, vWidth);
        return new Sample(
                sprite,
                quad.materialInfo().layer(),
                quad.materialInfo().itemRenderType(),
                uStart,
                vStart,
                uEnd,
                vEnd);
    }

    private static float atlasToSpriteU(long packedUV, float origin, float size) {
        return (UVPair.unpackU(packedUV) - origin) / size;
    }

    private static float atlasToSpriteV(long packedUV, float origin, float size) {
        return (UVPair.unpackV(packedUV) - origin) / size;
    }

    private static boolean near(float value, float expected) {
        return Math.abs(value - expected) < EPSILON;
    }
}
