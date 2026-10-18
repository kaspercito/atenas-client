package com.atenas.client.core;

public abstract class TextHud extends HudModule {

    private String cached = "";
    private int cachedWidth = 12;
    private long cachedAt = -1000L;

    protected TextHud(String name, String description, int x, int y) {
        super(name, description, x, y);
    }

    protected abstract String getText();

    /** Cada cuantos ms se vuelve a calcular el texto (antes se armaba 3 veces por cuadro: ancho, alto y dibujo). */
    protected long refreshMs() { return 50L; }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < refreshMs() && now >= cachedAt) return;
        cachedAt = now;
        String t = getText();
        if (t == null) t = "";
        if (!t.equals(cached)) {
            cached = t;
            cachedWidth = mc.fontRendererObj.getStringWidth(t) + 12;
        }
    }

    @Override
    public int getWidth() { refresh(); return cachedWidth; }

    @Override
    public int getHeight() { return 15; }

    @Override
    protected void draw() {
        refresh();
        box(cachedWidth, getHeight());
        text(cached, 7, 4);
    }
}
