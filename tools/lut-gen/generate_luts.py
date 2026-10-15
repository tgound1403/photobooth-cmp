#!/usr/bin/env python3
"""Generates the 3D LUT PNGs and catalog.json used by the photobooth filters.

Run from the repository root:

    python3 -I tools/lut-gen/generate_luts.py

Standard library only (zlib + struct for PNG encoding).

LUT layout (must match the renderer):
  * PNG 512x512, RGB 8-bit, 64 levels per channel.
  * 8x8 grid of 64x64 tiles. Input (r, g, b) with each channel in 0..63:
    tile index = b, tile column = b % 8, tile row = b // 8, and inside the tile x = r, y = g.
    Pixel position = (col * 64 + r, row * 64 + g). The pixel value is the output colour.
  * The identity LUT stores round(c * 255 / 63) for each channel.
"""

import json
import math
import os
import shutil
import struct
import sys
import zlib

LEVELS = 64
GRID = 8
SIZE = LEVELS * GRID  # 512
MAX_TOTAL_BYTES = 3 * 1024 * 1024

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSET_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "filters")
LUT_DIR = os.path.join(ASSET_DIR, "luts")
CATALOG_PATH = os.path.join(ASSET_DIR, "catalog.json")

# ---------------------------------------------------------------------------
# Building blocks. Every colour op takes and returns floats in 0..1.
# ---------------------------------------------------------------------------


def clamp01(x):
    if x < 0.0:
        return 0.0
    if x > 1.0:
        return 1.0
    return x


def luma709(r, g, b):
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def monotone_curve(points):
    """Monotone cubic Hermite (Fritsch-Carlson) tone curve through (x, y) control points."""
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    n = len(xs)
    d = [(ys[i + 1] - ys[i]) / (xs[i + 1] - xs[i]) for i in range(n - 1)]
    m = [0.0] * n
    m[0] = d[0]
    m[-1] = d[-1]
    for i in range(1, n - 1):
        m[i] = 0.0 if d[i - 1] * d[i] <= 0 else (d[i - 1] + d[i]) / 2.0
    for i in range(n - 1):
        if d[i] == 0.0:
            m[i] = 0.0
            m[i + 1] = 0.0
            continue
        a = m[i] / d[i]
        b = m[i + 1] / d[i]
        s = a * a + b * b
        if s > 9.0:
            t = 3.0 / math.sqrt(s)
            m[i] = t * a * d[i]
            m[i + 1] = t * b * d[i]

    def curve(x):
        x = clamp01(x)
        if x <= xs[0]:
            return ys[0]
        if x >= xs[-1]:
            return ys[-1]
        i = 0
        while x > xs[i + 1]:
            i += 1
        h = xs[i + 1] - xs[i]
        t = (x - xs[i]) / h
        t2 = t * t
        t3 = t2 * t
        h00 = 2 * t3 - 3 * t2 + 1
        h10 = t3 - 2 * t2 + t
        h01 = -2 * t3 + 3 * t2
        h11 = t3 - t2
        return h00 * ys[i] + h10 * h * m[i] + h01 * ys[i + 1] + h11 * h * m[i + 1]

    return curve


def op_curve(points):
    """Same tone curve applied to each channel."""
    c = monotone_curve(points)
    return lambda r, g, b: (c(r), c(g), c(b))


def op_lift_gamma_gain(lift=(0.0, 0.0, 0.0), gamma=(1.0, 1.0, 1.0), gain=(1.0, 1.0, 1.0)):
    """Per-channel lift (shadows), gamma (midtones) and gain (highlights)."""

    def op(r, g, b):
        out = []
        for x, lf, gm, gn in zip((r, g, b), lift, gamma, gain):
            x = x + lf * (1.0 - x)
            x = max(x, 0.0) ** (1.0 / gm)
            out.append(x * gn)
        return tuple(out)

    return op


def op_saturation(s):
    """Scales chroma around Rec.709 luma. s=0 is grayscale, s=1 is unchanged."""

    def op(r, g, b):
        y = luma709(r, g, b)
        return (y + (r - y) * s, y + (g - y) * s, y + (b - y) * s)

    return op


def op_white_balance(temp=0.0, tint=0.0):
    """Shifts white balance. temp > 0 warms (red up, blue down); tint > 0 adds magenta."""
    dr = 0.07 * temp + 0.03 * tint
    dg = -0.05 * tint
    db = -0.07 * temp + 0.03 * tint
    return lambda r, g, b: (r + dr, g + dg, b + db)


