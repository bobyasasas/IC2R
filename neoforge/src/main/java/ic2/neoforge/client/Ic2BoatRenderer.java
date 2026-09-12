package ic2.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/**
 * Vanilla BoatRenderer pinned to an explicit IC2 texture: the vanilla class derives the texture
 * path from the model layer id, which cannot carry the ic2 namespace, so the body is copied and
 * the texture passed straight to AbstractBoatRenderer.
 */
public class Ic2BoatRenderer extends AbstractBoatRenderer {
    private final Model.Simple waterPatchModel;
    private final EntityModel<BoatRenderState> model;

    public Ic2BoatRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context, texture);
        this.waterPatchModel =
                new Model.Simple(
                        context.bakeLayer(ModelLayers.BOAT_WATER_PATCH),
                        t -> RenderTypes.waterMask());
        this.model = new BoatModel(context.bakeLayer(ModelLayers.OAK_BOAT));
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return this.model;
    }

    @Override
    protected void submitTypeAdditions(
            BoatRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords) {
        if (!state.isUnderWater) {
            submitNodeCollector.submitModel(
                    this.waterPatchModel,
                    Unit.INSTANCE,
                    poseStack,
                    this.texture,
                    lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    state.outlineColor,
                    null);
        }
    }
}
