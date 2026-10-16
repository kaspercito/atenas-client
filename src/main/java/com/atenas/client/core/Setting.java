package com.atenas.client.core;

public class Setting {
    public enum Type { BOOL, NUM, COLOR, CHOICE }

    public static final int[] PALETTE = {
        0xFFFFFFFF, 0xFFF5C542, 0xFF55FFFF, 0xFF55FF55,
        0xFFFF5555, 0xFFFF55FF, 0xFF5577FF, 0xFFAAAAAA,
        0xFFA78BFA, 0xFFEC4899,
        0xFFFF9F1C, 0xFF2DD4BF, 0xFFFFB3D9, 0xFF111111
    };

    public final String name;
    public final Type type;
    public boolean bool;
    public double num, min, max, step;
    public int color;
    /** Solo CHOICE: textos de las opciones (el indice elegido se guarda en 'color'). */
    public String[] options;

    private Setting(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public static Setting bool(String name, boolean def) {
        Setting s = new Setting(name, Type.BOOL);
        s.bool = def;
        return s;
    }

    public static Setting num(String name, double def, double min, double max, double step) {
        Setting s = new Setting(name, Type.NUM);
        s.num = def;
        s.min = min;
        s.max = max;
        s.step = step;
        return s;
    }

    public static Setting color(String name, int paletteIndex) {
        Setting s = new Setting(name, Type.COLOR);
        s.color = paletteIndex;
        return s;
    }

    public static Setting choice(String name, int def, String... options) {
        Setting s = new Setting(name, Type.CHOICE);
        s.color = def;
        s.options = options;
        return s;
    }

    /** Indice elegido de un CHOICE (siempre dentro de rango). */
    public int index() {
        if (options == null || options.length == 0) return 0;
        return Math.max(0, Math.min(options.length - 1, color));
    }

    public int getColor() {
        return PALETTE[Math.abs(color) % PALETTE.length];
    }

    public float getFloat() { return (float) num; }

    public int getInt() { return (int) Math.round(num); }

    public double progress() {
        return (num - min) / (max - min);
    }

    public void setFromSlider(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        double v = min + (max - min) * t;
        if (step > 0) v = Math.round(v / step) * step;
        num = Math.max(min, Math.min(max, v));
    }
}
