package com.atenas.client.modules;

import com.atenas.client.core.HudModule;
import com.atenas.client.core.Setting;
import com.atenas.client.core.Stats;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public class KeystrokesModule extends HudModule {
    private final Setting showCps;
    private final Setting showSpace;

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

    private String label(KeyBinding kb) {
        String s = GameSettings.getKeyDisplayString(kb.getKeyCode());
        return s.length() > 3 ? s.substring(0, 3) : s;
    }

    @Override
    protected void draw() {
        key(22, 0, 20, 20, label(mc.gameSettings.keyBindForward), mc.gameSettings.keyBindForward.isKeyDown(), null);
        key(0, 22, 20, 20, label(mc.gameSettings.keyBindLeft), mc.gameSettings.keyBindLeft.isKeyDown(), null);
        key(22, 22, 20, 20, label(mc.gameSettings.keyBindBack), mc.gameSettings.keyBindBack.isKeyDown(), null);
        key(44, 22, 20, 20, label(mc.gameSettings.keyBindRight), mc.gameSettings.keyBindRight.isKeyDown(), null);
        key(0, 44, 31, 20, "LMB", mc.gameSettings.keyBindAttack.isKeyDown(),
                showCps.bool ? Stats.leftCps() + " CPS" : null);
        key(33, 44, 31, 20, "RMB", mc.gameSettings.keyBindUseItem.isKeyDown(),
                showCps.bool ? Stats.rightCps() + " CPS" : null);
        if (showSpace.bool) {
            boolean down = mc.gameSettings.keyBindJump.isKeyDown();
            Gui.drawRect(0, 66, 64, 78, down ? 0xC8A78BFA : (background.bool ? 0xA0090812 : 0x00000000));
            Gui.drawRect(22, 71, 42, 72, down ? 0xFF0F0D1D : color.getColor());
        }
    }

    private void key(int kx, int ky, int w, int h, String label, boolean pressed, String sub) {
        int bg = pressed ? 0xC8A78BFA : (background.bool ? 0xA0090812 : 0x00000000);
        Gui.drawRect(kx, ky, kx + w, ky + h, bg);
        if (background.bool) {
            Gui.drawRect(kx, ky, kx + w, ky + 1, pressed ? 0xFFFFFFFF : 0x30FFFFFF);
            if (!pressed) Gui.drawRect(kx, ky + h - 1, kx + w, ky + h, 0x60A78BFA);
        }
        int tc = pressed ? 0xFF0F0D1D : color.getColor();
        boolean sh = shadow.bool && !pressed;
        FontRenderer fr = mc.fontRendererObj;
        if (sub == null) {
            fr.drawString(label, kx + (w - fr.getStringWidth(label)) / 2, ky + (h - 8) / 2, tc, sh);
        } else {
            fr.drawString(label, kx + (w - fr.getStringWidth(label)) / 2, ky + 3, tc, sh);
            fr.drawString(sub, kx + (w - fr.getStringWidth(sub)) / 2, ky + 11, tc, sh);
        }
    }
}
