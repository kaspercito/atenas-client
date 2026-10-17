"""Capas ANIMADAS (12 fotogramas, tira vertical) para el cliente 1.8.9 y el mod 1.20.1.
Uso: python3 tools/gen_capes_anim.py   (Pillow + numpy). Escribe en los dos proyectos si existe ../atenas-social (hermano).
Cada fotograma es la hoja 64x32 a escala 8 (512x256): cara externa en (8,8) 80x128, interna en (96,8). La tira mide 512 x 3072.
Todo movimiento es periodico (el fotograma 12 enlaza con el 0): no se nota el salto del bucle."""
import os, math
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
TARGETS = [(os.path.join(ROOT, "src/main/resources/assets/atenasclient"))]
SOC = os.path.abspath(os.path.join(os.environ.get("ATENAS_SOCIAL", os.path.join(ROOT, "..", "atenas-social")), "src/main/resources/assets/atenassocial"))
if os.path.isdir(SOC): TARGETS.append(SOC)
LOGO = os.path.join(ROOT, "src/main/resources/assets/atenasclient/textures/gui/logo.png")

W, H, F = 40, 64, 12
Y, X = np.mgrid[0:H, 0:W].astype(float)
TAU = 2 * math.pi

def hexc(h): return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255], float)

def vgrad(stops):
    out = np.zeros((H, W, 3))
    t = Y / (H - 1)
    for (p0, c0), (p1, c1) in zip(stops[:-1], stops[1:]):
        m = (t >= p0) & (t <= p1)
        f = np.clip((t - p0) / max(1e-6, p1 - p0), 0, 1)[..., None]
        out = np.where(m[..., None], hexc(c0) * (1 - f) + hexc(c1) * f, out)
    return out

def smooth_noise(seed, gw, gh):
    r = np.random.RandomState(seed)
    g = (r.rand(gh, gw) * 255).astype("uint8")
    return np.asarray(Image.fromarray(g).resize((W, H), Image.BICUBIC), float) / 255.0

def over(img, color, a):
    a = np.clip(a, 0, 1)[..., None]
    return img * (1 - a) + np.asarray(color, float) * a

def add(img, color, a):
    return img + np.asarray(color, float) * np.clip(a, 0, None)[..., None]

def pal(stops, v):
    """stops [(pos,hex)] ; v array 0..1 -> RGB array."""
    v = np.clip(v, 0, 1); out = np.zeros(v.shape + (3,))
    for (p0, c0), (p1, c1) in zip(stops[:-1], stops[1:]):
        m = (v >= p0) & (v <= p1)
        f = np.clip((v - p0) / max(1e-6, p1 - p0), 0, 1)[..., None]
        out = np.where(m[..., None], hexc(c0) * (1 - f) + hexc(c1) * f, out)
    return out

def stars(img, seed, n, ymax, t, col=0xFFFFFF, big=0.2):
    r = np.random.RandomState(seed)
    for _ in range(n):
        x, y = r.randint(0, W), r.randint(0, ymax)
        k, ph = r.randint(1, 3), r.rand()
        b = 0.5 + 0.5 * math.sin(TAU * (t * k + ph))
        img[y, x] = img[y, x] * (1 - 0.8 * b) + hexc(col) * (0.8 * b)
        if r.rand() < big and b > 0.7:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < W and 0 <= yy < H: img[yy, xx] = img[yy, xx] * 0.6 + hexc(col) * 0.4 * b
    return img

# ------------------------------------------------------------------ ATENAS (colores del logo)
_logo = Image.open(LOGO).convert("RGBA")
_navy = (13, 11, 33)
_bg = Image.new("RGBA", _logo.size, _navy + (255,)); _bg.alpha_composite(_logo)
_emb = np.asarray(_bg.crop((22, 22, 234, 234)).convert("RGB").resize((36, 36), Image.LANCZOS), float)
_lum = _emb.mean(axis=2) / 255.0
EMB_X, EMB_Y = 2, 5
# nodos del hexagono del logo (en el logo 256: arriba, 4 laterales, abajo) -> coordenadas de la capa
_NODES = [(128, 32, 0x9FE8FF), (210, 80, 0x4FB8FF), (210, 176, 0xA78BFA), (128, 222, 0xFF5CB0), (45, 176, 0xA78BFA), (45, 80, 0x4FB8FF)]
_NODES = [(EMB_X + (x - 22) * 36 / 212.0, EMB_Y + (y - 22) * 36 / 212.0, c) for x, y, c in _NODES]
_FONT = {"A": "010101111101101", "T": "111010010010010", "E": "111100110100111", "N": "101111111101101", "S": "111100111001111"}
_RING = [(0, 0x22D3EE), (.33, 0x8B5CF6), (.66, 0xEC4899), (1, 0x22D3EE)]  # cian -> violeta -> rosa -> cian (cicla)

