package com.atenas.client.social;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.atenas.client.gui.Draw;
import com.atenas.client.gui.Theme;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

/** Amigos + chat (Shift+Tab, menu principal y menu de pausa). */
public class SocialScreen extends GuiScreen {

    private static String openChatKey; // para no avisar de mensajes del chat que ya estas viendo
    public static boolean isChattingWith(String key) { return key != null && key.equals(openChatKey); }

    private static final class Hit {
        final int x, y, w, h;
        final Runnable click;
        Hit(int x, int y, int w, int h, Runnable click) { this.x = x; this.y = y; this.w = w; this.h = h; this.click = click; }
        boolean contains(int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private static final int LIST_W = 150;

    private final GuiScreen parent;
    private final List<Hit> hits = new ArrayList<Hit>();
    private final List<SocialClient.Msg> msgs = new ArrayList<SocialClient.Msg>();
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

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(450, width - 12);
        ph = Math.min(260, height - 12);
        x0 = (width - pw) / 2;
        y0 = (height - ph) / 2;
        String keepAdd = addField == null ? "" : addField.getText();
        String keepChat = chatField == null ? "" : chatField.getText();
        // Cada campo usa EXACTAMENTE el rectangulo que dibuja drawList / drawChat (antes el del chat quedaba 10 px mas abajo y el texto salia de la caja)
        addField = field(1, x0 + 8, y0 + 30, LIST_W - 32, 16, 16);
        addField.setText(keepAdd);
        chatField = field(2, chatInputX(), chatInputY(), chatInputW(), 16, 300);
        chatField.setText(keepChat);
        chatField.setFocused(selected != null);
        if (SocialClient.snapshot() == null || SocialClient.sinceLastSync() > 3000) SocialClient.sync(mc.theWorld != null, "");
    }

    // caja de escribir mensaje (la usan initGui y drawChat)
    private int chatX() { return x0 + LIST_W + 16; }
    private int chatW() { return pw - LIST_W - 24; }
    private int chatInputX() { return chatX() + 6; }
    private int chatInputY() { return y0 + 30 + (ph - 38) - 22; }
    private int chatInputW() { return chatW() - 12 - 44; }

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
        lastId = 0;
        scrollChat = 0;
        confirmRemove = null;
        chatField.setFocused(true);
        addField.setFocused(false);
        loadChat(0);
    }

