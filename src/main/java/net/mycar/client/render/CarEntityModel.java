package net.mycar.client.render;

import net.mycar.entity.CarEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class CarEntityModel extends EntityModel<CarEntity> {

    public static final int TEX_W = 256;
    public static final int TEX_H = 256;

    public final ModelPart body;
    public final ModelPart hood;
    public final ModelPart cabin;
    public final ModelPart roof;
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

    public CarEntityModel() {
        this.textureWidth = TEX_W;
        this.textureHeight = TEX_H;

        this.body = new ModelPart(this, 0, 0);
        this.body.addCuboid(-16f, 0f, -32f, 32f, 16f, 64f);
        this.body.setPivot(0f, 0f, 0f);

        this.hood = new ModelPart(this, 0, 128);
        this.hood.addCuboid(-14f, -2f, -30f, 28f, 2f, 20f);
        this.hood.setPivot(0f, 0f, 0f);

        this.cabin = new ModelPart(this, 0, 80);
        this.cabin.addCuboid(-14f, -12f, -10f, 28f, 12f, 32f);
        this.cabin.setPivot(0f, 0f, 0f);

        this.roof = new ModelPart(this, 0, 152);
        this.roof.addCuboid(-13f, -14f, -8f, 26f, 2f, 28f);
        this.roof.setPivot(0f, 0f, 0f);

        this.frontBumper = new ModelPart(this, 0, 184);
        this.frontBumper.addCuboid(-15f, 8f, -34f, 30f, 4f, 2f);
        this.frontBumper.setPivot(0f, 0f, 0f);

        this.rearBumper = new ModelPart(this, 66, 184);
        this.rearBumper.addCuboid(-15f, 8f, 32f, 30f, 4f, 2f);
        this.rearBumper.setPivot(0f, 0f, 0f);

        this.headlightL = new ModelPart(this, 0, 192);
        this.headlightL.addCuboid(-14f, 3f, -33f, 3f, 3f, 1f);
        this.headlightL.setPivot(0f, 0f, 0f);

        this.headlightR = new ModelPart(this, 10, 192);
        this.headlightR.addCuboid(11f, 3f, -33f, 3f, 3f, 1f);
        this.headlightR.setPivot(0f, 0f, 0f);

        this.taillightL = new ModelPart(this, 20, 192);
        this.taillightL.addCuboid(-14f, 3f, 32f, 3f, 3f, 1f);
        this.taillightL.setPivot(0f, 0f, 0f);

        this.taillightR = new ModelPart(this, 30, 192);
        this.taillightR.addCuboid(11f, 3f, 32f, 3f, 3f, 1f);
        this.taillightR.setPivot(0f, 0f, 0f);

        this.mirrorL = new ModelPart(this, 40, 196);
        this.mirrorL.addCuboid(-16f, -10f, -7f, 2f, 3f, 4f);
        this.mirrorL.setPivot(0f, 0f, 0f);

        this.mirrorR = new ModelPart(this, 54, 196);
        this.mirrorR.addCuboid(14f, -10f, -7f, 2f, 3f, 4f);
        this.mirrorR.setPivot(0f, 0f, 0f);

        this.wheelFL = new ModelPart(this, 200, 0);
        this.wheelFL.addCuboid(-17f, 16f, -28f, 4f, 8f, 12f);
        this.wheelFL.setPivot(0f, 0f, 0f);

        this.wheelFR = new ModelPart(this, 200, 20);
        this.wheelFR.addCuboid(13f, 16f, -28f, 4f, 8f, 12f);
        this.wheelFR.setPivot(0f, 0f, 0f);

        this.wheelRL = new ModelPart(this, 200, 40);
        this.wheelRL.addCuboid(-17f, 16f, 16f, 4f, 8f, 12f);
        this.wheelRL.setPivot(0f, 0f, 0f);

        this.wheelRR = new ModelPart(this, 200, 60);
        this.wheelRR.addCuboid(13f, 16f, 16f, 4f, 8f, 12f);
        this.wheelRR.setPivot(0f, 0f, 0f);
    }

    @Override
    public void setAngles(CarEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        this.body.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.hood.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.cabin.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.roof.render(matrices, vertices, light, overlay, red, green, blue, alpha);
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
