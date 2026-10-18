package com.atenas.client.social;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.atenas.client.core.Lang;
import com.atenas.client.gui.Backdrop;
import com.atenas.client.gui.Draw;
import com.atenas.client.gui.Theme;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** Amigos + chat (Shift+Tab, menu principal y menu de pausa). Izquierda: lista con caras; derecha: conversacion. */
public class SocialScreen extends GuiScreen {

    private static String openChatKey; // para no avisar de mensajes del chat que ya estas viendo
    public static boolean isChattingWith(String key) { return key != null && key.equals(openChatKey); }

    private static final class Hit {
        final int x, y, w, h;
        final Runnable click;
        Hit(int x, int y, int w, int h, Runnable click) { this.x = x; this.y = y; this.w = w; this.h = h; this.click = click; }
        boolean contains(int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    /** Un mensaje ya partido en lineas (se recalcula solo si cambia). */
    private static final class Bub {
        List<String> lines;
        int w, h, sig, maxW;
        String time;
    }

    // Medidas y colores IGUALES a la ventana de Amigos de 1.20.1 (mod atenas-social).
    private static final int HEAD = 38, FOOT = 18, PAD = 10, CUT = 6, FIELD_H = 20, FRIEND_H = 32, REQ_H = 30, SEC_H = 20;
    private static final int PANEL = 0xFF0F0D22, PANEL2 = 0xFF1E1B38, LINE = 0xFF2B2848, ACCENT = 0xFF8B5CF6, ACCENT_LIGHT = 0xFFA78BFA, PINK = 0xFFEC4899;
    private static final int TEXT = 0xFFF1EEFC, MUTED = 0xFF9B97B8, FAINT = 0xFF6E6A8F, GREEN = 0xFF34D399, RED = 0xFFFB7185, AMBER = 0xFFFBBF24, CYAN = 0xFF22D3EE, OFFLINE = 0xFF5A5680;
    private static final int NOTICE_MS = 6000;

    private final GuiScreen parent;
    private final List<Hit> hits = new ArrayList<Hit>();
    private final List<SocialClient.Msg> msgs = new ArrayList<SocialClient.Msg>();
    private final Map<SocialClient.Msg, Bub> bubCache = new IdentityHashMap<SocialClient.Msg, Bub>();
    private GuiTextField addField, chatField;
    private String selected;
    private long lastId;
    private int scrollList, scrollChat, ticks, pollTick;
    private boolean loadingChat;
    private String confirmRemove;
    private String notice = "";
    private boolean noticeBad;
    private long noticeAt;
    private int x0, y0, pw, ph;
    private final float[] glow = new float[3]; // brillo animado de los botones: 0 = agregar, 1 = enviar, 2 = quitar

    public SocialScreen(GuiScreen parent) { this.parent = parent; }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    // ------------------------------------------------------------ medidas (initGui y el dibujo usan las MISMAS)
    private int lw() { return pw < 440 ? 150 : 178; }
    private int bodyTop() { return y0 + HEAD + 8; }
    private int bodyBottom() { return y0 + ph - FOOT - 6; }
    private int listX() { return x0 + PAD; }
    private int listTop() { return bodyTop() + FIELD_H + 6; }
    private int cx() { return x0 + PAD + lw() + 8; }
    private int cw() { return pw - lw() - PAD * 2 - 8; }
    private int inputY() { return bodyBottom() - FIELD_H - 6; }
    private int inputX() { return cx() + 6; }
    private int inputW() { return cw() - 12 - 56 - 4; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(520, width - 16);
        ph = Math.min(320, height - 16);
        x0 = (width - pw) / 2;
        y0 = (height - ph) / 2;
        String keepAdd = addField == null ? "" : addField.getText();
        String keepChat = chatField == null ? "" : chatField.getText();
        addField = field(1, listX() + 22, bodyTop() + (FIELD_H - 8) / 2, lw() - 54, 16);
        addField.setText(keepAdd);
        chatField = field(2, inputX() + 8, inputY() + (FIELD_H - 8) / 2, inputW() - 16, 300);
        chatField.setText(keepChat);
        chatField.setFocused(selected != null);
        if (SocialClient.snapshot() == null || SocialClient.sinceLastSync() > 3000) SocialClient.sync(mc.theWorld != null, "");
    }

    /** Campo de texto sin fondo propio (la caja la dibuja inputBox): el texto se pinta justo en (fx, fy). */
    private GuiTextField field(int id, int fx, int fy, int fw, int max) {
        GuiTextField f = new GuiTextField(id, fontRendererObj, fx, fy, fw, 8);
        f.setMaxStringLength(max);
        f.setEnableBackgroundDrawing(false);
        f.setCanLoseFocus(true);
        return f;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        openChatKey = null;
    }

    @Override
    public void updateScreen() {
        addField.updateCursorCounter();
        chatField.updateCursorCounter();
        ticks++;
        if (selected != null && ++pollTick >= 50) { pollTick = 0; pollChat(); }
        if (selected != null && SocialClient.find(selected) == null && SocialClient.snapshot() != null) closeChat();
    }

    // ------------------------------------------------------------ acciones

    private void say(final String text, final boolean bad) {
        mc.addScheduledTask(new Runnable() {
            @Override public void run() { notice = text; noticeBad = bad; noticeAt = System.currentTimeMillis(); }
        });
    }

    private void openChat(String key) {
        if (key.equals(selected)) return;
        selected = key;
        openChatKey = key;
        msgs.clear();
        bubCache.clear();
        lastId = 0;
        scrollChat = 0;
        confirmRemove = null;
        chatField.setFocused(true);
        addField.setFocused(false);
        loadChat(0);
    }

    private void closeChat() { selected = null; openChatKey = null; msgs.clear(); bubCache.clear(); lastId = 0; }

    private void loadChat(final long after) {
        final String key = selected;
        if (key == null || loadingChat) return;
        loadingChat = true;
        SocialClient.history(key, after, new SocialClient.Callback<List<SocialClient.Msg>>() {
            @Override public void done(final List<SocialClient.Msg> list, final String err) {
                mc.addScheduledTask(new Runnable() {
                    @Override public void run() {
                        loadingChat = false;
                        if (!key.equals(selected)) return;
                        if (err != null) { notice = err; noticeBad = true; noticeAt = System.currentTimeMillis(); return; }
                        // los mensajes que se estan enviando (pending) se quedan hasta que llegue la respuesta del envio
                        boolean added = false;
                        for (SocialClient.Msg m : list) {
                            boolean dup = false;
                            for (SocialClient.Msg o : msgs) if (o.id == m.id) { dup = true; break; }
                            if (!dup) { msgs.add(m); added = true; }
                            if (m.id > lastId) lastId = m.id;
                        }
                        if (added) { sortMsgs(); scrollChat = 0; }
                    }
                });
            }
        });
    }

    private void pollChat() { loadChat(lastId); }

    /** Por id; los que todavia no tienen id (recien enviados) van al final. */
    private void sortMsgs() {
        Collections.sort(msgs, new Comparator<SocialClient.Msg>() {
            @Override public int compare(SocialClient.Msg a, SocialClient.Msg b) {
                long x = a.id == 0 ? Long.MAX_VALUE : a.id, y = b.id == 0 ? Long.MAX_VALUE : b.id;
                if (x != y) return x < y ? -1 : 1;
                return a.ts < b.ts ? -1 : a.ts > b.ts ? 1 : 0;
            }
        });
    }

    private void sendChat() {
        final String text = chatField.getText().trim();
        final String key = selected;
        if (text.isEmpty() || key == null) return;
        chatField.setText("");
        final SocialClient.Msg temp = new SocialClient.Msg();
        temp.mine = true;
        temp.body = text;
        temp.ts = System.currentTimeMillis();
        temp.pending = true;
        msgs.add(temp);
        scrollChat = 0;
        SocialClient.send(key, text, new SocialClient.Callback<SocialClient.Msg>() {
            @Override public void done(final SocialClient.Msg m, final String err) {
                mc.addScheduledTask(new Runnable() {
                    @Override public void run() {
                        if (err != null || m == null) { temp.pending = false; temp.failed = true; say(err, true); return; }
                        temp.pending = false;
                        boolean dupe = false;
                        for (SocialClient.Msg o : msgs) if (o != temp && o.id == m.id) { dupe = true; break; }
                        if (dupe) msgs.remove(temp); // el sondeo ya trajo este mensaje desde el servidor
                        else { temp.id = m.id; temp.ts = m.ts; }
                        // lastId NO se sube aqui: si el amigo escribio justo antes, su mensaje tiene un id menor y no se pediria nunca.
                        sortMsgs();
                        pollTick = 45;
                    }
                });
            }
        });
    }

    private void sendRequest() {
        String n = addField.getText().trim();
        if (n.isEmpty()) return;
        if (!SocialClient.validName(n)) { say(Lang.t("Nick no valido (3-16 letras, numeros o _).", "Invalid name (3-16 letters, numbers or _)."), true); return; }
        addField.setText("");
        SocialClient.add(n, new SocialClient.Callback<String>() {
            @Override public void done(String v, String err) {
                if (err != null) say(err, true);
                else say(v.startsWith("accepted") ? Lang.t("Ahora eres amigo de ", "You are now friends with ") + v.substring(v.indexOf('|') + 1) : Lang.t("Solicitud enviada a ", "Request sent to ") + v.substring(v.indexOf('|') + 1), false);
            }
        });
    }

    private SocialClient.Callback<String> plain(final String ok) {
        return new SocialClient.Callback<String>() {
            @Override public void done(String v, String err) { say(err != null ? err : ok, err != null); }
        };
    }

    // ------------------------------------------------------------ piezas de dibujo

    private String stateText(SocialClient.Friend f) {
        if (f.playing()) return f.server.isEmpty() ? Lang.t("Jugando", "Playing") : Lang.t("Jugando en ", "Playing on ") + f.server;
        return f.online() ? Lang.t("En el launcher", "In the launcher") : Lang.t("Desconectado", "Offline");
    }

    private int stateColor(SocialClient.Friend f) {
        return f.playing() ? GREEN : f.online() ? CYAN : OFFLINE;
    }

    /** Cara del jugador (o su inicial mientras llega) con marco del color de su estado y un punto de estado opcional. */
    private void avatar(String name, int x, int y, int s, int ring, boolean dim, boolean dot) {
        Draw.rect(x - 1, y - 1, s + 2, s + 2, ring);
        if (!Faces.draw(name, x, y, s, 1f, false)) {
            Draw.rect(x, y, s, s, 0xFF2A2550);
            String ini = name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
            Draw.centered(ini, x + s / 2, y + (s - 8) / 2, ACCENT_LIGHT);
        }
        if (dim) Draw.rect(x, y, s, s, 0x88090812);
        if (dot) {
            Draw.rect(x + s - 5, y + s - 5, 7, 7, PANEL);
            Draw.rect(x + s - 4, y + s - 4, 5, 5, ring);
        }
    }

    private void smallBtn(String label, int x, int y, int w, int h, int mx, int my, int color, Runnable r) {
        boolean hov = Draw.inside(mx, my, x, y, w, h);
        Draw.batchBegin();
        Draw.chamfer(x, y, w, h, 2, hov ? PANEL2 : PANEL);
        Draw.chamferBorder(x, y, w, h, 2, hov ? color : LINE);
        Draw.batchEnd();
        Draw.centered(label, x + w / 2, y + (h - 8) / 2, hov ? color : TEXT);
        hits.add(new Hit(x, y, w, h, r));
    }

    /** Boton futurista (igual que Ui.Btn de 1.20.1): "primary" lleva degradado violeta -> rosa; el otro es cristal con barra de neon al pasar el mouse. */
    private void btn(int slot, String label, int x, int y, int w, int h, int mx, int my, boolean primary, Runnable r) {
        boolean hot = Draw.inside(mx, my, x, y, w, h);
        glow[slot] += ((hot ? 1f : 0f) - glow[slot]) * 0.22f;
        float gl = glow[slot];
        int cut = h >= 18 ? 4 : 3;
        Draw.batchBegin();
        if (primary) {
            Draw.chamfer(x, y, w, h, cut, 0xFF2A1A5E);
            Draw.gradient(x + cut, y + 1, w - 2 * cut, h - 2, hot ? ACCENT_LIGHT : ACCENT, hot ? PINK : 0xFF6D4FC7, true);
            Draw.chamferBorder(x, y, w, h, cut, hot ? 0xFFFFFFFF : ACCENT_LIGHT);
        } else {
            Draw.chamfer(x, y, w, h, cut, 0xE60D0B1E);
            int a = (int) (0x46 * gl);
            Draw.gradient(x + cut, y + h / 2, w - 2 * cut, h - 1 - h / 2, 0x008B5CF6, (a << 24) | 0x8B5CF6, true);
            Draw.chamferBorder(x, y, w, h, cut, hot ? ACCENT_LIGHT : 0xFF3A3466);
            int bar = (int) (gl * (w - 2 * cut));
            if (bar > 0) Draw.gradient(x + w / 2 - bar / 2, y + h - 2, bar, 1, ACCENT, CYAN, false);
            Draw.rect(x + w - cut - 5, y, 5, 1, hot ? CYAN : 0xFF6D4FC7);
            Draw.rect(x + cut, y + h - 1, 5, 1, hot ? PINK : 0xFF6D4FC7);
        }
        Draw.batchEnd();
        Draw.headingFit(label, x + cut, w - 2 * cut, y + (h - 8) / 2, hot ? 0xFFFFFFFF : TEXT, (int) (gl * 1.5f));
        hits.add(new Hit(x, y, w, h, r));
    }

    private void closeBtn(int x, int y, int s, int mx, int my) {
        boolean hov = Draw.inside(mx, my, x, y, s, s);
        Draw.batchBegin();
        Draw.chamfer(x, y, s, s, 3, hov ? 0xFF3A1F3F : PANEL);
        Draw.chamferBorder(x, y, s, s, 3, hov ? RED : LINE);
        int c = hov ? RED : TEXT;
        for (int i = 0; i < 7; i++) {
            Draw.rect(x + 5 + i, y + 5 + i, 1, 1, c);
            Draw.rect(x + 11 - i, y + 5 + i, 1, 1, c);
        }
        Draw.batchEnd();
        hits.add(new Hit(x, y, s, s, new Runnable() { @Override public void run() { mc.displayGuiScreen(parent); } }));
    }

    /** Caja de texto: cristal oscuro con esquinas cortadas; con el foco, borde claro y una linea de degradado abajo. */
    private void inputBox(int x, int y, int w, int h, boolean focus) {
        Draw.batchBegin();
        Draw.chamfer(x, y, w, h, 3, 0xFF0A0918);
        Draw.chamferBorder(x, y, w, h, 3, focus ? ACCENT_LIGHT : LINE);
        if (focus) Draw.gradient(x + 4, y + h - 1, w - 8, 1, ACCENT, PINK, false);
        Draw.batchEnd();
    }

    private static void userIcon(int x, int y, int c) {
        Draw.rect(x + 3, y, 4, 4, c);
        Draw.rect(x + 2, y + 5, 6, 1, c);
        Draw.rect(x + 1, y + 6, 8, 3, c);
    }

    // ------------------------------------------------------------ dibujo

    @Override
    public void drawScreen(int mx, int my, float pt) {
        hits.clear();
        long t = System.currentTimeMillis();

        // fondo: con mundo detras, un velo oscuro; sin mundo, el fondo animado de Atenas (una sola llamada de dibujo)
        boolean inWorld = mc.theWorld != null;
        if (!inWorld) Backdrop.render(width, height);

        // Toda la "carcasa" (velo, resplandor, panel, cabecera) son formas lisas: un solo lote de dibujo
        Draw.batchBegin();
        if (inWorld) Draw.gradient(0, 0, width, height, 0xC0070914, 0xE00E0B22, true);
        for (int k = 3; k >= 1; k--) Draw.chamfer(x0 - k * 2, y0 - k * 2, pw + k * 4, ph + k * 4, CUT + k * 2, ((0x16 / k) << 24) | 0x8B5CF6);
        Draw.chamfer(x0, y0, pw, ph, CUT, 0xF4090818);
        Draw.chamferBorder(x0, y0, pw, ph, CUT, 0xFF3A3466);
        // cabecera
        Draw.gradient(x0 + CUT, y0 + 1, pw - 2 * CUT, CUT - 1, 0xFF1B1740, 0xFF181436, true);
        Draw.gradient(x0 + 1, y0 + CUT, pw - 2, HEAD - CUT, 0xFF181436, 0xFF0C0A1A, true);
        Draw.rect(x0 + 1, y0 + HEAD, pw - 2, 1, LINE);
        Draw.gradient(x0 + CUT, y0, pw - 2 * CUT, 2, ACCENT, PINK, false);
        Draw.gradient(x0 + 14, y0 + HEAD, 70, 1, ACCENT, 0x008B5CF6, false);
        float pulse = (float) (0.5 + 0.5 * Math.sin(t * 0.003));
        Draw.rect(x0 + 10, y0 + 7, 26, 26, (((int) (12 + 16 * pulse)) << 24) | 0x8B5CF6);
        Draw.batchEnd();

        Draw.logo(x0 + 12, y0 + 9, 22);
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) (x0 + 42), (float) (y0 + 8), 0f);
        GlStateManager.scale(1.25f, 1.25f, 1f);
        Draw.heading("Amigos", 0, 0, TEXT);
        GlStateManager.popMatrix();

