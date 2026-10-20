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
MAX_TOTAL_BYTES = 100 * 1024 * 1024

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSET_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "filters")
LUT_DIR = os.path.join(ASSET_DIR, "luts")
CATALOG_PATH = os.path.join(ASSET_DIR, "catalog.json")
PACK_JSON = os.path.join(REPO_ROOT, "tools", "lut-gen", "japan_pack.json")

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


def op_curve_rgb(points_r, points_g, points_b):
    """Separate tone curve per channel."""
    cr, cg, cb = monotone_curve(points_r), monotone_curve(points_g), monotone_curve(points_b)
    return lambda r, g, b: (cr(r), cg(g), cb(b))


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


def op_mid_tone(offset=(0.0, 0.0, 0.0)):
    """Adds a colour offset weighted toward midtones (peak at luma 0.5), like a colour-grading midtone wheel."""

    def op(r, g, b):
        y = clamp01(luma709(r, g, b))
        w = 4.0 * y * (1.0 - y)
        return (r + offset[0] * w, g + offset[1] * w, b + offset[2] * w)

    return op


HSL_BAND_CENTERS = {
    "red": 0.0, "orange": 30.0, "yellow": 60.0, "green": 120.0,
    "aqua": 180.0, "blue": 240.0, "purple": 275.0, "magenta": 320.0,
}


def _rgb_to_hsl(r, g, b):
    mx = max(r, g, b)
    mn = min(r, g, b)
    l = (mx + mn) / 2.0
    d = mx - mn
    if d < 1e-9:
        return 0.0, 0.0, l
    s = d / (1.0 - abs(2.0 * l - 1.0)) if 0.0 < l < 1.0 else 0.0
    if mx == r:
        h = ((g - b) / d) % 6.0
    elif mx == g:
        h = (b - r) / d + 2.0
    else:
        h = (r - g) / d + 4.0
    return h * 60.0, s, l


def _hsl_to_rgb(h, s, l):
    c = (1.0 - abs(2.0 * l - 1.0)) * s
    hp = (h % 360.0) / 60.0
    x = c * (1.0 - abs(hp % 2.0 - 1.0))
    r1, g1, b1 = [(c, x, 0), (x, c, 0), (0, c, x), (0, x, c), (x, 0, c), (c, 0, x)][min(int(hp), 5)]
    m = l - c / 2.0
    return r1 + m, g1 + m, b1 + m


def op_hsl(bands):
    """Lightroom-style HSL. bands maps a band name to (hue_shift_deg, sat_scale, lum_offset).

    Each band has a smooth cosine falloff (60 degrees half-width), so neighbouring bands blend.
    Grey pixels (no chroma) are left untouched.
    """
    active = [(HSL_BAND_CENTERS[k], v) for k, v in bands.items()]

    def op(r, g, b):
        h, s, l = _rgb_to_hsl(clamp01(r), clamp01(g), clamp01(b))
        if s < 1e-6:
            return r, g, b
        dh, ds, dl = 0.0, 0.0, 0.0
        for centre, (hs, ss, ls) in active:
            dist = abs((h - centre + 180.0) % 360.0 - 180.0)
            if dist >= 60.0:
                continue
            w = 0.5 * (1.0 + math.cos(math.pi * dist / 60.0))
            w *= min(1.0, s * 4.0)  # fade out near grey so noise does not swing hue
            dh += hs * w
            ds += (ss - 1.0) * w
            dl += ls * w
        return _hsl_to_rgb(h + dh, clamp01(s * (1.0 + ds)), clamp01(l + dl))

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
# Looks. Each entry: id, name, group, description, builder.
# Fujifilm-, Kodak- and VSCO-style film looks built from the ops above.
# Highlights stay below ~0.98 so skin tones do not clip; blacks are lifted a little.
# ---------------------------------------------------------------------------

