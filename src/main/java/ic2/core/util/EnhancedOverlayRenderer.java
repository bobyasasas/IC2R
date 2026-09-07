package ic2.core.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class EnhancedOverlayRenderer {
    private static final float FACE_EPS = 0.002F;
    private static final double OUTLINE_EPS = 0.002;
    private static final float R = 0.0F;
    private static final float G = 0.0F;
    private static final float B = 0.0F;
    private static final float A = 0.5F;

    private EnhancedOverlayRenderer() {}

    public static boolean render(
            Level world,
            BlockHitResult target,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Direction currentFacing) {
        BlockPos pos = target.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Direction side = target.getDirection();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        VoxelShape shape = state.getShape(world, pos);
        if (!shape.isEmpty()) {
            AABB bounds = shape.bounds().inflate(0.002);
            LevelRenderer.renderLineBox(poseStack, lines, bounds, 0.0F, 0.0F, 0.0F, 0.5F);
        }

        Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        drawFaceGrid(lines, matrix, normal, side);
        if (currentFacing != null) {
            drawFacingMark(lines, matrix, normal, side, currentFacing);
        }

        poseStack.popPose();
        return true;
    }

    private static void drawFaceGrid(
            VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, Direction side) {
        lineOnFace(consumer, matrix, normal, side, 0.0F, 0.25F, 1.0F, 0.25F);
        lineOnFace(consumer, matrix, normal, side, 0.0F, 0.75F, 1.0F, 0.75F);
        lineOnFace(consumer, matrix, normal, side, 0.25F, 0.0F, 0.25F, 1.0F);
        lineOnFace(consumer, matrix, normal, side, 0.75F, 0.0F, 0.75F, 1.0F);
    }

    private static void drawFacingMark(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            Direction sideHit,
            Direction facing) {
        if (facing == sideHit) {
            drawX(consumer, matrix, normal, sideHit, 0.25F, 0.25F, 0.75F, 0.75F);
        } else if (facing == sideHit.getOpposite()) {
            drawX(consumer, matrix, normal, sideHit, 0.0F, 0.0F, 0.25F, 0.25F);
            drawX(consumer, matrix, normal, sideHit, 0.75F, 0.0F, 1.0F, 0.25F);
            drawX(consumer, matrix, normal, sideHit, 0.0F, 0.75F, 0.25F, 1.0F);
            drawX(consumer, matrix, normal, sideHit, 0.75F, 0.75F, 1.0F, 1.0F);
        } else {
            float[] edge = edgeUvForFacing(sideHit, facing);
            if (edge != null) {
                drawX(consumer, matrix, normal, sideHit, edge[0], edge[1], edge[2], edge[3]);
            }
        }
    }

    private static float[] edgeUvForFacing(Direction sideHit, Direction facing) {
        return switch (sideHit) {
            case WEST, EAST -> {
                switch (facing) {
                    case NORTH:
                        yield new float[] {0.0F, 0.25F, 0.25F, 0.75F};
                    case SOUTH:
                        yield new float[] {0.75F, 0.25F, 1.0F, 0.75F};
                    case DOWN:
                        yield new float[] {0.25F, 0.0F, 0.75F, 0.25F};
                    case UP:
                        yield new float[] {0.25F, 0.75F, 0.75F, 1.0F};
                    default:
                        yield null;
                }
            }
            case NORTH, SOUTH -> {
                switch (facing) {
                    case WEST:
                        yield new float[] {0.0F, 0.25F, 0.25F, 0.75F};
                    case EAST:
                        yield new float[] {0.75F, 0.25F, 1.0F, 0.75F};
                    case NORTH:
                    case SOUTH:
                    default:
                        yield null;
                    case DOWN:
                        yield new float[] {0.25F, 0.0F, 0.75F, 0.25F};
                    case UP:
                        yield new float[] {0.25F, 0.75F, 0.75F, 1.0F};
                }
            }
            case DOWN, UP -> {
                switch (facing) {
                    case WEST:
                        yield new float[] {0.0F, 0.25F, 0.25F, 0.75F};
                    case EAST:
                        yield new float[] {0.75F, 0.25F, 1.0F, 0.75F};
                    case NORTH:
                        yield new float[] {0.25F, 0.0F, 0.75F, 0.25F};
                    case SOUTH:
                        yield new float[] {0.25F, 0.75F, 0.75F, 1.0F};
                    default:
                        yield null;
                }
            }
        };
    }

    private static void drawX(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            Direction side,
            float u0,
            float v0,
            float u1,
            float v1) {
        lineOnFace(consumer, matrix, normal, side, u0, v0, u1, v1);
        lineOnFace(consumer, matrix, normal, side, u0, v1, u1, v0);
    }

    private static void lineOnFace(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normalMatrix,
            Direction side,
            float u1,
            float v1,
            float u2,
            float v2) {
        float[] p1 = uvToLocal(side, u1, v1);
        float[] p2 = uvToLocal(side, u2, v2);
        line(consumer, matrix, normalMatrix, p1[0], p1[1], p1[2], p2[0], p2[1], p2[2]);
    }

    private static float[] uvToLocal(Direction side, float u, float v) {
        return switch (side) {
            case WEST -> new float[] {-0.002F, v, u};
            case EAST -> new float[] {1.002F, v, u};
            case NORTH -> new float[] {u, v, -0.002F};
            case SOUTH -> new float[] {u, v, 1.002F};
            case DOWN -> new float[] {u, -0.002F, v};
            case UP -> new float[] {u, 1.002F, v};
        };
    }

    private static void line(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normalMatrix,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (!(len < 1.0E-6F)) {
            dx /= len;
            dy /= len;
            dz /= len;
            consumer.vertex(matrix, x1, y1, z1)
                    .color(0.0F, 0.0F, 0.0F, 0.5F)
                    .normal(normalMatrix, dx, dy, dz)
                    .endVertex();
            consumer.vertex(matrix, x2, y2, z2)
                    .color(0.0F, 0.0F, 0.0F, 0.5F)
                    .normal(normalMatrix, dx, dy, dz)
                    .endVertex();
        }
    }
}
