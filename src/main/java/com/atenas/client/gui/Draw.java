package com.atenas.client.gui;

import org.lwjgl.opengl.GL11;

import com.atenas.client.core.Lang;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/** Utilidades de dibujo para las pantallas del cliente. */
public final class Draw {
    private Draw() {}

    private static final Gui GUI = new Gui();

    private static Minecraft mc() { return Minecraft.getMinecraft(); }

    /** Dibuja un trozo (u,v,w,h) de la textura ya enlazada (256x256). */
    public static void texRect(int x, int y, int u, int v, int w, int h) {
        GUI.drawTexturedModalRect(x, y, u, v, w, h);
    }

    public static void rect(int x, int y, int w, int h, int color) {
        Gui.drawRect(x, y, x + w, y + h, color);
    }

    public static void border(int x, int y, int w, int h, int color) {
        rect(x, y, w, 1, color);
        rect(x, y + h - 1, w, 1, color);
        rect(x, y, 1, h, color);
        rect(x + w - 1, y, 1, h, color);
    }

    /** Rectangulo con las esquinas cortadas en diagonal ("cut" pixeles). */
    public static void chamfer(int x, int y, int w, int h, int cut, int color) {
        rect(x, y + cut, w, h - 2 * cut, color);
        for (int i = 0; i < cut; i++) {
            rect(x + cut - i, y + i, w - 2 * (cut - i), 1, color);
            rect(x + cut - i, y + h - 1 - i, w - 2 * (cut - i), 1, color);
        }
    }

    /** Contorno de 1 px de un rectangulo con esquinas cortadas. */
    public static void chamferBorder(int x, int y, int w, int h, int cut, int color) {
        rect(x + cut, y, w - 2 * cut, 1, color);
        rect(x + cut, y + h - 1, w - 2 * cut, 1, color);
        rect(x, y + cut, 1, h - 2 * cut, color);
        rect(x + w - 1, y + cut, 1, h - 2 * cut, color);
        for (int i = 0; i < cut; i++) {
            rect(x + cut - 1 - i, y + i, 1, 1, color);
            rect(x + w - cut + i, y + i, 1, 1, color);
            rect(x + cut - 1 - i, y + h - 1 - i, 1, 1, color);
            rect(x + w - cut + i, y + h - 1 - i, 1, 1, color);
        }
    }

    public static int withAlpha(int rgb, float a) { return ((int) (255 * Math.max(0f, Math.min(1f, a))) << 24) | (rgb & 0xFFFFFF); }

    public static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static void gradient(int x, int y, int w, int h, int c1, int c2, boolean vertical) {
        float a1 = (c1 >> 24 & 255) / 255f, r1 = (c1 >> 16 & 255) / 255f, g1 = (c1 >> 8 & 255) / 255f, b1 = (c1 & 255) / 255f;
        float a2 = (c2 >> 24 & 255) / 255f, r2 = (c2 >> 16 & 255) / 255f, g2 = (c2 >> 8 & 255) / 255f, b2 = (c2 & 255) / 255f;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        if (vertical) {
            wr.pos(x + w, y, 0).color(r1, g1, b1, a1).endVertex();
            wr.pos(x, y, 0).color(r1, g1, b1, a1).endVertex();
            wr.pos(x, y + h, 0).color(r2, g2, b2, a2).endVertex();
            wr.pos(x + w, y + h, 0).color(r2, g2, b2, a2).endVertex();
        } else {
            wr.pos(x + w, y, 0).color(r2, g2, b2, a2).endVertex();
            wr.pos(x, y, 0).color(r1, g1, b1, a1).endVertex();
            wr.pos(x, y + h, 0).color(r1, g1, b1, a1).endVertex();
            wr.pos(x + w, y + h, 0).color(r2, g2, b2, a2).endVertex();
        }
        t.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    // Todo lo que se dibuja pasa por Lang.tr: el texto sale en el idioma del juego (ver Lang).
    public static void text(String s, int x, int y, int color) {
        s = Lang.tr(s);
        if (AtenasFont.ready()) AtenasFont.draw(s, x, y, color, true);
        else mc().fontRendererObj.drawStringWithShadow(s, x, y, color);
    }

    /** Titulo en mayusculas con letras separadas (look futurista). */
    public static void heading(String s, int x, int y, int color) {
        s = Lang.tr(s).toUpperCase();
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            text(ch, cx, y, color);
            cx += width(ch) + 1;
        }
    }