def op_split_tone(shadow=(0.0, 0.0, 0.0), highlight=(0.0, 0.0, 0.0)):
    """Adds a colour offset to shadows and highlights, weighted by Rec.709 luma."""

    def op(r, g, b):
        y = clamp01(luma709(r, g, b))
        ws = (1.0 - y) * (1.0 - y)
        wh = y * y
        return (
            r + shadow[0] * ws + highlight[0] * wh,
            g + shadow[1] * ws + highlight[1] * wh,
            b + shadow[2] * ws + highlight[2] * wh,
        )

    return op


def op_fade(amount):
    """Black-point lift: blacks move up to `amount`, whites stay put."""
    return lambda r, g, b: (
        amount + r * (1.0 - amount),
        amount + g * (1.0 - amount),
        amount + b * (1.0 - amount),
    )


def op_channel_mix(weights):
    """Grayscale channel mixer. Weights should sum to about 1."""
    wr, wg, wb = weights

    def op(r, g, b):
        y = wr * r + wg * g + wb * b
        return (y, y, y)

    return op


def op_color_matrix(m):
    """Android ColorMatrix (4x5, row-major) applied to normalised RGB with alpha = 255.

    The offset column is in 0..255 units, so it is divided by 255 here.
    """

    def op(r, g, b):
        out = []
        for row in range(3):
            k = row * 5
            v = m[k] * r + m[k + 1] * g + m[k + 2] * b + m[k + 3] * 1.0 + m[k + 4] / 255.0
            out.append(v)
        return tuple(out)

    return op


def compose(*ops):
    """Chains ops left to right, clamping to 0..1 after each step."""

    def fn(r, g, b):
        for op in ops:
            r, g, b = op(r, g, b)
            r, g, b = clamp01(r), clamp01(g), clamp01(b)
        return r, g, b

    return fn


# ---------------------------------------------------------------------------
# Classic presets: ColorMatrix values copied from EditImageViewModel.filters.
# "None" maps to the original (no LUT file).
# ---------------------------------------------------------------------------

CLASSIC_MATRICES = {
    "bnw": [0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0],
    "kodak": [1.0, 0.0, 0.0, 0.0, 0.0, -0.4, 1.3, -0.4, 0.2, -0.1, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0],
    "fujifilm": [1.0, 0.0, 0.0, 1.7, -1.7, 0.6, 1.0, 0.0, 0.0, 0.3, -0.2, 0.0, 1.0, 0.0, 0.0, 0.1, 0.0, 0.0, 0.6, 0.2],
    "nikon": [1.3, -0.3, 1.1, 0.0, 0.0, 0.0, 1.3, 0.2, 0.0, 0.0, 0.0, 0.0, 0.8, 0.2, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0],
    "pentax": [0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.6, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0],
    "minolta": [1.0, 0.0, 0.0, 0.0, 0.0, -0.2, 1.0, 0.3, 0.1, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0],
}

CLASSIC_NAMES = {
    "bnw": "BnW",
    "kodak": "Kodak",
    "fujifilm": "FujiFilm",
    "nikon": "Nikon",
    "pentax": "Pentax",
    "minolta": "Minolta",
}

# ---------------------------------------------------------------------------
# New looks. Each entry: id, name, group, premium, builder.
# Portrait looks keep highlights below ~0.97 so skin tones do not clip.
# ---------------------------------------------------------------------------

