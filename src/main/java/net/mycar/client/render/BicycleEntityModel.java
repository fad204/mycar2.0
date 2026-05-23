package net.mycar.client.render;

import net.mycar.entity.BicycleEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Bicycle model — 7 parts in a 128x64 texture.
 *
 *   FrontWheel (2 x 12 x 12) at (0,   0)     -> 28 x 24
 *   RearWheel  (2 x 12 x 12) at (28,  0)     -> 28 x 24
 *   TopTube    (2 x  2 x 16) at (56,  0)     -> 36 x 18
 *   SeatPost   (2 x  6 x  2) at (92,  0)     ->  8 x 8
 *   Saddle     (4 x  1 x  6) at (100, 0)     -> 20 x 7
 *   Stem       (2 x  4 x  2) at (0,  28)     ->  8 x 6
 *   Handlebars (8 x  1 x  2) at (8,  28)     -> 20 x 3
 */
public class BicycleEntityModel extends EntityModel<BicycleEntity> {

    public static final int TEX_W = 128;
    public static final int TEX_H = 64;

    public final ModelPart frontWheel;
    public final ModelPart rearWheel;
    public final ModelPart topTube;
    public final ModelPart seatPost;
    public final ModelPart saddle;
    public final ModelPart stem;
    public final ModelPart handlebars;

    public BicycleEntityModel() {
        this.textureWidth = TEX_W;
        this.textureHeight = TEX_H;

        // Front wheel (2 x 12 x 12), front-half of bike
        this.frontWheel = new ModelPart(this, 0, 0);
        this.frontWheel.addCuboid(-1f, 12f, -12f, 2f, 12f, 12f);
        this.frontWheel.setPivot(0f, 0f, 0f);

        // Rear wheel (2 x 12 x 12), back-half
        this.rearWheel = new ModelPart(this, 28, 0);
        this.rearWheel.addCuboid(-1f, 12f, 0f, 2f, 12f, 12f);
        this.rearWheel.setPivot(0f, 0f, 0f);

        // Top tube (2 x 2 x 16) — horizontal frame member between wheels
        this.topTube = new ModelPart(this, 56, 0);
        this.topTube.addCuboid(-1f, 12f, -8f, 2f, 2f, 16f);
        this.topTube.setPivot(0f, 0f, 0f);

        // Seat post (2 x 6 x 2) — vertical from frame
        this.seatPost = new ModelPart(this, 92, 0);
        this.seatPost.addCuboid(-1f, 6f, 6f, 2f, 6f, 2f);
        this.seatPost.setPivot(0f, 0f, 0f);

        // Saddle (4 x 1 x 6) on top of seat post
        this.saddle = new ModelPart(this, 100, 0);
        this.saddle.addCuboid(-2f, 5f, 4f, 4f, 1f, 6f);
        this.saddle.setPivot(0f, 0f, 0f);

        // Stem (2 x 4 x 2) — vertical near front
        this.stem = new ModelPart(this, 0, 28);
        this.stem.addCuboid(-1f, 8f, -7f, 2f, 4f, 2f);
        this.stem.setPivot(0f, 0f, 0f);

        // Handlebars (8 x 1 x 2) — horizontal bar
        this.handlebars = new ModelPart(this, 8, 28);
        this.handlebars.addCuboid(-4f, 7f, -7f, 8f, 1f, 2f);
        this.handlebars.setPivot(0f, 0f, 0f);
    }

    @Override
    public void setAngles(BicycleEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        // No animations yet.
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        this.frontWheel.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.rearWheel.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.topTube.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.seatPost.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.saddle.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.stem.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.handlebars.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
