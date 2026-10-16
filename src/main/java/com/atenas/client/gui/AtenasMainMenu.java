package com.atenas.client.gui;

import java.io.IOException;
import java.util.Random;

import org.lwjgl.input.Keyboard;

import com.atenas.client.AtenasClient;
import com.atenas.client.social.SocialClient;
import com.atenas.client.social.SocialEvents;
import com.atenas.client.social.SocialScreen;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;

/**
 * Menu principal de Atenas (oro sobre azul tinta): Un jugador, Multijugador, Opciones, Principal, Amigos, Cuenta y una X para cerrar.
 * Todo se calcula desde {@link #layout()}: el bloque (logo + botones) queda centrado y se compacta solo en pantallas chicas.
 */
public class AtenasMainMenu extends GuiScreen {

    private static final int STARS = 110;
    private static final int ID_SINGLE = 0, ID_MULTI = 1, ID_MODS = 2, ID_OPTIONS = 3, ID_CLOSE = 4, ID_FRIENDS = 5, ID_ACCOUNT = 6;
    private static final int FOOTER = 46; // franja inferior reservada (version, atajo, cuenta)

    private final float[] sx = new float[STARS];
    private final float[] sy = new float[STARS];
    private final float[] sv = new float[STARS];
    private final float[] ss = new float[STARS];

    // medidas del bloque central (las llena layout())
    private boolean compact;
    private int top, logo, bw, bh, gap, btnY;
    // la "ficha" de la cuenta, abajo a la derecha (clic = cambiar de cuenta)
    private int chipX, chipY, chipW, chipH;
    private MenuButton friendsBtn;

