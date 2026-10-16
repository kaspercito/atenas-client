import random, math
from PIL import Image, ImageDraw, ImageFont

ROOT = "/home/claude/AtenasClient/src/main/resources/assets/atenasclient/textures/"

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def grad(img_w, img_h, c1, c2):
    im = Image.new("RGBA", (img_w, img_h))
    px = im.load()
    for y in range(img_h):
        c = lerp(c1, c2, y / max(1, img_h - 1))
        for x in range(img_w):
            px[x, y] = c + (255,)
    return im

# ---------- CAPES (64x32, cara externa en (1,1) 10x16, interna en (12,1)) ----------
def cape(name, design):
    tex = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    outer = design()
    inner = Image.new("RGBA", (10, 16))
    ip = inner.load(); op = outer.load()
    for y in range(16):
        for x in range(10):
            r, g, b, a = op[x, y]
            ip[x, y] = (int(r * .55), int(g * .55), int(b * .55), 255)
    tex.paste(outer, (1, 1))
    tex.paste(inner, (12, 1))
    # bordes (arriba, abajo, lados) con colores del diseño
    top = op[5, 0]; bot = op[5, 15]; side = op[0, 8]
    for x in range(10):
        tex.putpixel((1 + x, 0), top)
        tex.putpixel((11 + x, 0), bot)
    for y in range(16):
        tex.putpixel((0, 1 + y), side)
        tex.putpixel((11, 1 + y), side)
    tex.save(ROOT + "cosmetics/capes/%s.png" % name)

A_GLYPH = ["01110", "10001", "10001", "11111", "10001", "10001", "10001"]

def d_atenas():
    im = grad(10, 16, (14, 22, 52), (28, 52, 120))
    d = ImageDraw.Draw(im)
    gold = (245, 197, 66, 255)
    d.rectangle([0, 0, 9, 15], outline=gold)
    for y, row in enumerate(A_GLYPH):
        for x, ch in enumerate(row):
            if ch == "1":
                im.putpixel((2 + x, 4 + y), gold)
    for x in range(2, 8):  # greca inferior
        if x % 2 == 0:
            im.putpixel((x, 13), gold)
    return im

def d_fuego():
    rnd = random.Random(3)
    im = grad(10, 16, (90, 8, 8), (255, 170, 20))
    for x in range(10):
        h = rnd.randint(3, 8)
        for y in range(16 - h, 16):
            t = (y - (16 - h)) / h
            im.putpixel((x, y), lerp((255, 120, 10), (255, 235, 90), t) + (255,))
    return im

def d_hielo():
    rnd = random.Random(5)
    im = grad(10, 16, (190, 235, 255), (60, 130, 220))
    for _ in range(14):
        im.putpixel((rnd.randint(0, 9), rnd.randint(0, 15)), (255, 255, 255, 255))
    d = ImageDraw.Draw(im); d.rectangle([0, 0, 9, 15], outline=(235, 250, 255, 255))
    return im

def d_galaxia():
    rnd = random.Random(7)
    im = grad(10, 16, (16, 8, 40), (70, 20, 110))
    for _ in range(18):
        c = rnd.choice([(255, 255, 255), (255, 170, 255), (160, 200, 255)])
        im.putpixel((rnd.randint(0, 9), rnd.randint(0, 15)), c + (255,))
    return im

