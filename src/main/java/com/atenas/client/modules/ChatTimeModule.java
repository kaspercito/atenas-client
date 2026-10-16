package com.atenas.client.modules;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

/** Antepone la hora [HH:mm] a cada mensaje del chat. */
public class ChatTimeModule extends Module {
    private final Setting h24;

    public ChatTimeModule() {
        super("Hora en el chat", "Agrega la hora [HH:mm] delante de cada mensaje del chat.", Category.UTIL);
        h24 = addBool("Formato 24 horas", true);
    }

    @Override
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || e.message == null) return; // 2 = barra de accion
        String stamp = "[" + new SimpleDateFormat(h24.bool ? "HH:mm" : "hh:mm a").format(new Date()) + "] ";
        ChatComponentText time = new ChatComponentText(stamp);
        time.getChatStyle().setColor(EnumChatFormatting.GRAY);
        // contenedor vacio: asi el color gris de la hora no se hereda al mensaje
        ChatComponentText wrap = new ChatComponentText("");
        wrap.appendSibling(time);
        wrap.appendSibling(e.message);
        e.message = wrap;
    }
}
