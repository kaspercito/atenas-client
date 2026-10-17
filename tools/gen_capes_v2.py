"""Capas nuevas (pixel art bonito) + capa exclusiva Atenas con logo. Uso: python3 tools/gen_capes_v2.py (Pillow + numpy).
Textura 512x256 = 64x32 a escala 8: cara externa en (8,8) 80x128, interna en (96,8)."""
import os, random
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "assets", "atenasclient")
CAPES = os.path.join(RES, "textures", "cosmetics", "capes") + os.sep
ICONS = os.path.join(RES, "textures", "cosmetics", "icons") + os.sep
W, H = 40, 64

def hexc(h): return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255], float)

def vgrad(stops):
    """stops: [(pos 0..1, 0xRRGGBB)] -> array HxWx3"""
    img = np.zeros((H, W, 3))
    for y in range(H):
        t = y / (H - 1)
        for i in range(len(stops) - 1):
            p0, c0 = stops[i]; p1, c1 = stops[i + 1]
            if p0 <= t <= p1:
                f = (t - p0) / max(1e-6, p1 - p0); img[y, :] = hexc(c0) * (1 - f) + hexc(c1) * f; break
    return img

def noise(seed, sx, sy):
    r = np.random.RandomState(seed)
    g = r.rand(H // sy + 2, W // sx + 2)
    out = np.zeros((H, W))
    for y in range(H):
        for x in range(W):
            gy, gx = y / sy, x / sx; y0, x0 = int(gy), int(gx); fy, fx = gy - y0, gx - x0
            fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
            out[y, x] = (g[y0, x0] * (1 - fx) + g[y0, x0 + 1] * fx) * (1 - fy) + (g[y0 + 1, x0] * (1 - fx) + g[y0 + 1, x0 + 1] * fx) * fy
    return out

def clouds(img, seed, y0, y1, light, dark, cover=0.52, sx=9, sy=4):
    n = noise(seed, sx, sy) * 0.65 + noise(seed + 7, 4, 2) * 0.35
    for y in range(max(0, y0), min(H, y1)):
        fade = min(1.0, (y - y0) / 6.0, (y1 - y) / 6.0)
        for x in range(W):
            v = n[y, x] - cover
            if v > 0:
                a = min(1.0, v * 5) * fade
                shade = min(1.0, v * 3.2)  # centro claro, borde oscuro
                c = hexc(dark) * (1 - shade) + hexc(light) * shade
                img[y, x] = img[y, x] * (1 - a) + c * a

def stars(img, seed, n, y1, col=0xFFFFFF):
    r = random.Random(seed)
    for _ in range(n):
        x, y = r.randrange(W), r.randrange(y1)
        img[y, x] = hexc(col) * r.uniform(0.6, 1.0) + img[y, x] * 0.2
        if r.random() < 0.15:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < W and 0 <= y + dy < H: img[y + dy, x + dx] = img[y + dy, x + dx] * 0.6 + hexc(col) * 0.4

def moon(img, cx, cy, r, col=0xF4F1E8, cut=0.62):
    for y in range(H):
        for x in range(W):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** .5
            d2 = ((x - cx - r * cut) ** 2 + (y - cy + r * 0.15) ** 2) ** .5
            if d <= r and d2 > r * 0.92: img[y, x] = hexc(col)
            elif r < d <= r + 4:
                a = (1 - (d - r) / 4) * 0.22; img[y, x] = img[y, x] * (1 - a) + hexc(col) * a

def beam(img, cx, y0, y1, col=0xBFD8FF):
    for y in range(y0, y1):
        a = 0.28 * (1 - (y - y0) / max(1, y1 - y0))
        for x in (cx - 1, cx, cx + 1):
            w = a if x == cx else a * .45
            img[y, x] = img[y, x] * (1 - w) + hexc(col) * w

def ridge(img, seed, base, amp, col, sx=7):
    n = noise(seed, sx, 64)[0]
    for x in range(W):
        top = int(base - n[x] * amp)
        for y in range(max(0, top), H): img[y, x] = hexc(col)

def finish(img, name, vignette=True):
    if vignette:
        for x in range(W):
            e = min(x, W - 1 - x)
            if e < 2: img[:, x] *= (0.82 + 0.09 * e)
    low = Image.fromarray(np.clip(img, 0, 255).astype("uint8"), "RGB")
    return low

def save(low, name, glow=0xFFFFFF):
    outer = low.resize((80, 128), Image.NEAREST).convert("RGBA")
    inner = outer.point(lambda v: int(v * .55)); inner.putalpha(255)
    tex = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    tex.paste(outer, (8, 8)); tex.paste(inner, (96, 8))
    top = outer.crop((0, 0, 80, 1)).resize((80, 8)); tex.paste(top, (8, 0))
    bot = outer.crop((0, 127, 80, 128)).resize((80, 8)); tex.paste(bot, (88, 0))
    tex.paste(outer.crop((0, 0, 1, 128)).resize((8, 128)), (0, 8))
    tex.paste(outer.crop((79, 0, 80, 128)).resize((8, 128)), (88, 8))
    tex.save(CAPES + name + ".png")
    # icono 32x32 de la capa (cara externa reducida con marco)
    ic = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    ic.paste(low.resize((20, 32), Image.BOX).convert("RGBA"), (6, 0))
    ImageDraw.Draw(ic).rectangle((6, 0, 25, 31), outline=(0, 0, 0, 120))
    ic.save(ICONS + "cape_" + name + ".png")

# ---------- diseños ----------
def atardecer_luna():
    img = vgrad([(0, 0x1B3A6B), (.30, 0x3E5C9A), (.55, 0xE08A6E), (.78, 0x8C4A7A), (1, 0x2B2A5C)])
    stars(img, 3, 14, 18)
    moon(img, 20, 13, 5); beam(img, 20, 20, 40)
    clouds(img, 11, 28, 56, 0xFFC98A, 0x7A3E6E, .50)
    clouds(img, 21, 40, 64, 0xFF9E7A, 0x4A2E6E, .55)
    return img

def aurora():
    img = vgrad([(0, 0x050A1E), (.5, 0x0B2742), (1, 0x061A2C)])
    stars(img, 5, 26, 50)
    n = noise(2, 14, 6)
    for y in range(8, 46):
        for x in range(W):
            band = np.exp(-((y - (22 + 6 * np.sin(x / 5.0 + n[y, x] * 3))) ** 2) / 40.0)
            col = hexc(0x2BFFA8) * (1 - y / 46) + hexc(0xA855F7) * (y / 46)
            img[y, x] = img[y, x] * (1 - band * .85) + col * band * .85
    ridge(img, 9, 54, 10, 0x04101C); ridge(img, 4, 58, 7, 0x020A12)
    return img

def sakura():
    img = vgrad([(0, 0xFFD6E8), (.5, 0xF8A9C8), (1, 0x7A3A6E)])
    clouds(img, 6, 6, 30, 0xFFF0F6, 0xF3B6D0, .55)
    r = random.Random(8)
    for _ in range(46):
        x, y = r.randrange(W), r.randrange(H)
        c = r.choice([0xFFFFFF, 0xFFE0EE, 0xFF9CC4]); img[y, x] = hexc(c)
        if r.random() < .5: img[min(H - 1, y + 1), min(W - 1, x + 1)] = hexc(c) * .85
    for bx in (5, 33):  # ramas
        for y in range(40, 64): img[y, bx + (y % 3 == 0)] = hexc(0x4A2A3A)
    return img

def oceano():
    img = vgrad([(0, 0xFFB86B), (.32, 0xFF7E6B), (.40, 0x2A5AA8), (1, 0x06183A)])
    for y in range(26, 64):
        for x in range(W):
            w = np.sin(x * .6 + y * .9) + np.sin(x * .25 - y * .5)
            if w > 1.1: img[y, x] = img[y, x] * .7 + hexc(0x9CD3FF) * .3
    for y in range(26, 44):
        wd = int((44 - y) / 3) + 1
        for x in range(20 - wd, 20 + wd + 1):
            if (x + y) % 2 == 0: img[y, x] = hexc(0xFFE2A8)
    r = random.Random(1)
    for dy in range(8):
        for x in range(14, 27): img[18 + dy // 3 if False else 20, x] = img[20, x]
    sun_y = 24
    for y in range(sun_y - 6, sun_y + 1):
        for x in range(W):
            if ((x - 20) ** 2 + (y - sun_y) ** 2) ** .5 <= 6: img[y, x] = hexc(0xFFF1B8)
    return img

def bosque():
    img = vgrad([(0, 0xA8D8C8), (.4, 0xE8F0D0), (1, 0x2E5B3E)])
    ridge(img, 12, 34, 12, 0x7FB59A); ridge(img, 15, 42, 12, 0x4F8F72); ridge(img, 18, 50, 10, 0x2F6A52)
    r = random.Random(4)
    for _ in range(18):
        x = r.randrange(W); h = r.randrange(7, 14); y0 = 64 - r.randrange(0, 4)
        for i in range(h):
            wdt = i // 3 + 1
            for dx in range(-wdt, wdt + 1):
                if 0 <= x + dx < W and y0 - h + i >= 0: img[y0 - h + i, x + dx] = hexc(0x153B2C if r.random() < .8 else 0x1F5A3F)
    for _ in range(16):
        x, y = r.randrange(W), r.randrange(30, 60); img[y, x] = hexc(0xFFF6A8)
    return img

def galaxia():
    img = vgrad([(0, 0x07041C), (1, 0x120A34)])
    n1, n2 = noise(31, 10, 8), noise(32, 7, 6)
    for y in range(H):
        for x in range(W):
            d = abs((x - 6) * 0.9 - (y - 8) * 0.5 - 6 * n1[y, x]) 
            glow = np.exp(-d * d / 90.0) * (0.35 + n2[y, x] * 0.8)
            col = hexc(0x7C3AED) * (1 - y / H) + hexc(0xEC4899) * (y / H)
            img[y, x] = img[y, x] * (1 - glow * .8) + col * glow * .8
    stars(img, 77, 60, H)
    return img

def lava():
    img = vgrad([(0, 0x1A0A12), (.5, 0x4A1220), (1, 0xFF6A1A)])
    ridge(img, 21, 36, 14, 0x120508); ridge(img, 22, 46, 10, 0x1E0A0C)
    n = noise(5, 6, 4)
    for y in range(38, 64):
        for x in range(W):
            if n[y, x] > .72: img[y, x] = hexc(0xFFB02E)
    r = random.Random(2)
    for _ in range(30):
        x, y = r.randrange(W), r.randrange(0, 40); img[y, x] = hexc(0xFFA040)
    return img

def hielo():
    img = vgrad([(0, 0xDFF6FF), (.5, 0x8ACBF0), (1, 0x2B5CA8)])
    r = random.Random(9)
    for _ in range(9):
        cx, cy = r.randrange(4, 36), r.randrange(4, 60); s = r.randrange(2, 4)
        for i in range(-s, s + 1):
            for (x, y) in ((cx + i, cy), (cx, cy + i)):
                if 0 <= x < W and 0 <= y < H: img[y, x] = hexc(0xFFFFFF)
    stars(img, 12, 20, H, 0xFFFFFF)
    return img

def atenas_exclusiva():
    """Exclusiva del cliente: noche violeta/azul, estrellas, luna dorada, logo grande y borde dorado."""
    img = vgrad([(0, 0x120B3A), (.45, 0x2B1B6B), (.8, 0x5B2A8C), (1, 0x1A0F40)])
    stars(img, 41, 30, 40, 0xE8E0FF)
    clouds(img, 13, 44, 64, 0xB58CFF, 0x3A2380, .55)
    low = Image.fromarray(np.clip(img, 0, 255).astype("uint8"), "RGB").convert("RGBA")
    logo = Image.open(os.path.join(RES, "textures", "gui", "logo.png")).convert("RGBA")
    bb = logo.getbbox(); logo = logo.crop(bb)
    s = 30 / max(logo.size); logo = logo.resize((max(1, int(logo.width * s)), max(1, int(logo.height * s))), Image.LANCZOS)
    low.alpha_composite(logo, ((W - logo.width) // 2, 10))
    d = ImageDraw.Draw(low)
    gold = (245, 197, 66, 255)
    d.rectangle((0, 0, W - 1, H - 1), outline=gold)
    d.rectangle((2, 2, W - 3, H - 3), outline=(245, 197, 66, 110))
    for x in range(8, W - 8, 2): d.point((x, 52), fill=gold)
    # "ATENAS" en letras de 3x5
    G = {"A": "010101111101101", "T": "111010010010010", "E": "111100110100111", "N": "110101101101101", "S": "111100111001111"}
    x0 = (W - (6 * 4 - 1)) // 2
    for i, ch in enumerate("ATENAS"):
        for k, bit in enumerate(G[ch]):
            if bit == "1": d.point((x0 + i * 4 + k % 3, 54 + k // 3), fill=gold)
    return np.array(low.convert("RGB"), float)

DESIGNS = {"atenas": atenas_exclusiva, "atardecer": atardecer_luna, "aurora": aurora, "sakura": sakura,
           "oceano": oceano, "bosque": bosque, "galaxia": galaxia, "lava": lava, "hielo": hielo}

if __name__ == "__main__":
    for name, fn in DESIGNS.items():
        save(finish(fn(), name, vignette=name != "atenas"), name)
        print("ok", name)
