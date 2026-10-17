package com.atenas.client.modules;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.atenas.client.AtenasClient;
import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

import net.minecraft.client.settings.KeyBinding;

/**
 * Perspective (mirar alrededor): mantienes la tecla (Alt izquierdo por defecto) y la camara gira alrededor de tu personaje
 * en tercera persona SIN que el personaje gire ni cambie hacia donde caminas o pegas. Al soltarla vuelve la vista de antes.
 * La parte de camara y de mouse la hacen MixinEntityRenderer y MixinEntity; aqui solo vive el estado.
 */
public class PerspectiveModule extends Module {
    private final Setting mode, invert;
    private boolean active, toggled, prevKey;
    private int prevView;
    private float camYaw, camPitch;

    public PerspectiveModule() {
        super("Perspective", "Mantén la tecla (Alt izquierdo por defecto) para mirar alrededor de tu personaje sin girarlo. Al soltarla vuelve tu vista de antes.", Category.UTIL);
        mode = addChoice("Modo", 0, "Mantener", "Alternar");
        invert = addBool("Invertir eje vertical", false);
        startEnabled();
    }

    private boolean keyDown() {
        KeyBinding k = AtenasClient.keyPerspective;
        if (k == null || mc.currentScreen != null) return false;
        int c = k.getKeyCode();
        if (c > 0) return Keyboard.isKeyDown(c);
        if (c < 0) return Mouse.isButtonDown(c + 100);
        return false;
    }

    /** True mientras la camara esta suelta del personaje (la consultan los mixins). */
    public boolean freelooking() { return active && mc.thePlayer != null; }

    public float camYaw() { return camYaw; }

    public float camPitch() { return camPitch; }

    /** Movimiento del mouse que normalmente giraria al jugador (mismo factor 0.15 que Entity.setAngles). */
    public void look(float dYaw, float dPitch) {
        camYaw += dYaw * 0.15f;
        camPitch += (invert.bool ? dPitch : -dPitch) * 0.15f;
        if (camPitch > 90f) camPitch = 90f;
        if (camPitch < -90f) camPitch = -90f;
    }

    @Override
    public void onTick() {
        boolean down = keyDown();
        boolean want;
        if (mode.index() == 1) {
            if (down && !prevKey) toggled = !toggled;
            want = toggled;
        } else {
            want = down;
        }
        prevKey = down;
        if (want && !active) start();
        else if (!want && active) stop();
    }

    private void start() {
        if (mc.thePlayer == null) return;
        camYaw = mc.thePlayer.rotationYaw;
        camPitch = mc.thePlayer.rotationPitch;
        prevView = mc.gameSettings.thirdPersonView;
        if (prevView == 0) mc.gameSettings.thirdPersonView = 1; // detras del personaje; si ya estabas en 3a persona se respeta
        active = true;
    }

    private void stop() {
        active = false;
        mc.gameSettings.thirdPersonView = prevView;
    }

    @Override
    public void onDisable() {
        if (active) stop();
        toggled = false;
    }
}
