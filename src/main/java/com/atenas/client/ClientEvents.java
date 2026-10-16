package com.atenas.client;

import com.atenas.client.gui.AtenasMainMenu;
import com.atenas.client.gui.AtenasMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ClientEvents {
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (mc.currentScreen == null && AtenasClient.keyMenu.isPressed()) {
            mc.displayGuiScreen(new AtenasMenu(null));
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e) {
        if (e.gui != null && e.gui.getClass() == GuiMainMenu.class) {
            e.gui = new AtenasMainMenu();
        }
    }
}
