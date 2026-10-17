package com.atenas.client.cosmetics;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;

/** Envuelve la capa de Mojang: si llevas una capa de Atenas, la de Minecraft no se dibuja (si no, se ve por debajo). */
public class VanillaCapeGuard implements LayerRenderer<AbstractClientPlayer> {
    private final LayerRenderer<AbstractClientPlayer> inner;

    public VanillaCapeGuard(LayerRenderer<AbstractClientPlayer> inner) { this.inner = inner; }

    @Override
    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float pt,
                              float age, float headYaw, float headPitch, float scale) {
        if (Cosmetics.of(p, Cosmetics.Type.CAPE) != null) return;
        inner.doRenderLayer(p, limbSwing, limbSwingAmount, pt, age, headYaw, headPitch, scale);
    }

    @Override
    public boolean shouldCombineTextures() { return inner.shouldCombineTextures(); }
}