        String st = SocialClient.status();
        SocialClient.Snapshot snap = SocialClient.snapshot();
        int sx = x0 + 42, sy = y0 + 23;
        if (snap == null || !"ok".equals(st)) {
            boolean err = "error".equals(st);
            String sub = "noaccount".equals(st) ? Lang.t("Inicia sesión", "Sign in") : "disabled".equals(st) ? Lang.t("Desactivado en ajustes", "Disabled in settings")
                    : err ? Lang.t("Sin conexión", "Offline") : Lang.t("Conectando...", "Connecting...");
            Draw.rect(sx, sy + 2, 5, 5, err ? RED : AMBER);
            Draw.text(sub, sx + 9, sy, err ? RED : MUTED);
        } else {
            int on = onlineNow();
            Draw.rect(sx, sy + 2, 5, 5, on > 0 ? GREEN : OFFLINE);
            String line = snap.friends.isEmpty() ? Lang.t("Sin amigos todavía", "No friends yet")
                    : Lang.t(on + " de " + snap.friends.size() + " conectados", on + " of " + snap.friends.size() + " online");
            Draw.text(line, sx + 9, sy, MUTED);
        }
        closeBtn(x0 + pw - 26, y0 + 11, 16, mx, my);
        if (!notice.isEmpty() && System.currentTimeMillis() - noticeAt < NOTICE_MS) { // aviso dentro de la cabecera (hasta 2 lineas)
            int fx = x0 + 170, fw = pw - 170 - 40;
            if (fw > 60) {
                List<String> ls = Draw.wrap(notice, fw - 12, 2);
                int n = ls.size();
                int fy = y0 + (HEAD - n * 10) / 2 + 1;
                Draw.batchBegin();
                Draw.chamfer(fx - 6, y0 + 6, fw, HEAD - 12, 2, 0xCC2A1A5E);
                Draw.rect(fx - 6, y0 + 8, 2, HEAD - 16, AMBER);
                Draw.batchEnd();
                for (int i = 0; i < n; i++) Draw.text(ls.get(i), fx + 2, fy + i * 10, AMBER);
            }
        }

