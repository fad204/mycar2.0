package net.mycar.client.render;

import net.mycar.entity.TruckEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Truck model — 15 parts in a 256x256 texture.
 *
 * 2 wide x 2 tall (visible) x 6 long blocks. Cab in the front (32 long),
 * closed cargo box in the back (56 long). 4 wheels.
 *
 * UV layout (computed for cuboid footprint = 2*sx+2*sz wide, sy+sz tall):
 *
 *   Body       (32 x 16 x 96) at (0,   0)    -> 256 x 112
 *   Cab        (28 x 16 x 32) at (0, 112)    -> 120 x 48
 *   Wheel 0    ( 5 x 12 x 18) at (120,112)   ->  46 x 30
 *   Wheel 1    ( 5 x 12 x 18) at (166,112)   ->  46 x 30
 *   Wheel 2    ( 5 x 12 x 18) at (120,142)   ->  46 x 30
 *   Wheel 3    ( 5 x 12 x 18) at (166,142)   ->  46 x 30
 *   Cargo Box  (28 x 16 x 56) at (0, 172)    -> 168 x 72
 *   FrontBump  (32 x  4 x  2) at (168,172)   ->  68 x  6
 *   RearBump   (32 x  4 x  2) at (168,178)   ->  68 x  6
 *   Headlight L (3 x  3 x  1) at (168,184)
 *   Headlight R (3 x  3 x  1) at (178,184)
 *   Taillight L (3 x  3 x  1) at (188,184)
 *   Taillight R (3 x  3 x  1) at (198,184)
 *   Mirror L    (2 x  3 x  4) at (168,188)
 *   Mirror R    (2 x  3 x  4) at (182,188)
 */
public class TruckEntityModel extends EntityModel<TruckEntity> {

    public static final int TEX_W = 320;
    public static final int TEX_H = 384;

    public final ModelPart body;
    public final ModelPart bodyExt;
    public final ModelPart cab;
    public final ModelPart cargo;
    public final ModelPart cargoExt;
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
    public final ModelPart wheelRRL;
    public final ModelPart wheelRRR;