FILTERS = [
    # Classic: Fujifilm style
    ("fuji_velvia", "Fuji Velvia", "Fuji", "Slide-film punch: deep saturated blues, emerald greens, strong reds, rich blacks and high contrast.", lambda: compose(
        op_curve([(0,0.0),(0.2,0.14),(0.5,0.49),(0.8,0.87),(1,0.99)]),
        op_hsl({"red":(-2,1.15,-0.02),"orange":(1,0.92,0.0),"yellow":(-2,1.08,0.0),
                "green":(5,1.3,-0.05),"aqua":(0,1.2,-0.04),"blue":(-2,1.3,-0.08),"purple":(0,1.1,-0.03),"magenta":(0,1.1,0.0)}),
        op_split_tone(shadow=(-0.012,0.004,0.02)),
        op_saturation(1.06))),
    ("fuji_classic_chrome", "Fuji Classic Chrome", "Fuji", "Muted documentary colour: low saturation, deep cool shadows, olive greens and vibrant cyan-leaning blues.", lambda: compose(
        op_curve([(0,0.0),(0.2,0.155),(0.5,0.49),(0.8,0.82),(1,0.97)]),
        op_hsl({"red":(2,0.85,-0.02),"orange":(-2,0.85,0.0),"yellow":(-4,0.8,-0.02),
                "green":(-8,0.6,-0.05),"aqua":(-6,0.75,-0.04),"blue":(-8,1.0,-0.07),"purple":(-6,0.6,-0.03),"magenta":(0,0.45,-0.03)}),
        op_split_tone(shadow=(-0.015,0.004,0.03),highlight=(0.012,0.006,-0.006)),
        op_saturation(0.82))),
    ("fuji_superia", "Fuji Superia", "Fuji", "Everyday colour negative: neutral-cool whites, cyan-green shadows, strong greens and blues, natural skin.", lambda: compose(
        op_curve([(0,0.01),(0.25,0.225),(0.5,0.50),(0.75,0.78),(1,0.985)]),
        op_hsl({"red":(0,1.05,0.0),"orange":(0,1.0,0.02),"yellow":(-2,0.95,0.0),
                "green":(5,1.2,-0.03),"aqua":(2,1.15,-0.03),"blue":(0,1.15,-0.04),"magenta":(0,0.9,0.0)}),
        op_split_tone(shadow=(-0.012,0.018,0.014),highlight=(0.006,0.004,-0.004)),
        op_saturation(1.08))),
    ("fuji_classic_neg", "Fuji Classic Neg", "Fuji", "Contrasty negative look: warm golden highlights, teal shadows, muted yellow-greens, cyan-leaning blues.", lambda: compose(
        op_curve_rgb([(0,0.0),(0.25,0.215),(0.5,0.51),(0.75,0.80),(1,0.985)],
                     [(0,0.01),(0.25,0.235),(0.5,0.50),(0.75,0.775),(1,0.97)],
                     [(0,0.03),(0.25,0.26),(0.5,0.49),(0.75,0.73),(1,0.94)]),
        op_curve([(0,0.0),(0.2,0.17),(0.5,0.5),(0.8,0.82),(1,1.0)]),
        op_hsl({"red":(0,1.1,0.0),"orange":(1,1.2,0.05),"yellow":(-6,0.85,0.0),
                "green":(12,0.7,-0.04),"aqua":(-3,0.8,-0.05),"blue":(-4,0.9,-0.07),"purple":(-8,0.8,-0.04),"magenta":(0,0.8,0.0)}),
        op_split_tone(shadow=(-0.018,0.012,0.022),highlight=(0.03,0.015,-0.02)),
        op_saturation(1.08))),
    ("fuji_forest", "Fuji Forest", "Fuji", "Deep emerald greens, cool lifted shadows, soft warm skin, fine-grain slide feel.", lambda: compose(
        op_white_balance(temp=-0.02, tint=-0.1),
        op_curve_rgb([(0, 0.02), (0.25, 0.225), (0.5, 0.495), (0.75, 0.775), (1, 0.975)],
                     [(0, 0.045), (0.25, 0.265), (0.5, 0.515), (0.75, 0.775), (1, 0.98)],
                     [(0, 0.055), (0.25, 0.255), (0.5, 0.495), (0.75, 0.73), (1, 0.95)]),
        op_curve([(0, 0.0), (0.2, 0.18), (0.5, 0.5), (0.8, 0.815), (1, 1.0)]),
        op_hsl({"red": (4, 0.95, -0.02), "orange": (-4, 0.95, 0.0), "yellow": (10, 0.85, -0.02),
                "green": (6, 1.18, -0.07), "aqua": (6, 1.0, -0.05),
                "blue": (-4, 0.85, -0.06), "magenta": (10, 0.8, 0.0)}),
        op_split_tone(shadow=(-0.02, 0.018, 0.012), highlight=(0.015, 0.012, -0.012)),
        op_saturation(1.04))),
    # Film: Kodak style
    ("kodak_colorplus", "Kodak ColorPlus", "Kodak", "Budget everyday film: golden warmth, creamy highlights, soft lifted blacks and gentle colour.", lambda: compose(
        op_curve_rgb([(0,0.02),(0.25,0.245),(0.5,0.52),(0.75,0.79),(1,0.985)],
                     [(0,0.015),(0.25,0.235),(0.5,0.50),(0.75,0.765),(1,0.97)],
                     [(0,0.03),(0.25,0.22),(0.5,0.465),(0.75,0.71),(1,0.92)]),
        op_hsl({"red":(-2,1.05,0.0),"orange":(-3,1.1,0.06),"yellow":(-6,1.1,0.0),
                "green":(-6,0.85,-0.02),"aqua":(-3,0.9,-0.02),"blue":(0,1.0,-0.03),"purple":(-6,0.85,-0.02)}),
        op_split_tone(shadow=(0.006,0.004,-0.004),highlight=(0.03,0.02,-0.03)),
        op_saturation(0.98))),
    ("kodak_portra", "Kodak Portra", "Kodak", "Soft pastel colour: flattering warm skin, gentle contrast, muted greens and blues, creamy highlights.", lambda: compose(
        op_curve_rgb([(0,0.025),(0.25,0.255),(0.5,0.52),(0.75,0.785),(1,0.975)],
                     [(0,0.025),(0.25,0.25),(0.5,0.505),(0.75,0.77),(1,0.965)],
                     [(0,0.03),(0.25,0.24),(0.5,0.475),(0.75,0.735),(1,0.95)]),
        op_hsl({"red":(0,0.95,0.0),"orange":(0,1.0,0.05),"yellow":(-3,0.9,0.02),
                "green":(4,0.75,0.0),"aqua":(-4,0.75,0.0),"blue":(-4,0.8,0.0),"purple":(0,0.8,0.0),"magenta":(0,0.85,0.0)}),
        op_split_tone(shadow=(0.0,0.006,0.006),highlight=(0.015,0.008,-0.012)),
        op_saturation(0.95))),
    ("kodak_gold", "Kodak Gold", "Kodak", "Warm golden cast, rich yellows and reds, warm greens, nostalgic sunny contrast.", lambda: compose(
        op_curve_rgb([(0,0.01),(0.25,0.24),(0.5,0.52),(0.75,0.79),(1,0.985)],
                     [(0,0.01),(0.25,0.23),(0.5,0.495),(0.75,0.765),(1,0.97)],
                     [(0,0.02),(0.25,0.21),(0.5,0.455),(0.75,0.70),(1,0.91)]),
        op_hsl({"red":(-1,1.15,0.0),"orange":(-2,1.15,0.04),"yellow":(-4,1.2,0.0),
                "green":(-8,0.95,-0.03),"aqua":(-4,0.95,-0.03),"blue":(-2,0.95,-0.04),"magenta":(0,0.9,0.0)}),
        op_split_tone(shadow=(0.01,0.004,-0.012),highlight=(0.025,0.015,-0.03)),
        op_saturation(1.08))),
    ("kodachrome", "Kodachrome", "Kodak", "Slide classic: deep contrast, rich crimson reds, warm highlights, olive greens and deep cyan blues.", lambda: compose(
        op_curve_rgb([(0,0.0),(0.2,0.15),(0.5,0.52),(0.8,0.855),(1,0.985)],
                     [(0,0.0),(0.2,0.15),(0.5,0.495),(0.8,0.84),(1,0.98)],
                     [(0,0.01),(0.2,0.145),(0.5,0.475),(0.8,0.815),(1,0.95)]),
        op_hsl({"red":(0,1.15,-0.03),"orange":(1,0.95,0.0),"yellow":(-3,1.0,0.0),
                "green":(-10,0.85,-0.05),"aqua":(-4,1.0,-0.05),"blue":(-5,1.15,-0.10),"purple":(0,0.9,-0.03),"magenta":(4,0.8,0.0)}),
        op_split_tone(shadow=(0.0,0.0,0.008),highlight=(0.02,0.012,-0.02)),
        op_saturation(1.05))),
    # Portrait: VSCO style
    ("vsco_faded_warm", "Faded Warm", "Vintage", "Lifted blacks with warm highlights, soft matte finish.", lambda: compose(
        op_fade(0.07),
        op_curve([(0, 0.0), (0.5, 0.52), (1, 0.96)]),
        op_split_tone(shadow=(0.005, 0.0, 0.015), highlight=(0.04, 0.02, -0.03)),
        op_white_balance(temp=0.1),
        op_saturation(0.88))),
    ("seoul_soft", "Seoul Soft", "Korea", "Soft creamy skin, olive-yellow greens, gentle matte blacks, warm daylight.", lambda: compose(
        op_white_balance(temp=0.13, tint=0.015),
        op_curve_rgb([(0, 0.045), (0.25, 0.26), (0.5, 0.525), (0.75, 0.79), (1, 0.98)],
                     [(0, 0.04), (0.25, 0.25), (0.5, 0.51), (0.75, 0.775), (1, 0.97)],
                     [(0, 0.05), (0.25, 0.235), (0.5, 0.47), (0.75, 0.715), (1, 0.93)]),
        op_hsl({"red": (-1, 1.1, 0.04), "orange": (-3, 1.3, 0.08), "yellow": (-6, 1.0, 0.04),
                "green": (-12, 0.78, 0.0), "aqua": (-4, 0.8, -0.02),
                "blue": (6, 0.85, -0.03), "magenta": (0, 0.85, 0.0)}),
        op_split_tone(shadow=(0.005, 0.0, -0.002), highlight=(0.02, 0.014, -0.014)),
        op_saturation(0.97))),
    ("airy_cream", "Airy Cream", "Korea", "Bright airy exposure, cream whites, teal-green glass, soft film matte.", lambda: compose(
        op_white_balance(temp=0.11),
        op_curve_rgb([(0, 0.04), (0.25, 0.27), (0.5, 0.575), (0.75, 0.83), (1, 0.985)],
                     [(0, 0.04), (0.25, 0.265), (0.5, 0.56), (0.75, 0.81), (1, 0.97)],
                     [(0, 0.055), (0.25, 0.25), (0.5, 0.49), (0.75, 0.73), (1, 0.925)]),
        op_hsl({"red": (-1, 1.1, 0.03), "orange": (-7, 1.3, 0.07), "yellow": (-8, 0.95, 0.03),
                "green": (6, 0.85, 0.0), "aqua": (-4, 1.1, -0.04),
                "blue": (-8, 0.85, -0.03), "purple": (-8, 0.8, 0.0)}),
        op_split_tone(shadow=(-0.01, 0.01, 0.015), highlight=(0.018, 0.014, -0.02)),
        op_saturation(0.95))),
    ("pastel_sea", "Pastel Sea", "Korea", "Cool teal sea tone, peach pastels, lifted teal blacks, quiet and airy.", lambda: compose(
        op_white_balance(temp=-0.12, tint=-0.08),
        op_curve_rgb([(0, 0.035), (0.25, 0.235), (0.5, 0.5), (0.75, 0.765), (1, 0.975)],
                     [(0, 0.055), (0.25, 0.27), (0.5, 0.52), (0.75, 0.775), (1, 0.975)],
                     [(0, 0.075), (0.25, 0.285), (0.5, 0.525), (0.75, 0.775), (1, 0.97)]),
        op_hsl({"red": (0, 1.2, 0.04), "orange": (-3, 3.5, 0.12), "yellow": (0, 1.3, 0.04),
                "green": (12, 0.68, -0.03), "aqua": (4, 0.9, -0.02),
                "blue": (-8, 0.82, -0.02), "magenta": (4, 0.8, 0.02)}),
        op_split_tone(shadow=(-0.025, 0.018, 0.02), highlight=(0.006, 0.006, 0.012)),
        op_saturation(0.9))),
    # Mood
    ("vsco_soft_film", "Soft Film", "Vintage", "Faded, low contrast, soft green tint.", lambda: compose(
        op_fade(0.08),
        op_curve([(0, 0.0), (0.5, 0.51), (1, 0.95)]),
        op_split_tone(shadow=(-0.01, 0.02, 0.0), highlight=(0.0, 0.015, 0.0)),
        op_saturation(0.82))),
    ("vsco_cool_fade", "Cool Fade", "Vintage", "Muted colour with cool blue shadows and a faded finish.", lambda: compose(
        op_fade(0.06),
        op_white_balance(temp=-0.18),
        op_split_tone(shadow=(-0.02, 0.0, 0.04), highlight=(0.005, 0.01, 0.015)),
        op_curve([(0, 0.0), (0.5, 0.5), (1, 0.96)]),
        op_saturation(0.78))),
    ("neon_night", "Neon Night", "Japan Pack", "Warm orange-red glow, brown-teal lifted blacks, creamy peach highlights.", lambda: compose(
        op_white_balance(temp=0.09, tint=0.02),
        op_curve_rgb([(0, 0.05), (0.25, 0.26), (0.5, 0.53), (0.75, 0.80), (1, 0.985)],
                     [(0, 0.035), (0.25, 0.225), (0.5, 0.47), (0.75, 0.73), (1, 0.95)],
                     [(0, 0.05), (0.25, 0.22), (0.5, 0.43), (0.75, 0.66), (1, 0.89)]),
        op_hsl({"red": (3, 1.1, 0.03), "orange": (-1, 1.0, 0.1), "yellow": (-4, 1.0, 0.03),
                "green": (-10, 0.7, -0.04), "aqua": (4, 0.9, -0.05),
                "blue": (6, 0.9, -0.05), "magenta": (-6, 1.0, 0.0)}),
        op_split_tone(shadow=(0.012, -0.004, 0.012), highlight=(0.03, 0.015, -0.025)),
        op_saturation(1.04))),
    ("tokyo_rain", "Tokyo Rain", "Japan Pack", "Cool teal-green wash, muted tones, soft contrast, warm accents stay alive.", lambda: compose(
        op_white_balance(temp=-0.06, tint=-0.1),
        op_curve_rgb([(0, 0.02), (0.25, 0.22), (0.5, 0.485), (0.75, 0.76), (1, 0.965)],
                     [(0, 0.045), (0.25, 0.26), (0.5, 0.515), (0.75, 0.775), (1, 0.98)],
                     [(0, 0.06), (0.25, 0.265), (0.5, 0.505), (0.75, 0.75), (1, 0.955)]),
        op_hsl({"red": (2, 1.05, 0.0), "orange": (-2, 1.0, 0.01), "yellow": (10, 0.75, -0.02),
                "green": (14, 0.8, -0.04), "aqua": (4, 0.95, -0.03),
                "blue": (-6, 0.85, -0.05), "magenta": (8, 0.8, 0.0)}),
        op_split_tone(shadow=(-0.018, 0.014, 0.014), highlight=(0.008, 0.012, 0.004)),
        op_saturation(0.9))),
    ("matte_meadow", "Matte Meadow", "Vintage", "Milky lifted blacks, soft yellow-green meadow, warm cream highlights, muted and airy.", lambda: compose(
        op_curve_rgb([(0, 0.10), (0.25, 0.33), (0.5, 0.60), (0.75, 0.84), (1, 0.98)],
                     [(0, 0.11), (0.25, 0.34), (0.5, 0.60), (0.75, 0.84), (1, 0.98)],
                     [(0, 0.12), (0.25, 0.33), (0.5, 0.60), (0.75, 0.84), (1, 0.98)]),
        op_hsl({"blue": (-1, 1.7, -0.05), "aqua": (2, 1.5, -0.03), "green": (14, 0.9, 0.02),
                "yellow": (0, 0.9, 0.10), "orange": (-2, 0.9, 0.05)}),
        op_split_tone(shadow=(-0.01, 0.0, 0.03), highlight=(0.012, 0.008, -0.01)),
        op_saturation(0.95))),
    ("sunlit_ruins", "Sunlit Ruins", "Vintage", "Deep cyan-blue sky, muted olive greens, cool blue-tinted shadows and a contrasty film finish.", lambda: compose(
        op_curve_rgb([(0, 0.045), (0.25, 0.21), (0.5, 0.45), (0.75, 0.71), (1, 0.95)],
                     [(0, 0.045), (0.25, 0.22), (0.5, 0.46), (0.75, 0.72), (1, 0.95)],
                     [(0, 0.055), (0.25, 0.23), (0.5, 0.46), (0.75, 0.70), (1, 0.92)]),
        op_hsl({"blue": (-3, 1.8, -0.2), "aqua": (0, 1.4, -0.12), "green": (10, 0.5, 0.02),
                "yellow": (6, 0.65, 0.07), "orange": (0, 0.9, 0.0)}),
        op_split_tone(shadow=(-0.003, -0.006, 0.02), highlight=(0.02, 0.01, -0.01)),
        op_saturation(1.0))),
    ("clear_alpine", "Clear Alpine", "Vintage", "Clean deep-blue sky, muted olive greens, warm earth tones and rich blacks with natural contrast.", lambda: compose(
        op_curve_rgb([(0, 0.0), (0.25, 0.20), (0.5, 0.48), (0.75, 0.77), (1, 0.99)],
                     [(0, 0.0), (0.25, 0.21), (0.5, 0.48), (0.75, 0.77), (1, 0.99)],
                     [(0, 0.01), (0.25, 0.22), (0.5, 0.48), (0.75, 0.76), (1, 0.98)]),
        op_hsl({"blue": (-3, 1.45, -0.10), "aqua": (-2, 1.35, -0.07), "green": (10, 0.45, 0.0),
                "yellow": (2, 0.65, -0.02), "orange": (-1, 0.9, -0.02)}),
        op_split_tone(shadow=(-0.01, 0.0, 0.02), highlight=(0.01, 0.0, -0.005)),
        op_saturation(0.97))),
    # Leica- and Ricoh-style looks
    ("leica_standard", "Leica Standard", "Leica", "Rich contrast with deep blacks, slightly warm neutrals, deep reds and restrained greens and blues.", lambda: compose(
        op_curve_rgb([(0,0.0),(0.2,0.165),(0.5,0.50),(0.8,0.835),(1,0.985)],
        [(0,0.0),(0.2,0.165),(0.5,0.495),(0.8,0.83),(1,0.98)],
        [(0,0.005),(0.2,0.165),(0.5,0.485),(0.8,0.815),(1,0.97)]),
        op_hsl({"red":(0,1.1,-0.03),"orange":(0,1.0,0.0),"yellow":(-3,0.9,-0.02),
        "green":(-5,0.8,-0.06),"aqua":(-2,0.85,-0.05),"blue":(-3,0.95,-0.07),"purple":(0,0.85,-0.03),"magenta":(0,0.9,-0.02)}),
        op_split_tone(shadow=(-0.006,0.002,0.01),highlight=(0.008,0.004,-0.006)),
        op_saturation(0.98))),
    ("leica_vivid", "Leica Vivid", "Leica", "Punchier Leica rendering: stronger reds and blues, crisp contrast and deep blacks.", lambda: compose(
        op_curve_rgb([(0,0.0),(0.2,0.15),(0.5,0.50),(0.8,0.85),(1,0.99)],
        [(0,0.0),(0.2,0.15),(0.5,0.495),(0.8,0.845),(1,0.985)],
        [(0,0.005),(0.2,0.15),(0.5,0.485),(0.8,0.83),(1,0.975)]),
        op_hsl({"red":(-1,1.25,-0.04),"orange":(0,1.05,0.0),"yellow":(-3,1.0,-0.02),
        "green":(-3,1.0,-0.07),"aqua":(-2,1.1,-0.06),"blue":(-3,1.2,-0.1),"purple":(0,1.0,-0.04),"magenta":(0,1.0,-0.02)}),
        op_split_tone(shadow=(-0.006,0.002,0.012),highlight=(0.008,0.004,-0.006)),
        op_saturation(1.08))),
    ("leica_natural", "Leica Natural", "Leica", "Gentle contrast, softly lifted blacks and understated colour, close to what the eye saw.", lambda: compose(
        op_curve_rgb([(0,0.015),(0.25,0.245),(0.5,0.50),(0.75,0.765),(1,0.98)],
        [(0,0.015),(0.25,0.245),(0.5,0.50),(0.75,0.765),(1,0.975)],
        [(0,0.02),(0.25,0.245),(0.5,0.495),(0.75,0.755),(1,0.97)]),
        op_hsl({"red":(0,0.95,-0.01),"orange":(0,0.95,0.01),"yellow":(-2,0.85,0.0),
        "green":(-3,0.75,-0.03),"aqua":(-2,0.8,-0.03),"blue":(-2,0.85,-0.03),"magenta":(0,0.85,0.0)}),
        op_split_tone(highlight=(0.004,0.002,-0.003)),
        op_saturation(0.92))),
    ("leica_mono", "Leica Mono", "Leica", "Fine-tonality monochrome with deep blacks, crisp midtone contrast and clean highlights.", lambda: compose(
        op_channel_mix((0.36,0.52,0.12)),
        op_curve([(0,0.0),(0.15,0.09),(0.5,0.50),(0.85,0.915),(1,0.99)]))),
    ("ricoh_positive_film", "Ricoh Positive Film", "Ricoh", "Vivid slide-film colour with firm contrast, deep blacks and saturated blues and greens.", lambda: compose(
        op_curve([(0,0.0),(0.2,0.145),(0.5,0.49),(0.8,0.86),(1,0.99)]),
        op_hsl({"red":(0,1.15,-0.02),"orange":(0,0.98,0.0),"yellow":(-2,1.05,0.0),
        "green":(2,1.25,-0.05),"aqua":(0,1.2,-0.04),"blue":(0,1.3,-0.07),"purple":(0,1.1,-0.03),"magenta":(0,1.1,0.0)}),
        op_split_tone(shadow=(-0.008,0.0,0.014)),
        op_saturation(1.08))),
    ("ricoh_negative_film", "Ricoh Negative Film", "Ricoh", "Soft negative look: gentle contrast, slightly lifted blacks, green-tinged shadows and relaxed colour.", lambda: compose(
        op_curve_rgb([(0,0.02),(0.25,0.24),(0.5,0.505),(0.75,0.78),(1,0.98)],
        [(0,0.025),(0.25,0.25),(0.5,0.51),(0.75,0.78),(1,0.975)],
        [(0,0.035),(0.25,0.25),(0.5,0.495),(0.75,0.75),(1,0.955)]),
        op_hsl({"orange":(0,1.0,0.03),"yellow":(-3,0.9,0.0),"green":(3,0.8,-0.02),"aqua":(-3,0.85,-0.02),"blue":(-3,0.85,-0.03)}),
        op_split_tone(shadow=(-0.006,0.01,0.006),highlight=(0.012,0.006,-0.008)),
        op_saturation(0.9))),
    ("ricoh_bleach_bypass", "Ricoh Bleach Bypass", "Ricoh", "Desaturated, gritty and contrasty with cool-neutral shadows.", lambda: compose(
        op_curve([(0,0.0),(0.2,0.12),(0.5,0.48),(0.8,0.88),(1,1.0)]),
        op_saturation(0.5),
        op_split_tone(shadow=(-0.006,0.0,0.012),highlight=(0.006,0.004,0.0)))),
    ("ricoh_cross_process", "Ricoh Cross Process", "Ricoh", "Cross-processed shift: yellow-green highlights, blue shadows and punchy unpredictable colour.", lambda: compose(
        op_curve_rgb([(0,0.0),(0.25,0.17),(0.5,0.52),(0.75,0.88),(1,1.0)],
        [(0,0.0),(0.25,0.24),(0.5,0.52),(0.75,0.79),(1,0.98)],
        [(0,0.10),(0.25,0.32),(0.5,0.46),(0.75,0.6),(1,0.86)]),
        op_saturation(1.12))),
    ("ricoh_hicon_bw", "Ricoh Hi-Contrast B&W", "Ricoh", "Hard black and white with crushed blacks and bright whites for street photography.", lambda: compose(
        op_channel_mix((0.34,0.50,0.16)),
        op_curve([(0,0.0),(0.2,0.08),(0.5,0.5),(0.8,0.93),(1,1.0)]))),
    # B&W
    ("bw_tri_x", "Tri-X Mono", "Vintage", "High-contrast mono with deep blacks, ready for grain.", lambda: compose(
        op_channel_mix((0.3, 0.55, 0.15)),
        op_curve([(0, 0.0), (0.2, 0.12), (0.5, 0.5), (0.8, 0.9), (1, 1.0)]))),
    ("bw_noir", "Noir Mono", "Vintage", "Dark, moody mono with crushed shadows and bright highlights.", lambda: compose(
        op_channel_mix((0.4, 0.45, 0.15)),
        op_curve([(0, 0.0), (0.25, 0.1), (0.5, 0.42), (0.75, 0.82), (1, 1.0)]))),
    ("cyan_sun", "Cyan Sun", "Vintage", "Deep cyan-blue skies, hot orange roofs, creamy whites, warm reddish shadows, bright and slightly cool mids.", lambda: compose(
        op_white_balance(temp=0.12, tint=-0.03),
        op_curve_rgb([(0, 0.02), (0.25, 0.27), (0.5, 0.59), (0.75, 0.80), (1, 0.985)],
                     [(0, 0.04), (0.25, 0.245), (0.5, 0.54), (0.75, 0.79), (1, 0.985)],
                     [(0, 0.0), (0.25, 0.345), (0.5, 0.70), (0.75, 0.78), (1, 0.985)]),
        op_hsl({"red": (6, 0.95, -0.04), "orange": (0, 1.4, 0.03), "yellow": (-3, 0.8, -0.02),
                "green": (-24, 0.6, -0.06), "aqua": (-14, 1.9, -0.18),
                "blue": (-15, 1.45, -0.11), "purple": (-33, 0.5, 0.18)}),
        op_split_tone(shadow=(0.075, 0.032, 0.01), highlight=(0.05, 0.04, -0.035)),
        op_saturation(1.2))),
    ("turquoise_cream", "Turquoise Cream", "Vintage", "Warm cream whites, turquoise blues, hot orange-red, lifted mauve-grey blacks, bright and punchy.", lambda: compose(
        op_white_balance(temp=0.03, tint=0.02),
        op_curve_rgb([(0, 0.05), (0.25, 0.26), (0.5, 0.54), (0.75, 0.80), (1, 0.985)],
                     [(0, 0.035), (0.25, 0.235), (0.5, 0.50), (0.75, 0.77), (1, 0.97)],
                     [(0, 0.045), (0.25, 0.245), (0.5, 0.52), (0.75, 0.75), (1, 0.93)]),
        op_hsl({"red": (4, 1.1, -0.03), "orange": (0, 1.12, 0.0), "yellow": (-4, 0.9, 0.0),
                "green": (-20, 0.75, -0.05), "aqua": (-12, 1.6, -0.06), "blue": (-14, 1.4, 0.0),
                "purple": (-20, 0.7, 0.05), "magenta": (0, 0.9, 0.0)}),
        op_split_tone(shadow=(0.02, -0.004, 0.012), highlight=(0.02, 0.014, -0.02)),
        op_saturation(1.08))),
    ("teal_sea", "Teal Sea", "Vintage", "Bright airy teal seas, pale lavender-white skies, jade green land, lifted milky highlights.", lambda: compose(
        op_white_balance(temp=-0.09, tint=-0.101),
        op_curve_rgb([(0, 0.032), (0.25, 0.204), (0.5, 0.553), (0.75, 0.749), (1, 0.985)],
                     [(0, 0.0), (0.25, 0.273), (0.5, 0.676), (0.75, 0.685), (1, 0.985)],
                     [(0, 0.0), (0.25, 0.223), (0.5, 0.629), (0.75, 0.851), (1, 0.985)]),
        op_hsl({"red": (0.0, 1.0, 0.0), "orange": (0.0, 1.0, 0.0), "yellow": (-3.375, 1.0, 0.045), "green": (-5.625, 0.588, 0.263), "aqua": (9.0, 0.7, 0.15), "blue": (-3.375, 0.962, 0.203), "purple": (0.0, 1.0, 0.0)}),
        op_split_tone(shadow=(0.011, -0.007, -0.004), highlight=(0.086, 0.034, 0.049)),
        op_saturation(1.0))),
    ("street_green", "Street Green", "Vintage", "Deep teal-emerald foliage, cool neutral whites, soft muted city tones.", lambda: compose(
        op_white_balance(temp=-0.045, tint=-0.022),
        op_curve_rgb([(0, 0.0), (0.25, 0.19), (0.5, 0.477), (0.75, 0.798), (1, 0.985)],
                     [(0, 0.022), (0.25, 0.254), (0.5, 0.518), (0.75, 0.772), (1, 0.985)],
                     [(0, 0.007), (0.25, 0.261), (0.5, 0.511), (0.75, 0.766), (1, 0.985)]),
        op_hsl({"red": (1.125, 0.925, 0.0), "orange": (1.125, 0.925, 0.12), "yellow": (0.0, 0.925, 0.052), "green": (10.125, 1.075, -0.007), "aqua": (-6.75, 0.587, 0.09), "blue": (0.0, 1.0, -0.052), "purple": (0.0, 1.0, 0.0)}),
        op_split_tone(shadow=(0.011, 0.007, -0.004), highlight=(-0.004, 0.004, 0.011)),
        op_saturation(0.955))),
    ("park_gold", "Park Gold", "Vintage", "Sunlit golden-yellow foliage, creamy warm highlights, lifted soft blacks.", lambda: compose(
        op_white_balance(temp=0.141, tint=-0.023),
        op_curve_rgb([(0, 0.0), (0.25, 0.34), (0.5, 0.573), (0.75, 0.751), (1, 0.985)],
                     [(0, 0.0), (0.25, 0.307), (0.5, 0.544), (0.75, 0.775), (1, 0.985)],
                     [(0, 0.005), (0.25, 0.23), (0.5, 0.5), (0.75, 0.736), (1, 0.985)]),
        op_hsl({"red": (1.125, 1.038, 0.015), "orange": (10.125, 1.0, 0.12), "yellow": (-10, 1.15, 0.075), "green": (-1, 1.25, 0.09), "aqua": (0.0, 1.0, 0.0), "blue": (0.0, 1.0, 0.007), "purple": (0.0, 1.0, 0.0)}),
        op_split_tone(shadow=(0.004, 0.019, 0.004), highlight=(0.042, 0.007, -0.024)),
        op_saturation(0.82))),
    ("dusk_gold", "Golden Dusk", "Vintage", "Golden-hour haze with orange-amber midtones, cool blue shadows, creamy highlights, rich olive greens.", lambda: compose(
        op_white_balance(temp=0.15, tint=-0.4),
        op_curve_rgb([(0, 0.012), (0.25, 0.38), (0.5, 0.65), (0.75, 0.9), (1, 0.985)],
                     [(0, 0.012), (0.25, 0.35), (0.5, 0.64), (0.75, 0.87), (1, 0.985)],
                     [(0, 0.0), (0.25, 0.37), (0.5, 0.58), (0.75, 0.8), (1, 0.985)]),
        op_mid_tone((0.04, 0.01, -0.05)),
        op_hsl({"red": (2, 1.25, 0.03), "orange": (8, 1.25, 0.05), "yellow": (2, 0.95, 0.03), "green": (-8, 1.15, -0.13), "aqua": (-10, 0.9, 0.0)}),
        op_split_tone(shadow=(-0.004, -0.018, 0.022), highlight=(0.03, 0.02, -0.015)),
        op_saturation(0.84))),
    ("surf_teal", "Teal Surf", "Vintage", "Deep teal seawater, creamy white foam, warm olive-amber rocks, matte soft blacks.", lambda: compose(
        op_white_balance(temp=0.0, tint=-0.011),
        op_curve_rgb([(0, 0.002), (0.25, 0.254), (0.5, 0.486), (0.75, 0.808), (1, 0.985)],
                     [(0, 0.0), (0.25, 0.233), (0.5, 0.543), (0.75, 0.797), (1, 0.985)],
                     [(0, 0.009), (0.25, 0.231), (0.5, 0.547), (0.75, 0.761), (1, 0.985)]),
        op_hsl({"red": (1.125, 0.963, 0.052), "orange": (8.0, 1.1, -0.03), "yellow": (-2.0, 1.2, -0.04), "green": (10.0, 1.4, -0.03), "aqua": (14, 1.7, 0.06), "blue": (0.0, 1.0, 0.0), "purple": (0.0, 1.0, 0.0)}),
        op_split_tone(shadow=(-0.011, 0.007, 0.004), highlight=(0.0, -0.004, 0.046)),
        op_saturation(1.1))),
    ("bw_soft_mono", "Soft Mono", "Vintage", "Gentle mono with lifted shadows and soft highlights.", lambda: compose(
        op_channel_mix((0.33, 0.5, 0.17)),
        op_fade(0.06),
        op_curve([(0, 0.06), (0.5, 0.52), (1, 0.97)]))),
]

