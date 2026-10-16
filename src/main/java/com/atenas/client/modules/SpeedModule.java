package com.atenas.client.modules;

import java.util.Locale;

import com.atenas.client.core.TextHud;

/** Velocidad horizontal en bloques por segundo. */
public class SpeedModule extends TextHud {
    private double bps, lastX, lastZ;
    private boolean has;

    public SpeedModule() {
        super("Velocidad", "Tu velocidad en bloques por segundo.", 0, 40);
        anchorRight();
    }

    @Override public void onEnable() { has = false; }

    @Override
    public void onTick() {
        if (mc.thePlayer == null) return;
        double x = mc.thePlayer.posX, z = mc.thePlayer.posZ;
        if (has) {
            double inst = Math.sqrt((x - lastX) * (x - lastX) + (z - lastZ) * (z - lastZ)) * 20.0;
            bps = bps * 0.6 + inst * 0.4;
        }
        lastX = x;
        lastZ = z;
        has = true;
    }

    @Override
    protected String getText() { return String.format(Locale.US, "%.1f b/s", bps); }
}
