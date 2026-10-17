package com.atenas.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

/**
 * Deja el servidor de Atenas (arefy.net) primero en la lista de Multijugador, con una estrella. Si el launcher manda otro
 * servidor para este pack (-Datenas.server / -Datenas.servername), se usa ese. Se llama justo antes de abrir Multijugador,
 * asi la pantalla lee la lista ya arreglada. No entra solo al servidor: solo lo pone arriba.
 */
public final class FeaturedServer {
    public static final String DEFAULT_IP = "arefy.net";
    public static final String DEFAULT_NAME = "Arefy";
    /** Estrella amarilla y el resto del nombre en blanco (codigos de color de Minecraft). */
    public static final String STAR = "\u00A7e\u2605 \u00A7f";

    private FeaturedServer() {}

    private static String prop(String k) {
        String v = System.getProperty(k);
        return v == null ? "" : v.trim();
    }

    public static String ip() {
        String v = prop("atenas.server");
        return v.isEmpty() ? DEFAULT_IP : v;
    }

    public static String title() {
        String n = prop("atenas.servername");
        return STAR + (n.isEmpty() ? DEFAULT_NAME : n);
    }

    public static void ensure() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            String ip = ip(), title = title();
            ServerList list = new ServerList(mc);
            list.loadServerList();
            int found = -1;
            for (int i = 0; i < list.countServers(); i++) {
                if (list.getServerData(i).serverIP.equalsIgnoreCase(ip)) { found = i; break; }
            }
            boolean changed = false;
            if (found < 0) {
                list.addServerData(new ServerData(title, ip, false));
                found = list.countServers() - 1;
                changed = true;
            } else if (!title.equals(list.getServerData(found).serverName)) {
                list.getServerData(found).serverName = title;
                changed = true;
            }
            for (int i = found; i > 0; i--) { list.swapServers(i, i - 1); changed = true; }
            if (changed) list.saveServerList();
        } catch (Exception e) {
            System.err.println("[AtenasClient] servidor destacado: " + e);
        }
    }
}
