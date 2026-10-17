package com.atenas.client.modules;

import com.atenas.client.core.ModuleManager;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Recortes de dibujo para ganar FPS (los usa el modulo "Rendimiento"). */
public class PerfEvents {
    private double lastDist = -1, lastSq;

    @SubscribeEvent
    public void onLiving(RenderLivingEvent.Pre e) {
        PerformanceModule m = ModuleManager.find(PerformanceModule.class);
        if (m == null || !m.isEnabled()) return;
        int d = m.entityCullDistance();
        if (d <= 0) return;
        Entity ent = e.entity;
        if (ent instanceof EntityPlayer) return; // jugadores siempre se ven
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || ent == mc.thePlayer) return;
        if (d != lastDist) { lastDist = d; lastSq = (double) d * d; }
        if (ent.getDistanceSqToEntity(mc.thePlayer) > lastSq) e.setCanceled(true);
    }
}
