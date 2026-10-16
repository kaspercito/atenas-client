"""Genera las texturas de los cosmeticos NUEVOS (capas, alas y sombreros). No toca el logo ni los cosmeticos originales.
Uso:  python3 tools/gen_cosmetics.py   (necesita Pillow)"""
import os, math, random
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "atenasclient", "textures", "cosmetics") + os.sep

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def grad(w, h, stops):
    im = Image.new("RGBA", (w, h)); px = im.load()
    n = len(stops) - 1
    for y in range(h):
        t = y / max(1, h - 1) * n
        i = min(n - 1, int(t)); c = lerp(stops[i], stops[i + 1], t - i)
        for x in range(w):
            px[x, y] = c + (255,)
    return im

# ---------------- CAPAS (64x32: cara externa en (1,1) 10x16, interna en (12,1)) ----------------
def cape(name, outer):
    tex = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    inner = Image.new("RGBA", (10, 16)); ip = inner.load(); op = outer.load()
    for y in range(16):
        for x in range(10):
            r, g, b, a = op[x, y]
            ip[x, y] = (int(r * .55), int(g * .55), int(b * .55), 255)
    tex.paste(outer, (1, 1)); tex.paste(inner, (12, 1))
    top = op[5, 0]; bot = op[5, 15]; side = op[0, 8]
    for x in range(10):
        tex.putpixel((1 + x, 0), top); tex.putpixel((11 + x, 0), bot)
    for y in range(16):
        tex.putpixel((0, 1 + y), side); tex.putpixel((11, 1 + y), side)
    tex.save(ROOT + "capes/%s.png" % name)

A_GLYPH = ["01110", "10001", "10001", "11111", "10001", "10001", "10001"]

def d_violeta():
    im = grad(10, 16, [(139, 92, 246), (192, 38, 211), (236, 72, 153)])
    for y, row in enumerate(A_GLYPH):
        for x, ch in enumerate(row):
            if ch == "1": im.putpixel((2 + x, 4 + y), (255, 255, 255, 255))
    ImageDraw.Draw(im).rectangle([0, 0, 9, 15], outline=(200, 170, 255, 255))
    return im

