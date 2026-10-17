package com.atenas.client.modules;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

/** Scoreboard (la tabla de la derecha) sin fondo oscuro y, si quieres, sin los numeros rojos. Lo aplica MixinGuiIngame. */
public class ScoreboardModule extends Module {
    private final Setting noBg, noNumbers;

    public ScoreboardModule() {
        super("Scoreboard", "Quita el fondo de la tabla de la derecha y limpia sus números.", Category.HUD);
        noBg = addBool("Sin fondo", true);
        noNumbers = addBool("Ocultar números rojos", true);
        startEnabled();
    }

    public boolean hidesBackground() { return isEnabled() && noBg.bool; }

    public boolean hidesNumbers() { return isEnabled() && noNumbers.bool; }
}
