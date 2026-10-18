package com.atenas.client.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import com.atenas.client.cosmetics.CosmeticSync;
import com.atenas.client.cosmetics.Cosmetics;
import com.atenas.client.modules.AutotextModule;
import com.atenas.client.social.SocialClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;

/** Guarda y carga ajustes en .minecraft/config/atenasclient.json */
public final class Config {

    private static boolean loading = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {}

    public static boolean isLoading() { return loading; }

    private static File file() {
        return new File(new File(Minecraft.getMinecraft().mcDataDir, "config"), "atenasclient.json");
    }

    public static void load() {
        File f = file();
        if (!f.exists()) return;
        loading = true;
        Reader r = null;
        try {
            r = new InputStreamReader(new FileInputStream(f), "UTF-8");
            JsonObject root = new JsonParser().parse(r).getAsJsonObject();

            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : ModuleManager.all()) {
                if (!mods.has(m.name)) continue;
                JsonObject o = mods.getAsJsonObject(m.name);
                if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
                if (m instanceof HudModule) {
                    HudModule h = (HudModule) m;
                    if (o.has("x") && o.has("y")) {
                        h.x = o.get("x").getAsInt();
                        h.y = o.get("y").getAsInt();
                        h.positioned = true;
                    }
                }
                if (o.has("settings")) {
                    JsonObject so = o.getAsJsonObject("settings");
                    for (Setting s : m.settings) {
                        if (!so.has(s.name)) continue;
                        JsonElement el = so.get(s.name);
                        switch (s.type) {
                            case BOOL: s.bool = el.getAsBoolean(); break;
                            case NUM: s.num = el.getAsDouble(); break;
                            default: s.color = el.getAsInt(); break;
                        }
                    }
                }
            }

            if (root.has("cosmetics")) {
                JsonObject c = root.getAsJsonObject("cosmetics");
                for (Cosmetics.Type t : Cosmetics.Type.values()) {
                    String key = t.name().toLowerCase();
                    if (c.has(key)) Cosmetics.restore(t, c.get(key).getAsString());
                }
                if (c.has("animate")) Cosmetics.animate = c.get("animate").getAsBoolean();
            }

            if (root.has("online")) {
                JsonObject on = root.getAsJsonObject("online");
                // "enabled" se ignora: una version anterior pudo dejarlo en false y la sincronizacion quedaba apagada para siempre
                CosmeticSync.enabled = true;
                if (on.has("url") && on.get("url").getAsString().trim().length() > 0) {
                    CosmeticSync.url = on.get("url").getAsString().trim();
                }
                if (on.has("token")) CosmeticSync.token = on.get("token").getAsString();
            }

            if (root.has("social")) {
                JsonObject so = root.getAsJsonObject("social");
                if (so.has("enabled")) SocialClient.enabled = so.get("enabled").getAsBoolean();
                if (so.has("toasts")) SocialClient.toasts = so.get("toasts").getAsBoolean();
                if (so.has("sharePresence")) SocialClient.sharePresence = so.get("sharePresence").getAsBoolean();
            }

            if (root.has("autotext")) {
                AutotextModule.entries.clear();
                for (JsonElement el : root.getAsJsonArray("autotext")) {
                    JsonObject e = el.getAsJsonObject();
                    AutotextModule.entries.add(new AutotextModule.Entry(
                            e.get("key").getAsInt(), e.get("message").getAsString()));
                }
            }
        } catch (Exception e) {
            System.err.println("[AtenasClient] No se pudo leer la configuracion: " + e);
        } finally {
            loading = false;
            if (r != null) try { r.close(); } catch (IOException ignored) { }
        }
    }

    public static void save() {
        if (loading) return;
        Writer w = null;
        try {
            JsonObject root = new JsonObject();
            JsonObject mods = new JsonObject();
            for (Module m : ModuleManager.all()) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.isEnabled());
                if (m instanceof HudModule) {
                    o.addProperty("x", ((HudModule) m).x);
                    o.addProperty("y", ((HudModule) m).y);
                }
                JsonObject so = new JsonObject();
                for (Setting s : m.settings) {
                    switch (s.type) {
                        case BOOL: so.addProperty(s.name, s.bool); break;
                        case NUM: so.addProperty(s.name, s.num); break;
                        default: so.addProperty(s.name, s.color); break;
                    }
                }
                o.add("settings", so);
                mods.add(m.name, o);
            }
            root.add("modules", mods);

            JsonObject cos = new JsonObject();
            for (Cosmetics.Type t : Cosmetics.Type.values()) {
                Cosmetics.Item i = Cosmetics.selected(t);
                String pend = i == null ? Cosmetics.pendingId(t) : null; // eleccion que aun espera al catalogo: no se pierde
                cos.addProperty(t.name().toLowerCase(), i != null ? i.id : pend != null ? pend : "none");
            }
            cos.addProperty("animate", Cosmetics.animate);
            root.add("cosmetics", cos);

            JsonObject on = new JsonObject();
            on.addProperty("enabled", CosmeticSync.enabled);
            on.addProperty("url", CosmeticSync.url);
            on.addProperty("token", CosmeticSync.token);
            root.add("online", on);

            JsonObject soc = new JsonObject();
            soc.addProperty("enabled", SocialClient.enabled);
            soc.addProperty("toasts", SocialClient.toasts);
            soc.addProperty("sharePresence", SocialClient.sharePresence);
            root.add("social", soc);

            JsonArray arr = new JsonArray();
            for (AutotextModule.Entry e : AutotextModule.entries) {
                JsonObject o = new JsonObject();
                o.addProperty("key", e.key);
                o.addProperty("message", e.message == null ? "" : e.message);
                arr.add(o);
            }
            root.add("autotext", arr);

            File f = file();
            f.getParentFile().mkdirs();
            w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
            GSON.toJson(root, w);
        } catch (Exception e) {
            System.err.println("[AtenasClient] No se pudo guardar la configuracion: " + e);
        } finally {
            if (w != null) try { w.close(); } catch (IOException ignored) { }
        }
    }
}
