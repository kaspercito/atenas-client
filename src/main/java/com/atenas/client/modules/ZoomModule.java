package com.atenas.client.modules;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.atenas.client.AtenasClient;
import com.atenas.client.core.Config;
import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;

public class ZoomModule extends Module {
    private final Setting level, smooth, wheel;
    private boolean smoothApplied, prevSmooth;

    public ZoomModule() {
        super("Zoom", "Mant\u00e9n la tecla de zoom (C por defecto) para acercar la vista. Rueda del mouse = m\u00e1s o menos zoom.", Category.UTIL);
        level = addNum("Nivel", 4.0, 1.5, 10.0, 0.5);
        smooth = addBool("C\u00e1mara suave al hacer zoom", true);
        wheel = addBool("Rueda ajusta el zoom", true);
        startEnabled();
    }

    private boolean active() {
        if (AtenasClient.hasOptiFine()) return false; // OptiFine ya trae su zoom (tambien en C): no lo duplicamos
        if (AtenasClient.keyZoom == null || mc.currentScreen != null) return false;
        int k = AtenasClient.keyZoom.getKeyCode();
        if (k > 0) return Keyboard.isKeyDown(k);
        if (k < 0) return Mouse.isButtonDown(k + 100);
        return false;
    }

    @Override
    public void onFov(FOVUpdateEvent e) {
        if (active()) e.newfov = e.newfov / level.getFloat();
    }

    @Override
    public void onTick() {
        boolean a = active();
        if (smooth.bool && a && !smoothApplied) {
            prevSmooth = mc.gameSettings.smoothCamera;
            mc.gameSettings.smoothCamera = true;
            smoothApplied = true;
        } else if (smoothApplied && !a) {
            mc.gameSettings.smoothCamera = prevSmooth;
            smoothApplied = false;
        }
    }

    @Override
    public void onMouse(MouseEvent e) {
        if (!wheel.bool || e.dwheel == 0 || !active()) return;
        double v = level.num + (e.dwheel > 0 ? 0.5 : -0.5);
        level.num = Math.max(level.min, Math.min(level.max, v));
        e.setCanceled(true); // no cambia la ranura de la barra de objetos
        Config.save();
    }

    @Override
    public void onDisable() {
        if (smoothApplied) { mc.gameSettings.smoothCamera = prevSmooth; smoothApplied = false; }
    }
}
