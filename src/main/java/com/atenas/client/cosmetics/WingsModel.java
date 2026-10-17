package com.atenas.client.cosmetics;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * Alas de plumas en tres capas: un hueso (borde de ataque), 10 plumas largas y 7 coberteras cortas encima.
 * Todo son cajas finas (1 de grosor): la forma de pluma sale de la transparencia de la textura (alta resolucion).
 * El modelo se compila una sola vez (display lists de Minecraft): dibujarlo cuesta casi nada, y cada cuadro solo se
 * mueven unos angulos.
 *
 * Textura: se declara de 64x128 "unidades"; la imagen real puede ser mas grande (6x). Cada pluma ocupa una franja
 * que empieza en v = V_PRIMARY[i] / V_COVERT[i] / V_BONE. Debe coincidir con tools/gen_wings.py.
 */
public class WingsModel extends ModelBase {
    // ---- geometria (compartida con tools/gen_wings.py: si cambias algo aqui, cambialo alli) ----
    static final int[] P_LEN = {22, 22, 21, 20, 19, 17, 15, 13, 11, 9};
    static final int P_H = 4;
    static final int[] C_LEN = {12, 12, 11, 10, 9, 8, 7};
    static final int C_H = 3;
    static final int BONE_LEN = 15, BONE_H = 2;
    static final int P_V0 = 0;                          // 10 franjas de 5
    static final int C_V0 = P_V0 + P_LEN.length * (P_H + 1);   // 7 franjas de 4
    static final int BONE_V = C_V0 + C_LEN.length * (C_H + 1);

    private final ModelRenderer right, left;
    private final ModelRenderer[] rightFeathers, leftFeathers;
    private final float[] baseAng;

    public WingsModel() {
        textureWidth = 64;
        textureHeight = 128;
        int n = P_LEN.length + C_LEN.length + 1;
        right = new ModelRenderer(this, 0, 0);
        right.setRotationPoint(-1f, 1f, 2.2f);
        left = new ModelRenderer(this, 0, 0);
        left.setRotationPoint(1f, 1f, 2.2f);
        rightFeathers = new ModelRenderer[n];
        leftFeathers = new ModelRenderer[n];
        baseAng = new float[n];
        int k = 0;

        // hueso: borde de ataque, apunta hacia arriba y afuera
        k = add(k, BONE_LEN, BONE_H, 0, BONE_V, 0.66f, 0.00f);
        // plumas largas (de arriba hacia abajo); z alterna para que las vecinas no se pisen
        for (int i = 0; i < P_LEN.length; i++) {
            float ang = 0.50f - i * 0.15f;
            k = add(k, P_LEN[i], P_H, 0, P_V0 + i * (P_H + 1), ang, 0.10f + (i % 2) * 0.35f);
        }
        // coberteras: mas cortas, delante (hacia afuera) de las largas
        for (int i = 0; i < C_LEN.length; i++) {
            float ang = 0.42f - i * 0.19f;
            k = add(k, C_LEN[i], C_H, 0, C_V0 + i * (C_H + 1), ang, 0.95f + (i % 2) * 0.30f);
        }
    }

    /** Crea una pluma en cada ala y devuelve el siguiente indice. */
    private int add(int k, int len, int h, int u, int v, float ang, float z) {
        ModelRenderer r = new ModelRenderer(this, u, v);
        r.setRotationPoint(0f, 0f, z);
        r.addBox(-len, -h / 2f, 0f, len, h, 1);
        r.rotateAngleZ = ang;
        right.addChild(r);
        rightFeathers[k] = r;

        ModelRenderer l = new ModelRenderer(this, u, v);
        l.mirror = true;
        l.setRotationPoint(0f, 0f, z);
        l.addBox(0f, -h / 2f, 0f, len, h, 1);
        l.rotateAngleZ = -ang;
        left.addChild(l);
        leftFeathers[k] = l;
        baseAng[k] = ang;
        return k + 1;
    }

    /**
     * open: apertura de cada ala en radianes (0 = pegadas a la espalda).
     * flap: cuanto "ondula" la punta respecto a la base (0 = rigidas); ticks: tiempo para desfasar cada pluma.
     */
    public void renderWings(float scale, float open, float flap, float ticks) {
        right.rotateAngleY = open;
        left.rotateAngleY = -open;
        if (flap > 0f) {
            for (int i = 0; i < rightFeathers.length; i++) {
                // cada pluma se retrasa un poco respecto a la anterior: se ve una onda que recorre el ala
                float w = (float) Math.sin(ticks * 0.55f - i * 0.22f) * flap;
                rightFeathers[i].rotateAngleY = w;
                leftFeathers[i].rotateAngleY = -w;
            }
        } else {
            for (int i = 0; i < rightFeathers.length; i++) { rightFeathers[i].rotateAngleY = 0f; leftFeathers[i].rotateAngleY = 0f; }
        }
        right.render(scale);
        left.render(scale);
    }
}
