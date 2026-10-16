package com.atenas.client.modules;

import com.atenas.client.core.HudModule;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

public class CoordsModule extends HudModule {
    public CoordsModule() {
        super("Coordenadas", "Tu posici\u00f3n y hacia d\u00f3nde miras.", 4, 58);
        startEnabled();
    }

    private String line1() {
        EntityPlayer p = mc.thePlayer;
        if (p == null) return "X: 0  Y: 0  Z: 0";
        return "X: " + (int) Math.floor(p.posX) + "  Y: " + (int) Math.floor(p.posY)
                + "  Z: " + (int) Math.floor(p.posZ);
    }

    private String line2() {
        EntityPlayer p = mc.thePlayer;
        if (p == null) return "Norte";
        String n = EnumFacing.fromAngle(p.rotationYaw).getName();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    @Override
    public int getWidth() {
        return Math.max(mc.fontRendererObj.getStringWidth(line1()), mc.fontRendererObj.getStringWidth(line2())) + 10;
    }

    @Override
    public int getHeight() { return 26; }

    @Override
    protected void draw() {
        box(getWidth(), getHeight());
        text(line1(), 5, 4);
        text(line2(), 5, 14);
    }
}