def d_arcoiris():
    im = Image.new("RGBA", (10, 16))
    cols = [(255, 0, 0), (255, 127, 0), (255, 220, 0), (0, 200, 0), (0, 120, 255), (140, 0, 255)]
    for y in range(16):
        c = cols[min(5, y * 6 // 16)]
        for x in range(10):
            im.putpixel((x, y), c + (255,))
    return im

def d_esmeralda():
    im = grad(10, 16, (10, 90, 50), (46, 204, 113))
    d = ImageDraw.Draw(im)
    for y in range(0, 16, 4):
        for x in range(0, 10, 4):
            d.point((x + (2 if (y // 4) % 2 else 0), y + 1), fill=(200, 255, 220, 255))
    d.rectangle([0, 0, 9, 15], outline=(20, 130, 70, 255))
    return im

for n, f in [("atenas", d_atenas), ("fuego", d_fuego), ("hielo", d_hielo),
             ("galaxia", d_galaxia), ("arcoiris", d_arcoiris), ("esmeralda", d_esmeralda)]:
    cape(n, f)

# ---------- ALAS (64x32; cara exterior en (22,1) 20x22, interior en (1,1)) ----------
def wing_outer(main, tip, outline):
    S = 8
    W, H = 20 * S, 22 * S
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    ax, ay = 19.5 * S, 11 * S  # punto de union: borde derecho
    feathers = [(-62, 15), (-42, 18), (-22, 19.5), (-3, 19), (17, 17), (35, 13.5), (52, 10)]
    for ang, ln in feathers:
        a = math.radians(ang)
        dx, dy = -math.cos(a), math.sin(a)
        px_, py_ = -dy, dx
        tipx, tipy = ax + dx * ln * S, ay + dy * ln * S
        w0, w1 = 1.1 * S, 2.1 * S
        pts = [(ax + px_ * w0, ay + py_ * w0), (ax + dx * ln * .55 * S + px_ * w1, ay + dy * ln * .55 * S + py_ * w1),
               (tipx, tipy), (ax + dx * ln * .55 * S - px_ * w1, ay + dy * ln * .55 * S - py_ * w1),
               (ax - px_ * w0, ay - py_ * w0)]
        d.polygon(pts, fill=outline + (255,))
        inner = [(x * .72 + (ax * .28 if i in (0, 4) else 0) + (tipx * .28 if i == 2 else 0),
                  y * .72 + (ay * .28 if i in (0, 4) else 0) + (tipy * .28 if i == 2 else 0)) for i, (x, y) in enumerate(pts)]
        d.polygon(pts, fill=outline + (255,))
        d.polygon([(ax + (p[0] - ax) * .8, ay + (p[1] - ay) * .8) for p in pts], fill=main + (255,))
        d.ellipse([tipx - 1.2 * S, tipy - 1.2 * S, tipx + 1.2 * S, tipy + 1.2 * S], fill=tip + (255,))
    return im.resize((20, 22), Image.BOX)

def wings(name, main, tip, outline):
    outer = wing_outer(main, tip, outline)
    inner = outer.transpose(Image.FLIP_LEFT_RIGHT)
    tex = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    tex.paste(outer, (22, 1))
    tex.paste(inner, (1, 1))
    tex.save(ROOT + "cosmetics/wings/%s.png" % name)

wings("doradas", (245, 197, 66), (255, 245, 190), (150, 105, 20))
wings("oscuras", (50, 25, 80), (200, 60, 255), (15, 8, 28))
wings("angel", (245, 250, 255), (255, 255, 255), (150, 180, 215))

# ---------- SOMBREROS (64x64) ----------
def hat(name, base, extra=None):
    im = Image.new("RGBA", (64, 64), base + (255,))
    if extra:
        d = ImageDraw.Draw(im)
        d.rectangle(extra, fill=(190, 25, 35, 255))
    im.save(ROOT + "cosmetics/hats/%s.png" % name)

hat("halo", (255, 240, 150))
hat("corona", (245, 197, 66))
hat("galera", (24, 24, 34), extra=[0, 32, 31, 44])

# ---------- LOGO 256x256 ----------
S = 4
im = Image.new("RGBA", (256 * S, 256 * S), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
c = 128 * S
d.ellipse([8 * S, 8 * S, 248 * S, 248 * S], fill=(245, 197, 66, 255))
d.ellipse([20 * S, 20 * S, 236 * S, 236 * S], fill=(14, 22, 52, 255))
d.ellipse([32 * S, 32 * S, 224 * S, 224 * S], outline=(245, 197, 66, 255), width=3 * S)
font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf", 150 * S)
bb = d.textbbox((0, 0), "A", font=font)
d.text((c - (bb[0] + bb[2]) / 2, c - (bb[1] + bb[3]) / 2 - 4 * S), "A", font=font, fill=(245, 197, 66, 255))
im.resize((256, 256), Image.LANCZOS).save(ROOT + "gui/logo.png")
print("texturas listas")