    public TruckEntityModel() {
        this.textureWidth = TEX_W;
        this.textureHeight = TEX_H;

        // ----- Body (36 x 16 x 96) — widened from 32 to 36 (visual 2.0→2.25
        // blocks wide). Hitbox is 3 blocks wide so we have plenty of room. -----
        this.body = new ModelPart(this, 0, 0);
        this.body.addCuboid(-18f, 0f, -48f, 36f, 16f, 96f);
        this.body.setPivot(0f, 0f, 0f);

        // ----- Body Extension (36 x 16 x 24) at the rear, widened to match. -----
        this.bodyExt = new ModelPart(this, 120, 260);
        this.bodyExt.addCuboid(-18f, 0f, 48f, 36f, 16f, 24f);
        this.bodyExt.setPivot(0f, 0f, 0f);

        // ----- Cab (36 x 22 x 32) at front — widened to match body. -----
        this.cab = new ModelPart(this, 0, 112);
        this.cab.addCuboid(-18f, -22f, -46f, 36f, 22f, 32f);
        this.cab.setPivot(0f, 0f, 0f);

        // ----- Cargo Box (36 x 32 x 56) — widened to 36. Small 4-unit gap
        // between cab back (z=-14) and cargo front (z=-10). -----
        this.cargo = new ModelPart(this, 0, 172);
        this.cargo.addCuboid(-18f, -32f, -10f, 36f, 32f, 56f);
        this.cargo.setPivot(0f, 0f, 0f);

        // ----- Cargo Extension (36 x 32 x 24) — widened to 36. -----
        this.cargoExt = new ModelPart(this, 0, 260);
        this.cargoExt.addCuboid(-18f, -32f, 46f, 36f, 32f, 24f);
        this.cargoExt.setPivot(0f, 0f, 0f);

        // ----- Front Bumper (36 x 4 x 2) — widened to match body -----
        this.frontBumper = new ModelPart(this, 168, 184);
        this.frontBumper.addCuboid(-18f, 10f, -50f, 36f, 4f, 2f);
        this.frontBumper.setPivot(0f, 0f, 0f);

        // ----- Rear Bumper (36 x 4 x 2) -----
        this.rearBumper = new ModelPart(this, 168, 190);
        this.rearBumper.addCuboid(-18f, 10f, 48f, 36f, 4f, 2f);
        this.rearBumper.setPivot(0f, 0f, 0f);

        // ----- Headlights (3 x 3 x 1) at front corners — moved outward for
        // wider body. Old x=-14/11, new x=-16/13 (2 units from corner). -----
        this.headlightL = new ModelPart(this, 168, 196);
        this.headlightL.addCuboid(-16f, 4f, -49f, 3f, 3f, 1f);
        this.headlightL.setPivot(0f, 0f, 0f);

        this.headlightR = new ModelPart(this, 176, 196);
        this.headlightR.addCuboid(13f, 4f, -49f, 3f, 3f, 1f);
        this.headlightR.setPivot(0f, 0f, 0f);

        // ----- Taillights (3 x 3 x 1) at rear corners — moved outward. -----
        this.taillightL = new ModelPart(this, 184, 196);
        this.taillightL.addCuboid(-16f, 4f, 48f, 3f, 3f, 1f);
        this.taillightL.setPivot(0f, 0f, 0f);

        this.taillightR = new ModelPart(this, 192, 196);
        this.taillightR.addCuboid(13f, 4f, 48f, 3f, 3f, 1f);
        this.taillightR.setPivot(0f, 0f, 0f);

        // ----- Mirrors (2 x 3 x 4) on cab sides — moved outward to stick out
        // 1 unit beyond the wider body. Old x=-16/14, new x=-19/17. -----
        this.mirrorL = new ModelPart(this, 168, 200);
        this.mirrorL.addCuboid(-19f, -14f, -42f, 2f, 3f, 4f);
        this.mirrorL.setPivot(0f, 0f, 0f);

        this.mirrorR = new ModelPart(this, 180, 200);
        this.mirrorR.addCuboid(17f, -14f, -42f, 2f, 3f, 4f);
        this.mirrorR.setPivot(0f, 0f, 0f);

        // ----- 4 Wheels (5 x 16 x 20) — UV at u=144 (cab UV grew to 136 wide).
        // Wheel x moved outward: outer at -19 (1 beyond body west at -18). -----
        this.wheelFL = new ModelPart(this, 144, 112);
        this.wheelFL.addCuboid(-19f, 12f, -40f, 5f, 16f, 20f);
        this.wheelFL.setPivot(0f, 0f, 0f);

        this.wheelFR = new ModelPart(this, 194, 112);
        this.wheelFR.addCuboid(14f, 12f, -40f, 5f, 16f, 20f);
        this.wheelFR.setPivot(0f, 0f, 0f);

        this.wheelRL = new ModelPart(this, 144, 148);
        this.wheelRL.addCuboid(-19f, 12f, 20f, 5f, 16f, 20f);
        this.wheelRL.setPivot(0f, 0f, 0f);

        this.wheelRR = new ModelPart(this, 194, 148);
        this.wheelRR.addCuboid(14f, 12f, 20f, 5f, 16f, 20f);
        this.wheelRR.setPivot(0f, 0f, 0f);

        // ----- Second rear axle (5 x 16 x 20). -----
        this.wheelRRL = new ModelPart(this, 120, 316);
        this.wheelRRL.addCuboid(-19f, 12f, 46f, 5f, 16f, 20f);
        this.wheelRRL.setPivot(0f, 0f, 0f);

        this.wheelRRR = new ModelPart(this, 170, 316);
        this.wheelRRR.addCuboid(14f, 12f, 46f, 5f, 16f, 20f);
        this.wheelRRR.setPivot(0f, 0f, 0f);
    }

    @Override
    public void setAngles(TruckEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        // No animations yet.
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        this.body.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.bodyExt.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.cab.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.cargo.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.cargoExt.render(matrices, vertices, light, overlay, red, green, blue, alpha);
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
        this.wheelRRL.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.wheelRRR.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
