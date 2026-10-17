package com.atenas.client.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;

public class WingsLayer implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer renderer;
    private final WingsModel model = new WingsModel();

    public WingsLayer(RenderPlayer renderer) { this.renderer = renderer; }

    @Override
    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float pt,
                              float age, float headYaw, float headPitch, float scale) {
        Minecraft mc = Minecraft.getMinecraft();
        if (p.isInvisible()) return;
        Cosmetics.Item item = Cosmetics.of(p, Cosmetics.Type.WINGS);
        if (item == null) return;

        mc.getTextureManager().bindTexture(item.texture);
        // quietas con un respiro leve en el suelo; abiertas y aleteando en el aire
        boolean air = !p.onGround && !p.isInWater() && p.ridingEntity == null;
        float open = air ? 0.45f + (float) Math.sin(age * 0.55f) * 0.30f
                         : 0.95f + (float) Math.sin(age * 0.07f) * 0.06f;
        if (p.isSneaking()) open = 1.15f;

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        if (p.isSneaking()) GlStateManager.rotate(28.6f, 1f, 0f, 0f);
        model.renderWings(0.0625f, open);
        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
    }

    @Override
    public boolean shouldCombineTextures() { return false; }
}
