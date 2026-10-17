package com.atenas.client.modules;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

/** Lista de jugadores (TAB) sin fondo. Lo aplica MixinGuiPlayerTabOverlay. */
public class TabListModule extends Module {
    private final Setting noBg, noCells;

    public TabListModule() {
        super("Lista TAB", "Quita el fondo de la lista de jugadores (TAB) para ver el juego detrás.", Category.HUD);
        noBg = addBool("Sin fondo general", true);
        noCells = addBool("Sin cuadros por jugador", true);
        startEnabled();
    }

    public boolean hidesBackground() { return isEnabled() && noBg.bool; }

    public boolean hidesCells() { return isEnabled() && noCells.bool; }
}
