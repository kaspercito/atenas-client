package com.atenas.client.cosmetics;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

/**
 * Sombrero como malla 3D (formato .atm generado por tools/gen_hats.py): grupos de quads con UV/normales suaves.
 * Cada grupo se compila UNA vez en una display list. Flags: 1 emisivo (aditivo, sin luz), 2 late, 4 gira sobre Y, 8 brillo suave.
 */
public final class HatMesh {
    public static final int EMIT = 1, PULSE = 2, SPIN = 4, SOFT = 8;
    private static final Map<String, HatMesh> CACHE = new HashMap<String, HatMesh>();
    private static final HatMesh NONE = new HatMesh(new int[0], new int[0]);

    public final int[] lists;
    public final int[] flags;

    private HatMesh(int[] lists, int[] flags) { this.lists = lists; this.flags = flags; }

    /** Malla del sombrero (o null si no existe: se usa el modelo de cajas). HILO PRINCIPAL. */
    public static HatMesh get(String id) {
        HatMesh m = CACHE.get(id);
        if (m == null) {
            try { m = load(id); } catch (Throwable t) {
                System.err.println("[AtenasClient] malla de sombrero " + id + ": " + t);
                m = NONE;
            }
            CACHE.put(id, m);
        }
        return m == NONE ? null : m;
    }

    private static HatMesh load(String id) throws Exception {
        InputStream raw = Minecraft.getMinecraft().getResourceManager()
                .getResource(new ResourceLocation("atenasclient", "models/hats/" + id + ".atm")).getInputStream();
        DataInputStream in = new DataInputStream(new BufferedInputStream(raw, 1 << 16));
        try {
            byte[] magic = new byte[4];
            in.readFully(magic);
            if (magic[0] != 'A' || magic[1] != 'T' || magic[2] != 'M' || magic[3] != '1') throw new Exception("formato");
            int groups = in.readInt();
            int[] lists = new int[groups], flags = new int[groups];
            final float s = 0.0625f;
            for (int g = 0; g < groups; g++) {
                in.readUTF();
                flags[g] = in.readByte();
                int quads = in.readInt();
                float[] f = new float[quads * 32];
                for (int i = 0; i < f.length; i++) f[i] = in.readFloat();
                int list = GLAllocation.generateDisplayLists(1);
                GL11.glNewList(list, GL11.GL_COMPILE);
                Tessellator t = Tessellator.getInstance();
                WorldRenderer wr = t.getWorldRenderer();
                wr.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
                for (int q = 0; q < quads; q++) {
                    for (int v = 0; v < 4; v++) {
                        int o = q * 32 + v * 8;
                        wr.pos(f[o] * s, f[o + 1] * s, f[o + 2] * s).tex(f[o + 3], f[o + 4]).normal(f[o + 5], f[o + 6], f[o + 7]).endVertex();
                    }
                }
                t.draw();
                GL11.glEndList();
                lists[g] = list;
            }
            return new HatMesh(lists, flags);
        } finally {
            in.close();
        }
    }
}
