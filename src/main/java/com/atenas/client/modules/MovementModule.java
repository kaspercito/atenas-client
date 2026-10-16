package com.atenas.client.modules;

import org.lwjgl.input.Keyboard;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;

/** Movimiento: Sprint automatico + Agacharse toggle en un solo mod (antes eran dos). */
public class MovementModule extends Module {
    private final Setting sprint, toggleSneak;
    private boolean sneaking;

    public MovementModule() {
        super("Movimiento", "Sprint automático al avanzar y agacharse con toggle (pulsas una vez para quedarte agachado, otra para soltar).", Category.PVP);
        sprint = addBool("Sprint automático", true);
        toggleSneak = addBool("Agacharse (toggle)", false);
        startEnabled();
    }

    private int code() { return mc.gameSettings.keyBindSneak.getKeyCode(); }

    @Override
    public void onKey(int key) {
        if (toggleSneak.bool && mc.currentScreen == null && key == code()) sneaking = !sneaking;
    }

    @Override
    public void onTick() {
        EntityPlayerSP p = mc.thePlayer;
        if (p == null) return;
        if (toggleSneak.bool) {
            int c = code();
            boolean held = c > 0 && Keyboard.isKeyDown(c);
            KeyBinding.setKeyBindState(c, sneaking || held);
        } else if (sneaking) {
            release();   // lo apagaste en los ajustes mientras estabas agachado
        }
        if (sprint.bool && mc.currentScreen == null
                && p.moveForward > 0 && !p.isSneaking() && !p.isUsingItem() && !p.isCollidedHorizontally
                && p.getFoodStats().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }

    private void release() {
        sneaking = false;
        int c = code();
        KeyBinding.setKeyBindState(c, c > 0 && Keyboard.isKeyDown(c));
    }

    @Override
    public void onDisable() { release(); }
}
