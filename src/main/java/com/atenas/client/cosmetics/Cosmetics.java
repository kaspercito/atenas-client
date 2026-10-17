package com.atenas.client.cosmetics;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.atenas.client.core.Config;
import com.atenas.client.social.SocialClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

/** Registro de cosmeticos y seleccion actual (se ven en tu propio personaje). */
public final class Cosmetics {

    public enum Type {
        CAPE("Capas", "capes"), WINGS("Alas", "wings"), HAT("Sombreros", "hats"), PET("Mascotas", "pets");

        public final String label;
        public final String folder;

        Type(String label, String folder) {
            this.label = label;
            this.folder = folder;
        }

        /** cape / wings / hat / pet (nombre del icono). */
        public String key() { return name().toLowerCase(Locale.ROOT); }
    }

    public static final class Item {
        public final String id;
        public final String name;
        public final Type type;
        public final int color;
        /** Textura. Los cosmeticos del catalogo (x_...) usan la del modelo base hasta que termina la descarga. */
        public ResourceLocation texture;
        public ResourceLocation icon;
        /** Fotogramas de una capa animada (la textura es una tira vertical de 'frames' imagenes). 1 = capa normal. */
        public int frames;
        /** Viene del catalogo del servicio (id "x_...") y no del mod. */
        public final boolean custom;
        /** Cosmetico del mod cuya forma reutiliza (solo los del catalogo). */
        public final String base;
        public final boolean glow;
        /** Alas de membrana (se dibujan con {@link MembraneWing} en vez de plumas). */
        public final boolean membrane;
        /** Multiplicador de la apertura de las alas (1 = normal). Lo aplica WingsLayer. */
        public float openMul = 1f;
        /** Version de la textura que ya esta cargada (los del catalogo); -1 = aun no. */
        int loadedVer = -1;
        /** Version que pide el catalogo. */
        int wantVer;
        /** Texturas dinamicas ya creadas (para borrar la vieja al cambiar de version). */
        ResourceLocation dynTex, dynIcon;
        /** Privado y tu nick no esta en su lista: no aparece en tu menu ni puedes elegirlo. */
        public boolean locked;

        Item(Type type, String id, String name, int color, int frames) {
            this(type, id, name, color, frames, false, null, false, false);
        }

        Item(Type type, String id, String name, int color, int frames, boolean custom, String base, boolean glow, boolean membrane) {
            this.frames = frames;
            this.type = type;
            this.id = id;
            this.name = name;
            this.color = color;
            this.custom = custom;
            this.base = base == null ? "" : base;
            this.glow = glow;
            this.membrane = membrane;
            this.texture = new ResourceLocation("atenasclient",
                    "textures/cosmetics/" + type.folder + "/" + id + ".png");
            this.icon = new ResourceLocation("atenasclient",
                    "textures/cosmetics/icons/" + type.key() + "_" + id + ".png");
        }

        /** Id del modelo que dibuja este cosmetico: el suyo, o el de su base si viene del catalogo. */
        public String modelId() { return custom && !base.isEmpty() ? base : id; }

        public String key() { return type.key() + ":" + id; }
    }

    /** Una entrada del catalogo del servicio (/cosmetics/catalog). */
    public static final class Def {
        public final Type type;
        public final String id, name, base, scope;
        public final int color, frames, ver;
        public final boolean glow, custom;
        /** Nicks (minusculas) que pueden usarlo si es privado; null si es global. */
        public final Set<String> owners;

        public Def(Type type, String id, String name, int color, boolean glow, String base, int frames, String scope, boolean custom, int ver, Set<String> owners) {
            this.type = type; this.id = id; this.name = name; this.color = color; this.glow = glow;
            this.base = base == null ? "" : base; this.frames = frames; this.scope = scope; this.custom = custom;
            this.ver = ver; this.owners = owners;
        }

        boolean isPrivate() { return "private".equals(scope); }
    }

    /** Capas animadas encendidas (se puede apagar desde Cosmeticos; apagadas se ve el primer fotograma, sin coste). */
    public static boolean animate = true;
    /** Milisegundos por fotograma de las capas animadas (~11 fps). */
    public static final int FRAME_MS = 90;
    /** Los cosmeticos de otros jugadores solo se dibujan dentro de esta distancia (en bloques al cuadrado). */
    public static final double MAX_DIST_SQ = 40.0 * 40.0;

    public static final List<Item> ALL = new ArrayList<Item>();
    private static final Map<Type, Item> SELECTED = new EnumMap<Type, Item>(Type.class);

