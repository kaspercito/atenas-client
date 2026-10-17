# Atenas Client (Forge 1.8.9)

Cliente PvP propio para Minecraft 1.8.9, hecho como mod de Forge. Todo el codigo es nuestro.

## Que incluye

**HUD (movible con el Editor HUD):** FPS, CPS, Ping, Coordenadas, Combo, Alcance, Reloj, Teclas (WASD + clics con CPS), **Armadura** (vertical u horizontal, durabilidad en numero o %, barra, parpadeo con poca durabilidad, contador de flechas con el arco) y **Pociones** (icono del efecto, nombre con nivel en romano y tiempo que cambia de color y parpadea al acabarse).

**PvP:** **Movimiento** (Sprint automatico + Agacharse toggle en un solo mod), Mira personalizada (con vista previa en vivo en su menu de ajustes) (5 estilos: cruz, T, punto, circulo, cuadrado; tamano, separacion fija o dinamica, grosor, opacidad, color, punto, contorno y otro color al apuntar a una entidad), Sin Fuego.

**Animaciones (estilo 1.7):** un solo mod `Animaciones 1.7` (espada, comer/beber y arco; mantener clic = balanceo continuo; ahora puedes bloquear con el clic derecho aunque mantengas el izquierdo, y trae la posicion/tamano del objeto en mano).

**Rendimiento (estilo OptiFine):** graficos rapidos, sin nubes, sin sombras de entidades, iluminacion simple, particulas minimas, distancia de render y limite de FPS. Devuelve tus ajustes al apagarlo.

**Chat:** un solo mod `Chat` con hora [HH:mm] en cada mensaje, copiar un mensaje con clic derecho (con o sin la hora), chat sin fondo y animacion suave al llegar un mensaje (duracion ajustable) y chat infinito (no borra los mensajes viejos; vanilla guarda solo 100). Aqui tambien esta **Autotext** (tecla **o boton del mouse** -> mensaje/comando; se edita dentro del menu: Mods > Autotext > engranaje).

**Utilidades:** Perspective, Fullbright y Hora (fija la hora del mundo solo para ti). El Zoom se quito: lo trae OptiFine.

**Secciones del menu:** HUD (FPS, Ping, Coordenadas, Reloj, Memoria, Velocidad), PvP (Mira, Combo, Alcance, CPS, Teclas, Armadura, Pociones, Movimiento, Sin Fuego, Resalte de bloque), Animaciones, Rendimiento, Chat y Utilidades. La seccion de cada mod se decide en un solo sitio: `ModuleManager.init()`.

**Idioma:** todo el cliente (mods, ajustes, menus y menu principal) sale en el idioma del juego: espanol si el juego esta en espanol, ingles en cualquier otro idioma. Los textos nuevos se traducen en `core/Lang.java`.

**Cosmeticos (se ven en tu personaje y, con el Worker, en los demas usuarios de Atenas):** 20 capas de tela segmentada (6 animadas), 9 alas de plumas con aleteo, 9 sombreros y 6 mascotas que te siguen (perro, husky, dalmata, gato, gato negro y zorro); son los mismos que en el mod de 1.20.1. Vista previa giratoria en el menu.

**Interfaz:** menu principal futurista (Principal, Singleplayer, Multiplayer, Opciones y X para cerrar), Amigos y chat (Shift+Tab, menu principal y pausa) con solicitudes, estado en linea y avisos propios (con la cara del jugador); menu principal de Atenas, menu del cliente (RShift) con ajustes por modulo, Editor HUD (arrastrar + rueda para tamano). Config en `.minecraft/config/atenasclient.json`.

Sin hacks: todo es visual o de utilidad, no da ventajas injustas.

## Como compilarlo

Guia paso a paso (con instalacion de Java): **COMPILAR.md**.

Requisitos: **JDK 8** y Gradle 2.14.1 (ForgeGradle 2.1 no funciona con versiones nuevas).

Camino mas facil y fiable:
1. Descarga el **Forge 1.8.9 MDK** (11.15.1.2318) desde files.minecraftforge.net.
2. Copia la carpeta `src/` de este proyecto dentro del MDK (reemplaza su `src/`).
3. Abre una terminal en el MDK y ejecuta:
   - `gradlew setupDecompWorkspace`
   - `gradlew build`
4. El jar queda en `build/libs/`. Ponlo en la carpeta `mods` del perfil Forge 1.8.9.

(Tambien incluyo un `build.gradle` por si prefieres compilar directo, pero el MDK es lo mas seguro.)

## Personalizar

- **Logo:** reemplaza `src/main/resources/assets/atenasclient/textures/gui/logo.png` (256x256).
- **Capas:** 64x32, cara externa en (1,1) de 10x16. Pon el PNG en `textures/cosmetics/capes/` y agrega una linea en `Cosmetics.java`.
- **Regenerar texturas de ejemplo:** `python3 tools/gen_textures.py` (necesita Pillow).

## Limitaciones (a proposito)

- Los cosmeticos de otros jugadores solo se ven si despliegas el Worker de `worker/` y pones su URL (ver COMPILAR.md). Un nick lo reclama quien lo registra primero (token por instalacion); sin cuentas de Mojang no se puede verificar mas.
- **Animaciones 1.7 (Mixin):** `Animaciones 1.7` hace que el objeto se balancee al bloquear, comer/beber y tensar el arco en primera persona (`MixinItemRenderer`) ; el golpe lo dispara `OldAnimationsModule` (un clic = un balanceo, mantener clic sobre un bloque = balanceo continuo, tambien al comer y con el arco). `MixinMinecraft` solo se usa si activas "Picar el bloque de verdad". Siguen SIN incluir: la pose de espada en tercera persona, el color de golpe y el motion blur.
- **OptiFine:** no se incluye ni se reescribe (su licencia no permite redistribuirlo y es de codigo cerrado). El launcher lo baja solo de optifine.net en el PC del jugador la primera vez que juega (o con el boton de Ajustes > OptiFine); si eso falla, queda el metodo manual. Con OptiFine cargado, el modulo Rendimiento no toca nada (OptiFine manda en los graficos). El launcher acepta el .jar aunque el navegador lo haya renombrado ("(1)", espacios, preview_), comprueba que de verdad sea OptiFine, lo busca solo en Descargas/Escritorio/Documentos al pulsar Jugar y quita cualquier otro OptiFine de la carpeta mods para que no haya dos.
- **Cosmeticos:** 14 capas, 9 alas y 9 sombreros (los nuevos se generan con `tools/gen_cosmetics.py`; no hace falta tocar el Worker, acepta cualquier id).
- Mixin va empaquetado dentro del jar (ver `build.gradle`). Si el mixin no engancha, el juego arranca igual y el modulo no hace nada: revisa `logs/latest.log` buscando `[mixin]`.

## Estado

Escrito contra la API de Forge 1.8.9 (MCP stable_22). El codigo paso una revision de sintaxis, pero **no se pudo compilar contra Forge en el entorno donde se escribio**. Si al compilar aparece algun error, es un ajuste menor de nombres: copia el mensaje y se corrige.
