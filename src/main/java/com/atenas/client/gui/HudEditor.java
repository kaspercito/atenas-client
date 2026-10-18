package com.atenas.client.gui;

import java.io.IOException;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.atenas.client.core.Config;
import com.atenas.client.core.HudModule;
import com.atenas.client.core.ModuleManager;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

/** Editor de HUD: arrastra los modulos y usa la rueda para cambiar su tamano. */
public class HudEditor extends GuiScreen {

    private final GuiScreen parent;
    private HudModule dragged;
    private int offX, offY;

    public HudEditor(GuiScreen parent) { this.parent = parent; }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private HudModule hovered(int mx, int my) {
        List<HudModule> list = ModuleManager.hud();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule h = list.get(i);
            if (h.isEnabled() && Draw.inside(mx, my, h.x, h.y, h.scaledWidth(), h.scaledHeight())) return h;
        }
        return null;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        Draw.batchBegin(); // fondo, rejilla y ejes: formas lisas, una sola llamada de dibujo
        Draw.rect(0, 0, width, height, 0x80050813);
        // rejilla tenue cada 20 px
        for (int gx = 0; gx < width; gx += 20) Draw.rect(gx, 0, 1, height, 0x0F22D3EE);
        for (int gy = 0; gy < height; gy += 20) Draw.rect(0, gy, width, 1, 0x0F22D3EE);
        // ejes centrales (se iluminan al arrastrar cerca del centro)
        int axis = dragged != null ? 0x6022D3EE : 0x2822D3EE;
        Draw.rect(width / 2, 0, 1, height, axis);
        Draw.rect(0, height / 2, width, 1, axis);
        Draw.batchEnd();
        ScaledResolution sr = new ScaledResolution(mc);
        HudModule over = hovered(mx, my);
        boolean any = false;
        for (HudModule h : ModuleManager.hud()) {
            if (!h.isEnabled()) continue;
            any = true;
            h.ensurePositioned(sr);
            h.render();
            boolean active = h == dragged || (dragged == null && h == over);
            int x = h.x - 2, y = h.y - 2, w = h.scaledWidth() + 4, hh = h.scaledHeight() + 4;
            int c = active ? Theme.ACCENT : 0x70FFFFFF;
            int L = 5;
            // esquinas tipo visor
            Draw.rect(x, y, L, 1, c); Draw.rect(x, y, 1, L, c);
            Draw.rect(x + w - L, y, L, 1, c); Draw.rect(x + w - 1, y, 1, L, c);
            Draw.rect(x, y + hh - 1, L, 1, c); Draw.rect(x, y + hh - L, 1, L, c);
            Draw.rect(x + w - L, y + hh - 1, L, 1, c); Draw.rect(x + w - 1, y + hh - L, 1, L, c);
            if (active) {
                String tag = h.name + "  " + h.x + "," + h.y + "  x" + String.format(java.util.Locale.US, "%.1f", h.getScale());
                int tw = Draw.width(tag) + 8;
                int ty = h.y > 14 ? h.y - 14 : h.y + h.scaledHeight() + 4;
                Draw.rect(h.x, ty, tw, 11, 0xE0090812);
                Draw.rect(h.x, ty, 2, 11, Theme.ACCENT);
                Draw.text(tag, h.x + 5, ty + 2, Theme.TEXT);
            }
        }
        // cabecera
        int hw = Math.min(width - 20, 330);
        int hx = width / 2 - hw / 2;
        Draw.rect(hx, 6, hw, 28, 0xE0090812);
        Draw.border(hx, 6, hw, 28, Theme.BORDER);
        Draw.gradient(hx, 6, hw, 2, Theme.ACCENT, 0x0022D3EE, false);
        Draw.logo(hx + 6, 10, 20);
        Draw.text("EDITOR HUD", hx + 32, 12, Theme.ACCENT);
        Draw.text("Arrastra \u00b7 Rueda: tama\u00f1o \u00b7 ESC: guardar", hx + 32, 22, Theme.DIM);
        if (!any) {
            Draw.centered("No hay m\u00f3dulos HUD activados. Act\u00edvalos en el men\u00fa.", width / 2, height / 2 + 6, Theme.DIM);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        HudModule h = hovered(mx, my);
        if (h != null) {
            dragged = h;
            offX = mx - h.x;
            offY = my - h.y;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        if (dragged == null) return;
        int nx = mx - offX, ny = my - offY;
        int w = dragged.scaledWidth(), h = dragged.scaledHeight();
        nx = Math.max(0, Math.min(width - w, nx));
        ny = Math.max(0, Math.min(height - h, ny));
        if (Math.abs(nx + w / 2 - width / 2) < 4) nx = width / 2 - w / 2;
        if (Math.abs(ny + h / 2 - height / 2) < 4) ny = height / 2 - h / 2;
        dragged.x = nx;
        dragged.y = ny;
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        if (dragged != null) {
            dragged = null;
            Config.save();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        HudModule h = hovered(mx, my);
        if (h != null) {
            h.setScale(h.getScale() + (d > 0 ? 0.1f : -0.1f));
        }
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (code == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() { Config.save(); }
}
