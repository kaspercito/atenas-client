package com.atenas.client.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.atenas.client.core.HudModule;
import com.atenas.client.core.Setting;
import com.atenas.client.gui.Draw;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

/** Efectos activos: icono del efecto, nombre con nivel en romano y tiempo restante (cambia de color y parpadea al acabarse). */
public class PotionModule extends HudModule {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    private static final int ROW = 20;
    private static final String[] ROMAN = {"", "", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private final Setting style, icons, tint, blink, sort;

    public PotionModule() {
        super("Pociones", "Efectos activos con su icono, nombre, nivel y tiempo restante.", 0, 4);
        style = addChoice("Estilo", 0, "Completo", "Compacto");
        icons = addBool("Mostrar icono del efecto", true);
        tint = addBool("Color según el tiempo que queda", true);
        blink = addBool("Parpadear cuando se está acabando", true);
        sort = addBool("Ordenar por tiempo restante", false);
        anchorRight();
        startEnabled();
    }

    private List<PotionEffect> effects() {
        List<PotionEffect> out = new ArrayList<PotionEffect>();
        if (mc.thePlayer != null) out.addAll(mc.thePlayer.getActivePotionEffects());
        if (out.isEmpty() && isEditing()) {
            out.add(new PotionEffect(Potion.moveSpeed.id, 1830, 1));
            out.add(new PotionEffect(Potion.damageBoost.id, 190, 0));
            out.add(new PotionEffect(Potion.regeneration.id, 620, 2));
        }
        if (sort.bool) {
            Collections.sort(out, new Comparator<PotionEffect>() {
                @Override public int compare(PotionEffect a, PotionEffect b) { return a.getDuration() - b.getDuration(); }
            });
        }
        return out;
    }

    private static String level(int amplifier) {
        int lvl = amplifier + 1;
        if (lvl <= 1) return "";
        return " " + (lvl < ROMAN.length ? ROMAN[lvl] : String.valueOf(lvl));
    }

    private String name(PotionEffect e) { return I18n.format(e.getEffectName()) + level(e.getAmplifier()); }

    private boolean compact() { return style.index() == 1; }

    @Override
    public int getWidth() {
        int w = 60;
        for (PotionEffect e : effects()) {
            String time = Potion.getDurationString(e);
            int tw = mc.fontRendererObj.getStringWidth(time);
            if (!compact()) tw = Math.max(tw, mc.fontRendererObj.getStringWidth(name(e)));
            w = Math.max(w, (icons.bool ? 25 : 7) + tw + 8);
        }
        return w;
    }

    @Override
    public int getHeight() { return 4 + Math.max(1, effects().size()) * ROW; }

    @Override
    protected void draw() {
        List<PotionEffect> list = effects();
        if (list.isEmpty()) return;
        box(getWidth(), getHeight());
        long now = System.currentTimeMillis();
        for (int i = 0; i < list.size(); i++) {
            PotionEffect e = list.get(i);
            int y = 2 + i * ROW;
            Potion p = e.getPotionID() >= 0 && e.getPotionID() < Potion.potionTypes.length ? Potion.potionTypes[e.getPotionID()] : null;
            int tx = 6;
            if (icons.bool) {
                if (p != null && p.hasStatusIcon()) {
                    mc.getTextureManager().bindTexture(INVENTORY);
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    GlStateManager.enableBlend();
                    int idx = p.getStatusIconIndex();
                    Draw.texRect(4, y, idx % 8 * 18, 198 + idx / 8 * 18, 18, 18);
                }
                tx = 25;
            }
            int left = e.getDuration();
            boolean ending = left <= 200;                         // 10 segundos
            int timeColor = color.getColor();
            if (tint.bool) timeColor = left <= 200 ? 0xFFFF5555 : left <= 600 ? 0xFFFBBF24 : color.getColor();
            if (blink.bool && ending && (now / 300L) % 2L == 0L) timeColor = 0xFFFFFFFF;
            int nameColor = (p != null && p.isBadEffect()) ? 0xFFFF7777 : color.getColor();
            String time = Potion.getDurationString(e);
            if (compact()) {
                mc.fontRendererObj.drawString(time, tx, y + 5, timeColor, shadow.bool);
            } else {
                mc.fontRendererObj.drawString(name(e), tx, y + 1, nameColor, shadow.bool);
                mc.fontRendererObj.drawString(time, tx, y + 10, timeColor, shadow.bool);
            }
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
