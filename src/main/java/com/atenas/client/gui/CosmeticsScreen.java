package com.atenas.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import com.atenas.client.core.Config;
import com.atenas.client.cosmetics.CosmeticSync;
import com.atenas.client.cosmetics.Cosmetics;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;

/**
 * Pantalla de cosmeticos con el MISMO diseno que la de 1.20.1: panel centrado con titulo, pestanas (capas, alas, sombreros,
 * mascotas), cuadricula de tarjetas con "Ninguno", vista previa a la derecha, "Animadas" y "Listo".
 */
public class CosmeticsScreen extends GuiScreen {
    private static final int CW = 62, CH = 58, GAP = 6, PREVIEW_W = 118;

    private final GuiScreen parent;
    private Cosmetics.Type tab = Cosmetics.Type.CAPE;
    private int px, py, pw, ph, gx, gy, gw, gh, scroll;
    private float spinAngle;
    private long spinLast = System.currentTimeMillis();
    private AbstractClientPlayer mannequin;
    private int mannequinFails;
    private int tabW;

    public CosmeticsScreen(GuiScreen parent) { this.parent = parent; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(false);
        CosmeticSync.refreshNow(); // un cambio hecho en 1.20.1 aparece al abrir
        pw = Math.min(width - 16, 440);
        ph = Math.min(height - 16, 270);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        gx = px + 8;
        gy = py + 52;
        gw = pw - PREVIEW_W - 24;
        gh = ph - 60;
        tabW = Math.min(70, (gw - 8) / Cosmetics.Type.values().length - 4);
    }

    private int cols() { return Math.max(1, (gw + GAP) / (CW + GAP)); }

    private List<Cosmetics.Item> entries() {
        List<Cosmetics.Item> l = new ArrayList<Cosmetics.Item>();
        l.add(null); // "Ninguno"
        l.addAll(Cosmetics.visible(tab)); // sin los privados ajenos
        return l;
    }

    private int maxScroll() {
        int rows = (entries().size() + cols() - 1) / cols();
        return Math.max(0, rows * (CH + GAP) - gh);
    }

