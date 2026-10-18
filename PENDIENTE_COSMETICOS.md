# PENDIENTE — Mejora HD de cosméticos (leer esto primero al retomar)

Para retomar: envía este zip y pega "sigue con PENDIENTE_COSMETICOS.md". Respuestas en español, entregar zip listo.

## Ya hecho (en este zip)
- **Sombreros HD (12):** gorra, mago, corona, galera, gato, cuernos, auriculares, conejo, halo + **nuevos: pirata (tricornio), vikingo, laurel** (ya registrados en `Cosmetics.java`).
  - La gorra se afinó: domo más bajo y redondo por encima de la cabeza (sin 'almohadilla') y sin que asomen las esquinas de la cabeza.
  - Generador: `tools/hatlib.py` (motor: superficies paramétricas, lathe, tube, slab, decal, atlas, rasterizador de vista previa),
    `tools/hatpaint.py` (pintura: tela, metal, gemas, brillos), `tools/gen_hats.py` (un `@hat def hat_<id>()` por sombrero).
  - Regenerar todo: `python3 tools/gen_hats.py src/main/resources/assets/atenasclient [ids...]` (Pillow + numpy).
    Escribe `models/hats/<id>.atm`, `textures/cosmetics/hats/<id>.png` (atlas 1024 o 2048) e `icons/hat_<id>.png` (128x128 render 3D).
  - Java: `cosmetics/HatMesh.java` (carga .atm, display lists, flags 1 emisivo / 2 late / 4 gira / 8 suave),
    `cosmetics/HatLayer.java` (usa malla; los `item.custom` del catálogo siguen con el `HatModel` de cajas 64x64),
    `cosmetics/TexFilter.java` (filtro lineal + mipmaps una vez por textura; también aplicado en `CapeLayer`).
- Compila con stubs de Minecraft; **NUNCA se probó dentro del juego** (el proxy bloquea Forge). Probar y reportar errores/visual.

## Falta (por prioridad)
1. ~~Capas (32)~~ HECHO: cara 160x256, hoja 1024x512 por fotograma (tira 1024x6144; las 5 estáticas 1024x512), iconos 128x128.
   Generador unificado: `python3 tools/gen_capes_hd.py src/main/resources/assets/atenasclient [ids...] [--contact hoja.png]`
   (módulos `hd_core.py`, `hd_scenes.py` 10 escenas, `hd_fields.py` 15 campos, `hd_paint.py` atenas/matrix/5 estáticas). Reemplaza a gen_capes_v2/gen_capes_anim/gen_capes (esos escriben a baja resolución: NO ejecutarlos).
   Peso: capes 5 MB -> 28 MB en el jar. VRAM: ~33 MB por capa distinta visible. Si da problemas: compactar la hoja (solo se usa 368x288 de cada 1024x512; requiere UV propio en CapeModel para las capas HD) o bajar a 128x205.
   Pendiente en juego: mirar parpadeo entre fotogramas con los mipmaps (TexFilter limita a nivel 3) y que los iconos 128 se vean bien (se dibujan a 32x32 con filtro NEAREST; si salen granulados, usar filtro lineal en esos iconos).
   Opcional: animar las 5 estáticas (cambiar a addAnim en Cosmetics.java y dar 12 fotogramas en hd_paint.py). El mod social 1.20.1 NO se tocó (usar --mod <assets_atenassocial> para escribir allí también; revisar que su CapeModel use el mismo UV).
2. **Alas** (generadores ya en tools/: gen_wings.py, gen_membrane.py): plumas `wings/*.png` (256x1040, `gen_wings.py`) y membrana (128x128, `gen_membrane.py`): más resolución y detalle (plumas con barbas, venas, brillo).
3. **Mascotas** (tools/gen_pets.py): `pets/*.png` (384x192, `gen_pets.py`): pelaje con sombreado/detalle. Modelo de cajas en `PetModel`/`PetSpec` (UV fijo).
4. **Iconos** de capas y alas (32x32 hoy): regenerar a 128x128 (la GUI ya escala: `CosmeticsScreen` dibuja 32x32 con textura "32f", sirve cualquier resolución).
5. ~~Sombreros nuevos~~ HECHO (pirata, vikingo, laurel, gorra afinada). Solo falta verlos dentro del juego: revisar que el laurel (hojas de doble cara) y el vikingo (cuernos anchos) no se crucen con capa/alas.
6. **Mod social 1.20.1 (`atenas-social`)**: portar lo mismo (HatMesh/TexFilter con la API de 1.20.1: PoseStack/VertexConsumer en vez de display lists;
   sus cosméticos están en `social/src/main/java/com/atenas/social/cosmetics/`, y `CosmeticRender.java` dibuja sombreros). Copiar los .atm/png/iconos generados.
7. **Launcher (`atenas-launcher`):** solo si hace falta (subir capa: `subir-capa.html`, worker `social-worker/`). Sombreros del catálogo siguen en el UV viejo de cajas.

## Notas técnicas
- Coordenadas del generador: y arriba, z adelante (cara), cabeza y 0..8, x ±4. Al exportar se pasa a ModelRenderer (y→-y, z→-z). Unidad = 1/16 de bloque.
- Formato .atm (big endian): 'ATM1', int grupos; por grupo: UTF nombre, byte flags, int quads, 32 floats/quad (4 vértices × x y z u v nx ny nz).
- MC 1.8.9 usa filtro NEAREST sin mipmaps en texturas de entidad: por eso `TexFilter`. Texturas de capas animadas: mipmaps limitados a nivel 3 para no mezclar fotogramas.
- Capas custom del catálogo (descargadas) usan el UV viejo de la caja de Mojang: no cambiar el mapeo UV de `CapeModel`.
