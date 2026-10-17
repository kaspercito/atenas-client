package com.atenas.client.modules;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

import net.minecraft.client.gui.GuiIngameMenu;

/** Pone el estilo Atenas (cristal oscuro con neon violeta) a los botones vanilla del juego. Ver MixinGuiButton. */
public class ButtonsModule extends Module {
    private final Setting where;

    public ButtonsModule() {
        super("Botones Atenas", "Cambia el aspecto de los botones de Minecraft (menú de pausa, opciones...) al estilo Atenas.", Category.UTIL);
        where = addChoice("Dónde", 1, "Solo menú de pausa", "Todos los menús");
        startEnabled();
    }

    /** true si hay que dibujar con el estilo Atenas los botones de la pantalla actual. */
    public boolean applies() {
        if (!isEnabled()) return false;
        if (where.index() == 1) return true;
        return mc.currentScreen instanceof GuiIngameMenu;
    }
}
