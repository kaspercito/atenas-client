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
        boolean air = !p.onGround;
        float speed = air ? 0.55f : 0.12f;
        float amp = air ? 22f : 5f;
        float angle = 18f + (float) Math.sin(age * speed) * amp;

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        if (p.isSneaking()) GlStateManager.rotate(28.6f, 1f, 0f, 0f);
        GlStateManager.translate(0f, 0.02f, 0.14f);
        model.renderWings(0.0625f, angle);
        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
    }

    @Override
    public boolean shouldCombineTextures() { return false; }
}
