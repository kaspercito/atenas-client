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

    /** Cada cuantos ms se vuelven a leer los efectos, sus textos y anchos (antes: lista nueva, nombres y tiempos nuevos 3 veces por cuadro). */
    private static final long REFRESH_MS = 100L;
    private static final int MAX_ROWS = 32;

    private static final Comparator<PotionEffect> BY_TIME = new Comparator<PotionEffect>() {
        @Override public int compare(PotionEffect a, PotionEffect b) { return a.getDuration() - b.getDuration(); }
    };

    // ---- cache
    private long cachedAt = -1000L;
    private final List<PotionEffect> cEffects = new ArrayList<PotionEffect>();
    private final String[] cNames = new String[MAX_ROWS], cTimes = new String[MAX_ROWS];
    private final int[] cLeft = new int[MAX_ROWS], cIcon = new int[MAX_ROWS];
    private final boolean[] cBad = new boolean[MAX_ROWS];
    private int cRows, cW = 60, cH = 4 + ROW;
    private boolean cCompact, cIcons;
    private PotionEffect demoSpeed, demoStrength, demoRegen;

    private void collect(List<PotionEffect> out) {
        out.clear();
        if (mc.thePlayer != null) out.addAll(mc.thePlayer.getActivePotionEffects());
        if (out.isEmpty() && isEditing()) {
            if (demoSpeed == null) {
                demoSpeed = new PotionEffect(Potion.moveSpeed.id, 1830, 1);
                demoStrength = new PotionEffect(Potion.damageBoost.id, 190, 0);
                demoRegen = new PotionEffect(Potion.regeneration.id, 620, 2);
            }
            out.add(demoSpeed);
            out.add(demoStrength);
            out.add(demoRegen);
        }
        if (sort.bool && out.size() > 1) Collections.sort(out, BY_TIME);
    }

    private static String level(int amplifier) {
        int lvl = amplifier + 1;
        if (lvl <= 1) return "";
        return " " + (lvl < ROMAN.length ? ROMAN[lvl] : String.valueOf(lvl));
    }

    private String name(PotionEffect e) { return I18n.format(e.getEffectName()) + level(e.getAmplifier()); }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < REFRESH_MS && now >= cachedAt) return;
        cachedAt = now;

        collect(cEffects);
        cCompact = style.index() == 1;
        cIcons = icons.bool;
        cRows = Math.min(cEffects.size(), MAX_ROWS);
        int w = 60;
        for (int i = 0; i < cRows; i++) {
            PotionEffect e = cEffects.get(i);
            Potion p = e.getPotionID() >= 0 && e.getPotionID() < Potion.potionTypes.length ? Potion.potionTypes[e.getPotionID()] : null;
            String time = Potion.getDurationString(e);
            cTimes[i] = time;
            cNames[i] = cCompact ? null : name(e);
            cLeft[i] = e.getDuration();
            cBad[i] = p != null && p.isBadEffect();
            cIcon[i] = (p != null && p.hasStatusIcon()) ? p.getStatusIconIndex() : -1;
            int tw = mc.fontRendererObj.getStringWidth(time);
            if (!cCompact) tw = Math.max(tw, mc.fontRendererObj.getStringWidth(cNames[i]));
            w = Math.max(w, (cIcons ? 25 : 7) + tw + 8);
        }
        cW = w;
        cH = 4 + Math.max(1, cEffects.size()) * ROW;
    }

    @Override
    public int getWidth() { refresh(); return cW; }

    @Override
    public int getHeight() { refresh(); return cH; }

    @Override
    protected void draw() {
        refresh();
        if (cRows == 0) return;
        box(cW, cH);
        // pasada 1: iconos (la textura del inventario se enlaza una sola vez, antes de escribir ningun texto)
        if (cIcons) {
            boolean bound = false;
            for (int i = 0; i < cRows; i++) {
                if (cIcon[i] < 0) continue;
                if (!bound) {
                    mc.getTextureManager().bindTexture(INVENTORY);
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    GlStateManager.enableBlend();
                    bound = true;
                }
                int idx = cIcon[i];
                Draw.texRect(4, 2 + i * ROW, idx % 8 * 18, 198 + idx / 8 * 18, 18, 18);
            }
        }
        // pasada 2: textos
        boolean flash = (System.currentTimeMillis() / 300L) % 2L == 0L;
        int tx = cIcons ? 25 : 6;
        for (int i = 0; i < cRows; i++) {
            int y = 2 + i * ROW;
            int left = cLeft[i];
            int timeColor = color.getColor();
            if (tint.bool) timeColor = left <= 200 ? 0xFFFF5555 : left <= 600 ? 0xFFFBBF24 : color.getColor();
            if (blink.bool && left <= 200 && flash) timeColor = 0xFFFFFFFF;   // 200 ticks = 10 segundos
            if (cCompact) {
                mc.fontRendererObj.drawString(cTimes[i], tx, y + 5, timeColor, shadow.bool);
            } else {
                int nameColor = cBad[i] ? 0xFFFF7777 : color.getColor();
                mc.fontRendererObj.drawString(cNames[i], tx, y + 1, nameColor, shadow.bool);
                mc.fontRendererObj.drawString(cTimes[i], tx, y + 10, timeColor, shadow.bool);
            }
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
