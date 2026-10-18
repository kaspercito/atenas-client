package com.atenas.client.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

/**
 * Letra propia del cliente (suave, con anti-alias) para TODAS las pantallas de Atenas.
 * Lee assets/atenasclient/fonts/ui.ttf: para cambiar la tipografia solo hay que reemplazar ese archivo por otro .ttf
 * (por ejemplo Orbitron o Rajdhani). Se pinta a 4x en un atlas y se dibuja reducido, asi se ve nitida en cualquier escala de GUI.
 * Si algo falla al cargar, {@link #ready()} devuelve false y Draw usa la letra normal de Minecraft.
 */
public final class AtenasFont {
    private static final int SS = 4;              // supersampling
    private static final float PX = 9.6f;         // alto de letra en unidades de GUI
    private static final int FIRST = 32, LAST = 255, COLS = 16, CW = 56, CH = 64, ATLAS = 1024;
    /** Glifos extra dibujados a mano (la fuente .ttf no los trae y salian como \"?\"): punto lleno y punto vacio. */
    private static final int DOT_FULL = LAST + 1, DOT_RING = LAST + 2, TOTAL = LAST + 3;
    private static final int[] PALETTE = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private static boolean tried, ok;
    private static ResourceLocation tex;
    private static final float[] ADV = new float[TOTAL];
    private static final int[] GW = new int[TOTAL];
    private static float ascent;

    private AtenasFont() {}

    public static boolean ready() {
        if (!tried) init();
        return ok;
    }