NEW_FILTERS = [
    # Film
    ("golden_hour", "Golden Hour", "Film", False, lambda: compose(
        op_white_balance(temp=0.35),
        op_curve([(0, 0.04), (0.25, 0.27), (0.5, 0.51), (0.75, 0.77), (1, 0.97)]),
        op_split_tone(shadow=(0.0, 0.0, 0.04), highlight=(0.05, 0.025, -0.04)),
        op_saturation(1.05))),
    ("faded_mint", "Faded Mint", "Film", True, lambda: compose(
        op_fade(0.07),
        op_white_balance(temp=-0.15),
        op_split_tone(shadow=(-0.02, 0.03, 0.0), highlight=(0.0, 0.02, 0.01)),
        op_saturation(0.85))),
    ("amber_roll", "Amber Roll", "Film", False, lambda: compose(
        op_white_balance(temp=0.25),
        op_curve([(0, 0.02), (0.25, 0.23), (0.5, 0.5), (0.75, 0.79), (1, 0.98)]),
        op_split_tone(highlight=(0.05, 0.03, -0.03)),
        op_saturation(0.92))),
    ("cool_negative", "Cool Negative", "Film", True, lambda: compose(
        op_white_balance(temp=-0.3),
        op_curve([(0, 0.02), (0.5, 0.5), (1, 0.98)]),
        op_fade(0.03),
        op_saturation(0.8))),
    ("sunday_film", "Sunday Film", "Film", False, lambda: compose(
        op_white_balance(tint=0.3),
        op_fade(0.05),
        op_curve([(0, 0.03), (0.5, 0.5), (1, 0.97)]),
        op_saturation(0.95))),
    ("dusk_print", "Dusk Print", "Film", False, lambda: compose(
        op_split_tone(shadow=(0.02, -0.01, 0.05), highlight=(0.04, 0.01, -0.02)),
        op_curve([(0, 0.01), (0.25, 0.22), (0.5, 0.5), (0.75, 0.78), (1, 0.99)]),
        op_saturation(0.9))),
    # Portrait
    ("soft_glow", "Soft Glow", "Portrait", False, lambda: compose(
        op_curve([(0, 0.03), (0.5, 0.51), (0.9, 0.93), (1, 0.96)]),
        op_white_balance(temp=0.1),
        op_saturation(0.98))),
    ("rose_skin", "Rose Skin", "Portrait", True, lambda: compose(
        op_white_balance(temp=0.12, tint=0.15),
        op_split_tone(highlight=(0.03, 0.0, 0.01)),
        op_curve([(0, 0.01), (0.5, 0.5), (1, 0.96)]))),
    ("honey_light", "Honey Light", "Portrait", False, lambda: compose(
        op_white_balance(temp=0.2),
        op_curve([(0, 0.02), (0.5, 0.51), (1, 0.97)]),
        op_saturation(1.05))),
    ("clear_tone", "Clear Tone", "Portrait", False, lambda: compose(
        op_curve([(0, 0.01), (0.25, 0.22), (0.5, 0.5), (0.75, 0.78), (1, 0.98)]),
        op_saturation(0.95))),
    ("peach_glow", "Peach Glow", "Portrait", True, lambda: compose(
        op_white_balance(temp=0.25),
        op_split_tone(shadow=(0.02, 0.0, -0.02), highlight=(0.04, 0.02, -0.02)),
        op_fade(0.03),
        op_saturation(0.95))),
    ("cream_matte", "Cream Matte", "Portrait", False, lambda: compose(
        op_fade(0.06),
        op_white_balance(temp=0.08),
        op_curve([(0, 0.05), (0.5, 0.52), (1, 0.95)]),
        op_saturation(0.85))),
    # Mood
    ("teal_night", "Teal Night", "Mood", False, lambda: compose(
        op_split_tone(shadow=(-0.04, 0.03, 0.05), highlight=(0.04, 0.02, -0.03)),
        op_curve([(0, 0.0), (0.25, 0.2), (0.5, 0.5), (0.75, 0.8), (1, 1.0)]),
        op_saturation(0.9))),
    ("moody_blue", "Moody Blue", "Mood", True, lambda: compose(
        op_saturation(0.7),
        op_split_tone(shadow=(-0.02, 0.0, 0.06)),
        op_white_balance(temp=-0.1),
        op_fade(0.04),
        op_curve([(0, 0.02), (0.5, 0.48), (1, 0.95)]))),
    ("autumn_haze", "Autumn Haze", "Mood", False, lambda: compose(
        op_fade(0.08),
        op_split_tone(shadow=(0.03, 0.0, -0.03), highlight=(0.05, 0.03, -0.02)),
        op_white_balance(temp=0.1),
        op_saturation(0.85))),
    ("velvet_dusk", "Velvet Dusk", "Mood", True, lambda: compose(
        op_curve([(0, 0.0), (0.25, 0.21), (0.5, 0.5), (0.75, 0.79), (1, 0.99)]),
        op_split_tone(shadow=(0.03, -0.01, 0.04), highlight=(0.05, 0.0, 0.0)),
        op_saturation(1.0))),
    ("pastel_dream", "Pastel Dream", "Mood", False, lambda: compose(
        op_fade(0.1),
        op_saturation(0.75),
        op_split_tone(shadow=(0.02, 0.0, 0.02), highlight=(0.02, 0.02, 0.04)),
        op_white_balance(temp=0.05))),
    ("winter_fog", "Winter Fog", "Mood", False, lambda: compose(
        op_white_balance(temp=-0.2),
        op_fade(0.08),
        op_saturation(0.7),
        op_curve([(0, 0.02), (0.5, 0.5), (1, 0.97)]))),
    # B&W
    ("silver_gelatin", "Silver Gelatin", "B&W", False, lambda: compose(
        op_channel_mix((0.3, 0.59, 0.11)),
        op_curve([(0, 0.0), (0.25, 0.22), (0.5, 0.5), (0.75, 0.78), (1, 1.0)]))),
    ("noir_contrast", "Noir Contrast", "B&W", True, lambda: compose(
        op_channel_mix((0.25, 0.6, 0.15)),
        op_curve([(0, 0.0), (0.2, 0.12), (0.5, 0.5), (0.8, 0.88), (1, 1.0)]))),
    ("soft_mono", "Soft Mono", "B&W", False, lambda: compose(
        op_channel_mix((0.33, 0.5, 0.17)),
        op_fade(0.07),
        op_curve([(0, 0.07), (0.5, 0.52), (1, 0.97)]))),
    ("crimson_mono", "Crimson Mono", "B&W", True, lambda: compose(
        op_channel_mix((0.6, 0.3, 0.1)),
        op_curve([(0, 0.0), (0.25, 0.2), (0.5, 0.5), (0.8, 0.85), (1, 1.0)]))),
    ("ink_wash", "Ink Wash", "B&W", False, lambda: compose(
        op_channel_mix((0.2, 0.6, 0.2)),
        op_curve([(0, 0.04), (0.5, 0.45), (1, 0.92)]))),
    ("light_mono", "Light Mono", "B&W", False, lambda: compose(
        op_channel_mix((0.33, 0.4, 0.27)),
        op_curve([(0, 0.1), (0.5, 0.6), (1, 0.98)]))),
]

