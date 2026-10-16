package com.atenas.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Boton del menu principal: titulo en espanol a la izquierda, nombre en ingles a la derecha, barra dorada que crece al pasar el mouse. */
public class MenuButton extends GuiButton {

    private final String sub;
    private final boolean secondary;
    private float glow; // 0..1 animado

    public MenuButton(int id, int x, int y, int w, int h, String title, String sub, boolean secondary) {
        super(id, x, y, w, h, title);
        this.sub = sub;
        this.secondary = secondary;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
        boolean on = hovered && enabled;
        glow += ((on ? 1f : 0f) - glow) * 0.25f;
        int x = xPosition, y = yPosition, w = width, h = height;

        Draw.rect(x, y, w, h, secondary ? 0xC00B0E1C : 0xE00D1020);
        Draw.gradient(x, y, w, h, 0x008B5CF6, ((int) (0x34 * glow) << 24) | 0x8B5CF6, false);
        Draw.border(x, y, w, h, on ? Theme.VIOLET : 0xFF262A48);
        Draw.rect(x, y, 3, h, on ? Theme.ACCENT : (secondary ? 0xFF4A4F78 : Theme.ACCENT_DIM));
        int bar = 3 + (int) (glow * (w - 3));
        Draw.rect(x, y + h - 1, bar, 1, Theme.VIOLET);

        int ty = y + (h - 8) / 2;
        int tc = !enabled ? Theme.OFF : on ? Theme.ACCENT : Theme.TEXT;
        Draw.text(displayString, x + 14 + (int) (glow * 2), ty, tc);
        if (sub != null) Draw.text(sub, x + w - Draw.width(sub) - 12, ty, on ? Theme.VIOLET : Theme.DIM);
    }
}
