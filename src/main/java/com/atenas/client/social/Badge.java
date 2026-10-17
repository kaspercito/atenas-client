package com.atenas.client.social;

import java.util.Locale;

import org.lwjgl.opengl.GL11;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.cosmetics.CosmeticSync;
import com.atenas.client.gui.Theme;
import com.atenas.client.modules.BadgeModule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Logo de Atenas: sobre el nametag de quien usa el cliente/mod (este evento) y en el TAB (ver MixinGuiPlayerTabOverlay). */
public final class Badge {

    /** true si ese jugador usa el cliente o el mod (el servicio de cosmeticos lo conoce) o eres tu mismo. */
    public static boolean isUser(String name) {
        if (name == null) return false;
        if (name.equalsIgnoreCase(SocialClient.myName())) return true;
        return CosmeticSync.idsOf(name.toLowerCase(Locale.ROOT)) != null;
    }

    /** Busca en un texto del TAB (puede traer prefijos/colores de equipo) una palabra que sea un usuario de Atenas. */
    public static boolean textHasUser(String formatted) {
        if (formatted == null || formatted.isEmpty()) return false;
        String plain = formatted.replaceAll("\u00a7.", "");
        for (String tok : plain.split("[^A-Za-z0-9_]+")) {
            if (tok.length() >= 3 && isUser(tok)) return true;
        }
        return false;
    }

    /** Logo de 8x8 en pantalla (coordenadas de GUI). */
    public static void drawIcon(float x, float y, float size) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(Theme.LOGO);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        float k = size / 256f;
        GlStateManager.scale(k, k, 1f);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0f, 0f, 256, 256, 256f, 256f);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onNametag(RenderLivingEvent.Specials.Post e) {
        if (!(e.entity instanceof EntityPlayer)) return;
        BadgeModule m = ModuleManager.find(BadgeModule.class);
        if (m == null || !m.inNametag()) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = (EntityPlayer) e.entity;
        EntityPlayerSP me = mc.thePlayer;
        if (me == null || p == me || p.isInvisibleToPlayer(me) || p.isSneaking() || !Minecraft.isGuiEnabled()) return;
        double d2 = p.getDistanceSqToEntity(me);
        if (d2 > 64.0 * 64.0 || !isUser(p.getName())) return;

        float yOff = p.height + 0.5f;
        if (mc.theWorld != null && d2 < 100.0 && mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(2) != null) {
            yOff += mc.fontRendererObj.FONT_HEIGHT * 1.15f * 0.02666667f;
        }
        int nameW = mc.fontRendererObj.getStringWidth(p.getDisplayName().getFormattedText());
        float vy = mc.getRenderManager().playerViewY, vx = mc.getRenderManager().playerViewX;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) e.x, (float) e.y + yOff - (p.isChild() ? p.height / 2f : 0f), (float) e.z);
        GL11.glNormal3f(0f, 1f, 0f);
        GlStateManager.rotate(-vy, 0f, 1f, 0f);
        GlStateManager.rotate(vx * (mc.gameSettings.thirdPersonView == 2 ? -1f : 1f), 1f, 0f, 0f);
        GlStateManager.scale(-0.02666667f, -0.02666667f, 0.02666667f);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        drawIcon(-nameW / 2f - 12f, -2f, 10f);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }


}