def d_oceano():
    im = grad(10, 16, [(8, 50, 130), (30, 120, 210), (90, 200, 240)])
    for y in range(2, 16, 4):
        for x in range(10):
            yy = y + (1 if (x // 2) % 2 else 0)
            if yy < 16: im.putpixel((x, yy), (200, 240, 255, 255))
    return im

def d_atardecer():
    im = grad(10, 16, [(255, 210, 90), (240, 100, 90), (150, 50, 130), (50, 30, 100)])
    d = ImageDraw.Draw(im); d.ellipse([2, 2, 7, 7], fill=(255, 240, 160, 255))
    return im

def d_matrix():
    rnd = random.Random(11)
    im = Image.new("RGBA", (10, 16), (2, 12, 6, 255))
    for x in range(10):
        y = rnd.randint(0, 6)
        while y < 16:
            im.putpixel((x, y), (30, rnd.randint(120, 230), 70, 255))
            y += rnd.choice([1, 1, 2, 3])
        im.putpixel((x, rnd.randint(0, 15)), (200, 255, 210, 255))
    return im

def d_neon():
    im = Image.new("RGBA", (10, 16), (8, 8, 28, 255)); d = ImageDraw.Draw(im)
    for x in range(1, 10, 3): d.line([x, 0, x, 15], fill=(34, 211, 238, 255))
    for x in range(2, 10, 3): d.line([x, 0, x, 15], fill=(236, 72, 153, 255))
    d.rectangle([0, 0, 9, 15], outline=(255, 255, 255, 255))
    return im

def d_oro():
    im = grad(10, 16, [(255, 226, 110), (230, 180, 40), (170, 120, 15)]); d = ImageDraw.Draw(im)
    dark = (120, 80, 8, 255)
    for y in (2, 13):
        for x in range(0, 10, 2): d.point((x, y), fill=dark); d.point((x, y - 1 if y == 13 else y + 1), fill=dark) if x % 4 == 0 else None
    d.rectangle([0, 0, 9, 15], outline=(255, 245, 170, 255))
    for y, row in enumerate(A_GLYPH):
        for x, ch in enumerate(row):
            if ch == "1": im.putpixel((2 + x, 4 + y), dark)
    return im

def d_noche():
    rnd = random.Random(21)
    im = grad(10, 16, [(8, 10, 36), (28, 30, 86), (46, 44, 110)])
    for _ in range(14): im.putpixel((rnd.randint(0, 9), rnd.randint(0, 15)), (255, 255, 235, 255))
    d = ImageDraw.Draw(im)
    d.ellipse([2, 3, 7, 8], fill=(250, 240, 190, 255)); d.ellipse([4, 2, 9, 7], fill=(24, 26, 70, 255))
    return im

def d_creeper():
    rnd = random.Random(5)
    im = Image.new("RGBA", (10, 16))
    for y in range(16):
        for x in range(10):
            g = rnd.randint(110, 175); im.putpixel((x, y), (40, g, 50, 255))
    k = (8, 20, 10, 255)
    for (x, y) in [(2, 3), (3, 3), (2, 4), (3, 4), (6, 3), (7, 3), (6, 4), (7, 4),
                   (4, 5), (5, 5), (3, 6), (4, 6), (5, 6), (6, 6), (3, 7), (6, 7), (3, 8), (6, 8)]:
        im.putpixel((x, y), k)
    return im

for n, f in [("violeta", d_violeta), ("oceano", d_oceano), ("atardecer", d_atardecer), ("matrix", d_matrix),
             ("neon", d_neon), ("oro", d_oro), ("noche", d_noche), ("creeper", d_creeper)]:
    cape(n, f())

# ---------------- ALAS (64x32: exterior en (22,1) 20x22, interior en (1,1)) ----------------
def wing_outer(main, tip, outline):
    S = 8; W, H = 20 * S, 22 * S
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    ax, ay = 19.5 * S, 11 * S
    for ang, ln in [(-62, 15), (-42, 18), (-22, 19.5), (-3, 19), (17, 17), (35, 13.5), (52, 10)]:
        a = math.radians(ang); dx, dy = -math.cos(a), math.sin(a); px_, py_ = -dy, dx
        tipx, tipy = ax + dx * ln * S, ay + dy * ln * S
        w0, w1 = 1.1 * S, 2.1 * S
        pts = [(ax + px_ * w0, ay + py_ * w0), (ax + dx * ln * .55 * S + px_ * w1, ay + dy * ln * .55 * S + py_ * w1), (tipx, tipy),
               (ax + dx * ln * .55 * S - px_ * w1, ay + dy * ln * .55 * S - py_ * w1), (ax - px_ * w0, ay - py_ * w0)]
        d.polygon(pts, fill=outline + (255,))
        d.polygon([(ax + (p[0] - ax) * .8, ay + (p[1] - ay) * .8) for p in pts], fill=main + (255,))
        d.ellipse([tipx - 1.2 * S, tipy - 1.2 * S, tipx + 1.2 * S, tipy + 1.2 * S], fill=tip + (255,))
    return im.resize((20, 22), Image.BOX)

def wings(name, main, tip, outline):
    outer = wing_outer(main, tip, outline)
    tex = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    tex.paste(outer, (22, 1)); tex.paste(outer.transpose(Image.FLIP_LEFT_RIGHT), (1, 1))
    tex.save(ROOT + "wings/%s.png" % name)

wings("atenas", (139, 92, 246), (236, 72, 153), (40, 20, 90))
wings("fuego", (255, 110, 20), (255, 230, 80), (120, 30, 10))
wings("hielo", (170, 225, 255), (255, 255, 255), (60, 120, 190))
wings("neon", (20, 230, 230), (255, 60, 200), (10, 40, 70))
wings("esmeralda", (46, 204, 113), (210, 255, 225), (10, 90, 50))
wings("demonio", (170, 20, 30), (255, 120, 40), (30, 5, 8))

# ---------------- SOMBREROS (64x64: color base en todo el lienzo, acento en la franja v >= 48) ----------------
def hat(name, base, accent):
    im = Image.new("RGBA", (64, 64), base + (255,))
    ImageDraw.Draw(im).rectangle([0, 48, 63, 63], fill=accent + (255,))
    im.save(ROOT + "hats/%s.png" % name)

hat("gato", (36, 36, 46), (255, 150, 180))
hat("cuernos", (190, 25, 35), (35, 10, 15))
hat("mago", (88, 48, 170), (245, 197, 66))
hat("auriculares", (40, 40, 58), (34, 211, 238))
hat("conejo", (242, 242, 247), (255, 170, 200))
hat("gorra", (36, 71, 184), (240, 240, 250))
print("cosmeticos nuevos listos")
