package com.atenas.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Boton del menu principal: titulo a la izquierda, pista corta a la derecha, esquinas cortadas y neon que recorre el borde al pasar el mouse. */
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
        glow += ((on ? 1f : 0f) - glow) * 0.22f;
        int x = xPosition, y = yPosition, w = width, h = height, cut = 4;

        Draw.chamfer(x, y, w, h, cut, secondary ? 0xCC0A0C1B : 0xE60C0F20);
        Draw.gradient(x + cut, y + 1, w - 2 * cut, h - 2, 0x008B5CF6, ((int) (0x40 * glow) << 24) | 0x8B5CF6, false);
        Draw.chamferBorder(x, y, w, h, cut, on ? Theme.VIOLET : 0xFF2A2E52);
        // barra de acento a la izquierda: se alarga con el hover
        int bh = 6 + (int) (glow * (h - 12));
        Draw.rect(x + 3, y + (h - bh) / 2, 2, bh, on ? Theme.CYAN : (secondary ? 0xFF4A4F78 : Theme.ACCENT_DIM));
        int bar = (int) (glow * (w - 2 * cut));
        if (bar > 0) Draw.gradient(x + cut, y + h - 1, bar, 1, Theme.VIOLET, 0x00EC4899, false);

        int ty = y + (h - 8) / 2;
        int tc = !enabled ? Theme.OFF : on ? 0xFFFFFFFF : Theme.TEXT;
        Draw.heading(displayString, x + 14 + (int) (glow * 3), ty, tc);
        if (sub != null) Draw.text(sub, x + w - Draw.width(sub) - 12, ty, on ? Theme.CYAN : Theme.DIM);
    }
}
