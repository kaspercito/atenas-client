# Guía de despliegue (cosméticos, Worker, mod y launcher)

## 1. Worker social (amigos + cosméticos) — una vez, y cada vez que cambie `worker.mjs`
Carpeta: `atenas-launcher/social-worker/` (Node 22+).

```
npx wrangler login
npx wrangler secret put ADMIN_KEY      # pega una clave de 12+ caracteres; sin ella /admin queda apagado
npx wrangler deploy
```
- `database_id` de `wrangler.toml` ya apunta a tu D1 `atenas-social`; las tablas se crean/migran solas al primer uso.
- Comprueba: `https://TU-WORKER.workers.dev/` debe responder `{"name":"atenas-social",...}` y `/admin` pide la clave.
- La mascota **Catalina** se siembra sola como privada (solo `kaspercitoo`). Para darla a otro nick: `/admin` → Catalina → owners.
- Para crear cosméticos nuevos (id `x_...`) usa `/admin`: eliges el modelo base (p. ej. alas `murcielago` para membrana) y subes el PNG con la **misma medida** que el base (mira `tools/check_textures.py`).

## 2. Cliente 1.8.9 (mod Forge)
1. Antes de compilar: `python3 tools/check_textures.py` (debe decir `0 problemas`).
2. Sube la versión en `AtenasClient.java` (`VERSION`) y en `build.gradle` (`modVersion`): deben coincidir.
3. Publica: `git add . && git commit -m "v1.0.5" && git push && git tag v1.0.5 && git push origin v1.0.5`.
   GitHub Actions compila y crea el Release con `AtenasClient-1.0.5.jar`. El launcher lo descarga solo (`client-updater.js`).

## 3. Mod 1.20.1 (atenas-social)
Igual: sube la versión, `git tag vX.Y.Z` y `git push origin vX.Y.Z` (el workflow publica el jar en el Release).

## 4. Launcher
Doble clic en `publicar.bat` (carpeta del launcher): te pide la versión (p. ej. `1.2.10`), hace commit, push y el tag `v...`; Actions construye el instalador y lo sube al Release.

## Verificación premium (Mojang)
Desde esta versión el Worker verifica con Mojang las cuentas premium (`/auth/challenge` + `/auth/verify`): los cosméticos privados (p. ej. Catalina) solo los usa quien tenga la cuenta verificada; las cuentas sin conexión funcionan pero sin cosméticos privados. Hay que desplegar el Worker **antes** que los mods/launcher nuevos. Las columnas nuevas de la base se crean solas al primer uso.

## Orden recomendado cuando cambian los cosméticos
Worker (`wrangler deploy`) → mod 1.8.9 → mod 1.20.1 → launcher. Los clientes viejos siguen funcionando: ignoran lo que no conocen.

## Si algo no se ve
- Catálogo: se pide cada 45 s; se guarda en `%USERPROFILE%\.atenasclient\cosmetics\catalog.json` (texturas descargadas en la misma carpeta; bórrala para forzar una descarga limpia).
- Un cosmético privado que no te corresponde no aparece en tu menú y, si lo habías elegido, se quita con un aviso.
