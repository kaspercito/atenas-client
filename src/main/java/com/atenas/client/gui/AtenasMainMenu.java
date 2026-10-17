package com.atenas.client.gui;

import java.io.IOException;
import java.util.Random;

import org.lwjgl.input.Keyboard;

import com.atenas.client.AtenasClient;
import com.atenas.client.account.Accounts;
import com.atenas.client.core.Lang;
import com.atenas.client.social.Faces;
import com.atenas.client.social.SocialClient;
import com.atenas.client.social.SocialEvents;
import com.atenas.client.social.SocialScreen;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;

/**
 * Menu principal de Atenas (violeta sobre azul noche): Un jugador, Multijugador, Opciones, Amigos, Cuenta y una X para cerrar. Un solo idioma, el del juego.
 * Todo se calcula desde {@link #layout()}: el bloque (logo + botones) queda centrado y se compacta solo en pantallas chicas.
 */
public class AtenasMainMenu extends GuiScreen {

    private static final int STARS = 110;
    private static final int ID_SINGLE = 0, ID_MULTI = 1, ID_MODS = 2, ID_OPTIONS = 3, ID_CLOSE = 4, ID_FRIENDS = 5, ID_ACCOUNT = 6;
    private static final int FOOTER = 46;        // franja inferior reservada en pantallas normales (version, atajo, cuenta)
    private static final int FOOTER_TIGHT = 26;  // y en ventanas bajitas (todo en una sola linea)
    private static final int TIGHT_BELOW = 300;  // alto (en pixeles de GUI) por debajo del cual se usa la franja chica

    private final float[] sx = new float[STARS];
    private final float[] sy = new float[STARS];
    private final float[] sv = new float[STARS];
    private final float[] ss = new float[STARS];

    // medidas del bloque central (las llena layout())
    private boolean tight;           // ventana baja: franja inferior de una linea y ficha de cuenta de una linea
    private int tier;                // 0 = completo ... 3 = minimo (sin logo)
    private int top, logo, bw, bh, sh, gap, btnY;
    private float nameScale;
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

    // niveles de tamano: {logo, escala del nombre, alto de la marca, separacion, alto de boton, hueco entre botones, recorte de los botones chicos} (abajo: una sola fila chica con Amigos y Cuenta)
    private static final float[][] TIERS = {
        {64, 3f, 112, 24, 26, 5, 4},
        {40, 2f, 82, 12, 22, 4, 4},
        {28, 1.5f, 64, 8, 20, 3, 3},
        {0, 1.5f, 32, 6, 18, 2, 3},
    };

