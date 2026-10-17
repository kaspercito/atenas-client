package com.atenas.client.gui;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.gui.GuiButton;

/** Sabe si una clase de boton es un deslizador (sobrescribe mouseDragged): esos se dibujan con su propia barra y no se tocan a ciegas. */
public final class ButtonKind {
    private static final Map<Class<?>, Boolean> CACHE = new ConcurrentHashMap<Class<?>, Boolean>();

    private ButtonKind() {}

    /** true si la clase (o alguna superior hasta GuiButton) declara mouseDragged. En desarrollo se llama asi; en el juego, func_146119_b. */
    public static boolean draggable(Class<?> c) {
        Boolean known = CACHE.get(c);
        if (known != null) return known;
        boolean r = false;
        for (Class<?> k = c; k != null && k != GuiButton.class && !r; k = k.getSuperclass()) {
            try {
                for (Method m : k.getDeclaredMethods()) {
                    String n = m.getName();
                    if ((n.equals("mouseDragged") || n.equals("func_146119_b")) && m.getParameterTypes().length == 3) { r = true; break; }
                }
            } catch (Throwable ignored) { /* clase rara: se trata como boton normal */ }
        }
        CACHE.put(c, r);
        return r;
    }
}
