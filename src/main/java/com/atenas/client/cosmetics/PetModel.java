package com.atenas.client.cosmetics;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * Modelo de una mascota (un ModelRenderer por pieza). Las medidas, posiciones y la animacion vienen de {@link PetSpec};
 * aqui solo se construyen las cajas y se aplican los angulos. Hay una instancia por tipo de modelo (perro, husky, gato, zorro).
 * Textura: hoja logica 64x32 (la imagen real es mas grande, las coordenadas son proporcionales).
 */
public class PetModel extends ModelBase {
    private final PetSpec.Kind k;
    private final ModelRenderer body, head, snout, earL, earR, tail1, tail2, legFR, legFL, legRR, legRL;
    private final float[] pose = new float[PetSpec.POSE_SIZE];

    public PetModel(PetSpec.Kind kind) {
        this.k = kind;
        textureWidth = 64;
        textureHeight = 32;

        body = box(k.body, -k.body.w / 2f, -k.body.h / 2f, -k.body.d / 2f);
        body.setRotationPoint(0f, k.bodyY(), 0f);

        head = box(k.head, -k.head.w / 2f, -k.head.h / 2f, -k.head.d);
        head.setRotationPoint(0f, k.headY(), k.headZ());
        body.addChild(head);

        snout = box(k.snout, -k.snout.w / 2f, 0f, -k.snout.d);
        snout.setRotationPoint(0f, k.snoutY(), k.snoutZ());
        head.addChild(snout);

        earL = box(k.ear, -k.ear.w / 2f, -k.ear.h, -k.ear.d / 2f);
        earL.setRotationPoint(k.earX(), k.earY(), k.earZ());
        head.addChild(earL);
        earR = box(k.ear, -k.ear.w / 2f, -k.ear.h, -k.ear.d / 2f);
        earR.setRotationPoint(-k.earX(), k.earY(), k.earZ());
        head.addChild(earR);

        tail1 = box(k.tail1, -k.tail1.w / 2f, -k.tail1.h / 2f, 0f);
        tail1.setRotationPoint(0f, k.tailY(), k.tailZ());
        body.addChild(tail1);
        tail2 = box(k.tail2, -k.tail2.w / 2f, -k.tail2.h / 2f, 0f);
        tail2.setRotationPoint(0f, 0f, k.tail1.d);
        tail1.addChild(tail2);

        legFR = leg(-k.legX(), k.frontLegZ());
        legFL = leg(k.legX(), k.frontLegZ());
        legRR = leg(-k.legX(), k.rearLegZ());
        legRL = leg(k.legX(), k.rearLegZ());
    }

    private ModelRenderer box(PetSpec.Part p, float x, float y, float z) {
        ModelRenderer r = new ModelRenderer(this, p.u, p.v);
        r.addBox(x, y, z, p.w, p.h, p.d);
        return r;
    }

    private ModelRenderer leg(float x, float z) {
        ModelRenderer r = box(k.leg, -k.leg.w / 2f, 0f, -k.leg.d / 2f);
        r.setRotationPoint(x, k.legY(), z);
        return r;
    }

    /** Dibuja la mascota (la textura ya debe estar puesta y la matriz en el marco del modelo, pies en y = 24). */
    public void render(PetBrain brain, float pt, float scale) {
        PetSpec.pose(k, brain, pt, pose);
        body.rotateAngleX = pose[PetSpec.BODY_X];
        body.rotationPointY = k.bodyY() + pose[PetSpec.BODY_DY];
        head.rotateAngleX = pose[PetSpec.HEAD_X];
        head.rotateAngleY = pose[PetSpec.HEAD_Y];
        snout.rotateAngleX = pose[PetSpec.SNOUT_X];
        earL.rotateAngleZ = pose[PetSpec.EAR_L_Z];
        earR.rotateAngleZ = pose[PetSpec.EAR_R_Z];
        tail1.rotateAngleX = pose[PetSpec.TAIL_X];
        tail1.rotateAngleY = pose[PetSpec.TAIL_Y];
        tail2.rotateAngleX = pose[PetSpec.TAIL2_X];
        legFR.rotateAngleX = pose[PetSpec.LEG_FR];
        legFL.rotateAngleX = pose[PetSpec.LEG_FL];
        legRR.rotateAngleX = pose[PetSpec.LEG_RR];
        legRL.rotateAngleX = pose[PetSpec.LEG_RL];
        legRR.rotationPointY = legRL.rotationPointY = k.legY() + pose[PetSpec.REAR_DY];
        legRR.rotationPointZ = legRL.rotationPointZ = k.rearLegZ() + pose[PetSpec.REAR_DZ];

        body.render(scale);
        legFR.render(scale);
        legFL.render(scale);
        legRR.render(scale);
        legRL.render(scale);
    }
}
