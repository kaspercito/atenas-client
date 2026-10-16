package com.atenas.client.modules;

import com.atenas.client.core.Module;

import net.minecraftforge.client.event.RenderBlockOverlayEvent;

public class LowFireModule extends Module {
    public LowFireModule() {
        super("Sin Fuego", "Oculta el fuego que tapa la pantalla cuando te quemas.", Category.PVP);
        startEnabled();
    }

    @Override
    public void onFireOverlay(RenderBlockOverlayEvent e) {
        if (e.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE) e.setCanceled(true);
    }
}
