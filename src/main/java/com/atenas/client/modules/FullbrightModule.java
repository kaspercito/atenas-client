package com.atenas.client.modules;

import com.atenas.client.core.Module;

public class FullbrightModule extends Module {
    private float previous = -1f;

    public FullbrightModule() {
        super("Fullbright", "M\u00e1ximo brillo: ves bien en cuevas y de noche.", Category.UTIL);
    }

    @Override
    public void onEnable() { previous = -1f; }

    @Override
    public void onTick() {
        if (previous < 0f) {
            previous = mc.gameSettings.gammaSetting;
            if (previous > 1f) previous = 0.5f;
        }
        mc.gameSettings.gammaSetting = 100f;
    }

    @Override
    public void onDisable() {
        if (previous >= 0f) mc.gameSettings.gammaSetting = previous;
        previous = -1f;
    }
}
