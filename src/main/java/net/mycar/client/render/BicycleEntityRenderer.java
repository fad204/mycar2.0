package net.mycar.client.render;

import net.mycar.MyCarMod;
import net.mycar.entity.AbstractVehicleEntity;
import net.mycar.entity.BicycleEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;

public class BicycleEntityRenderer extends EntityRenderer<BicycleEntity> {
    private static final Identifier[] TEXTURES;
    static {
        TEXTURES = new Identifier[AbstractVehicleEntity.NUM_VARIANTS];
        for (int i = 0; i < AbstractVehicleEntity.NUM_VARIANTS; i++) {
            TEXTURES[i] = MyCarMod.id("textures/entity/bicycle_" + AbstractVehicleEntity.VARIANT_NAMES[i] + ".png");
        }
    }

    private final BicycleEntityModel model;

    public BicycleEntityRenderer(EntityRenderDispatcher dispatcher) {
        super(dispatcher);
        this.model = new BicycleEntityModel();
        this.shadowRadius = 0.4f;
    }

    @Override
    public Identifier getTexture(BicycleEntity entity) {
        int v = entity.getVariant();
        if (v < 0 || v >= TEXTURES.length) v = 0;
        return TEXTURES[v];
    }

    @Override
    public void render(BicycleEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light) {
        matrices.push();
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F - yaw));
        // Bicycle wheel bottoms reach model y=24, so translate(0, 1.5, 0) puts them on the ground.
        matrices.translate(0.0D, 1.5D, 0.0D);
        matrices.scale(-1.0F, -1.0F, 1.0F);

        Identifier tex = getTexture(entity);
        VertexConsumer vc = vcp.getBuffer(this.model.getLayer(tex));
        this.model.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vcp, light);
    }
}
