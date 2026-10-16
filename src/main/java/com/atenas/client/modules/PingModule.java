package com.atenas.client.modules;

import com.atenas.client.core.TextHud;

import net.minecraft.client.network.NetworkPlayerInfo;

public class PingModule extends TextHud {
    public PingModule() {
        super("Ping", "Tu latencia con el servidor.", 4, 40);
        startEnabled();
    }

    @Override
    protected String getText() {
        int ping = 0;
        if (mc.getNetHandler() != null && mc.thePlayer != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (info != null) ping = info.getResponseTime();
        }
        return "Ping: " + ping + " ms";
    }
}
