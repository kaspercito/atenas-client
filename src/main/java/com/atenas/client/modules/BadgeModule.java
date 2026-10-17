package com.atenas.client.modules;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

/** Muestra el logo de Atenas junto al nombre (nametag) y en la lista TAB de quienes usan el cliente o el mod. */
public class BadgeModule extends Module {
    private final Setting tab, nametag;

    public BadgeModule() {
        super("Logo Atenas", "Pone el logo de Atenas en el TAB y sobre la cabeza de quienes usan el cliente o el mod.", Category.UTIL);
        tab = addBool("Mostrar en TAB", true);
        nametag = addBool("Mostrar en nametag", true);
        startEnabled();
    }

    public boolean inTab() { return isEnabled() && tab.bool; }

    public boolean inNametag() { return isEnabled() && nametag.bool; }
}
