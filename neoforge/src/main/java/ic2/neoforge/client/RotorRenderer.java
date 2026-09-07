package ic2.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineBlockEntity;
import ic2.neoforge.machine.RotorGeneratorBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/** Extracts animation into render state; submission never reads or mutates the world. */
public final class RotorRenderer
        implements BlockEntityRenderer<MachineBlockEntity, RotorRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.parse("ic2:textures/item/rotor/iron_rotor_model.png");
    private final ModelPart model = createRotor();
    private final Map<MachineBlockEntity, Animation> animations = new WeakHashMap<>();

    public RotorRenderer(BlockEntityRendererProvider.Context context) {}

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float angle;
    }

    private static final class Animation {
        double previousTime = Double.NaN;
        float angle;

        float sample(double time, float speed) {
            if (Double.isFinite(previousTime))
                angle = (float) ((angle + Math.max(0, time - previousTime) * 20 * speed) % 360);
            previousTime = time;
            return angle;
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            MachineBlockEntity blockEntity,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity, state, partialTick, camera, overlay);
        var rotor = (RotorGeneratorBlockEntity) blockEntity;
        state.facing = blockEntity.getBlockState().getValue(MachineBlock.FACING);
        var level = blockEntity.getLevel();
        if (level == null) return;
        double time = level.getGameTime() + (Minecraft.getInstance().isPaused() ? 0 : partialTick);
        state.angle =
                animations
                        .computeIfAbsent(blockEntity, ignored -> new Animation())
                        .sample(time, rotor.rotorSpeed());
        state.lightCoords =
                LevelRenderer.getLightCoords(
                        level, blockEntity.getBlockPos().relative(state.facing));
    }

    @Override
    public void submit(
            State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        poses.translate(.5, .5, .5);
        switch (state.facing) {
            case NORTH -> poses.mulPose(Axis.YP.rotationDegrees(-90));
            case EAST -> poses.mulPose(Axis.YP.rotationDegrees(-180));
            case SOUTH -> poses.mulPose(Axis.YP.rotationDegrees(-270));
            case UP -> poses.mulPose(Axis.ZP.rotationDegrees(-90));
            case DOWN -> poses.mulPose(Axis.ZP.rotationDegrees(90));
            case WEST -> {}
        }
        poses.mulPose(Axis.XP.rotationDegrees(state.angle));
        poses.translate(-.2, 0, 0);
        collector.submitModelPart(
                model,
                poses,
                RenderTypes.entitySolid(TEXTURE),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                null,
                -1,
                state.breakProgress);
        poses.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MachineBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1);
    }

    private static ModelPart createRotor() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        float[][] rotations = {{0, -.5f, 0}, {3.1f, .5f, 0}, {4.7f, 0, .5f}, {1.5f, 0, -.5f}};
        for (int blade = 0; blade < rotations.length; blade++) {
            var rotation = rotations[blade];
            root.addOrReplaceChild(
                    "blade_" + blade,
                    CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, -4, 1, 16, 8),
                    PartPose.offsetAndRotation(-8, 0, 0, rotation[0], rotation[1], rotation[2]));
        }
        return LayerDefinition.create(mesh, 32, 256).bakeRoot();
    }
}
