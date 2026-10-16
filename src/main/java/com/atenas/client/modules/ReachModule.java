package com.atenas.client.modules;

import com.atenas.client.core.Stats;
import com.atenas.client.core.TextHud;

public class ReachModule extends TextHud {
    public ReachModule() {
        super("Alcance", "Distancia de tu \u00faltimo golpe.", 4, 94);
    }

    @Override
    protected String getText() { return Stats.reachText(); }
}
