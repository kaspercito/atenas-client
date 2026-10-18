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
        halo.setTextureOffset(0, 0);
        halo.addBox(-5.0f, -12.0f, -5.0f, 10, 1, 1);
        halo.addBox(-5.0f, -12.0f, 4.0f, 10, 1, 1);
        halo.addBox(-5.0f, -12.0f, -4.0f, 1, 1, 8);
        halo.addBox(4.0f, -12.0f, -4.0f, 1, 1, 8);
        halo.addBox(-4.0f, -13.0f, -4.0f, 8, 1, 1, -0.2f);
        halo.addBox(-4.0f, -13.0f, 3.0f, 8, 1, 1, -0.2f);
        halo.addBox(-4.0f, -13.0f, -3.0f, 1, 1, 6, -0.2f);
        halo.addBox(3.0f, -13.0f, -3.0f, 1, 1, 6, -0.2f);
        halo.setTextureOffset(0, 48);
        halo.addBox(-1.0f, -12.0f, -6.0f, 2, 1, 1);
        halo.addBox(-1.0f, -12.0f, 5.0f, 2, 1, 1);
        halo.addBox(-6.0f, -12.0f, -1.0f, 1, 1, 2);
        halo.addBox(5.0f, -12.0f, -1.0f, 1, 1, 2);
        hats.put("halo", halo);

        ModelRenderer corona = part(0, 0);
        corona.setTextureOffset(0, 0);
        corona.addBox(-5.0f, -10.0f, -5.0f, 10, 2, 1);
        corona.addBox(-5.0f, -10.0f, 4.0f, 10, 2, 1);
        corona.addBox(-5.0f, -10.0f, -4.0f, 1, 2, 8);
        corona.addBox(4.0f, -10.0f, -4.0f, 1, 2, 8);
        corona.addBox(-5.0f, -13.0f, -5.0f, 2, 3, 2);
        corona.addBox(3.0f, -13.0f, -5.0f, 2, 3, 2);
        corona.addBox(-5.0f, -13.0f, 3.0f, 2, 3, 2);
        corona.addBox(3.0f, -13.0f, 3.0f, 2, 3, 2);
        corona.addBox(-1.0f, -12.0f, -5.0f, 2, 2, 1);
        corona.addBox(-1.0f, -12.0f, 4.0f, 2, 2, 1);
        corona.addBox(-5.0f, -12.0f, -1.0f, 1, 2, 2);
        corona.addBox(4.0f, -12.0f, -1.0f, 1, 2, 2);
        corona.addBox(-5.0f, -14.0f, -5.0f, 1, 1, 1);
        corona.addBox(4.0f, -14.0f, -5.0f, 1, 1, 1);
        corona.addBox(-5.0f, -14.0f, 4.0f, 1, 1, 1);
        corona.addBox(4.0f, -14.0f, 4.0f, 1, 1, 1);
        corona.addBox(-1.0f, -13.0f, -5.0f, 2, 1, 1, -0.25f);
        corona.addBox(-1.0f, -13.0f, 4.0f, 2, 1, 1, -0.25f);
        corona.setTextureOffset(0, 48);
        corona.addBox(-1.0f, -9.0f, -6.0f, 2, 1, 1);
        corona.addBox(-6.0f, -9.0f, -1.0f, 1, 1, 2);
        corona.addBox(5.0f, -9.0f, -1.0f, 1, 1, 2);
        corona.addBox(-1.0f, -9.0f, 5.0f, 2, 1, 1);
        corona.addBox(-4.0f, -11.0f, -6.0f, 1, 1, 1);
        corona.addBox(3.0f, -11.0f, -6.0f, 1, 1, 1);
        hats.put("corona", corona);

        ModelRenderer galera = part(0, 0);
        galera.setTextureOffset(0, 0);
        galera.addBox(-6.0f, -9.0f, -6.0f, 12, 1, 12);
        galera.addBox(-4.0f, -16.0f, -4.0f, 8, 7, 8);
        galera.addBox(-4.0f, -17.0f, -4.0f, 8, 1, 8, -0.3f);
        galera.addBox(-6.0f, -10.0f, -6.0f, 12, 1, 12, -0.45f);
        galera.setTextureOffset(0, 32);
        galera.addBox(-4.0f, -11.0f, -4.0f, 8, 2, 8, 0.25f);
        galera.setTextureOffset(0, 48);
        galera.addBox(3.0f, -13.0f, -5.0f, 1, 3, 2);
        galera.addBox(4.0f, -14.0f, -5.0f, 1, 2, 1);
        galera.addBox(-1.0f, -11.0f, -5.0f, 2, 2, 1, 0.1f);
        hats.put("galera", galera);

        ModelRenderer gato = part(0, 0);
        gato.setTextureOffset(0, 0);
        gato.addBox(-4.0f, -11.0f, -3.0f, 3, 3, 2);
        gato.addBox(1.0f, -11.0f, -3.0f, 3, 3, 2);
        gato.addBox(-4.0f, -13.0f, -3.0f, 1, 2, 2);
        gato.addBox(3.0f, -13.0f, -3.0f, 1, 2, 2);
        gato.addBox(-4.0f, -14.0f, -3.0f, 1, 1, 1, -0.2f);
        gato.addBox(3.0f, -14.0f, -3.0f, 1, 1, 1, -0.2f);
        gato.addBox(-4.0f, -9.0f, -4.0f, 8, 1, 8, 0.2f);
        gato.setTextureOffset(0, 48);
        gato.addBox(-3.0f, -10.0f, -4.0f, 2, 2, 1);
        gato.addBox(1.0f, -10.0f, -4.0f, 2, 2, 1);
        gato.addBox(-3.0f, -12.0f, -3.0f, 1, 1, 1, -0.15f);
        gato.addBox(2.0f, -12.0f, -3.0f, 1, 1, 1, -0.15f);
        gato.addBox(-1.0f, -9.0f, -5.0f, 2, 1, 1, 0.1f);
        hats.put("gato", gato);

        ModelRenderer cuernos = part(0, 0);
        cuernos.setTextureOffset(0, 0);
        cuernos.addBox(-6.0f, -10.0f, -2.0f, 3, 2, 3);
        cuernos.addBox(3.0f, -10.0f, -2.0f, 3, 2, 3);
        cuernos.addBox(-6.0f, -12.0f, -1.0f, 2, 2, 2);
        cuernos.addBox(4.0f, -12.0f, -1.0f, 2, 2, 2);
        cuernos.addBox(-7.0f, -14.0f, -1.0f, 2, 2, 2, -0.2f);
        cuernos.addBox(5.0f, -14.0f, -1.0f, 2, 2, 2, -0.2f);
        cuernos.addBox(-8.0f, -16.0f, -1.0f, 1, 2, 1);
        cuernos.addBox(7.0f, -16.0f, -1.0f, 1, 2, 1);
        cuernos.setTextureOffset(0, 48);
        cuernos.addBox(-8.0f, -18.0f, -1.0f, 1, 2, 1, -0.15f);
        cuernos.addBox(7.0f, -18.0f, -1.0f, 1, 2, 1, -0.15f);
        cuernos.addBox(-8.0f, -17.0f, -1.0f, 1, 1, 1, 0.1f);
        cuernos.addBox(7.0f, -17.0f, -1.0f, 1, 1, 1, 0.1f);
        hats.put("cuernos", cuernos);

        ModelRenderer mago = part(0, 0);
        mago.setTextureOffset(0, 0);
        mago.addBox(-7.0f, -9.0f, -7.0f, 14, 1, 14);
        mago.addBox(-4.0f, -12.0f, -4.0f, 8, 3, 8);
        mago.addBox(-3.0f, -15.0f, -3.0f, 6, 3, 6);
        mago.addBox(-2.0f, -18.0f, -2.0f, 4, 3, 4);
        mago.addBox(-1.0f, -20.0f, -1.0f, 2, 2, 2);
        mago.addBox(0.0f, -22.0f, 0.0f, 2, 2, 2);
        mago.addBox(1.0f, -23.0f, 1.0f, 1, 1, 2, -0.1f);
        mago.addBox(-7.0f, -10.0f, -7.0f, 14, 1, 14, -0.4f);
        mago.setTextureOffset(0, 48);
        mago.addBox(-4.0f, -10.0f, -4.0f, 8, 1, 8, 0.3f);
        mago.addBox(-1.0f, -10.0f, -5.0f, 2, 2, 1, 0.2f);
        mago.addBox(2.0f, -13.0f, -5.0f, 1, 1, 1);
        mago.addBox(-3.0f, -16.0f, -4.0f, 1, 1, 1);
        mago.addBox(1.0f, -17.0f, -3.0f, 1, 1, 1);
        mago.addBox(-3.0f, -11.0f, -5.0f, 1, 1, 1);
        hats.put("mago", mago);

        ModelRenderer auriculares = part(0, 0);
        auriculares.setTextureOffset(0, 0);
        auriculares.addBox(-5.0f, -10.0f, -1.0f, 11, 1, 2);
        auriculares.addBox(-6.0f, -9.0f, -1.0f, 1, 4, 2);
        auriculares.addBox(5.0f, -9.0f, -1.0f, 1, 4, 2);
        auriculares.addBox(-7.0f, -5.0f, -3.0f, 2, 4, 5);
        auriculares.addBox(5.0f, -5.0f, -3.0f, 2, 4, 5);
        auriculares.addBox(-6.0f, -10.0f, -1.0f, 1, 1, 2, -0.1f);
        auriculares.addBox(5.0f, -10.0f, -1.0f, 1, 1, 2, -0.1f);
        auriculares.setTextureOffset(0, 48);
        auriculares.addBox(-8.0f, -4.0f, -2.0f, 1, 3, 3);
        auriculares.addBox(7.0f, -4.0f, -2.0f, 1, 3, 3);
        auriculares.addBox(-7.0f, -2.0f, -5.0f, 1, 1, 3);
        auriculares.addBox(-7.0f, -2.0f, -6.0f, 2, 1, 1, 0.1f);
        auriculares.addBox(-2.0f, -11.0f, -1.0f, 4, 1, 2, -0.15f);
        hats.put("auriculares", auriculares);

        ModelRenderer conejo = part(0, 0);
        conejo.setTextureOffset(0, 0);
        conejo.addBox(-4.0f, -17.0f, -1.0f, 2, 9, 1);
        conejo.addBox(2.0f, -17.0f, -1.0f, 2, 9, 1);
        conejo.addBox(-4.0f, -19.0f, -2.0f, 2, 2, 2, -0.1f);
        conejo.addBox(2.0f, -19.0f, -2.0f, 2, 2, 2, -0.1f);
        conejo.addBox(-4.0f, -9.0f, -4.0f, 8, 1, 8, 0.2f);
        conejo.setTextureOffset(0, 48);
        conejo.addBox(-3.0f, -16.0f, -1.0f, 1, 7, 1);
        conejo.addBox(3.0f, -16.0f, -1.0f, 1, 7, 1);
        conejo.addBox(-3.0f, -18.0f, -2.0f, 1, 1, 1);
        conejo.addBox(3.0f, -18.0f, -2.0f, 1, 1, 1);
        hats.put("conejo", conejo);

        ModelRenderer gorra = part(0, 0);
        gorra.setTextureOffset(0, 0);
        gorra.addBox(-4.0f, -9.0f, -4.0f, 8, 2, 8, 0.5f);
        gorra.addBox(-4.0f, -10.0f, -4.0f, 8, 1, 8, 0.2f);
        gorra.setTextureOffset(0, 48);
        gorra.addBox(-4.0f, -8.0f, -8.0f, 8, 1, 4);
        gorra.addBox(-5.0f, -8.0f, -6.0f, 1, 1, 2);
        gorra.addBox(4.0f, -8.0f, -6.0f, 1, 1, 2);
        gorra.addBox(-1.0f, -11.0f, -1.0f, 2, 1, 2, -0.25f);
        gorra.addBox(-2.0f, -9.0f, -5.0f, 4, 2, 1);
        hats.put("gorra", gorra);
    }

    private ModelRenderer part(int u, int v) { return new ModelRenderer(this, u, v); }

    public void renderHat(String id, float scale) {
        ModelRenderer r = hats.get(id);
        if (r != null) r.render(scale);
    }
}
