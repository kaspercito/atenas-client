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
        boolean on = hovered && enabled;
        glow += ((on ? 1f : 0f) - glow) * 0.22f;
        int x = xPosition, y = yPosition, w = width, h = height, cut = h >= 18 ? 4 : 3;
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
        int tw = Draw.headingWidth(displayString);
        Draw.heading(displayString, x + (w - tw) / 2 + (int) (glow * 1.5f), y + (h - 8) / 2, tc);
    }
}
