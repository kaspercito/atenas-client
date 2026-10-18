package com.atenas.client.social;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

/**
 * Caras (cabezas) de los jugadores para los avisos. Se bajan una sola vez por nick, en un hilo aparte, de la misma pagina que usa
 * el launcher (mc-heads.net). Mientras llegan, o si no hay conexion, el aviso muestra la inicial del nick. Solo el hilo principal
 * llama a {@link #get}.
 */
public final class Faces {
    private static final int SIZE = 40;
    private static final Map<String, ResourceLocation> READY = new HashMap<String, ResourceLocation>();
    private static final Set<String> BUSY = new HashSet<String>();
    /** Nicks cuya cara no se pudo bajar (no existe, sin conexion...): se dibujan como Steve. Lo escribe el hilo de descarga. */
    private static final Set<String> FAILED = Collections.synchronizedSet(new HashSet<String>());

    private static volatile ResourceLocation ownSkin;
    private static boolean ownAsked;

    private Faces() {}

    /** Skin REAL de la cuenta con la que juegas (se pide una sola vez, en segundo plano). null mientras llega o si la cuenta no tiene. */
    private static ResourceLocation ownSkin() {
        if (ownSkin != null) return ownSkin;
        if (!ownAsked) {
            ownAsked = true;
            final Minecraft mc = Minecraft.getMinecraft();
            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        GameProfile gp = mc.getSession().getProfile();
                        try { GameProfile full = mc.getSessionService().fillProfileProperties(gp, true); if (full != null) gp = full; } catch (Throwable ignored) { /* se prueba con el perfil tal cual */ }
                        final GameProfile profile = gp;
                        mc.addScheduledTask(new Runnable() {
                            @Override public void run() {
                                mc.getSkinManager().loadProfileTextures(profile, new SkinManager.SkinAvailableCallback() {
                                    @Override public void skinAvailable(MinecraftProfileTexture.Type type, ResourceLocation loc, MinecraftProfileTexture tex) {
                                        if (type == MinecraftProfileTexture.Type.SKIN) ownSkin = loc;
                                    }
                                }, false);
                            }
                        });
                    } catch (Throwable ignored) { /* queda la cabeza de mc-heads o Steve */ }
                }
            }, "Atenas-OwnSkin");
            t.setDaemon(true);
            t.start();
        }
        return ownSkin;
    }

    private static boolean isSelf(String name) {
        try { return name != null && name.equalsIgnoreCase(Minecraft.getMinecraft().getSession().getUsername()); } catch (Throwable t) { return false; }
    }

    /** La textura de la cara, o null si todavia no llego (o no se pudo bajar). */
    public static ResourceLocation get(String name) {
        if (name == null || !SocialClient.validName(name)) return null;
        final String key = name.toLowerCase(java.util.Locale.ROOT);
        ResourceLocation rl = READY.get(key);
        if (rl != null || BUSY.contains(key)) return rl;
        BUSY.add(key); // se intenta una vez por sesion: si falla, queda la inicial
        final String shown = name;
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL("https://mc-heads.net/avatar/" + shown + "/" + SIZE).openConnection();
                    c.setConnectTimeout(5000);
                    c.setReadTimeout(6000);
                    c.setRequestProperty("User-Agent", "AtenasClient");
                    InputStream in = c.getInputStream();
                    final BufferedImage img;
                    try { img = ImageIO.read(in); } finally { in.close(); }
                    if (img == null) { FAILED.add(key); return; }
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override public void run() {
                            READY.put(key, Minecraft.getMinecraft().getTextureManager()
                                    .getDynamicTextureLocation("atenas_face_" + key, new DynamicTexture(img)));
                        }
                    });
                } catch (Exception ignored) { FAILED.add(key); /* sin cara: se dibuja Steve */ }
            }
        }, "Atenas-Face");
        t.setDaemon(true);
        t.start();
        return null;
    }

    /** true si ya se sabe que no hay cara para este nick (entonces se usa Steve). */
    public static boolean failed(String name) {
        return name != null && FAILED.contains(name.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Dibuja la cabeza del skin por defecto de la cuenta (Steve o Alex segun el UUID, igual que se ve dentro del juego), a "size"
     * pixeles de GUI. Lee el tamano REAL de la textura: antes se suponia 64x32 y con el Steve de 64x64 se recortaba el torso
     * (salian 4 cuadros de colores en vez de la cara).
     */
    public static void drawSteve(int x, int y, int size, float alpha) {
        drawHead(DefaultPlayerSkin.getDefaultSkinLegacy(), x, y, size, alpha);
    }

    /** Skin por defecto de ESTA cuenta (Steve o Alex segun su UUID): lo que se ve de uno mismo en el juego cuando la cuenta no tiene skin. */
    private static ResourceLocation ownDefaultSkin() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getSession() != null && mc.getSession().getProfile() != null && mc.getSession().getProfile().getId() != null) {
                return DefaultPlayerSkin.getDefaultSkin(mc.getSession().getProfile().getId());
            }
        } catch (Throwable ignored) { /* se queda Steve */ }
        return DefaultPlayerSkin.getDefaultSkinLegacy();
    }

    private static void drawHead(ResourceLocation skin, int x, int y, int size, float alpha) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(skin);
        int tw = org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_TEXTURE_WIDTH);
        int th = org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_TEXTURE_HEIGHT);
        if (tw <= 0 || th <= 0) { tw = 64; th = 64; }
        float k = tw / 64f; // skins HD: las coordenadas se escalan
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, alpha);
        Gui.drawScaledCustomSizeModalRect(x, y, 8f * k, 8f * k, (int) (8 * k), (int) (8 * k), size, size, (float) tw, (float) th);
        Gui.drawScaledCustomSizeModalRect(x, y, 40f * k, 8f * k, (int) (8 * k), (int) (8 * k), size, size, (float) tw, (float) th);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /**
     * Dibuja la cabeza del jugador. "steve" = true fuerza a Steve (cuenta sin conexion: no tiene skin que bajar). Devuelve false
     * mientras la cara todavia esta llegando; entonces quien llama pone la inicial. Si la descarga falla, se dibuja Steve.
     */
    public static boolean draw(String name, int x, int y, int size, float alpha, boolean steve) {
        if (steve) { drawHead(ownDefaultSkin(), x, y, size, alpha); return true; } // cuenta sin conexion (la tuya): su skin por defecto real
        if (name == null || !SocialClient.validName(name)) { drawSteve(x, y, size, alpha); return true; }
        if (isSelf(name)) { ResourceLocation own = ownSkin(); if (own != null) { drawHead(own, x, y, size, alpha); return true; } }
        ResourceLocation face = get(name);
        if (face != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(face);
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, alpha);
            Gui.drawScaledCustomSizeModalRect(x, y, 0f, 0f, SIZE, SIZE, size, size, (float) SIZE, (float) SIZE);
            GlStateManager.color(1f, 1f, 1f, 1f);
            return true;
        }
        if (failed(name)) { drawSteve(x, y, size, alpha); return true; }
        return false;
    }
}
