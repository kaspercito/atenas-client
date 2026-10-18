package com.atenas.client.modules;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

/**
 * Antes cambiaba los ajustes de video (nubes, sombras, VBO...) al estilo OptiFine, pero OptiFine ya viene instalado y hace eso mejor,
 * asi que esa parte se quito. Lo unico que queda, y que OptiFine NO trae, es ocultar los mobs y hologramas lejanos (ver PerfEvents).
 * Se mantiene el nombre "Rendimiento" para no perder lo que ya estaba guardado en la config.
 */
public class PerformanceModule extends Module {
    private final Setting cull, holoCull;

    public PerformanceModule() {
        super("Rendimiento", "Más FPS en granjas, lobbies y zonas con muchos mobs: no dibuja los mobs ni los hologramas que están lejos (los jugadores y sus nombres siempre se ven). Los gráficos los maneja OptiFine.", Category.PERF);
        cull = addNum("Ocultar mobs lejanos (bloques, 0 = no)", 48, 0, 128, 8);
        holoCull = addNum("Ocultar hologramas lejanos (bloques, 0 = no)", 24, 0, 64, 4);
    }

    public int entityCullDistance() { return cull.getInt(); }

    /**
     * Distancia a partir de la cual se ocultan los hologramas. Siempre es MENOR O IGUAL que la de los mobs (un holograma lejano
     * es texto flotante que casi nadie lee, asi que se oculta antes). 0 = no ocultar hologramas.
     */
    public int hologramCullDistance() {
        int h = holoCull.getInt();
        int m = cull.getInt();
        if (h <= 0) return 0;
        return m > 0 ? Math.min(h, m) : h;
    }
}
