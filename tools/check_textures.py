"""Revisa las MEDIDAS de todas las texturas e iconos de cosmeticos contra lo que registra Cosmetics.java.
Uso:  python3 tools/check_textures.py        (necesita Pillow; sale con codigo 1 si hay algun problema)

Medidas esperadas (la imagen puede ser mas grande si es multiplo exacto; aqui se fijan las que usa el juego):
  capa normal   512 x 256            capa animada (12 fotogramas)  512 x 3072 (12 x 256)
  alas plumas   256 x 1040           alas membrana                 128 x 128
  sombrero      64 x 64              mascota                       384 x 192
  icono         32 x 32
Tambien avisa de cosmeticos registrados sin textura o sin icono, y de archivos que no estan registrados.
"""
import os, re, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "src", "main")
TEX = os.path.join(SRC, "resources", "assets", "atenasclient", "textures", "cosmetics")
JAVA = os.path.join(SRC, "java", "com", "atenas", "client", "cosmetics", "Cosmetics.java")
FOLDER = {"CAPE": "capes", "WINGS": "wings", "HAT": "hats", "PET": "pets"}
KEY = {"CAPE": "cape", "WINGS": "wings", "HAT": "hat", "PET": "pet"}

def registered():
    src = open(JAVA, encoding="utf-8").read()
    items = []
    for m in re.finditer(r'(addAnim|addMembrane|add)\(\s*(?:Type\.(\w+),\s*)?"(\w+)"[^;]*?\)\s*;', src):
        fn, typ, iid = m.group(1), m.group(2), m.group(3)
        if fn == "addMembrane": typ = "WINGS"
        if typ not in FOLDER: continue
        frames = 12 if fn == "addAnim" else 1
        items.append((typ, iid, frames, fn == "addMembrane"))
    return items

def expect(typ, frames, membrane):
    if typ == "CAPE": return (512, 256 * frames)
    if typ == "WINGS": return (128, 128) if membrane else (256, 1040)
    if typ == "HAT": return (64, 64)
    return (384, 192)

def main():
    bad, seen = 0, set()
    items = registered()
    for typ, iid, frames, mem in items:
        for kind, path, want in (("textura", os.path.join(TEX, FOLDER[typ], iid + ".png"), expect(typ, frames, mem)),
                                 ("icono", os.path.join(TEX, "icons", "%s_%s.png" % (KEY[typ], iid)), (32, 32))):
            seen.add(os.path.normpath(path))
            if not os.path.isfile(path):
                print("FALTA   %-6s %-12s %s" % (typ, iid, kind)); bad += 1; continue
            size = Image.open(path).size
            if size != want:
                print("MEDIDA  %-6s %-12s %s %dx%d (esperado %dx%d)" % (typ, iid, kind, size[0], size[1], want[0], want[1])); bad += 1
    for root, _, files in os.walk(TEX):
        for f in files:
            p = os.path.normpath(os.path.join(root, f))
            if f.endswith(".png") and p not in seen:
                print("SOBRA   %s (no esta registrado en Cosmetics.java)" % os.path.relpath(p, TEX))
    print("%d cosmeticos registrados, %d problemas" % (len(items), bad))
    sys.exit(1 if bad else 0)

if __name__ == "__main__":
    main()
