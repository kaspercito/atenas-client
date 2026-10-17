package com.atenas.client.modules;

import org.lwjgl.opengl.GL11;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

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

    private void bar(int x1, int y1, int x2, int y2, int c) {
        if (outline.bool) {
            int oa = (c >>> 24) << 24;
            Gui.drawRect(x1 - 1, y1 - 1, x2 + 1, y2 + 1, oa);
        }
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

        if (kind == 0 || kind == 1) {
            int[][] bars = {
                {cx + lo - g - s, cy + lo, cx + lo - g, cy + lo + t},          // izquierda
                {cx + lo + t + g, cy + lo, cx + lo + t + g + s, cy + lo + t},  // derecha
                {cx + lo, cy + lo + t + g, cx + lo + t, cy + lo + t + g + s},  // abajo
                {cx + lo, cy + lo - g - s, cx + lo + t, cy + lo - g}           // arriba (la T no la dibuja)
            };
            int n = kind == 1 ? 3 : 4;
            if (outline.bool) for (int i = 0; i < n; i++) bar(bars[i][0], bars[i][1], bars[i][2], bars[i][3], c);
            for (int i = 0; i < n; i++) Gui.drawRect(bars[i][0], bars[i][1], bars[i][2], bars[i][3], c);
        } else if (kind == 3) {
            float r = g + s;
            if (outline.bool) circle(cx, cy, r, t + 2f, (c >>> 24) << 24, scaleFactor);
            circle(cx, cy, r, t, c, scaleFactor);
        } else if (kind == 4) {
            int r = g + s;
            if (outline.bool) frame(cx, cy, r + 1, t + 2, (c >>> 24) << 24);
            frame(cx, cy, r, t, c);
        }
        if (dot.bool || kind == 2) {
            int d = kind == 2 ? Math.max(t, 2) : t;
            int dl = -(d / 2);
            if (outline.bool) Gui.drawRect(cx + dl - 1, cy + dl - 1, cx + dl + d + 1, cy + dl + d + 1, (c >>> 24) << 24);
            Gui.drawRect(cx + dl, cy + dl, cx + dl + d, cy + dl + d, c);
        }
        GlStateManager.color(1f, 1f, 1f, 1f); // tambien la vista previa del menu: que no deje el color puesto
    }

    /** Marco cuadrado de "radio" r alrededor del centro. */
    private void frame(int cx, int cy, int r, int t, int c) {
        Gui.drawRect(cx - r, cy - r, cx + r + 1, cy - r + t, c);
        Gui.drawRect(cx - r, cy + r - t + 1, cx + r + 1, cy + r + 1, c);
        Gui.drawRect(cx - r, cy - r + t, cx - r + t, cy + r - t + 1, c);
        Gui.drawRect(cx + r - t + 1, cy - r + t, cx + r + 1, cy + r - t + 1, c);
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
