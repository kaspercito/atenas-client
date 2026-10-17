package com.atenas.client.cosmetics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.atenas.client.core.Config;
import com.atenas.client.social.SocialClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Comparte tus cosmeticos con otros usuarios de Atenas y descarga los de ellos.
 * Usa el MISMO servicio (y la misma identidad: tu nick) que Amigos y que el mod de 1.20.1, asi que lo que elijas aqui
 * se ve igual en 1.20.1 y viceversa: al abrir el juego se baja lo ultimo que elegiste, estes en la version que estes.
 */
public class CosmeticSync {

    /** Ya no se usa (el servicio es el de Amigos); se mantienen por compatibilidad con config/atenasclient.json. */
    public static final String DEFAULT_URL = "";
    public static String url = DEFAULT_URL;
    public static boolean enabled = true;
    public static String token = "";

    private static final Map<String, String[]> REMOTE = new ConcurrentHashMap<String, String[]>();
    private static final AtomicBoolean PUSH_BUSY = new AtomicBoolean(false);
    private static final AtomicBoolean FETCH_BUSY = new AtomicBoolean(false);
    private static final AtomicBoolean SELF_BUSY = new AtomicBoolean(false);
    /** Empieza en false: primero se baja lo que hay en el servicio y solo se sube si tu cambias algo (o el servicio no te conoce). */
    private static volatile boolean dirty = false;
    /** Sube cada vez que eliges algo: una respuesta del servidor mas vieja que tu ultima eleccion se descarta. */
    private static volatile int rev;
    private static volatile boolean selfKnown;
    private static long lastFetch, lastPush, lastSelf;
    private static String lastNames = "";
    private int tick;

    public static boolean available() { return enabled; }

    public static void ensureToken() { /* la identidad vive en SocialClient (social.json) */ }

    public static void markDirty() { dirty = true; rev++; lastPush = 0; }

    /** Cambiaste de cuenta: se baja lo que ESA cuenta tenia elegido en vez de subirle lo de la anterior. */
    public static void accountChanged() { dirty = false; selfKnown = false; lastSelf = 0; rev++; }

    /** Sube cada vez que cambia lo que sabemos de otros jugadores: Cosmetics vuelve a resolver su cache. */
    private static volatile int stamp;

    public static int stamp() { return stamp; }

    /** Ids de un jugador en el orden de Cosmetics.Type (capa, alas, sombrero, mascota), o null si no lo conocemos. */
    public static String[] idsOf(String name) {
        return name == null ? null : REMOTE.get(name.toLowerCase(Locale.ROOT));
    }

    public static String get(String name, Cosmetics.Type t) {
        String[] v = idsOf(name);
        return v == null ? null : v[t.ordinal()];
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !enabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getSession() == null) return;
        if (++tick % 20 != 0) return; // una vez por segundo
        final String self = SocialClient.myName();
        if (!SocialClient.validName(self)) return;
        long now = System.currentTimeMillis();

        // 1) subir lo que elegiste
        if (dirty && now - lastPush > 3000L && PUSH_BUSY.compareAndSet(false, true)) {
            lastPush = now;
            final int sent = rev;
            final JsonObject o = new JsonObject();
            for (Cosmetics.Type t : Cosmetics.Type.values()) {
                Cosmetics.Item i = Cosmetics.selected(t);
                o.addProperty(t.name().toLowerCase(Locale.ROOT), i == null ? "none" : i.id);
            }
            run("Atenas-CosmeticPush", new Runnable() {
                @Override public void run() {
                    try {
                        SocialClient.post("/cosmetics/set", o);
                        if (sent == rev) dirty = false;
                        selfKnown = true;
                    } catch (IOException ex) {
                        lastPush = System.currentTimeMillis() + 10000L; // reintenta en ~13 s
                        System.err.println("[AtenasClient] push: " + ex.getMessage());
                    } finally {
                        PUSH_BUSY.set(false);
                    }
                }
            });
        }

