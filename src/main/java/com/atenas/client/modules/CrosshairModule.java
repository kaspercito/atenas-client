package com.atenas.client.modules;

import org.lwjgl.opengl.GL11;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;
import com.atenas.client.gui.Draw;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

/** Mira personalizada: 5 estilos, tamano, separacion (fija o dinamica), grosor, opacidad, contorno y color (otro color al apuntar a una entidad). */
public class CrosshairModule extends Module {
    private final Setting style, size, gap, thickness, dot, outline, opacity, color, enemyOn, enemyColor, dynamic;
    private float dyn, dynPrev;

    public CrosshairModule() {
        super("Mira", "Mira personalizada: estilo (cruz, T, punto, círculo, cuadrado), tamaño, separación, grosor, opacidad y color.", Category.PVP);
        style = addChoice("Estilo", 0, "Cruz", "T", "Punto", "Círculo", "Cuadrado");
        size = addNum("Tamaño", 6, 1, 20, 1);
        gap = addNum("Separación", 3, 0, 12, 1);
        thickness = addNum("Grosor", 1, 1, 5, 1);
        opacity = addNum("Opacidad", 1.0, 0.2, 1.0, 0.1);
        dot = addBool("Punto central", false);
        outline = addBool("Contorno", true);
        dynamic = addBool("Separación dinámica (se abre al moverte/saltar)", false);
        color = addColor("Color", 1);
        enemyOn = addBool("Cambiar de color al apuntar a una entidad", true);
        enemyColor = addColor("Color al apuntar", 4);
    }

    @Override
    public void onTick() {
        dynPrev = dyn;
        float target = 0f;
        EntityPlayer p = mc.thePlayer;
        if (dynamic.bool && p != null) {
            double sp = Math.sqrt(p.motionX * p.motionX + p.motionZ * p.motionZ);
            if (sp > 0.03) target += p.isSprinting() ? 4f : 2f;
            if (!p.onGround) target += 3f;
            if (p.isSneaking()) target -= 1f;
        }
        dyn += (target - dyn) * 0.35f;
    }

    private int currentColor(int force) {
        int c = color.getColor();
        boolean aiming = force == 1 || (force == 0 && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY);
        if (enemyOn.bool && aiming) c = enemyColor.getColor();
        int a = Math.max(0, Math.min(255, (int) (opacity.num * 255)));
        return (a << 24) | (c & 0xFFFFFF);
    }

