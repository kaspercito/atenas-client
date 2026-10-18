package com.atenas.client.modules;

import com.atenas.client.core.ModuleManager;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Recortes de dibujo para ganar FPS (los usa el modulo "Rendimiento"). */
public class PerfEvents {
    private PerformanceModule module;
    // distancias al cuadrado ya calculadas (se recalculan solo si el jugador cambia el ajuste)
    private int lastMob = -1, lastHolo = -1;
    private double mobSq, holoSq;

    @SubscribeEvent
    public void onLiving(RenderLivingEvent.Pre e) {
        Entity ent = e.entity;
        // Los jugadores NUNCA se ocultan (ni ellos ni su nombre): en PvP hay que poder ver y seguir a la gente desde lejos.
        if (ent instanceof EntityPlayer) return;

        PerformanceModule m = module;
        if (m == null) {
            m = ModuleManager.find(PerformanceModule.class);
            module = m;
            if (m == null) return;
        }
        if (!m.isEnabled()) return;

        boolean holo = ent instanceof EntityArmorStand;  // los hologramas de los servidores son armor stands con nombre
        int d = holo ? m.hologramCullDistance() : m.entityCullDistance();
        if (d <= 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || ent == mc.thePlayer) return;

        double limitSq;
        if (holo) {
            if (d != lastHolo) { lastHolo = d; holoSq = (double) d * d; }
            limitSq = holoSq;
        } else {
            if (d != lastMob) { lastMob = d; mobSq = (double) d * d; }
            limitSq = mobSq;
        }
        if (ent.getDistanceSqToEntity(mc.thePlayer) > limitSq) e.setCanceled(true);
    }
}
