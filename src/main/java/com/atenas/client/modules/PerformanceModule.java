package com.atenas.client.modules;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

import com.atenas.client.AtenasClient;

import net.minecraft.client.settings.GameSettings;

/**
 * Mas FPS al estilo OptiFine, usando solo ajustes de video del propio Minecraft.
 * Guarda tus valores originales y los devuelve al apagar el modulo.
 */
public class PerformanceModule extends Module {
    private final Setting fast, noClouds, noShadows, flatLight, fewParticles, renderDist, maxFps;
    private boolean saved;
    private boolean oFancy, oShadows;
    private int oClouds, oAo, oParticles, oDist, oFps;

    public PerformanceModule() {
        super("Rendimiento", "Más FPS: gráficos rápidos, sin nubes ni sombras y menos partículas. Devuelve tus ajustes al apagarlo. (Si usas OptiFine, él manda y este mod no hace nada.)", Category.PERF);
        fast = addBool("Gráficos rápidos", true);
        noClouds = addBool("Sin nubes", true);
        noShadows = addBool("Sin sombras de entidades", true);
        flatLight = addBool("Iluminación simple", false);
        fewParticles = addBool("Partículas mínimas", false);
        renderDist = addNum("Distancia de render (0 = la tuya)", 0, 0, 24, 1);
        maxFps = addNum("FPS máximos (260 = sin límite)", 260, 30, 260, 10);
    }

    private void save(GameSettings g) {
        oFancy = g.fancyGraphics; oShadows = g.entityShadows;
        oClouds = g.clouds; oAo = g.ambientOcclusion; oParticles = g.particleSetting;
        oDist = g.renderDistanceChunks; oFps = g.limitFramerate;
        saved = true;
    }

    @Override
    public void onEnable() { saved = false; }

    @Override
    public void onTick() {
        // Con OptiFine cargado, OptiFine manda en los graficos: no tocamos nada para no pelear con sus ajustes.
        if (AtenasClient.hasOptiFine()) return;
        GameSettings g = mc.gameSettings;
        if (!saved) save(g);
        boolean reload = false;
        if (fast.bool && g.fancyGraphics) { g.fancyGraphics = false; reload = true; }
        if (noClouds.bool) g.clouds = 0; else g.clouds = oClouds;
        g.entityShadows = noShadows.bool ? false : oShadows;
        int ao = flatLight.bool ? 0 : oAo;
        if (g.ambientOcclusion != ao) { g.ambientOcclusion = ao; reload = true; }
        g.particleSetting = fewParticles.bool ? 2 : oParticles;
        int d = renderDist.getInt();
        if (d > 0 && g.renderDistanceChunks != d) g.renderDistanceChunks = d;
        if (d == 0 && g.renderDistanceChunks != oDist) g.renderDistanceChunks = oDist;
        g.limitFramerate = maxFps.getInt() >= 260 ? 260 : maxFps.getInt();
        if (reload && mc.renderGlobal != null) mc.renderGlobal.loadRenderers();
    }

    @Override
    public void onDisable() {
        if (!saved) return;
        GameSettings g = mc.gameSettings;
        g.fancyGraphics = oFancy; g.entityShadows = oShadows;
        g.clouds = oClouds; g.ambientOcclusion = oAo; g.particleSetting = oParticles;
        g.renderDistanceChunks = oDist; g.limitFramerate = oFps;
        saved = false;
        if (mc.renderGlobal != null) mc.renderGlobal.loadRenderers();
    }
}
