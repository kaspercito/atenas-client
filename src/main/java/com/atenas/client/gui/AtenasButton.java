package com.atenas.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Boton futurista: panel oscuro, barra de acento que crece al pasar el mouse y esquinas marcadas. */
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
        glow += ((on ? 1f : 0f) - glow) * 0.25f;
        int x = xPosition, y = yPosition, w = width, h = height;
        Draw.rect(x, y, w, h, enabled ? 0xE00F0D1D : 0xA0090812);
        Draw.gradient(x, y, w, h, 0x00A78BFA, ((int) (0x38 * glow) << 24) | 0xA78BFA, false);
        Draw.border(x, y, w, h, on ? Theme.ACCENT : Theme.BORDER);
        int bar = 2 + (int) (glow * (w / 2 - 2));
        Draw.rect(x, y + h - 1, bar, 1, Theme.ACCENT);
        Draw.rect(x + w - bar, y, bar, 1, Theme.ACCENT);
        Draw.rect(x, y, 2, h, on ? Theme.ACCENT : Theme.ACCENT_DIM);
        int tc = !enabled ? Theme.OFF : on ? Theme.ACCENT : Theme.TEXT;
        Draw.text(displayString, x + w / 2 - Draw.width(displayString) / 2 + (int) (glow * 2), y + (h - 8) / 2, tc);
    }
}
