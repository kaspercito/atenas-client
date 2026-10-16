package com.atenas.client.modules;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

public class TimeModule extends Module {
    private final Setting time;

    public TimeModule() {
        super("Hora", "Fija la hora del mundo solo para ti (0 amanecer, 6000 d\u00eda, 18000 noche).", Category.UTIL);
        time = addNum("Hora (ticks)", 6000, 0, 24000, 500);
    }

    @Override
    public void onTick() {
        if (mc.theWorld != null) mc.theWorld.setWorldTime((long) time.getInt());
    }
}
