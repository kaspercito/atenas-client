package com.atenas.client.social;

import java.util.ArrayList;
import java.util.List;

import com.atenas.client.gui.Draw;
import com.atenas.client.gui.Theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Avisos propios de Atenas (solicitudes, mensajes, amigos en linea). Cada aviso trae la cara del jugador, entra deslizandose desde la
 * derecha con un pequeno rebote, se acomoda suave cuando otro desaparece, muestra una barrita que se vacia y sale deslizandose.
 */
public final class Toasts {
    private static final int LIFE_MS = 5500, IN_MS = 380, OUT_MS = 320, W = 176, H = 34, GAP = 4, TOP = 6;

    private static final class Toast {
        final String title, text, face;
        final int accent;
        final long born = System.currentTimeMillis();
        float y = -1f; // -1 = todavia sin posicion: aparece directamente en su sitio
        Toast(String title, String text, String face, int accent) { this.title = title; this.text = text; this.face = face; this.accent = accent; }
    }

    private static final List<Toast> LIST = new ArrayList<Toast>();
    private static long lastFrame;

    private Toasts() {}

    /** Desde cualquier hilo. "face" es el nick del jugador (o null si el aviso no es de nadie). */
    public static synchronized void push(String title, String text, String face, int accent) {
        if (!SocialClient.toasts) return;
        if (LIST.size() >= 4) LIST.remove(0);
        LIST.add(new Toast(title, text, face, accent));
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    private static int withAlpha(int rgb, float a) {
        int al = Math.max(0, Math.min(255, (int) (a * 255f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    /** Dibuja los avisos (hilo principal). Llamar una vez por cuadro, con el HUD o sobre una pantalla. */
    public static synchronized void draw() {
        long now = System.currentTimeMillis();
        for (int i = LIST.size() - 1; i >= 0; i--) if (now - LIST.get(i).born > LIFE_MS) LIST.remove(i);
        if (LIST.isEmpty()) { lastFrame = now; return; }
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        for (int i = 0; i < LIST.size(); i++) {
            Toast t = LIST.get(i);
            float target = TOP + i * (H + GAP);
            t.y = t.y < 0f ? target : t.y + (target - t.y) * Math.min(1f, dt * 12f);

            long age = now - t.born;
            float in = Math.min(1f, age / (float) IN_MS);
            float out = Math.max(0f, (age - (LIFE_MS - OUT_MS)) / (float) OUT_MS);
            float slide = (1f - easeOutBack(in)) * (W + 12) + out * out * (W + 12);
            float alpha = Math.min(1f, age / 160f) * (1f - out * 0.6f);

            int x = sr.getScaledWidth() - W - 6 + (int) slide, y = (int) t.y;
            Draw.rect(x, y, W, H, withAlpha(0x090812, 0.90f * alpha));
            Draw.rect(x, y, 2, H, withAlpha(t.accent, alpha));
            Draw.border(x, y, W, H, withAlpha(0x2A3558, 0.7f * alpha));

            // cara (o la inicial si todavia no llego)
            int fx = x + 7, fy = y + 5, fs = 24;
            if (t.face != null && !Faces.draw(t.face, fx, fy, fs, alpha, false)) {
                Draw.rect(fx, fy, fs, fs, withAlpha(0x2A2550, alpha));
                Draw.centered(t.face.substring(0, 1).toUpperCase(java.util.Locale.ROOT), fx + fs / 2, fy + 8, withAlpha(0xA78BFA, alpha));
            }
            int tx = t.face != null ? fx + fs + 7 : x + 8, tw = W - (tx - x) - 6;
            Draw.text(mc.fontRendererObj.trimStringToWidth(t.title, tw), tx, y + 6, withAlpha(t.accent, alpha));
            Draw.text(mc.fontRendererObj.trimStringToWidth(t.text, tw), tx, y + 18, withAlpha(0xF1EEFC, alpha));

            // barrita de tiempo que se vacia
            int bw = (int) (W * Math.max(0f, 1f - age / (float) LIFE_MS));
            Draw.rect(x, y + H - 2, bw, 2, withAlpha(t.accent, 0.55f * alpha));
        }
    }
}
