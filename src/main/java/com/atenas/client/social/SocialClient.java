package com.atenas.client.social;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.atenas.client.AtenasClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;

/**
 * Cliente del servicio social (mismo Worker que usa el launcher): amigos, solicitudes, chat y presencia.
 * Comparte la clave con el launcher en ~/.atenasclient/social.json, asi que launcher y cliente son la misma persona.
 * Todas las llamadas de red van en un hilo aparte; la UI solo lee {@link #snapshot()}.
 */
public final class SocialClient {

    /** Misma URL que "socialUrl" en launcher.config.json (se sobreescribe con ~/.atenasclient/social.json "url"). */
    public static final String DEFAULT_URL = "https://atenas-presence.misafi282.workers.dev";
    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    public static final class Friend {
        public String name, key, state = "offline", server = "", pack = "", previewBody = "";
        public int unread;
        public long previewId;
        public boolean online() { return !"offline".equals(state); }
        public boolean playing() { return "playing".equals(state); }
    }

    public static final class Req {
        public String name, key;
        public long ts;
    }

    public static final class Msg {
        public long id;
        public boolean mine;
        public String body;
        public long ts;
        public boolean pending, failed;
    }

    public static final class Snapshot {
        public final List<Friend> friends = new ArrayList<Friend>();
        public final List<Req> incoming = new ArrayList<Req>();
        public final List<Req> outgoing = new ArrayList<Req>();
        public int unread;
        public String me = "";
    }

    /** Resultado de una accion para mostrar al usuario. */
    public interface Callback<T> {
        void done(T value, String error);
    }

    /** Error que devolvio el servicio (con su codigo, p. ej. verify_required). Un IOException normal es un fallo de red. */
    public static final class ApiError extends IOException {
        public final String code;
        public ApiError(String code, String message) { super(message); this.code = code; }
    }

    public static boolean enabled = true;
    public static boolean toasts = true;
    public static boolean sharePresence = true;

    private static String url = DEFAULT_URL;
    private static String token = "";
    private static volatile Snapshot snap;
    private static volatile String status = "loading"; // loading | ok | error | noaccount | disabled
    private static volatile String error = "";
    private static final AtomicBoolean SYNC_BUSY = new AtomicBoolean(false);
    private static final AtomicBoolean POLL_BUSY = new AtomicBoolean(false);
    /** Firma de la ultima consulta rapida (mensajes sin leer : ultimo mensaje : solicitudes). Si cambia, algo nuevo llego. */
    private static volatile String lastPoll;
    private static long lastSync, lastPollAt;
    private static boolean identityLoaded;
    /** Nick (en minusculas) que ya demostro con Mojang que su cuenta premium es suya, y ultimo intento que no se pudo verificar. */
    private static volatile String verifiedFor = "";
    private static String failedFor = "";
    private static long failedAt;
    private static boolean failedHard; // true: Mojang rechazo la sesion (cuenta sin conexion); false: el servicio no pudo confirmarla
    private static final long RETRY_UNVERIFIED_MS = 5L * 60L * 1000L;
    private static final long RETRY_SOFT_MS = 90L * 1000L;
    /** Por que fallo la ultima verificacion (vacio si fue bien o aun no se intento). Tambien sale en latest.log con el prefijo [AtenasSocial]. */
    private static volatile String verifyError = "";
    public static String verifyError() { return verifyError; }

