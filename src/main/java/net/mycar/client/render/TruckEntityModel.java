package net.mycar.client.render;

import net.mycar.entity.TruckEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class TruckEntityModel extends EntityModel<TruckEntity> {

    public static final int TEX_W = 256;
    public static final int TEX_H = 256;

    public final ModelPart body;
    public final ModelPart cab;
    public final ModelPart cargo;
    public final ModelPart frontBumper;
    public final ModelPart rearBumper;
    public final ModelPart headlightL;
    public final ModelPart headlightR;
    public final ModelPart taillightL;
    public final ModelPart taillightR;
    public final ModelPart mirrorL;
    public final ModelPart mirrorR;
    public final ModelPart wheelFL;
    public final ModelPart wheelFR;
    public final ModelPart wheelRL;
    public final ModelPart wheelRR;

    public TruckEntityModel() {
        this.textureWidth = TEX_W;
        this.textureHeight = TEX_H;

        this.body = new ModelPart(this, 0, 0);
        this.body.addCuboid(-16f, 0f, -48f, 32f, 16f, 96f);
        this.body.setPivot(0f, 0f, 0f);

        this.cab = new ModelPart(this, 0, 112);
        this.cab.addCuboid(-14f, -16f, -46f, 28f, 16f, 32f);
        this.cab.setPivot(0f, 0f, 0f);

        this.cargo = new ModelPart(this, 0, 172);
        this.cargo.addCuboid(-14f, -16f, -10f, 28f, 16f, 56f);
        this.cargo.setPivot(0f, 0f, 0f);

        this.frontBumper = new ModelPart(this, 168, 172);
        this.frontBumper.addCuboid(-16f, 10f, -50f, 32f, 4f, 2f);
        this.frontBumper.setPivot(0f, 0f, 0f);

        this.rearBumper = new ModelPart(this, 168, 178);
        this.rearBumper.addCuboid(-16f, 10f, 48f, 32f, 4f, 2f);
        this.rearBumper.setPivot(0f, 0f, 0f);

        this.headlightL = new ModelPart(this, 168, 184);
        this.headlightL.addCuboid(-14f, 4f, -49f, 3f, 3f, 1f);
        this.headlightL.setPivot(0f, 0f, 0f);

        this.headlightR = new ModelPart(this, 178, 184);
        this.headlightR.addCuboid(11f, 4f, -49f, 3f, 3f, 1f);
        this.headlightR.setPivot(0f, 0f, 0f);

        this.taillightL = new ModelPart(this, 188, 184);
        this.taillightL.addCuboid(-14f, 4f, 48f, 3f, 3f, 1f);
        this.taillightL.setPivot(0f, 0f, 0f);

        this.taillightR = new ModelPart(this, 198, 184);
        this.taillightR.addCuboid(11f, 4f, 48f, 3f, 3f, 1f);
        this.taillightR.setPivot(0f, 0f, 0f);

        this.mirrorL = new ModelPart(this, 168, 188);
        this.mirrorL.addCuboid(-16f, -14f, -42f, 2f, 3f, 4f);
        this.mirrorL.setPivot(0f, 0f, 0f);

        this.mirrorR = new ModelPart(this, 182, 188);
        this.mirrorR.addCuboid(14f, -14f, -42f, 2f, 3f, 4f);
        this.mirrorR.setPivot(0f, 0f, 0f);

        this.wheelFL = new ModelPart(this, 120, 112);
        this.wheelFL.addCuboid(-17f, 16f, -40f, 5f, 12f, 18f);
        this.wheelFL.setPivot(0f, 0f, 0f);

        this.wheelFR = new ModelPart(this, 166, 112);
        this.wheelFR.addCuboid(12f, 16f, -40f, 5f, 12f, 18f);
        this.wheelFR.setPivot(0f, 0f, 0f);

        this.wheelRL = new ModelPart(this, 120, 142);
        this.wheelRL.addCuboid(-17f, 16f, 22f, 5f, 12f, 18f);
        this.wheelRL.setPivot(0f, 0f, 0f);

        this.wheelRR = new ModelPart(this, 166, 142);
        this.wheelRR.addCuboid(12f, 16f, 22f, 5f, 12f, 18f);
        this.wheelRR.setPivot(0f, 0f, 0f);
    }

    @Override
    public void setAngles(TruckEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        this.body.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.cab.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.cargo.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.frontBumper.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.rearBumper.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.headlightL.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.headlightR.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.taillightL.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.taillightR.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.mirrorL.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.mirrorR.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.wheelFL.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.wheelFR.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.wheelRL.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.wheelRR.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
