package com.atenas.client.modules;

import com.atenas.client.core.Stats;
import com.atenas.client.core.TextHud;

public class CpsModule extends TextHud {
    public CpsModule() {
        super("CPS", "Clics por segundo (izquierdo | derecho).", 4, 22);
        startEnabled();
    }

    @Override
    protected String getText() { return "CPS: " + Stats.leftCps() + " | " + Stats.rightCps(); }
}
