package com.atenas.client.modules;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

/**
 * Antes cambiaba los ajustes de video (nubes, sombras, VBO...) al estilo OptiFine, pero OptiFine ya viene instalado y hace eso mejor,
 * asi que esa parte se quito. Lo unico que queda, y que OptiFine NO trae, es ocultar los mobs lejanos (ver PerfEvents).
 * Se mantiene el nombre "Rendimiento" para no perder lo que ya estaba guardado en la config.
 */
public class PerformanceModule extends Module {
    private final Setting cull;

    public PerformanceModule() {
        super("Rendimiento", "Más FPS en granjas y zonas con muchos mobs: no dibuja los mobs que están lejos (los jugadores siempre se ven). Los gráficos los maneja OptiFine.", Category.PERF);
        cull = addNum("Ocultar mobs lejanos (bloques, 0 = no)", 48, 0, 128, 8);
    }

    public int entityCullDistance() { return cull.getInt(); }
}
