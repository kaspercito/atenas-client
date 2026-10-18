package com.atenas.client.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atenas.client.gui.HudEditor;
import com.atenas.client.modules.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
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
    // Caches de rendimiento: find() se llama por CADA entidad y varias veces por cuadro (mixins), y los eventos se despachan solo a lo activado.
    private static final Map<Class<?>, Module> BY_CLASS = new HashMap<Class<?>, Module>();
    private static List<HudModule> HUDS = Collections.emptyList();
    private static HudModule[] HUD_ARRAY = new HudModule[0];
    private static Module[] ENABLED = new Module[0];
    private final Minecraft mc = Minecraft.getMinecraft();

    private ModuleManager() {}

    /**
     * Aqui se decide en que seccion va cada mod. Secciones del menu:
     *  - HUD: paneles de informacion (FPS, ping, coordenadas, reloj, memoria, velocidad).
     *  - PvP: lo que se usa peleando (mira, combo, alcance, CPS, teclas, armadura, pociones, sin fuego, resalte, movimiento).
     *  - Animaciones: animaciones 1.7.   - Rendimiento: FPS.
     *  - Chat: todo lo del chat (hora, copiar, sin fondo, animacion, atajos de texto).
     *  - Utilidades: lo demas (perspectiva, fullbright, hora del mundo).
     * El zoom ya no existe: lo trae OptiFine.
     */
    public static void init() {
        MODULES.clear();
        // HUD
        add(new FpsModule(), Module.Category.HUD);
        add(new PingModule(), Module.Category.HUD);
        add(new CoordsModule(), Module.Category.HUD);
        add(new ClockModule(), Module.Category.HUD);
        add(new MemoryModule(), Module.Category.HUD);
        add(new SpeedModule(), Module.Category.HUD);
        add(new DirectionModule(), Module.Category.HUD);
        add(new ScoreboardModule(), Module.Category.HUD);
        add(new TabListModule(), Module.Category.HUD);
        // PvP
        add(new CrosshairModule(), Module.Category.PVP);
        add(new ComboModule(), Module.Category.PVP);
        add(new ReachModule(), Module.Category.PVP);
        add(new CpsModule(), Module.Category.PVP);
        add(new KeystrokesModule(), Module.Category.PVP);
        add(new ArmorModule(), Module.Category.PVP);
        add(new PotionModule(), Module.Category.PVP);
        add(new MovementModule(), Module.Category.PVP);
        add(new LowFireModule(), Module.Category.PVP);
        add(new BlockOverlayModule(), Module.Category.PVP);
        // Animaciones (estilo 1.7)
        add(new OldAnimationsModule(), Module.Category.ANIM);
        // Rendimiento (estilo OptiFine)
        add(new PerformanceModule(), Module.Category.PERF);
        // Chat
        add(new ChatModule(), Module.Category.CHAT);
        add(new AutotextModule(), Module.Category.CHAT);
        // Utilidades
        add(new PerspectiveModule(), Module.Category.UTIL);
        add(new FullbrightModule(), Module.Category.UTIL);
        add(new TimeModule(), Module.Category.UTIL);
        add(new BadgeModule(), Module.Category.UTIL);
        add(new ButtonsModule(), Module.Category.UTIL);
        BY_CLASS.clear();
        for (Module m : MODULES) BY_CLASS.put(m.getClass(), m);
        List<HudModule> hs = new ArrayList<HudModule>();
        for (Module m : MODULES) if (m instanceof HudModule) hs.add((HudModule) m);
        HUD_ARRAY = hs.toArray(new HudModule[hs.size()]);
        HUDS = Collections.unmodifiableList(hs);
        refreshEnabled();
    }

    /** Vuelve a armar la lista de modulos activos (la llama Module.setEnabled). */
    static void refreshEnabled() {
        List<Module> on = new ArrayList<Module>();
        for (Module m : MODULES) if (m.isEnabled()) on.add(m);
        ENABLED = on.toArray(new Module[on.size()]);
    }

    private static void add(Module m, Module.Category category) {
        m.category = category;
        MODULES.add(m);
    }

    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }

    /** Busca un modulo por su clase (lo usan los mixins). Busqueda directa en un mapa: antes recorria todos los modulos en cada llamada. */
    public static <T extends Module> T find(Class<T> type) {
        Module m = BY_CLASS.get(type);
        if (m != null) return type.cast(m);
        for (Module x : MODULES) {
            if (type.isInstance(x)) { BY_CLASS.put(type, x); return type.cast(x); }
        }
        return null;
    }

    /** Los modulos con panel en pantalla (misma lista siempre: no se crea una nueva en cada cuadro). */
    public static List<HudModule> hud() { return HUDS; }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        for (Module m : ENABLED) m.onTick();
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (!Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        for (Module m : ENABLED) m.onKey(key);
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent e) {
        for (Module m : ENABLED) m.onFov(e);
    }

    @SubscribeEvent
    public void onFire(RenderBlockOverlayEvent e) {
        for (Module m : ENABLED) m.onFireOverlay(e);
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent e) {
        for (Module m : ENABLED) m.onRenderHand(e);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        for (Module m : ENABLED) m.onChat(e);
    }

    @SubscribeEvent
    public void onGuiMouse(GuiScreenEvent.MouseInputEvent.Pre e) {
        for (Module m : ENABLED) m.onGuiMouse(e);
    }

    @SubscribeEvent
    public void onHighlight(DrawBlockHighlightEvent e) {
        for (Module m : ENABLED) m.onHighlight(e);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        for (Module m : ENABLED) m.onMouse(e);
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre e) {
        for (Module m : ENABLED) m.onOverlayPre(e);
    }

    @SubscribeEvent
    public void onOverlayPost(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.currentScreen instanceof HudEditor) return;
        ScaledResolution sr = e.resolution;
        for (HudModule h : HUD_ARRAY) {
            if (!h.isEnabled()) continue;
            h.ensurePositioned(sr);
            h.render();
        }
    }
}
