package com.atenas.client.gui;

import java.util.Random;

/**
 * Fondo animado de Atenas: noche con horizonte violeta, rejilla de neon en perspectiva, estrellas que titilan y una linea de
 * escaneo. Lo usan el menu principal y la ventana de Amigos (cuando no hay mundo detras).
 *
 * Rendimiento: todo el fondo (unos 150 cuadrilateros) sale en UNA sola llamada de dibujo (ver {@link Draw#batchBegin}). Antes eran
 * ~370 llamadas a Gui.drawRect por cuadro. Cada linea de la rejilla es un solo cuadrilatero con degradado de transparencia en vez
 * de 12 trocitos, y las estrellas se calculan con unas pocas operaciones. Sin texturas, sin listas, sin memoria extra por cuadro.
 */
public final class Backdrop {
    private static final int STARS = 110, ROWS = 6, COLS = 10;
    private static final float[] SX = new float[STARS], SY = new float[STARS], SV = new float[STARS], SS = new float[STARS];

    static {
        Random r = new Random(1234L);
        for (int i = 0; i < STARS; i++) {
            SX[i] = r.nextFloat();
            SY[i] = r.nextFloat();
            SV[i] = 0.2f + r.nextFloat() * 0.8f;
            SS[i] = r.nextFloat();
        }
    }

    private Backdrop() {}

    public static void render(int width, int height) {
        long t = System.currentTimeMillis();
        int hz = height - 90;
        Draw.batchBegin();
        try {
            // cielo y resplandor del horizonte
            Draw.gradient(0, 0, width, height, 0xFF080A14, 0xFF171C3C, true);
            Draw.gradient(0, hz - 40, width, 40, 0x008B5CF6, 0x268B5CF6, true);
            // suelo + linea del horizonte
            Draw.rect(0, hz, width, height - hz, 0xFF070913);
            Draw.rect(0, hz, width, 1, 0x808B5CF6);
            // lineas horizontales que se acercan
            for (int i = 1; i <= ROWS; i++) {
                float k = (float) Math.pow((i + (t % 2000) / 2000f) / 7f, 2.0);
                int yy = hz + (int) (k * (height - hz));
                Draw.rect(0, yy, width, 1, (((int) (60 * k) + 14) << 24) | 0x8B5CF6);
            }
            // lineas que huyen al horizonte (un solo cuadrilatero cada una, mas opaco abajo)
            int cx = width / 2;
            for (int i = -COLS; i <= COLS; i++) {
                float bx = cx + i * (width / 14), ex = cx + i * (width / 3);
                Draw.batchQuad(bx, hz, bx + 1, hz, ex + 1, height, ex, height, 0x0A8B5CF6, 0x0A8B5CF6, 0x328B5CF6, 0x328B5CF6);
            }
            // estrellas
            for (int i = 0; i < STARS; i++) {
                float yy = (SY[i] * hz + t * SV[i] * 0.003f) % hz;
                int a = (int) (50 + 110 * (0.5 + 0.5 * Math.sin(t * 0.002 + i)));
                int size = SS[i] > 0.8f ? 2 : 1;
                int px = (int) (SX[i] * width), py = (int) yy;
                Draw.rect(px, py, size, size, (a << 24) | 0xFFFFFF);
            }
            // linea de escaneo
            int scan = (int) ((t / 14) % (height + 80)) - 40;
            Draw.gradient(0, scan, width, 14, 0x008B5CF6, 0x128B5CF6, true);
        } finally {
            Draw.batchEnd();
        }
    }
}