        // 2) bajar lo TUYO (la primera vez y cada 30 s): asi un cambio hecho en 1.20.1 aparece aqui
        if (!dirty && !PUSH_BUSY.get() && now - lastSelf > (selfKnown ? 30000L : 5000L) && SELF_BUSY.compareAndSet(false, true)) {
            lastSelf = now;
            final int seen = rev;
            run("Atenas-CosmeticSelf", new Runnable() {
                @Override public void run() {
                    try {
                        JsonObject b = new JsonObject();
                        JsonArray arr = new JsonArray();
                        arr.add(new JsonPrimitive(self.toLowerCase(Locale.ROOT)));
                        b.add("names", arr);
                        JsonObject players = SocialClient.post("/cosmetics/get", b).getAsJsonObject("players");
                        final String key = self.toLowerCase(Locale.ROOT);
                        if (players.has(key) && players.get(key).isJsonObject()) {
                            final JsonObject v = players.getAsJsonObject(key);
                            selfKnown = true;
                            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                                @Override public void run() {
                                    if (seen != rev || dirty) return; // elegiste algo mientras tanto: gana lo tuyo
                                    boolean changed = false;
                                    for (Cosmetics.Type t : Cosmetics.Type.values()) {
                                        changed |= Cosmetics.applyRemote(t, str(v, t.name().toLowerCase(Locale.ROOT)));
                                    }
                                    if (changed) Config.save();
                                }
                            });
                        } else if (seen == rev) {
                            selfKnown = true;
                            dirty = true; // el servicio no te conoce todavia: sube lo que tienes
                        }
                    } catch (Exception ex) {
                        System.err.println("[AtenasClient] self: " + ex.getMessage());
                    } finally {
                        SELF_BUSY.set(false);
                    }
                }
            });
        }

        // 3) bajar los de los jugadores cercanos
        if (mc.theWorld == null || mc.thePlayer == null || mc.isSingleplayer()) return;
        List<String> names = new ArrayList<String>();
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            String n = p.getName();
            if (n != null && !n.equalsIgnoreCase(self) && SocialClient.validName(n)) names.add(n.toLowerCase(Locale.ROOT));
        }
        if (names.isEmpty()) return;
        Collections.sort(names);
        if (names.size() > 40) names = names.subList(0, 40);
        final List<String> wanted = new ArrayList<String>(names);
        final String joined = wanted.toString();
        if ((joined.equals(lastNames) && now - lastFetch < 20000L) || !FETCH_BUSY.compareAndSet(false, true)) return;
        lastNames = joined;
        lastFetch = now;
        run("Atenas-CosmeticFetch", new Runnable() {
            @Override public void run() {
                try {
                    JsonObject b = new JsonObject();
                    JsonArray arr = new JsonArray();
                    for (String n : wanted) arr.add(new JsonPrimitive(n));
                    b.add("names", arr);
                    JsonObject players = SocialClient.post("/cosmetics/get", b).getAsJsonObject("players");
                    for (String n : wanted) {
                        if (players.has(n) && players.get(n).isJsonObject()) {
                            JsonObject v = players.getAsJsonObject(n);
                            // un elemento por Cosmetics.Type, en el mismo orden (CAPE, WINGS, HAT, PET)
                            String[] ids = new String[Cosmetics.Type.values().length];
                            for (Cosmetics.Type t : Cosmetics.Type.values()) ids[t.ordinal()] = str(v, t.key());
                            REMOTE.put(n, ids);
                        } else {
                            REMOTE.remove(n);
                        }
                    }
                    stamp++;
                } catch (Exception ex) {
                    lastFetch = 0;
                    System.err.println("[AtenasClient] fetch: " + ex.getMessage());
                } finally {
                    FETCH_BUSY.set(false);
                }
            }
        });
    }

    private static void run(String name, Runnable r) {
        Thread th = new Thread(r, name);
        th.setDaemon(true);
        th.start();
    }

    private static String str(JsonObject o, String k) {
        return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : null;
    }
}