    @Override
    public void onOverlayPre(RenderGameOverlayEvent.Pre e) {
        if (e.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        if (mc.gameSettings.thirdPersonView != 0 || mc.gameSettings.showDebugInfo) return;
        e.setCanceled(true);
        ScaledResolution sr = e.resolution;
        float dg = dynPrev + (dyn - dynPrev) * e.partialTicks;
        draw(sr.getScaledWidth() / 2, sr.getScaledHeight() / 2, sr.getScaleFactor(), Math.round(dg), 0);
        restoreGui();
    }

    /**
     * Gui.drawRect y el circulo dejan el GL "pintando" con el color de la mira (verde, rojo...). El juego sigue dibujando la
     * vida, el hambre y la armadura con ese color puesto y salian teñidos. Aqui se deja todo como lo deja la mira de vanilla:
     * color blanco, sin mezcla, textura de iconos puesta.
     */
    private void restoreGui() {
        GL11.glLineWidth(1f);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(Gui.icons);
    }

    /**
     * Dibuja la mira centrada en (cx, cy) con los ajustes actuales. Lo usan el juego y la vista previa del menu,
     * asi lo que ves en el menu es exactamente lo que sale en pantalla.
     * @param extraGap separacion extra (la dinamica)
     * @param force 0 = segun lo que apuntas, 1 = como si apuntaras a una entidad, 2 = sin apuntar
     */
    public void draw(int cx, int cy, int scaleFactor, int extraGap, int force) {
        int s = size.getInt(), t = thickness.getInt();
        int g = Math.max(0, gap.getInt() + extraGap);
        int lo = -(t / 2);
        int c = currentColor(force);
        int kind = style.index();

        int outlineArgb = (c >>> 24) << 24;
        boolean hasDot = dot.bool || kind == 2;

        // El circulo son lineas (su propio estado de GL); lo demas son rectangulos lisos y salen TODOS en una sola llamada de
        // dibujo (antes cada Gui.drawRect abria y cerraba el Tessellator: hasta ~10 llamadas por cuadro con contorno y punto).
        if (kind == 3) {
            float r = g + s;
            if (outline.bool) circle(cx, cy, r, t + 2f, outlineArgb, scaleFactor);
            circle(cx, cy, r, t, c, scaleFactor);
        }
        if (kind != 3 || hasDot) {
            Draw.batchBegin();
            if (kind == 0 || kind == 1) {
                // izquierda, derecha, abajo y arriba (la T no dibuja la de arriba)
                int l1 = cx + lo - g - s, l2 = cx + lo - g;
                int r1 = cx + lo + t + g, r2 = cx + lo + t + g + s;
                int d1 = cy + lo + t + g, d2 = cy + lo + t + g + s;
                int u1 = cy + lo - g - s, u2 = cy + lo - g;
                if (outline.bool) {
                    Draw.batchFill(l1 - 1, cy + lo - 1, l2 + 1, cy + lo + t + 1, outlineArgb);
                    Draw.batchFill(r1 - 1, cy + lo - 1, r2 + 1, cy + lo + t + 1, outlineArgb);
                    Draw.batchFill(cx + lo - 1, d1 - 1, cx + lo + t + 1, d2 + 1, outlineArgb);
                    if (kind == 0) Draw.batchFill(cx + lo - 1, u1 - 1, cx + lo + t + 1, u2 + 1, outlineArgb);
                }
                Draw.batchFill(l1, cy + lo, l2, cy + lo + t, c);
                Draw.batchFill(r1, cy + lo, r2, cy + lo + t, c);
                Draw.batchFill(cx + lo, d1, cx + lo + t, d2, c);
                if (kind == 0) Draw.batchFill(cx + lo, u1, cx + lo + t, u2, c);
            } else if (kind == 4) {
                int r = g + s;
                if (outline.bool) frame(cx, cy, r + 1, t + 2, outlineArgb);
                frame(cx, cy, r, t, c);
            }
            if (hasDot) {
                int d = kind == 2 ? Math.max(t, 2) : t;
                int dl = -(d / 2);
                if (outline.bool) Draw.batchFill(cx + dl - 1, cy + dl - 1, cx + dl + d + 1, cy + dl + d + 1, outlineArgb);
                Draw.batchFill(cx + dl, cy + dl, cx + dl + d, cy + dl + d, c);
            }
            Draw.batchEnd();
        }
        GlStateManager.color(1f, 1f, 1f, 1f); // tambien la vista previa del menu: que no deje el color puesto
    }

    /** Marco cuadrado de "radio" r alrededor del centro (4 rectangulos; hay que llamarlo dentro de un lote de Draw). */
    private void frame(int cx, int cy, int r, int t, int c) {
        Draw.batchFill(cx - r, cy - r, cx + r + 1, cy - r + t, c);
        Draw.batchFill(cx - r, cy + r - t + 1, cx + r + 1, cy + r + 1, c);
        Draw.batchFill(cx - r, cy - r + t, cx - r + t, cy + r - t + 1, c);
        Draw.batchFill(cx + r - t + 1, cy - r + t, cx + r + 1, cy + r - t + 1, c);
    }

    private void circle(int cx, int cy, float radius, float width, int argb, int scaleFactor) {
        float a = (argb >> 24 & 255) / 255f, r = (argb >> 16 & 255) / 255f, g = (argb >> 8 & 255) / 255f, b = (argb & 255) / 255f;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(Math.max(1f, width * scaleFactor));
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(2, DefaultVertexFormats.POSITION);   // 2 = GL_LINE_LOOP
        for (int i = 0; i < 48; i++) {
            double ang = i * Math.PI * 2.0 / 48.0;
            wr.pos(cx + 0.5 + Math.cos(ang) * radius, cy + 0.5 + Math.sin(ang) * radius, 0.0).endVertex();
        }
        tess.draw();
        GL11.glLineWidth(1f);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
