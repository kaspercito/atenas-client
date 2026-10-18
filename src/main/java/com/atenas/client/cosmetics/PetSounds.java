package com.atenas.client.cosmetics;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.Minecraft;

/**
 * Sonidos de las mascotas. Cada una tiene dos: uno "ambiente" (de vez en cuando, ronroneo/jadeo/pio) y uno de "saludo" (cuando te acercas
 * o la miras). Son sonidos del propio juego con otro tono y volumen: si el nombre no existe no pasa nada (solo no suena).
 * Para sonidos propios: pon un .ogg en assets/atenasclient/sounds/pets/, declaralo en sounds.json y cambia el nombre aqui por "atenasclient:pets/nombre".
 */
public final class PetSounds {
    private PetSounds() {}

    /** Poner en false para silenciar todas las mascotas. */
    public static volatile boolean enabled = true;

    private static final class S {
        final String name; final float pitch, vol;
        S(String name, float pitch, float vol) { this.name = name; this.pitch = pitch; this.vol = vol; }
    }

    private static final Map<String, S[]> T = new HashMap<String, S[]>();
    private static final Random RND = new Random();

    private static void put(String id, String amb, float ap, float av, String greet, float gp, float gv) {
        T.put(id, new S[] {new S(amb, ap, av), new S(greet, gp, gv)});
    }

    static {
        //  id          ambiente (nombre, tono, volumen)              saludo
        put("perro",      "mob.wolf.panting", 1.00f, 0.50f,   "mob.wolf.bark",    1.15f, 0.55f);
        put("dalmata",    "mob.wolf.panting", 1.10f, 0.50f,   "mob.wolf.bark",    1.30f, 0.55f);
        put("husky",      "mob.wolf.panting", 0.90f, 0.50f,   "mob.wolf.bark",    0.85f, 0.55f);
        put("gato",       "mob.cat.purr",     1.00f, 0.40f,   "mob.cat.meow",     1.10f, 0.50f);
        put("gatonegro",  "mob.cat.purr",     0.90f, 0.40f,   "mob.cat.meow",     0.90f, 0.50f);
        put("catalina",   "mob.cat.purr",     1.20f, 0.40f,   "mob.cat.meow",     1.30f, 0.50f);
        put("zorro",      "mob.wolf.panting", 1.60f, 0.40f,   "mob.wolf.whine",   1.80f, 0.45f);
        put("conejo",     "mob.rabbit.idle",  1.00f, 0.45f,   "mob.rabbit.idle",  1.25f, 0.50f);
        put("panda",      "mob.pig.say",      0.65f, 0.40f,   "mob.pig.say",      0.85f, 0.50f);
        put("loro",       "mob.bat.idle",     1.70f, 0.40f,   "mob.chicken.say",  1.90f, 0.45f);
        put("buho",       "mob.bat.idle",     0.60f, 0.45f,   "mob.bat.idle",     0.50f, 0.55f);
        put("dragoncito", "mob.blaze.breathe",1.30f, 0.35f,   "mob.enderdragon.growl", 1.90f, 0.20f);
    }

    /** Reproduce el sonido de la mascota "id" para el evento {@link PetBrain#SND_AMBIENT} o {@link PetBrain#SND_GREET} en esa posicion. */
    public static void play(String id, int event, double x, double y, double z) {
        if (!enabled || event == PetBrain.SND_NONE) return;
        S[] t = T.get(id);
        if (t == null) t = T.get("perro");
        S s = t[event == PetBrain.SND_GREET ? 1 : 0];
        float pitch = s.pitch + (RND.nextFloat() - 0.5f) * 0.16f;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        mc.theWorld.playSound(x, y, z, s.name, s.vol, pitch, false);
    }
}