    static {
        // Capas
        addAnim(Type.CAPE, "atenas", "Atenas (Exclusiva)", 0xFFF5C542, 12);
        add(Type.CAPE, "atardecer", "Atardecer Lunar", 0xFFFF7A59);
        addAnim(Type.CAPE, "aurora", "Aurora", 0xFF35E0A8, 12);
        add(Type.CAPE, "sakura", "Sakura", 0xFFF8A9C8);
        add(Type.CAPE, "oceano", "Oc\u00e9ano al Atardecer", 0xFF1E7FD8);
        add(Type.CAPE, "bosque", "Bosque Brumoso", 0xFF4F8F72);
        addAnim(Type.CAPE, "galaxia", "Galaxia", 0xFF7B4DFF, 12);
        addAnim(Type.CAPE, "lava", "Monta\u00f1a de Lava", 0xFFFF6A1A, 12);
        add(Type.CAPE, "hielo", "Cristal de Hielo", 0xFF7FD6FF);
        addAnim(Type.CAPE, "llamas", "Llamas", 0xFFFF7A1A, 12);
        addAnim(Type.CAPE, "matrix", "Matrix", 0xFF1BFF5A, 12);
        addAnim(Type.CAPE, "pulso", "Pulso Synthwave", 0xFFFF2DA6, 12);
        addAnim(Type.CAPE, "arcoiris", "Arco\u00edris", 0xFF9B7BFF, 12);
        addAnim(Type.CAPE, "mareas", "Mareas", 0xFF2E6FC4, 12);
        addAnim(Type.CAPE, "eclipse", "Eclipse Solar", 0xFFFFB84D, 12);
        addAnim(Type.CAPE, "tormenta", "Tormenta", 0xFF5B7CFA, 12);
        addAnim(Type.CAPE, "nieve", "Ventisca", 0xFFBFE3FF, 12);
        addAnim(Type.CAPE, "luciernagas", "Luci\u00e9rnagas", 0xFFB8FF4A, 12);
        addAnim(Type.CAPE, "oroliquido", "Oro L\u00edquido", 0xFFFFC83D, 12);
        addAnim(Type.CAPE, "cristal", "Cristal Prisma", 0xFF8FE9FF, 12);
        addAnim(Type.CAPE, "portal", "Portal M\u00edstico", 0xFFB04DFF, 12);
        addAnim(Type.CAPE, "dragon", "Drag\u00f3n", 0xFFD6202A, 12);

        add(Type.WINGS, "doradas", "Alas Doradas", 0xFFF5C542);
        add(Type.WINGS, "oscuras", "Alas Oscuras", 0xFF8E2DE2);
        add(Type.WINGS, "angel", "Alas de \u00c1ngel", 0xFFEAF6FF);
        add(Type.WINGS, "atenas", "Alas Atenas", 0xFF8B5CF6);
        add(Type.WINGS, "fuego", "Alas de Fuego", 0xFFFF6A00);
        add(Type.WINGS, "hielo", "Alas de Hielo", 0xFF9ADCFF);
        add(Type.WINGS, "neon", "Alas de Ne\u00f3n", 0xFF22D3EE);
        add(Type.WINGS, "esmeralda", "Alas Esmeralda", 0xFF2ECC71);
        add(Type.WINGS, "ambar", "Alas \u00c1mbar", 0xFFFF7814);
        add(Type.WINGS, "sakura", "Alas Sakura", 0xFFFFAACD);
        add(Type.WINGS, "aurora", "Alas Aurora", 0xFF28E6AA);
        add(Type.WINGS, "lunar", "Alas Lunares", 0xFF7896E6);
        add(Type.WINGS, "demonio", "Alas de Demonio", 0xFFC0182A);
        // Alas de membrana (otra malla: MembraneWing; textura 128x128 de tools/gen_membrane.py)
        addMembrane("murcielago", "Alas de Murci\u00e9lago", 0xFF7860A8);
        addMembrane("vampiro", "Alas de Vampiro", 0xFFAA1E34);
        addMembrane("dragon", "Alas de Drag\u00f3n", 0xFFFF8C28);
        addMembrane("abismo", "Alas del Abismo", 0xFF14AABE);
        addMembrane("fantasma", "Alas Fantasma", 0xFFEBFAFF);
        addMembrane("jade", "Alas de Jade", 0xFF5AD796);
        addMembrane("solar", "Alas Solares", 0xFFFFE06E);
        addMembrane("ceniza", "Alas de Ceniza", 0xFF78767E);
        addMembrane("cristal", "Alas de Cristal", 0xFFC8F5FF);
        addMembrane("nebulosa", "Alas Nebulosa", 0xFF9646DC);
        // Sombreros
        add(Type.HAT, "halo", "Halo", 0xFFFFE680);
        add(Type.HAT, "corona", "Corona", 0xFFF5C542);
        add(Type.HAT, "galera", "Galera", 0xFF3A3A4E);
        add(Type.HAT, "gato", "Orejas de Gato", 0xFF2A2A34);
        add(Type.HAT, "cuernos", "Cuernos", 0xFFC0182A);
        add(Type.HAT, "mago", "Sombrero de Mago", 0xFF5B2FA8);
        add(Type.HAT, "auriculares", "Auriculares", 0xFF28283A);
        add(Type.HAT, "conejo", "Orejas de Conejo", 0xFFF2F2F7);
        add(Type.HAT, "gorra", "Gorra", 0xFF2447B8);
        // Mascotas (el modelo se elige por id en PetModel: perro/dalmata = dog, husky, gato/gatonegro = cat, zorro = fox)
        add(Type.PET, "perro", "Perro", 0xFFCD9654);
        add(Type.PET, "husky", "Husky", 0xFF68707E);
        add(Type.PET, "dalmata", "D\u00e1lmata", 0xFFF4F4F6);
        add(Type.PET, "gato", "Gato", 0xFFE28E3E);
        add(Type.PET, "gatonegro", "Gato negro", 0xFF24242E);
        add(Type.PET, "zorro", "Zorro", 0xFFE87028);
        add(Type.PET, "catalina", "Catalina", 0xFFE9A15A);
        // Catalina es privada: hasta que el catalogo diga que tu nick esta en su lista, no aparece ni se puede elegir.
        find(Type.PET, "catalina").locked = true;
        SELECTED.put(Type.CAPE, find(Type.CAPE, "atenas"));
    }

