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

    /** Respuesta del launcher a un pedido. */
    public static final class Result {
        public boolean ok;
        public String message = "";
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

    /**
     * true si la cuenta con la que se juega ahora es SIN CONEXION (no premium). Se sabe porque la sesion es \"legacy\" o porque su
     * UUID es el que sale de la formula OfflinePlayer:<nick> (las premium tienen UUID aleatorio). Sin skin que bajar: se ve Steve.
     */
    public static boolean currentIsOffline() {
        Session s = Minecraft.getMinecraft().getSession();
        if (s == null) return true;
        if (s.getSessionType() == Session.Type.LEGACY) return true;
        String pid = dashless(s.getPlayerID());
        return pid.length() == 0 || pid.equals(dashless(offlineUuid(s.getUsername())));
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
        try { if (launchedByLauncher()) request("add-offline", null, name); } catch (IOException ignored) { /* solo para que aparezca en la lista */ }
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

    // ------------------------------------------------------------ pedidos al launcher

    private static File requestFile() { return new File(configDir(), "account-request.json"); }
    private static File resultFile() { return new File(configDir(), "account-result.json"); }

    /** Marca de tiempo de accounts.json: cambia cuando el launcher agrega o quita una cuenta. */
    public static long listStamp() {
        File f = new File(configDir(), "accounts.json");
        return f.isFile() ? f.lastModified() : 0L;
    }

    /**
     * Deja un pedido para el launcher. action: add-microsoft | add-offline | remove.
     * Devuelve el id del pedido (para pedir la respuesta con {@link #result}); lanza IOException con un mensaje listo para mostrar.
     */
    public static String request(String action, String target, String name) throws IOException {
        if (!launchedByLauncher()) throw new IOException("Abre el juego desde el launcher para agregar o quitar cuentas.");
        File dir = configDir();
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("No se pudo escribir en la carpeta del launcher.");
        String id = Long.toHexString(System.nanoTime()) + Long.toHexString(System.currentTimeMillis());
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("action", action);
        if (target != null) o.addProperty("target", target);
        if (name != null) o.addProperty("name", name);
        o.addProperty("at", System.currentTimeMillis());
        // se escribe en un archivo aparte y se renombra: el launcher nunca ve un archivo a medias
        File tmp = new File(dir, "account-request.tmp");
        OutputStream os = new FileOutputStream(tmp);
        try { os.write(o.toString().getBytes("UTF-8")); } finally { os.close(); }
        File dst = requestFile();
        if (dst.exists() && !dst.delete()) throw new IOException("Hay otro pedido en curso. Espera un momento.");
        if (!tmp.renameTo(dst)) throw new IOException("No se pudo avisar al launcher.");
        return id;
    }

    /** true mientras el launcher todavia no tomo el pedido (si pasan unos segundos asi, el launcher no esta abierto). */
    public static boolean requestQueued() { return requestFile().exists(); }

    /** Retira el pedido si el launcher no llego a tomarlo. */
    public static void cancelRequest() { requestFile().delete(); }

    /** Respuesta del launcher a ese pedido, o null si todavia no hay. Nunca lanza. */
    public static Result result(String id) {
        File f = resultFile();
        if (id == null || !f.isFile()) return null;
        try {
            JsonElement el = new JsonParser().parse(readAll(new FileInputStream(f)));
            if (!el.isJsonObject()) return null;
            JsonObject o = el.getAsJsonObject();
            if (!id.equals(str(o, "id"))) return null;
            Result r = new Result();
            r.ok = o.has("ok") && o.get("ok").isJsonPrimitive() && o.get("ok").getAsBoolean();
            r.message = str(o, "message");
            return r;
        } catch (Exception ignored) {
            return null; // a medio escribir: se vuelve a mirar en el siguiente tick
        }
    }

    private static void afterChange() {
        SocialClient.onAccountChanged();
        CosmeticSync.accountChanged();
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
