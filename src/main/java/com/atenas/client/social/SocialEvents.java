package com.atenas.client.social;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.atenas.client.gui.AtenasButton;
import com.atenas.client.gui.Theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Atajo Shift+Tab, boton en el menu de pausa, sincronizacion periodica y avisos (toasts) en pantalla. */
public class SocialEvents {

    private static final int PAUSE_BUTTON_ID = 7331;
    private int tick;

    // ------------------------------------------------------------ avisos (ver Toasts)

    /** Se llama desde el hilo de red cuando llega un estado nuevo. Compara con el anterior y avisa. */
    static void onSnapshot(SocialClient.Snapshot prev, SocialClient.Snapshot next) {
        if (prev == null) return; // la primera vez no se avisa de lo que ya estaba
        Map<String, SocialClient.Friend> before = new HashMap<String, SocialClient.Friend>();
        for (SocialClient.Friend f : prev.friends) before.put(f.key, f);
        Map<String, Boolean> prevIn = new HashMap<String, Boolean>();
        for (SocialClient.Req r : prev.incoming) prevIn.put(r.key, true);
        Map<String, Boolean> prevOut = new HashMap<String, Boolean>();
        for (SocialClient.Req r : prev.outgoing) prevOut.put(r.key, true);

        for (SocialClient.Req r : next.incoming) {
            if (!prevIn.containsKey(r.key)) Toasts.push("Solicitud de amistad", r.name + " quiere ser tu amigo", r.name, Theme.ACCENT);
        }
        for (SocialClient.Friend f : next.friends) {
            SocialClient.Friend p = before.get(f.key);
            if (p == null) {
                if (prevOut.containsKey(f.key)) Toasts.push("Nuevo amigo", f.name + " acepto tu solicitud", f.name, 0xFF55FF99);
                continue;
            }
            if (f.unread > 0 && f.previewId > p.previewId) {
                if (!SocialScreen.isChattingWith(f.key)) Toasts.push(f.name, f.previewBody, f.name, 0xFF55CCFF);
            } else if (p.state.equals("offline") && !f.state.equals("offline")) {
                Toasts.push("Amigos", f.name + (f.playing() ? " esta jugando" : " esta en linea"), f.name, 0xFF55FF99);
            }
        }
    }

    // ------------------------------------------------------------ eventos

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getSession() == null) return;
        if (++tick % 10 != 0) return; // cada medio segundo
        boolean inGame = mc.theWorld != null && mc.thePlayer != null;
        boolean screen = mc.currentScreen instanceof SocialScreen;
        long every = screen ? 4000L : (inGame ? 25000L : 15000L);
        String server = "";
        if (inGame) {
            net.minecraft.client.multiplayer.ServerData sd = mc.getCurrentServerData();
            server = mc.isSingleplayer() || sd == null ? "" : sd.serverIP;
        }
        if (SocialClient.snapshot() == null || SocialClient.sinceLastSync() > every) {
            SocialClient.sync(inGame, server);
        } else if (!screen && SocialClient.sinceLastPoll() > 4000L) {
            // consulta rapida: solo si algo cambio se baja el estado completo (ver SocialClient.poll)
            SocialClient.poll(inGame, server);
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_TAB
                && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
            mc.displayGuiScreen(new SocialScreen(null));
        }
    }

    /**
     * Shift+Tab dentro de los menus (pausa, un jugador, multijugador, opciones). Con una pantalla abierta el juego no manda
     * KeyInputEvent, asi que el atajo de arriba solo servia en la partida. El menu principal de Atenas lo atiende por su cuenta.
     * Solo pantallas sin cajas de texto, para no quitarle el Tab a quien esta escribiendo.
     */
    @SubscribeEvent
    public void onGuiKey(GuiScreenEvent.KeyboardInputEvent.Pre e) {
        if (!Keyboard.getEventKeyState() || Keyboard.getEventKey() != Keyboard.KEY_TAB) return;
        if (!(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) return;
        GuiScreen g = e.gui;
        if (g instanceof GuiIngameMenu || g instanceof GuiMultiplayer || g instanceof GuiSelectWorld || g instanceof GuiOptions) {
            e.setCanceled(true);
            Minecraft.getMinecraft().displayGuiScreen(new SocialScreen(g));
        }
    }

    /** Aviso en el juego (cuando no hay pantalla abierta). */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (Minecraft.getMinecraft().currentScreen != null) return;
        Toasts.draw();
    }

    /** Aviso sobre cualquier pantalla (menu principal, pausa, inventario...). */
    @SubscribeEvent
    public void onScreenDraw(GuiScreenEvent.DrawScreenEvent.Post e) {
        Toasts.draw();
    }

    @SubscribeEvent
    public void onPauseInit(GuiScreenEvent.InitGuiEvent.Post e) {
        if (!(e.gui instanceof GuiIngameMenu)) return;
        int w = 112;
        e.buttonList.add(new AtenasButton(PAUSE_BUTTON_ID, 6, 6, w, 20, badgeLabel("Amigos")));
    }

    @SubscribeEvent
    public void onPauseAction(GuiScreenEvent.ActionPerformedEvent.Post e) {
        if (e.gui instanceof GuiIngameMenu && e.button != null && e.button.id == PAUSE_BUTTON_ID) {
            Minecraft.getMinecraft().displayGuiScreen(new SocialScreen(e.gui));
        }
    }

    public static String badgeLabel(String base) {
        int n = SocialClient.badge();
        return n > 0 ? base + " §e(" + (n > 9 ? "9+" : String.valueOf(n)) + ")" : base;
    }
}