    private Cosmetics() {}

    private static void add(Type t, String id, String name, int color) {
        ALL.add(new Item(t, id, name, color, 1));
    }

    private static void addAnim(Type t, String id, String name, int color, int frames) {
        ALL.add(new Item(t, id, name, color, frames));
    }

    private static void addMembrane(String id, String name, int color) {
        ALL.add(new Item(Type.WINGS, id, name, color, 1, false, null, false, true));
    }

    /** True si este jugador es lo bastante cercano como para dibujarle cosmeticos (el tuyo siempre). */
    public static boolean inRange(AbstractClientPlayer p) {
        Minecraft mc = Minecraft.getMinecraft();
        return p == mc.thePlayer || mc.thePlayer == null || p.getDistanceSqToEntity(mc.thePlayer) <= MAX_DIST_SQ;
    }

    /** Fotograma actual de una capa animada (0 si esta apagado). */
    public static int frameOf(Item item) {
        if (item.frames <= 1 || !animate) return 0;
        return (int) ((Minecraft.getSystemTime() / FRAME_MS) % item.frames);
    }

    /** Maniquí de la vista previa del menu (cuando no hay mundo): lleva tu seleccion. Lo fija el menu. */
    public static volatile AbstractClientPlayer previewPlayer;

    public static Item selected(Type t) { return SELECTED.get(t); }

    public static void select(Type t, Item i) {
        if (i != null && i.locked) return; // privado y no es tuyo
        PENDING.remove(t);
        if (i == null) SELECTED.remove(t); else SELECTED.put(t, i);
        Config.save();
        if (!Config.isLoading()) CosmeticSync.markDirty();
    }

    // ------------------------------------------------------------------ seleccion pendiente

    /** Lo que elegiste pero todavia no existe aqui (un cosmetico del catalogo que aun no se ha bajado): se aplica cuando llega. */
    private static final Map<Type, String> PENDING = new EnumMap<Type, String>(Type.class);

    /** Carga una eleccion guardada en la config. Si el cosmetico aun no existe (es del catalogo) queda pendiente en vez de perderse. */
    public static void restore(Type t, String id) {
        if (id == null || id.length() == 0 || "none".equals(id)) { select(t, null); return; }
        Item i = find(t, id);
        if (i == null) { PENDING.put(t, id); SELECTED.remove(t); return; }
        select(t, i);
    }

    /** Id pendiente de este tipo (o null): la config lo conserva para no perder tu eleccion mientras baja el catalogo. */
    public static String pendingId(Type t) { return PENDING.get(t); }

    /** Mensaje corto para el menu (por ejemplo "no puedes llevar X"). Se apaga solo. */
    public static volatile String notice = "";
    public static volatile long noticeUntil;

