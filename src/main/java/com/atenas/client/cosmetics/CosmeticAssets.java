package com.atenas.client.cosmetics;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.imageio.ImageIO;

import com.atenas.client.AtenasClient;
import com.atenas.client.account.Accounts;
import com.atenas.client.social.SocialClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

/**
 * Descarga las texturas de los cosmeticos del catalogo (/cosmetics/asset) y las convierte en texturas dinamicas.
 * Cada textura se guarda en disco (carpeta cosmetics de .atenasclient) con su version en el nombre: si no cambia, no se vuelve a bajar.
 * La descarga y la decodificacion van en un hilo aparte; solo crear la textura de OpenGL pasa en el hilo principal.
 * Mientras tanto el cosmetico se dibuja con la textura de su modelo base (nunca queda invisible ni rompe).
 */
public final class CosmeticAssets {
    private static final int MAX_BYTES = 1100000;   // el servicio no guarda texturas de mas de 1 MB
    private static final int MAX_SIDE = 4096;
    private static final Set<String> BUSY = ConcurrentHashMap.newKeySet();
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Atenas-CosmeticAssets");
            t.setDaemon(true);
            return t;
        }
    });

    private CosmeticAssets() {}

    static File dir() {
        File d = new File(Accounts.root(), "cosmetics");
        d.mkdirs();
        return d;
    }

    /** Pide la textura (y el icono) de un cosmetico del catalogo si la version cargada no es la que toca. HILO PRINCIPAL. */
    public static void request(final Cosmetics.Item item) {
        if (item == null || !item.custom || item.loadedVer == item.wantVer) return;
        final String key = item.key();
        final int ver = item.wantVer;
        if (!BUSY.add(key + "#" + ver)) return;
        POOL.execute(new Runnable() {
            @Override public void run() {
                try {
                    final BufferedImage tex = fetch(key, "tex", ver, true);
                    BufferedImage ic = null;
                    try { ic = fetch(key, "icon", ver, false); } catch (IOException ignored) { /* el icono es opcional */ }
                    final BufferedImage icon = ic;
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override public void run() { install(key, ver, tex, icon); }
                    });
                } catch (Exception e) {
                    System.err.println("[AtenasClient] textura " + key + ": " + e.getMessage());
                } finally {
                    BUSY.remove(key + "#" + ver);
                }
            }
        });
    }

    /** Vuelve a pedir lo que fallo (sin internet, servicio caido...). HILO PRINCIPAL. */
    public static void retryMissing() {
        for (Cosmetics.Item i : Cosmetics.ALL) request(i);
    }

    private static void install(String key, int ver, BufferedImage tex, BufferedImage icon) {
        int c = key.indexOf(':');
        Cosmetics.Type type = null;
        for (Cosmetics.Type t : Cosmetics.Type.values()) if (t.key().equals(key.substring(0, c))) type = t;
        if (type == null) return;
        Cosmetics.Item item = Cosmetics.find(type, key.substring(c + 1));
        if (item == null || item.wantVer != ver) return; // se quito o ya hay una version mas nueva
        TextureManager tm = Minecraft.getMinecraft().getTextureManager();
        String base = "atenas_" + type.key() + "_" + item.id + "_v" + ver;
        ResourceLocation oldTex = item.dynTex, oldIcon = item.dynIcon;
        item.dynTex = tm.getDynamicTextureLocation(base, new DynamicTexture(tex));
        item.texture = item.dynTex;
        if (icon != null) {
            item.dynIcon = tm.getDynamicTextureLocation(base + "_icon", new DynamicTexture(icon));
            item.icon = item.dynIcon;
        }
        item.loadedVer = ver;
        if (oldTex != null) tm.deleteTexture(oldTex);
        if (oldIcon != null && icon != null) tm.deleteTexture(oldIcon);
    }

    // ------------------------------------------------------------------ disco / red

    /** Del disco si ya esta; si no, del servicio (y se guarda). Devuelve la imagen ya decodificada. */
    private static BufferedImage fetch(String key, String kind, int ver, boolean required) throws IOException {
        String safe = key.replace(':', '_');
        File f = new File(dir(), safe + "_" + kind + "_v" + ver + ".png");
        byte[] data = null;
        if (f.isFile() && f.length() > 0 && f.length() <= MAX_BYTES) {
            try { data = readAll(new FileInputStream(f)); } catch (IOException ignored) { data = null; }
        }
        if (data == null) {
            data = download(key, kind, ver);
            File tmp = new File(f.getPath() + ".tmp");
            OutputStream os = new FileOutputStream(tmp);
            try { os.write(data); } finally { os.close(); }
            if (f.exists()) f.delete();
            tmp.renameTo(f);
            // las versiones viejas de este mismo cosmetico ya no sirven
            File[] all = dir().listFiles();
            if (all != null) {
                String prefix = safe + "_" + kind + "_v";
                for (File o : all) if (o.getName().startsWith(prefix) && !o.equals(f)) o.delete();
            }
        }
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
        if (img == null || img.getWidth() < 1 || img.getHeight() < 1 || img.getWidth() > MAX_SIDE || img.getHeight() > MAX_SIDE) {
            f.delete();
            throw new IOException("imagen no valida");
        }
        return img;
    }

    private static byte[] download(String key, String kind, int ver) throws IOException {
        String u = SocialClient.baseUrl() + "/cosmetics/asset?key=" + key + "&kind=" + kind + "&v=" + ver;
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "AtenasClient/" + AtenasClient.VERSION);
        int code = c.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code);
        return readAll(c.getInputStream());
    }

    private static byte[] readAll(InputStream is) throws IOException {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bo.write(buf, 0, n);
                if (bo.size() > MAX_BYTES) throw new IOException("textura demasiado grande");
            }
            return bo.toByteArray();
        } finally {
            is.close();
        }
    }
}
