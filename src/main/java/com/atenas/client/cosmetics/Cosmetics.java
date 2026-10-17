package com.atenas.client.cosmetics;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
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
        CAPE("Capas", "capes"), WINGS("Alas", "wings"), HAT("Sombreros", "hats");

        public final String label;
        public final String folder;

        Type(String label, String folder) {
            this.label = label;
            this.folder = folder;
        }

        /** cape / wings / hat (nombre del icono). */
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
        add(Type.CAPE, "atenas", "Atenas", 0xFFF5C542);
        add(Type.CAPE, "violeta", "Violeta Atenas", 0xFF8B5CF6);
        add(Type.CAPE, "fuego", "Fuego", 0xFFFF6A00);
        add(Type.CAPE, "hielo", "Hielo", 0xFF7FD6FF);
        add(Type.CAPE, "galaxia", "Galaxia", 0xFF7B4DFF);
        add(Type.CAPE, "arcoiris", "Arco\u00edris", 0xFFFF55FF);
        add(Type.CAPE, "esmeralda", "Esmeralda", 0xFF2ECC71);
        add(Type.CAPE, "oceano", "Oc\u00e9ano", 0xFF1E7FD8);
        add(Type.CAPE, "atardecer", "Atardecer", 0xFFFF7A59);
        add(Type.CAPE, "matrix", "Matrix", 0xFF22DD66);
        add(Type.CAPE, "neon", "Ne\u00f3n", 0xFF22D3EE);
        add(Type.CAPE, "oro", "Oro", 0xFFE8B923);
        add(Type.CAPE, "noche", "Noche", 0xFF2B2F6B);
        add(Type.CAPE, "creeper", "Creeper", 0xFF4CAF50);
        // Capas animadas: una sola textura con los fotogramas en vertical; solo se desplaza la UV (sin coste extra)
        addAnim(Type.CAPE, "llamas", "Llamas (anim)", 0xFFFF6A00, 12);
        addAnim(Type.CAPE, "aurora", "Aurora (anim)", 0xFF35E0A8, 12);
        addAnim(Type.CAPE, "cosmos", "Cosmos (anim)", 0xFF7B4DFF, 12);
        addAnim(Type.CAPE, "lluvia", "Lluvia Matrix (anim)", 0xFF22DD66, 16);
        addAnim(Type.CAPE, "mareas", "Mareas (anim)", 0xFF1E9FE8, 12);
        addAnim(Type.CAPE, "pulso", "Pulso Ne\u00f3n (anim)", 0xFFEC4899, 12);
        // Alas
        add(Type.WINGS, "doradas", "Alas Doradas", 0xFFF5C542);
        add(Type.WINGS, "oscuras", "Alas Oscuras", 0xFF8E2DE2);
        add(Type.WINGS, "angel", "Alas de \u00c1ngel", 0xFFEAF6FF);
        add(Type.WINGS, "atenas", "Alas Atenas", 0xFF8B5CF6);
        add(Type.WINGS, "fuego", "Alas de Fuego", 0xFFFF6A00);
        add(Type.WINGS, "hielo", "Alas de Hielo", 0xFF9ADCFF);
        add(Type.WINGS, "neon", "Alas de Ne\u00f3n", 0xFF22D3EE);
        add(Type.WINGS, "esmeralda", "Alas Esmeralda", 0xFF2ECC71);
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
        CosmeticSync.markDirty();
    }

    /** Cosmetico de un jugador: el tuyo, o el que descargamos del servidor para los demas. */
    public static Item of(AbstractClientPlayer p, Type t) {
        if (p == Minecraft.getMinecraft().thePlayer) return selected(t);
        if (!CosmeticSync.enabled) return null;
        String id = CosmeticSync.get(p.getName(), t);
        return id == null ? null : find(t, id);
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