        drawList(mx, my);
        drawChat(mx, my);
        drawFooter();
        super.drawScreen(mx, my, pt);
    }

    /** Pie: atajos a la izquierda y, a la derecha, tu nick y si la cuenta esta verificada con Mojang (los cosmeticos privados lo exigen). */
    private void drawFooter() {
        int fy = y0 + ph - FOOT;
        Draw.rect(x0 + 1, fy, pw - 2, 1, LINE);
        if (pw >= 400) Draw.text(Lang.t("Esc cerrar  ·  Enter enviar  ·  Rueda para desplazar", "Esc close  ·  Enter send  ·  Scroll to move"), x0 + PAD, fy + 6, FAINT);
        boolean ok = SocialClient.verified();
        String who = mc.getSession() == null ? "" : mc.getSession().getUsername();
        String tag = who + "  ·  " + (ok ? Lang.t("cuenta verificada", "verified account") : Lang.t("sin verificar", "not verified"));
        int tx = x0 + pw - PAD - Draw.width(tag);
        Draw.rect(tx - 9, fy + 8, 5, 5, ok ? GREEN : AMBER);
        Draw.text(tag, tx, fy + 6, ok ? MUTED : AMBER);
    }

    private int onlineNow() {
        SocialClient.Snapshot s = SocialClient.snapshot();
        int on = 0;
        if (s != null) for (SocialClient.Friend f : s.friends) if (f.online()) on++;
        return on;
    }

