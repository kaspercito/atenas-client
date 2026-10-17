"""Plantillas de textura para dibujar cosmeticos propios (se guardan en tools/plantillas/).
Uso:  python3 tools/gen_plantillas.py      (necesita Pillow)
Cada plantilla es un PNG de la medida EXACTA que pide el juego, con guias: pinta encima y borra las guias (capa aparte).
  capa.png (512x256) | capa_animada.png (512x3072, 12 fotogramas) | alas_plumas.png (256x1040) | alas_membrana.png (128x128)
  sombrero.png (64x64) | mascota_<modelo>.png (384x192, uno por modelo: dog, husky, cat, fox)
Despues comprueba las medidas con:  python3 tools/check_textures.py
"""
import os, sys, math
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "plantillas")
os.makedirs(OUT, exist_ok=True)
G = (255, 0, 200, 255); T = (255, 255, 255, 255)

def grid(d, w, h, step, col=(120, 120, 140, 90)):
    for x in range(0, w + 1, step): d.line([(x, 0), (x, h)], fill=col)
    for y in range(0, h + 1, step): d.line([(0, y), (w, y)], fill=col)

def capa(frames, name):
    S = 8; w, h = 512, 256 * frames
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    for f in range(frames):
        y0 = f * 256
        d.rectangle([0, y0, w - 1, y0 + 255], outline=(120, 120, 140, 255))
        d.rectangle([1 * S, y0 + 1 * S, 11 * S, y0 + 17 * S], outline=G, width=2)
        d.text((1 * S + 6, y0 + 1 * S + 6), "CARA EXTERNA (la que se ve)\n10x16 unidades\nempieza en (1,1)", fill=G)
        d.text((8, y0 + 236), "fotograma %d" % (f + 1) if frames > 1 else "capa", fill=T)
    if frames == 1: grid(d, w, h, S)
    im.save(os.path.join(OUT, name))

def plumas():
    im = Image.new("RGBA", (256, 1040), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    names = ["hueso"] + ["primaria %d" % i for i in range(1, 12)] + ["secundaria %d" % i for i in range(1, 9)] + ["cobertera %d" % i for i in range(1, 7)]
    for k, n in enumerate(names):
        d.rectangle([0, k * 40, 255, k * 40 + 39], outline=(120, 120, 140, 255))
        d.text((6, k * 40 + 4), "%s   raiz (u=0) a la IZQUIERDA, punta a la DERECHA, borde de ataque ARRIBA" % n, fill=G)
    im.save(os.path.join(OUT, "alas_plumas.png"))

def membrana():
    import gen_membrane as gm
    W = gm.SIZE; im = Image.new("RGBA", (W, W), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    big = [(x * 1.0, y * 1.0) for x, y in [gm.to_px(px, py) for px, py in gm.outline()]]
    d.polygon([(x / gm.SS, y / gm.SS) for x, y in big], outline=G)
    for tx, ty in gm.tips():
        a, b = gm.to_px(gm.WX, gm.WY), gm.to_px(tx, ty)
        d.line([(a[0] / gm.SS, a[1] / gm.SS), (b[0] / gm.SS, b[1] / gm.SS)], fill=(0, 255, 255, 255))
    d.rectangle([0, int(gm.BONE_V0 * W), W - 1, int(gm.BONE_V1 * W)], outline=(255, 255, 0, 255))
    d.text((4, int(gm.BONE_V0 * W) + 8), "FRANJA DE HUESOS (color liso)", fill=(255, 255, 0, 255))
    d.text((4, 2), "MEMBRANA: pinta dentro del contorno (magenta); cian = huesos de los dedos", fill=G)
    im.save(os.path.join(OUT, "alas_membrana.png"))

def sombrero():
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    grid(d, 64, 64, 8); d.text((2, 2), "sombrero 64x64\n(mira HatModel.java\npara cada pieza)", fill=G)
    im.save(os.path.join(OUT, "sombrero.png"))

def mascotas():
    import gen_pets as gp
    for model, sizes in gp.SIZES.items():
        im = Image.new("RGBA", (gp.TW * gp.S, gp.TH * gp.S), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
        for part, (u, v) in gp.LAYOUT[model].items():
            w, h, dd = sizes[part]
            for face, (x, y, fw, fh) in gp.faces(u, v, w, h, dd).items():
                d.rectangle([x * gp.S, y * gp.S, (x + fw) * gp.S - 1, (y + fh) * gp.S - 1], outline=G)
                if fw * gp.S > 28: d.text((x * gp.S + 2, y * gp.S + 2), "%s\n%s" % (part, face[:3]), fill=T)
        im.save(os.path.join(OUT, "mascota_%s.png" % model))

if __name__ == "__main__":
    capa(1, "capa.png"); capa(12, "capa_animada.png"); plumas(); membrana(); sombrero(); mascotas()
    print("plantillas en", OUT, "->", ", ".join(sorted(os.listdir(OUT))))
