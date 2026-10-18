package com.atenas.client.modules;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.atenas.client.core.HudModule;
import com.atenas.client.core.Setting;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;

/** Armadura + objeto en mano: vertical u horizontal, durabilidad en numero o %, barra, aviso de poca durabilidad y contador de flechas. */
public class ArmorModule extends HudModule {
    private final Setting layout, text, bar, held, arrows, fixed, warn;

    public ArmorModule() {
        super("Armadura", "Armadura y objeto en mano con su durabilidad. Muévelo y cámbiale el tamaño en el Editor HUD.", 0, 120);
        layout = addChoice("Disposición", 0, "Vertical", "Horizontal");
        text = addChoice("Texto", 0, "Durabilidad", "Porcentaje", "Ninguno");
        bar = addBool("Barra de durabilidad", true);
        held = addBool("Objeto en la mano", true);
        arrows = addBool("Contar flechas con el arco", true);
        fixed = addBool("Ranuras fijas (el panel no cambia de tamaño)", true);
        warn = addBool("Parpadear con poca durabilidad", true);
        anchorRight();
        startEnabled();
    }

    /** Cada cuantos ms se vuelve a leer inventario, durabilidad, textos y anchos (antes se rehacia todo 3 o 4 veces por cuadro). */
    private static final long REFRESH_MS = 50L;

    // ---- cache (se rellena en refresh(); getWidth/getHeight/draw solo la leen)
    private long cachedAt = -1000L;
    private final List<ItemStack> cItems = new ArrayList<ItemStack>(8);
    private final String[] cLabels = new String[8];
    private final int[] cColors = new int[8];
    private final boolean[] cWarn = new boolean[8];
    private int cW = 26, cH = 22, cCell = 20, cLabelW;
    private boolean cHorizontal;
    /** Objetos de muestra del Editor HUD y pila de flechas: se crean una vez, no en cada cuadro. */
    private ItemStack[] demo;
    private ItemStack demoArrows, arrowStack;

    private boolean horizontal() { return layout.index() == 1; }

    private static ItemStack damaged(ItemStack s, int dmg) {
        s.setItemDamage(dmg);
        return s;
    }

    private void ensureDemo() {
        if (demo != null) return;
        demo = new ItemStack[] {
            damaged(new ItemStack(Items.diamond_sword), 120), new ItemStack(Items.diamond_helmet),
            damaged(new ItemStack(Items.diamond_chestplate), 60), damaged(new ItemStack(Items.diamond_leggings), 150),
            damaged(new ItemStack(Items.diamond_boots), 200)
        };
        demoArrows = new ItemStack(Items.arrow, 42);
        arrowStack = new ItemStack(Items.arrow, 1);
    }

    private void collect(List<ItemStack> out) {
        out.clear();
        EntityPlayer p = mc.thePlayer;
        boolean sample = isEditing();
        if (sample) ensureDemo();
        if (held.bool) out.add(p != null ? p.getCurrentEquippedItem() : null);
        for (int i = 3; i >= 0; i--) out.add(p != null ? p.inventory.armorInventory[i] : null);
        if (sample) {
            int off = held.bool ? 0 : 1;
            for (int i = 0; i < out.size(); i++) if (out.get(i) == null) out.set(i, demo[i + off]);
        }
        if (!fixed.bool) {
            for (int i = out.size() - 1; i >= 0; i--) if (out.get(i) == null) out.remove(i);
        }
        if (arrows.bool && p != null && p.getCurrentEquippedItem() != null && p.getCurrentEquippedItem().getItem() instanceof ItemBow) {
            int n = 0;
            for (ItemStack st : p.inventory.mainInventory) if (st != null && st.getItem() == Items.arrow) n += st.stackSize;
            if (n > 0) {
                ensureDemo();
                arrowStack.stackSize = n;
                out.add(arrowStack);
            }
        } else if (arrows.bool && sample) {
            out.add(demoArrows);
        }
    }

