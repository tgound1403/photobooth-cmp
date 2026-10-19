"""Generate procedural film-effect textures (light leaks, dust/scratches).

Deterministic: fixed seeds, no timestamps. Run with:
    python3 -I tools/fx-gen/generate_fx.py
Outputs go to app/src/main/assets/filters/fx/.
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

SIZE = 1024
MAX_BYTES = 300 * 1024
ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "filters" / "fx"


def smoothstep(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3.0 - 2.0 * t)


def leak_mask(cx, cy, angle, ax, ay, peak):
    """Elliptical soft blob, rotated, with smoothstep falloff. Returns L image."""
    cos_a, sin_a = math.cos(angle), math.sin(angle)
    data = []
    for y in range(SIZE):
        dy = y - cy
        for x in range(SIZE):
            dx = x - cx
            u = dx * cos_a + dy * sin_a
            v = -dx * sin_a + dy * cos_a
            d = math.sqrt((u / ax) ** 2 + (v / ay) ** 2)
            data.append(int(round(255 * peak * smoothstep(1.0 - d))))
    mask = Image.new("L", (SIZE, SIZE))
    mask.putdata(data)
    return mask.filter(ImageFilter.GaussianBlur(28))


def make_leak(color, cx, cy):
    mask = leak_mask(cx, cy, math.pi / 4, ax=640.0, ay=340.0, peak=0.78)
    img = Image.new("RGB", (SIZE, SIZE), color)
    img.putalpha(mask)
    return img


def make_dust(seed=1407):
    rng = random.Random(seed)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # About 120 specks: radius 1-3 px, mix of white and dark.
    for _ in range(120):
        x = rng.randrange(SIZE)
        y = rng.randrange(SIZE)
        r = rng.choice((1, 1, 2, 2, 3))
        if rng.random() < 0.6:
            color = (255, 255, 255, rng.randint(110, 200))
        else:
            color = (25, 20, 18, rng.randint(90, 170))
        draw.ellipse([x - r, y - r, x + r, y + r], fill=color)

    # About 6 thin curved scratches, 1 px wide, low alpha.
    for _ in range(6):
        p0 = (rng.uniform(0, SIZE), rng.uniform(0, SIZE))
        p2 = (p0[0] + rng.uniform(-420, 420), p0[1] + rng.uniform(-420, 420))
        bend = rng.uniform(-200, 200)
        mid = ((p0[0] + p2[0]) / 2.0, (p0[1] + p2[1]) / 2.0)
        ddx, ddy = p2[0] - p0[0], p2[1] - p0[1]
        length = math.hypot(ddx, ddy) or 1.0
        p1 = (mid[0] - ddy / length * bend, mid[1] + ddx / length * bend)
        alpha = rng.randint(40, 90)
        points = []
        steps = 96
        for i in range(steps + 1):
            t = i / steps
            bx = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
            by = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
            points.append((int(round(bx)), int(round(by))))
        shade = (255, 255, 255, alpha) if rng.random() < 0.5 else (20, 16, 14, alpha)
        draw.line(points, fill=shade, width=1)
    return img


def save_webp(img, path, lossless=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    kwargs = {"format": "WEBP", "method": 6}
    if lossless:
        kwargs["lossless"] = True
    else:
        kwargs["quality"] = 90
    img.save(path, **kwargs)
    if path.stat().st_size > MAX_BYTES and not lossless:
        img.save(path, format="WEBP", quality=80, method=6)
    size = path.stat().st_size
    print(f"{path.relative_to(ROOT)}: {size} bytes, {img.mode}, {img.size}")


def main():
    save_webp(make_leak((255, 100, 30), cx=150.0, cy=150.0), OUT_DIR / "leak_warm.webp")
    save_webp(make_dust(), OUT_DIR / "dust.webp", lossless=True)


if __name__ == "__main__":
    main()