GROUP_ORDER = ["Fuji", "Kodak", "Leica", "Ricoh", "Korea", "Japan Pack", "Vintage"]
ORIGINAL = {"id": "original", "name": "Original", "group": "Kodak", "lut": None}


def load_pack_entries():
    """Catalog entries written by import_cube_pack.py. Their PNGs come from that importer, so
    main() keeps them and counts their size instead of regenerating them."""
    if not os.path.exists(PACK_JSON):
        return []
    with open(PACK_JSON, encoding="utf-8") as f:
        return json.load(f)["filters"]

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


# ---------------------------------------------------------------------------
# Look tags: how a LUT grades the photo, read back from its PNG. The app's chips use these.
# Thresholds were set from the distribution of the shipped LUTs (see the tag summary in main()).
# ---------------------------------------------------------------------------

TAG_ORDER = ["warm", "cool", "soft", "bold", "mono"]
TAG_WARM_CUT = 8        # mean (R - B) of the grey samples, in 0..255
TAG_SOFT_MAX = 70.0     # mean distance from identity, in 0..441; at or below is soft
TAG_BOLD_MIN = 99.0     # at or above is bold
TAG_MONO_MAX_SAT = 0.1  # mean HSV saturation of the output; below this, and untinted, is mono


def lut_tags(png_bytes):
    """Tags for one LUT PNG. A look can be warm or cool, soft or bold, and mono stands alone."""
    _, _, pixels = decode_png_rgb(png_bytes)
    grey_warmth = []
    for k in (16, 32, 47):
        off = lut_offset(k, k, k)
        grey_warmth.append(pixels[off] - pixels[off + 2])
    warmth = sum(grey_warmth) / len(grey_warmth)

    levels = [0, 8, 16, 24, 32, 40, 48, 56, 63]
    distances, saturations = [], []
    for r in levels:
        for g in levels:
            for b in levels:
                off = lut_offset(r, g, b)
                out = (pixels[off], pixels[off + 1], pixels[off + 2])
                src = (r * 255 / 63, g * 255 / 63, b * 255 / 63)
                distances.append(math.dist(src, out))
                hi, lo = max(out), min(out)
                saturations.append(0.0 if hi == 0 else (hi - lo) / hi)
    distance = sum(distances) / len(distances)
    saturation = sum(saturations) / len(saturations)

    if saturation < TAG_MONO_MAX_SAT and abs(warmth) < TAG_WARM_CUT:
        return ["mono"]
    tags = []
    if warmth >= TAG_WARM_CUT:
        tags.append("warm")
    elif warmth <= -TAG_WARM_CUT:
        tags.append("cool")
    if distance <= TAG_SOFT_MAX:
        tags.append("soft")
    elif distance >= TAG_BOLD_MIN:
        tags.append("bold")
    return tags


