package com.atenas.client.core;

import com.atenas.client.gui.HudEditor;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public abstract class HudModule extends Module {

    public int x, y;
    /** false = todavia no se ha colocado (se ancla a la derecha de la pantalla). */
    public boolean positioned = true;

    protected final Setting scale;
    protected final Setting background;
    protected final Setting shadow;
    protected final Setting color;
    protected final Setting accentBar;

    protected HudModule(String name, String description, int x, int y) {
        super(name, description, Category.HUD);
        this.x = x;
        this.y = y;
        scale = addNum("Escala", 1.0, 0.5, 2.5, 0.1);
        background = addBool("Fondo", true);
        shadow = addBool("Sombra de texto", true);
        accentBar = addBool("Barra de acento", true);
        color = addColor("Color", 0);
    }

    protected void anchorRight() { positioned = false; }

    public abstract int getWidth();
    public abstract int getHeight();

    /** Se dibuja en (0,0); la posicion y la escala ya estan aplicadas. */
    protected abstract void draw();

    public float getScale() { return scale.getFloat(); }

    public void setScale(float s) {
        scale.num = Math.max(scale.min, Math.min(scale.max, Math.round(s * 10f) / 10.0));
    }

    public int scaledWidth() { return Math.max(1, (int) (getWidth() * getScale())); }

    public int scaledHeight() { return Math.max(1, (int) (getHeight() * getScale())); }

    public void ensurePositioned(ScaledResolution sr) {
        if (!positioned) {
            x = sr.getScaledWidth() - scaledWidth() - 6;
            positioned = true;
        }
    }

    public void render() {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 0f);
        float s = getScale();
        GlStateManager.scale(s, s, 1f);
        draw();
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    protected boolean isEditing() { return mc.currentScreen instanceof HudEditor; }

    protected void text(String t, int px, int py) {
        mc.fontRendererObj.drawString(t, px, py, color.getColor(), shadow.bool);
    }

    protected void box(int w, int h) {
        if (!background.bool) return;
        Gui.drawRect(0, 0, w, h, 0xB0090812);
        Gui.drawRect(0, 0, w, 1, 0x30FFFFFF);
        if (accentBar.bool) {
            com.atenas.client.gui.Draw.gradient(0, 0, 2, h, 0xFFA78BFA, 0xFFEC4899, true);
            Gui.drawRect(2, h - 1, Math.min(w - 2, 14), h, 0xFF22D3EE);
        }
    }
}
