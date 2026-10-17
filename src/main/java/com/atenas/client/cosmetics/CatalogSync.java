package com.atenas.client.cosmetics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.atenas.client.social.SocialClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Catalogo de cosmeticos del servicio: reglas de exclusividad de los incorporados (ej. la mascota Catalina) y cosmeticos nuevos
 * con textura. Se pide cada 45 s con la ultima revision que tenemos (si no cambio, el servicio contesta {unchanged:true}) y se guarda
 * en disco: sin internet el cliente arranca con el ultimo catalogo conocido. La aplicacion (Cosmetics.applyCatalog) va en el hilo principal.
 */
public class CatalogSync {
    public static final long PERIOD_MS = 45000L;

    private static final AtomicBoolean BUSY = new AtomicBoolean(false);
    private static String rev = "";
    private static List<Cosmetics.Def> last = new ArrayList<Cosmetics.Def>();
    private static long lastTry;
    private int tick;

    private static File cacheFile() { return new File(CosmeticAssets.dir(), "catalog.json"); }

    /** Lee el catalogo guardado en disco y lo aplica (sin red). Llamar una vez al iniciar, con la config ya cargada. HILO PRINCIPAL. */
    public static void loadCache() {
        File f = cacheFile();
        if (!f.isFile()) return;
        Reader r = null;
        try {
            r = new InputStreamReader(new FileInputStream(f), "UTF-8");
            JsonObject o = new JsonParser().parse(r).getAsJsonObject();
            List<Cosmetics.Def> defs = parse(o.getAsJsonArray("items"));
            rev = o.has("rev") ? o.get("rev").getAsString() : "";
            last = defs;
            Cosmetics.applyCatalog(defs);
        } catch (Exception e) {
            System.err.println("[AtenasClient] catalogo en disco: " + e);
            rev = "";
        } finally {
            if (r != null) try { r.close(); } catch (IOException ignored) { }
        }
    }

    /** Cambiaste de cuenta: las reglas privadas dependen del nick, asi que se vuelve a aplicar y se pide el catalogo ya. */
    public static void accountChanged() {
        lastTry = 0;
        if (!last.isEmpty()) Cosmetics.applyCatalog(last);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ++tick % 20 != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getSession() == null || !CosmeticSync.enabled) return;
        if (!SocialClient.validName(SocialClient.myName())) return;
        long now = System.currentTimeMillis();
        if (now - lastTry < PERIOD_MS || !BUSY.compareAndSet(false, true)) return;
        lastTry = now;
        final String sent = rev;
        Thread th = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    JsonObject body = new JsonObject();
                    body.addProperty("rev", sent);
                    final JsonObject res = SocialClient.post("/cosmetics/catalog", body);
                    if (res.has("unchanged") && res.get("unchanged").getAsBoolean()) {
                        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                            @Override public void run() { CosmeticAssets.retryMissing(); } // por si alguna textura fallo
                        });
                        return;
                    }
                    final List<Cosmetics.Def> defs = parse(res.getAsJsonArray("items"));
                    final String newRev = res.has("rev") ? res.get("rev").getAsString() : "";
                    save(newRev, res.getAsJsonArray("items"));
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override public void run() {
                            rev = newRev;
                            last = defs;
                            Cosmetics.applyCatalog(defs);
                        }
                    });
                } catch (Exception ex) {
                    lastTry = System.currentTimeMillis() - PERIOD_MS + 10000L; // reintenta en ~10 s
                    System.err.println("[AtenasClient] catalogo: " + ex.getMessage());
                } finally {
                    BUSY.set(false);
                }
            }
        }, "Atenas-Catalog");
        th.setDaemon(true);
        th.start();
    }

    private static void save(String newRev, JsonArray items) {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("rev", newRev);
            o.add("items", items);
            File f = cacheFile();
            File tmp = new File(f.getPath() + ".tmp");
            OutputStream os = new FileOutputStream(tmp);
            try { os.write(o.toString().getBytes("UTF-8")); } finally { os.close(); }
            if (f.exists()) f.delete();
            tmp.renameTo(f);
        } catch (IOException ex) {
            System.err.println("[AtenasClient] no se pudo guardar el catalogo: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ JSON

    static List<Cosmetics.Def> parse(JsonArray arr) {
        List<Cosmetics.Def> out = new ArrayList<Cosmetics.Def>();
        if (arr == null) return out;
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            try {
                Cosmetics.Type type = null;
                String ts = str(o, "type");
                for (Cosmetics.Type t : Cosmetics.Type.values()) if (t.key().equals(ts)) type = t;
                String id = str(o, "id");
                if (type == null || id == null || !id.matches("[a-z0-9_]{1,24}")) continue;
                Set<String> owners = null;
                if (o.has("owners") && o.get("owners").isJsonArray()) {
                    owners = new HashSet<String>();
                    for (JsonElement n : o.getAsJsonArray("owners")) owners.add(n.getAsString().toLowerCase(Locale.ROOT));
                }
                String name = str(o, "name");
                out.add(new Cosmetics.Def(type, id, name == null ? id : name,
                        o.has("color") && !o.get("color").isJsonNull() ? (int) o.get("color").getAsLong() : 0xFF8B5CF6,
                        bool(o, "glow"), str(o, "base"), o.has("frames") ? o.get("frames").getAsInt() : 1,
                        "private".equals(str(o, "scope")) ? "private" : "global", bool(o, "custom"),
                        o.has("ver") ? o.get("ver").getAsInt() : 0, owners));
            } catch (RuntimeException ex) {
                System.err.println("[AtenasClient] entrada del catalogo ignorada: " + ex);
            }
        }
        return out;
    }

    private static String str(JsonObject o, String k) {
        return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : null;
    }

    private static boolean bool(JsonObject o, String k) {
        if (!o.has(k) || o.get(k).isJsonNull()) return false;
        try { return o.get(k).getAsBoolean(); } catch (RuntimeException e) { return o.get(k).getAsInt() != 0; }
    }
}
