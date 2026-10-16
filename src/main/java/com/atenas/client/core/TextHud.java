package com.atenas.client.core;

public abstract class TextHud extends HudModule {

    protected TextHud(String name, String description, int x, int y) {
        super(name, description, x, y);
    }

    protected abstract String getText();

    @Override
    public int getWidth() { return mc.fontRendererObj.getStringWidth(getText()) + 12; }

    @Override
    public int getHeight() { return 15; }

    @Override
    protected void draw() {
        box(getWidth(), getHeight());
        text(getText(), 7, 4);
    }
}