# Difficulty: how far a LUT moves a photo from the original, so how hard it is to use well.
# Each term is measured per LUT, then turned into a percentile among all shipped LUTs, and the
# difficulty is the mean of those percentiles (0 = easiest, 1 = hardest). Cameras list their looks
# easiest first, so the default look of each camera is the gentlest one it offers.
# The chroma term uses absolute max-min change, not a ratio: a ratio is near zero on the dark and
# grey samples that make up most of the grid, so it would rate a strong saturation boost as mild.
DIFFICULTY_TERMS = ("distance", "warmth", "luma", "chroma")


def lut_difficulty_terms(png_bytes):
    """Raw difficulty terms for one LUT PNG: mean displacement, grey warmth, luma and chroma change."""
    _, _, pixels = decode_png_rgb(png_bytes)
    grey_warmth = []
    for k in (16, 32, 47):
        off = lut_offset(k, k, k)
        grey_warmth.append(abs(pixels[off] - pixels[off + 2]))

    levels = [0, 8, 16, 24, 32, 40, 48, 56, 63]
    distances, luma_shifts, chroma_shifts = [], [], []
    for r in levels:
        for g in levels:
            for b in levels:
                off = lut_offset(r, g, b)
                out = (pixels[off], pixels[off + 1], pixels[off + 2])
                src = (r * 255 / 63, g * 255 / 63, b * 255 / 63)
                distances.append(math.dist(src, out))
                luma_shifts.append(abs(luma709(*out) - luma709(*src)))
                chroma_shifts.append(abs((max(out) - min(out)) - (max(src) - min(src))))
    return {
        "distance": sum(distances) / len(distances),
        "warmth": sum(grey_warmth) / len(grey_warmth),
        "luma": sum(luma_shifts) / len(luma_shifts),
        "chroma": sum(chroma_shifts) / len(chroma_shifts),
    }


