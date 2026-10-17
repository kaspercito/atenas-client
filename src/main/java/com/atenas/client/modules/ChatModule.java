package com.atenas.client.modules;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.lwjgl.input.Mouse;

import com.atenas.client.core.Lang;
import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * Todo lo del chat en un solo mod (antes la hora era un mod aparte):
 * hora [HH:mm] en cada mensaje, copiar un mensaje con clic derecho, chat sin fondo y animacion suave al llegar un mensaje.
 * El fondo y la animacion los aplica MixinGuiNewChat leyendo este mod.
 */
public class ChatModule extends Module {
    private final Setting stamp, h24, copy, copyNoTime, noBg, anim, animMs, endless;
    private long lastMessage;

    private static Field drawnLines, allLines, scrollPos;

    static {
        try {
            drawnLines = ReflectionHelper.findField(GuiNewChat.class, "field_146253_i", "drawnChatLines");
            allLines = ReflectionHelper.findField(GuiNewChat.class, "field_146252_h", "chatLines");
            scrollPos = ReflectionHelper.findField(GuiNewChat.class, "field_146250_j", "scrollPos");
        } catch (Throwable t) {
            System.err.println("[AtenasClient] Chat: no se pudo acceder a las lineas del chat (copiar desactivado): " + t);
            drawnLines = null;
        }
    }

    public ChatModule() {
        super("Chat", "Mejoras del chat: hora en cada mensaje, copiar mensajes con clic derecho, sin fondo, animación suave e historial infinito.", Category.CHAT);
        stamp = addBool("Hora en cada mensaje", true);
        h24 = addBool("Formato 24 horas", true);
        endless = addBool("Chat infinito", true);
        copy = addBool("Copiar con clic derecho", true);
        copyNoTime = addBool("Copiar sin la hora", true);
        noBg = addBool("Sin fondo", true);
        anim = addBool("Animación suave", true);
        animMs = addNum("Duración de la animación (ms)", 400, 100, 1500, 50);
        startEnabled();
    }

    // ---- hora + marca de llegada

    @Override
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || e.message == null) return; // 2 = barra de accion
        lastMessage = System.currentTimeMillis();
        if (!stamp.bool) return;
        String text = "[" + new SimpleDateFormat(h24.bool ? "HH:mm" : "hh:mm a").format(new Date()) + "] ";
        ChatComponentText time = new ChatComponentText(text);
        time.getChatStyle().setColor(EnumChatFormatting.GRAY);
        // contenedor vacio: asi el color gris de la hora no se hereda al mensaje
        ChatComponentText wrap = new ChatComponentText("");
        wrap.appendSibling(time);
        wrap.appendSibling(e.message);
        e.message = wrap;
    }

    // ---- lo que lee el mixin

    /** true = el chat no borra mensajes viejos (vanilla guarda solo los ultimos 100). */
    public boolean keepsAllMessages() { return endless.bool; }

    /** true = no dibujar el fondo oscuro de cada linea. */
    public boolean hidesBackground() { return noBg.bool; }

    /**
     * Cuantos pixeles (de GUI) hay que bajar el chat ahora mismo: al llegar un mensaje todo el chat sube una linea, y esto lo
     * hace subir despacio en vez de dar el salto. 0 = sin animacion (desactivada, ya termino o estas mirando mensajes viejos).
     */
    public float slideOffset(float chatScale) {
        if (!anim.bool || scrolledUp()) return 0f;
        float dur = (float) animMs.num;
        long dt = System.currentTimeMillis() - lastMessage;
        if (dt < 0 || dt >= dur) return 0f;
        float t = dt / dur;
        float ease = 1f - (1f - t) * (1f - t) * (1f - t); // frena al final
        return 9f * chatScale * (1f - ease);
    }

    private boolean scrolledUp() {
        try {
            return scrollPos != null && scrollPos.getInt(mc.ingameGUI.getChatGUI()) > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---- copiar mensajes

    @Override
    public void onGuiMouse(GuiScreenEvent.MouseInputEvent.Pre e) {
        if (!copy.bool || drawnLines == null || !(e.gui instanceof GuiChat)) return;
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 1) return;
        String msg = messageAt(Mouse.getX(), Mouse.getY());
        if (msg == null || msg.isEmpty()) return;
        if (copyNoTime.bool) msg = msg.replaceFirst("^\\[\\d{1,2}:\\d{2}( [AaPp][Mm])?\\] ", "");
        GuiScreen.setClipboardString(msg);
        mc.ingameGUI.setRecordPlaying(Lang.t("§aMensaje copiado", "§aMessage copied"), false);
    }

    /** Texto completo (sin colores) del mensaje que hay bajo el mouse, o null. Mismas cuentas que GuiNewChat.getChatComponent. */
    @SuppressWarnings("unchecked")
    private String messageAt(int mouseX, int mouseY) {
        try {
            GuiNewChat chat = mc.ingameGUI.getChatGUI();
            if (!chat.getChatOpen()) return null;
            List<ChatLine> drawn = (List<ChatLine>) drawnLines.get(chat);
            int sf = new ScaledResolution(mc).getScaleFactor();
            float scale = chat.getChatScale();
            int x = MathHelper.floor_float((float) (mouseX / sf - 3) / scale);
            int y = MathHelper.floor_float((float) (mouseY / sf - 27) / scale);
            if (x < 0 || y < 0) return null;
            int lines = Math.min(chat.getLineCount(), drawn.size());
            int fh = mc.fontRendererObj.FONT_HEIGHT;
            if (x > MathHelper.floor_float((float) chat.getChatWidth() / scale) || y >= fh * lines + lines) return null;
            int idx = y / fh + scrollPos.getInt(chat);
            if (idx < 0 || idx >= drawn.size()) return null;

            ChatLine line = drawn.get(idx);
            String piece = strip(line.getChatComponent().getUnformattedText()).trim();
            // el mensaje original (sin partir en lineas): mismo tick y que contenga este trozo
            for (ChatLine full : (List<ChatLine>) allLines.get(chat)) {
                if (full.getUpdatedCounter() != line.getUpdatedCounter()) continue;
                String whole = strip(full.getChatComponent().getUnformattedText());
                if (piece.isEmpty() || whole.contains(piece)) return whole;
            }
            return piece;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String strip(String s) { return EnumChatFormatting.getTextWithoutFormattingCodes(s); }
}