    /** Olvida un intento de verificacion fallido para que la proxima llamada lo vuelva a intentar. */
    public static synchronized void retryVerify() { failedFor = ""; failedAt = 0; }

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Atenas-Social");
            t.setDaemon(true);
            return t;
        }
    });

    private SocialClient() {}

    // ------------------------------------------------------------ estado publico

    public static Snapshot snapshot() { return snap; }
    public static String status() { return status; }
    public static String error() { return error; }
    public static boolean usable() { return snap != null && ("ok".equals(status) || "error".equals(status)); }

    public static int badge() {
        Snapshot s = snap;
        return s == null ? 0 : s.incoming.size() + s.unread;
    }

    /** URL base del servicio (la misma que usa Amigos): de aqui salen el catalogo y las texturas de cosmeticos. */
    public static String baseUrl() { loadIdentity(); return url; }

    public static String myName() {
        String n = Minecraft.getMinecraft().getSession().getUsername();
        return n == null ? "" : n;
    }

    public static boolean validName(String n) { return n != null && NAME.matcher(n).matches(); }

    // ------------------------------------------------------------ identidad compartida

    private static File identityFile() {
        return new File(com.atenas.client.account.Accounts.root(), "social.json");
    }

    private static synchronized void loadIdentity() {
        if (identityLoaded) return;
        identityLoaded = true;
        File f = identityFile();
        try {
            if (f.exists()) {
                String s = readAll(new FileInputStream(f));
                JsonObject o = new JsonParser().parse(s).getAsJsonObject();
                if (o.has("token") && o.get("token").getAsString().length() >= 16) token = o.get("token").getAsString();
                if (o.has("url") && o.get("url").getAsString().trim().length() > 0) url = o.get("url").getAsString().trim();
            }
        } catch (Exception ignored) { /* se crea una nueva */ }
        if (token.length() < 16) {
            token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            try {
                f.getParentFile().mkdirs();
                JsonObject o = new JsonObject();
                o.addProperty("token", token);
                o.addProperty("url", url);
                OutputStream os = new FileOutputStream(f);
                try { os.write(o.toString().getBytes("UTF-8")); } finally { os.close(); }
            } catch (IOException ex) {
                System.err.println("[AtenasClient] social.json: " + ex.getMessage());
            }
        }
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
    }

    // ------------------------------------------------------------ red

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

    /** True si esta cuenta ya demostro con Mojang que es premium y suya (los cosmeticos privados lo exigen). */
    public static boolean verified() { return myName().equalsIgnoreCase(verifiedFor); }

    /**
     * Verificacion premium: pide un desafio al servicio, lo registra en Mojang con TU sesion (joinServer, solo el dueno de la
     * cuenta puede) y el servicio lo comprueba con Mojang (hasJoined). Con una cuenta sin conexion Mojang lo rechaza y la cuenta
     * queda sin verificar (todo funciona, menos los cosmeticos privados). Nunca lanza: si falla, el servicio la trata como no verificada.
     */
    private static synchronized void ensureVerified(boolean force) {
        String name = myName().toLowerCase();
        if (!force && name.equals(verifiedFor)) return;
        if (!force && name.equals(failedFor) && System.currentTimeMillis() - failedAt < (failedHard ? RETRY_UNVERIFIED_MS : RETRY_SOFT_MS)) return;
        String lastSid = "";
        try {
            // Hasta 3 intentos: Mojang a veces tarda en ver el joinServer y el servicio recibe un "no" que es pasajero.
            for (int attempt = 1; attempt <= 3; attempt++) {
                JsonObject ch = rawPost("/auth/challenge", new JsonObject());
                lastSid = str(ch, "serverId");
                Minecraft mc = Minecraft.getMinecraft();
                try {
                    mc.getSessionService().joinServer(mc.getSession().getProfile(), mc.getSession().getToken(), lastSid);
                } catch (Exception e) {
                    failedFor = name; failedAt = System.currentTimeMillis(); failedHard = true; // Mojang no acepto la sesion: cuenta sin conexion o sesion caducada
                    verifyError = "Mojang rechazo la sesion del juego (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")";
                    System.err.println("[AtenasSocial] verificacion: " + verifyError + " - nick=" + name);
                    return;
                }
                try { Thread.sleep(300L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                try {
                    rawPost("/auth/verify", new JsonObject());
                    verifiedFor = name;
                    failedFor = "";
                    verifyError = "";
                    return;
                } catch (ApiError e) {
                    if ("not_verified".equals(e.code) && attempt < 3) {
                        System.err.println("[AtenasSocial] verificacion: intento " + attempt + " sin confirmar, reintentando...");
                        continue;
                    }
                    throw e;
                }
            }
        } catch (ApiError e) {
            verifyError = "el servicio respondio " + e.code + ": " + e.getMessage();
            System.err.println("[AtenasSocial] verificacion: " + verifyError + " - nick=" + name);
            if ("not_verified".equals(e.code)) diagnoseVerify(name, lastSid);
            if (!"mojang_down".equals(e.code) && !"too_many".equals(e.code) && !"no_challenge".equals(e.code) && !"server".equals(e.code)) {
                failedFor = name; failedAt = System.currentTimeMillis(); failedHard = false; // el servicio dijo que no: se reintenta pronto
            }
        } catch (IOException e) {
            verifyError = "sin conexion con el servicio de verificacion (" + e.getMessage() + ")";
            System.err.println("[AtenasSocial] verificacion: " + verifyError);
            // sin internet: se reintenta en la proxima llamada
        }
    }

    /** Solo para el log: pregunta a Mojang desde este PC si ve el serverId, para saber si el fallo esta en la sesion o en el servicio. */
    private static void diagnoseVerify(String name, String sid) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            System.err.println("[AtenasSocial] diagnostico: token=" + mc.getSession().getToken().length() + " caracteres - uuid=" + mc.getSession().getPlayerID() + " - nick=" + name);
            if (sid == null || sid.isEmpty()) return;
            HttpURLConnection c = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + java.net.URLEncoder.encode(mc.getSession().getUsername(), "UTF-8") + "&serverId=" + sid).openConnection();
            c.setConnectTimeout(5000); c.setReadTimeout(5000);
            int code = c.getResponseCode();
            System.err.println("[AtenasSocial] diagnostico: Mojang hasJoined desde este PC = HTTP " + code + (code == 200 ? " (Mojang SI ve tu sesion: el fallo esta en el servicio)" : " (Mojang no ve tu sesion)"));
        } catch (Exception e) {
            System.err.println("[AtenasSocial] diagnostico: no se pudo consultar a Mojang (" + e.getMessage() + ")");
        }
    }

    /** POST al servicio. Lanza IOException con mensaje legible para el usuario (ApiError si la respuesta vino del servicio). */
    public static JsonObject post(String route, JsonObject body) throws IOException {
        loadIdentity();
        String name = myName();
        if (!validName(name)) throw new IOException("Tu nick no es valido para el servicio de amigos.");
        ensureVerified(false);
        try {
            return rawPost(route, body);
        } catch (ApiError e) {
            // este nick esta verificado desde otra instalacion y esta aun no: se demuestra con la sesion de Minecraft y se reintenta una vez
            if (!"verify_required".equals(e.code)) throw e;
            synchronized (SocialClient.class) { verifiedFor = ""; failedFor = ""; }
            ensureVerified(true);
            if (!verified()) throw e;
            return rawPost(route, body);
        }
    }

    private static JsonObject rawPost(String route, JsonObject body) throws IOException {
        loadIdentity();
        String name = myName();
        if (!validName(name)) throw new IOException("Tu nick no es valido para el servicio de amigos.");
        HttpURLConnection c = (HttpURLConnection) new URL(url + route).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout("/auth/verify".equals(route) ? 25000 : 8000); // el servicio puede tardar varios segundos preguntando a Mojang
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("User-Agent", "AtenasClient/" + AtenasClient.VERSION);
        c.setRequestProperty("x-name", name);
        c.setRequestProperty("x-token", token);
        c.setDoOutput(true);
        OutputStream os;
        try {
            os = c.getOutputStream();
        } catch (IOException e) {
            throw new IOException("Sin conexion con el servicio de amigos.");
        }
        try { os.write((body == null ? "{}" : body.toString()).getBytes("UTF-8")); } finally { os.close(); }
        int code;
        try { code = c.getResponseCode(); } catch (IOException e) { throw new IOException("Sin conexion con el servicio de amigos."); }
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String res = is == null ? "" : readAll(is);
        JsonObject o = null;
        try { o = new JsonParser().parse(res).getAsJsonObject(); } catch (Exception ignored) { }
        if (o == null || code >= 400 || (o.has("ok") && !o.get("ok").getAsBoolean())) {
            String msg = (o != null && o.has("message")) ? o.get("message").getAsString() : "El servicio respondio con error " + code + ".";
            if (o != null && o.has("error")) throw new ApiError(o.get("error").getAsString(), msg);
            throw new IOException(msg);
        }
        return o;
    }

    private static String str(JsonObject o, String k) {
        return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : "";
    }

    private static Snapshot parse(JsonObject o) {
        Snapshot s = new Snapshot();
        s.me = str(o, "me");
        s.unread = o.has("unread") ? o.get("unread").getAsInt() : 0;
        for (JsonElement el : o.getAsJsonArray("friends")) {
            JsonObject f = el.getAsJsonObject();
            Friend fr = new Friend();
            fr.name = str(f, "name");
            fr.key = str(f, "key");
            fr.state = str(f, "state");
            fr.server = str(f, "server");
            fr.pack = str(f, "pack");
            fr.unread = f.has("unread") ? f.get("unread").getAsInt() : 0;
            if (f.has("preview") && f.get("preview").isJsonObject()) {
                JsonObject p = f.getAsJsonObject("preview");
                fr.previewBody = str(p, "body");
                fr.previewId = p.has("id") ? p.get("id").getAsLong() : 0;
            }
            s.friends.add(fr);
        }
        fill(s.incoming, o.getAsJsonArray("incoming"));
        fill(s.outgoing, o.getAsJsonArray("outgoing"));
        return s;
    }

    private static void fill(List<Req> out, JsonArray arr) {
        for (JsonElement el : arr) {
            JsonObject r = el.getAsJsonObject();
            Req q = new Req();
            q.name = str(r, "name");
            q.key = str(r, "key");
            q.ts = r.has("ts") ? r.get("ts").getAsLong() : 0;
            out.add(q);
        }
    }

    // ------------------------------------------------------------ sincronizacion

    /** Pide al servicio el estado completo (y avisa nuestra presencia). No bloquea. */
    public static void sync(final boolean inGame, final String server) {
        if (!enabled) { status = "disabled"; return; }
        if (!validName(myName())) { status = "noaccount"; return; }
        if (!SYNC_BUSY.compareAndSet(false, true)) return;
        lastSync = System.currentTimeMillis();
        final String who = myName();
        POOL.execute(new Runnable() {
            @Override public void run() {
                try {
                    JsonObject body = new JsonObject();
                    if (inGame || !sharePresence) {
                        JsonObject p = new JsonObject();
                        p.addProperty("source", "game");
                        if (!sharePresence) p.addProperty("offline", true);
                        else {
                            p.addProperty("server", server == null ? "" : server);
                            p.addProperty("pack", "Atenas Client");
                            p.addProperty("playing", true);
                        }
                        body.add("presence", p);
                    }
                    Snapshot next = parse(post("/sync", body));
                    if (!who.equals(myName())) return; // cambiaste de cuenta mientras tanto: esta respuesta es de la otra persona
                    SocialEvents.onSnapshot(snap, next);
                    snap = next;
                    status = "ok";
                    error = "";
                } catch (Exception e) {
                    status = "error";
                    error = String.valueOf(e.getMessage());
                } finally {
                    SYNC_BUSY.set(false);
                }
            }
        });
    }

    public static long sinceLastSync() { return System.currentTimeMillis() - lastSync; }

    public static long sinceLastPoll() { return System.currentTimeMillis() - lastPollAt; }

    /**
     * Consulta rapida (/poll): el servicio responde con 2 lecturas pequenas en vez de bajar todo el estado. Se hace cada pocos
     * segundos y solo si algo cambio (mensaje o solicitud nueva) se pide el estado completo con /sync, asi los avisos llegan casi
     * al instante sin cargar al servicio. La primera vez solo guarda la firma. No bloquea.
     */
    public static void poll(final boolean inGame, final String server) {
        if (!enabled || snap == null || !validName(myName())) return; // sin estado completo, el sync normal se encarga
        if (!POLL_BUSY.compareAndSet(false, true)) return;
        lastPollAt = System.currentTimeMillis();
        final String who = myName();
        POOL.execute(new Runnable() {
            @Override public void run() {
                try {
                    JsonObject r = post("/poll", new JsonObject());
                    if (!who.equals(myName())) return;
                    String sig = str(r, "unread") + ":" + str(r, "last") + ":" + str(r, "incoming");
                    boolean changed = lastPoll != null && !sig.equals(lastPoll);
                    lastPoll = sig;
                    if (changed) sync(inGame, server);
                } catch (Exception ignored) { /* sin conexion: el ciclo normal reintenta */
                } finally {
                    POLL_BUSY.set(false);
                }
            }
        });
    }

    /** Cambio de cuenta dentro del juego: la lista de amigos y el chat eran de la otra persona; se pide todo de nuevo. */
    public static void onAccountChanged() {
        synchronized (SocialClient.class) { verifiedFor = ""; failedFor = ""; }
        snap = null;
        status = "loading";
        error = "";
        lastSync = 0;
        lastPoll = null;
    }

    private static void run(final Runnable r, final Callback<?> cb) {
        POOL.execute(new Runnable() {
            @Override public void run() {
                try { r.run(); } catch (Exception e) { if (cb != null) fail(cb, e); }
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void fail(Callback cb, Exception e) { cb.done(null, String.valueOf(e.getMessage())); }

    /** Accion simple (agregar, aceptar, quitar). Despues refresca el estado. */
    private static void action(final String route, final JsonObject body, final Callback<String> cb) {
        POOL.execute(new Runnable() {
            @Override public void run() {
                try {
                    JsonObject r = post(route, body);
                    try { snap = parse(post("/sync", new JsonObject())); status = "ok"; } catch (Exception ignored) { }
                    if (cb != null) cb.done(str(r, "result") + "|" + str(r, "name"), null);
                } catch (Exception e) {
                    if (cb != null) cb.done(null, String.valueOf(e.getMessage()));
                }
            }
        });
    }

    public static void add(String to, Callback<String> cb) {
        JsonObject b = new JsonObject();
        b.addProperty("to", to);
        action("/friends/add", b, cb);
    }

    public static void accept(String key, Callback<String> cb) {
        JsonObject b = new JsonObject();
        b.addProperty("name", key);
        action("/friends/accept", b, cb);
    }

    public static void remove(String key, Callback<String> cb) {
        JsonObject b = new JsonObject();
        b.addProperty("name", key);
        action("/friends/remove", b, cb);
    }

    public static void history(final String key, final long after, final Callback<List<Msg>> cb) {
        POOL.execute(new Runnable() {
            @Override public void run() {
                try {
                    JsonObject b = new JsonObject();
                    b.addProperty("with", key);
                    b.addProperty("after", after);
                    JsonObject r = post("/chat/history", b);
                    List<Msg> out = new ArrayList<Msg>();
                    for (JsonElement el : r.getAsJsonArray("messages")) {
                        JsonObject m = el.getAsJsonObject();
                        Msg x = new Msg();
                        x.id = m.get("id").getAsLong();
                        x.mine = m.get("mine").getAsBoolean();
                        x.body = str(m, "body");
                        x.ts = m.get("ts").getAsLong();
                        out.add(x);
                    }
                    cb.done(out, null);
                } catch (Exception e) {
                    cb.done(Collections.<Msg>emptyList(), String.valueOf(e.getMessage()));
                }
            }
        });
    }

    public static void send(String key, String text, final Callback<Msg> cb) {
        final JsonObject b = new JsonObject();
        b.addProperty("to", key);
        b.addProperty("body", text);
        POOL.execute(new Runnable() {
            @Override public void run() {
                try {
                    JsonObject m = post("/chat/send", b).getAsJsonObject("message");
                    Msg x = new Msg();
                    x.id = m.get("id").getAsLong();
                    x.mine = true;
                    x.body = str(m, "body");
                    x.ts = m.get("ts").getAsLong();
                    cb.done(x, null);
                } catch (Exception e) {
                    cb.done(null, String.valueOf(e.getMessage()));
                }
            }
        });
    }

    /** Para el estado de la UI: busca un amigo por clave (nick en minusculas). */
    public static Friend find(String key) {
        Snapshot s = snap;
        if (s == null || key == null) return null;
        for (Friend f : s.friends) if (f.key.equals(key)) return f;
        return null;
    }

    public static Map<String, Integer> unreadByKey() {
        Map<String, Integer> m = new HashMap<String, Integer>();
        Snapshot s = snap;
        if (s != null) for (Friend f : s.friends) if (f.unread > 0) m.put(f.key, f.unread);
        return m;
    }
}
