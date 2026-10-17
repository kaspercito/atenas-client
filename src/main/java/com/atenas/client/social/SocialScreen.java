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

    private static final int LIST_W = 168;
    private static final int HEAD_H = 28;
    private static final int ROW_H = 28, REQ_H = 26, SEC_H = 16;

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

    public SocialScreen(GuiScreen parent) { this.parent = parent; }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    // ------------------------------------------------------------ medidas (initGui y el dibujo usan las MISMAS)
    private int bodyTop() { return y0 + HEAD_H + 6; }
    private int bodyH() { return ph - HEAD_H - 14; }
    private int listX() { return x0 + 8; }
    private int chatX() { return x0 + LIST_W + 16; }
    private int chatW() { return pw - LIST_W - 24; }
    private int inputY() { return bodyTop() + bodyH() - 24; }
    private int inputX() { return chatX() + 6; }
    private int inputW() { return chatW() - 12 - 52; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(480, width - 12);
        ph = Math.min(290, height - 12);
        x0 = (width - pw) / 2;
        y0 = (height - ph) / 2;
        String keepAdd = addField == null ? "" : addField.getText();
        String keepChat = chatField == null ? "" : chatField.getText();
        addField = field(1, listX() + 20, bodyTop(), LIST_W - 20 - 28, 18, 16);
        addField.setText(keepAdd);
        chatField = field(2, inputX(), inputY(), inputW(), 18, 300);
        chatField.setText(keepChat);
        chatField.setFocused(selected != null);
        if (SocialClient.snapshot() == null || SocialClient.sinceLastSync() > 3000) SocialClient.sync(mc.theWorld != null, "");
    }

    /** Campo de texto dentro de la caja (bx, by, bw, bh): sin fondo propio, con el texto centrado en vertical y 4 px de aire a los lados. */
    private GuiTextField field(int id, int bx, int by, int bw, int bh, int max) {
        GuiTextField f = new GuiTextField(id, fontRendererObj, bx + 4, by + (bh - 8) / 2, bw - 8, 8);
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
        if (!SocialClient.validName(n)) { say("Nick no valido (3-16 letras, numeros o _).", true); return; }
        addField.setText("");
        SocialClient.add(n, new SocialClient.Callback<String>() {
            @Override public void done(String v, String err) {
                if (err != null) say(err, true);
                else say(v.startsWith("accepted") ? "Ahora eres amigo de " + v.substring(v.indexOf('|') + 1) : "Solicitud enviada a " + v.substring(v.indexOf('|') + 1), false);
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
        if (f.playing()) return f.server.isEmpty() ? "Jugando" : "Jugando en " + f.server;
        return f.online() ? "En el launcher" : "Desconectado";
    }

    private int stateColor(SocialClient.Friend f) {
        return f.playing() ? Theme.GREEN : f.online() ? Theme.CYAN : 0xFF5A5680;
    }

    /** Cara del jugador (o su inicial mientras llega) con marco del color de su estado y un punto de estado opcional. */
    private void avatar(String name, int x, int y, int s, int ring, boolean dim, boolean dot) {
        Draw.rect(x - 1, y - 1, s + 2, s + 2, ring);
        if (!Faces.draw(name, x, y, s, 1f, false)) {
            Draw.rect(x, y, s, s, 0xFF2A2550);
            String ini = name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
            Draw.centered(ini, x + s / 2, y + (s - 8) / 2, Theme.ACCENT);
        }
        if (dim) Draw.rect(x, y, s, s, 0x88090812);
        if (dot) {
            Draw.rect(x + s - 5, y + s - 5, 7, 7, Theme.PANEL);
            Draw.rect(x + s - 4, y + s - 4, 5, 5, ring);
        }
    }

    private void smallBtn(String label, int x, int y, int w, int h, int mx, int my, int color, Runnable r) {
        boolean hov = Draw.inside(mx, my, x, y, w, h);
        Draw.card(x, y, w, h, hov ? Theme.PANEL2 : Theme.PANEL, hov ? color : Theme.BORDER);
        Draw.centered(label, x + w / 2, y + (h - 8) / 2, hov ? color : Theme.TEXT);
        hits.add(new Hit(x, y, w, h, r));
    }

    private void primaryBtn(String label, int x, int y, int w, int h, int mx, int my, Runnable r) {
        boolean hov = Draw.inside(mx, my, x, y, w, h);
        Draw.chamfer(x, y, w, h, 2, 0xFF2A1A5E);
        Draw.gradient(x + 1, y + 1, w - 2, h - 2, hov ? Theme.ACCENT : Theme.VIOLET, hov ? Theme.PINK : Theme.MAGENTA, false);
        Draw.chamferBorder(x, y, w, h, 2, hov ? 0xFFFFFFFF : Theme.ACCENT);
        Draw.centered(label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
        hits.add(new Hit(x, y, w, h, r));
    }

    private void inputBox(int x, int y, int w, int h, boolean focus) {
        Draw.chamfer(x, y, w, h, 2, 0xFF0B0A1C);
        Draw.chamferBorder(x, y, w, h, 2, focus ? Theme.ACCENT : Theme.BORDER);
        if (focus) Draw.gradient(x + 3, y + h - 1, w - 6, 1, Theme.VIOLET, Theme.PINK, false);
    }

    // ------------------------------------------------------------ dibujo

    @Override
    public void drawScreen(int mx, int my, float pt) {
        hits.clear();
        Draw.rect(0, 0, width, height, 0xA0000000);
        // resplandor + panel
        Draw.rect(x0 - 3, y0 - 3, pw + 6, ph + 6, 0x30A78BFA);
        Draw.rect(x0 - 1, y0 - 1, pw + 2, ph + 2, 0x55A78BFA);
        Draw.rect(x0, y0, pw, ph, Theme.BG);
        Draw.border(x0, y0, pw, ph, Theme.BORDER);

        // cabecera
        Draw.gradient(x0 + 1, y0 + 1, pw - 2, HEAD_H - 1, 0xFF1B1740, 0xFF0C0A1A, true);
        Draw.gradient(x0, y0, pw / 2, 2, Theme.VIOLET, Theme.MAGENTA, false);
        Draw.gradient(x0 + pw / 2, y0, pw - pw / 2, 2, Theme.MAGENTA, Theme.PINK, false);
        Draw.rect(x0, y0 + HEAD_H, pw, 1, Theme.BORDER);
        Draw.logo(x0 + 9, y0 + 6, 18);
        Draw.heading("Amigos", x0 + 33, y0 + 5, Theme.TEXT);
        if (SocialClient.usable()) {
            int on = onlineNow();
            Draw.rect(x0 + 34, y0 + 18, 5, 5, on > 0 ? Theme.GREEN : 0xFF5A5680);
            Draw.text(onlineCount(), x0 + 43, y0 + 17, Theme.DIM);
        } else {
            Draw.text(stateLine(), x0 + 34, y0 + 17, Theme.DIM);
        }
        smallBtn("x", x0 + pw - 22, y0 + 7, 14, 14, mx, my, Theme.RED, new Runnable() {
            @Override public void run() { mc.displayGuiScreen(parent); }
        });
        String hint = "Shift+Tab";
        Draw.text(hint, x0 + pw - 28 - Draw.width(hint), y0 + 10, Theme.OFF);

        drawList(mx, my);
        drawChat(mx, my);

        if (System.currentTimeMillis() - noticeAt < 5000 && !notice.isEmpty() && selected == null) {
            List<String> ls = Draw.wrap(notice, pw - 40, 2);
            int bh = 8 + ls.size() * 10;
            int nx = x0 + 8, nw = pw - 16, ny = y0 + ph - 8 - bh;
            Draw.rect(nx, ny, nw, bh, noticeBad ? 0xF04A1620 : 0xF012361F);
            Draw.rect(nx, ny, 2, bh, noticeBad ? Theme.RED : Theme.GREEN);
            for (int i = 0; i < ls.size(); i++) Draw.centered(ls.get(i), x0 + pw / 2, ny + 4 + i * 10, noticeBad ? 0xFFFF9AA8 : 0xFF86F0B8);
        }
        super.drawScreen(mx, my, pt);
    }

    private int onlineNow() {
        SocialClient.Snapshot s = SocialClient.snapshot();
        int on = 0;
        if (s != null) for (SocialClient.Friend f : s.friends) if (f.online()) on++;
        return on;
    }

    private String onlineCount() {
        SocialClient.Snapshot s = SocialClient.snapshot();
        return s.friends.isEmpty() ? "Sin amigos todavia" : onlineNow() + " de " + s.friends.size() + " conectados";
    }

    private String stateLine() {
        String st = SocialClient.status();
        if ("noaccount".equals(st)) return "Inicia sesion para usar amigos";
        if ("disabled".equals(st)) return "Desactivado en ajustes";
        if ("error".equals(st)) return "Sin conexion";
        return "Conectando...";
    }

    private void drawList(int mx, int my) {
        int lx = listX(), top = bodyTop(), lw = LIST_W;
        // barra de "agregar amigo": icono + caja + boton
        Draw.rect(lx, top, 18, 18, Theme.PANEL2);
        Draw.border(lx, top, 18, 18, Theme.BORDER);
        Draw.centered("+", lx + 9, top + 5, Theme.ACCENT);
        inputBox(lx + 20, top, lw - 20 - 28, 18, addField.isFocused());
        if (addField.getText().isEmpty() && !addField.isFocused()) Draw.text("Agregar amigo...", lx + 24, top + 5, Theme.OFF);
        addField.drawTextBox();
        primaryBtn("\u2192", lx + lw - 26, top, 26, 18, mx, my, new Runnable() { @Override public void run() { sendRequest(); } });

        int ly = top + 24, lh = bodyH() - 24;
        Draw.rect(lx, ly, lw, lh, Theme.PANEL);
        Draw.border(lx, ly, lw, lh, Theme.BORDER);
        SocialClient.Snapshot s = SocialClient.snapshot();
        if (s == null || !SocialClient.usable()) {
            drawWrapped(stateLine() + ("error".equals(SocialClient.status()) ? ". " + SocialClient.error() : ""), lx + 10, ly + 12, lw - 20, Theme.DIM);
            return;
        }
        List<Object> rows = new ArrayList<Object>();
        if (!s.incoming.isEmpty()) { rows.add("SOLICITUDES  " + s.incoming.size()); rows.addAll(s.incoming); }
        addGroup(rows, s, "playing", "JUGANDO AHORA");
        addGroup(rows, s, "online", "EN EL LAUNCHER");
        addGroup(rows, s, "offline", "DESCONECTADOS");
        if (!s.outgoing.isEmpty()) { rows.add("ENVIADAS  " + s.outgoing.size()); for (SocialClient.Req r : s.outgoing) rows.add(new Object[] {r}); }
        if (rows.isEmpty()) {
            Draw.logo(lx + lw / 2 - 12, ly + lh / 2 - 44, 24);
            Draw.centered("Aun no tienes amigos", lx + lw / 2, ly + lh / 2 - 12, Theme.TEXT);
            List<String> ls = Draw.wrap("Escribe su nick arriba y envia una solicitud.", lw - 24, 3);
            for (int i = 0; i < ls.size(); i++) Draw.centered(ls.get(i), lx + lw / 2, ly + lh / 2 + 2 + i * 10, Theme.DIM);
            return;
        }
        int total = 0;
        for (Object o : rows) total += rowHeight(o);
        scrollList = Math.max(0, Math.min(scrollList, Math.max(0, total - lh + 4)));
        Draw.scissor(lx + 1, ly + 1, lw - 2, lh - 2);
        int y = ly + 2 - scrollList;
        for (Object o : rows) {
            int rh = rowHeight(o);
            if (y + rh < ly || y > ly + lh) { y += rh; continue; }
            if (o instanceof String) {
                String t = (String) o;
                Draw.text(t, lx + 9, y + 6, Theme.ACCENT_DIM);
                int tw = Draw.width(t);
                Draw.rect(lx + 9 + tw + 6, y + 10, lw - 9 - tw - 6 - 8, 1, Theme.BORDER);
            } else if (o instanceof SocialClient.Friend) {
                final SocialClient.Friend f = (SocialClient.Friend) o;
                boolean sel = f.key.equals(selected);
                boolean h = Draw.inside(mx, my, lx, y, lw, rh) && my >= ly && my < ly + lh;
                if (sel) {
                    Draw.gradient(lx + 1, y, lw - 2, rh, 0x558B5CF6, 0x108B5CF6, false);
                    Draw.gradient(lx + 1, y, 2, rh, Theme.VIOLET, Theme.PINK, true);
                } else if (h) {
                    Draw.rect(lx + 1, y, lw - 2, rh, Theme.HOVER);
                }
                avatar(f.name, lx + 9, y + 4, 20, stateColor(f), !f.online(), true);
                int nx = lx + 36;
                int badgeW = f.unread > 0 ? 18 : 0;
                Draw.text(Draw.fit(f.name, lw - 44 - badgeW), nx, y + 5, f.online() ? Theme.TEXT : Theme.DIM);
                boolean preview = f.unread > 0 && !f.previewBody.isEmpty();
                Draw.text(Draw.fit(preview ? f.previewBody : stateText(f), lw - 44 - badgeW), nx, y + 16, preview ? Theme.AMBER : stateColor(f));
                if (f.unread > 0) {
                    String n = f.unread > 9 ? "9+" : String.valueOf(f.unread);
                    int bx = lx + lw - 24, by = y + 8;
                    Draw.gradient(bx, by, 16, 12, Theme.VIOLET, Theme.PINK, false);
                    Draw.centered(n, bx + 8, by + 2, 0xFFFFFFFF);
                }
                addClipped(lx, y, lw, rh, ly + 1, ly + lh - 1, new Runnable() { @Override public void run() { openChat(f.key); } });
            } else if (o instanceof SocialClient.Req) {
                final SocialClient.Req r = (SocialClient.Req) o;
                avatar(r.name, lx + 9, y + 3, 20, Theme.AMBER, false, false);
                Draw.text(Draw.fit(r.name, lw - 100), lx + 36, y + 3, Theme.TEXT);
                Draw.text("quiere ser tu amigo", lx + 36, y + 14, Theme.DIM);
                int by = y + 6;
                smallBtn("+", lx + lw - 42, by, 16, 14, mx, my, Theme.GREEN, new Runnable() {
                    @Override public void run() { SocialClient.accept(r.key, plain("Ahora eres amigo de " + r.name)); }
                });
                smallBtn("x", lx + lw - 24, by, 16, 14, mx, my, Theme.RED, new Runnable() {
                    @Override public void run() { SocialClient.remove(r.key, plain("Solicitud rechazada")); }
                });
            } else {
                final SocialClient.Req r = (SocialClient.Req) ((Object[]) o)[0];
                avatar(r.name, lx + 9, y + 3, 20, 0xFF5A5680, true, false);
                Draw.text(Draw.fit(r.name, lw - 80), lx + 36, y + 3, Theme.DIM);
                Draw.text("esperando respuesta...", lx + 36, y + 14, Theme.OFF);
                smallBtn("x", lx + lw - 24, y + 6, 16, 14, mx, my, Theme.RED, new Runnable() {
                    @Override public void run() { SocialClient.remove(r.key, plain("Solicitud cancelada")); }
                });
            }
            y += rh;
        }
        Draw.endScissor();
        // barra de scroll fina
        if (total > lh) {
            int sbH = Math.max(14, (int) ((long) lh * lh / total));
            int sbY = ly + 2 + (int) ((long) (lh - 4 - sbH) * scrollList / Math.max(1, total - lh + 4));
            Draw.rect(lx + lw - 3, sbY, 2, sbH, Theme.ACCENT_DIM);
        }
    }

    private int rowHeight(Object o) {
        if (o instanceof String) return SEC_H;
        if (o instanceof SocialClient.Friend) return ROW_H;
        return REQ_H;
    }

    private void addClipped(int x, int y, int w, int h, int top, int bottom, Runnable r) {
        int hy = Math.max(y, top), hh = Math.min(y + h, bottom) - hy;
        if (hh > 0) hits.add(new Hit(x, hy, w, hh, r));
    }

    private void addGroup(List<Object> rows, SocialClient.Snapshot s, String state, String title) {
        int n = 0;
        for (SocialClient.Friend f : s.friends) if (f.state.equals(state)) n++;
        if (n == 0) return;
        rows.add(title + "  " + n);
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
        b.w = w + 12;
        b.h = b.lines.size() * 10 + 6;
        b.time = m.failed ? "no enviado" : m.pending ? "enviando..." : new java.text.SimpleDateFormat("HH:mm", Locale.ROOT).format(new java.util.Date(m.ts));
        bubCache.put(m, b);
        return b;
    }

    private void drawChat(int mx, int my) {
        int cx = chatX(), top = bodyTop(), cw = chatW(), ch = bodyH();
        Draw.rect(cx, top, cw, ch, Theme.PANEL);
        Draw.border(cx, top, cw, ch, Theme.BORDER);
        final SocialClient.Friend f = SocialClient.find(selected);
        if (f == null) {
            Draw.logo(cx + cw / 2 - 20, top + ch / 2 - 52, 40);
            Draw.centered("Tus conversaciones", cx + cw / 2, top + ch / 2 - 2, Theme.TEXT);
            Draw.centered("Elige a un amigo de la lista para chatear", cx + cw / 2, top + ch / 2 + 11, Theme.DIM);
            return;
        }
        // cabecera del chat
        Draw.gradient(cx + 1, top + 1, cw - 2, 30, 0xFF1E1B38, 0xFF171431, true);
        Draw.rect(cx + 1, top + 31, cw - 2, 1, Theme.BORDER);
        avatar(f.name, cx + 8, top + 5, 22, stateColor(f), !f.online(), true);
        Draw.text(f.name, cx + 38, top + 6, Theme.TEXT);
        Draw.text(Draw.fit(stateText(f), cw - 130), cx + 38, top + 18, stateColor(f));
        boolean armed = f.key.equals(confirmRemove);
        smallBtn(armed ? "Seguro?" : "Quitar", cx + cw - 56, top + 9, 48, 14, mx, my, Theme.RED, new Runnable() {
            @Override public void run() {
                if (f.key.equals(confirmRemove)) { confirmRemove = null; SocialClient.remove(f.key, plain(f.name + " ya no esta en tu lista")); closeChat(); }
                else { confirmRemove = f.key; say("Pulsa otra vez para quitar a " + f.name + " (borra la conversacion)", true); }
            }
        });

        // mensajes
        int logTop = top + 34, logBottom = inputY() - 6, logH = logBottom - logTop;
        int maxW = Math.max(60, (int) ((cw - 24) * 0.78f));
        int n = msgs.size();
        Bub[] bs = new Bub[n];
        int[] after = new int[n];
        boolean[] showTime = new boolean[n];
        int total = 0;
        for (int i = 0; i < n; i++) {
            SocialClient.Msg m = msgs.get(i);
            bs[i] = layout(m, maxW);
            SocialClient.Msg nx = i + 1 < n ? msgs.get(i + 1) : null;
            showTime[i] = nx == null || nx.mine != m.mine || nx.ts - m.ts > 120000L || m.failed || m.pending;
            after[i] = showTime[i] ? 12 : 3;
            total += bs[i].h + after[i];
        }
        scrollChat = Math.max(0, Math.min(scrollChat, Math.max(0, total - logH + 4)));
        Draw.scissor(cx + 1, logTop, cw - 2, logH);
        int y = logTop + logH - 2 + scrollChat - total;
        for (int i = 0; i < n; i++) {
            SocialClient.Msg m = msgs.get(i);
            Bub b = bs[i];
            if (y + b.h + after[i] > logTop - 2 && y < logTop + logH) {
                int bx = m.mine ? cx + cw - 10 - b.w : cx + 10;
                if (m.mine) {
                    int base = m.failed ? 0xFF5A1F2C : 0xFF3B2A86;
                    Draw.chamfer(bx, y, b.w, b.h, 2, base);
                    if (!m.failed) Draw.gradient(bx + 2, y + 1, b.w - 4, b.h - 2, m.pending ? 0xFF5B4AA8 : 0xFF6D4FC7, m.pending ? 0xFF7A3FA6 : 0xFF9D3FD0, false);
                } else {
                    Draw.chamfer(bx, y, b.w, b.h, 2, Theme.PANEL2);
                    Draw.chamferBorder(bx, y, b.w, b.h, 2, 0xFF2F2B52);
                }
                for (int j = 0; j < b.lines.size(); j++) Draw.text(b.lines.get(j), bx + 6, y + 3 + j * 10, m.failed ? 0xFFFFB3BD : Theme.TEXT);
                if (showTime[i]) {
                    int tx = m.mine ? bx + b.w - Draw.width(b.time) : bx;
                    Draw.text(b.time, tx, y + b.h + 2, m.failed ? Theme.RED : 0xFF6E6A8F);
                }
            }
            y += b.h + after[i];
        }
        if (msgs.isEmpty()) {
            Draw.centered(loadingChat ? "Cargando..." : "Aun no hay mensajes. Saluda!", cx + cw / 2, logTop + logH / 2 - 4, Theme.DIM);
        }
        Draw.endScissor();
        if (total > logH && scrollChat > 0) Draw.centered("Desplazado: baja para ver lo mas nuevo", cx + cw / 2, logBottom - 10, Theme.ACCENT_DIM);

        // entrada
        int iy = inputY(), ix = inputX(), iw = inputW();
        inputBox(ix, iy, iw, 18, chatField.isFocused());
        chatField.drawTextBox();
        if (chatField.getText().isEmpty() && !chatField.isFocused()) Draw.text("Escribe un mensaje...", ix + 5, iy + 5, Theme.OFF);
        primaryBtn("Enviar", cx + cw - 6 - 46, iy, 46, 18, mx, my, new Runnable() { @Override public void run() { sendChat(); } });
        if (System.currentTimeMillis() - noticeAt < 5000 && !notice.isEmpty()) {
            Draw.centered(Draw.fit(notice, cw - 16), cx + cw / 2, iy - 15, noticeBad ? 0xFFFF9AA8 : 0xFF86F0B8);
        }
    }

    // ------------------------------------------------------------ entrada

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        if (mx < chatX()) scrollList += d > 0 ? -18 : 18;
        else scrollChat += d > 0 ? 20 : -20;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        addField.mouseClicked(mx, my, button);
        if (selected != null) chatField.mouseClicked(mx, my, button);
        else chatField.setFocused(false);
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
