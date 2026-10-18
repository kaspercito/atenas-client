package com.atenas.client.cosmetics;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import net.minecraft.util.ResourceLocation;

/**
 * Minecraft carga las texturas de entidades con filtro NEAREST y sin mipmaps: una textura HD se ve granulada y titila de lejos.
 * Aqui se pone filtro suave + mipmaps UNA vez por textura (llamar con la textura ya enlazada). maxLevel limita los mipmaps
 * (las capas animadas son tiras de fotogramas: pocos niveles para que no se mezclen entre fotogramas).
 */
public final class TexFilter {
    private static final Set<ResourceLocation> DONE = Collections.newSetFromMap(new WeakHashMap<ResourceLocation, Boolean>());

    private TexFilter() {}

    public static void prepare(ResourceLocation loc, int maxLevel) {
        if (loc == null || DONE.contains(loc)) return;
        DONE.add(loc);
        try {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            if (GLContext.getCapabilities().OpenGL30) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
                if (maxLevel >= 0) GL11.glTexParameteri(GL11.GL_TEXTURE_2D, 33085, maxLevel); // GL_TEXTURE_MAX_LEVEL
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            } else {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            }
        } catch (Throwable t) {
            System.err.println("[AtenasClient] filtro de textura: " + t);
        }
    }
}
