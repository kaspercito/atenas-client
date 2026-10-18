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
 * Alas de plumas ({@link WingMesh}) y de membrana ({@link MembraneWing}). La forma sale de la malla (cada pluma es una tira de segmentos que ondula); aqui solo se calcula
 * la apertura del ala y se entregan los vertices al Tessellator. De lejos se dibujan con menos segmentos y una sola cara.
 */
public class WingsLayer implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer renderer;
    private final WingMesh mesh = new WingMesh();
    private final MembraneWing membrane = new MembraneWing();
    private final TessSink sink = new TessSink();

    /** Estado propio por jugador: la fase del aleteo avanza SIEMPRE y la mezcla suelo/aire es gradual (antes saltaba y se "reseteaba"). */
    private static final class State { float air, phase; long last; }
    private final java.util.Map<Integer, State> states = new java.util.HashMap<Integer, State>();

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
        boolean airNow = !p.onGround && !p.isInWater() && p.ridingEntity == null;
        State st = states.get(p.getEntityId());
        long now = System.nanoTime();
        if (st == null) { if (states.size() > 200) states.clear(); st = new State(); st.last = now; states.put(p.getEntityId(), st); }
        float dt = Math.min(0.1f, (now - st.last) / 1.0e9f) * 20f; // en ticks
        st.last = now;
        st.air += ((airNow ? 1f : 0f) - st.air) * Math.min(1f, dt * 0.25f); // 0 = suelo, 1 = aire, transicion suave
        st.phase += dt * (0.35f + st.air * 0.65f);                          // nunca se detiene ni retrocede
        float a = st.air;
        float openGround = 0.85f + (float) Math.sin(st.phase * 0.2f) * 0.05f;
        float openAir = 0.50f + (float) Math.sin(st.phase * 0.55f) * 0.32f;
        float open = openGround * (1f - a) + openAir * a;
        // Con capa: las alas nacen justo detras de la capa (zoff) y casi pegadas al centro (outset): no la tapan ni quedan separadas.
        boolean cape = Cosmetics.of(p, Cosmetics.Type.CAPE) != null;
        if (cape) open *= 0.80f;
        open *= item.openMul; // cada cosmetico puede abrir mas o menos (por defecto 1)
        float outset = cape ? 1.2f : 0f; // casi pegadas a la columna: nacen detras de la capa, no separadas
        float zoff = cape ? 2.6f : 0f; // sin capa: como antes; con capa: justo detras de ella
        mesh.outset = outset; mesh.zoff = zoff;
        membrane.outset = outset; membrane.zoff = zoff;
        if (p.isSneaking()) open = (cape ? 0.5f : 0.6f) * item.openMul; // agachado: mas cerradas, pegadas al cuerpo
        float flap = 0.025f + 0.075f * a; // siempre ondulan un poco; mas en el aire

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        if (twoSided) GlStateManager.enableCull(); else GlStateManager.disableCull();
        GlStateManager.pushMatrix();
        if (p.isSneaking()) { GlStateManager.translate(0f, 0.2f, 0f); GlStateManager.rotate(28.6f, 1f, 0f, 0f); } // ModelBiped baja el cuerpo 0.2 al agacharse

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL); // 7 = GL_QUADS
        sink.wr = wr;
        if (item.membrane) membrane.emit(sink, open, st.phase, flap, twoSided, 0.0625f);
        else mesh.emit(sink, open, st.phase, flap, segs, twoSided, 0.0625f);
        sink.wr = null;
        t.draw();

        GlStateManager.popMatrix();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
    }

    @Override
    public boolean shouldCombineTextures() { return false; }
}
