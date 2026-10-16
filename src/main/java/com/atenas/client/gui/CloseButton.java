package com.atenas.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Cuadrado con una X dibujada (no texto): se pone rojo al pasar el mouse. Cierra el cliente. */
public class CloseButton extends GuiButton {

    public CloseButton(int id, int x, int y, int size) {
        super(id, x, y, size, size, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
        Draw.rect(xPosition, yPosition, width, height, hovered ? 0xFFE5384F : 0xB00D1020);
        Draw.border(xPosition, yPosition, width, height, hovered ? 0xFFFF8A9A : 0xFF3A3F66);
        int pad = 6, n = width - pad * 2;
        int c = hovered ? 0xFFFFFFFF : Theme.DIM;
        for (int i = 0; i < n; i++) {
            Draw.rect(xPosition + pad + i, yPosition + pad + i, 1, 1, c);
            Draw.rect(xPosition + pad + n - 1 - i, yPosition + pad + i, 1, 1, c);
        }
    }
}
