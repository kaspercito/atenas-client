package com.atenas.client.modules;

import com.atenas.client.core.HudModule;
import com.atenas.client.core.Setting;
import com.atenas.client.core.Stats;
import com.atenas.client.gui.Draw;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

/**
 * WASD, clics y espacio con CPS. Rendimiento: los fondos de las 7 teclas salen en UNA sola llamada de dibujo (Draw.batch*, antes
 * eran ~20 Gui.drawRect) y los textos se arman una vez: nombres de tecla solo cuando cambia el codigo de la tecla, "N CPS" solo
 * cuando cambia el numero, y el ancho de cada texto se mide una vez.
 */
public class KeystrokesModule extends HudModule {
    private static final int BG = 0xA0090812, BG_PRESSED = 0xC8A78BFA, DARK = 0xFF0F0D1D;

    private final Setting showCps;
    private final Setting showSpace;

    // ---- cache de textos
    private final int[] codes = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
    private final String[] names = new String[4];
    private final int[] nameW = new int[4];
    private int lastL = -1, lastR = -1;
    private String subL = "", subR = "";
    private int subLW, subRW;
    private static final String LMB = "LMB", RMB = "RMB";
    private int lmbW = -1, rmbW = -1;

    public KeystrokesModule() {
        super("Teclas", "Muestra WASD, clics y espacio (con CPS).", 4, 134);
        showCps = addBool("Mostrar CPS", true);
        showSpace = addBool("Barra espaciadora", true);
        startEnabled();
    }

    @Override
    public int getWidth() { return 64; }

    @Override
    public int getHeight() { return showSpace.bool ? 78 : 64; }

    private KeyBinding bind(int i) {
        switch (i) {
            case 0: return mc.gameSettings.keyBindForward;
            case 1: return mc.gameSettings.keyBindLeft;
            case 2: return mc.gameSettings.keyBindBack;
            default: return mc.gameSettings.keyBindRight;
        }
    }

    /** Nombres de WASD: solo se vuelven a pedir al juego si el jugador cambio la tecla en los controles. */
    private void refreshNames() {
        FontRenderer fr = mc.fontRendererObj;
        for (int i = 0; i < 4; i++) {
            int code = bind(i).getKeyCode();
            if (code == codes[i] && names[i] != null) continue;
            codes[i] = code;
            String s = GameSettings.getKeyDisplayString(code);
            if (s == null) s = "";
            names[i] = s.length() > 3 ? s.substring(0, 3) : s;
            nameW[i] = fr.getStringWidth(names[i]);
        }
        if (lmbW < 0) { lmbW = fr.getStringWidth(LMB); rmbW = fr.getStringWidth(RMB); }
        if (showCps.bool) {
            int l = Stats.leftCps(), r = Stats.rightCps();
            if (l != lastL) { lastL = l; subL = l + " CPS"; subLW = fr.getStringWidth(subL); }
            if (r != lastR) { lastR = r; subR = r + " CPS"; subRW = fr.getStringWidth(subR); }
        }
    }

    @Override
    protected void draw() {
        refreshNames();
        boolean w = bind(0).isKeyDown(), a = bind(1).isKeyDown(), s = bind(2).isKeyDown(), d = bind(3).isKeyDown();
        boolean lmb = mc.gameSettings.keyBindAttack.isKeyDown(), rmb = mc.gameSettings.keyBindUseItem.isKeyDown();
        boolean space = showSpace.bool && mc.gameSettings.keyBindJump.isKeyDown();

        // fondos: todo en un solo lote
        Draw.batchBegin();
        keyBox(22, 0, 20, 20, w);
        keyBox(0, 22, 20, 20, a);
        keyBox(22, 22, 20, 20, s);
        keyBox(44, 22, 20, 20, d);
        keyBox(0, 44, 31, 20, lmb);
        keyBox(33, 44, 31, 20, rmb);
        if (showSpace.bool) {
            Draw.rect(0, 66, 64, 12, space ? BG_PRESSED : (background.bool ? BG : 0x00000000));
            Draw.rect(22, 71, 20, 1, space ? DARK : color.getColor());
        }
        Draw.batchEnd();

        // textos (despues del lote: el lote no puede llevar texto)
        keyText(22, 0, 20, 20, names[0], nameW[0], w, null, 0);
        keyText(0, 22, 20, 20, names[1], nameW[1], a, null, 0);
        keyText(22, 22, 20, 20, names[2], nameW[2], s, null, 0);
        keyText(44, 22, 20, 20, names[3], nameW[3], d, null, 0);
        keyText(0, 44, 31, 20, LMB, lmbW, lmb, showCps.bool ? subL : null, subLW);
        keyText(33, 44, 31, 20, RMB, rmbW, rmb, showCps.bool ? subR : null, subRW);
    }

    /** Fondo de una tecla (rectangulos lisos dentro del lote). */
    private void keyBox(int kx, int ky, int w, int h, boolean pressed) {
        Draw.rect(kx, ky, w, h, pressed ? BG_PRESSED : (background.bool ? BG : 0x00000000));
        if (background.bool) {
            Draw.rect(kx, ky, w, 1, pressed ? 0xFFFFFFFF : 0x30FFFFFF);
            if (!pressed) Draw.rect(kx, ky + h - 1, w, 1, 0x60A78BFA);
        }
    }

    private void keyText(int kx, int ky, int w, int h, String label, int labelW, boolean pressed, String sub, int subW) {
        int tc = pressed ? DARK : color.getColor();
        boolean sh = shadow.bool && !pressed;
        FontRenderer fr = mc.fontRendererObj;
        if (sub == null) {
            fr.drawString(label, kx + (w - labelW) / 2, ky + (h - 8) / 2, tc, sh);
        } else {
            fr.drawString(label, kx + (w - labelW) / 2, ky + 3, tc, sh);
            fr.drawString(sub, kx + (w - subW) / 2, ky + 11, tc, sh);
        }
    }
}
