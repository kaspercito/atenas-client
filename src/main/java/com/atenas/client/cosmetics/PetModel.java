package com.atenas.client.cosmetics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * Modelo de una mascota (un ModelRenderer por pieza), construido generico desde pets/models.json (ver {@link PetSpec}).
 * Hay una instancia por tipo de modelo. Textura: hoja logica 128x64 (la imagen real es mas grande, las coordenadas son proporcionales).
 */
public class PetModel extends ModelBase {
    private final PetSpec.Kind k;
    private final List<ModelRenderer> roots = new ArrayList<ModelRenderer>();
    private final ModelRenderer[] part = new ModelRenderer[PetSpec.N];
    private final float[][] base = new float[PetSpec.N][];
    private final PetSpec.Pose pose = new PetSpec.Pose();

    public PetModel(PetSpec.Kind kind) {
        this.k = kind;
        textureWidth = PetSpec.texW;
        textureHeight = PetSpec.texH;
        Map<String, ModelRenderer> byName = new HashMap<String, ModelRenderer>();
        for (PetSpec.Piece p : k.pieces) {
            ModelRenderer r = new ModelRenderer(this, p.uv[0], p.uv[1]);
            r.addBox(p.o[0], p.o[1], p.o[2], (int) p.s[0], (int) p.s[1], (int) p.s[2]);
            r.setRotationPoint(p.at[0], p.at[1], p.at[2]);
            byName.put(p.n, r);
            if (p.p == null || p.p.isEmpty()) roots.add(r); else byName.get(p.p).addChild(r);
            int s = PetSpec.slot(p.n);
            if (s >= 0) { part[s] = r; base[s] = p.at; }
        }
    }

    public PetSpec.Kind kind() { return k; }

    /** Dibuja la mascota (la textura ya debe estar puesta y la matriz en el marco del modelo, pies en y = 24). */
    public void render(PetBrain brain, float pt, float scale) {
        PetSpec.pose(k, brain, pt, pose);
        for (int i = 0; i < PetSpec.N; i++) {
            ModelRenderer r = part[i];
            if (r == null) continue;
            r.rotateAngleX = pose.rx[i]; r.rotateAngleY = pose.ry[i]; r.rotateAngleZ = pose.rz[i];
            r.rotationPointX = base[i][0] + pose.dx[i];
            r.rotationPointY = base[i][1] + pose.dy[i];
            r.rotationPointZ = base[i][2] + pose.dz[i];
        }
        for (ModelRenderer r : roots) r.render(scale);
    }
}
