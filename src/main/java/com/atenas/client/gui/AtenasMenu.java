package com.atenas.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.atenas.client.AtenasClient;
import com.atenas.client.core.Config;
import com.atenas.client.core.HudModule;
import com.atenas.client.core.Lang;
import com.atenas.client.core.Module;
import com.atenas.client.core.ModuleManager;
import com.atenas.client.core.Setting;
import com.atenas.client.cosmetics.CosmeticSync;
import com.atenas.client.cosmetics.Cosmetics;
import com.atenas.client.modules.AutotextModule;
import com.atenas.client.modules.CrosshairModule;
import com.atenas.client.modules.OldAnimationsModule;
import com.atenas.client.modules.PerformanceModule;
import com.atenas.client.social.SocialClient;
import com.atenas.client.social.SocialEvents;
import com.atenas.client.social.SocialScreen;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;

/** Menu principal del cliente (RShift): mods, cosmeticos y ajustes. */
public class AtenasMenu extends GuiScreen {

    private enum Tab { MODS, COSMETICS, ABOUT }

    private static final class Hit {
        final int x, y, w, h;
        final Runnable click;
        IntConsumer drag;
        Runnable alt;   // clic derecho

        Hit(int x, int y, int w, int h, Runnable click) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.click = click;
        }

        boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private static final class CRow {
        final Cosmetics.Type type;
        final Cosmetics.Item item;
        final boolean header;

        CRow(Cosmetics.Type type, Cosmetics.Item item, boolean header) {
            this.type = type;
            this.item = item;
            this.header = header;
        }
    }

    private static final int SIDE = 104;

    private final GuiScreen parent;
    private Tab tab = Tab.MODS;
    private Module selected;
    private int scrollMods, scrollCos, scrollSet, settingsH;
    private float scrollModsSmooth;
    private final List<Hit> hits = new ArrayList<Hit>();
    private Hit dragging;
    private int x0, y0, pw, ph;
    private GuiTextField search;
    private Module.Category filter;      // null = todos
    private boolean settingsOpen;
    private Cosmetics.Type cosFilter = Cosmetics.Type.CAPE;  // pestana activa
    private boolean spinPaused;          // vista previa: giro en pausa
    private float spinAngle;             // angulo actual de la vista previa
    private long spinLast;
    private AbstractClientPlayer mannequin;   // jugador de mentira para la vista previa sin mundo
    private int mannequinFails;
    private final List<GuiTextField> atFields = new ArrayList<GuiTextField>();
    private int atCapturing = -1;
    private boolean atDirty = true;
    private int setTop, setBottom;

    private static int catColor(Module.Category c) {
        switch (c) {
            case HUD: return Theme.CYAN;
            case PVP: return Theme.RED;
            case ANIM: return Theme.ACCENT;
            case PERF: return Theme.GREEN;
            case CHAT: return Theme.VIOLET;
            default: return Theme.AMBER;
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        String keep = search == null ? "" : search.getText();
        search = new GuiTextField(9, fontRendererObj, 0, 0, 100, 10);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(24);
        search.setText(keep);
    }

    public AtenasMenu(GuiScreen parent) { this.parent = parent; }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        hits.clear();
        pw = Math.min(600, width - 24);
        ph = Math.min(330, height - 24);
        x0 = (width - pw) / 2;
        y0 = (height - ph) / 2;
        if (selected == null && !ModuleManager.all().isEmpty()) selected = ModuleManager.all().get(0);

        Draw.gradient(0, 0, width, height, 0xB0070610, 0xC0140A2A, true);
        Draw.card(x0 - 3, y0 - 3, pw + 6, ph + 6, 0x30000000, 0x18A78BFA);
        Draw.card(x0, y0, pw, ph, Theme.BG, Theme.BORDER);
        drawSidebar(mx, my);
        Draw.gradient(x0 + 1, y0, pw - 2, 2, Theme.VIOLET, 0xFFEC4899, false);

        int bx = x0 + SIDE + 10, by = y0 + 10, bw = pw - SIDE - 20, bh = ph - 20;
        switch (tab) {
            case MODS: drawMods(mx, my, bx, by, bw, bh); break;
            case COSMETICS: drawCosmetics(mx, my, bx, by, bw, bh); break;
            default: drawAbout(bx, by, bw, bh); break;
        }
    }

    // ---------------------------------------------------------------- sidebar