    private void drawList(int mx, int my) {
        int lx = listX(), top = bodyTop(), w = lw();
        // campo para agregar
        boolean af = addField.isFocused();
        inputBox(lx, top, w - 26, FIELD_H, af);
        userIcon(lx + 7, top + 6, af ? ACCENT_LIGHT : FAINT);
        if (addField.getText().isEmpty() && !af) Draw.text(Lang.t("Agregar amigo...", "Add friend..."), lx + 22, top + (FIELD_H - 8) / 2, FAINT);
        addField.drawTextBox();
        btn(0, "+", lx + w - 22, top, 22, FIELD_H, mx, my, true, new Runnable() { @Override public void run() { sendRequest(); } });

        int ly = listTop(), lb = bodyBottom(), lh = lb - ly;
        Draw.batchBegin();
        Draw.chamfer(lx, ly, w, lh, 3, PANEL);
        Draw.chamferBorder(lx, ly, w, lh, 3, LINE);
        Draw.batchEnd();
        SocialClient.Snapshot s = SocialClient.snapshot();
        if (s == null || !SocialClient.usable()) {
            boolean err = "error".equals(SocialClient.status());
            String msg = err ? Lang.t("Sin conexión. ", "Offline. ") + SocialClient.error()
                    : "noaccount".equals(SocialClient.status()) ? Lang.t("Inicia sesión para usar amigos", "Sign in to use friends")
                    : "disabled".equals(SocialClient.status()) ? Lang.t("Desactivado en ajustes", "Disabled in settings") : Lang.t("Conectando...", "Connecting...");
            drawWrapped(msg, lx + 10, ly + 12, w - 20, err ? RED : MUTED);
            return;
        }
        List<Object> rows = new ArrayList<Object>();
        if (!s.incoming.isEmpty()) { rows.add(new Title(Lang.t("Solicitudes", "Requests"), s.incoming.size(), AMBER)); rows.addAll(s.incoming); }
        addGroup(rows, s, "playing", Lang.t("Jugando ahora", "Playing now"), GREEN);
        addGroup(rows, s, "online", Lang.t("En línea", "Online"), CYAN);
        addGroup(rows, s, "offline", Lang.t("Desconectados", "Offline"), OFFLINE);
        if (!s.outgoing.isEmpty()) { rows.add(new Title(Lang.t("Enviadas", "Sent"), s.outgoing.size(), FAINT)); for (SocialClient.Req r : s.outgoing) rows.add(new Object[] {r}); }
        if (rows.isEmpty()) {
            Draw.logo(lx + w / 2 - 14, ly + lh / 2 - 48, 28);
            Draw.centered(Lang.t("Aún no tienes amigos", "No friends yet"), lx + w / 2, ly + lh / 2 - 12, TEXT);
            drawWrapped(Lang.t("Escribe su nick arriba y envía una solicitud.", "Type their name above and send a request."), lx + 12, ly + lh / 2 + 2, w - 24, MUTED);
            return;
        }
        int total = 0;
        for (Object o : rows) total += rowHeight(o);
        scrollList = Math.max(0, Math.min(scrollList, Math.max(0, total - lh + 4)));
        Draw.scissor(lx + 1, ly + 1, w - 2, lh - 2);
        int y = ly + 2 - scrollList;
        int right = lx + w - 8;
        for (Object o : rows) {
            int rh = rowHeight(o);
            if (y + rh < ly || y > lb) { y += rh; continue; }
            if (o instanceof Title) {
                Title ti = (Title) o;
                String cnt = String.valueOf(ti.count);
                int cw = Draw.width(cnt);
                Draw.rect(lx + 9, y + 8, 4, 4, ti.color);
                Draw.heading(ti.text, lx + 18, y + 6, ti.color);
                int hw = Draw.headingWidth(ti.text);
                int lineX = lx + 18 + hw + 6, lineW = right - cw - 6 - lineX;
                if (lineW > 0) Draw.rect(lineX, y + 10, lineW, 1, LINE);
                Draw.text(cnt, right - cw, y + 6, MUTED);
            } else if (o instanceof SocialClient.Friend) {
                final SocialClient.Friend f = (SocialClient.Friend) o;
                boolean active = f.key.equals(selected);
                boolean hover = Draw.inside(mx, my, lx, y, w, rh) && my >= ly && my < lb;
                if (active) {
                    Draw.gradient(lx + 1, y, w - 2, rh, 0xFF2A2458, 0xFF1D1940, true);
                    Draw.gradient(lx + 1, y, 2, rh, ACCENT, PINK, true);
                } else if (hover) {
                    Draw.rect(lx + 1, y, w - 2, rh, 0xFF181530);
                }
                int col = stateColor(f);
                avatar(f.name, lx + 9, y + 4, 24, col, !f.online(), true);
                boolean preview = f.unread > 0 && !f.previewBody.isEmpty();
                int badge = f.unread > 0 ? 24 : 0;
                int tw = w - 46 - badge;
                Draw.text(Draw.fit(f.name, tw), lx + 40, y + 6, f.online() ? TEXT : MUTED);
                Draw.text(Draw.fit(preview ? f.previewBody : stateText(f), tw), lx + 40, y + 18, preview ? AMBER : col);
                if (f.unread > 0) {
                    String n = f.unread > 9 ? "9+" : String.valueOf(f.unread);
                    Draw.batchBegin();
                    Draw.chamfer(right - 16, y + 9, 17, 13, 3, ACCENT);
                    Draw.gradient(right - 14, y + 10, 13, 11, ACCENT_LIGHT, PINK, true);
                    Draw.batchEnd();
                    Draw.text(n, right - 7 - Draw.width(n) / 2, y + 12, 0xFFFFFFFF);
                }
                addClipped(lx, y, w, rh, ly + 1, lb - 1, new Runnable() { @Override public void run() { openChat(f.key); } });
            } else if (o instanceof SocialClient.Req) {
                final SocialClient.Req r = (SocialClient.Req) o;
                avatar(r.name, lx + 9, y + 3, 22, AMBER, false, false);
                Draw.text(Draw.fit(r.name, w - 98), lx + 38, y + 5, TEXT);
                Draw.text(Draw.fit(Lang.t("quiere ser tu amigo", "wants to be friends"), w - 98), lx + 38, y + 16, MUTED);
                smallBtn("+", right - 38, y + 7, 17, 16, mx, my, GREEN, new Runnable() {
                    @Override public void run() { SocialClient.accept(r.key, plain(Lang.t("Ahora eres amigo de ", "You are now friends with ") + r.name)); }
                });
                smallBtn("x", right - 18, y + 7, 17, 16, mx, my, RED, new Runnable() {
                    @Override public void run() { SocialClient.remove(r.key, plain(Lang.t("Solicitud rechazada", "Request declined"))); }
                });
            } else {
                final SocialClient.Req r = (SocialClient.Req) ((Object[]) o)[0];
                avatar(r.name, lx + 9, y + 3, 22, OFFLINE, true, false);
                Draw.text(Draw.fit(r.name, w - 70), lx + 38, y + 5, MUTED);
                Draw.text(Draw.fit(Lang.t("esperando respuesta...", "waiting for reply..."), w - 70), lx + 38, y + 16, FAINT);
                smallBtn("x", right - 18, y + 7, 17, 16, mx, my, RED, new Runnable() {
                    @Override public void run() { SocialClient.remove(r.key, plain(Lang.t("Solicitud cancelada", "Request cancelled"))); }
                });
            }
            y += rh;
        }
        Draw.endScissor();
        if (total > lh) { // barra de scroll fina
            int sbH = Math.max(14, (int) ((long) lh * lh / total));
            int sbY = ly + 2 + (int) ((long) (lh - 4 - sbH) * scrollList / Math.max(1, total - lh + 4));
            Draw.rect(lx + w - 3, sbY, 2, sbH, 0xFF6D4FC7);
        }
    }

