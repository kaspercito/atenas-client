package com.atenas.client.cosmetics;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * Alas de 7 plumas en abanico (igual que en el mod de 1.20.1).
 * Textura 64x64: la pluma i usa la franja que empieza en v = 4*i.
 */
public class WingsModel extends ModelBase {
    private static final float[] FEATHER_LEN = {18, 17, 16, 14, 12, 10, 8};

    private final ModelRenderer right;
    private final ModelRenderer left;

    public WingsModel() {
        textureWidth = 64;
        textureHeight = 64;
        right = new ModelRenderer(this, 0, 0);
        right.setRotationPoint(-1f, 1f, 2.2f);
        left = new ModelRenderer(this, 0, 0);
        left.setRotationPoint(1f, 1f, 2.2f);
        for (int i = 0; i < FEATHER_LEN.length; i++) {
            float len = FEATHER_LEN[i];
            float ang = 0.30f - i * 0.17f;
            float z = i * 0.12f; // cada pluma un poco mas atras: sin parpadeo entre capas

            ModelRenderer r = new ModelRenderer(this, 0, 4 * i);
            r.setRotationPoint(0f, 0f, z);
            r.addBox(-len, -1.5f, 0f, (int) len, 3, 1);
            r.rotateAngleZ = ang;
            right.addChild(r);

            ModelRenderer l = new ModelRenderer(this, 0, 4 * i);
            l.mirror = true;
            l.setRotationPoint(0f, 0f, z);
            l.addBox(0f, -1.5f, 0f, (int) len, 3, 1);
            l.rotateAngleZ = -ang;
            left.addChild(l);
        }
    }

    /** open: apertura de cada ala en radianes (0 = pegadas a la espalda). */
    public void renderWings(float scale, float open) {
        right.rotateAngleY = open;
        left.rotateAngleY = -open;
        right.render(scale);
        left.render(scale);
    }
}
