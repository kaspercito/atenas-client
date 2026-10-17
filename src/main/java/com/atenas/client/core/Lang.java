package com.atenas.client.core;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;

/**
 * Idioma del cliente = idioma del juego (Opciones > Idioma). Si el juego esta en espanol (es_ES, es_MX, es_EC...) todo sale en
 * espanol; con cualquier otro idioma sale en ingles. Los textos del codigo se escriben en espanol y aqui se traducen por texto
 * exacto: {@link #tr(String)} devuelve el ingles si el juego no esta en espanol y, si no hay traduccion, el mismo texto.
 *
 * Los nombres de los mods y de sus ajustes se guardan en la config con su nombre en espanol (no cambian al cambiar de idioma);
 * solo se traduce lo que se muestra en pantalla ({@link com.atenas.client.gui.Draw} traduce todo lo que dibuja).
 */
public final class Lang {

    private static final Map<String, String> EN = new HashMap<String, String>();
    private static String lastCode;
    private static boolean lastEs = true;

    private Lang() {}

    /** true si el idioma del juego es espanol. */
    public static boolean es() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null || mc.gameSettings.language == null) return true;
        String code = mc.gameSettings.language;
        if (!code.equals(lastCode)) {
            lastCode = code;
            lastEs = code.toLowerCase(java.util.Locale.ROOT).startsWith("es");
        }
        return lastEs;
    }

    /** Texto segun el idioma, escrito en el sitio: t("Un jugador", "Singleplayer"). */
    public static String t(String es, String en) { return es() ? es : en; }

    /** Traduce un texto escrito en espanol (por texto exacto). Sin traduccion: lo devuelve igual. */
    public static String tr(String es) {
        if (es == null || es.isEmpty() || es()) return es;
        String en = EN.get(es);
        return en != null ? en : es;
    }

    private static void p(String es, String en) { EN.put(es, en); }

    static {
        // --- categorias
        p("Animaciones", "Animations");
        p("Rendimiento", "Performance");
        p("Utilidades", "Utilities");
        p("Todos", "All");

        // --- nombres y descripciones de los mods
        p("Armadura", "Armor");
        p("Armadura y objeto en mano con su durabilidad. Muévelo y cámbiale el tamaño en el Editor HUD.",
          "Armor and held item with their durability. Move and resize it in the HUD Editor.");
        p("Autotext", "Autotext");
        p("Asigna una tecla o un botón del mouse a un mensaje o comando que se envía al chat al instante.",
          "Bind a key or mouse button to a message or command that is sent to chat instantly.");
        p("Resalte de bloque", "Block outline");
        p("Cambia el contorno del bloque que miras: color, grosor y relleno.",
          "Changes the outline of the block you look at: color, thickness and fill.");
        p("Chat", "Chat");
        p("Mejoras del chat: hora en cada mensaje, copiar mensajes con clic derecho, sin fondo, animación suave e historial infinito.",
          "Chat improvements: timestamps, copy messages with right click, no background, smooth animation and unlimited history.");
        p("Reloj", "Clock");
        p("Muestra la hora real de tu PC.", "Shows your computer's real time.");
        p("Combo", "Combo");
        p("Golpes seguidos sin recibir daño.", "Hits in a row without taking damage.");
        p("Coordenadas", "Coordinates");
        p("Tu posición y hacia dónde miras.", "Your position and the direction you face.");
        p("CPS", "CPS");
        p("Clics por segundo (izquierdo | derecho).", "Clicks per second (left | right).");
        p("Mira", "Crosshair");
        p("Mira personalizada: estilo (cruz, T, punto, círculo, cuadrado), tamaño, separación, grosor, opacidad y color.",
          "Custom crosshair: style (cross, T, dot, circle, square), size, gap, thickness, opacity and color.");
        p("FPS", "FPS");
        p("Muestra tus fotogramas por segundo.", "Shows your frames per second.");
        p("Fullbright", "Fullbright");
        p("Máximo brillo: ves bien en cuevas y de noche.", "Maximum brightness: see clearly in caves and at night.");
        p("Teclas", "Keystrokes");
        p("Muestra WASD, clics y espacio (con CPS).", "Shows WASD, clicks and space (with CPS).");
        p("Sin Fuego", "Low Fire");
        p("Oculta el fuego que tapa la pantalla cuando te quemas.", "Hides the fire that covers your screen when you burn.");
        p("Memoria", "Memory");
        p("Muestra cuánta RAM usa Minecraft.", "Shows how much RAM Minecraft is using.");
        p("Movimiento", "Movement");
        p("Sprint automático al avanzar y agacharse con toggle (pulsas una vez para quedarte agachado, otra para soltar).",
          "Auto sprint when moving forward and toggle sneak (press once to stay crouched, again to release).");
        p("Animaciones 1.7", "1.7 Animations");
        p("Todas las animaciones 1.7 juntas: balanceo al bloquear con la espada, comer/beber y usar el arco, y golpear bloques mientras usas el objeto.",
          "All the 1.7 animations together: swing while blocking with the sword, eating/drinking and using the bow, and hitting blocks while using an item.");
        p("Más FPS en granjas y zonas con muchos mobs: no dibuja los mobs que están lejos (los jugadores siempre se ven). Los gráficos los maneja OptiFine.",
          "More FPS in farms and mob-heavy areas: mobs far away are not drawn (players are always shown). Graphics are handled by OptiFine.");
        p("Perspective", "Perspective");
        p("Mantén la tecla (Alt izquierdo por defecto) para mirar alrededor de tu personaje sin girarlo. Al soltarla vuelve tu vista de antes.",
          "Hold the key (Left Alt by default) to look around your character without turning it. Releasing it restores your previous view.");
        p("Ping", "Ping");
        p("Tu latencia con el servidor.", "Your latency to the server.");
        p("Pociones", "Potions");
        p("Efectos activos con su icono, nombre, nivel y tiempo restante.", "Active effects with icon, name, level and time left.");
        p("Alcance", "Reach");
        p("Distancia de tu último golpe.", "Distance of your last hit.");
        p("Velocidad", "Speed");
        p("Tu velocidad en bloques por segundo.", "Your speed in blocks per second.");
        p("Hora", "World time");
        p("Fija la hora del mundo solo para ti (0 amanecer, 6000 día, 18000 noche).",
          "Sets the world time just for you (0 dawn, 6000 day, 18000 night).");

        // --- ajustes (los de todos los mods)
        p("Escala", "Scale");
        p("Fondo", "Background");
        p("Sombra de texto", "Text shadow");
        p("Barra de acento", "Accent bar");
        p("Color", "Color");
        p("Disposición", "Layout");
        p("Vertical", "Vertical");
        p("Horizontal", "Horizontal");
        p("Texto", "Text");
        p("Durabilidad", "Durability");
        p("Porcentaje", "Percentage");
        p("Ninguno", "None");
        p("Barra de durabilidad", "Durability bar");
        p("Objeto en la mano", "Held item");
        p("Contar flechas con el arco", "Count arrows with the bow");
        p("Ranuras fijas (el panel no cambia de tamaño)", "Fixed slots (the panel never resizes)");
        p("Parpadear con poca durabilidad", "Blink on low durability");
        p("Grosor", "Thickness");
        p("Relleno", "Fill");
        p("Opacidad del relleno", "Fill opacity");
        p("Estilo", "Style");
        p("Cruz", "Cross");
        p("T", "T");
        p("Punto", "Dot");
        p("Círculo", "Circle");
        p("Cuadrado", "Square");
        p("Tamaño", "Size");
        p("Separación", "Gap");
        p("Opacidad", "Opacity");
        p("Punto central", "Center dot");
        p("Contorno", "Outline");
        p("Separación dinámica (se abre al moverte/saltar)", "Dynamic gap (opens when you move/jump)");
        p("Cambiar de color al apuntar a una entidad", "Change color when aiming at an entity");
        p("Color al apuntar", "Color when aiming");
        p("Mostrar CPS", "Show CPS");
        p("Barra espaciadora", "Space bar");
        p("Sprint automático", "Auto sprint");
        p("Agacharse (toggle)", "Sneak (toggle)");
        p("Espada: bloquear + golpear", "Sword: block + hit");
        p("Comer/beber: golpear mientras tanto", "Eat/drink: hit meanwhile");
        p("Arco: golpear mientras tensas", "Bow: hit while drawing");
        p("Mantener clic = balanceo continuo", "Hold click = continuous swing");
        p("Picar el bloque de verdad (puede dar problemas con anticheat)", "Really mine the block (may cause anticheat problems)");
        p("Clic derecho aunque mantengas el izquierdo (bloquear/comer mientras picas)",
          "Right click even while holding left (block/eat while mining)");
        p("Intensidad (modo de respaldo)", "Intensity (fallback mode)");
        p("Mover/cambiar tamaño del objeto en mano", "Move/resize the held item");
        p("Objeto: horizontal", "Item: horizontal");
        p("Objeto: vertical", "Item: vertical");
        p("Objeto: distancia", "Item: distance");
        p("Objeto: tamaño", "Item: size");
        p("Gráficos rápidos", "Fast graphics");
        p("Sin nubes", "No clouds");
        p("Sin sombras de entidades", "No entity shadows");
        p("Iluminación simple", "Simple lighting");
        p("Partículas mínimas", "Minimal particles");
        p("Distancia de render (0 = la tuya)", "Render distance (0 = yours)");
        p("FPS máximos (260 = sin límite)", "Max FPS (260 = unlimited)");
        p("Modo", "Mode");
        p("Mantener", "Hold");
        p("Alternar", "Toggle");
        p("Invertir eje vertical", "Invert vertical axis");
        p("Completo", "Full");
        p("Compacto", "Compact");
        p("Mostrar icono del efecto", "Show effect icon");
        p("Color según el tiempo que queda", "Color by time left");
        p("Parpadear cuando se está acabando", "Blink when running out");
        p("Ordenar por tiempo restante", "Sort by time left");
        p("Hora (ticks)", "Time (ticks)");
        // chat
        p("Hora en cada mensaje", "Timestamp on every message");
        p("Formato 24 horas", "24-hour format");
        p("Chat infinito", "Unlimited chat history");
        p("Copiar con clic derecho", "Copy with right click");
        p("Copiar sin la hora", "Copy without the timestamp");
        p("Sin fondo", "No background");
        p("Animación suave", "Smooth animation");
        p("Duración de la animación (ms)", "Animation duration (ms)");

        // --- HUD / teclas / cuentas
        p("Norte", "North");
        p("Sur", "South");
        p("Este", "East");
        p("Oeste", "West");
        p("Sin tecla", "No key");
        p("Clic izq.", "Left click");
        p("Clic der.", "Right click");
        p("Clic central", "Middle click");
        p("Abrir menú Atenas", "Open Atenas menu");
        p("Perspective (mirar alrededor)", "Perspective (look around)");

        // --- menu de Atenas (RShift)
        p("Mods", "Mods");
        p("Cosméticos", "Cosmetics");
        p("Editor HUD", "HUD Editor");
        p("Amigos", "Friends");
        p("Acerca de", "About");
        p("Buscar...", "Search...");
        p("No hay mods con ese filtro.", "No mods match that filter.");
        p("← Volver", "← Back");
        p("Mods  ›  ", "Mods  ›  ");
        p("Mods → elige un módulo para ver sus ajustes.", "Mods → pick a module to see its settings.");
        p("Editor HUD → arrastra cada módulo y usa la rueda para su tamaño.",
          "HUD Editor → drag each module and use the wheel to resize it.");
        p("RShift  →  abrir / cerrar este menú   ·   Shift+Tab  →  amigos y chat",
          "RShift  →  open / close this menu   ·   Shift+Tab  →  friends and chat");
        p("Sin ventajas injustas: solo mejoras visuales y de utilidad.", "No unfair advantages: only visual and utility improvements.");
        p("Entra a un mundo para ver la vista previa. Igual puedes elegir tus cosméticos.",
          "Join a world to see the preview. You can still pick your cosmetics.");
        p("Animadas", "Animated");
        p("Normal", "Normal");
        p("Apuntando", "Aiming");
        p("apuntando a una entidad", "aiming at an entity");
        p("normal", "normal");
        p("oscuro", "dark");
        p("pasto", "grass");
        p("ATAJOS", "SHORTCUTS");
        p("+ Añadir atajo", "+ Add shortcut");
        p("Sin atajos todavía.", "No shortcuts yet.");
        p("mensaje o /comando", "message or /command");
        p("Pulsa algo...", "Press something...");
        p("AMIGOS Y CHAT", "FRIENDS AND CHAT");
        p("Activar amigos y chat", "Enable friends and chat");
        p("Avisos en pantalla", "On-screen alerts");
        p("Mostrar mi estado a mis amigos", "Show my status to my friends");
        p("Mover en pantalla", "Move on screen");
        p("Activado", "Enabled");
        p("En línea", "Online");
        p("Solo local", "Local only");
        p("Versión ", "Version ");
        p("detectado", "detected");
        p("no detectado (opcional, se activa en Ajustes del launcher)", "not detected (optional, enabled in the launcher settings)");
        p("● Mixin activo", "● Mixin active");
        p("○ Mixin sin comprobar (entra a un mundo con un objeto en la mano)", "○ Mixin not checked (join a world holding an item)");
        p("● OptiFine detectado: este mod no cambia nada (usa los ajustes de OptiFine)",
          "● OptiFine detected: this mod changes nothing (use OptiFine's settings)");
        p("EDITOR HUD", "HUD EDITOR");
        p("Arrastra · Rueda: tamaño · ESC: guardar", "Drag · Wheel: size · ESC: save");
        p("No hay módulos HUD activados. Actívalos en el menú.", "No HUD modules enabled. Turn them on in the menu.");

        // --- menu principal
        p("Un jugador", "Singleplayer");
        p("Multijugador", "Multiplayer");
        p("Opciones", "Options");
        p("Principal", "Main");
        p("Mods y cosméticos", "Mods & cosmetics");
        p("Cuenta", "Account");
        p("Cambiar", "Switch");
        p("Cambiar de cuenta", "Switch account");
        p("● en línea", "● online");
        p("○ desconectado", "○ offline");
        p("Shift+Tab  ·  Amigos y chat", "Shift+Tab  ·  Friends and chat");

        // --- pantalla de cuentas
        p("CUENTAS", "ACCOUNTS");
        p("Volver", "Back");
        p("Cancelar", "Cancel");
        p("Cancelado.", "Cancelled.");
        p("Entrar", "Sign in");
        p("Usar", "Use");
        p("Quitar", "Remove");
        p("Reiniciar", "Restart");
        p("Listo", "Done");
        p("Premium", "Premium");
        p("Sin conexion", "Offline");
        p(" (sin conexion)", " (offline)");
        p("● Activa", "● Active");
        p("Nick de Minecraft", "Minecraft username");
        p("Entrar con otro nombre (sin conexion)", "Sign in with another name (offline)");
        p("+  Agregar cuenta premium (Microsoft)", "+  Add premium account (Microsoft)");
        p("Agrega una premium o entra con un nombre.", "Add a premium account or sign in with a name.");
        p("Esperando al launcher...", "Waiting for the launcher...");
        p("Inicia sesion en la ventana de Microsoft", "Sign in on the Microsoft window");
        p("Se abrio la ventana de Microsoft en el launcher. Si no la ves, busca el launcher en la barra de tareas.",
          "The Microsoft window opened in the launcher. If you can't see it, look for the launcher in the taskbar.");
        p("Se acabo el tiempo de espera.", "Timed out.");
        p("No se pudo completar", "Could not complete");
        p("Cerrar sesion", "Sign out");
        p("Cerrando sesion", "Signing out");
        p("Abre el juego desde el launcher para agregar una cuenta premium.", "Open the game from the launcher to add a premium account.");
        p("Abre el juego desde el launcher para cerrar sesion.", "Open the game from the launcher to sign out.");
        p("Abre el juego desde el launcher para quitar cuentas.", "Open the game from the launcher to remove accounts.");
        p("Abre el juego desde el launcher para usar cuentas premium.", "Open the game from the launcher to use premium accounts.");
        p("El launcher no responde. Abrelo y vuelve a intentar.", "The launcher isn't responding. Open it and try again.");
        p("El launcher todavia no guardo cuentas.", "The launcher hasn't saved any accounts yet.");
        p("El juego se va a cerrar y el launcher lo abre otra vez con esa cuenta.",
          "The game will close and the launcher will reopen it with that account.");
        p("Se borra de la lista del launcher. Tu cuenta no se elimina: puedes agregarla otra vez cuando quieras.",
          "It is removed from the launcher list. Your account is not deleted: you can add it again whenever you want.");
        p("Se quita esta cuenta del launcher y el juego se cierra. Para volver a jugar con ella tendras que iniciar sesion otra vez.",
          "This account is removed from the launcher and the game closes. To play with it again you will have to sign in again.");
    }
}