def atenas(t):
    img = vgrad([(0, 0x0C0B22), (.55, 0x120F34), (1, 0x1A1245)])
    # rejilla hexagonal tenue que respira
    hexg = (np.sin(X * 0.9 + Y * 0.52) * np.sin(X * 0.9 - Y * 0.52) > 0.82).astype(float)
    img = add(img, (30, 24, 70), hexg * (0.25 + 0.15 * math.sin(TAU * t)))
    stars(img, 7, 16, H, t, 0xBBD0FF)
    # emblema del logo (hexagono + A) con el brillo diagonal que lo recorre
    e = img[EMB_Y:EMB_Y + 36, EMB_X:EMB_X + 36]
    mask = np.clip((_lum - 0.075) * 5, 0, 1)[..., None]
    e[:] = e * (1 - mask) + _emb * mask
    yy, xx = np.mgrid[0:36, 0:36].astype(float)
    d = (xx + yy * 0.7) / 55.0 - (t * 1.7 - 0.35)
    band = np.exp(-(d / 0.07) ** 2) * np.clip((_lum - 0.25) * 3, 0, 1)
    e[:] = e + np.array([255, 235, 255]) * band[..., None] * 0.65
    # nodos que laten uno tras otro
    for i, (nx, ny, c) in enumerate(_NODES):
        b = 0.5 + 0.5 * math.sin(TAU * (t - i / 6.0))
        for r, a in ((3.2, 0.10), (2.0, 0.22), (1.0, 0.7)):
            dd = (X - nx) ** 2 + (Y - ny) ** 2
            img = add(img, hexc(c) * (0.35 + 0.65 * b), np.exp(-dd / (r * r)) * a * (0.5 + b))
    # texto ATENAS con destello
    x0, y0 = 8, 48
    for li, ch in enumerate("ATENAS"):
        g = _FONT[ch]
        for k, bit in enumerate(g):
            if bit == "1":
                px, py = x0 + li * 4 + (k % 3), y0 + k // 3
                sh = math.exp(-(((px / 40.0) - ((t * 1.6) % 1.6 - 0.3)) / 0.10) ** 2)
                base = hexc(0xB9A6FF) * (1 - (py - y0) / 8.0) + hexc(0xF472B6) * ((py - y0) / 8.0)
                img[py, px] = np.clip(base * (0.85 + 0.15 * math.sin(TAU * (t + li / 6.0))) + 255 * sh * 0.6, 0, 255)
    # barras del logo (una ancha y dos finas)
    for yb, wb, a in ((55, 22, 1.0), (57, 16, 0.55), (59, 10, 0.35)):
        xs = np.arange(20 - wb // 2, 20 + wb // 2)
        img[yb, xs] = hexc(0x8B5CF6) * 0.6 * a + hexc(0xEC4899) * 0.4 * a + (30 * (0.5 + 0.5 * np.sin(TAU * (t - (xs - 8) / 24.0))) * a)[:, None]
    # borde de neon: gradiente que cicla + cometa que le da la vuelta
    per = 2 * (W + H - 2)
    def ring_pos(x, y):
        if y == 0: return x
        if x == W - 1: return W - 1 + y
        if y == H - 1: return (W - 1) + (H - 1) + (W - 1 - x)
        return 2 * (W - 1) + (H - 1) + (H - 1 - y)
    for yy_ in range(H):
        for xx_ in range(W):
            if min(xx_, yy_, W - 1 - xx_, H - 1 - yy_) > 1: continue
            edge = min(xx_, yy_, W - 1 - xx_, H - 1 - yy_)
            # proyecta al perimetro exterior para tener un parametro continuo
            px = min(max(xx_, 0), W - 1); py = min(max(yy_, 0), H - 1)
            if edge == 0: s = ring_pos(px, py)
            else:
                cx = min(max(px, 1), W - 2); cy = min(max(py, 1), H - 2)
                s = (ring_pos(cx, 0) if py == 1 and 1 <= px <= W - 2 else
                     ring_pos(W - 1, cy) if px == W - 2 else
                     ring_pos(cx, H - 1) if py == H - 2 else ring_pos(0, cy))
            u = (s / per + t) % 1.0
            col = pal(_RING, np.array([u]))[0]
            dist = abs(((s / per) - t + 0.5) % 1.0 - 0.5)
            comet = math.exp(-(dist / 0.045) ** 2)
            k = (1.0 if edge == 0 else 0.45) * (0.8 + 0.5 * comet)
            img[yy_, xx_] = np.clip(col * k + 255 * comet * 0.55 * (edge == 0), 0, 255)
    return img

# ------------------------------------------------------------------ resto de capas
def aurora(t):
    img = vgrad([(0, 0x040818), (.55, 0x09263F), (1, 0x061A2C)])
    stars(img, 5, 26, 44, t)
    for bi, (base, ph, wd) in enumerate(((20, 0.0, 6.5), (31, 0.5, 5.0))):
        c = base + 5 * np.sin(TAU * (X / W * 1.0 + t + ph)) + 2.5 * np.sin(TAU * (X / W * 2.0 - 2 * t + ph))
        inten = np.exp(-((Y - c) / wd) ** 2)
        streak = 0.65 + 0.35 * np.sin(TAU * (X / W * 5.0 + t * 2 + bi * .3))
        col = pal([(0, 0x35FFB0), (.55, 0x2CC4FF), (1, 0xB45CFF)], np.clip((Y - c) / (2 * wd) + .5, 0, 1))
        img = img + col * (inten * streak * 0.85)[..., None]
    n = smooth_noise(4, 9, 3)
    top = 52 - n[0] * 10
    img = np.where((Y >= top[None, :])[..., None], hexc(0x060B14) * (0.8 + 0.3 * (Y - 44) / 20)[..., None], img)
    return img

def galaxia(t):
    img = vgrad([(0, 0x080420), (.5, 0x1C0B4A), (1, 0x0A1038)])
    n1 = np.sin(X * 0.33 + Y * 0.21 + TAU * t) + np.sin(X * 0.17 - Y * 0.29 - TAU * t) + np.sin(Y * 0.38 + TAU * 2 * t + 1)
    n1 = (n1 + 3) / 6.0
    img = add(img, (120, 40, 190), np.clip(n1 - 0.45, 0, 1) * 1.6)
    n2 = (np.sin(X * 0.25 - Y * 0.4 + TAU * t + 2) + np.sin(X * 0.4 + Y * 0.12 - TAU * t) + 2) / 4.0
    img = add(img, (30, 90, 200), np.clip(n2 - 0.5, 0, 1) * 1.3)
    stars(img, 9, 44, H, t, 0xFFFFFF, 0.3)
    # estrella fugaz
    if t < 0.62:
        p = t / 0.62
        hx, hy = 38 - p * 44, -6 + p * 34
        for k in range(10):
            xx, yy = int(round(hx + k * 0.8)), int(round(hy - k * 0.55))
            if 0 <= xx < W and 0 <= yy < H: img[yy, xx] = np.clip(img[yy, xx] + 255 * (1 - k / 10.0), 0, 255)
    return img

def lava(t):
    rock = 0x1A1210
    n = smooth_noise(11, 7, 11) * 0.65 + smooth_noise(12, 14, 22) * 0.35
    crack = np.clip(1 - np.abs(n - 0.5) * 7.0, 0, 1)
    img = np.ones((H, W, 3)) * hexc(rock) * (0.6 + 0.8 * smooth_noise(3, 10, 16))[..., None]
    ph = smooth_noise(21, 6, 9)
    glow = crack * (0.65 + 0.35 * np.sin(TAU * (t + ph * 2)))
    col = pal([(0, 0x5A0A00), (.45, 0xE0300A), (.75, 0xFF8A12), (1, 0xFFE46A)], glow)
    img = np.where((crack > 0.28)[..., None], col, img)
    img = add(img, (255, 80, 10), np.clip(crack - 0.1, 0, 1) * 0.25 * (0.6 + 0.4 * np.sin(TAU * t)))
    r = np.random.RandomState(5)
    for _ in range(14):
        x0, y0, k = r.randint(0, W), r.randint(0, H), r.randint(1, 3)
        y = (y0 - H * t * k) % H
        x = int(round(x0 + 2 * math.sin(TAU * (t * k + x0 / 7.0))))
        if 0 <= x < W: img[int(y), x] = np.clip(img[int(y), x] + np.array([255, 190, 60]) * (0.5 + 0.5 * math.sin(TAU * t * 3 + x0)), 0, 255)
    return img

def llamas(t):
    img = vgrad([(0, 0x090203), (.6, 0x1C0504), (1, 0x2E0A04)])
    top = 24 + 7 * np.sin(TAU * (X / W * 2 + t)) + 4 * np.sin(TAU * (X / W * 5 - 2 * t) + 1) + 3 * np.sin(TAU * (X / W * 9 + 3 * t))
    tongue = np.abs(np.sin(TAU * (X / W * 4 - t) + 0.5))
    top = top - tongue * 8
    depth = np.clip((Y - top) / np.maximum(1, H - top), 0, 1)
    inside = Y >= top
    k = np.round(depth * 5) / 5.0   # bandas planas = aspecto pixel art
    col = pal([(0, 0x6E0C08), (.25, 0xD01E0A), (.55, 0xFF6A0C), (.8, 0xFFB81E), (1, 0xFFF2A0)], np.clip(k * 1.25, 0, 1))
    img = np.where(inside[..., None], col, img)
    img = add(img, (255, 70, 10), np.clip(1 - np.abs(Y - top) / 5.0, 0, 1) * 0.25 * ~inside)
    r = np.random.RandomState(8)
    for _ in range(12):
        x0, y0, kk = r.randint(0, W), r.randint(0, H), r.randint(1, 3)
        y = (y0 - H * t * kk) % H
        x = int(round(x0 + 1.5 * math.sin(TAU * (t * kk + x0 / 5.0)))) % W
        img[int(y), x] = np.clip(img[int(y), x] * 0.4 + np.array([255, 200, 70]) * 0.8, 0, 255)
    return img

def matrix(t):
    img = vgrad([(0, 0x010603), (1, 0x02100A)])
    r = np.random.RandomState(13)
    for c in range(0, W, 2):
        sp, off, tail = r.randint(1, 3), r.randint(0, H), r.randint(16, 30)
        head = (off + sp * H * t) % H
        for y in range(H):
            dist = (head - y) % H
            if dist >= tail: continue
            if y % 3 == 2: continue                     # hueco entre "letras"
            if np.random.RandomState(c * 977 + (y // 3) * 31 + int(t * F) * 7).rand() < 0.3 and dist > 1: continue  # titileo de letras
            b = 1 - dist / tail
            col = hexc(0xE8FFE8) if dist < 1.2 else hexc(0x1BFF5A) * b + hexc(0x04301A) * (1 - b)
            img[y, c] = col
            if c + 1 < W: img[y, c + 1] = col * 0.78
    return img

def pulso(t):
    img = vgrad([(0, 0x0A0430), (.4, 0x3A0F6B), (.56, 0xB0307A), (.57, 0x10062A), (1, 0x1A0A44)])
    stars(img, 3, 20, 26, t, 0xFFFFFF)
    cy = 29
    dd = np.sqrt((X - 20) ** 2 + (Y - cy) ** 2)
    sun = dd <= 11
    sc = pal([(0, 0xFFE15A), (.5, 0xFF7A8E), (1, 0xFF2DA6)], np.clip((Y - (cy - 11)) / 22.0, 0, 1))
    slit = ((Y - cy + 12) - 4.0 * (round(t * F) / F)) % 4 < np.clip((Y - (cy - 6)) / 5.0, 0, 3) * (Y > cy - 4)
    img = np.where((sun & ~slit)[..., None], sc, img)
    img = add(img, (255, 60, 160), np.exp(-np.clip(dd - 11, 0, None) / 3.0) * 0.28 * (dd > 11))
    ridge = 36 - (smooth_noise(2, 8, 3)[0] * 6)
    img = np.where(((Y >= ridge[None, :]) & (Y < 37))[..., None], hexc(0x120633), img)
    fl = Y >= 37
    img = np.where(fl[..., None], hexc(0x12063A), img)
    for k in range(8):                                   # lineas horizontales que se acercan
        yk = 37 + (H - 37) * (((k + t) / 8.0) ** 2)
        img = add(img, (255, 60, 190), np.exp(-((Y - yk) / 0.7) ** 2) * fl * 0.95)
    for j in range(-9, 10):                               # lineas que salen del punto de fuga
        xl = 20 + j * 4.2 * (Y - 36) / 27.0
        img = add(img, (40, 220, 255), np.exp(-((X - xl) / 0.55) ** 2) * fl * 0.8)
    img = add(img, (255, 255, 255), np.exp(-((Y - 36.5) / .6) ** 2) * 0.35 * (0.6 + 0.4 * math.sin(TAU * t)))
    return img

def arcoiris(t):
    h = ((X * 0.8 + Y * 0.55 + 2.2 * np.sin(Y / 9.0 + TAU * t)) / 40.0 - t) % 1.0
    h6 = h * 6; i = np.floor(h6).astype(int) % 6; f = h6 - np.floor(h6)
    s, v = 0.62, 0.97
    p, q, tt = v * (1 - s), v * (1 - s * f), v * (1 - s * (1 - f))
    rgb = np.stack([np.choose(i, [v, q, p, p, tt, v]), np.choose(i, [tt, v, v, q, p, p]), np.choose(i, [p, p, tt, v, v, q])], axis=-1) * 255
    rgb = np.round(rgb / 28) * 28                          # bandas planas
    img = rgb * (0.82 + 0.18 * np.sin((X + Y) * 1.2)[..., None])
    stars(img, 17, 22, H, t, 0xFFFFFF, 0.5)
    img[:, 0:2] *= 0.45; img[:, W - 2:] *= 0.45; img[0:2] *= 0.45; img[H - 2:] *= 0.45
    return img

def mareas(t):
    hz = 27
    img = vgrad([(0, 0x1A1B52), (.25, 0x5A3B8A), (.42, 0xF08A62), (.43, 0x0F2A5C), (1, 0x061434)])
    stars(img, 6, 14, 14, t)
    sun = np.sqrt((X - 20) ** 2 + ((Y - hz) * 1.0) ** 2) <= 9 + 0.4 * math.sin(TAU * t)
    img = np.where((sun & (Y < hz))[..., None], pal([(0, 0xFFF1A8), (1, 0xFF8A5C)], np.clip((Y - (hz - 9)) / 9.0, 0, 1)), img)
    sea = Y >= hz
    d = (Y - hz) / (H - hz)
    wave = np.sin(TAU * (X / W * (3 + d * 5) + d * 3 - t))
    img = np.where((sea & (wave > 0.55))[..., None], hexc(0x2E6FC4) * (0.6 + 0.6 * d)[..., None] + 14, img)
    gl = np.exp(-((X - 20) / (2.2 + d * 7)) ** 2) * (np.sin(TAU * (Y * 0.33 - 2 * t)) > 0.15) * sea * np.clip(1 - d * 1.1, 0, 1)
    img = add(img, (255, 170, 90), gl * 1.1)
    foam = sea & (np.sin(TAU * (X / W * 6 - t + d * 4)) > 0.93) & (d > 0.25)
    img = np.where(foam[..., None], np.clip(img + 70, 0, 255), img)
    return img

DESIGNS = [("atenas", atenas), ("aurora", aurora), ("galaxia", galaxia), ("lava", lava), ("llamas", llamas),
           ("matrix", matrix), ("pulso", pulso), ("arcoiris", arcoiris), ("mareas", mareas)]

# ------------------------------------------------------------------ empaquetado
def sheet(low):
    outer = Image.fromarray(np.clip(low, 0, 255).astype("uint8"), "RGB").resize((80, 128), Image.NEAREST).convert("RGBA")
    inner = outer.point(lambda v: int(v * .55)); inner.putalpha(255)
    tex = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    tex.paste(outer, (8, 8)); tex.paste(inner, (96, 8))
    tex.paste(outer.crop((0, 0, 80, 1)).resize((80, 8)), (8, 0))
    tex.paste(outer.crop((0, 127, 80, 128)).resize((80, 8)), (88, 0))
    tex.paste(outer.crop((0, 0, 1, 128)).resize((8, 128)), (0, 8))
    tex.paste(outer.crop((79, 0, 80, 128)).resize((8, 128)), (88, 8))
    return tex

def main():
    for cid, fn in DESIGNS:
        frames = [fn(f / float(F)) for f in range(F)]
        strip = Image.new("RGBA", (512, 256 * F), (0, 0, 0, 0))
        for f, low in enumerate(frames): strip.paste(sheet(low), (0, 256 * f))
        ic = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        ic.paste(Image.fromarray(np.clip(frames[2], 0, 255).astype("uint8"), "RGB").resize((20, 32), Image.BOX).convert("RGBA"), (6, 0))
        ImageDraw.Draw(ic).rectangle((6, 0, 25, 31), outline=(0, 0, 0, 120))
        for base in TARGETS:
            cd = os.path.join(base, "textures", "cosmetics", "capes"); idr = os.path.join(base, "textures", "cosmetics", "icons")
            os.makedirs(cd, exist_ok=True); os.makedirs(idr, exist_ok=True)
            strip.save(os.path.join(cd, cid + ".png"), optimize=True); ic.save(os.path.join(idr, "cape_" + cid + ".png"))
        # vista previa (4 fotogramas) para revisar
        prev = Image.new("RGB", (W * 4 * 4, H * 4), (0, 0, 0))
        for k, f in enumerate((0, 3, 6, 9)):
            prev.paste(Image.fromarray(np.clip(frames[f], 0, 255).astype("uint8"), "RGB").resize((W * 4, H * 4), Image.NEAREST), (k * W * 4, 0))
        prev.save(os.path.join(HERE, "preview_" + cid + ".png"))
        print(cid, "ok")

if __name__ == "__main__":
    main()