    public AtenasMainMenu() {
        Random r = new Random(1234L);
        for (int i = 0; i < STARS; i++) {
            sx[i] = r.nextFloat();
            sy[i] = r.nextFloat();
            sv[i] = 0.2f + r.nextFloat() * 0.8f;
            ss[i] = r.nextFloat();
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    /** El launcher espera esta linea en la consola del juego para cerrar su ventana de carga. */
    private static boolean readyAnnounced;

    /** Calcula tamanos y posiciones. Alto del bloque = marca (logo + nombre) + separacion + botones; se centra entre la X y la franja inferior. */
    private void layout() {
        for (int pass = 0; pass < 2; pass++) {
            compact = pass == 1;
            logo = compact ? 40 : 64;
            bw = 250;
            bh = compact ? 22 : 26;
            gap = compact ? 4 : 5;
            int brandH = compact ? 82 : 112;
            int sep = compact ? 12 : 24;
            int btnsH = 3 * bh + 2 * gap + 8 + 2 * (bh - 4) + 6;
            int total = brandH + sep + btnsH;
            int avail = height - 16 - FOOTER;
            top = Math.max(8, 16 + (avail - total) / 2);
            btnY = top + brandH + sep;
            if (total <= avail) break;
        }
    }

    @Override
    public void initGui() {
        buttonList.clear();
        layout();
        int bx = width / 2 - bw / 2;
        int sh = bh - 4;                 // los botones secundarios son un poco mas bajos
        int y2 = btnY + 3 * (bh + gap) - gap + 8;
        int half = (bw - 6) / 2;
        // Lo principal para jugar (espanol / ingles), el hub de Atenas aparte y la X para cerrar el cliente.
        buttonList.add(new MenuButton(ID_SINGLE, bx, btnY, bw, bh, "Un jugador", "Singleplayer", false));
        buttonList.add(new MenuButton(ID_MULTI, bx, btnY + (bh + gap), bw, bh, "Multijugador", "Multiplayer", false));
        buttonList.add(new MenuButton(ID_OPTIONS, bx, btnY + 2 * (bh + gap), bw, bh, "Opciones", "Options", false));
        buttonList.add(new MenuButton(ID_MODS, bx, y2, bw, sh, "Principal", "Mods y cosm\u00e9ticos", true));
        friendsBtn = new MenuButton(ID_FRIENDS, bx, y2 + sh + 6, half, sh, "Amigos", "Shift+Tab", true);
        buttonList.add(friendsBtn);
        buttonList.add(new MenuButton(ID_ACCOUNT, bx + half + 6, y2 + sh + 6, half, sh, "Cuenta", "Cambiar", true));
        buttonList.add(new CloseButton(ID_CLOSE, width - 32, 10, 22));
        SocialClient.sync(false, "");
        if (!readyAnnounced) {
            readyAnnounced = true;
            System.out.println("[Atenas] MENU_READY");
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        switch (b.id) {
            case ID_SINGLE: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
            case ID_MULTI: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
            case ID_OPTIONS: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
            case ID_FRIENDS: mc.displayGuiScreen(new SocialScreen(this)); break;
            case ID_ACCOUNT: mc.displayGuiScreen(new AccountScreen(this)); break;
            case ID_CLOSE: mc.shutdown(); break;
            default: mc.displayGuiScreen(new AtenasMenu(this)); break;
        }
    }

    /** Shift+Tab: en un menu no llega el evento de teclado del juego, asi que se atiende aqui. */
    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (code == Keyboard.KEY_TAB && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
            mc.displayGuiScreen(new SocialScreen(this));
            return;
        }
        super.keyTyped(c, code);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button == 0 && Draw.inside(mx, my, chipX, chipY, chipW, chipH)) {
            mc.displayGuiScreen(new AccountScreen(this));
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        long t = System.currentTimeMillis();
        Draw.gradient(0, 0, width, height, 0xFF080A14, 0xFF171C3C, true);

        // resplandor del horizonte y rejilla en perspectiva
        int hz = height - 90;
        Draw.gradient(0, hz - 40, width, 40, 0x008B5CF6, 0x268B5CF6, true);
        Draw.rect(0, hz, width, height - hz, 0xFF070913);
        Draw.rect(0, hz, width, 1, 0x808B5CF6);
        for (int i = 1; i < 7; i++) {
            float k = (float) Math.pow((i + (t % 2000) / 2000f) / 7f, 2.0);
            int yy = hz + (int) (k * (height - hz));
            Draw.rect(0, yy, width, 1, ((int) (60 * k) + 14 << 24) | 0x8B5CF6);
        }
        for (int i = -10; i <= 10; i++) {
            int bx = width / 2 + i * (width / 14);
            int ex = width / 2 + i * (width / 3);
            int steps = 12;
            for (int s = 0; s < steps; s++) {
                float a0 = s / (float) steps;
                int px = bx + (int) ((ex - bx) * a0);
                int py = hz + (int) ((height - hz) * a0);
                Draw.rect(px, py, 1, (height - hz) / steps + 1, ((int) (10 + 40 * a0) << 24) | 0x8B5CF6);
            }
        }

        for (int i = 0; i < STARS; i++) {
            float yy = (sy[i] * hz + t * sv[i] * 0.003f) % hz;
            int a = (int) (50 + 110 * (0.5 + 0.5 * Math.sin(t * 0.002 + i)));
            int size = ss[i] > 0.8f ? 2 : 1;
            Draw.rect((int) (sx[i] * width), (int) yy, size, size, (a << 24) | 0xFFFFFF);
        }

        // linea de escaneo
        int scan = (int) ((t / 14) % (height + 80)) - 40;
        Draw.gradient(0, scan, width, 14, 0x008B5CF6, 0x128B5CF6, true);

        // marca: logo con pulso, nombre y subtitulo
        float pulse = (float) (0.5 + 0.5 * Math.sin(t * 0.003));
        for (int g = 3; g >= 1; g--) {
            int gs = logo + g * 10;
            Draw.rect(width / 2 - gs / 2, top + logo / 2 - gs / 2, gs, gs, ((int) ((8 + 10 * pulse) / g) << 24) | 0x8B5CF6);
        }
        Draw.logo(width / 2 - logo / 2, top, logo);
        float nameScale = compact ? 2f : 3f;
        int nameY = top + logo + (compact ? 6 : 6);
        Draw.scaled("ATENAS", width / 2f, nameY, nameScale, Theme.ACCENT, true);
        int subY = nameY + (int) (8 * nameScale) + 4;
        Draw.scaled("C   L   I   E   N   T", width / 2f, subY, 1f, Theme.CYAN, true);
        Draw.rect(width / 2 - 60, subY + 12, 120, 1, Theme.ACCENT_DIM);
        Draw.rect(width / 2 - 8, subY + 11, 16, 3, Theme.VIOLET);

        // el texto de "Amigos" muestra cuantas solicitudes / mensajes nuevos hay
        if (friendsBtn != null) friendsBtn.displayString = SocialEvents.badgeLabel("Amigos");

        super.drawScreen(mx, my, pt);

        // greca dorada arriba y esquinas HUD (arriba a la derecha va la X; abajo a la derecha va la ficha de la cuenta)
        for (int gx = 0; gx + 12 <= width; gx += 12) {
            int ga = 0x558B5CF6;
            Draw.rect(gx, 3, 8, 1, ga); Draw.rect(gx, 3, 1, 6, ga); Draw.rect(gx, 8, 5, 1, ga); Draw.rect(gx + 4, 5, 1, 3, ga);
        }
        int c = 0xAA8B5CF6;
        Draw.rect(8, 14, 22, 1, c); Draw.rect(8, 14, 1, 22, c);
        Draw.rect(8, height - 9, 22, 1, c); Draw.rect(8, height - 30, 1, 22, c);

        // pie: version (izquierda), atajo (centro), cuenta (derecha)
        Draw.text("Atenas Client " + AtenasClient.VERSION + "  \u00b7  Minecraft 1.8.9", 18, height - 22, Theme.DIM);
        String fr = SocialEvents.badgeLabel("Shift+Tab  \u00b7  Amigos y chat");
        Draw.text(fr, width / 2 - Draw.width(fr) / 2, height - 22, Theme.DIM);
        drawAccountChip(mx, my);
    }

    /** Ficha de la cuenta: inicial del nick, nick y estado del servicio de amigos. Al pasar el mouse invita a cambiar de cuenta. */
    private void drawAccountChip(int mx, int my) {
        String user = mc.getSession() == null ? "" : mc.getSession().getUsername();
        boolean online = SocialClient.usable();
        chipH = 26;
        int textW = Math.max(Draw.width(user), Draw.width("\u25cf en l\u00ednea"));
        textW = Math.max(textW, Draw.width("Cambiar de cuenta"));
        chipW = 6 + 18 + 7 + textW + 10;
        chipX = width - 18 - chipW;
        chipY = height - 16 - chipH;
        boolean over = Draw.inside(mx, my, chipX, chipY, chipW, chipH);
        Draw.rect(chipX, chipY, chipW, chipH, over ? Theme.PANEL2 : 0xC00B0E1C);
        Draw.border(chipX, chipY, chipW, chipH, over ? Theme.VIOLET : 0xFF262A48);
        Draw.rect(chipX, chipY, 2, chipH, over ? Theme.ACCENT : Theme.ACCENT_DIM);
        Draw.rect(chipX + 6, chipY + 4, 18, 18, Theme.VIOLET);
        if (user.length() > 0) Draw.centered(user.substring(0, 1).toUpperCase(), chipX + 15, chipY + 9, Theme.INK);
        Draw.text(user, chipX + 31, chipY + 4, over ? Theme.ACCENT : Theme.TEXT);
        if (over) Draw.text("Cambiar de cuenta", chipX + 31, chipY + 14, Theme.VIOLET);
        else Draw.text(online ? "\u25cf en l\u00ednea" : "\u25cb desconectado", chipX + 31, chipY + 14, online ? Theme.GREEN : Theme.DIM);
    }
}
