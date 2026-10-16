package com.atenas.client.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Estadisticas compartidas por los modulos HUD (CPS, combo, alcance). */
public class Stats {

    private static final List<Long> LEFT = new ArrayList<Long>();
    private static final List<Long> RIGHT = new ArrayList<Long>();
    private static int combo;
    private static long lastHit;
    private static double reach = -1;
    private static long reachTime;
    private static int prevHurt;

    public static int leftCps() { return count(LEFT); }

    public static int rightCps() { return count(RIGHT); }

    private static int count(List<Long> list) {
        long now = System.currentTimeMillis();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (now - list.get(i) > 1000L) list.remove(i);
        }
        return list.size();
    }

    public static int combo() { return combo; }

    public static String reachText() {
        if (reach < 0 || System.currentTimeMillis() - reachTime > 5000L) return "Alcance: --";
        return String.format(Locale.US, "Alcance: %.2f", reach);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate) return;
        long now = System.currentTimeMillis();
        if (e.button == 0) LEFT.add(now);
        else if (e.button == 1) RIGHT.add(now);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || e.entityPlayer != mc.thePlayer) return;
        long now = System.currentTimeMillis();
        if (e.target instanceof EntityLivingBase) {
            EntityLivingBase t = (EntityLivingBase) e.target;
            if (t.hurtResistantTime <= 10) {
                combo++;
                lastHit = now;
            }
        }
        if (mc.objectMouseOver != null && mc.objectMouseOver.hitVec != null
                && mc.objectMouseOver.entityHit == e.target) {
            reach = mc.objectMouseOver.hitVec.distanceTo(mc.thePlayer.getPositionEyes(1.0F));
            reachTime = now;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (p == null) {
            combo = 0;
            return;
        }
        if (p.hurtTime > 0 && prevHurt == 0) combo = 0;
        prevHurt = p.hurtTime;
        if (combo > 0 && System.currentTimeMillis() - lastHit > 3000L) combo = 0;
    }
}
