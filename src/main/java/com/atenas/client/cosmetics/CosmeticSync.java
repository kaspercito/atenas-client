package com.atenas.client.cosmetics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.atenas.client.AtenasClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Comparte tus cosmeticos con otros usuarios de Atenas Client y descarga los de ellos.
 * Usa el Worker de Cloudflare de la carpeta /worker. Envia solo tu nick y los ids elegidos.
 */
public class CosmeticSync {

    /** Pon aqui la URL de tu Worker antes de compilar (o en config/atenasclient.json, "online" > "url"). */
    public static final String DEFAULT_URL = "";

    public static String url = DEFAULT_URL;
    public static boolean enabled = true;
    public static String token = "";

    private static final Map<String, String[]> REMOTE = new ConcurrentHashMap<String, String[]>();
    private static final AtomicBoolean PUSH_BUSY = new AtomicBoolean(false);
    private static final AtomicBoolean FETCH_BUSY = new AtomicBoolean(false);
    private static volatile boolean dirty = true;
    private static long lastFetch, lastPush;
    private static String lastNames = "";
    private int tick;

    public static boolean available() { return url != null && url.trim().length() > 0; }

    public static void ensureToken() {
        if (token == null || token.length() < 16) token = UUID.randomUUID().toString().replace("-", "");
    }

    public static void markDirty() { dirty = true; }

    public static String get(String name, Cosmetics.Type t) {
        if (name == null) return null;
        String[] v = REMOTE.get(name.toLowerCase(Locale.ROOT));
        return v == null ? null : v[t.ordinal()];
    }

    private static String base() {
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !enabled || !available()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null || mc.isSingleplayer()) return;
        if (++tick % 20 != 0) return; // una vez por segundo
        long now = System.currentTimeMillis();
        String self = mc.getSession().getUsername();

        if (dirty && now - lastPush > 15000L && PUSH_BUSY.compareAndSet(false, true)) {
            lastPush = now;
            JsonObject o = new JsonObject();
            o.addProperty("name", self);
            o.addProperty("token", token);
            for (Cosmetics.Type t : Cosmetics.Type.values()) {
                Cosmetics.Item i = Cosmetics.selected(t);
                o.addProperty(t.name().toLowerCase(Locale.ROOT), i == null ? "none" : i.id);
            }
            final String body = o.toString();
            Thread th = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        http("POST", base() + "/set", body);
                        dirty = false;
                    } catch (IOException ex) {
                        if (String.valueOf(ex.getMessage()).startsWith("HTTP 4")) dirty = false;
                        System.err.println("[AtenasClient] push: " + ex.getMessage());
                    } finally {
                        PUSH_BUSY.set(false);
                    }
                }
            }, "Atenas-CosmeticPush");
            th.setDaemon(true);
            th.start();
        }

        List<String> names = new ArrayList<String>();
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            String n = p.getName();
            if (n != null && !n.equalsIgnoreCase(self)) names.add(n.toLowerCase(Locale.ROOT));
        }
        if (names.isEmpty()) return;
        Collections.sort(names);
        if (names.size() > 40) names = names.subList(0, 40);
        final List<String> wanted = new ArrayList<String>(names);
        final StringBuilder sb = new StringBuilder();
        for (String n : wanted) {
            if (sb.length() > 0) sb.append(',');
            sb.append(n);
        }
        final String joined = sb.toString();
        if ((joined.equals(lastNames) && now - lastFetch < 20000L) || !FETCH_BUSY.compareAndSet(false, true)) return;
        lastNames = joined;
        lastFetch = now;
        Thread th = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    String res = http("GET", base() + "/get?names=" + URLEncoder.encode(joined, "UTF-8"), null);
                    JsonObject o = new JsonParser().parse(res).getAsJsonObject();
                    for (String n : wanted) {
                        if (o.has(n) && o.get(n).isJsonObject()) {
                            JsonObject v = o.getAsJsonObject(n);
                            REMOTE.put(n, new String[] {str(v, "cape"), str(v, "wings"), str(v, "hat")});
                        } else {
                            REMOTE.remove(n);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("[AtenasClient] fetch: " + ex.getMessage());
                } finally {
                    FETCH_BUSY.set(false);
                }
            }
        }, "Atenas-CosmeticFetch");
        th.setDaemon(true);
        th.start();
    }

    private static String str(JsonObject o, String k) {
        return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : null;
    }

    private static String http(String method, String target, String body) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(target).openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        c.setRequestMethod(method);
        c.setRequestProperty("User-Agent", "AtenasClient/" + AtenasClient.VERSION);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            OutputStream os = c.getOutputStream();
            try { os.write(body.getBytes("UTF-8")); } finally { os.close(); }
        }
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String res = "";
        if (is != null) {
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[2048];
                int n;
                while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
                res = new String(bo.toByteArray(), "UTF-8");
            } finally {
                is.close();
            }
        }
        if (code >= 400) throw new IOException("HTTP " + code + " " + res);
        return res;
    }
}
