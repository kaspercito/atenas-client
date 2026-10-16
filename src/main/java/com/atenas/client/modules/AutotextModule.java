package com.atenas.client.modules;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.atenas.client.core.Module;

import net.minecraftforge.client.event.MouseEvent;

/**
 * Atajos que envian un mensaje o comando al chat (como el AutoText de Lunar).
 * Cada atajo puede ser una tecla del teclado o un boton del mouse. Se editan desde Mods > Autotext (engranaje).
 * Codigo de tecla: > 0 teclado (LWJGL); < 0 mouse (igual que los controles de Minecraft: -100 + boton, 0 = clic izquierdo).
 */
public class AutotextModule extends Module {

    public static class Entry {
        public int key;
        public String message;

        public Entry(int key, String message) {
            this.key = key;
            this.message = message;
        }
    }

    public static final int MAX_ENTRIES = 12;
    public static final List<Entry> entries = new ArrayList<Entry>();
    private long lastSent;

    public AutotextModule() {
        super("Autotext", "Asigna una tecla o un botón del mouse a un mensaje o comando que se envía al chat al instante.", Category.UTIL);
        startEnabled();
    }

    /** Nombre legible de una tecla o boton del mouse. */
    public static String keyName(int key) {
        if (key == 0) return "Sin tecla";
        if (key < 0) {
            int b = key + 100;
            if (b == 0) return "Clic izq.";
            if (b == 1) return "Clic der.";
            if (b == 2) return "Clic central";
            return "Mouse " + (b + 1);
        }
        String n = Keyboard.getKeyName(key);
        return n == null ? "Tecla " + key : n;
    }

    private void fire(int code) {
        if (mc.thePlayer == null || mc.currentScreen != null || code == 0) return;
        long now = System.currentTimeMillis();
        if (now - lastSent < 250L) return;
        for (Entry e : entries) {
            if (e.key == code && e.message != null && e.message.trim().length() > 0) {
                mc.thePlayer.sendChatMessage(e.message);
                lastSent = now;
                return;
            }
        }
    }

    @Override
    public void onKey(int key) { if (key > 0) fire(key); }

    @Override
    public void onMouse(MouseEvent e) {
        if (e.button >= 0 && e.buttonstate) fire(e.button - 100);
    }
}