    static void notice(String msg) { notice = msg; noticeUntil = System.currentTimeMillis() + 8000L; }

    /** El servicio rechazo un cosmetico (privado y tu nick no esta en la lista): se quita de tu seleccion y se avisa. Hilo principal. */
    public static void denied(Type t, String id) {
        Item cur = SELECTED.get(t);
        if (cur != null && cur.id.equals(id)) SELECTED.remove(t);
        if (id != null && id.equals(PENDING.get(t))) PENDING.remove(t);
        Item it = find(t, id);
        if (it != null) it.locked = true;
        notice("No puedes llevar \"" + (it != null ? it.name : id) + "\": es privado.");
        Config.save();
    }

    /** Cosmeticos de un tipo que puedes elegir (los privados ajenos no salen). */
    public static List<Item> visible(Type t) {
        List<Item> out = new ArrayList<Item>();
        for (Item i : ALL) if (i.type == t && !i.locked) out.add(i);
        return out;
    }

    /** Aplica lo que hay en el servicio para TI (otra version del juego lo cambio). No vuelve a subirlo. Devuelve true si cambio algo. */
    public static boolean applyRemote(Type t, String id) {
        if (id == null) return false;
        Item cur = SELECTED.get(t);
        if ("none".equals(id)) {
            PENDING.remove(t);
            if (cur == null) return false;
            SELECTED.remove(t);
            return true;
        }
        Item next = find(t, id);
        if (next == null) { PENDING.put(t, id); return false; } // aun no tenemos el catalogo: se aplicara cuando llegue
        if (next.locked || next == cur) return false;
        PENDING.remove(t);
        SELECTED.put(t, next);
        return true;
    }

    // ------------------------------------------------------------------ catalogo del servicio

    /** True cuando ya se aplico un catalogo (del disco o del servicio). Antes de eso los cosmeticos privados incorporados siguen ocultos. */
    private static boolean catalogApplied;

    public static boolean catalogApplied() { return catalogApplied; }

    /**
     * Aplica el catalogo del servicio. HILO PRINCIPAL.
     *  - reglas sobre cosmeticos incorporados: privado => solo los nicks de "owners" lo ven y lo eligen;
     *  - cosmeticos nuevos (id x_...): se crean/actualizan (con la forma de su "base") y se piden sus texturas;
     *  - los que ya no estan en el catalogo se quitan (y si lo llevabas, se desequipa).
     * Despues resuelve las elecciones pendientes.
     */
    public static void applyCatalog(List<Def> defs) {
        String me = SocialClient.myName();
        me = me == null ? "" : me.toLowerCase(Locale.ROOT);
        Map<String, Def> byKey = new HashMap<String, Def>();
        for (Def d : defs) byKey.put(d.type.key() + ":" + d.id, d);

        // 1) cosmeticos incorporados: bloqueo segun la regla
        for (Item i : ALL) {
            if (i.custom) continue;
            Def d = byKey.get(i.key());
            i.locked = d != null && d.isPrivate() && !(d.owners != null && d.owners.contains(me));
        }
        // 2) cosmeticos del catalogo: quitar los que ya no estan, crear/actualizar el resto
        for (Iterator<Item> it = ALL.iterator(); it.hasNext();) {
            Item i = it.next();
            if (!i.custom || byKey.containsKey(i.key())) continue;
            it.remove();
            if (SELECTED.get(i.type) == i) SELECTED.remove(i.type);
        }
        for (Def d : defs) {
            if (!d.custom) continue;
            Item base = d.base.length() > 0 ? find(d.type, d.base) : null;
            Item cur = find(d.type, d.id);
            boolean membrane = base != null && base.membrane;
            if (cur == null || cur.glow != d.glow || cur.membrane != membrane || !cur.name.equals(d.name) || cur.color != d.color || !cur.base.equals(d.base)) {
                Item n = new Item(d.type, d.id, d.name, d.color, d.type == Type.CAPE ? Math.max(1, d.frames) : 1, true, d.base, d.glow, membrane);
                if (base != null) { n.texture = base.texture; n.icon = base.icon; n.openMul = base.openMul; }
                if (cur != null) {
                    ALL.set(ALL.indexOf(cur), n);
                    if (SELECTED.get(d.type) == cur) SELECTED.put(d.type, n);
                    n.loadedVer = -1;
                } else {
                    ALL.add(n);
                }
                cur = n;
            }
            cur.frames = d.type == Type.CAPE ? Math.max(1, d.frames) : 1;
            cur.wantVer = d.ver;
            cur.locked = d.isPrivate() && !(d.owners != null && d.owners.contains(me));
            CosmeticAssets.request(cur);
        }
        // 3) lo que llevas puesto pero ya no puedes llevar se quita (y se sincroniza)
        boolean dropped = false;
        for (Type t : Type.values()) {
            Item c = SELECTED.get(t);
            if (c != null && c.locked) { SELECTED.remove(t); dropped = true; notice("No puedes llevar \"" + c.name + "\": es privado."); }
        }
        // 4) elecciones pendientes que ya existen
        boolean resolved = false;
        for (Iterator<Map.Entry<Type, String>> it = PENDING.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Type, String> e = it.next();
            Item f = find(e.getKey(), e.getValue());
            if (f == null) continue; // sigue sin existir (se queda pendiente)
            it.remove();
            if (!f.locked && SELECTED.get(e.getKey()) == null) { SELECTED.put(e.getKey(), f); resolved = true; }
        }
        catalogApplied = true;
        CosmeticSync.touch(); // los demas jugadores se vuelven a resolver con el catalogo nuevo
        if (dropped) CosmeticSync.markDirty();
        if (dropped || resolved) Config.save();
    }

