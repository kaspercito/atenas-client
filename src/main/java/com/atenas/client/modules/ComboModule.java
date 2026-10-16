package com.atenas.client.modules;

import com.atenas.client.core.Stats;
import com.atenas.client.core.TextHud;

public class ComboModule extends TextHud {
    public ComboModule() {
        super("Combo", "Golpes seguidos sin recibir da\u00f1o.", 4, 76);
    }

    @Override
    protected String getText() { return "Combo: " + Stats.combo(); }
}
