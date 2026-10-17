package com.atenas.client.cosmetics;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.atenas.client.core.Config;

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
        public final ResourceLocation texture;
        public final ResourceLocation icon;
        /** Fotogramas de una capa animada (la textura es una tira vertical de 'frames' imagenes). 1 = capa normal. */
        public final int frames;

        Item(Type type, String id, String name, int color, int frames) {
            this.frames = frames;
            this.type = type;
            this.id = id;
            this.name = name;
            this.color = color;
            this.texture = new ResourceLocation("atenasclient",
                    "textures/cosmetics/" + type.folder + "/" + id + ".png");
            this.icon = new ResourceLocation("atenasclient",
                    "textures/cosmetics/icons/" + type.key() + "_" + id + ".png");
        }
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
        SELECTED.put(Type.CAPE, find(Type.CAPE, "atenas"));
    }

    private Cosmetics() {}

    private static void add(Type t, String id, String name, int color) {
        ALL.add(new Item(t, id, name, color, 1));
    }

    private static void addAnim(Type t, String id, String name, int color, int frames) {
        ALL.add(new Item(t, id, name, color, frames));
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

    public static Item selected(Type t) { return SELECTED.get(t); }

    public static void select(Type t, Item i) {
        if (i == null) SELECTED.remove(t); else SELECTED.put(t, i);
        Config.save();
        if (!Config.isLoading()) CosmeticSync.markDirty();
    }

    /** Aplica lo que hay en el servicio para TI (otra version del juego lo cambio). No vuelve a subirlo. Devuelve true si cambio algo. */
    public static boolean applyRemote(Type t, String id) {
        if (id == null) return false;
        Item cur = SELECTED.get(t);
        if ("none".equals(id)) {
            if (cur == null) return false;
            SELECTED.remove(t);
            return true;
        }
        Item next = find(t, id);
        if (next == null || next == cur) return false;
        SELECTED.put(t, next);
        return true;
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
        if (p == Minecraft.getMinecraft().thePlayer) return selected(t);
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