GROUP_ORDER = ["Classic", "Film", "Portrait", "Mood", "B&W"]
ORIGINAL = {"id": "original", "name": "Original", "group": "Classic", "lut": None, "premium": False}

# ---------------------------------------------------------------------------
# LUT and PNG helpers.
# ---------------------------------------------------------------------------


def to_byte(v):
    return int(clamp01(v) * 255.0 + 0.5)


def build_lut(fn):
    """Returns the 512x512 RGB raw pixel buffer (row-major, 3 bytes per pixel)."""
    buf = bytearray(SIZE * SIZE * 3)
    for b in range(LEVELS):
        col = b % GRID
        row = b // GRID
        bv = b / 63.0
        for g in range(LEVELS):
            gv = g / 63.0
            y = row * LEVELS + g
            base_row = y * SIZE
            for r in range(LEVELS):
                ro, go, bo = fn(r / 63.0, gv, bv)
                off = (base_row + col * LEVELS + r) * 3
                buf[off] = to_byte(ro)
                buf[off + 1] = to_byte(go)
                buf[off + 2] = to_byte(bo)
    return bytes(buf)


def _predict(ftype, x, a, b, c):
    """PNG predictor for one byte. a = left, b = up, c = up-left."""
    if ftype == 0:
        return 0
    if ftype == 1:
        return a
    if ftype == 2:
        return b
    if ftype == 3:
        return (a + b) >> 1
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def _filter_row(ftype, cur, prev, bpp=3):
    out = bytearray(len(cur))
    for i in range(len(cur)):
        a = cur[i - bpp] if i >= bpp else 0
        b = prev[i]
        c = prev[i - bpp] if i >= bpp else 0
        out[i] = (cur[i] - _predict(ftype, a, a, b, c)) & 0xFF
    return out


def _best_filtered_row(cur, prev):
    """Tries all five PNG filter types and keeps the one with the smallest signed-byte sum."""
    best = None
    for ftype in range(5):
        filtered = _filter_row(ftype, cur, prev)
        score = sum(v if v < 128 else 256 - v for v in filtered)
        if best is None or score < best[0]:
            best = (score, ftype, filtered)
    return best[1], best[2]


def encode_png(width, height, rgb):
    stride = width * 3
    raw = bytearray()
    prev = bytes(stride)
    for y in range(height):
        cur = rgb[y * stride:(y + 1) * stride]
        ftype, filtered = _best_filtered_row(cur, prev)
        raw.append(ftype)
        raw.extend(filtered)
        prev = cur

    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)  # 8-bit RGB
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", ihdr)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )


def decode_png_rgb(data):
    """Minimal 8-bit RGB PNG decoder (all five filter types, no interlace)."""
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a PNG"
    pos = 8
    idat = bytearray()
    width = height = None
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        if tag == b"IHDR":
            width, height, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            assert depth == 8 and ctype == 2 and interlace == 0, "unexpected PNG format"
        elif tag == b"IDAT":
            idat.extend(body)
        pos += 12 + length
    raw = zlib.decompress(bytes(idat))
    stride = width * 3
    out = bytearray()
    prev = bytearray(stride)
    for y in range(height):
        ftype = raw[y * (stride + 1)]
        line = raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)]
        cur = bytearray(stride)
        for i in range(stride):
            a = cur[i - 3] if i >= 3 else 0
            c = prev[i - 3] if i >= 3 else 0
            cur[i] = (line[i] + _predict(ftype, a, a, prev[i], c)) & 0xFF
        out.extend(cur)
        prev = cur
    return width, height, bytes(out)


def lut_offset(r, g, b):
    """Byte offset of the LUT pixel for input levels (r, g, b) in 0..63."""
    col = b % GRID
    row = b // GRID
    x = col * LEVELS + r
    y = row * LEVELS + g
    return (y * SIZE + x) * 3


def identity_expected(level):
    return (level * 255 * 2 + 63) // (2 * 63)  # integer round-half-up of level * 255 / 63


def self_check(identity_png):
    width, height, pixels = decode_png_rgb(identity_png)
    assert (width, height) == (SIZE, SIZE), "identity LUT has wrong size"
    samples = [(0, 0, 0), (63, 0, 0), (0, 63, 0), (0, 0, 63), (5, 17, 42)]
    for r, g, b in samples:
        off = lut_offset(r, g, b)
        got = (pixels[off], pixels[off + 1], pixels[off + 2])
        want = (identity_expected(r), identity_expected(g), identity_expected(b))
        if got != want:
            raise SystemExit(f"self-check failed at input {(r, g, b)}: got {got}, want {want}")
    print("self-check: identity LUT matches formula at 5 sample positions")


def main():
    all_entries = [ORIGINAL]
    builders = []
    for fid, name in CLASSIC_NAMES.items():
        matrix = CLASSIC_MATRICES[fid]
        all_entries.append({"id": fid, "name": name, "group": "Classic", "lut": f"luts/{fid}.png", "premium": False})
        builders.append((fid, op_color_matrix(matrix)))
    for fid, name, group, premium, make in NEW_FILTERS:
        all_entries.append({"id": fid, "name": name, "group": group, "lut": f"luts/{fid}.png", "premium": premium})
        builders.append((fid, make()))

    ids = [e["id"] for e in all_entries]
    assert len(ids) == len(set(ids)), "duplicate filter ids"
    for e in all_entries:
        assert e["id"] == e["id"].lower() and " " not in e["id"], "ids must be lowercase snake_case"
        assert e["group"] in GROUP_ORDER, f"unknown group {e['group']}"

    identity_png = encode_png(SIZE, SIZE, build_lut(lambda r, g, b: (r, g, b)))
    self_check(identity_png)

    os.makedirs(LUT_DIR, exist_ok=True)
    expected_files = {f"{fid}.png" for fid, _ in builders}
    for stale in os.listdir(LUT_DIR):
        if stale.endswith(".png") and stale not in expected_files:
            os.remove(os.path.join(LUT_DIR, stale))

    total_bytes = 0
    for fid, fn in builders:
        png = encode_png(SIZE, SIZE, build_lut(fn))
        with open(os.path.join(LUT_DIR, f"{fid}.png"), "wb") as f:
            f.write(png)
        total_bytes += len(png)
        print(f"wrote luts/{fid}.png ({len(png) / 1024:.1f} KB)")

    catalog = {"version": 1, "filters": all_entries}
    with open(CATALOG_PATH, "w", encoding="utf-8") as f:
        f.write(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n")

    print()
    print("Summary")
    for group in GROUP_ORDER:
        members = [e for e in all_entries if e["group"] == group]
        premium = sum(1 for e in members if e["premium"])
        print(f"  {group:<9} {len(members):>3} filters ({premium} premium)")
    print(f"  total     {len(all_entries):>3} entries, {len(builders)} LUT files")
    print(f"  LUT PNG total size: {total_bytes / 1024:.1f} KB")
    if total_bytes >= MAX_TOTAL_BYTES:
        sys.exit(f"LUT assets exceed {MAX_TOTAL_BYTES} bytes")


if __name__ == "__main__":
    main()
