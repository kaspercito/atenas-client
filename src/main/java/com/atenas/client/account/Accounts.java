package com.atenas.client.account;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.atenas.client.cosmetics.CosmeticSync;
import com.atenas.client.social.SocialClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * Cuentas del launcher dentro del juego.
 *
 * - Lee la LISTA de cuentas que el launcher guarda en config/accounts.json (solo tipo, nick y uuid; los tokens van cifrados
 *   y este codigo nunca los lee ni los muestra).
 * - Cuenta sin conexion: se cambia en caliente, sin reiniciar (se reemplaza la Session del juego).
 * - Cuenta de Microsoft: el token solo lo puede descifrar el launcher, asi que el juego deja config/switch-request.json y se
 *   cierra; el launcher activa esa cuenta y vuelve a abrir el mismo modpack solo.
 */
public final class Accounts {

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    public static final class Entry {
        public String id = "", kind = "microsoft", name = "", uuid = "";
        public boolean offline() { return "offline".equals(kind); }
    }

    private Accounts() {}

    // ------------------------------------------------------------ rutas

    /** Carpeta del launcher: el launcher la pasa con -Datenas.home; si se abre sin launcher, ~/.atenasclient. */
    public static File root() {
        String h = System.getProperty("atenas.home");
        if (h != null && h.trim().length() > 0) return new File(h.trim());
        return new File(System.getProperty("user.home"), ".atenasclient");
    }

    private static File configDir() { return new File(root(), "config"); }

    /** true si el juego lo abrio el launcher (entonces puede reabrirlo con otra cuenta). */
    public static boolean launchedByLauncher() {
        String h = System.getProperty("atenas.home");
        return h != null && h.trim().length() > 0;
    }

    // ------------------------------------------------------------ lectura

    public static boolean validName(String n) { return n != null && NAME.matcher(n).matches(); }

    /** Misma formula que el launcher (OfflinePlayer:<nick>), con guiones. */
    public static String offlineUuid(String name) {
        try {
            byte[] h = MessageDigest.getInstance("MD5").digest(("OfflinePlayer:" + name).getBytes("UTF-8"));
            h[6] = (byte) ((h[6] & 0x0f) | 0x30);
            h[8] = (byte) ((h[8] & 0x3f) | 0x80);
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b & 255));
            String x = sb.toString();
            return x.substring(0, 8) + "-" + x.substring(8, 12) + "-" + x.substring(12, 16) + "-" + x.substring(16, 20) + "-" + x.substring(20);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String dashless(String u) { return u == null ? "" : u.replace("-", "").toLowerCase(Locale.ROOT); }

    /** Cuentas guardadas en el launcher (la mas reciente primero). Nunca lanza: si no hay archivo, lista vacia. */
    public static List<Entry> list() {
        List<Entry> out = new ArrayList<Entry>();
        File f = new File(configDir(), "accounts.json");
        if (!f.isFile()) return out;
        try {
            JsonElement root = new JsonParser().parse(readAll(new FileInputStream(f)));
            if (!root.isJsonArray()) return out;
            for (JsonElement el : root.getAsJsonArray()) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String kind = str(o, "kind");
                String name = str(o, "name");
                String uuid = str(o, "uuid");
                if (!"offline".equals(kind) && !"microsoft".equals(kind)) continue;
                if (!validName(name) || uuid.length() == 0) continue;
                Entry e = new Entry();
                e.kind = kind;
                e.name = name;
                e.uuid = uuid;
                e.id = kind + ":" + uuid.toLowerCase(Locale.ROOT); // igual que accounts.js del launcher
                boolean dup = false;
                for (Entry x : out) if (x.id.equals(e.id)) { dup = true; break; }
                if (!dup) out.add(e);
            }
        } catch (Exception ignored) { /* archivo a medio escribir: se muestra vacio */ }
        return out;
    }

    /** true si esta cuenta es la que esta jugando ahora. */
    public static boolean isActive(Entry e) {
        Session s = Minecraft.getMinecraft().getSession();
        if (s == null || e == null) return false;
        String pid = dashless(s.getPlayerID());
        if (pid.length() > 0 && pid.equals(dashless(e.uuid))) return true;
        return pid.length() == 0 && e.name.equalsIgnoreCase(s.getUsername());
    }

    // ------------------------------------------------------------ cambio de cuenta

    /**
     * Cambia a una cuenta SIN CONEXION al instante. Devuelve null si salio bien o el mensaje de error.
     * Ojo: en servidores con online-mode=true esa cuenta no puede entrar (igual que en el launcher).
     */
    public static String switchToOffline(String name) {
        if (!validName(name)) return "El nombre debe tener de 3 a 16 caracteres: letras, numeros o _";
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) return "Sal del mundo o del servidor antes de cambiar de cuenta";
        String id = dashless(offlineUuid(name));
        Session next = new Session(name, id, id, "legacy");
        try {
            ReflectionHelper.setPrivateValue(Minecraft.class, mc, next, "session", "field_71449_j");
        } catch (Throwable t) {
            return "No se pudo cambiar la cuenta dentro del juego. Cambiala en el launcher.";
        }
        afterChange();
        return null;
    }

    /** Cuenta de Microsoft: pide al launcher que la active y reabra el juego. Devuelve null si salio bien o el error. */
    public static String requestLauncherSwitch(Entry e) {
        if (e == null) return "Cuenta no valida";
        if (!launchedByLauncher()) return "Abre el juego desde el launcher para cambiar a una cuenta de Microsoft";
        try {
            File dir = configDir();
            if (!dir.isDirectory() && !dir.mkdirs()) return "No se pudo escribir en la carpeta del launcher";
            JsonObject o = new JsonObject();
            o.addProperty("id", e.id);
            o.addProperty("name", e.name);
            o.addProperty("at", System.currentTimeMillis());
            OutputStream os = new FileOutputStream(new File(dir, "switch-request.json"));
            try { os.write(o.toString().getBytes("UTF-8")); } finally { os.close(); }
            return null;
        } catch (IOException ex) {
            return "No se pudo avisar al launcher: " + ex.getMessage();
        }
    }

    private static void afterChange() {
        SocialClient.onAccountChanged();
        CosmeticSync.markDirty();
    }

    // ------------------------------------------------------------ utilidades

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }

    private static String readAll(InputStream is) throws IOException {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            return new String(bo.toByteArray(), "UTF-8");
        } finally {
            is.close();
        }
    }
}