    /**
     * Calcula tamanos y posiciones. Alto del bloque = marca (logo + nombre) + separacion + botones; se centra entre la X y la
     * franja inferior. Se prueba el nivel mas grande que quepa; asi nada se solapa ni en ventanas muy bajas.
     */
    private void layout() {
        tight = height < TIGHT_BELOW;
        int avail = height - 14 - (tight ? FOOTER_TIGHT : FOOTER);
        int total = 0, brandH = 0, sep = 0;
        for (tier = 0; tier < TIERS.length; tier++) {
            float[] t = TIERS[tier];
            logo = (int) t[0];
            nameScale = t[1];
            brandH = (int) t[2];
            sep = (int) t[3];
            bh = (int) t[4];
            gap = (int) t[5];
            sh = bh - (int) t[6];
            bw = 250;
            int btnsH = 3 * bh + 2 * gap + 8 + sh;
            total = brandH + sep + btnsH;
            if (total <= avail || tier == TIERS.length - 1) break;
        }
        top = Math.max(8, 14 + (avail - total) / 2);
        btnY = top + brandH + sep;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        layout();
        int bx = width / 2 - bw / 2;
        int y2 = btnY + 3 * (bh + gap) - gap + 8;
        int half = (bw - 6) / 2;
        // Un solo idioma: el del juego (ver Lang). Los tres botones grandes llevan solo su nombre; los chicos, una pista corta.
        buttonList.add(new MenuButton(ID_SINGLE, bx, btnY, bw, bh, Lang.t("Un jugador", "Singleplayer"), null, false));
        buttonList.add(new MenuButton(ID_MULTI, bx, btnY + (bh + gap), bw, bh, Lang.t("Multijugador", "Multiplayer"), null, false));
        buttonList.add(new MenuButton(ID_OPTIONS, bx, btnY + 2 * (bh + gap), bw, bh, Lang.t("Opciones", "Options"), null, false));
        friendsBtn = new MenuButton(ID_FRIENDS, bx, y2, half, sh, Lang.t("Amigos", "Friends"), "Shift+Tab", true);
        buttonList.add(friendsBtn);
        buttonList.add(new MenuButton(ID_ACCOUNT, bx + half + 6, y2, half, sh, Lang.t("Cuenta", "Account"), Lang.t("Cambiar", "Switch"), true));
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
        int nameY = top;
        if (logo > 0) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(t * 0.003));
            for (int g = 3; g >= 1; g--) {
                int gs = logo + g * 10;
                Draw.rect(width / 2 - gs / 2, top + logo / 2 - gs / 2, gs, gs, ((int) ((8 + 10 * pulse) / g) << 24) | 0x8B5CF6);
            }
            Draw.logo(width / 2 - logo / 2, top, logo);
            nameY = top + logo + 6;
        }
        Draw.scaled("ATENAS", width / 2f, nameY, nameScale, Theme.ACCENT, true);
        int subY = nameY + (int) (8 * nameScale) + 4;
        Draw.scaled("C   L   I   E   N   T", width / 2f, subY, 1f, Theme.CYAN, true);
        Draw.rect(width / 2 - 60, subY + 12, 120, 1, Theme.ACCENT_DIM);
        Draw.rect(width / 2 - 8, subY + 11, 16, 3, Theme.VIOLET);

        // el texto de "Amigos" muestra cuantas solicitudes / mensajes nuevos hay
        if (friendsBtn != null) friendsBtn.displayString = SocialEvents.badgeLabel(Lang.t("Amigos", "Friends"));

        super.drawScreen(mx, my, pt);

        // greca dorada arriba y esquinas HUD (arriba a la derecha va la X; abajo a la derecha va la ficha de la cuenta)
        for (int gx = 0; gx + 12 <= width; gx += 12) {
            int ga = 0x558B5CF6;
            Draw.rect(gx, 3, 8, 1, ga); Draw.rect(gx, 3, 1, 6, ga); Draw.rect(gx, 8, 5, 1, ga); Draw.rect(gx + 4, 5, 1, 3, ga);
        }
        int c = 0xAA8B5CF6;
        Draw.rect(8, 14, 22, 1, c); Draw.rect(8, 14, 1, 22, c);
        Draw.rect(8, height - 9, 22, 1, c); Draw.rect(8, height - 30, 1, 22, c);

        // pie: version (izquierda), atajo (centro), cuenta (derecha). Cada pieza se dibuja solo si cabe sin pisar a la otra.
        int fy = tight ? height - 20 : height - 22;
        int leftX = 18;
        String ver = "Atenas Client " + AtenasClient.VERSION + "  \u00b7  Minecraft 1.8.9";
        int chipLeft = drawAccountChip(mx, my);
        int verEnd = leftX + Draw.width(ver);
        if (verEnd + 8 <= chipLeft) Draw.text(ver, leftX, fy, Theme.DIM);
        else {
            String shortVer = "Atenas Client " + AtenasClient.VERSION;
            if (leftX + Draw.width(shortVer) + 8 <= chipLeft) { Draw.text(shortVer, leftX, fy, Theme.DIM); verEnd = leftX + Draw.width(shortVer); }
            else verEnd = leftX;
        }
        String fr = SocialEvents.badgeLabel(Lang.t("Shift+Tab  \u00b7  Amigos y chat", "Shift+Tab  \u00b7  Friends and chat"));
        int frW = Draw.width(fr), frX = width / 2 - frW / 2;
        if (frX > verEnd + 14 && frX + frW + 14 < chipLeft) Draw.text(fr, frX, fy, Theme.DIM);
    }

    /**
     * Ficha de la cuenta: inicial del nick, nick y estado del servicio de amigos. Al pasar el mouse invita a cambiar de cuenta.
     * En ventanas bajas es de una sola linea. Devuelve el borde izquierdo de la ficha (para que el pie no la pise).
     */
    private int drawAccountChip(int mx, int my) {
        String user = mc.getSession() == null ? "" : mc.getSession().getUsername();
        boolean online = SocialClient.usable();
        boolean offlineAcc = Accounts.currentIsOffline(); // sin conexion = no premium: se dibuja Steve
        boolean over;
        if (tight) {
            chipH = 16;
            int nameW = Draw.width(user);
            chipW = 4 + 12 + 6 + nameW + 8 + 5 + 8;
            chipX = width - 18 - chipW;
            chipY = height - 8 - chipH - 2;
            over = Draw.inside(mx, my, chipX, chipY, chipW, chipH);
            Draw.rect(chipX, chipY, chipW, chipH, over ? Theme.PANEL2 : 0xC00B0E1C);
            Draw.border(chipX, chipY, chipW, chipH, over ? Theme.VIOLET : 0xFF262A48);
            Draw.rect(chipX, chipY, 2, chipH, over ? Theme.ACCENT : Theme.ACCENT_DIM);
            if (!Faces.draw(user, chipX + 4, chipY + 2, 12, 1f, offlineAcc)) {
                Draw.rect(chipX + 4, chipY + 2, 12, 12, Theme.VIOLET);
                if (user.length() > 0) Draw.centered(user.substring(0, 1).toUpperCase(), chipX + 10, chipY + 4, Theme.INK);
            }
            Draw.text(user, chipX + 22, chipY + 4, over ? Theme.ACCENT : Theme.TEXT);
            Draw.rect(chipX + chipW - 12, chipY + 6, 5, 5, online ? Theme.GREEN : Theme.OFF);
            return chipX;
        }
        chipH = 26;
        int textW = Math.max(Draw.width(user), Draw.width("\u25cf en l\u00ednea"));
        textW = Math.max(textW, Draw.width("Cambiar de cuenta"));
        chipW = 6 + 18 + 7 + textW + 10;
        chipX = width - 18 - chipW;
        chipY = height - 16 - chipH;
        over = Draw.inside(mx, my, chipX, chipY, chipW, chipH);
        Draw.rect(chipX, chipY, chipW, chipH, over ? Theme.PANEL2 : 0xC00B0E1C);
        Draw.border(chipX, chipY, chipW, chipH, over ? Theme.VIOLET : 0xFF262A48);
        Draw.rect(chipX, chipY, 2, chipH, over ? Theme.ACCENT : Theme.ACCENT_DIM);
        Draw.rect(chipX + 5, chipY + 3, 20, 20, over ? Theme.ACCENT : Theme.ACCENT_DIM);
        if (!Faces.draw(user, chipX + 6, chipY + 4, 18, 1f, offlineAcc)) {
            Draw.rect(chipX + 6, chipY + 4, 18, 18, Theme.VIOLET);
            if (user.length() > 0) Draw.centered(user.substring(0, 1).toUpperCase(), chipX + 15, chipY + 9, Theme.INK);
        }
        Draw.text(user, chipX + 31, chipY + 4, over ? Theme.ACCENT : Theme.TEXT);
        if (over) Draw.text(Lang.t("Cambiar de cuenta", "Switch account"), chipX + 31, chipY + 14, Theme.VIOLET);
        else Draw.text(online ? Lang.t("\u25cf en l\u00ednea", "\u25cf online") : Lang.t("\u25cb desconectado", "\u25cb offline"), chipX + 31, chipY + 14, online ? Theme.GREEN : Theme.DIM);
        return chipX;
    }
}