    private boolean in(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        int ti = 0;
        for (Cosmetics.Type t : Cosmetics.Type.values()) {
            if (in(mx, my, gx + ti * (tabW + 4), py + 26, tabW, 18)) { tab = t; scroll = 0; return; }
            ti++;
        }
        int bx = px + pw - PREVIEW_W - 4, bw = PREVIEW_W - 4;
        if (in(mx, my, bx, py + ph - 48, bw, 18)) { Cosmetics.animate = !Cosmetics.animate; Config.save(); return; }
        if (in(mx, my, bx, py + ph - 26, bw, 18)) { mc.displayGuiScreen(parent); return; }
        if (in(mx, my, gx, gy, gw, gh)) {
            List<Cosmetics.Item> list = entries();
            for (int i = 0; i < list.size(); i++) {
                int x = gx + (i % cols()) * (CW + GAP), y = gy + (i / cols()) * (CH + GAP) - scroll;
                if (in(mx, my, x, y, CW, CH)) { Cosmetics.select(tab, list.get(i)); return; }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) scroll = Math.max(0, Math.min(maxScroll(), scroll - (d > 0 ? 24 : -24)));
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private void button(String label, int x, int y, int w, int h, boolean primary, boolean hot) {
        Draw.card(x, y, w, h, primary ? Theme.VIOLET : hot ? Theme.PANEL2 : Theme.PANEL, hot ? Theme.ACCENT : Theme.BORDER);
        Draw.centered(label, x + w / 2, y + (h - 8) / 2, Theme.TEXT);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        if (mc.theWorld != null) drawGradientRect(0, 0, width, height, 0xC0070914, 0xE00E0B22);
        else drawDefaultBackground();
        for (int k = 3; k >= 1; k--) Draw.chamfer(px - k * 2, py - k * 2, pw + k * 4, ph + k * 4, 6 + k * 2, ((0x16 / k) << 24) | 0x8B5CF6);
        Draw.chamfer(px, py, pw, ph, 6, 0xF4090818);
        Draw.chamferBorder(px, py, pw, ph, 6, 0xFF3A3466);
        Draw.gradient(px + 6, py, pw - 12, 2, Theme.ACCENT, Theme.PINK, false);
        Draw.heading("Cosm\u00e9ticos", px + 10, py + 8, Theme.TEXT);
        int hw = Draw.headingWidth("Cosm\u00e9ticos");
        boolean warn = Cosmetics.notice.length() > 0 && System.currentTimeMillis() < Cosmetics.noticeUntil;
        String problem = warn ? null : CosmeticSync.problem();
        if (problem != null) warn = true;
        Draw.text(Draw.fit(problem != null ? problem : warn ? Cosmetics.notice : "Los ven todos los que usan Atenas", pw - hw - 30),
                px + 10 + hw + 10, py + 8, warn ? Theme.AMBER : Theme.DIM);

        // pestanas
        int ti = 0;
        for (Cosmetics.Type t : Cosmetics.Type.values()) {
            int x = gx + ti * (tabW + 4);
            boolean on = t == tab, hot = in(mx, my, x, py + 26, tabW, 18);
            Draw.card(x, py + 26, tabW, 18, on ? Theme.PANEL2 : hot ? Theme.HOVER : Theme.PANEL, on || hot ? Theme.ACCENT : Theme.BORDER);
            Draw.centered(t.label, x + tabW / 2, py + 31, on ? Theme.TEXT : Theme.DIM);
            if (on) Draw.rect(x, py + 44, tabW, 2, Theme.ACCENT);
            ti++;
        }

        // cuadricula
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        Draw.scissor(gx, gy, gw, gh);
        List<Cosmetics.Item> list = entries();
        Cosmetics.Item cur = Cosmetics.selected(tab);
        Cosmetics.Item hovered = null;
        for (int i = 0; i < list.size(); i++) {
            Cosmetics.Item it = list.get(i);
            int x = gx + (i % cols()) * (CW + GAP), y = gy + (i / cols()) * (CH + GAP) - scroll;
            if (y + CH < gy || y > gy + gh) continue;
            boolean on = it == cur, hot = in(mx, my, x, y, CW, CH) && my >= gy && my < gy + gh;
            Draw.card(x, y, CW, CH, hot ? Theme.PANEL2 : Theme.PANEL, on ? Theme.ACCENT : hot ? Theme.ACCENT_DIM : Theme.BORDER);
            if (it == null) {
                Draw.centered("\u2715", x + CW / 2, y + 16, Theme.DIM);
                Draw.centered("Ninguno", x + CW / 2, y + CH - 14, on ? Theme.TEXT : Theme.DIM);
            } else {
                mc.getTextureManager().bindTexture(it.icon);
                GlStateManager.enableBlend();
                GlStateManager.color(1f, 1f, 1f, 1f);
                net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x + (CW - 32) / 2, y + 6, 0f, 0f, 32, 32, 32f, 32f);
                Draw.centered(Draw.fit(it.name, CW - 4), x + CW / 2, y + CH - 14, on ? Theme.TEXT : Theme.DIM);
                if (hot) hovered = it;
            }
            if (on) Draw.rect(x + CW - 9, y + 3, 6, 6, Theme.ACCENT);
        }
        Draw.endScissor();

        // vista previa
        int rx = px + pw - PREVIEW_W - 4, ry = py + 26, rw = PREVIEW_W - 4, rh = ph - 80;
        Draw.card(rx, ry, rw, rh, Theme.INK, Theme.BORDER);
        int cx = rx + rw / 2, feet = ry + rh - 22;
        long now = System.currentTimeMillis();
        spinAngle = (spinAngle + (now - spinLast) * 0.06f) % 360f;
        spinLast = now;
        AbstractClientPlayer who = mc.thePlayer != null ? mc.thePlayer : mannequin();
        if (who != null) {
            Draw.scissor(rx + 1, ry + 1, rw - 2, rh - 2);
            drawPlayer(who, cx - 10, feet, 42);
            Draw.endScissor();
        } else {
            Draw.centered("Entra a un mundo", cx, ry + rh / 2 - 8, Theme.TEXT);
            Draw.centered("para verte puesto", cx, ry + rh / 2 + 4, Theme.DIM);
        }
        StringBuilder sb = new StringBuilder();
        for (Cosmetics.Type t : Cosmetics.Type.values()) {
            Cosmetics.Item s = Cosmetics.selected(t);
            if (s != null) { if (sb.length() > 0) sb.append(" \u00b7 "); sb.append(s.name); }
        }
        List<String> lines = fontRendererObj.listFormattedStringToWidth(sb.length() == 0 ? "Sin cosm\u00e9ticos" : sb.toString(), rw - 6);
        int shown = Math.min(2, lines.size()), ly = ry + rh - 3 - shown * 9;
        for (int li = 0; li < shown; li++) Draw.centered(lines.get(li), cx, ly + li * 9, Theme.DIM);

        button("Animadas: " + (Cosmetics.animate ? "S\u00ed" : "No"), rx, py + ph - 48, rw, 18, false, in(mx, my, rx, py + ph - 48, rw, 18));
        button("Listo", rx, py + ph - 26, rw, 18, true, in(mx, my, rx, py + ph - 26, rw, 18));
        if (hovered != null && Draw.width(hovered.name) > CW - 4) drawHoveringText(java.util.Collections.singletonList(hovered.name), mx, my);
    }

    /** Jugador girando para ver capa, alas y sombrero desde todos los lados. */
    private void drawPlayer(AbstractClientPlayer ent, int posX, int posY, int scale) {
        float spin = spinAngle;
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY, 50f);
        GlStateManager.scale((float) -scale, (float) scale, (float) scale);
        GlStateManager.rotate(180f, 0f, 0f, 1f);
        float rYawOff = ent.renderYawOffset, rYaw = ent.rotationYaw, rPitch = ent.rotationPitch;
        float rPrevHead = ent.prevRotationYawHead, rHead = ent.rotationYawHead;
        float rPrevOff = ent.prevRenderYawOffset;
        GlStateManager.rotate(135f, 0f, 1f, 0f);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135f, 0f, 1f, 0f);
        ent.renderYawOffset = spin;
        ent.prevRenderYawOffset = spin;
        ent.rotationYaw = spin;
        ent.rotationPitch = 0f;
        ent.rotationYawHead = spin;
        ent.prevRotationYawHead = spin;
        GlStateManager.translate(0f, 0f, 0f);
        // el dibujo de la interfaz deja el color/estado en cualquier valor: sin esto el jugador sale oscuro o negro sobre fondo oscuro
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f); // brillo completo: no se ve oscuro
        RenderManager rm = mc.getRenderManager();
        rm.setPlayerViewY(180f);
        rm.setRenderShadow(false);
        Cosmetics.inPreview = true;
        try {
            rm.renderEntityWithPosYaw(ent, 0.0, 0.0, 0.0, 0f, 1f);
        } catch (RuntimeException ex) {
            // sin mundo, el nombre del maniqui puede fallar al final del dibujo (el cuerpo ya se dibujo): no es un error
            // no se descarta el maniqui: asi la vista previa no desaparece a los pocos cuadros
        }
        rm.setRenderShadow(true);
        Cosmetics.inPreview = false;
        ent.renderYawOffset = rYawOff;
        ent.prevRenderYawOffset = rPrevOff;
        ent.rotationYaw = rYaw;
        ent.rotationPitch = rPitch;
        ent.prevRotationYawHead = rPrevHead;
        ent.rotationYawHead = rHead;
        // la mascota elegida, a un lado (en el mismo marco del jugador, sin girar con el)
        Cosmetics.Item pet = Cosmetics.selected(Cosmetics.Type.PET);
        if (pet != null) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(-135f, 0f, 1f, 0f);
            GlStateManager.rotate(135f, 0f, 1f, 0f);
            com.atenas.client.cosmetics.PetRender.renderPreview(pet);
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /** Jugador de mentira (en un mundo vacio) para la vista previa cuando no hay partida. Devuelve null si no se puede crear. */
    private AbstractClientPlayer mannequin() {
        if (mannequin != null) return mannequin;
        if (mannequinFails > 30 || mc.getSession() == null) return null;
        try {
            net.minecraft.world.WorldSettings ws = new net.minecraft.world.WorldSettings(0L,
                    net.minecraft.world.WorldSettings.GameType.SURVIVAL, false, false, net.minecraft.world.WorldType.DEFAULT);
            net.minecraft.client.multiplayer.WorldClient w = new net.minecraft.client.multiplayer.WorldClient(null, ws, 0,
                    net.minecraft.world.EnumDifficulty.PEACEFUL, mc.mcProfiler);
            mannequin = new net.minecraft.client.entity.EntityOtherPlayerMP(w, mc.getSession().getProfile()) {
                @Override public int getBrightnessForRender(float pt) { return 15728880; } // sin mundo la luz seria 0
                // Sin partida no hay "net handler": estos metodos lo consultan y daban NullPointerException (el jugador no se dibujaba)
                @Override public boolean isSpectator() { return false; }
                @Override public boolean hasPlayerInfo() { return false; }
                @Override public String getSkinType() { return "default"; }
                @Override public net.minecraft.util.ResourceLocation getLocationSkin() { return com.atenas.client.social.Faces.skinOrDefault(); }
                @Override public net.minecraft.util.ResourceLocation getLocationCape() { return null; }
            };
            Cosmetics.previewPlayer = mannequin;
        } catch (Throwable t) {
            mannequinFails = 99;
            System.err.println("[AtenasClient] maniqui de vista previa: " + t);
        }
        return mannequin;
    }
}