    private void closeChat() { selected = null; openChatKey = null; msgs.clear(); lastId = 0; }

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
                        // los mensajes que se estan enviando (pending) se quedan: antes se borraban aqui y, si la respuesta del envio
                        // llegaba despues, el mensaje desaparecia de la pantalla hasta volver a abrir el chat
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
        java.util.Collections.sort(msgs, new java.util.Comparator<SocialClient.Msg>() {
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
                        // El siguiente sondeo trae todo lo que falta, en orden.
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

    // ------------------------------------------------------------ dibujo

    private String stateText(SocialClient.Friend f) {
        if (f.playing()) return f.server.isEmpty() ? "Jugando" : "Jugando en " + f.server;
        return f.online() ? "En el launcher" : "Desconectado";
    }

    private int stateColor(SocialClient.Friend f) {
        return f.playing() ? 0xFF55FF99 : f.online() ? 0xFF55CCFF : Theme.OFF;
    }

    private void smallBtn(String label, int x, int y, int w, int mx, int my, int color, Runnable r) {
        boolean hov = Draw.inside(mx, my, x, y, w, 14);
        Draw.rect(x, y, w, 14, hov ? Theme.PANEL2 : Theme.PANEL);
        Draw.border(x, y, w, 14, hov ? color : Theme.BORDER);
        Draw.centered(label, x + w / 2, y + 3, hov ? color : Theme.TEXT);
        hits.add(new Hit(x, y, w, 14, r));
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        hits.clear();
        Draw.rect(0, 0, width, height, 0x90000000);
        Draw.rect(x0 - 2, y0 - 2, pw + 4, ph + 4, 0x40A78BFA);
        Draw.rect(x0, y0, pw, ph, Theme.BG);
        Draw.border(x0, y0, pw, ph, Theme.BORDER);
        Draw.gradient(x0, y0, pw, 2, Theme.ACCENT, 0x00A78BFA, false);

        // cabecera
        Draw.logo(x0 + 8, y0 + 5, 18);
        Draw.text("AMIGOS", x0 + 30, y0 + 6, Theme.ACCENT);
        String sub = SocialClient.usable() ? onlineCount() : stateLine();
        Draw.text(sub, x0 + 30, y0 + 16, Theme.DIM);
        String hint = "Shift+Tab / Esc para cerrar";
        Draw.text(hint, x0 + pw - Draw.width(hint) - 8, y0 + 8, Theme.DIM);

        drawList(mx, my);
        drawChat(mx, my);

        if (System.currentTimeMillis() - noticeAt < 5000 && !notice.isEmpty() && selected == null) {
            // el aviso se parte en lineas y se dibuja DENTRO del panel (antes se salia de la pantalla)
            java.util.List<String> ls = fontRendererObj.listFormattedStringToWidth(notice, pw - 32);
            int n = Math.min(2, ls.size());
            int bh = 7 + n * 10;
            int nx = x0 + 8, nw = pw - 16, ny = y0 + ph - 8 - bh;
            Draw.rect(nx, ny, nw, bh, 0xF0000000 | (noticeBad ? 0x4A1620 : 0x12361F));
            for (int i = 0; i < n; i++) {
                String line = (i == n - 1 && ls.size() > n) ? Draw.fit(ls.get(i) + "...", pw - 32) : ls.get(i);
                Draw.centered(line, x0 + pw / 2, ny + 4 + i * 10, noticeBad ? 0xFFFF7777 : 0xFF77FF99);
            }
        }
        super.drawScreen(mx, my, pt);
    }

    private String onlineCount() {
        SocialClient.Snapshot s = SocialClient.snapshot();
        int on = 0;
        for (SocialClient.Friend f : s.friends) if (f.online()) on++;
        return s.friends.isEmpty() ? "Sin amigos todavia" : on + " de " + s.friends.size() + " conectados";
    }

    private String stateLine() {
        String st = SocialClient.status();
        if ("noaccount".equals(st)) return "Inicia sesion para usar amigos";
        if ("disabled".equals(st)) return "Desactivado en ajustes";
        if ("error".equals(st)) return "Sin conexion";
        return "Conectando...";
    }

    private void drawList(int mx, int my) {
        int lx = x0 + 8, top = y0 + 52, lh = ph - 60, lw = LIST_W;
        // formulario de agregar
        Draw.rect(lx, y0 + 30, lw - 32, 16, Theme.PANEL);
        Draw.border(lx, y0 + 30, lw - 32, 16, addField.isFocused() ? Theme.ACCENT : Theme.BORDER);
        if (addField.getText().isEmpty() && !addField.isFocused()) Draw.text("Agregar amigo...", lx + 4, y0 + 34, Theme.OFF);
        addField.drawTextBox();
        boolean hov = Draw.inside(mx, my, lx + lw - 30, y0 + 30, 30, 16);
        Draw.rect(lx + lw - 30, y0 + 30, 30, 16, hov ? Theme.ACCENT : Theme.ACCENT_DIM);
        Draw.centered("+", lx + lw - 15, y0 + 34, Theme.INK);
        hits.add(new Hit(lx + lw - 30, y0 + 30, 30, 16, new Runnable() { @Override public void run() { sendRequest(); } }));

        Draw.rect(lx, top, lw, lh, Theme.PANEL);
        SocialClient.Snapshot s = SocialClient.snapshot();
        if (s == null || !SocialClient.usable()) {
            drawWrapped(stateLine() + ("error".equals(SocialClient.status()) ? ". " + SocialClient.error() : ""), lx + 8, top + 10, lw - 16, Theme.DIM);
            return;
        }
        List<Object> rows = new ArrayList<Object>();
        if (!s.incoming.isEmpty()) { rows.add("SOLICITUDES"); rows.addAll(s.incoming); }
        addGroup(rows, s, "playing", "JUGANDO AHORA");
        addGroup(rows, s, "online", "EN EL LAUNCHER");
        addGroup(rows, s, "offline", "DESCONECTADOS");
        if (!s.outgoing.isEmpty()) { rows.add("ENVIADAS"); for (SocialClient.Req r : s.outgoing) rows.add(new Object[] {r}); }
        if (rows.isEmpty()) {
            drawWrapped("Escribe el nick de un amigo arriba y envia una solicitud. Cuando la acepte podran chatear.", lx + 8, top + 10, lw - 16, Theme.DIM);
            return;
        }
        int total = 0;
        for (Object o : rows) total += o instanceof String ? 13 : 22;
        scrollList = Math.max(0, Math.min(scrollList, Math.max(0, total - lh)));
        Draw.scissor(lx, top, lw, lh);
        int y = top - scrollList;
        for (Object o : rows) {
            if (o instanceof String) {
                Draw.text((String) o, lx + 6, y + 4, Theme.ACCENT_DIM);
                y += 13;
                continue;
            }
            boolean vis = y + 22 > top && y < top + lh;
            if (o instanceof SocialClient.Friend) {
                final SocialClient.Friend f = (SocialClient.Friend) o;
                boolean sel = f.key.equals(selected);
                boolean h = Draw.inside(mx, my, lx, y, lw, 22) && my >= top && my < top + lh;
                if (sel) { Draw.rect(lx, y, lw, 22, Theme.PANEL2); Draw.rect(lx, y, 2, 22, Theme.ACCENT); }
                else if (h) Draw.rect(lx, y, lw, 22, Theme.HOVER);
                Draw.rect(lx + 8, y + 8, 6, 6, stateColor(f));
                Draw.text(trim(f.name, lw - 50), lx + 20, y + 3, f.online() ? Theme.TEXT : Theme.DIM);
                String line = f.unread > 0 && !f.previewBody.isEmpty() ? f.previewBody : stateText(f);
                Draw.text(trim(line, lw - 30), lx + 20, y + 12, f.unread > 0 ? 0xFFFFE08A : Theme.DIM);
                if (f.unread > 0) {
                    String n = f.unread > 9 ? "9+" : String.valueOf(f.unread);
                    Draw.rect(lx + lw - 18, y + 5, 12, 12, Theme.ACCENT);
                    Draw.centered(n, lx + lw - 12, y + 7, Theme.INK);
                }
                if (vis) addClipped(lx, y, lw, 22, top, top + lh, new Runnable() { @Override public void run() { openChat(f.key); } });
            } else if (o instanceof SocialClient.Req) {
                final SocialClient.Req r = (SocialClient.Req) o;
                Draw.text(trim(r.name, lw - 60), lx + 8, y + 7, Theme.TEXT);
                if (vis) {
                    smallBtn("✓", lx + lw - 42, y + 4, 18, mx, my, 0xFF55FF99, new Runnable() {
                        @Override public void run() { SocialClient.accept(r.key, plain("Ahora eres amigo de " + r.name)); }
                    });
                    smallBtn("✗", lx + lw - 22, y + 4, 18, mx, my, 0xFFFF7777, new Runnable() {
                        @Override public void run() { SocialClient.remove(r.key, plain("Solicitud rechazada")); }
                    });
                }
            } else {
                final SocialClient.Req r = (SocialClient.Req) ((Object[]) o)[0];
                Draw.text(trim(r.name, lw - 60), lx + 8, y + 3, Theme.DIM);
                Draw.text("esperando...", lx + 8, y + 12, Theme.OFF);
                if (vis) smallBtn("✗", lx + lw - 22, y + 4, 18, mx, my, 0xFFFF7777, new Runnable() {
                    @Override public void run() { SocialClient.remove(r.key, plain("Solicitud cancelada")); }
                });
            }
            y += 22;
        }
        Draw.endScissor();
    }

    private void addClipped(int x, int y, int w, int h, int top, int bottom, Runnable r) {
        int hy = Math.max(y, top), hh = Math.min(y + h, bottom) - hy;
        if (hh > 0) hits.add(new Hit(x, hy, w, hh, r));
    }

    private void addGroup(List<Object> rows, SocialClient.Snapshot s, String state, String title) {
        boolean any = false;
        for (SocialClient.Friend f : s.friends) {
            if (!f.state.equals(state)) continue;
            if (!any) { rows.add(title); any = true; }
            rows.add(f);
        }
    }

    private String trim(String s, int w) { return fontRendererObj.trimStringToWidth(s, Math.max(10, w)); }

    private void drawWrapped(String text, int x, int y, int w, int color) {
        for (Object l : fontRendererObj.listFormattedStringToWidth(text, w)) { Draw.text((String) l, x, y, color); y += 10; }
    }

    private void drawChat(int mx, int my) {
        int cx = chatX(), top = y0 + 30, cw = chatW(), ch = ph - 38;
        Draw.rect(cx, top, cw, ch, Theme.PANEL);
        final SocialClient.Friend f = SocialClient.find(selected);
        if (f == null) {
            Draw.logo(cx + cw / 2 - 16, top + ch / 2 - 40, 32);
            Draw.centered("Elige un amigo para chatear", cx + cw / 2, top + ch / 2 + 2, Theme.DIM);
            Draw.centered("Shift+Tab abre y cierra esta pantalla", cx + cw / 2, top + ch / 2 + 14, Theme.OFF);
            return;
        }
        // cabecera del chat
        Draw.rect(cx, top, cw, 22, Theme.PANEL2);
        Draw.rect(cx + 8, top + 8, 6, 6, stateColor(f));
        Draw.text(f.name, cx + 20, top + 3, Theme.TEXT);
        Draw.text(trim(stateText(f), cw - 90), cx + 20, top + 12, Theme.DIM);
        boolean armed = f.key.equals(confirmRemove);
        smallBtn(armed ? "Seguro?" : "Quitar", cx + cw - 50, top + 4, 44, mx, my, 0xFFFF7777, new Runnable() {
            @Override public void run() {
                if (f.key.equals(confirmRemove)) { confirmRemove = null; SocialClient.remove(f.key, plain(f.name + " ya no esta en tu lista")); closeChat(); }
                else { confirmRemove = f.key; say("Pulsa otra vez para quitar a " + f.name + " (borra la conversacion)", true); }
            }
        });

        // mensajes
        int logTop = top + 24, logH = ch - 24 - 24;
        int bw = cw - 24;
        List<Object[]> lines = new ArrayList<Object[]>(); // {texto, mine, hora?}
        for (SocialClient.Msg m : msgs) {
            List<?> wrapped = fontRendererObj.listFormattedStringToWidth(m.body, bw - 36);
            for (int i = 0; i < wrapped.size(); i++) {
                lines.add(new Object[] {wrapped.get(i), m.mine, i == wrapped.size() - 1 ? timeOf(m) : null, m.failed});
            }
        }
        int total = lines.size() * 10;
        scrollChat = Math.max(0, Math.min(scrollChat, Math.max(0, total - logH + 4)));
        Draw.scissor(cx, logTop, cw, logH);
        int y = logTop + logH - 4 + scrollChat - total;
        for (Object[] l : lines) {
            boolean mine = (Boolean) l[1];
            String t = (String) l[0];
            if (y > logTop - 10 && y < logTop + logH) {
                int tw = Draw.width(t);
                int x = mine ? cx + cw - 8 - tw : cx + 8;
                Draw.rect(x - 3, y - 1, tw + 6, 10, mine ? 0xFF3A3220 : Theme.PANEL2);
                Draw.text(t, x, y, mine ? Theme.ACCENT : Theme.TEXT);
                if (l[2] != null) {
                    String ts = (String) l[2];
                    boolean failed = (Boolean) l[3];
                    Draw.text(ts, mine ? x - Draw.width(ts) - 6 : x + tw + 6, y, failed ? 0xFFFF7777 : Theme.OFF);
                }
            }
            y += 10;
        }
        if (msgs.isEmpty()) Draw.centered(loadingChat ? "Cargando..." : "Aun no hay mensajes. Saluda!", cx + cw / 2, logTop + logH / 2, Theme.DIM);
        Draw.endScissor();

        // entrada
        int iy = chatInputY(), ix = chatInputX(), iw = chatInputW();
        Draw.rect(ix, iy, iw, 16, 0xFF090812);
        Draw.border(ix, iy, iw, 16, chatField.isFocused() ? Theme.ACCENT : Theme.BORDER);
        chatField.drawTextBox();
        if (chatField.getText().isEmpty() && !chatField.isFocused()) Draw.text("Escribe un mensaje...", ix + 4, iy + 4, Theme.OFF);
        smallBtn("Enviar", cx + cw - 46, iy + 1, 40, mx, my, Theme.ACCENT, new Runnable() { @Override public void run() { sendChat(); } });
        if (System.currentTimeMillis() - noticeAt < 5000 && !notice.isEmpty()) {
            Draw.centered(trim(notice, cw - 12), cx + cw / 2, iy - 11, noticeBad ? 0xFFFF7777 : 0xFF77FF99);
        }
    }

    private String timeOf(SocialClient.Msg m) {
        if (m.failed) return "no enviado";
        if (m.pending) return "...";
        return new java.text.SimpleDateFormat("HH:mm", Locale.ROOT).format(new java.util.Date(m.ts));
    }

    // ------------------------------------------------------------ entrada

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        if (mx < x0 + LIST_W + 12) scrollList += d > 0 ? -22 : 22;
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
