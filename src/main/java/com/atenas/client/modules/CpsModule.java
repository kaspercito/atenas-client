package com.atenas.client.modules;

import com.atenas.client.core.Stats;
import com.atenas.client.core.TextHud;

public class CpsModule extends TextHud {
    private int lastL = -1, lastR = -1;
    private String text = "CPS: 0 | 0";

    public CpsModule() {
        super("CPS", "Clics por segundo (izquierdo | derecho).", 4, 22);
        startEnabled();
    }

    /** El texto solo se vuelve a armar cuando cambia algun numero (casi siempre es el mismo). */
    @Override
    protected String getText() {
        int l = Stats.leftCps(), r = Stats.rightCps();
        if (l != lastL || r != lastR) {
            lastL = l;
            lastR = r;
            text = "CPS: " + l + " | " + r;
        }
        return text;
    }
}
