package com.atenas.client.cosmetics;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class WingsModel extends ModelBase {
    private final ModelRenderer right;
    private final ModelRenderer left;

    public WingsModel() {
        textureWidth = 64;
        textureHeight = 32;
        right = new ModelRenderer(this, 0, 0);
        right.addBox(0f, -10f, 0f, 20, 22, 1);
        left = new ModelRenderer(this, 0, 0);
        left.mirror = true;
        left.addBox(-20f, -10f, 0f, 20, 22, 1);
    }

    /** angle: apertura de cada ala en grados (0 = pegadas a la espalda). */
    public void renderWings(float scale, float angle) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(-angle, 0f, 1f, 0f);
        right.render(scale);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.rotate(angle, 0f, 1f, 0f);
        left.render(scale);
        GlStateManager.popMatrix();
    }
}
