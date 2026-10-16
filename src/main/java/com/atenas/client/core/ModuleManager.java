package com.atenas.client.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.atenas.client.gui.HudEditor;
import com.atenas.client.modules.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public final class ModuleManager {

    public static final ModuleManager INSTANCE = new ModuleManager();
    private static final List<Module> MODULES = new ArrayList<Module>();
    private final Minecraft mc = Minecraft.getMinecraft();

    private ModuleManager() {}

    public static void init() {
        MODULES.clear();
        // HUD
        add(new FpsModule());
        add(new CpsModule());
        add(new PingModule());
        add(new CoordsModule());
        add(new ComboModule());
        add(new ReachModule());
        add(new ClockModule());
        add(new MemoryModule());
        add(new SpeedModule());
        add(new KeystrokesModule());
        add(new ArmorModule());
        add(new PotionModule());
        // PvP
        add(new MovementModule());
        add(new CrosshairModule());
        add(new LowFireModule());
        add(new BlockOverlayModule());
        // Animaciones (estilo 1.7)
        add(new OldAnimationsModule());
        // Rendimiento (estilo OptiFine)
        add(new PerformanceModule());
        // Utilidades
        add(new ZoomModule());
        add(new FullbrightModule());
        add(new TimeModule());
        add(new ChatTimeModule());
        add(new AutotextModule());
    }

    private static void add(Module m) { MODULES.add(m); }

    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }

    /** Busca un modulo por su clase (lo usan los mixins). */
    public static <T extends Module> T find(Class<T> type) {
        for (Module m : MODULES) if (type.isInstance(m)) return type.cast(m);
        return null;
    }

    public static List<HudModule> hud() {
        List<HudModule> out = new ArrayList<HudModule>();
        for (Module m : MODULES) if (m instanceof HudModule) out.add((HudModule) m);
        return out;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        for (Module m : MODULES) if (m.isEnabled()) m.onTick();
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (!Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        for (Module m : MODULES) if (m.isEnabled()) m.onKey(key);
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent e) {
        for (Module m : MODULES) if (m.isEnabled()) m.onFov(e);
    }

    @SubscribeEvent
    public void onFire(RenderBlockOverlayEvent e) {
        for (Module m : MODULES) if (m.isEnabled()) m.onFireOverlay(e);
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent e) {
        for (Module m : MODULES) if (m.isEnabled()) m.onRenderHand(e);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        for (Module m : MODULES) if (m.isEnabled()) m.onChat(e);
    }

    @SubscribeEvent
    public void onHighlight(DrawBlockHighlightEvent e) {
        for (Module m : MODULES) if (m.isEnabled()) m.onHighlight(e);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        for (Module m : MODULES) if (m.isEnabled()) m.onMouse(e);
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre e) {
        for (Module m : MODULES) if (m.isEnabled()) m.onOverlayPre(e);
    }

    @SubscribeEvent
    public void onOverlayPost(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.currentScreen instanceof HudEditor) return;
        ScaledResolution sr = e.resolution;
        for (HudModule h : hud()) {
            if (!h.isEnabled()) continue;
            h.ensurePositioned(sr);
            h.render();
        }
    }
}
