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

    private boolean horizontal() { return layout.index() == 1; }

    private static ItemStack damaged(ItemStack s, int dmg) {
        s.setItemDamage(dmg);
        return s;
    }

    private List<ItemStack> slots() {
        List<ItemStack> out = new ArrayList<ItemStack>();
        EntityPlayer p = mc.thePlayer;
        boolean sample = isEditing();
        if (held.bool) out.add(p != null ? p.getCurrentEquippedItem() : null);
        for (int i = 3; i >= 0; i--) out.add(p != null ? p.inventory.armorInventory[i] : null);
        if (sample) {
            ItemStack[] demo = {
                damaged(new ItemStack(Items.diamond_sword), 120), new ItemStack(Items.diamond_helmet),
                damaged(new ItemStack(Items.diamond_chestplate), 60), damaged(new ItemStack(Items.diamond_leggings), 150),
                damaged(new ItemStack(Items.diamond_boots), 200)
            };
            int off = held.bool ? 0 : 1;
            for (int i = 0; i < out.size(); i++) if (out.get(i) == null) out.set(i, demo[i + off]);
        }
        if (!fixed.bool) {
            for (int i = out.size() - 1; i >= 0; i--) if (out.get(i) == null) out.remove(i);
        }
        if (arrows.bool && p != null && p.getCurrentEquippedItem() != null && p.getCurrentEquippedItem().getItem() instanceof ItemBow) {
            int n = 0;
            for (ItemStack st : p.inventory.mainInventory) if (st != null && st.getItem() == Items.arrow) n += st.stackSize;
            if (n > 0) out.add(new ItemStack(Items.arrow, n));
        } else if (arrows.bool && sample) {
            out.add(new ItemStack(Items.arrow, 42));
        }
        return out;
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

    private int labelColor(ItemStack s) {
        if (!s.isItemStackDamageable()) return color.getColor();
        int max = Math.max(1, s.getMaxDamage());
        float frac = Math.max(0f, (float) (max - s.getItemDamage()) / max);
        if (warn.bool && frac <= 0.10f && (System.currentTimeMillis() / 300L) % 2L == 0L) return 0xFFFFFFFF;
        return Color.HSBtoRGB(frac * 0.33f, 0.9f, 1f);
    }

    private int maxLabelWidth(List<ItemStack> l) {
        int w = 0;
        for (ItemStack s : l) {
            if (s == null) continue;
            String t = label(s);
            if (t != null) w = Math.max(w, mc.fontRendererObj.getStringWidth(t));
        }
        return w;
    }

    private int cell(List<ItemStack> l) { return Math.max(20, maxLabelWidth(l) + 4); }

    @Override
    public int getWidth() {
        List<ItemStack> l = slots();
        int n = Math.max(1, l.size());
        if (horizontal()) return n * cell(l) + 4;
        int tw = maxLabelWidth(l);
        return tw > 0 ? Math.max(34, 25 + tw + 6) : 26;
    }

    @Override
    public int getHeight() {
        List<ItemStack> l = slots();
        int n = Math.max(1, l.size());
        if (horizontal()) return 22 + (maxLabelWidth(l) > 0 ? 10 : 0);
        return n * 18 + 4;
    }

    @Override
    protected void draw() {
        List<ItemStack> l = slots();
        box(getWidth(), getHeight());
        int cell = cell(l);
        for (int i = 0; i < l.size(); i++) {
            ItemStack s = l.get(i);
            if (s == null) continue;
            int ix, iy;
            if (horizontal()) { ix = 2 + i * cell + (cell - 16) / 2; iy = 3; }
            else { ix = 5; iy = 2 + i * 18; }
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(s, ix, iy);
            if (bar.bool && s.isItemStackDamageable()) {
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, s, ix, iy, "");
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            GlStateManager.disableLighting();
            GlStateManager.enableAlpha();
            GlStateManager.disableDepth();
            String t = label(s);
            if (t != null) {
                int c = labelColor(s);
                if (horizontal()) {
                    int tw = mc.fontRendererObj.getStringWidth(t);
                    mc.fontRendererObj.drawString(t, 2 + i * cell + (cell - tw) / 2, 21, c, shadow.bool);
                } else {
                    mc.fontRendererObj.drawString(t, 25, iy + 4, c, shadow.bool);
                }
            }
            GlStateManager.enableDepth();
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