    private String label(ItemStack s) {
        if (s.isItemStackDamageable()) {
            int max = Math.max(1, s.getMaxDamage());
            int left = max - s.getItemDamage();
            if (text.index() == 0) return String.valueOf(left);
            if (text.index() == 1) return (left * 100 / max) + "%";
            return null;
        }
        if (text.index() != 2 && s.stackSize > 1) return "x" + s.stackSize;
        return null;
    }

    /** Color base del texto (verde a rojo segun lo que queda). El parpadeo blanco se decide al dibujar, con cWarn. */
    private int baseColor(ItemStack s) {
        if (!s.isItemStackDamageable()) return color.getColor();
        int max = Math.max(1, s.getMaxDamage());
        float frac = Math.max(0f, (float) (max - s.getItemDamage()) / max);
        return Color.HSBtoRGB(frac * 0.33f, 0.9f, 1f);
    }

    private boolean lowDurability(ItemStack s) {
        if (!warn.bool || !s.isItemStackDamageable()) return false;
        int max = Math.max(1, s.getMaxDamage());
        return (float) (max - s.getItemDamage()) / max <= 0.10f;
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < REFRESH_MS && now >= cachedAt) return;
        cachedAt = now;

        collect(cItems);
        int n = Math.min(cItems.size(), cLabels.length);
        int maxW = 0;
        for (int i = 0; i < n; i++) {
            ItemStack s = cItems.get(i);
            if (s == null) { cLabels[i] = null; continue; }
            String t = label(s);
            cLabels[i] = t;
            cColors[i] = baseColor(s);
            cWarn[i] = lowDurability(s);
            if (t != null) maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(t));
        }
        cLabelW = maxW;
        cHorizontal = horizontal();
        cCell = Math.max(20, maxW + 4);
        int count = Math.max(1, cItems.size());
        if (cHorizontal) {
            cW = count * cCell + 4;
            cH = 22 + (maxW > 0 ? 10 : 0);
        } else {
            cW = maxW > 0 ? Math.max(34, 25 + maxW + 6) : 26;
            cH = count * 18 + 4;
        }
    }

    @Override
    public int getWidth() { refresh(); return cW; }

    @Override
    public int getHeight() { refresh(); return cH; }

    @Override
    protected void draw() {
        refresh();
        int count = Math.min(cItems.size(), cLabels.length);
        box(cW, cH);
        final boolean hor = cHorizontal;
        final int cell = cCell;

        // pasada 1: objetos (la iluminacion de GUI se activa una sola vez, como en la barra de vanilla)
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < count; i++) {
            ItemStack s = cItems.get(i);
            if (s == null) continue;
            int ix, iy;
            if (hor) { ix = 2 + i * cell + (cell - 16) / 2; iy = 3; }
            else { ix = 5; iy = 2 + i * 18; }
            mc.getRenderItem().renderItemAndEffectIntoGUI(s, ix, iy);
            if (bar.bool && s.isItemStackDamageable()) {
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, s, ix, iy, "");
            }
        }
        RenderHelper.disableStandardItemLighting();

        // pasada 2: textos
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
        GlStateManager.disableDepth();
        boolean flash = (System.currentTimeMillis() / 300L) % 2L == 0L;
        for (int i = 0; i < count; i++) {
            String t = cLabels[i];
            if (t == null || cItems.get(i) == null) continue;
            int c = (cWarn[i] && flash) ? 0xFFFFFFFF : cColors[i];
            if (hor) {
                int tw = mc.fontRendererObj.getStringWidth(t);
                mc.fontRendererObj.drawString(t, 2 + i * cell + (cell - tw) / 2, 21, c, shadow.bool);
            } else {
                mc.fontRendererObj.drawString(t, 25, 2 + i * 18 + 4, c, shadow.bool);
            }
        }
        GlStateManager.enableDepth();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
