package com.atenas.client.social;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

/**
 * Caras (cabezas) de los jugadores para los avisos. Se bajan una sola vez por nick, en un hilo aparte, de la misma pagina que usa
 * el launcher (mc-heads.net). Mientras llegan, o si no hay conexion, el aviso muestra la inicial del nick. Solo el hilo principal
 * llama a {@link #get}.
 */
final class Faces {
    private static final int SIZE = 40;
    private static final Map<String, ResourceLocation> READY = new HashMap<String, ResourceLocation>();
    private static final Set<String> BUSY = new HashSet<String>();

    private Faces() {}

    /** La textura de la cara, o null si todavia no llego (o no se pudo bajar). */
    static ResourceLocation get(String name) {
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
                    if (img == null) return;
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override public void run() {
                            READY.put(key, Minecraft.getMinecraft().getTextureManager()
                                    .getDynamicTextureLocation("atenas_face_" + key, new DynamicTexture(img)));
                        }
                    });
                } catch (Exception ignored) { /* sin cara: se ve la inicial */ }
            }
        }, "Atenas-Face");
        t.setDaemon(true);
        t.start();
        return null;
    }
}
