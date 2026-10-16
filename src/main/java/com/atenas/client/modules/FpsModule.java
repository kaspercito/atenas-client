package com.atenas.client.modules;

import com.atenas.client.core.TextHud;

import net.minecraft.client.Minecraft;

public class FpsModule extends TextHud {
    public FpsModule() {
        super("FPS", "Muestra tus fotogramas por segundo.", 4, 4);
        startEnabled();
    }

    @Override
    protected String getText() { return "FPS: " + Minecraft.getDebugFPS(); }
}
