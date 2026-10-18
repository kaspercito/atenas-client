package com.atenas.client;

import com.atenas.client.gui.Draw;

/** Datos del modpack que manda el launcher (-Datenas.pack / -Datenas.kind) y el icono de 10x10 que se dibuja en el pie del menu principal. */
public final class PackInfo {
    private PackInfo() {}

    private static String prop(String k) {
        String v = System.getProperty(k);
        return v == null ? "" : v.trim();
    }

    public static String packName() {
        String n = prop("atenas.pack");
        return n.isEmpty() ? "Atenas Client 1.8.9" : n;
    }

    /** pvp | smp | mods | cube (el launcher lo calcula; si falta, se deduce del nombre). */
    public static String kind() {
        String k = prop("atenas.kind");
        return k.isEmpty() ? kindOfName(packName()) : k;
    }

    public static String kindOfName(String name) {
        String t = name == null ? "" : name.toLowerCase();
        if (t.contains("pvp") || t.contains("client") || t.contains("cliente") || t.contains("1.8")) return "pvp";
        if (t.contains("smp") || t.contains("survival") || t.contains("supervivencia")) return "smp";
        if (t.contains("1.20") || t.contains("forge") || t.contains("fabric") || t.contains("mod")) return "mods";
        return "cube";
    }

    /** Icono de 10x10 dibujado con rectangulos (sin texturas), el mismo que el del mod de 1.20.1. */
    public static void drawIcon(int x, int y, String kind) {
        if ("pvp".equals(kind)) { // espadas cruzadas
            for (int i = 0; i < 9; i++) {
                Draw.rect(x + i, y + i, 2, 1, 0xFFE8F1FB);
                Draw.rect(x + 8 - i, y + i, 2, 1, 0xFFA9BDD3);
            }
            Draw.rect(x, y + 8, 3, 2, 0xFFB45309);
            Draw.rect(x + 7, y + 8, 3, 2, 0xFFB45309);
        } else if ("smp".equals(kind)) { // arbol
            Draw.rect(x + 4, y + 6, 2, 4, 0xFF6B4423);
            Draw.rect(x + 1, y + 2, 8, 5, 0xFF2F8F46);
            Draw.rect(x + 3, y, 4, 3, 0xFF3BB55A);
        } else if ("mods".equals(kind)) { // pieza de rompecabezas
            Draw.rect(x + 1, y + 3, 8, 6, 0xFF8B5CF6);
            Draw.rect(x + 3, y + 1, 4, 2, 0xFFA78BFA);
            Draw.rect(x + 4, y + 5, 2, 2, 0xFF22D3EE);
        } else { // cubo
            Draw.rect(x + 1, y + 1, 8, 8, 0xFF3F9A4B);
            Draw.rect(x + 1, y + 1, 8, 2, 0xFF67C877);
            Draw.rect(x + 1, y + 3, 8, 6, 0xFF7A5230);
        }
    }
}
