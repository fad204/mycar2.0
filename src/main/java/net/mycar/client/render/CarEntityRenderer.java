package net.mycar.client.render;

import net.mycar.MyCarMod;
import net.mycar.entity.AbstractVehicleEntity;
import net.mycar.entity.CarEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;

public class CarEntityRenderer extends EntityRenderer<CarEntity> {
    /** Extra vertical lift for the plate label so it sits above the cabin roof
     *  instead of inside it. Car roof in world coords reaches y ≈ 2.4 while
     *  the default label sits at y = 2.0; +0.8 puts it cleanly above. */
    private static final double LABEL_LIFT = 0.8;

    private static final Identifier[] TEXTURES;
    static {
        TEXTURES = new Identifier[AbstractVehicleEntity.NUM_VARIANTS];
        for (int i = 0; i < AbstractVehicleEntity.NUM_VARIANTS; i++) {
            TEXTURES[i] = MyCarMod.id("textures/entity/car_" + AbstractVehicleEntity.VARIANT_NAMES[i] + ".png");
        }
    }

    private final CarEntityModel model;

    public CarEntityRenderer(EntityRenderDispatcher dispatcher) {
        super(dispatcher);
        this.model = new CarEntityModel();
        this.shadowRadius = 1.2f;
    }

    @Override
    public Identifier getTexture(CarEntity entity) {
        int v = entity.getVariant();
        if (v < 0 || v >= TEXTURES.length) v = 0;
        return TEXTURES[v];
    }

    @Override
    public void render(CarEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light) {
        matrices.push();
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F - yaw));
        matrices.translate(0.0D, 1.5D, 0.0D);
        matrices.scale(-1.0F, -1.0F, 1.0F);

        Identifier tex = getTexture(entity);
        // getEntityCutoutNoCull (instead of getEntityTranslucent): renders both
        // sides of every polygon, so when another car shoves your camera inside
        // its model during collision you still see its body instead of looking
        // straight through. Matches what the bicycle model uses by default.
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
        this.model.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vcp, light);
    }

    @Override
    protected void renderLabelIfPresent(CarEntity entity, Text text, MatrixStack matrices,
                                        VertexConsumerProvider vcp, int light) {
        // Color the plate text red+bold whenever this vehicle has outstanding
        // toll/speed-camera debt. The flag is the synced HAS_DEBT data tracker,
        // refreshed once per second on the server from RfcAccountState.
        Text displayed = entity.hasDebt()
            ? text.shallowCopy().formatted(Formatting.RED, Formatting.BOLD)
            : text;
        // Push the label-rendering coordinate frame up by LABEL_LIFT before
        // delegating to the vanilla billboard label rendering, so the plate
        // floats above the roof rather than inside the cabin.
        matrices.push();
        matrices.translate(0.0D, LABEL_LIFT, 0.0D);
        super.renderLabelIfPresent(entity, displayed, matrices, vcp, light);
        matrices.pop();
    }
}
