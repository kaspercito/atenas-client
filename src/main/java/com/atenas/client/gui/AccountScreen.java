package com.atenas.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.atenas.client.account.Accounts;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

/** Cambiar de cuenta desde el menu principal: cuentas guardadas en el launcher + entrar con un nick sin conexion. */
public class AccountScreen extends GuiScreen {

    private static final int ROW = 28, GAP = 3, ID_BACK = 0, ID_OFFLINE = 1;

    private static final class Hit {
        final int x, y, w, h;
        final Runnable click;
        Hit(int x, int y, int w, int h, Runnable click) { this.x = x; this.y = y; this.w = w; this.h = h; this.click = click; }
        boolean contains(int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final GuiScreen parent;
    private final List<Hit> hits = new ArrayList<Hit>();
    private List<Accounts.Entry> list = new ArrayList<Accounts.Entry>();
    private GuiTextField nameField;
    private Accounts.Entry confirm;       // cuenta de Microsoft pendiente de confirmar (hay que reiniciar el juego)
    private String notice = "";
    private boolean noticeBad;
    private int scroll, rows, px, py, pw, ph, listTop, fieldY;

    public AccountScreen(GuiScreen parent) { this.parent = parent; }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        list = Accounts.list();
        pw = Math.min(340, width - 16);
        rows = Math.max(1, Math.min(list.size(), 4));
        // alto = cabecera + lista + bloque "otro nombre" + aviso + boton volver
        while (true) {
            ph = 50 + rows * (ROW + GAP) + 10 + 12 + 20 + 18 + 30;
            if (ph <= height - 8 || rows == 1) break;
            rows--;
        }
        if (ph > height - 4) ph = height - 4;
        px = (width - pw) / 2;
        py = Math.max(2, (height - ph) / 2);
        listTop = py + 50;
        fieldY = listTop + rows * (ROW + GAP) + 22;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - rows)));

        String keep = nameField == null ? "" : nameField.getText();
        nameField = new GuiTextField(2, fontRendererObj, px + 14, fieldY + 5, pw - 28 - 78, 12);
        nameField.setEnableBackgroundDrawing(false);
        nameField.setMaxStringLength(16);
        nameField.setCanLoseFocus(true);
        nameField.setText(keep);

        buttonList.add(new AtenasButton(ID_OFFLINE, px + pw - 14 - 72, fieldY, 72, 22, "Entrar"));
        buttonList.add(new AtenasButton(ID_BACK, px + pw - 14 - 90, py + ph - 30, 90, 20, "Volver"));
    }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    @Override
    public void updateScreen() { if (nameField != null) nameField.updateCursorCounter(); }

    // ------------------------------------------------------------ acciones

    private void say(String text, boolean bad) { notice = text; noticeBad = bad; }

    private void back() { mc.displayGuiScreen(parent); }

    private void useAccount(Accounts.Entry e) {
        if (Accounts.isActive(e)) return;
        if (e.offline()) {
            String err = Accounts.switchToOffline(e.name);
            if (err != null) say(err, true);
            else say("Ahora juegas como " + e.name, false);
        } else {
            confirm = e; // las de Microsoft piden reiniciar el juego: se confirma antes
        }
    }

    private void confirmMicrosoft() {
        Accounts.Entry e = confirm;
        confirm = null;
        if (e == null) return;
        String err = Accounts.requestLauncherSwitch(e);
        if (err != null) { say(err, true); return; }
        mc.shutdown(); // el launcher activa la cuenta y abre el juego otra vez
    }

    private void enterOffline() {
        String n = nameField.getText().trim();
        String err = Accounts.switchToOffline(n);
        if (err != null) say(err, true);
        else { say("Ahora juegas como " + n + " (sin conexion)", false); nameField.setText(""); }
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (confirm != null) return;
        if (b.id == ID_BACK) back();
        else if (b.id == ID_OFFLINE) enterOffline();
    }

    // ------------------------------------------------------------ dibujo

    private static String kindLabel(Accounts.Entry e) { return e.offline() ? "Sin conexion" : "Premium"; }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        hits.clear();
        if (parent != null) {
            // el menu de atras se ve apagado: asi se entiende que esto es una ventana encima
            Draw.gradient(0, 0, width, height, 0xFF080A14, 0xFF171C3C, true);
        } else {
            drawDefaultBackground();
        }
        Draw.rect(0, 0, width, height, 0x99000000);
        Draw.card(px - 3, py - 3, pw + 6, ph + 6, 0x30000000, 0x188B5CF6);
        Draw.card(px, py, pw, ph, 0xF00A0D1C, Theme.BORDER);
        Draw.gradient(px + 1, py, pw - 2, 2, Theme.ACCENT_DIM, Theme.ACCENT, false);

        Draw.scaled("CUENTAS", px + pw / 2f, py + 10, 1.5f, Theme.ACCENT, true);
        String cur = mc.getSession() == null ? "" : mc.getSession().getUsername();
        Draw.centered("Jugando como \u00a7f" + cur, px + pw / 2, py + 28, Theme.DIM);
        Draw.rect(px + 14, py + 41, pw - 28, 1, Theme.BORDER);

        // lista de cuentas
        if (list.isEmpty()) {
            Draw.centered("El launcher todavia no guardo cuentas.", px + pw / 2, listTop + 4, Theme.DIM);
            Draw.centered("Entra con un nombre aqui abajo.", px + pw / 2, listTop + 16, Theme.DIM);
        }
        int from = scroll, to = Math.min(list.size(), scroll + rows);
        for (int i = from; i < to; i++) {
            final Accounts.Entry e = list.get(i);
            int y = listTop + (i - from) * (ROW + GAP);
            int x = px + 14, w = pw - 28 - (list.size() > rows ? 8 : 0);
            boolean active = Accounts.isActive(e);
            boolean over = confirm == null && Draw.inside(mx, my, x, y, w, ROW);
            Draw.rect(x, y, w, ROW, over ? Theme.PANEL2 : Theme.PANEL);
            Draw.border(x, y, w, ROW, active ? Theme.ACCENT_DIM : over ? Theme.VIOLET : Theme.BORDER);
            Draw.rect(x, y, 2, ROW, active ? Theme.VIOLET : Theme.ACCENT_DIM);
            // inicial del nick en un cuadro dorado
            Draw.rect(x + 8, y + 5, 18, 18, active ? Theme.VIOLET : Theme.ACCENT_DIM);
            Draw.centered(e.name.substring(0, 1).toUpperCase(), x + 17, y + 10, Theme.INK);
            Draw.text(Draw.fit(e.name, w - 150), x + 34, y + 6, active ? Theme.ACCENT : Theme.TEXT);
            Draw.text(kindLabel(e), x + 34, y + 16, e.offline() ? Theme.DIM : Theme.CYAN);
            if (active) {
                String t = "\u25cf Activa";
                Draw.text(t, x + w - Draw.width(t) - 10, y + 10, Theme.GREEN);
            } else {
                int bw = 52, bx = x + w - bw - 6, by = y + 5;
                boolean bo = confirm == null && Draw.inside(mx, my, bx, by, bw, 18);
                Draw.button("Usar", bx, by, bw, 18, bo);
                hits.add(new Hit(bx, by, bw, 18, new Runnable() { @Override public void run() { useAccount(e); } }));
            }
        }
        if (list.size() > rows) { // barra de scroll
            int bx = px + pw - 14 - 4, th = rows * (ROW + GAP) - GAP;
            Draw.rect(bx, listTop, 3, th, 0x20FFFFFF);
            int bh = Math.max(14, th * rows / list.size());
            int by = listTop + (th - bh) * scroll / Math.max(1, list.size() - rows);
            Draw.rect(bx, by, 3, bh, Theme.ACCENT_DIM);
        }

        // entrar con un nombre sin conexion
        int ly = listTop + rows * (ROW + GAP) + 6;
        Draw.text("Entrar con otro nombre (sin conexion)", px + 14, ly, Theme.DIM);
        Draw.rect(px + 14, fieldY, pw - 28 - 78, 22, 0xFF080A14);
        Draw.border(px + 14, fieldY, pw - 28 - 78, 22, nameField.isFocused() ? Theme.VIOLET : Theme.BORDER);
        nameField.drawTextBox();
        if (nameField.getText().length() == 0 && !nameField.isFocused()) Draw.text("Nick de Minecraft", px + 18, fieldY + 7, Theme.OFF);

        if (notice.length() > 0) {
            Draw.centered(Draw.fit(notice, pw - 24), px + pw / 2, fieldY + 28, noticeBad ? Theme.RED : Theme.GREEN);
        } else if (!Accounts.launchedByLauncher()) {
            Draw.centered("Para cuentas de Microsoft abre el juego desde el launcher.", px + pw / 2, fieldY + 28, Theme.DIM);
        }

        if (confirm != null) drawConfirm(mx, my); else super.drawScreen(mx, my, pt); // con la confirmacion abierta no se dibujan los botones de atras
    }

    private void drawConfirm(int mx, int my) {
        Draw.rect(px, py, pw, ph, 0xCC000000);
        int w = Math.min(pw - 24, 280), h = 86, x = px + (pw - w) / 2, y = py + (ph - h) / 2;
        Draw.card(x, y, w, h, 0xFF0F1224, Theme.ACCENT_DIM);
        Draw.gradient(x + 1, y, w - 2, 2, Theme.ACCENT_DIM, Theme.ACCENT, false);
        Draw.centered("Entrar como \u00a7e" + confirm.name, x + w / 2, y + 9, Theme.TEXT);
        List<String> lines = Draw.wrap("El juego se va a cerrar y el launcher lo abre otra vez con esa cuenta.", w - 20, 3);
        for (int i = 0; i < lines.size(); i++) Draw.centered(lines.get(i), x + w / 2, y + 24 + i * 10, Theme.DIM);
        int bw = (w - 30) / 2, by = y + h - 26;
        Draw.button("Reiniciar", x + 10, by, bw, 18, Draw.inside(mx, my, x + 10, by, bw, 18));
        Draw.button("Cancelar", x + w - 10 - bw, by, bw, 18, Draw.inside(mx, my, x + w - 10 - bw, by, bw, 18));
        hits.add(new Hit(x + 10, by, bw, 18, new Runnable() { @Override public void run() { confirmMicrosoft(); } }));
        hits.add(new Hit(x + w - 10 - bw, by, bw, 18, new Runnable() { @Override public void run() { confirm = null; } }));
    }

    // ------------------------------------------------------------ entrada

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (confirm == null) {
            super.mouseClicked(mx, my, button);
            nameField.mouseClicked(mx, my, button);
        }
        if (button != 0) return;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(mx, my)) { h.click.run(); return; }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0 && confirm == null && list.size() > rows) {
            scroll = Math.max(0, Math.min(list.size() - rows, scroll + (d > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (code == Keyboard.KEY_ESCAPE) {
            if (confirm != null) confirm = null; else back();
            return;
        }
        if (confirm != null) {
            if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) confirmMicrosoft();
            return;
        }
        if (nameField.isFocused()) {
            if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) enterOffline();
            else nameField.textboxKeyTyped(c, code);
        }
    }
}
