package com.atenas.client.cosmetics;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
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

        // los del catalogo (texturas subidas con el UV de cajas de 64x64) siguen con el modelo de cajas
        HatMesh mesh = item.custom ? null : HatMesh.get(item.id);

        mc.getTextureManager().bindTexture(item.texture);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.pushMatrix();
        if (p.isSneaking()) GlStateManager.translate(0f, 0.2f, 0f);
        renderer.getMainModel().bipedHead.postRender(0.0625f);

        if (mesh == null) {
            boolean glow = item.glow || "halo".equals(item.modelId());
            if (glow) {
                GlStateManager.disableLighting();
                GlStateManager.translate(0f, (float) Math.sin(age * 0.1f) * 0.02f, 0f);
            }
            model.renderHat(item.modelId(), 0.0625f);
            if (glow) GlStateManager.enableLighting();
            GlStateManager.popMatrix();
            return;
        }

        TexFilter.prepare(item.texture, -1);
        if ("halo".equals(item.id)) GlStateManager.translate(0f, (float) Math.sin(age * 0.1f) * 0.02f, 0f);
        GlStateManager.disableCull();
        // piezas normales (con luz)
        for (int i = 0; i < mesh.lists.length; i++) {
            if ((mesh.flags[i] & HatMesh.EMIT) == 0) GL11.glCallList(mesh.lists[i]);
        }
        // piezas que brillan: sin luz, aditivas, a pleno brillo
        boolean any = false;
        for (int f : mesh.flags) if ((f & HatMesh.EMIT) != 0) any = true;
        if (any) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 1);
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            for (int i = 0; i < mesh.lists.length; i++) {
                int fl = mesh.flags[i];
                if ((fl & HatMesh.EMIT) == 0) continue;
                float a = 1f;
                if ((fl & HatMesh.PULSE) != 0) a = 0.62f + 0.38f * (float) Math.sin(age * 0.13f + i * 1.7f);
                if ((fl & HatMesh.SOFT) != 0) a *= 0.7f;
                GlStateManager.color(1f, 1f, 1f, a);
                GlStateManager.pushMatrix();
                if ((fl & HatMesh.SPIN) != 0) GlStateManager.rotate(age * 1.6f, 0f, 1f, 0f);
                GL11.glCallList(mesh.lists[i]);
                GlStateManager.popMatrix();
            }
            int br = p.getBrightnessForRender(pt);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) (br % 65536), (float) (br / 65536));
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() { return false; }
}
