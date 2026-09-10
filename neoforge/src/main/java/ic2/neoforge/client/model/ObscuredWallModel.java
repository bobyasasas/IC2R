package ic2.neoforge.client.model;

import com.mojang.serialization.MapCodec;

import ic2.neoforge.machine.ObscuredWallBlockEntity;
import ic2.neoforge.model.WallRenderState;
import ic2.neoforge.registration.ModFoam;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of the legacy WallModelForge: every face renders the wall color's foam wall plus one flat
 * overlay per sampled texture, offset slightly off the surface. Geometry comes from the model data
 * published by ObscuredWallBlockEntity.
 */
public final class ObscuredWallModel {
    public static final String ID = "obscured_wall";

    private static final float OVERLAY_OFFSET = 0.001f;
    private static final byte[][] BLOCK_UV_MAP =
            new byte[][] {
                {1, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 1, 0},
                {0, 0, 1, 0, 1, 0}
            };

    public record Unbaked() implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return CODEC;
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            return new Model();
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {}
    }

    private static final class Model implements BlockStateModel {
        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            output.add(new Part(WallRenderState.of(DyeColor.LIGHT_GRAY, null)));
        }

        @Override
        public void collectParts(
                BlockAndTintGetter level,
                BlockPos pos,
                BlockState state,
                RandomSource random,
                List<BlockStateModelPart> output) {
            var data = level.getModelData(pos);
            WallRenderState render =
                    data == net.neoforged.neoforge.model.data.ModelData.EMPTY
                            ? null
                            : data.get(ObscuredWallBlockEntity.RENDER_STATE);
            output.add(
                    new Part(
                            render == null
                                    ? WallRenderState.of(DyeColor.LIGHT_GRAY, null)
                                    : render));
        }

        @Override
        public Material.Baked particleMaterial() {
            return baseModel(
                            ModFoam.WALLS
                                    .get(DyeColor.LIGHT_GRAY.getName())
                                    .get()
                                    .defaultBlockState())
                    .particleMaterial();
        }

        @Override
        public Material.Baked particleMaterial(
                BlockAndTintGetter level, BlockPos pos, BlockState state) {
            return particleMaterial();
        }

        @Override
        public int materialFlags() {
            return 0;
        }
    }

    private static BlockStateModel baseModel(BlockState state) {
        BlockStateModelSet models =
                Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        return models.get(state);
    }

    private static final class Part implements BlockStateModelPart {
        private final List<List<BakedQuad>> faces;

        Part(WallRenderState render) {
            this.faces = buildFaces(render);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            if (side == null) return List.of();
            return this.faces.get(side.ordinal());
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public Material.Baked particleMaterial() {
            return baseModel(
                            ModFoam.WALLS
                                    .get(DyeColor.LIGHT_GRAY.getName())
                                    .get()
                                    .defaultBlockState())
                    .particleMaterial();
        }

        @Override
        public int materialFlags() {
            return 0;
        }

        private static List<List<BakedQuad>> buildFaces(WallRenderState render) {
            BlockStateModel base =
                    baseModel(
                            ModFoam.WALLS.get(render.color().getName()).get().defaultBlockState());
            List<BlockStateModelPart> parts = new ArrayList<>();
            base.collectParts(RandomSource.create(42L), parts);
            List<List<BakedQuad>> faces = new ArrayList<>(6);
            for (Direction side : Direction.values()) {
                List<BakedQuad> merged = new ArrayList<>();
                for (BlockStateModelPart part : parts) merged.addAll(part.getQuads(side));
                ObscuredWallBlockEntity.FaceData data = render.face(side.ordinal());
                if (data != null) {
                    ObscuredFaceSampler.RenderInfo info =
                            ObscuredFaceSampler.renderInfo(
                                    data.referenceState(), data.referenceSide());
                    if (info != null) {
                        for (int texture = 0; texture < info.samples.length; texture++) {
                            merged.add(
                                    overlayQuad(
                                            side,
                                            info.samples[texture],
                                            data.colorMultipliers()[texture]));
                        }
                    }
                }
                faces.add(List.copyOf(merged));
            }
            return faces;
        }

        private static BakedQuad overlayQuad(
                Direction face, ObscuredFaceSampler.Sample sample, int colorMultiplier) {
            MutableQuad quad = new MutableQuad();
            float neg = -OVERLAY_OFFSET;
            float pos = 1.0f + OVERLAY_OFFSET;
            quad.setCubeFace(face, new Vector3f(neg, neg, neg), new Vector3f(pos, pos, pos));
            quad.setSprite(sample.sprite(), sample.layer(), sample.itemRenderType());
            byte[] map = BLOCK_UV_MAP[face.ordinal() / 2];
            float du = sample.uEnd() - sample.uStart();
            float dv = sample.vEnd() - sample.vStart();
            for (int vertex = 0; vertex < 4; vertex++) {
                float x = quad.x(vertex);
                float y = quad.y(vertex);
                float z = quad.z(vertex);
                quad.setUvFromSprite(
                        vertex,
                        sample.uStart() + du * (x * map[0] + y * map[1] + z * map[2]),
                        sample.vStart() + dv * (x * map[3] + y * map[4] + z * map[5]));
            }
            quad.setColor(argb(colorMultiplier));
            quad.recomputeNormals(false);
            return quad.toBakedQuad();
        }

        /** Legacy mapVertexColor without the channel swap; 26.1.2 vertex colors are ARGB. */
        private static int argb(int colorMultiplier) {
            return (colorMultiplier >>> 24) > 0 ? colorMultiplier : 0xFF000000 | colorMultiplier;
        }
    }
}
