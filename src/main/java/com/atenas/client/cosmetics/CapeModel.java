package com.atenas.client.cosmetics;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * Capa de tela: 8 tiras horizontales de 2 de alto encadenadas (cada una cuelga de la anterior). Cada tira se dobla un poco
 * mas que la de arriba y se le suma una onda que baja por la tela, asi la capa se curva con el viento y ondea al correr en
 * vez de moverse como una tabla rigida.
 *
 * Textura: la misma hoja 64x32 de siempre (capa de 10 x 16 x 1 en u=0, v=0). La tira i usa v = 2*i: su cara frontal cae en las
 * filas 2i+1..2i+2 de la capa original, asi que cualquier textura de capa (normal o animada) se ve igual que antes.
 */
public class CapeModel extends ModelBase {
    public static final int SEGS = CapeWave.SEGS;
    private static final int SEG_H = CapeWave.SEG_H;

    private final ModelRenderer[] seg = new ModelRenderer[SEGS];

    public CapeModel() {
        textureWidth = 64;
        textureHeight = 32;
        for (int i = 0; i < SEGS; i++) {
            ModelRenderer r = new ModelRenderer(this, 0, SEG_H * i);
            r.addBox(-5f, 0f, -1f, 10, SEG_H, 1);
            r.setRotationPoint(0f, i == 0 ? 0f : (float) SEG_H, 0f);
            if (i > 0) seg[i - 1].addChild(r);
            seg[i] = r;
        }
    }

    private final float[] rx = new float[SEGS], rz = new float[SEGS];

    /** Pone la capa en su pose (ver {@link CapeWave#angles}) y la dibuja. */
    public void renderChain(float scale, float bend, float amp, float time, float side) {
        CapeWave.angles(bend, amp, time, side, rx, rz);
        for (int i = 0; i < SEGS; i++) {
            seg[i].rotateAngleX = rx[i];
            seg[i].rotateAngleZ = rz[i];
        }
        seg[0].render(scale);
    }
}
