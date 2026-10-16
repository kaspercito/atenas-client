package com.atenas.client.gui;

import net.minecraft.util.ResourceLocation;

/**
 * Paleta unica de Atenas Client, sacada del LOGO: violeta/indigo -> magenta -> rosa, con cian de apoyo, sobre azul noche.
 * Todas las pantallas (menu principal, menu del cliente, amigos, cuentas, editor HUD, avisos) usan SOLO estas constantes.
 *
 *   Fondo ........ azul noche   #080A14 / #090812  (paneles #14122A / #1E1B38, bordes #2B2848)
 *   Acento ....... violeta      #8B5CF6  (claro #A78BFA al pasar el mouse, oscuro #6D4FC7 en reposo)
 *   Degradado .... violeta #8B5CF6 -> magenta #C026D3 -> rosa #EC4899
 *   Apoyo ........ cian         #22D3EE  (nodos del logo)
 *   Texto ........ casi blanco  #F1EEFC  (secundario #9B97B8, apagado #3A3658)
 *   Estados ...... verde #34D399 / ambar #FBBF24 / rojo #FB7185
 */
public final class Theme {
    // --- fondos
    public static final int BG = 0xF2090812;
    public static final int SIDE = 0xFF0C0A1A;
    public static final int PANEL = 0xFF14122A;
    public static final int PANEL2 = 0xFF1E1B38;
    public static final int HOVER = 0xFF191631;
    public static final int BORDER = 0xFF2B2848;
    // --- acento (violeta del logo)
    public static final int ACCENT = 0xFFA78BFA;      // violeta claro: hover, titulos
    public static final int ACCENT_DIM = 0xFF6D4FC7;  // violeta oscuro: reposo
    public static final int VIOLET = 0xFF8B5CF6;
    public static final int MAGENTA = 0xFFC026D3;
    public static final int PINK = 0xFFEC4899;
    public static final int CYAN = 0xFF22D3EE;
    // --- estados
    public static final int GREEN = 0xFF34D399;
    public static final int AMBER = 0xFFFBBF24;
    public static final int RED = 0xFFFB7185;
    // --- texto
    public static final int TEXT = 0xFFF1EEFC;
    public static final int DIM = 0xFF9B97B8;
    public static final int OFF = 0xFF3A3658;
    /** Texto oscuro para ir encima de un fondo de acento (insignias, iniciales). */
    public static final int INK = 0xFF090812;
    public static final ResourceLocation LOGO = new ResourceLocation("atenasclient", "textures/gui/logo.png");

    private Theme() {}
}
