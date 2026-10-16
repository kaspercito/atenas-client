package com.atenas.client.modules;

import com.atenas.client.core.TextHud;

/** Uso de RAM de Minecraft (usada / maxima asignada). */
public class MemoryModule extends TextHud {
    public MemoryModule() {
        super("Memoria", "Muestra cu\u00e1nta RAM usa Minecraft.", 0, 22);
        anchorRight();
    }

    @Override
    protected String getText() {
        Runtime r = Runtime.getRuntime();
        long used = (r.totalMemory() - r.freeMemory()) / 1048576L;
        long max = r.maxMemory() / 1048576L;
        return "RAM: " + used + "/" + max + " MB";
    }
}
