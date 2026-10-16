package com.atenas.client.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;

public abstract class Module {

    public enum Category {
        HUD("HUD"), PVP("PvP"), ANIM("Animaciones"), PERF("Rendimiento"), UTIL("Utilidades");

        public final String label;

        Category(String label) { this.label = label; }
    }

    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<Setting>();
    protected final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
        Config.save();
    }

    public void toggle() { setEnabled(!enabled); }

    /** Para usar solo en constructores: arranca activado sin disparar callbacks. */
    protected void startEnabled() { enabled = true; }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onKey(int key) {}
    public void onFov(FOVUpdateEvent event) {}
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {}
    public void onFireOverlay(RenderBlockOverlayEvent event) {}
    /** Se llama justo antes de dibujar la mano/objeto en primera persona; las transformaciones GL que hagas aqui afectan al objeto. */
    public void onRenderHand(RenderHandEvent event) {}
    public void onChat(ClientChatReceivedEvent event) {}
    public void onHighlight(DrawBlockHighlightEvent event) {}
    public void onMouse(MouseEvent event) {}

    protected Setting addBool(String name, boolean def) {
        Setting s = Setting.bool(name, def);
        settings.add(s);
        return s;
    }

    protected Setting addNum(String name, double def, double min, double max, double step) {
        Setting s = Setting.num(name, def, min, max, step);
        settings.add(s);
        return s;
    }

    protected Setting addChoice(String name, int def, String... options) {
        Setting s = Setting.choice(name, def, options);
        settings.add(s);
        return s;
    }

    protected Setting addColor(String name, int paletteIndex) {
        Setting s = Setting.color(name, paletteIndex);
        settings.add(s);
        return s;
    }
}