    /** Titulo de grupo de la lista (Jugando ahora, En linea...), con su color y cuantos hay. */
    private static final class Title {
        final String text;
        final int count, color;
        Title(String text, int count, int color) { this.text = text; this.count = count; this.color = color; }
    }

    private int rowHeight(Object o) {
        if (o instanceof Title) return SEC_H;
        if (o instanceof SocialClient.Friend) return FRIEND_H;
        return REQ_H;
    }

    private void addClipped(int x, int y, int w, int h, int top, int bottom, Runnable r) {
        int hy = Math.max(y, top), hh = Math.min(y + h, bottom) - hy;
        if (hh > 0) hits.add(new Hit(x, hy, w, hh, r));
    }

    private void addGroup(List<Object> rows, SocialClient.Snapshot s, String state, String title, int color) {
        int n = 0;
        for (SocialClient.Friend f : s.friends) if (f.state.equals(state)) n++;
        if (n == 0) return;
        rows.add(new Title(title, n, color));
        for (SocialClient.Friend f : s.friends) if (f.state.equals(state)) rows.add(f);
    }

    private void drawWrapped(String text, int x, int y, int w, int color) {
        for (String l : Draw.wrap(text, w, 6)) { Draw.text(l, x, y, color); y += 10; }
    }

    // ------------------------------------------------------------ chat

