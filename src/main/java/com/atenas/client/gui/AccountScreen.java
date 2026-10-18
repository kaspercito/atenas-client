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

/**
 * Cuentas dentro del juego:
 *  - cambiar de cuenta (las sin conexion al instante; las de Microsoft reinician el juego),
 *  - agregar una cuenta premium (el launcher abre el inicio de sesion de Microsoft),
 *  - quitar una cuenta guardada o cerrar sesion de la activa,
 *  - entrar con un nick sin conexion.
 * Todo lo que toca la lista de cuentas lo hace el launcher (ver {@link Accounts}); esta pantalla solo pide y espera la respuesta.
 */
public class AccountScreen extends GuiScreen {

    private static final int ROW = 28, GAP = 3, ID_BACK = 0, ID_OFFLINE = 1;
    private static final long ACK_MS = 6000L;          // si el launcher no toma el pedido en este tiempo, no esta abierto
    private static final long LOGIN_MS = 6 * 60000L;   // tiempo maximo esperando el inicio de sesion de Microsoft

    private static final class Hit {
        final int x, y, w, h;
        final Runnable click;
        Hit(int x, int y, int w, int h, Runnable click) { this.x = x; this.y = y; this.w = w; this.h = h; this.click = click; }
        boolean contains(int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    /** Ventana de confirmacion encima de la pantalla. */
    private static final class Confirm {
        final String title, text, okLabel;
        final boolean danger;
        final Runnable onOk;
        Confirm(String title, String text, String okLabel, boolean danger, Runnable onOk) {
            this.title = title; this.text = text; this.okLabel = okLabel; this.danger = danger; this.onOk = onOk;
        }
    }

    /** Pedido enviado al launcher que todavia no tiene respuesta. */
    private static final class Pending {
        final String id, label;
        final boolean slow;          // true = espera al usuario (ventana de Microsoft)
        final Runnable onOk;
        final long since = System.currentTimeMillis();
        Pending(String id, String label, boolean slow, Runnable onOk) { this.id = id; this.label = label; this.slow = slow; this.onOk = onOk; }
    }

    private final GuiScreen parent;
    private final List<Hit> hits = new ArrayList<Hit>();
    private List<Accounts.Entry> list = new ArrayList<Accounts.Entry>();
    private GuiTextField nameField;
    private Confirm confirm;
    private Pending pending;
    private String notice = "";
    private boolean noticeBad;
    private long listStamp;
    private int scroll, rows, px, py, pw, ph, listTop, premiumY, fieldY, bottomY, ticks;

    public AccountScreen(GuiScreen parent) { this.parent = parent; }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        reloadList();
        pw = Math.min(340, width - 16);
        rows = Math.max(1, Math.min(list.size(), 4));
        // alto = cabecera + lista + boton premium + bloque "otro nombre" + fila inferior (aviso + Volver)
        while (true) {
            ph = 50 + rows * (ROW + GAP) + 4 + 20 + 8 + 11 + 22 + 8 + 20 + 10;
            if (ph <= height - 8 || rows == 1) break;
            rows--;
        }
        if (ph > height - 4) ph = height - 4;
        px = (width - pw) / 2;
        py = Math.max(2, (height - ph) / 2);
        listTop = py + 50;
        premiumY = listTop + rows * (ROW + GAP) + 4;
        fieldY = premiumY + 20 + 8 + 11;
        bottomY = fieldY + 22 + 8;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - rows)));

        String keep = nameField == null ? "" : nameField.getText();
        // misma geometria que la caja que se dibuja: texto centrado en vertical y con aire a los lados
        nameField = new GuiTextField(2, fontRendererObj, px + 14 + 5, fieldY + (22 - 8) / 2, pw - 28 - 78 - 10, 8);
        nameField.setEnableBackgroundDrawing(false);
        nameField.setMaxStringLength(16);
        nameField.setCanLoseFocus(true);
        nameField.setText(keep);

        buttonList.add(new AtenasButton(ID_OFFLINE, px + pw - 14 - 72, fieldY, 72, 22, "Entrar"));
        buttonList.add(new AtenasButton(ID_BACK, px + pw - 14 - 90, bottomY, 90, 20, "Volver"));
    }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    private void reloadList() {
        list = Accounts.list();
        listStamp = Accounts.listStamp();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - Math.max(1, rows))));
    }

    @Override
    public void updateScreen() {
        if (nameField != null) nameField.updateCursorCounter();
        ticks++;
        // el launcher agrego o quito una cuenta: se vuelve a leer la lista (y se reacomoda la pantalla si cambia el alto)
        if (ticks % 10 == 0 && Accounts.listStamp() != listStamp) {
            int before = list.size();
            reloadList();
            if (list.size() != before) initGui();
        }
        if (pending != null && ticks % 4 == 0) checkPending();
    }

    // ------------------------------------------------------------ acciones

    private void say(String text, boolean bad) { notice = text; noticeBad = bad; }

    private void back() { mc.displayGuiScreen(parent); }

    /** Envia un pedido al launcher y se queda esperando su respuesta. */
    private void ask(String action, String target, String name, String label, boolean slow, Runnable onOk) {
        if (pending != null) return;
        try {
            String id = Accounts.request(action, target, name);
            pending = new Pending(id, label, slow, onOk);
            say("", false);
        } catch (IOException e) {
            say(e.getMessage(), true);
        }
    }

    private void checkPending() {
        Pending p = pending;
        Accounts.Result r = Accounts.result(p.id);
        if (r != null) {
            pending = null;
            say(r.message.length() > 0 ? r.message : (r.ok ? "Listo" : "No se pudo completar"), !r.ok);
            if (r.ok) {
                int before = list.size();
                reloadList();
                if (list.size() != before) initGui();
                if (p.onOk != null) p.onOk.run();
            }
            return;
        }
        long age = System.currentTimeMillis() - p.since;
        if (Accounts.requestQueued() && age > ACK_MS) { // nadie lo tomo: el launcher esta cerrado
            Accounts.cancelRequest();
            pending = null;
            say("El launcher no responde. Abrelo y vuelve a intentar.", true);
        } else if (age > (p.slow ? LOGIN_MS : ACK_MS * 2)) {
            pending = null;
            say("Se acabo el tiempo de espera.", true);
        }
    }

    private void useAccount(Accounts.Entry e) {
        if (Accounts.isActive(e)) return;
        if (e.offline()) {
            String err = Accounts.switchToOffline(e.name);
            if (err != null) say(err, true);
            else say("Ahora juegas como " + e.name, false);
        } else {
            confirm = new Confirm("Entrar como §e" + e.name,
                    "El juego se va a cerrar y el launcher lo abre otra vez con esa cuenta.", "Reiniciar", false,
                    new Runnable() { @Override public void run() { switchMicrosoft(); } });
            switchTarget = e;
        }
    }

    private Accounts.Entry switchTarget;

    private void switchMicrosoft() {
        Accounts.Entry e = switchTarget;
        switchTarget = null;
        if (e == null) return;
        String err = Accounts.requestLauncherSwitch(e);
        if (err != null) { say(err, true); return; }
        mc.shutdown(); // el launcher activa la cuenta y abre el juego otra vez
    }

    private void addPremium() {
        if (!Accounts.launchedByLauncher()) { say("Abre el juego desde el launcher para agregar una cuenta premium.", true); return; }
        ask("add-microsoft", null, null, "Inicia sesion en la ventana de Microsoft", true, null);
    }

    /** Quitar una cuenta guardada que no esta en uso. */
    private void askRemove(final Accounts.Entry e) {
        if (!Accounts.launchedByLauncher()) { say("Abre el juego desde el launcher para quitar cuentas.", true); return; }
        confirm = new Confirm("Quitar §e" + e.name,
                "Se borra de la lista del launcher. Tu cuenta no se elimina: puedes agregarla otra vez cuando quieras.", "Quitar", true,
                new Runnable() { @Override public void run() { ask("remove", e.id, null, "Quitando " + e.name, false, null); } });
    }

    /** Cerrar sesion de la cuenta que esta jugando: se quita del launcher y el juego se cierra. */
    private void askLogout(final Accounts.Entry e) {
        if (!Accounts.launchedByLauncher()) { say("Abre el juego desde el launcher para cerrar sesion.", true); return; }
        confirm = new Confirm("Cerrar sesion de §e" + e.name,
                "Se quita esta cuenta del launcher y el juego se cierra. Para volver a jugar con ella tendras que iniciar sesion otra vez.", "Cerrar sesion", true,
                new Runnable() {
                    @Override public void run() {
                        ask("remove", e.id, null, "Cerrando sesion", false, new Runnable() { @Override public void run() { mc.shutdown(); } });
                    }
                });
    }

    private void enterOffline() {
        String n = nameField.getText().trim();
        String err = Accounts.switchToOffline(n);
        if (err != null) say(err, true);
        else { say("Ahora juegas como " + n + " (sin conexion)", false); nameField.setText(""); }
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (confirm != null || pending != null) return;
        if (b.id == ID_BACK) back();
        else if (b.id == ID_OFFLINE) enterOffline();
    }

    // ------------------------------------------------------------ dibujo

    private static String kindLabel(Accounts.Entry e) { return e.offline() ? "Sin conexion" : "Premium"; }

    private boolean modal() { return confirm != null || pending != null; }

    /** Boton chico de la lista. Devuelve nada: registra su zona de clic. */
    private void small(String label, int x, int y, int w, int h, int mx, int my, int hoverColor, boolean dim, Runnable r) {
        boolean over = !modal() && Draw.inside(mx, my, x, y, w, h);
        Draw.rect(x, y, w, h, over ? Theme.PANEL2 : Theme.PANEL);
        Draw.border(x, y, w, h, over ? hoverColor : Theme.BORDER);
        Draw.centered(label, x + w / 2, y + (h - 8) / 2, dim ? Theme.OFF : over ? hoverColor : Theme.TEXT);
        hits.add(new Hit(x, y, w, h, r));
    }

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
        Draw.centered("Jugando como §f" + cur, px + pw / 2, py + 28, Theme.DIM);
        Draw.rect(px + 14, py + 41, pw - 28, 1, Theme.BORDER);

        boolean viaLauncher = Accounts.launchedByLauncher();

        // lista de cuentas
        if (list.isEmpty()) {
            Draw.centered("El launcher todavia no guardo cuentas.", px + pw / 2, listTop + 4, Theme.DIM);
            Draw.centered("Agrega una premium o entra con un nombre.", px + pw / 2, listTop + 16, Theme.DIM);
        }
        int from = scroll, to = Math.min(list.size(), scroll + rows);
        for (int i = from; i < to; i++) {
            final Accounts.Entry e = list.get(i);
            int y = listTop + (i - from) * (ROW + GAP);
            int x = px + 14, w = pw - 28 - (list.size() > rows ? 8 : 0);
            boolean active = Accounts.isActive(e);
            boolean over = !modal() && Draw.inside(mx, my, x, y, w, ROW);
            Draw.rect(x, y, w, ROW, over ? Theme.PANEL2 : Theme.PANEL);
            Draw.border(x, y, w, ROW, active ? Theme.ACCENT_DIM : over ? Theme.VIOLET : Theme.BORDER);
            Draw.rect(x, y, 2, ROW, active ? Theme.VIOLET : Theme.ACCENT_DIM);
            // inicial del nick en un cuadro
            if (!com.atenas.client.social.Faces.draw(e.name, x + 8, y + 5, 18, 1f, e.offline())) {
                Draw.rect(x + 8, y + 5, 18, 18, active ? Theme.VIOLET : Theme.ACCENT_DIM);
                Draw.centered(e.name.substring(0, 1).toUpperCase(), x + 17, y + 10, Theme.INK);
            }

            int by = y + 6, bh = 16;
            int right = x + w - 6;
            if (active) {
                int cw = 78;
                small("Cerrar sesion", right - cw, by, cw, bh, mx, my, Theme.RED, !viaLauncher, new Runnable() { @Override public void run() { askLogout(e); } });
                String t = "● Activa";
                int tw = Draw.width(t);
                Draw.text(t, right - cw - 6 - tw, y + 10, Theme.GREEN);
                right -= cw + 6 + tw + 6;
            } else {
                small("✕", right - 18, by, 18, bh, mx, my, Theme.RED, !viaLauncher, new Runnable() { @Override public void run() { askRemove(e); } });
                small("Usar", right - 18 - 4 - 44, by, 44, bh, mx, my, Theme.ACCENT, false, new Runnable() { @Override public void run() { useAccount(e); } });
                right -= 18 + 4 + 44 + 6;
            }
            int nameW = Math.max(30, right - (x + 34));
            Draw.text(Draw.fit(e.name, nameW), x + 34, y + 6, active ? Theme.ACCENT : Theme.TEXT);
            Draw.text(kindLabel(e), x + 34, y + 16, e.offline() ? Theme.DIM : Theme.CYAN);
        }
        if (list.size() > rows) { // barra de scroll
            int bx = px + pw - 14 - 4, th = rows * (ROW + GAP) - GAP;
            Draw.rect(bx, listTop, 3, th, 0x20FFFFFF);
            int bh = Math.max(14, th * rows / list.size());
            int by = listTop + (th - bh) * scroll / Math.max(1, list.size() - rows);
            Draw.rect(bx, by, 3, bh, Theme.ACCENT_DIM);
        }

        // agregar cuenta premium
        {
            int x = px + 14, w = pw - 28, h = 20;
            boolean over = !modal() && viaLauncher && Draw.inside(mx, my, x, premiumY, w, h);
            Draw.rect(x, premiumY, w, h, over ? Theme.PANEL2 : Theme.PANEL);
            Draw.border(x, premiumY, w, h, over ? Theme.VIOLET : viaLauncher ? Theme.ACCENT_DIM : Theme.BORDER);
            Draw.rect(x, premiumY, 2, h, viaLauncher ? Theme.VIOLET : Theme.OFF);
            Draw.centered("+  Agregar cuenta premium (Microsoft)", x + w / 2, premiumY + 6, !viaLauncher ? Theme.OFF : over ? Theme.ACCENT : Theme.TEXT);
            hits.add(new Hit(x, premiumY, w, h, new Runnable() { @Override public void run() { addPremium(); } }));
        }

        // entrar con un nombre sin conexion
        Draw.text("Entrar con otro nombre (sin conexion)", px + 14, premiumY + 20 + 8, Theme.DIM);
        Draw.rect(px + 14, fieldY, pw - 28 - 78, 22, 0xFF080A14);
        Draw.border(px + 14, fieldY, pw - 28 - 78, 22, nameField.isFocused() ? Theme.VIOLET : Theme.BORDER);
        nameField.drawTextBox();
        if (nameField.getText().length() == 0 && !nameField.isFocused()) Draw.text("Nick de Minecraft", px + 14 + 5, fieldY + (22 - 8) / 2, Theme.OFF);

        // aviso (izquierda de "Volver")
        int noticeW = pw - 28 - 90 - 8;
        if (notice.length() > 0) {
            Draw.text(Draw.fit(notice, noticeW), px + 14, bottomY + 6, noticeBad ? Theme.RED : Theme.GREEN);
        } else if (!viaLauncher) {
            Draw.text(Draw.fit("Abre el juego desde el launcher para usar cuentas premium.", noticeW), px + 14, bottomY + 6, Theme.DIM);
        }

        if (modal()) {
            // con una ventana abierta solo cuentan sus propios botones (los de la lista de abajo no deben recibir el clic)
            hits.clear();
            if (confirm != null) drawConfirm(mx, my); else drawPending(mx, my);
        } else {
            super.drawScreen(mx, my, pt);
        }
    }

    private void drawConfirm(int mx, int my) {
        Draw.rect(px, py, pw, ph, 0xCC000000);
        int w = Math.min(pw - 24, 290), x = px + (pw - w) / 2;
        List<String> lines = Draw.wrap(confirm.text, w - 20, 4);
        int h = 14 + 10 + 6 + lines.size() * 10 + 10 + 18 + 10, y = py + (ph - h) / 2;
        int accent = confirm.danger ? Theme.RED : Theme.ACCENT_DIM;
        Draw.card(x, y, w, h, 0xFF0F1224, accent);
        Draw.gradient(x + 1, y, w - 2, 2, accent, confirm.danger ? Theme.PINK : Theme.ACCENT, false);
        Draw.centered(Draw.fit(confirm.title, w - 16), x + w / 2, y + 9, Theme.TEXT);
        for (int i = 0; i < lines.size(); i++) Draw.centered(lines.get(i), x + w / 2, y + 26 + i * 10, Theme.DIM);
        int bw = (w - 30) / 2, by = y + h - 28;
        boolean okHover = Draw.inside(mx, my, x + 10, by, bw, 18);
        Draw.card(x + 10, by, bw, 18, okHover ? Theme.PANEL2 : Theme.PANEL, okHover ? (confirm.danger ? Theme.RED : Theme.ACCENT) : Theme.BORDER);
        Draw.centered(confirm.okLabel, x + 10 + bw / 2, by + 5, okHover ? (confirm.danger ? Theme.RED : Theme.ACCENT) : Theme.TEXT);
        Draw.button("Cancelar", x + w - 10 - bw, by, bw, 18, Draw.inside(mx, my, x + w - 10 - bw, by, bw, 18));
        final Confirm c = confirm;
        hits.add(new Hit(x + 10, by, bw, 18, new Runnable() { @Override public void run() { confirm = null; if (c.onOk != null) c.onOk.run(); } }));
        hits.add(new Hit(x + w - 10 - bw, by, bw, 18, new Runnable() { @Override public void run() { confirm = null; switchTarget = null; } }));
    }

    private void drawPending(int mx, int my) {
        Draw.rect(px, py, pw, ph, 0xCC000000);
        int w = Math.min(pw - 24, 290), x = px + (pw - w) / 2;
        String hint = pending.slow
                ? "Se abrio la ventana de Microsoft en el launcher. Si no la ves, busca el launcher en la barra de tareas."
                : "Esperando al launcher...";
        List<String> lines = Draw.wrap(hint, w - 20, 3);
        int h = 14 + 10 + 6 + lines.size() * 10 + 10 + 18 + 10, y = py + (ph - h) / 2;
        Draw.card(x, y, w, h, 0xFF0F1224, Theme.ACCENT_DIM);
        Draw.gradient(x + 1, y, w - 2, 2, Theme.ACCENT_DIM, Theme.ACCENT, false);
        int dots = (int) ((System.currentTimeMillis() / 400) % 4);
        String dot = "...".substring(0, dots);
        Draw.centered(Draw.fit(pending.label, w - 24) + dot, x + w / 2, y + 9, Theme.TEXT);
        for (int i = 0; i < lines.size(); i++) Draw.centered(lines.get(i), x + w / 2, y + 26 + i * 10, Theme.DIM);
        int bw = 90, bx = x + (w - bw) / 2, by = y + h - 28;
        Draw.button("Cancelar", bx, by, bw, 18, Draw.inside(mx, my, bx, by, bw, 18));
        hits.add(new Hit(bx, by, bw, 18, new Runnable() {
            @Override public void run() { Accounts.cancelRequest(); pending = null; say("Cancelado.", false); }
        }));
    }

    // ------------------------------------------------------------ entrada

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (!modal()) {
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
        if (d != 0 && !modal() && list.size() > rows) {
            scroll = Math.max(0, Math.min(list.size() - rows, scroll + (d > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (code == Keyboard.KEY_ESCAPE) {
            if (pending != null) { Accounts.cancelRequest(); pending = null; say("Cancelado.", false); }
            else if (confirm != null) { confirm = null; switchTarget = null; }
            else back();
            return;
        }
        if (confirm != null) {
            if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) {
                Confirm cf = confirm;
                confirm = null;
                if (cf.onOk != null) cf.onOk.run();
            }
            return;
        }
        if (pending != null) return;
        if (nameField.isFocused()) {
            if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) enterOffline();
            else nameField.textboxKeyTyped(c, code);
        }
    }
}
