package com.atenas.client.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Alas de plumas. La forma sale de {@link WingMesh} (cada pluma es una tira de segmentos que ondula); aqui solo se calcula
 * la apertura del ala y se entregan los vertices al Tessellator. De lejos se dibujan con menos segmentos y una sola cara.
 */
public class WingsLayer implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer renderer;
    private final WingMesh mesh = new WingMesh();
    private final TessSink sink = new TessSink();

    public WingsLayer(RenderPlayer renderer) { this.renderer = renderer; }

    /** Entrega los vertices de la malla al WorldRenderer del Tessellator (formato posicion + textura + normal). */
    private static final class TessSink implements WingMesh.Sink {
        WorldRenderer wr;
        float nx, ny = 1f, nz;

        @Override public void normal(float x, float y, float z) { nx = x; ny = y; nz = z; }

        @Override public void vertex(float x, float y, float z, float u, float v) {
            wr.pos(x, y, z).tex(u, v).normal(nx, ny, nz).endVertex();
        }
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float pt,
                              float age, float headYaw, float headPitch, float scale) {
        Minecraft mc = Minecraft.getMinecraft();
        if (p.isInvisible()) return;
        Cosmetics.Item item = Cosmetics.of(p, Cosmetics.Type.WINGS);
        if (item == null || !Cosmetics.inRange(p)) return;

        double d2 = (p == mc.thePlayer || mc.thePlayer == null) ? 0.0 : p.getDistanceSqToEntity(mc.thePlayer);
        int segs = WingMesh.segsFor(d2);
        boolean twoSided = WingMesh.twoSidedFor(d2);

        mc.getTextureManager().bindTexture(item.texture);
        // quietas con un respiro leve en el suelo; abiertas y aleteando en el aire
        boolean air = !p.onGround && !p.isInWater() && p.ridingEntity == null;
        float open = air ? 0.50f + (float) Math.sin(age * 0.55f) * 0.32f
                         : 0.85f + (float) Math.sin(age * 0.07f) * 0.05f;
        if (Cosmetics.of(p, Cosmetics.Type.CAPE) != null) open += 0.14f; // mas abiertas para que se vea la capa entre ellas
        if (p.isSneaking()) open = 1.15f;
        float flap = air ? 0.10f : 0f; // en el aire las plumas ondulan; en el suelo quedan quietas

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        if (twoSided) GlStateManager.enableCull(); else GlStateManager.disableCull();
        GlStateManager.pushMatrix();
        if (p.isSneaking()) GlStateManager.rotate(28.6f, 1f, 0f, 0f);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL); // 7 = GL_QUADS
        sink.wr = wr;
        mesh.emit(sink, open, age, flap, segs, twoSided, 0.0625f);
        sink.wr = null;
        t.draw();

        GlStateManager.popMatrix();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
    }

    @Override
    public boolean shouldCombineTextures() { return false; }
}
