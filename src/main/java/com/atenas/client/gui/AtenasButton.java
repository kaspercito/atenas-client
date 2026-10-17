package com.atenas.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Boton futurista: cristal oscuro con esquinas cortadas, borde de neon violeta->cian y brillo que sube al pasar el mouse. */
public class AtenasButton extends GuiButton {

    private float glow; // 0..1 animado

    public AtenasButton(int id, int x, int y, int w, int h, String text) {
        super(id, x, y, w, h, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
        glow = paint(xPosition, yPosition, width, height, displayString, enabled, hovered, glow);
    }

    /**
     * Dibuja un boton con el estilo Atenas y devuelve el nuevo brillo (0..1, animado). Lo usa este boton y tambien MixinGuiButton,
     * que lo pone a los botones vanilla sin cambiar de clase (asi las pantallas que guardan sus botones siguen funcionando).
     */
    public static float paint(int x, int y, int w, int h, String text, boolean enabled, boolean hovered, float glow) {
        boolean on = hovered && enabled;
        glow += ((on ? 1f : 0f) - glow) * 0.22f;
        int cut = h >= 18 ? 4 : 3;
        Draw.chamfer(x, y, w, h, cut, enabled ? 0xE60D0B1E : 0xA0090812);
        // brillo interior que sube desde abajo
        Draw.gradient(x + cut, y + h / 2, w - 2 * cut, h / 2 - 1, 0x00A78BFA, ((int) (0x46 * glow) << 24) | 0x8B5CF6, true);
        int edge = on ? Theme.ACCENT : (enabled ? 0xFF3A3466 : 0xFF241F3C);
        Draw.chamferBorder(x, y, w, h, cut, edge);
        // regla de neon abajo: crece desde el centro hacia los lados
        int bar = (int) (glow * (w - 2 * cut));
        if (bar > 0) {
            Draw.gradient(x + w / 2 - bar / 2, y + h - 2, bar / 2 + 1, 2, 0x008B5CF6, Theme.VIOLET, false);
            Draw.gradient(x + w / 2, y + h - 2, bar / 2, 2, Theme.CYAN, 0x0022D3EE, false);
        }
        // esquina brillante
        Draw.rect(x + w - cut - 5, y, 5, 1, on ? Theme.CYAN : Theme.ACCENT_DIM);
        Draw.rect(x + cut, y + h - 1, 5, 1, on ? Theme.PINK : Theme.ACCENT_DIM);
        int tc = !enabled ? Theme.OFF : on ? 0xFFFFFFFF : Theme.TEXT;
        Draw.headingFit(text, x + cut, w - 2 * cut, y + (h - 8) / 2, tc, (int) (glow * 1.5f));
        return glow;
    }

    /** Barra de progreso y tirador de un deslizador (v = 0..1). Se dibuja encima del boton ya pintado. */
    public static void sliderHandle(int x, int y, int w, int h, float v, boolean enabled) {
        int hx = x + (int) (Math.max(0f, Math.min(1f, v)) * (w - 6));
        Draw.rect(x + 3, y + h - 3, Math.max(0, hx - x - 3), 2, enabled ? 0xAA8B5CF6 : 0x663A3658);
        Draw.rect(hx, y + 2, 6, h - 4, enabled ? 0xD08B5CF6 : 0xB03A3658);
        Draw.rect(hx, y + 2, 6, 1, enabled ? Theme.CYAN : Theme.OFF);
        Draw.border(hx, y + 2, 6, h - 4, enabled ? Theme.ACCENT : 0xFF241F3C);
    }

    /** Candado dibujado con rectangulos (el de la dificultad en Opciones). locked = cerrado. */
    public static void padlock(int x, int y, int w, int h, boolean locked, int c) {
        int cx = x + w / 2, top = y + h / 2 - 6;
        Draw.rect(cx - 4, top + 6, 9, 7, c);
        Draw.rect(cx, top + 8, 1, 3, 0xFF090812);
        Draw.rect(cx - 2, top, 5, 1, c);
        Draw.rect(cx - 3, top + 1, 1, locked ? 5 : 3, c);
        Draw.rect(cx + 3, top + 1, 1, locked ? 5 : 2, c);
    }
}
