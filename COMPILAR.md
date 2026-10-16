# Guia para compilar Atenas Client (Windows)

## 1. Instalar Java 8 (JDK)
1. Entra a https://adoptium.net/temurin/releases/?version=8 y baja **Temurin 8 (JDK) - Windows x64 - .msi**.
2. Al instalar, activa **"Set JAVA_HOME variable"** y **"Add to PATH"**.
3. Abre una terminal NUEVA (cmd) y comprueba:  `javac -version`  -> debe decir `1.8.0_xxx`.
   Si dice otra version, cierra y abre la terminal; si sigue igual, ejecuta (ajusta la ruta):
   `set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.xxx-hotspot`
   `set PATH=%JAVA_HOME%\bin;%PATH%`

## 2. Preparar el proyecto
1. Baja el **Forge 1.8.9 MDK (11.15.1.2318)** desde https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html (boton "Mdk").
2. Descomprimelo en una carpeta sin espacios ni tildes, por ejemplo `C:\atenas-mdk`.
3. Borra la carpeta `src` del MDK y copia dentro la carpeta `src` de este proyecto. **Reemplaza tambien el `build.gradle`** por el de este proyecto.

## 3. Compilar
En la terminal, dentro de `C:\atenas-mdk`:
1. `gradlew setupDecompWorkspace`   (tarda varios minutos y necesita internet; solo la primera vez)
2. `gradlew build`
3. El mod queda en `build\libs\AtenasClient-1.0.0.jar` (no uses el `-sources`).

Para probarlo sin instalar nada: `gradlew runClient` abre Minecraft con el mod.

## 4. Instalar
Con Forge 1.8.9 instalado, copia el jar a la carpeta `mods` del perfil (`.minecraft\mods`).

## Si falla
- "Could not resolve ForgeGradle": revisa que el build.gradle sea el de este proyecto y ejecuta `gradlew --refresh-dependencies`.
- Errores de Java/version: confirma que `javac -version` dice 1.8.
- Error de compilacion en el codigo: copia el mensaje completo para corregirlo.
- Alternativa sin instalar Java: sube el proyecto a GitHub; el workflow `.github/workflows/build.yml` compila y deja el jar en Actions > Artifacts.

## Cosmeticos en linea (que otros te vean)
1. En la carpeta `worker`: `npx wrangler kv namespace create COSMETICS` (en wrangler antiguo: `npx wrangler kv:namespace create COSMETICS`).
2. Pega el id en `worker/wrangler.toml`.
3. `npx wrangler deploy` y copia la URL que te da (https://atenas-cosmetics.TU-SUBDOMINIO.workers.dev).
4. Pegala en `CosmeticSync.DEFAULT_URL` (src/main/java/com/atenas/client/cosmetics/CosmeticSync.java) antes de compilar.

## Instalar como VERSION (en vez de la carpeta mods)
Requisitos: Forge 1.8.9 (11.15.1.2318) ya instalado y Node.js.
1. Compila el jar (pasos de arriba).
2. En la carpeta `version`: `node instalar-version.mjs ..\build\libs\AtenasClient-1.0.0.jar --perfil`
   (`--mc "ruta\.minecraft"` si tu carpeta no es la normal; `--perfil` agrega el perfil al launcher oficial).
3. Elige la version **AtenasClient-1.8.9** en el launcher.
4. Si copiaste el jar a `mods`, borralo de ahi (si no, se carga dos veces).
El script crea `versions/AtenasClient-1.8.9/` (copia de la version de Forge + el mod como libreria) y guarda el jar en `libraries/com/atenas/atenasclient/`.
Nota: Forge detecta mods del classpath, asi que deberia cargarlo; si en tu caso no aparece "Atenas Client" en la lista de mods, usa la carpeta mods.

---------------------------------------------------------------------
# YA CONECTADO AL LAUNCHER
El Atenas Launcher (carpeta `atenas-launcher`) ya trae el perfil "Atenas Client 1.8.9": instala Java 8, Forge 1.8.9 y el jar de la ultima Release de este repo, y lo actualiza solo. Solo necesitas:
1. Subir este proyecto a GitHub (repo PUBLICO; por defecto el launcher busca `kaspercito/AtenasClient`, se cambia en `launcher.config.json` > `clientPack.repo`).
2. Publicar una version: `git tag v1.0.0 && git push origin v1.0.0` y esperar a que Actions termine (la Release debe traer `AtenasClient-1.0.0.jar`).
3. Abrir el launcher, elegir "Atenas Client 1.8.9" y Jugar.
Para probar un jar sin publicar: copialo a `%APPDATA%\Atenas Launcher\client-local\`.
(`launcher-integration/client-updater.js` queda como referencia; el launcher ya usa su propia version en `src/lib/client.js`.)

---------------------------------------------------------------------
# AUTOMATICO (como Lunar / LabyMod) - detalle del diseno original

La idea: el launcher descarga y actualiza el cliente solo, sin que el jugador toque nada.

## Flujo
1. Subes el proyecto a un repo de GitHub (ej. TU-USUARIO/AtenasClient).
2. Cada vez que quieres sacar una version nueva:
   `git tag v1.0.1` y `git push origin v1.0.1`
   -> GitHub Actions compila y publica una Release con `AtenasClient-1.0.1.jar`.
3. El launcher, antes de abrir el juego, ejecuta `launcher-integration/client-updater.js`:
   consulta la ultima Release, descarga el jar a la carpeta `mods` de la instancia si es nueva y borra la vieja.
   Si no hay internet, usa el jar que ya tiene.
4. Resultado: publicas un tag y a todos les llega la actualizacion al abrir el launcher.

## Uso en el launcher (Electron/Node)
```js
const { ensureClient } = require('./client-updater');
const r = await ensureClient({
  repo: 'TU-USUARIO/AtenasClient',
  modsDir: path.join(gameDir, 'mods'),   // carpeta de la instancia 1.8.9 del launcher
  onProgress: (hecho, total) => { /* tu barra de progreso */ }
});
// r = { updated, tag, file }
```
Recomendado: que la instancia tenga su propia carpeta de juego (gameDir) para no mezclar mods con el .minecraft normal del jugador.

## Forge 1.8.9 automatico
El jar necesita Forge 1.8.9 (11.15.1.2318) en la instancia. El launcher puede instalarlo solo con el instalador oficial:
`java -jar forge-1.8.9-11.15.1.2318-1.8.9-installer.jar --installClient <carpeta .minecraft de la instancia>`
(se hace una sola vez y se comprueba si existe la carpeta versions/1.8.9-forge...).

## Orden recomendado
1. Instalar Java 8 y compilar a mano UNA vez (`gradlew runClient`) para confirmar que abre y se ve bien.
2. Si hay errores de compilacion, pasar el mensaje para corregirlo antes de publicar.
3. Subir a GitHub, desplegar el Worker de cosmeticos y poner su URL en CosmeticSync.DEFAULT_URL.
4. Publicar el tag v1.0.0 y comprobar que la Release trae el jar.
5. Integrar client-updater.js (o pasar el zip del launcher para que lo integre).

---------------------------------------------------------------------
# AMIGOS Y CHAT (v1.1.0)
El cliente habla con el mismo Worker que el launcher (`social-worker/` del launcher; guia en su LEEME.md). La URL se lee de `%USERPROFILE%\.atenasclient\social.json` (la crea el launcher) y, si no existe, de `SocialClient.DEFAULT_URL` (src/main/java/com/atenas/client/social/SocialClient.java). Atajo: Shift+Tab.
