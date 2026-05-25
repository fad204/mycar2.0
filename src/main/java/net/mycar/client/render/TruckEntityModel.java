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

    public static final int TEX_W = 256;
    public static final int TEX_H = 320;

    public final ModelPart body;
    public final ModelPart cab;
    public final ModelPart cargo;
    public final ModelPart cargoExt;
    public final ModelPart cabFront;
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

        // ----- Body (32 x 16 x 96) — fe1de6e baseline -----
        this.body = new ModelPart(this, 0, 0);
        this.body.addCuboid(-16f, 0f, -48f, 32f, 16f, 96f);
        this.body.setPivot(0f, 0f, 0f);

        // ----- Cab (28 x 24 x 32) at front — taller cab to match real truck
        // proportions (was 28x16x32). Roof now at world Y ≈ 3.25. -----
        this.cab = new ModelPart(this, 0, 112);
        this.cab.addCuboid(-14f, -24f, -46f, 28f, 24f, 32f);
        this.cab.setPivot(0f, 0f, 0f);

        // ----- Cargo Box (28 x 26 x 56) behind cab — slightly taller than the
        // cab so the closed cargo box extends just above the cab roof, the way
        // a real Italian delivery truck looks (was 28x16x56). -----
        this.cargo = new ModelPart(this, 0, 172);
        this.cargo.addCuboid(-14f, -26f, -10f, 28f, 26f, 56f);
        this.cargo.setPivot(0f, 0f, 0f);

        // ----- Cargo Extension (28 x 26 x 24) behind cargo — extends the
        // truck length by 1.5 blocks so it doesn't look so stubby. Same
        // dimensions as cargo in X/Y so they form one continuous box. -----
        this.cargoExt = new ModelPart(this, 0, 254);
        this.cargoExt.addCuboid(-14f, -26f, 46f, 28f, 26f, 24f);
        this.cargoExt.setPivot(0f, 0f, 0f);

        // ----- Cab Front grille/bumper box (28 x 6 x 2) — small protrusion at
        // the bottom-front of the cab, gives the cab front a distinct "face"
        // (engine grille area) instead of just being a flat windshield. -----
        this.cabFront = new ModelPart(this, 168, 207);
        this.cabFront.addCuboid(-14f, -6f, -48f, 28f, 6f, 2f);
        this.cabFront.setPivot(0f, 0f, 0f);

        // ----- Front Bumper (32 x 4 x 2) — UV moved to v=184 to make room
        // for bigger wheels -----
        this.frontBumper = new ModelPart(this, 168, 184);
        this.frontBumper.addCuboid(-16f, 10f, -50f, 32f, 4f, 2f);
        this.frontBumper.setPivot(0f, 0f, 0f);

        // ----- Rear Bumper (32 x 4 x 2) -----
        this.rearBumper = new ModelPart(this, 168, 190);
        this.rearBumper.addCuboid(-16f, 10f, 48f, 32f, 4f, 2f);
        this.rearBumper.setPivot(0f, 0f, 0f);

        // ----- Headlights (3 x 3 x 1) at front corners -----
        this.headlightL = new ModelPart(this, 168, 196);
        this.headlightL.addCuboid(-14f, 4f, -49f, 3f, 3f, 1f);
        this.headlightL.setPivot(0f, 0f, 0f);

        this.headlightR = new ModelPart(this, 176, 196);
        this.headlightR.addCuboid(11f, 4f, -49f, 3f, 3f, 1f);
        this.headlightR.setPivot(0f, 0f, 0f);

        // ----- Taillights (3 x 3 x 1) at rear corners -----
        this.taillightL = new ModelPart(this, 184, 196);
        this.taillightL.addCuboid(-14f, 4f, 48f, 3f, 3f, 1f);
        this.taillightL.setPivot(0f, 0f, 0f);

        this.taillightR = new ModelPart(this, 192, 196);
        this.taillightR.addCuboid(11f, 4f, 48f, 3f, 3f, 1f);
        this.taillightR.setPivot(0f, 0f, 0f);

        // ----- Mirrors (2 x 3 x 4) on cab sides -----
        this.mirrorL = new ModelPart(this, 168, 200);
        this.mirrorL.addCuboid(-16f, -14f, -42f, 2f, 3f, 4f);
        this.mirrorL.setPivot(0f, 0f, 0f);

        this.mirrorR = new ModelPart(this, 180, 200);
        this.mirrorR.addCuboid(14f, -14f, -42f, 2f, 3f, 4f);
        this.mirrorR.setPivot(0f, 0f, 0f);

        // ----- 4 Wheels (5 x 16 x 20) — bigger than before so they read as
        // proper truck tires. Y range 12-28 puts wheel bottom on the ground
        // (world Y=0) and wheel top at world Y=1.0 — partially overlapping
        // body chassis like a fender. -----
        this.wheelFL = new ModelPart(this, 120, 112);
        this.wheelFL.addCuboid(-17f, 12f, -40f, 5f, 16f, 20f);
        this.wheelFL.setPivot(0f, 0f, 0f);

        this.wheelFR = new ModelPart(this, 170, 112);
        this.wheelFR.addCuboid(12f, 12f, -40f, 5f, 16f, 20f);
        this.wheelFR.setPivot(0f, 0f, 0f);

        this.wheelRL = new ModelPart(this, 120, 148);
        this.wheelRL.addCuboid(-17f, 12f, 20f, 5f, 16f, 20f);
        this.wheelRL.setPivot(0f, 0f, 0f);

        this.wheelRR = new ModelPart(this, 170, 148);
        this.wheelRR.addCuboid(12f, 12f, 20f, 5f, 16f, 20f);
        this.wheelRR.setPivot(0f, 0f, 0f);
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
        this.cab.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.cargo.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.cargoExt.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.cabFront.render(matrices, vertices, light, overlay, red, green, blue, alpha);
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