    private static void init() {
        tried = true;
        try {
            InputStream in = AtenasFont.class.getResourceAsStream("/assets/atenasclient/fonts/ui.ttf");
            if (in == null) return;
            Font base = Font.createFont(Font.TRUETYPE_FONT, in);
            in.close();
            Font f = base.deriveFont(PX * SS);
            BufferedImage img = new BufferedImage(ATLAS, ATLAS, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont(f);
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            ascent = fm.getAscent();
            for (int c = FIRST; c <= LAST; c++) {
                int i = c - FIRST, cx = (i % COLS) * CW, cy = (i / COLS) * CH;
                ADV[c] = fm.charWidth((char) c);
                GW[c] = (int) Math.ceil(ADV[c]) + 4;
                g.drawString(String.valueOf((char) c), cx + 2, cy + 8 + ascent);
            }
            // puntos de estado (\u25cf \u25cb) dibujados como figuras, del tamano de una letra
            float d = PX * SS * 0.5f;
            for (int c = DOT_FULL; c <= DOT_RING; c++) {
                int i = c - FIRST, cx = (i % COLS) * CW, cy = (i / COLS) * CH;
                ADV[c] = d * 1.45f;
                GW[c] = (int) Math.ceil(ADV[c]) + 4;
                float ox = cx + 2 + (ADV[c] - d) / 2f, oy = cy + 8 + ascent - d * 1.05f;
                if (c == DOT_FULL) g.fill(new java.awt.geom.Ellipse2D.Float(ox, oy, d, d));
                else {
                    g.setStroke(new java.awt.BasicStroke(Math.max(2f, d * 0.16f)));
                    g.draw(new java.awt.geom.Ellipse2D.Float(ox + 1.5f, oy + 1.5f, d - 3f, d - 3f));
                }
            }
            g.dispose();
            DynamicTexture dt = new DynamicTexture(img);
            tex = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("atenas_font", dt);
            GlStateManager.bindTexture(dt.getGlTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            ok = true;
        } catch (Throwable t) {
            System.err.println("[AtenasClient] fuente propia no disponible: " + t);
            ok = false;
        }
    }

    private static int idx(char c) {
        if (c == '\u25cf') return DOT_FULL;
        if (c == '\u25cb') return DOT_RING;
        if (c == '\u2022') return DOT_FULL;
        if (c == '\u2026') return '.';
        return c < FIRST || c > LAST ? '?' : c;
    }

    /** Ancho en unidades de GUI (ignora los codigos de color). */
    public static float widthF(String s) {
        if (s == null) return 0f;
        float w = 0f;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7' && i + 1 < s.length()) { i++; continue; }
            w += ADV[idx(c)] / SS;
        }
        return w;
    }

    public static int width(String s) { return (int) Math.ceil(widthF(s)); }

    /** Dibuja con sombra suave. Soporta los codigos de color de Minecraft. */
    public static void draw(String s, float x, float y, int argb, boolean shadow) {
        drawImpl(s, x, y, argb, shadow, false);
    }

    /** Como {@link #draw}, pero con las letras separadas 1 px (titulos): la misma separacion que Draw.heading calculaba letra por letra. */
    public static void drawSpaced(String s, float x, float y, int argb, boolean shadow) {
        drawImpl(s, x, y, argb, shadow, true);
    }

    /** La sombra y el texto van en el MISMO buffer: una sola llamada de dibujo por cadena (antes eran dos). */
    private static void drawImpl(String s, float x, float y, int argb, boolean shadow, boolean spaced) {
        if (s == null || s.isEmpty()) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        if (shadow) {
            int a = (argb >>> 24) == 0 ? 255 : (argb >>> 24);
            emit(wr, s, x + 0.75f, y + 0.75f, (a * 0x70 / 255) << 24, true, spaced);
        }
        emit(wr, s, x, y, argb, false, spaced);
        t.draw();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void emit(WorldRenderer wr, String s, float x, float y, int argb, boolean flat, boolean spaced) {
        float a0 = (argb >> 24 & 255) / 255f;
        if (a0 == 0f && !flat) a0 = 1f;
        if (flat && (argb >>> 24) == 0) return;
        float r = (argb >> 16 & 255) / 255f, g = (argb >> 8 & 255) / 255f, b = (argb & 255) / 255f;
        float top = y + 7f - (ascent + 8f) / SS;
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7' && i + 1 < s.length()) {
                char k = Character.toLowerCase(s.charAt(++i));
                int p = "0123456789abcdef".indexOf(k);
                if (p >= 0 && !flat) { r = (PALETTE[p] >> 16 & 255) / 255f; g = (PALETTE[p] >> 8 & 255) / 255f; b = (PALETTE[p] & 255) / 255f; }
                else if (k == 'r' && !flat) { r = (argb >> 16 & 255) / 255f; g = (argb >> 8 & 255) / 255f; b = (argb & 255) / 255f; }
                continue;
            }
            int ci = idx(c), n = ci - FIRST;
            if (c != ' ') {
                float u0 = (n % COLS) * CW / (float) ATLAS, v0 = (n / COLS) * CH / (float) ATLAS;
                float u1 = u0 + CW / (float) ATLAS, v1 = v0 + CH / (float) ATLAS;
                float x0 = cx - 2f / SS, x1 = x0 + CW / (float) SS, y0 = top, y1 = top + CH / (float) SS;
                wr.pos(x0, y1, 0).tex(u0, v1).color(r, g, b, a0).endVertex();
                wr.pos(x1, y1, 0).tex(u1, v1).color(r, g, b, a0).endVertex();
                wr.pos(x1, y0, 0).tex(u1, v0).color(r, g, b, a0).endVertex();
                wr.pos(x0, y0, 0).tex(u0, v0).color(r, g, b, a0).endVertex();
            }
            // con separacion: cada letra ocupa su ancho redondeado + 1 px (igual que width(letra) + 1 de Draw.heading)
            cx += spaced ? (float) Math.ceil(ADV[ci] / SS) + 1f : ADV[ci] / SS;
        }
    }

    /** Recorta por ancho (sin codigos de color a medias). */
    public static String trim(String s, int w) {
        float acc = 0f;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7' && i + 1 < s.length()) { i++; continue; }
            acc += ADV[idx(c)] / SS;
            if (acc > w) return s.substring(0, i);
        }
        return s;
    }
}