    public static int headingWidth(String s) {
        s = Lang.tr(s).toUpperCase();
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += width(String.valueOf(s.charAt(i))) + 1;
        return w;
    }

    public static int width(String s) {
        s = Lang.tr(s);
        return AtenasFont.ready() ? AtenasFont.width(s) : mc().fontRendererObj.getStringWidth(s);
    }

    public static void centered(String s, int cx, int y, int color) {
        text(s, cx - width(s) / 2, y, color);
    }

    public static void scaled(String s, float x, float y, float scale, int color, boolean centered) {
        s = Lang.tr(s);
        float w = width(s) * scale;
        GlStateManager.pushMatrix();
        GlStateManager.translate(centered ? x - w / 2f : x, y, 0f);
        GlStateManager.scale(scale, scale, 1f);
        if (AtenasFont.ready()) AtenasFont.draw(s, 0, 0, color, true);
        else mc().fontRendererObj.drawStringWithShadow(s, 0, 0, color);
        GlStateManager.popMatrix();
    }

    /** Rectangulo con las esquinas cortadas (look "tech"). */
    public static void card(int x, int y, int w, int h, int fill, int border) {
        int cut = h >= 18 && w >= 18 ? 3 : 2;
        chamfer(x, y, w, h, cut, fill);
        chamferBorder(x, y, w, h, cut, border);
    }

    public static void toggle(int x, int y, boolean on) {
        if (on) {
            gradient(x, y, 22, 10, Theme.VIOLET, Theme.MAGENTA, false);
            border(x, y, 22, 10, Theme.ACCENT);
        } else {
            card(x, y, 22, 10, Theme.OFF, 0xFF4A4670);
        }
        rect(on ? x + 13 : x + 1, y + 1, 8, 8, on ? Theme.TEXT : 0xFFB8B4D4);
    }

    /** Icono de ajustes: tres barras con perillas. */
    public static void sliders(int x, int y, int c) {
        rect(x, y + 1, 11, 1, c);  rect(x + 6, y, 2, 3, c);
        rect(x, y + 5, 11, 1, c);  rect(x + 2, y + 4, 2, 3, c);
        rect(x, y + 9, 11, 1, c);  rect(x + 7, y + 8, 2, 3, c);
    }

    /** Recorta con "..." si no cabe en w pixeles. */
    public static String fit(String s, int w) {
        net.minecraft.client.gui.FontRenderer f = mc().fontRendererObj;
        s = Lang.tr(s);
        if (width(s) <= w) return s;
        if (AtenasFont.ready()) return AtenasFont.trim(s, Math.max(0, w - width("..."))) + "...";
        return f.trimStringToWidth(s, Math.max(0, w - f.getStringWidth("..."))) + "...";
    }

    /** Parte el texto en lineas de w pixeles; si sobran lineas, la ultima termina en "...". */
    public static java.util.List<String> wrap(String s, int w, int maxLines) {
        java.util.List<String> lines = new java.util.ArrayList<String>();
        if (AtenasFont.ready()) {
            StringBuilder cur = new StringBuilder();
            for (String word : Lang.tr(s).split(" ")) {
                String test = cur.length() == 0 ? word : cur + " " + word;
                if (cur.length() > 0 && width(test) > w) { lines.add(cur.toString()); cur = new StringBuilder(word); }
                else cur = new StringBuilder(test);
            }
            if (cur.length() > 0) lines.add(cur.toString());
        } else {
            for (Object o : mc().fontRendererObj.listFormattedStringToWidth(Lang.tr(s), w)) lines.add((String) o);
        }
        if (lines.size() <= maxLines) return lines;
        java.util.List<String> out = new java.util.ArrayList<String>(lines.subList(0, maxLines));
        out.set(maxLines - 1, fit(out.get(maxLines - 1) + "...", w));
        return out;
    }

    public static void button(String label, int x, int y, int w, int h, boolean hover) {
        card(x, y, w, h, hover ? Theme.PANEL2 : Theme.PANEL, hover ? Theme.ACCENT : Theme.BORDER);
        centered(label, x + w / 2, y + (h - 8) / 2, hover ? Theme.ACCENT : Theme.TEXT);
    }

    public static void scissor(int x, int y, int w, int h) {
        ScaledResolution sr = new ScaledResolution(mc());
        int f = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * f, mc().displayHeight - (y + h) * f, w * f, h * f);
    }

    public static void endScissor() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }

    public static void logo(int x, int y, int size) {
        mc().getTextureManager().bindTexture(Theme.LOGO);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, size, size, (float) size, (float) size);
    }
}
