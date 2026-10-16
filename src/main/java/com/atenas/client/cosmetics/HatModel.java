package com.atenas.client.cosmetics;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * Sombreros hechos con cajas. El origen es el pivote de la cabeza (y negativo = arriba; la cabeza va de y=-8 a 0).
 * Cada textura es de 64x64: todo el lienzo es el color base y la franja de abajo (v >= 48) es el color de acento.
 * Las cajas del color base usan el offset (0,0); las de acento, (0,48). "galera" tiene su propia banda en (0,32).
 */
public class HatModel extends ModelBase {
    private final Map<String, ModelRenderer> hats = new HashMap<String, ModelRenderer>();

    public HatModel() {
        textureWidth = 64;
        textureHeight = 64;

        ModelRenderer halo = part(0, 0);
        halo.addBox(-5f, -12f, -5f, 10, 1, 1);
        halo.addBox(-5f, -12f, 4f, 10, 1, 1);
        halo.addBox(-5f, -12f, -4f, 1, 1, 8);
        halo.addBox(4f, -12f, -4f, 1, 1, 8);
        hats.put("halo", halo);

        ModelRenderer crown = part(0, 0);
        crown.addBox(-5f, -10f, -5f, 10, 2, 1);
        crown.addBox(-5f, -10f, 4f, 10, 2, 1);
        crown.addBox(-5f, -10f, -4f, 1, 2, 8);
        crown.addBox(4f, -10f, -4f, 1, 2, 8);
        crown.addBox(-5f, -13f, -5f, 2, 3, 2);
        crown.addBox(3f, -13f, -5f, 2, 3, 2);
        crown.addBox(-5f, -13f, 3f, 2, 3, 2);
        crown.addBox(3f, -13f, 3f, 2, 3, 2);
        crown.addBox(-1f, -12f, -5f, 2, 2, 1);
        crown.addBox(-1f, -12f, 4f, 2, 2, 1);
        hats.put("corona", crown);

        ModelRenderer topHat = part(0, 0);
        topHat.addBox(-6f, -9f, -6f, 12, 1, 12);
        topHat.setTextureOffset(0, 16);
        topHat.addBox(-4f, -16f, -4f, 8, 7, 8);
        topHat.setTextureOffset(0, 32);
        topHat.addBox(-4f, -11f, -4f, 8, 2, 8, 0.25f);
        hats.put("galera", topHat);

        // orejas de gato: oreja oscura + interior rosa
        ModelRenderer cat = part(0, 0);
        cat.addBox(-4f, -11f, -3f, 3, 3, 2);
        cat.addBox(1f, -11f, -3f, 3, 3, 2);
        cat.addBox(-4f, -13f, -3f, 1, 2, 2);
        cat.addBox(3f, -13f, -3f, 1, 2, 2);
        cat.setTextureOffset(0, 48);
        cat.addBox(-3.5f, -10.5f, -3.2f, 2, 2, 1);
        cat.addBox(1.5f, -10.5f, -3.2f, 2, 2, 1);
        hats.put("gato", cat);

        // cuernos de demonio: base roja, punta oscura
        ModelRenderer horns = part(0, 0);
        horns.addBox(-5f, -10f, -1f, 2, 2, 2);
        horns.addBox(3f, -10f, -1f, 2, 2, 2);
        horns.addBox(-5.5f, -12f, -0.5f, 1, 2, 1);
        horns.addBox(4.5f, -12f, -0.5f, 1, 2, 1);
        horns.setTextureOffset(0, 48);
        horns.addBox(-6f, -14f, -0.5f, 1, 2, 1);
        horns.addBox(5f, -14f, -0.5f, 1, 2, 1);
        hats.put("cuernos", horns);

        // sombrero de mago: ala + cono apilado, con banda dorada
        ModelRenderer wizard = part(0, 0);
        wizard.addBox(-7f, -9f, -7f, 14, 1, 14);
        wizard.addBox(-4f, -12f, -4f, 8, 3, 8);
        wizard.addBox(-3f, -15f, -3f, 6, 3, 6);
        wizard.addBox(-2f, -18f, -2f, 4, 3, 4);
        wizard.addBox(-1f, -20f, -1f, 2, 2, 2);
        wizard.setTextureOffset(0, 48);
        wizard.addBox(-4f, -10f, -4f, 8, 1, 8, 0.3f);
        hats.put("mago", wizard);

        // auriculares: arco y almohadillas oscuras, detalle de acento en cada lado
        ModelRenderer phones = part(0, 0);
        phones.addBox(-5.5f, -10f, -0.5f, 11, 1, 1);
        phones.addBox(-5.5f, -9f, -0.5f, 1, 4, 1);
        phones.addBox(4.5f, -9f, -0.5f, 1, 4, 1);
        phones.addBox(-6.5f, -5f, -2f, 2, 4, 4);
        phones.addBox(4.5f, -5f, -2f, 2, 4, 4);
        phones.setTextureOffset(0, 48);
        phones.addBox(-7f, -4.5f, -1f, 1, 3, 2);
        phones.addBox(6f, -4.5f, -1f, 1, 3, 2);
        hats.put("auriculares", phones);

        // orejas de conejo: blancas con interior rosa
        ModelRenderer bunny = part(0, 0);
        bunny.addBox(-3.5f, -17f, -1f, 2, 9, 1);
        bunny.addBox(1.5f, -17f, -1f, 2, 9, 1);
        bunny.setTextureOffset(0, 48);
        bunny.addBox(-3f, -16f, -1.2f, 1, 7, 1);
        bunny.addBox(2f, -16f, -1.2f, 1, 7, 1);
        hats.put("conejo", bunny);

        // gorra: copa y visera con boton de acento
        ModelRenderer cap = part(0, 0);
        cap.addBox(-4f, -9f, -4f, 8, 2, 8, 0.5f);
        cap.setTextureOffset(0, 48);
        cap.addBox(-4f, -8.5f, -8f, 8, 1, 4);
        cap.addBox(-0.5f, -10.5f, -0.5f, 1, 1, 1);
        hats.put("gorra", cap);
    }

    private ModelRenderer part(int u, int v) { return new ModelRenderer(this, u, v); }

    public void renderHat(String id, float scale) {
        ModelRenderer r = hats.get(id);
        if (r != null) r.render(scale);
    }
}