    private void drawSidebar(int mx, int my) {
        Draw.rect(x0 + 1, y0 + 2, SIDE - 1, ph - 3, Theme.SIDE);
        Draw.rect(x0 + SIDE, y0 + 2, 1, ph - 3, Theme.BORDER);
        Draw.logo(x0 + 10, y0 + 12, 28);
        Draw.heading("ATENAS", x0 + 44, y0 + 14, Theme.ACCENT);
        Draw.heading("CLIENT", x0 + 44, y0 + 25, Theme.DIM);
        String[] labels = {"Mods", "Cosm\u00e9ticos", "Editor HUD", SocialEvents.badgeLabel("Amigos"), "Acerca de"};
        int avail = ph - 50 - 28;
        int step = Math.max(17, Math.min(25, avail / labels.length));
        int rowH = step - 3;
        int y = y0 + 50;
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            boolean active = (i == 0 && tab == Tab.MODS) || (i == 1 && tab == Tab.COSMETICS)
                    || (i == 4 && tab == Tab.ABOUT);
            boolean hover = Draw.inside(mx, my, x0 + 6, y, SIDE - 12, rowH);
            if (active) {
                Draw.card(x0 + 6, y, SIDE - 12, rowH, Theme.PANEL2, Theme.BORDER);
                Draw.gradient(x0 + 6, y + 3, 2, rowH - 6, Theme.VIOLET, Theme.PINK, true);
            } else if (hover) {
                Draw.card(x0 + 6, y, SIDE - 12, rowH, Theme.HOVER, Theme.HOVER);
            }
            Draw.text(Draw.fit(labels[i], SIDE - 28), x0 + 16, y + (rowH - 8) / 2, (active || hover) ? Theme.TEXT : Theme.DIM);
            hits.add(new Hit(x0 + 6, y, SIDE - 12, rowH, new Runnable() {
                @Override public void run() { onSide(idx); }
            }));
            y += step;
        }
        if (y + 6 > y0 + ph - 24) return;   // sin sitio para el pie: no se dibuja (evita que se encime con "Acerca de")
        Draw.gradient(x0 + 10, y0 + ph - 24, SIDE - 20, 1, Theme.ACCENT_DIM, 0x006D4FC7, false);
        Draw.text("v" + AtenasClient.VERSION, x0 + 12, y0 + ph - 16, Theme.DIM);
    }

    private void onSide(int i) {
        switch (i) {
            case 0: tab = Tab.MODS; settingsOpen = false; break;
            case 1: mc.displayGuiScreen(new CosmeticsScreen(this)); break;
            case 2: mc.displayGuiScreen(new HudEditor(this)); break;
            case 3: mc.displayGuiScreen(new SocialScreen(this)); break;
            default: tab = Tab.ABOUT; break;
        }
    }

    // ------------------------------------------------------------------- mods

    private void openSettings(Module m) {
        selected = m;
        settingsOpen = true;
        scrollSet = 0;
        atCapturing = -1;
        atDirty = true;
    }

    private void addClipped(int x, int y, int w, int h, int top, int bottom, Runnable r) {
        int hy = Math.max(y, top);
        int hh = Math.min(y + h, bottom) - hy;
        if (hh > 0) hits.add(new Hit(x, hy, w, hh, r));
    }

    private void drawMods(int mx, int my, int bx, int by, int bw, int bh) {
        if (settingsOpen && selected != null) {
            int top = by + 20;
            Draw.text("Mods  \u203a  " + Lang.tr(selected.name), bx, by + 4, Theme.ACCENT);
            smallButton("\u2190 Volver", bx + bw - 64, by, 64, mx, my, new Runnable() {
                @Override public void run() { settingsOpen = false; atCapturing = -1; Config.save(); }
            });
            Draw.card(bx, top, bw, bh - 20, Theme.PANEL, Theme.BORDER);
            drawSettings(mx, my, bx, top, bw, bh - 20);
            return;
        }
        Draw.scaled("Mods", bx, by, 1.5f, Theme.TEXT, false);

        // buscador
        int sw = 120;
        int sx = bx + bw - sw;
        Draw.card(sx, by - 2, sw, 16, Theme.PANEL, search.isFocused() ? Theme.ACCENT : Theme.BORDER);
        search.xPosition = sx + 6; search.yPosition = by + 2; search.width = sw - 12; search.height = 10;
        if (search.getText().isEmpty() && !search.isFocused()) Draw.text("Buscar...", sx + 6, by + 2, Theme.OFF);
        search.drawTextBox();

        // filtros por categoria
        int cx = bx;
        List<Module.Category> chips = new ArrayList<Module.Category>();
        chips.add(null);
        for (Module.Category c : Module.Category.values()) chips.add(c);
        int chipY = by + 20;
        for (final Module.Category c : chips) {
            String label = c == null ? "Todos" : c.label;
            int w = Draw.width(label) + 16;
            if (cx + w > bx + bw) break;
            boolean on = c == filter;
            boolean hov = Draw.inside(mx, my, cx, chipY, w, 15);
            int col = c == null ? Theme.ACCENT : catColor(c);
            Draw.card(cx, chipY, w, 15, on ? Theme.PANEL2 : hov ? Theme.HOVER : Theme.PANEL, on ? col : Theme.BORDER);
            if (c != null) Draw.rect(cx + 5, chipY + 6, 3, 3, col);
            Draw.text(label, cx + (c == null ? 8 : 11), chipY + 4, on ? Theme.TEXT : Theme.DIM);
            hits.add(new Hit(cx, chipY, w, 15, new Runnable() { @Override public void run() { filter = c; scrollMods = 0; } }));
            cx += w + 4;
        }

        // cuadricula de tarjetas
        final int top = by + 42;
        final int listH = bh - 42;
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        List<Module> mods = new ArrayList<Module>();
        for (Module m : ModuleManager.all()) {
            if (filter != null && m.category != filter) continue;
            if (!q.isEmpty() && !Lang.tr(m.name).toLowerCase(Locale.ROOT).contains(q) && !Lang.tr(m.description).toLowerCase(Locale.ROOT).contains(q)) continue;
            mods.add(m);
        }
        int cols = bw >= 540 ? 3 : bw >= 300 ? 2 : 1;
        int gap = 8;
        int cw = (bw - 6 - gap * (cols - 1)) / cols, chh = 56;
        int rows = (mods.size() + cols - 1) / cols;
        int total = Math.max(0, rows * (chh + gap) - gap);
        scrollMods = Math.max(0, Math.min(scrollMods, Math.max(0, total - listH)));
        scrollModsSmooth += (scrollMods - scrollModsSmooth) * 0.35f;
        if (Math.abs(scrollMods - scrollModsSmooth) < 0.5f) scrollModsSmooth = scrollMods;
        int off = Math.round(scrollModsSmooth);
        if (mods.isEmpty()) Draw.centered("No hay mods con ese filtro.", bx + bw / 2, top + 30, Theme.DIM);
        Draw.scissor(bx, top, bw, listH);
        for (int i = 0; i < mods.size(); i++) {
            final Module m = mods.get(i);
            int x = bx + (i % cols) * (cw + gap);
            int y = top + (i / cols) * (chh + gap) - off;
            if (y + chh < top || y > top + listH) continue;
            int col = catColor(m.category);
            boolean on = m.isEnabled();
            boolean hov = Draw.inside(mx, my, x, y, cw, chh) && my >= top && my < top + listH;
            Draw.card(x, y, cw, chh, hov ? Theme.PANEL2 : Theme.PANEL, on ? col : hov ? Theme.ACCENT_DIM : Theme.BORDER);
            if (on) Draw.gradient(x + 1, y + 1, cw - 2, chh - 2, 0x00000000, (0x26 << 24) | (col & 0xFFFFFF), true);
            // icono: inicial en cuadro del color de la categoria
            Draw.rect(x + 8, y + 8, 22, 22, (0x40 << 24) | (col & 0xFFFFFF));
            Draw.border(x + 8, y + 8, 22, 22, col);
            Draw.centered(Lang.tr(m.name).substring(0, 1).toUpperCase(Locale.ROOT), x + 19, y + 15, Theme.TEXT);
            Draw.text(Draw.fit(m.name, cw - 36 - 34), x + 37, y + 9, Theme.TEXT);
            Draw.text(m.category.label, x + 37, y + 20, col);
            Draw.toggle(x + cw - 30, y + 8, on);
            boolean hasSettings = !m.settings.isEmpty() || m instanceof HudModule || m instanceof AutotextModule;
            List<String> desc = Draw.wrap(m.description, cw - 16 - (hasSettings ? 16 : 0), 2);
            for (int l = 0; l < desc.size(); l++) Draw.text(desc.get(l), x + 8, y + 34 + l * 10, Theme.DIM);
            if (hasSettings) {
                boolean gh = Draw.inside(mx, my, x + cw - 20, y + chh - 18, 16, 14);
                Draw.sliders(x + cw - 17, y + chh - 16, gh ? Theme.ACCENT : Theme.OFF);
            }
            int ay = Math.max(y, top), ah = Math.min(y + chh, top + listH) - ay;
            if (ah <= 0) continue;
            Hit card = new Hit(x, ay, cw, ah, new Runnable() {
                @Override public void run() { m.toggle(); }
            });
            if (hasSettings) {
                card.alt = new Runnable() {
                    @Override public void run() { openSettings(m); }
                };
            }
            hits.add(card);
            if (hasSettings) {
                final Module mm = m;
                int gy = Math.max(y + chh - 20, top), gh2 = Math.min(y + chh, top + listH) - gy;
                if (gh2 > 0) hits.add(new Hit(x + cw - 22, gy, 20, gh2, new Runnable() {
                    @Override public void run() { openSettings(mm); }
                }));
            }
        }
        Draw.endScissor();
        if (total > listH) {
            int barH = Math.max(18, listH * listH / total);
            int barY = top + (int) ((listH - barH) * (scrollModsSmooth / (double) (total - listH)));
            Draw.rect(bx + bw - 3, top, 2, listH, 0x20FFFFFF);
            Draw.gradient(bx + bw - 3, barY, 2, barH, Theme.VIOLET, Theme.PINK, true);
        }
    }

    private void drawSettings(int mx, int my, int sx, int top, int sw, int listH) {
        final Module m = selected;
        int px = sx + 14, pwid = sw - 28;
        // La mira tiene una vista previa fija arriba (no se va al hacer scroll) para ver cada cambio al instante.
        int clipTop = top;
        if (m instanceof CrosshairModule) {
            drawCrosshairPreview((CrosshairModule) m, px, top + 8, pwid, 64);
            clipTop = top + 78;
        }
        int viewH = listH - (clipTop - top);
        setTop = clipTop;
        setBottom = top + listH;
        scrollSet = Math.max(0, Math.min(scrollSet, Math.max(0, settingsH - viewH)));
        int py = clipTop + 10 - scrollSet;
        final int contentTop = py;
        Draw.scissor(sx, clipTop, sw, viewH);
        Draw.scaled(m.name, px, py, 1.3f, Theme.TEXT, false);
        py += 16;
        for (String line : fontRendererObj.listFormattedStringToWidth(m.description, pwid)) {
            Draw.text(line, px, py, Theme.DIM);
            py += 10;
        }
        if (m instanceof OldAnimationsModule) {
            boolean ok = OldAnimationsModule.mixinLoaded;
            Draw.text(ok ? "\u25cf Mixin activo" : "\u25cb Mixin sin comprobar (entra a un mundo con un objeto en la mano)", px, py + 2, ok ? Theme.GREEN : Theme.AMBER);
            py += 12;
        }
        py += 6;

        py = row(px, py, pwid, "Activado", m.isEnabled(), new Runnable() {
            @Override public void run() { m.toggle(); }
        });

        for (final Setting s : m.settings) {
            switch (s.type) {
                case BOOL:
                    py = row(px, py, pwid, s.name, s.bool, new Runnable() {
                        @Override public void run() { s.bool = !s.bool; Config.save(); }
                    });
                    break;
                case NUM: {
                    String v = s.step >= 1 ? String.valueOf((int) Math.round(s.num))
                            : String.format(Locale.US, "%.1f", s.num);
                    Draw.card(px - 6, py - 4, pwid + 12, 34, Theme.PANEL2, Theme.BORDER);
                    Draw.text(s.name, px + 2, py, Theme.TEXT);
                    Draw.text(v, px + pwid - 2 - Draw.width(v), py, Theme.ACCENT);
                    int ty = py + 16;
                    int tx = px + 2, tw = pwid - 4;
                    Draw.rect(tx, ty, tw, 4, Theme.OFF);
                    int fill = (int) (s.progress() * tw);
                    Draw.gradient(tx, ty, Math.max(1, fill), 4, Theme.VIOLET, Theme.PINK, false);
                    Draw.rect(Math.max(tx, Math.min(tx + tw - 3, tx + fill - 1)), ty - 2, 3, 8, Theme.TEXT);
                    final int sliderX = tx, sliderW = tw;
                    Hit h = new Hit(px - 4, py + 10, pwid + 8, 16, new Runnable() {
                        @Override public void run() { }
                    });
                    h.drag = new IntConsumer() {
                        @Override public void accept(int mouseX) {
                            s.setFromSlider((mouseX - sliderX) / (double) sliderW);
                        }
                    };
                    hits.add(h);
                    py += 40;
                    break;
                }
                case CHOICE: {
                    int n = s.options.length;
                    int[] xs = new int[n], ys = new int[n], ws = new int[n];
                    int cx = px + 2, cy = py + 13, rows = 1;
                    for (int i = 0; i < n; i++) {
                        int w = Draw.width(s.options[i]) + 14;
                        if (cx + w > px + pwid && cx > px + 2) { cx = px + 2; cy += 17; rows++; }
                        xs[i] = cx; ys[i] = cy; ws[i] = w;
                        cx += w + 4;
                    }
                    int h = 17 + rows * 17;
                    Draw.card(px - 6, py - 4, pwid + 12, h, Theme.PANEL2, Theme.BORDER);
                    Draw.text(s.name, px + 2, py, Theme.TEXT);
                    for (int i = 0; i < n; i++) {
                        final int idx = i;
                        boolean on = s.index() == i;
                        boolean hov = Draw.inside(mx, my, xs[i], ys[i], ws[i], 14);
                        Draw.card(xs[i], ys[i], ws[i], 14, on ? Theme.PANEL : hov ? Theme.HOVER : Theme.PANEL, on ? Theme.ACCENT : Theme.BORDER);
                        Draw.centered(s.options[i], xs[i] + ws[i] / 2, ys[i] + 3, on ? Theme.TEXT : Theme.DIM);
                        hits.add(new Hit(xs[i], ys[i], ws[i], 14, new Runnable() {
                            @Override public void run() { s.color = idx; Config.save(); }
                        }));
                    }
                    py += h + 4;
                    break;
                }
                default: {
                    int perRow = Math.max(1, (pwid - 4) / 18);
                    int crows = (Setting.PALETTE.length + perRow - 1) / perRow;
                    Draw.card(px - 6, py - 4, pwid + 12, 36 + (crows - 1) * 18, Theme.PANEL2, Theme.BORDER);
                    Draw.text(s.name, px + 2, py, Theme.TEXT);
                    for (int i = 0; i < Setting.PALETTE.length; i++) {
                        final int idx = i;
                        int cx = px + 2 + (i % perRow) * 18;
                        int cy = py + 13 + (i / perRow) * 18;
                        Draw.rect(cx, cy, 14, 14, Setting.PALETTE[i]);
                        if (s.color == i) Draw.border(cx - 1, cy - 1, 16, 16, Theme.TEXT);
                        hits.add(new Hit(cx, cy, 14, 14, new Runnable() {
                            @Override public void run() { s.color = idx; Config.save(); }
                        }));
                    }
                    py += 42 + (crows - 1) * 18;
                    break;
                }
            }
        }

        if (m instanceof AutotextModule) py = drawAutotext(mx, my, px, py, pwid, top, listH);

        if (m instanceof HudModule) {
            smallButton("Mover en pantalla", px, py, pwid, mx, my, new Runnable() {
                @Override public void run() { mc.displayGuiScreen(new HudEditor(AtenasMenu.this)); }
            });
        }
        settingsH = py - contentTop + 36;
        Draw.endScissor();
    }

    /** Vista previa en vivo de la mira: a la izquierda "normal" y a la derecha "apuntando a una entidad", sobre fondos de contraste. */
    private void drawCrosshairPreview(CrosshairModule c, int x, int y, int w, int h) {
        Draw.card(x - 6, y - 4, w + 12, h + 8, Theme.PANEL2, Theme.BORDER);
        int half = w / 2;
        int[] bg = {0xFF2B3A28, 0xFF1A1830};   // "pasto" y "oscuro": la mira se ve distinto sobre cada uno
        String[] label = {"Normal", "Apuntando"};
        for (int i = 0; i < 2; i++) {
            int bx = x + i * half + (i == 1 ? 2 : 0), bw = half - 2;
            Draw.gradient(bx, y, bw, h, bg[i], 0xFF0B0A14, true);
            Draw.border(bx, y, bw, h, Theme.BORDER);
            Draw.text(label[i], bx + 4, y + 3, Theme.DIM);
            c.draw(bx + bw / 2, y + h / 2 + 4, 1, 0, i == 0 ? 2 : 1);
        }
    }

    /** Editor de atajos dentro de los ajustes del mod Autotext: tecla o boton del mouse -> mensaje. Devuelve la siguiente y. */
    private int drawAutotext(int mx, int my, int px, int py, int pwid, int top, int listH) {
        final List<AutotextModule.Entry> es = AutotextModule.entries;
        if (atDirty || atFields.size() != es.size()) {
            atFields.clear();
            for (int i = 0; i < es.size(); i++) {
                GuiTextField f = new GuiTextField(300 + i, fontRendererObj, 0, 0, 100, 10);
                f.setEnableBackgroundDrawing(false);
                f.setMaxStringLength(100);
                f.setText(es.get(i).message == null ? "" : es.get(i).message);
                atFields.add(f);
            }
            atDirty = false;
        }
        Draw.text("ATAJOS", px, py, Theme.ACCENT_DIM);
        py += 12;
        for (String l : fontRendererObj.listFormattedStringToWidth(
                "Haz clic en el bot\u00f3n de la izquierda y pulsa una tecla o un bot\u00f3n del mouse (Retroceso = quitar). A la derecha escribe el mensaje o /comando.", pwid)) {
            Draw.text(l, px, py, Theme.DIM);
            py += 10;
        }
        py += 5;
        if (es.isEmpty()) {
            Draw.text("Sin atajos todav\u00eda.", px, py, Theme.DIM);
            py += 16;
        }
        final int keyW = 84;
        for (int i = 0; i < es.size(); i++) {
            final int idx = i;
            AutotextModule.Entry e = es.get(i);
            boolean cap = atCapturing == i;
            int ky = py;
            boolean khov = Draw.inside(mx, my, px, ky, keyW, 18) && my >= top && my < top + listH;
            Draw.card(px, ky, keyW, 18, khov ? Theme.HOVER : Theme.PANEL2, cap ? Theme.AMBER : khov ? Theme.ACCENT_DIM : Theme.BORDER);
            String kl = cap ? "Pulsa algo..." : AutotextModule.keyName(e.key);
            Draw.centered(Draw.fit(kl, keyW - 8), px + keyW / 2, ky + 5, cap ? Theme.AMBER : e.key == 0 ? Theme.DIM : Theme.TEXT);
            addClipped(px, ky, keyW, 18, top, top + listH, new Runnable() {
                @Override public void run() {
                    for (GuiTextField f : atFields) f.setFocused(false);
                    atCapturing = idx;
                }
            });
            int fx = px + keyW + 6, fw = pwid - keyW - 6 - 24;
            GuiTextField f = atFields.get(i);
            Draw.card(fx, ky, fw, 18, Theme.PANEL, f.isFocused() ? Theme.ACCENT : Theme.BORDER);
            f.xPosition = fx + 6;
            f.yPosition = ky + 5;
            f.width = fw - 12;
            f.height = 10;
            if (f.getText().isEmpty() && !f.isFocused()) Draw.text("mensaje o /comando", fx + 6, ky + 5, Theme.OFF);
            f.drawTextBox();
            addClipped(fx, ky, fw, 18, top, top + listH, new Runnable() {
                @Override public void run() { atCapturing = -1; }
            });
            int dx = px + pwid - 18;
            Draw.button("x", dx, ky, 18, 18, Draw.inside(mx, my, dx, ky, 18, 18) && my >= top && my < top + listH);
            addClipped(dx, ky, 18, 18, top, top + listH, new Runnable() {
                @Override public void run() {
                    if (idx < AutotextModule.entries.size()) AutotextModule.entries.remove(idx);
                    atCapturing = -1;
                    atDirty = true;
                    Config.save();
                }
            });
            py += 22;
        }
        if (es.size() < AutotextModule.MAX_ENTRIES) {
            smallButton("+ A\u00f1adir atajo", px, py, pwid, mx, my, new Runnable() {
                @Override public void run() {
                    AutotextModule.entries.add(new AutotextModule.Entry(0, ""));
                    atDirty = true;
                    Config.save();
                }
            });
            py += 22;
        }
        return py;
    }

    /** Fila con interruptor; devuelve la siguiente y. */
    private int row(int px, int py, int pwid, String label, boolean on, Runnable click) {
        Draw.card(px - 6, py - 4, pwid + 12, 20, Theme.PANEL2, Theme.BORDER);
        Draw.text(label, px + 2, py + 1, Theme.TEXT);
        Draw.toggle(px + pwid - 24, py, on);
        hits.add(new Hit(px - 6, py - 4, pwid + 12, 20, click));
        return py + 24;
    }

    private void smallButton(String label, int x, int y, int w, int mx, int my, Runnable r) {
        Draw.button(label, x, y, w, 18, Draw.inside(mx, my, x, y, w, 18));
        hits.add(new Hit(x, y, w, 18, r));
    }

    // -------------------------------------------------------------- cosmetics

    private long cosSeenAt;

    private void drawCosmetics(int mx, int my, int bx, int by, int bw, int bh) {
        long nowCos = System.currentTimeMillis();
        if (nowCos - cosSeenAt > 1500L) CosmeticSync.refreshNow(); // acaba de abrirse esta pesta\u00f1a
        cosSeenAt = nowCos;
        Draw.scaled("Cosm\u00e9ticos", bx, by, 1.5f, Theme.TEXT, false);
        final int top = by + 20;
        final int listH = bh - 20;
        final int prevW = 112;

        Draw.card(bx, top, prevW, listH, Theme.PANEL, Theme.BORDER);
        if (CosmeticSync.available()) {
            Draw.text("En l\u00ednea", bx + 8, top + 8, Theme.TEXT);
            Draw.toggle(bx + prevW - 30, top + 7, CosmeticSync.enabled);
            hits.add(new Hit(bx, top, prevW, 22, new Runnable() {
                @Override public void run() {
                    CosmeticSync.enabled = !CosmeticSync.enabled;
                    if (CosmeticSync.enabled) CosmeticSync.markDirty();
                    Config.save();
                }
            }));
        } else {
            Draw.text("Solo local", bx + 8, top + 8, Theme.DIM);
        }
        Draw.text("Animadas", bx + 8, top + 30, Theme.TEXT);
        Draw.toggle(bx + prevW - 30, top + 29, Cosmetics.animate);
        hits.add(new Hit(bx, top + 22, prevW, 22, new Runnable() {
            @Override public void run() { Cosmetics.animate = !Cosmetics.animate; Config.save(); }
        }));
        // botones de la vista previa: pausar/girar y quitar todo lo equipado
        final int by1 = top + 44;
        smallButton(spinPaused ? "Girar" : "Pausar", bx + 6, by1, prevW - 12, mx, my, new Runnable() {
            @Override public void run() { spinPaused = !spinPaused; }
        });
        smallButton("Quitar todo", bx + 6, by1 + 22, prevW - 12, mx, my, new Runnable() {
            @Override public void run() { for (Cosmetics.Type t : Cosmetics.Type.values()) Cosmetics.select(t, null); }
        });
        long nowMs = System.currentTimeMillis();
        if (!spinPaused) spinAngle = (spinAngle + (nowMs - spinLast) * 0.06f) % 360f;
        spinLast = nowMs;
        int pScale = Math.max(26, Math.min(60, (listH - 96) / 2));
        AbstractClientPlayer who = mc.thePlayer != null ? mc.thePlayer : mannequin();
        if (who != null) {
            drawPlayer(who, bx + prevW / 2 - 12, top + listH - 10, pScale);
        } else {
            List<String> msg = fontRendererObj.listFormattedStringToWidth(
                    "No se pudo crear la vista previa. Igual puedes elegir tus cosm\u00e9ticos.", prevW - 16);
            int ty = top + listH / 2 + 20 - msg.size() * 5;
            for (String l : msg) {
                Draw.centered(l, bx + prevW / 2, ty, Theme.DIM);
                ty += 10;
            }
        }
        if (Cosmetics.notice.length() > 0 && System.currentTimeMillis() < Cosmetics.noticeUntil) {
            Draw.text(Draw.fit(Cosmetics.notice, bw - prevW - 16), bx + prevW + 12, by + 5, Theme.AMBER);
        } else if (CosmeticSync.problem() != null) {
            // la sincronizacion con 1.20.1 esta fallando: se dice por que (antes solo quedaba en el log)
            Draw.text(Draw.fit(CosmeticSync.problem(), bw - prevW - 16), bx + prevW + 12, by + 5, Theme.AMBER);
        }

        int lx = bx + prevW + 8, lw = bw - prevW - 8;

        // pestanas: Capas / Alas / Sombreros
        if (cosFilter == null) cosFilter = Cosmetics.Type.CAPE;
        int fcx = lx;
        for (final Cosmetics.Type t : Cosmetics.Type.values()) {
            int w = Draw.width(t.label) + 18;
            if (fcx + w > lx + lw) break;
            boolean on = t == cosFilter;
            boolean hov = Draw.inside(mx, my, fcx, top, w, 16);
            Draw.card(fcx, top, w, 16, on ? Theme.PANEL2 : hov ? Theme.HOVER : Theme.PANEL, on ? Theme.ACCENT : Theme.BORDER);
            Draw.text(t.label, fcx + 9, top + 4, on ? Theme.TEXT : Theme.DIM);
            hits.add(new Hit(fcx, top, w, 16, new Runnable() { @Override public void run() { cosFilter = t; scrollCos = 0; } }));
            fcx += w + 4;
        }

        // cuadricula de tarjetas con icono (como en el mod de 1.20.1)
        final int gtop = top + 21;
        final int gh = listH - 21;
        final int cw = 62, ch = 58, gap = 6;
        final int cols = Math.max(1, (lw - 8 + gap) / (cw + gap));
        final int gx = lx + 4 + Math.max(0, (lw - 8 - (cols * cw + (cols - 1) * gap)) / 2);
        final Cosmetics.Type ty = cosFilter;

        List<Cosmetics.Item> entries = new ArrayList<Cosmetics.Item>();
        entries.add(null); // "Ninguno"
        entries.addAll(Cosmetics.visible(ty)); // los privados ajenos no salen
        int rowsN = (entries.size() + cols - 1) / cols;
        int total = rowsN * (ch + gap) + 4;
        scrollCos = Math.max(0, Math.min(scrollCos, Math.max(0, total - gh)));

        Draw.card(lx, gtop, lw, gh, Theme.PANEL, Theme.BORDER);
        Draw.scissor(lx + 1, gtop + 1, lw - 2, gh - 2);
        Cosmetics.Item cur = Cosmetics.selected(ty);
        for (int i = 0; i < entries.size(); i++) {
            final Cosmetics.Item it = entries.get(i);
            int x = gx + (i % cols) * (cw + gap);
            int y = gtop + 4 + (i / cols) * (ch + gap) - scrollCos;
            if (y + ch < gtop || y > gtop + gh) continue;
            boolean on = it == cur;
            boolean hov = Draw.inside(mx, my, x, y, cw, ch) && my >= gtop && my < gtop + gh;
            Draw.card(x, y, cw, ch, on || hov ? Theme.PANEL2 : Theme.HOVER, on ? Theme.ACCENT : hov ? Theme.ACCENT_DIM : Theme.BORDER);
            if (it == null) {
                Draw.centered("\u2715", x + cw / 2, y + 16, Theme.DIM);
            } else {
                mc.getTextureManager().bindTexture(it.icon);
                GlStateManager.enableBlend();
                GlStateManager.color(1f, 1f, 1f, 1f);
                net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x + (cw - 32) / 2, y + 6, 0f, 0f, 32, 32, 32f, 32f);
            }
            Draw.centered(Draw.fit(it == null ? "Ninguno" : it.name, cw - 4), x + cw / 2, y + ch - 14, on ? Theme.TEXT : Theme.DIM);
            if (on) Draw.rect(x + cw - 9, y + 3, 6, 6, Theme.ACCENT);
            addClipped(x, y, cw, ch, gtop, gtop + gh, new Runnable() {
                @Override public void run() { Cosmetics.select(ty, it); }
            });
        }
        Draw.endScissor();
    }

    /** Jugador girando para ver capa, alas y sombrero desde todos los lados. */
    private void drawPlayer(AbstractClientPlayer ent, int posX, int posY, int scale) {
        float spin = spinAngle;
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY, 50f);
        GlStateManager.scale((float) -scale, (float) scale, (float) scale);
        GlStateManager.rotate(180f, 0f, 0f, 1f);
        float rYawOff = ent.renderYawOffset, rYaw = ent.rotationYaw, rPitch = ent.rotationPitch;
        float rPrevHead = ent.prevRotationYawHead, rHead = ent.rotationYawHead;
        float rPrevOff = ent.prevRenderYawOffset;
        GlStateManager.rotate(135f, 0f, 1f, 0f);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135f, 0f, 1f, 0f);
        ent.renderYawOffset = spin;
        ent.prevRenderYawOffset = spin;
        ent.rotationYaw = spin;
        ent.rotationPitch = 0f;
        ent.rotationYawHead = spin;
        ent.prevRotationYawHead = spin;
        GlStateManager.translate(0f, 0f, 0f);
        RenderManager rm = mc.getRenderManager();
        rm.setPlayerViewY(180f);
        rm.setRenderShadow(false);
        try {
            rm.renderEntityWithPosYaw(ent, 0.0, 0.0, 0.0, 0f, 1f);
        } catch (RuntimeException ex) {
            // sin mundo, el nombre del maniqui puede fallar al final del dibujo (el cuerpo ya se dibujo): no es un error
            if (ent != mc.thePlayer && ++mannequinFails > 30) mannequin = null;
        }
        rm.setRenderShadow(true);
        ent.renderYawOffset = rYawOff;
        ent.prevRenderYawOffset = rPrevOff;
        ent.rotationYaw = rYaw;
        ent.rotationPitch = rPitch;
        ent.prevRotationYawHead = rPrevHead;
        ent.rotationYawHead = rHead;
        // la mascota elegida, a un lado (en el mismo marco del jugador, sin girar con el)
        Cosmetics.Item pet = Cosmetics.selected(Cosmetics.Type.PET);
        if (pet != null) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(-135f, 0f, 1f, 0f);
            GlStateManager.rotate(135f, 0f, 1f, 0f);
            com.atenas.client.cosmetics.PetRender.renderPreview(pet);
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /** Jugador de mentira (en un mundo vacio) para la vista previa cuando no hay partida. Devuelve null si no se puede crear. */
    private AbstractClientPlayer mannequin() {
        if (mannequin != null) return mannequin;
        if (mannequinFails > 30 || mc.getSession() == null) return null;
        try {
            net.minecraft.world.WorldSettings ws = new net.minecraft.world.WorldSettings(0L,
                    net.minecraft.world.WorldSettings.GameType.SURVIVAL, false, false, net.minecraft.world.WorldType.DEFAULT);
            net.minecraft.client.multiplayer.WorldClient w = new net.minecraft.client.multiplayer.WorldClient(null, ws, 0,
                    net.minecraft.world.EnumDifficulty.PEACEFUL, mc.mcProfiler);
            mannequin = new net.minecraft.client.entity.EntityOtherPlayerMP(w, mc.getSession().getProfile()) {
                @Override public int getBrightnessForRender(float pt) { return 15728880; } // sin mundo la luz seria 0
                // Sin partida no hay "net handler": estos metodos lo consultan y daban NullPointerException (el jugador no se dibujaba)
                @Override public boolean isSpectator() { return false; }
                @Override public boolean hasPlayerInfo() { return false; }
                @Override public String getSkinType() { return "default"; }
                @Override public net.minecraft.util.ResourceLocation getLocationSkin() { return com.atenas.client.social.Faces.skinOrDefault(); }
                @Override public net.minecraft.util.ResourceLocation getLocationCape() { return null; }
            };
            Cosmetics.previewPlayer = mannequin;
        } catch (Throwable t) {
            mannequinFails = 99;
            System.err.println("[AtenasClient] maniqui de vista previa: " + t);
        }
        return mannequin;
    }

    // ------------------------------------------------------------------ about

    private void drawAbout(int bx, int by, int bw, int bh) {
        Draw.scaled("Acerca de", bx, by, 1.5f, Theme.TEXT, false);
        int top = by + 20;
        Draw.card(bx, top, bw, bh - 20, Theme.PANEL, Theme.BORDER);
        Draw.logo(bx + 14, top + 14, 56);
        Draw.scaled("Atenas Client", bx + 82, top + 18, 2f, Theme.TEXT, false);
        Draw.text(Lang.t("Versi\u00f3n ", "Version ") + AtenasClient.VERSION + "  \u00b7  Minecraft 1.8.9 (Forge)", bx + 82, top + 40, Theme.DIM);
        String[] info = {
            "RShift  \u2192  abrir / cerrar este men\u00fa   \u00b7   Shift+Tab  \u2192  amigos y chat",
            "Mods \u2192 elige un m\u00f3dulo para ver sus ajustes.",
            "Editor HUD \u2192 arrastra cada m\u00f3dulo y usa la rueda para su tama\u00f1o.",
            "Sin ventajas injustas: solo mejoras visuales y de utilidad."
        };
        int y = top + 78;
        for (String s : info) {
            Draw.text(s, bx + 14, y, Theme.TEXT);
            y += 13;
        }
        boolean of = AtenasClient.hasOptiFine();
        Draw.text("OptiFine: " + (of ? "detectado" : "no detectado (opcional, se activa en Ajustes del launcher)"), bx + 14, y, of ? Theme.GREEN : Theme.DIM);
        y += 17;
        Draw.rect(bx + 14, y, bw - 28, 1, Theme.BORDER);
        y += 6;
        Draw.text("AMIGOS Y CHAT", bx + 14, y, Theme.ACCENT_DIM);
        y += 12;
        socialToggle(bx + 14, y, bw - 28, "Activar amigos y chat", SocialClient.enabled, 0);
        y += 15;
        socialToggle(bx + 14, y, bw - 28, "Avisos en pantalla", SocialClient.toasts, 1);
        y += 15;
        socialToggle(bx + 14, y, bw - 28, "Mostrar mi estado a mis amigos", SocialClient.sharePresence, 2);
    }

    private void socialToggle(int x, int y, int w, String label, boolean on, final int which) {
        Draw.text(label, x, y + 1, Theme.TEXT);
        Draw.toggle(x + w - 22, y, on);
        hits.add(new Hit(x, y - 2, w, 14, new Runnable() {
            @Override public void run() {
                if (which == 0) SocialClient.enabled = !SocialClient.enabled;
                else if (which == 1) SocialClient.toasts = !SocialClient.toasts;
                else SocialClient.sharePresence = !SocialClient.sharePresence;
                Config.save();
            }
        }));
    }

    // ------------------------------------------------------------------ input

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) {
            int step = d > 0 ? -17 : 17;
            if (tab == Tab.MODS && settingsOpen) scrollSet += step;
            else if (tab == Tab.MODS) scrollMods += step;
            else if (tab == Tab.COSMETICS) scrollCos += step * 2;
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (settingsOpen && selected instanceof AutotextModule && atCapturing >= 0 && atCapturing < AutotextModule.entries.size()) {
            AutotextModule.entries.get(atCapturing).key = button - 100;   // boton del mouse (0 = clic izquierdo)
            atCapturing = -1;
            Config.save();
            return;
        }
        super.mouseClicked(mx, my, button);
        if (search != null) search.mouseClicked(mx, my, button);
        if (settingsOpen && selected instanceof AutotextModule && button == 0 && my >= setTop && my < setBottom) {
            for (GuiTextField f : atFields) f.mouseClicked(mx, my, button);
        } else {
            for (GuiTextField f : atFields) f.setFocused(false);
        }
        if (button == 1) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.contains(mx, my) && h.alt != null) { h.alt.run(); return; }
            }
            return;
        }
        if (button != 0) return;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(mx, my)) {
                h.click.run();
                if (h.drag != null) {
                    dragging = h;
                    h.drag.accept(mx);
                }
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        if (dragging != null) dragging.drag.accept(mx);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        if (dragging != null) {
            dragging = null;
            Config.save();
        }
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (settingsOpen && selected instanceof AutotextModule) {
            if (atCapturing >= 0 && atCapturing < AutotextModule.entries.size()) {
                AutotextModule.Entry en = AutotextModule.entries.get(atCapturing);
                if (code == Keyboard.KEY_ESCAPE) {
                    // cancelar
                } else if (code == Keyboard.KEY_BACK || code == Keyboard.KEY_DELETE) {
                    en.key = 0;
                } else {
                    en.key = code;
                }
                atCapturing = -1;
                Config.save();
                return;
            }
            for (int i = 0; i < atFields.size() && i < AutotextModule.entries.size(); i++) {
                GuiTextField f = atFields.get(i);
                if (!f.isFocused()) continue;
                if (code == Keyboard.KEY_ESCAPE) { f.setFocused(false); return; }
                f.textboxKeyTyped(c, code);
                AutotextModule.entries.get(i).message = f.getText();
                return;
            }
        }
        if (search != null && search.isFocused() && tab == Tab.MODS && !settingsOpen) {
            if (code == Keyboard.KEY_ESCAPE) { search.setFocused(false); return; }
            search.textboxKeyTyped(c, code);
            scrollMods = 0;
            return;
        }
        if (code == Keyboard.KEY_ESCAPE && settingsOpen) { settingsOpen = false; return; }
        if (code == Keyboard.KEY_ESCAPE || code == AtenasClient.keyMenu.getKeyCode()) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void updateScreen() {
        if (search != null) search.updateCursorCounter();
        for (GuiTextField f : atFields) f.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() { Cosmetics.previewPlayer = null; mannequin = null; Keyboard.enableRepeatEvents(false); Config.save(); }
}
