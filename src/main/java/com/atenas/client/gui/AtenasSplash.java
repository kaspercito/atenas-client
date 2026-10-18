package com.atenas.client.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * Pantalla de carga de Atenas DENTRO de la ventana del juego: reemplaza el logo de Mojang (blanco) que sale al abrir Minecraft.
 * Ya no hace falta que el launcher abra otra ventana encima con una barra de carga.
 *
 * Solo usa OpenGL "viejo" y Java2D (nada de texturas ni fuentes de Minecraft, que todavia no estan cargadas en este momento).
 * {@link MixinMinecraftSplash} llama a {@link #begin()} en lugar de drawSplashScreen, y los eventos de carga (init, postInit, recursos)
 * llaman a {@link #step(float, String)}. Si algo falla, se apaga sola y el juego sigue con la pantalla normal.
 */
public final class AtenasSplash {
    private static final int BG = 0x07060F;

    private static boolean started, broken;
    private static float target, shown;
    private static String status = "";
    private static int logoTex;
    private static long t0;
    private static Font baseFont;
    private static final Map<String, int[]> TEXTS = new HashMap<String, int[]>(); // clave -> {textura, ancho, alto}
    private static float[] starX, starY, starS, starP;

    private AtenasSplash() {}

    /** Dibuja el primer cuadro. true = ya se encargo Atenas (el metodo vanilla se cancela). */
    public static boolean begin() {
        if (broken) return false;
        try {
            if (!Display.isCreated() || Display.getWidth() <= 0) return false;
            t0 = System.currentTimeMillis();
            Random r = new Random(7);
            starX = new float[70]; starY = new float[70]; starS = new float[70]; starP = new float[70];
            for (int i = 0; i < 70; i++) { starX[i] = r.nextFloat(); starY[i] = r.nextFloat() * 0.8f; starS[i] = 1f + r.nextInt(3); starP[i] = r.nextFloat() * 6.28f; }
            started = true;
            status = "Cargando mods";
            target = 0.30f;
            shown = 0.05f;
            animate();
            return true;
        } catch (Throwable t) {
            broken = true;
            started = false;
            System.out.println("[Atenas] splash desactivado: " + t);
            return false;
        }
    }

    /** Sube la barra hasta "to" (0 a 1) con el texto indicado. No hace nada si la pantalla de carga no esta activa. */
    public static void step(float to, String text) {
        if (!started || broken) return;
        try {
            if (to > target) target = to;
            status = text == null ? "" : text;
            animate();
        } catch (Throwable t) {
            broken = true;
            started = false;
            System.out.println("[Atenas] splash desactivado: " + t);
        }
    }

    private static void animate() {
        int guard = 0;
        while (shown < target - 0.001f && guard++ < 40) {
            shown = Math.min(target, shown + 0.025f);
            frame();
            try { Thread.sleep(8); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        frame();
    }

    // ------------------------------------------------------------------ dibujo

    private static void frame() {
        final int w = Display.getWidth(), h = Display.getHeight();
        if (w <= 0 || h <= 0) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, w, h, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        try {
            GL11.glViewport(0, 0, w, h);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glClearColor(((BG >> 16) & 255) / 255f, ((BG >> 8) & 255) / 255f, (BG & 255) / 255f, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            draw(w, h);
        } finally {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        Display.update();
    }

    private static void draw(int w, int h) {
        final float u = Math.max(0.6f, Math.min(w / 960f, h / 540f)); // unidad de escala
        final float t = (System.currentTimeMillis() - t0) / 1000f;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        // resplandor violeta del horizonte (abajo)
        gradV(0, h * 0.62f, w, h * 0.38f, 0x008B5CF6, 0x558B5CF6);

        // estrellas
        for (int i = 0; i < starX.length; i++) {
            float a = 0.25f + 0.55f * (0.5f + 0.5f * (float) Math.sin(t * 1.4f + starP[i]));
            rect(starX[i] * w, starY[i] * h, starS[i] * u, starS[i] * u, 0xFFFFFF, a * 0.8f);
        }

        // marca: logo con pulso
        final float cx = w / 2f;
        final float logo = 120f * u, ly = h * 0.5f - 170f * u;
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3f);
        for (int k = 3; k >= 1; k--) {
            float g = logo + k * 18f * u;
            rect(cx - g / 2f, ly + logo / 2f - g / 2f, g, g, 0x8B5CF6, (0.03f + 0.04f * pulse) / k);
        }
        if (logoTex == 0) loadLogo();
        if (logoTex != 0) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, logoTex);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            quadTex(cx - logo / 2f, ly, logo, logo);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }

        // titulo y subtitulo
        float y = ly + logo + 16f * u;
        y += text("ATENAS", 46f * u, 0xA78BFA, cx, y, true) + 2f * u;
        y += text("C  L  I  E  N  T", 15f * u, 0x22D3EE, cx, y, true) + 10f * u;
        rect(cx - 150f * u, y, 300f * u, Math.max(1f, u), 0x6D4FC7, 1f);
        rect(cx - 22f * u, y - Math.max(1f, u), 44f * u, 3f * Math.max(1f, u), 0x8B5CF6, 1f);

        // barra de carga
        final float bw = 360f * u, bh = Math.max(5f, 7f * u), bx = cx - bw / 2f, by = h * 0.5f + 118f * u;
        rect(bx - 2, by - 2, bw + 4, bh + 4, 0x2B2848, 1f);
        rect(bx, by, bw, bh, 0x0B0E1C, 1f);
        float fill = bw * Math.max(0f, Math.min(1f, shown));
        if (fill > 0.5f) {
            gradH(bx, by, fill, bh, 0xFF22D3EE, 0xFF8B5CF6, 0xFFEC4899, bw);
            rect(bx + fill - 2f, by - 2f, 2f, bh + 4f, 0xFFFFFF, 0.9f); // punta brillante
        }

        // estado y porcentaje
        text(status, 14f * u, 0x9B97B8, cx - 30f * u, by + bh + 14f * u, false);
        text(Math.round(Math.max(0f, Math.min(1f, shown)) * 100f) + "%", 14f * u, 0xF1EEFC, cx + bw / 2f - 18f * u, by + bh + 14f * u, false);
    }

    // ------------------------------------------------------------------ piezas

    private static void rect(float x, float y, float w, float h, int rgb, float a) {
        GL11.glColor4f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y); GL11.glVertex2f(x + w, y); GL11.glVertex2f(x + w, y + h); GL11.glVertex2f(x, y + h);
        GL11.glEnd();
    }

    /** Degradado vertical entre dos colores ARGB. */
    private static void gradV(float x, float y, float w, float h, int top, int bottom) {
        GL11.glBegin(GL11.GL_QUADS);
        argb(top); GL11.glVertex2f(x, y); GL11.glVertex2f(x + w, y);
        argb(bottom); GL11.glVertex2f(x + w, y + h); GL11.glVertex2f(x, y + h);
        GL11.glEnd();
    }

    /** Degradado horizontal de 3 colores; "total" es el ancho de la barra completa (para que el color dependa de la posicion). */
    private static void gradH(float x, float y, float w, float h, int c0, int c1, int c2, float total) {
        float k = Math.min(1f, w / total);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= 12; i++) {
            float f = i / 12f;          // 0..1 sobre el trozo dibujado
            float g = f * k;            // 0..1 sobre la barra completa
            int c = g < 0.5f ? mix(c0, c1, g / 0.5f) : mix(c1, c2, (g - 0.5f) / 0.5f);
            argb(c);
            GL11.glVertex2f(x + w * f, y);
            GL11.glVertex2f(x + w * f, y + h);
        }
        GL11.glEnd();
    }

    private static int mix(int a, int b, float f) {
        int r = 0;
        for (int s = 24; s >= 0; s -= 8) {
            int ca = (a >> s) & 255, cb = (b >> s) & 255;
            r |= ((int) (ca + (cb - ca) * f) & 255) << s;
        }
        return r;
    }

    private static void argb(int c) {
        GL11.glColor4f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, ((c >> 24) & 255) / 255f);
    }

    private static void quadTex(float x, float y, float w, float h) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + w, y);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + h);
        GL11.glEnd();
    }

    // ------------------------------------------------------------------ texturas

    private static void loadLogo() {
        logoTex = -1; // si falla, no se intenta otra vez
        try {
            InputStream in = AtenasSplash.class.getResourceAsStream("/assets/atenasclient/textures/gui/logo.png");
            if (in == null) return;
            BufferedImage img;
            try { img = ImageIO.read(in); } finally { in.close(); }
            if (img == null) return;
            logoTex = upload(img);
        } catch (Throwable ignored) { logoTex = -1; }
        if (logoTex < 0) logoTex = -1;
    }

    private static int upload(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        ByteBuffer bb = ByteBuffer.allocateDirect(w * h * 4);
        for (int p : px) bb.put((byte) (p >> 16 & 255)).put((byte) (p >> 8 & 255)).put((byte) (p & 255)).put((byte) (p >> 24 & 255));
        bb.flip();
        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bb);
        return id;
    }

    /**
     * Dibuja texto centrado en cx con la letra del cliente (ui.ttf), pintado con Java2D en una textura que se guarda en cache.
     * Devuelve el alto usado. "px" es el tamano de letra en pixeles de pantalla.
     */
    private static float text(String s, float px, int rgb, float cx, float y, boolean bold) {
        if (s == null || s.isEmpty()) return px;
        int size = Math.max(8, Math.round(px));
        String key = s + "|" + size + "|" + rgb + "|" + bold;
        int[] t = TEXTS.get(key);
        if (t == null) {
            t = makeText(s, size, rgb, bold);
            if (TEXTS.size() > 40) TEXTS.clear(); // los estados cambian pocas veces; los porcentajes son los que mas se acumulan
            TEXTS.put(key, t);
        }
        if (t[0] == 0) return px;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, t[0]);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        quadTex(cx - t[1] / 2f, y, t[1], t[2]);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        return t[2];
    }

    private static int[] makeText(String s, int size, int rgb, boolean bold) {
        try {
            if (baseFont == null) {
                InputStream in = AtenasSplash.class.getResourceAsStream("/assets/atenasclient/fonts/ui.ttf");
                try { baseFont = in != null ? Font.createFont(Font.TRUETYPE_FONT, in) : new Font("SansSerif", Font.PLAIN, 12); }
                finally { if (in != null) in.close(); }
            }
            Font f = baseFont.deriveFont(bold ? Font.BOLD : Font.PLAIN, (float) size);
            BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = probe.createGraphics();
            pg.setFont(f);
            FontMetrics fm = pg.getFontMetrics();
            int tw = fm.stringWidth(s) + 8, th = fm.getHeight() + 6;
            int asc = fm.getAscent();
            pg.dispose();
            BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(f);
            g.setColor(new Color(rgb));
            g.drawString(s, 4, 3 + asc);
            g.dispose();
            return new int[] { upload(img), tw, th };
        } catch (Throwable e) {
            return new int[] { 0, 0, 0 };
        }
    }
}
