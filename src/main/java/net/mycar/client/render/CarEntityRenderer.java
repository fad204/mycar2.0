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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;

public class CarEntityRenderer extends EntityRenderer<CarEntity> {
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
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(tex));
        this.model.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vcp, light);
    }
}