def assign_difficulty(terms_by_id):
    """Maps each id to its difficulty in 0..1 from the mean percentile of its terms."""
    ids = list(terms_by_id)
    percentile = {fid: [] for fid in ids}
    for term in DIFFICULTY_TERMS:
        values = sorted(terms_by_id[fid][term] for fid in ids)
        denom = max(len(values) - 1, 1)
        for fid in ids:
            # Mean rank among ties, so equal LUTs get equal difficulty.
            v = terms_by_id[fid][term]
            below = sum(1 for x in values if x < v)
            equal = sum(1 for x in values if x == v)
            percentile[fid].append((below + (equal - 1) / 2) / denom)
    return {fid: round(sum(p) / len(p), 3) for fid, p in percentile.items()}


def main():
    all_entries = [ORIGINAL]
    builders = []
    for fid, name, group, _desc, make in FILTERS:
        all_entries.append({"id": fid, "name": name, "group": group, "lut": f"luts/{fid}.png"})
        builders.append((fid, make()))
    pack = load_pack_entries()
    all_entries += pack

    ids = [e["id"] for e in all_entries]
    assert len(ids) == len(set(ids)), "duplicate filter ids"
    for e in all_entries:
        assert e["id"] == e["id"].lower() and " " not in e["id"], "ids must be lowercase snake_case"
        assert e["group"] in GROUP_ORDER, f"unknown group {e['group']}"

    identity_png = encode_png(SIZE, SIZE, build_lut(lambda r, g, b: (r, g, b)))
    self_check(identity_png)

    os.makedirs(LUT_DIR, exist_ok=True)
    expected_files = {f"{fid}.png" for fid, _ in builders}
    expected_files |= {os.path.basename(e["lut"]) for e in pack}
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
    for e in pack:
        total_bytes += os.path.getsize(os.path.join(LUT_DIR, os.path.basename(e["lut"])))

    terms_by_id = {}
    for e in all_entries:
        if e["lut"]:
            with open(os.path.join(ASSET_DIR, e["lut"]), "rb") as f:
                png = f.read()
            e["tags"] = lut_tags(png)
            terms_by_id[e["id"]] = lut_difficulty_terms(png)
        else:
            e["tags"] = []
    difficulty = assign_difficulty(terms_by_id)
    for e in all_entries:
        e["difficulty"] = difficulty.get(e["id"], 0.0)

    catalog = {"version": 1, "filters": all_entries}
    with open(CATALOG_PATH, "w", encoding="utf-8") as f:
        f.write(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n")

    print()
    print("Summary")
    for group in GROUP_ORDER:
        members = [e for e in all_entries if e["group"] == group]
        print(f"  {group:<9} {len(members):>3} filters")
    print(f"  total     {len(all_entries):>3} entries, {len(builders)} builder LUT files")
    for tag in TAG_ORDER:
        print(f"  tag {tag:<5} {sum(1 for e in all_entries if tag in e['tags']):>3} entries")
    print(f"  LUT PNG total size: {total_bytes / 1024:.1f} KB")
    if total_bytes >= MAX_TOTAL_BYTES:
        sys.exit(f"LUT assets exceed {MAX_TOTAL_BYTES} bytes")


if __name__ == "__main__":
    main()
