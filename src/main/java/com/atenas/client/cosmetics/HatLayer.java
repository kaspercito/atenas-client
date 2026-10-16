package com.atenas.client.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;

public class HatLayer implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer renderer;
    private final HatModel model = new HatModel();

    public HatLayer(RenderPlayer renderer) { this.renderer = renderer; }

    @Override
    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float pt,
                              float age, float headYaw, float headPitch, float scale) {
        Minecraft mc = Minecraft.getMinecraft();
        if (p.isInvisible()) return;
        Cosmetics.Item item = Cosmetics.of(p, Cosmetics.Type.HAT);
        if (item == null) return;

        mc.getTextureManager().bindTexture(item.texture);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.pushMatrix();
        if (p.isSneaking()) GlStateManager.translate(0f, 0.2f, 0f);
        renderer.getMainModel().bipedHead.postRender(0.0625f);
        boolean glow = "halo".equals(item.id);
        if (glow) {
            GlStateManager.disableLighting();
            GlStateManager.translate(0f, (float) Math.sin(age * 0.1f) * 0.02f, 0f);
        }
        model.renderHat(item.id, 0.0625f);
        if (glow) GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() { return false; }
}