    /** Lo que lleva puesto otro jugador, ya resuelto (se recalcula solo cuando llegan datos nuevos del servicio). */
    private static final class Look {
        final Item[] items = new Item[Type.values().length];
        int stamp = -1;
    }

    /** Cache por jugador (clave = nick tal como lo devuelve getName(), cuyo hash ya viene calculado). Solo el hilo principal la toca. */
    private static final Map<String, Look> LOOKS = new HashMap<String, Look>();

    /** Cosmetico de un jugador: el tuyo, o el que descargamos del servidor para los demas (con cache: sin buscar ni crear texto cada cuadro). */
    public static Item of(AbstractClientPlayer p, Type t) {
        if (p == Minecraft.getMinecraft().thePlayer || p == previewPlayer) return selected(t);
        if (!CosmeticSync.enabled) return null;
        String n = p.getName();
        if (n == null) return null;
        Look l = LOOKS.get(n);
        if (l == null) {
            if (LOOKS.size() > 256) LOOKS.clear();
            l = new Look();
            LOOKS.put(n, l);
        }
        int stamp = CosmeticSync.stamp();
        if (l.stamp != stamp) {
            String[] ids = CosmeticSync.idsOf(n);
            for (Type ty : Type.values()) {
                String id = ids == null || ty.ordinal() >= ids.length ? null : ids[ty.ordinal()];
                l.items[ty.ordinal()] = id == null ? null : find(ty, id);
            }
            l.stamp = stamp;
        }
        return l.items[t.ordinal()];
    }

    public static Item find(Type t, String id) {
        for (Item i : ALL) if (i.type == t && i.id.equals(id)) return i;
        return null;
    }

    /** A\u00f1ade nuestras capas de render a los modelos de jugador (default y slim). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerLayers() {
        try {
            Method add = null;
            for (String n : new String[] {"addLayer", "func_177094_a"}) {
                try {
                    add = RendererLivingEntity.class.getDeclaredMethod(n, LayerRenderer.class);
                    break;
                } catch (NoSuchMethodException ignored) { }
            }
            if (add == null) throw new NoSuchMethodException("addLayer");
            add.setAccessible(true);
            Map<String, RenderPlayer> skins = Minecraft.getMinecraft().getRenderManager().getSkinMap();
            for (RenderPlayer rp : skins.values()) {
                guardVanillaCape(rp);
                add.invoke(rp, new CapeLayer(rp));
                add.invoke(rp, new WingsLayer(rp));
                add.invoke(rp, new HatLayer(rp));
            }
        } catch (Exception e) {
            System.err.println("[AtenasClient] No se pudieron registrar los cosmeticos: " + e);
        }
    }

    /** Si llevas una capa de Atenas, la de Mojang no se dibuja. Busca la lista de capas por tipo, no por nombre. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void guardVanillaCape(RenderPlayer rp) {
        try {
            for (java.lang.reflect.Field f : RendererLivingEntity.class.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                List layers = (List) f.get(rp);
                for (int i = 0; i < layers.size(); i++) {
                    if (layers.get(i) instanceof LayerCape) layers.set(i, new VanillaCapeGuard((LayerRenderer) layers.get(i)));
                }
                return;
            }
        } catch (Exception e) {
            System.err.println("[AtenasClient] capa vanilla: " + e);
        }
    }
}
