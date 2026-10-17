package com.atenas.client.modules;

import com.atenas.client.core.TextHud;

import net.minecraft.util.MathHelper;

/** Brujula: hacia donde miras (N, NE, E...) y los grados. */
public class DirectionModule extends TextHud {
    private static final String[] DIRS = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};

    public DirectionModule() {
        super("Direcci\u00f3n", "Muestra hacia d\u00f3nde miras: punto cardinal y grados.", 4, 100);
    }

    @Override
    protected String getText() {
        if (mc.thePlayer == null) return "";
        float yaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
        int i = (int) Math.floor((yaw + 180f + 22.5f) / 45f) & 7;
        return DIRS[(i + 4) & 7] + "  " + (int) (yaw + 180f) + "\u00b0";
    }
}