    /** Parte el texto en lineas de maxW pixeles; una palabra mas ancha que la linea se corta por letras. */
    private List<String> wrapText(String s, int maxW) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
            while (Draw.width(word) > maxW && word.length() > 1) {
                int n = word.length();
                while (n > 1 && Draw.width(word.substring(0, n)) > maxW) n--;
                if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
                out.add(word.substring(0, n));
                word = word.substring(n);
            }
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && Draw.width(test) > maxW) { out.add(cur.toString()); cur.setLength(0); cur.append(word); }
            else { cur.setLength(0); cur.append(test); }
        }
        if (cur.length() > 0 || out.isEmpty()) out.add(cur.toString());
        return out;
    }

    private Bub layout(SocialClient.Msg m, int maxW) {
        int sig = (m.pending ? 1 : 0) | (m.failed ? 2 : 0) | (int) (m.ts / 60000L) << 2;
        Bub b = bubCache.get(m);
        if (b != null && b.maxW == maxW && b.sig == sig) return b;
        b = new Bub();
        b.maxW = maxW;
        b.sig = sig;
        b.lines = wrapText(m.body, maxW);
        int w = 0;
        for (String l : b.lines) w = Math.max(w, Draw.width(l));
        b.w = w + 14;
        b.h = b.lines.size() * 10 + 8;
        b.time = m.failed ? Lang.t("no enviado", "not sent") : m.pending ? Lang.t("enviando...", "sending...") : new java.text.SimpleDateFormat("HH:mm", Locale.ROOT).format(new java.util.Date(m.ts));
        bubCache.put(m, b);
        return b;
    }

    private void drawChat(int mx, int my) {
        int cx = cx(), top = bodyTop(), cw = cw(), bottom = bodyBottom(), ch = bottom - top;
        Draw.batchBegin();
        Draw.chamfer(cx, top, cw, ch, 3, PANEL);
        Draw.chamferBorder(cx, top, cw, ch, 3, LINE);
        Draw.batchEnd();
        final SocialClient.Friend f = selected == null ? null : SocialClient.find(selected);
        if (f == null) {
            Draw.logo(cx + cw / 2 - 24, top + ch / 2 - 58, 48);
            String title = Lang.t("Tus conversaciones", "Your conversations");
            Draw.heading(title, cx + cw / 2 - Draw.headingWidth(title) / 2, top + ch / 2 - 2, TEXT);
            Draw.centered(Lang.t("Elige a un amigo de la lista para chatear", "Pick a friend from the list to chat"), cx + cw / 2, top + ch / 2 + 13, MUTED);
            return;
        }
        // cabecera del chat
        Draw.batchBegin();
        Draw.gradient(cx + 1, top + 1, cw - 2, 39, 0xFF1E1B38, 0xFF171431, true);
        Draw.rect(cx + 1, top + 40, cw - 2, 1, LINE);
        Draw.batchEnd();
        int ring = stateColor(f);
        avatar(f.name, cx + 9, top + 7, 26, ring, !f.online(), true);
        Draw.text(f.name, cx + 44, top + 9, TEXT);
        Draw.text(Draw.fit(stateText(f), cw - 44 - 76), cx + 44, top + 22, ring);
        boolean armed = f.key.equals(confirmRemove);
        btn(2, armed ? Lang.t("¿Seguro?", "Sure?") : Lang.t("Quitar", "Remove"), cx + cw - 6 - 58, top + 11, 58, 18, mx, my, false, new Runnable() {
            @Override public void run() {
                if (f.key.equals(confirmRemove)) {
                    confirmRemove = null;
                    SocialClient.remove(f.key, plain(f.name + Lang.t(" ya no está en tu lista", " is no longer in your list")));
                    closeChat();
                } else {
                    confirmRemove = f.key;
                    say(Lang.t("Pulsa «Quitar» otra vez para confirmar (borra el chat)", "Press «Remove» again to confirm (deletes the chat)"), true);
                }
            }
        });

        // mensajes: se arman de abajo hacia arriba; la rueda los desplaza
        int logTop = top + 45, logBottom = inputY() - 6, logH = logBottom - logTop;
        int maxW = Math.max(60, (int) ((cw - 28) * 0.78f));
        int n = msgs.size();
        Bub[] bs = new Bub[n];
        int[] after = new int[n], ys = new int[n];
        boolean[] showTime = new boolean[n];
        int total = 0;
        for (int i = 0; i < n; i++) {
            SocialClient.Msg m = msgs.get(i);
            bs[i] = layout(m, maxW);
            SocialClient.Msg nx = i + 1 < n ? msgs.get(i + 1) : null;
            showTime[i] = nx == null || nx.mine != m.mine || nx.ts - m.ts > 120000L || m.failed || m.pending;
            after[i] = showTime[i] ? 13 : 3;
            total += bs[i].h + after[i];
        }
        scrollChat = Math.max(0, Math.min(scrollChat, Math.max(0, total - logH + 4)));
        Draw.scissor(cx + 1, logTop, cw - 2, logH);
        int y = logBottom - 2 + scrollChat - total;
        for (int i = 0; i < n; i++) { ys[i] = y; y += bs[i].h + after[i]; }
        // 1.o pasada: todas las burbujas visibles en un solo lote; 2.a: el texto
        Draw.batchBegin();
        for (int i = 0; i < n; i++) {
            Bub b = bs[i];
            if (ys[i] + b.h + after[i] <= logTop - 2 || ys[i] >= logBottom) continue;
            SocialClient.Msg m = msgs.get(i);
            int bx = m.mine ? cx + cw - 10 - b.w : cx + 10;
            if (m.mine) {
                Draw.chamfer(bx, ys[i], b.w, b.h, 3, m.failed ? 0xFF5A1F2C : 0xFF3B2A86);
                if (!m.failed) Draw.gradient(bx + 3, ys[i] + 1, b.w - 6, b.h - 2, m.pending ? 0xFF5B4AA8 : 0xFF6D4FC7, m.pending ? 0xFF7A3FA6 : 0xFF9D3FD0, true);
            } else {
                Draw.chamfer(bx, ys[i], b.w, b.h, 3, PANEL2);
                Draw.chamferBorder(bx, ys[i], b.w, b.h, 3, 0xFF2F2B52);
            }
        }
        Draw.batchEnd();
        for (int i = 0; i < n; i++) {
            Bub b = bs[i];
            if (ys[i] + b.h + after[i] <= logTop - 2 || ys[i] >= logBottom) continue;
            SocialClient.Msg m = msgs.get(i);
            int bx = m.mine ? cx + cw - 10 - b.w : cx + 10;
            for (int j = 0; j < b.lines.size(); j++) Draw.text(b.lines.get(j), bx + 7, ys[i] + 4 + j * 10, m.failed ? 0xFFFFB3BD : TEXT);
            if (showTime[i]) {
                int tx = m.mine ? bx + b.w - Draw.width(b.time) : bx;
                Draw.text(b.time, tx, ys[i] + b.h + 2, m.failed ? RED : FAINT);
            }
        }
        Draw.endScissor();
        if (n == 0) Draw.centered(loadingChat ? Lang.t("Cargando...", "Loading...") : Lang.t("Aún no hay mensajes. ¡Saluda!", "No messages yet. Say hi!"), cx + cw / 2, logTop + logH / 2 - 4, MUTED);
        else if (scrollChat > 0) Draw.centered(Lang.t("Desplazado: baja para ver lo más nuevo", "Scrolled: scroll down for the newest"), cx + cw / 2, logBottom - 10, 0xFF6D4FC7);

        // caja de mensaje
        boolean cf = chatField.isFocused();
        inputBox(inputX(), inputY(), inputW(), FIELD_H, cf);
        if (chatField.getText().isEmpty() && !cf) Draw.text(Lang.t("Escribe un mensaje...", "Type a message..."), inputX() + 8, inputY() + (FIELD_H - 8) / 2, FAINT);
        chatField.drawTextBox();
        btn(1, Lang.t("Enviar", "Send"), cx + cw - 6 - 56, inputY(), 56, FIELD_H, mx, my, true, new Runnable() { @Override public void run() { sendChat(); } });
    }

    // ------------------------------------------------------------ entrada

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        if (mx < cx() - 4) scrollList += d > 0 ? -18 : 18;
        else scrollChat += d > 0 ? 20 : -20;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        // clic en cualquier parte de la caja = enfocarla
        boolean inAdd = Draw.inside(mx, my, listX(), bodyTop(), lw() - 26, FIELD_H);
        boolean inChat = selected != null && Draw.inside(mx, my, inputX(), inputY(), inputW(), FIELD_H);
        if (button == 0) {
            if (inAdd) { addField.setFocused(true); chatField.setFocused(false); }
            else if (inChat) { chatField.setFocused(true); addField.setFocused(false); }
            else { addField.mouseClicked(mx, my, button); if (selected != null) chatField.mouseClicked(mx, my, button); }
        }
        if (button != 0) return;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(mx, my)) { h.click.run(); return; }
        }
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (code == Keyboard.KEY_ESCAPE || (code == Keyboard.KEY_TAB && shift)) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (code == Keyboard.KEY_TAB) {
            boolean toAdd = chatField.isFocused() || selected == null;
            addField.setFocused(toAdd);
            chatField.setFocused(!toAdd && selected != null);
            return;
        }
        if (addField.isFocused()) {
            if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) sendRequest();
            else addField.textboxKeyTyped(c, code);
        } else if (selected != null && chatField.isFocused()) {
            if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) sendChat();
            else chatField.textboxKeyTyped(c, code);
        }
    }
}
