#!/usr/bin/env python3
"""Derives each Japan-pack camera's body colour from the LUTs it ships.

Run from the repository root to rewrite `modelTint` inside the BEGIN/END GENERATED block of
CameraProfiles.kt:

    python3 -I tools/lut-gen/camera_tints.py

The colour is the cast the camera's looks put on neutral mid greys: each LUT is applied to a ramp of greys,
the shift away from grey is averaged over the camera's looks, and the direction becomes the body hue
(strength becomes saturation). Neutral or black-and-white looks give a grey body. Also imported by
import_cube_pack.py. Standard library only.
"""

import colorsys
import os
import re
import struct
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
LUT_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "filters", "luts")
KOTLIN = os.path.join(
    REPO_ROOT, "app", "src", "main", "java", "com", "example", "photogether", "core", "render", "CameraProfiles.kt"
)
GRAYS = (14, 22, 32, 42, 50)  # LUT levels (0..63) of the grey ramp
CUBE = (6, 18, 30, 42, 54)    # levels sampled on each axis for the whole-cube shift
CAST_GAIN = 7.0               # LUT casts are subtle (a few percent); this makes them readable on a body
MIN_SAT, MAX_SAT = 0.10, 0.78
BODY_VALUE = 0.80


def _read_png_rgb(path):
    """Returns (width, height, rows) of an 8-bit non-interlaced RGB PNG; rows are bytearrays."""
    with open(path, "rb") as f:
        data = f.read()
    pos, idat, w = 8, b"", 0
    while pos < len(data):
        n, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        if kind == b"IHDR":
            w, h, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            assert depth == 8 and ctype == 2 and interlace == 0, f"unsupported PNG {path}"
        elif kind == b"IDAT":
            idat += body
        pos += 12 + n
    raw = zlib.decompress(idat)
    stride, bpp = w * 3, 3
    rows, prev = [], bytearray(stride)
    for y in range(h):
        ft = raw[y * (stride + 1)]
        cur = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for i in range(stride):
            a = cur[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            if ft == 1:
                cur[i] = (cur[i] + a) & 255
            elif ft == 2:
                cur[i] = (cur[i] + b) & 255
            elif ft == 3:
                cur[i] = (cur[i] + (a + b) // 2) & 255
            elif ft == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if pa <= pb and pa <= pc else (b if pb <= pc else c)
                cur[i] = (cur[i] + pr) & 255
        rows.append(cur)
        prev = cur
    return w, h, rows


def lut_cast(path):
    """Mean (r, g, b) shift in 0..1 the LUT puts on a grey ramp, averaged with its shift over the whole colour cube."""
    _, _, rows = _read_png_rgb(path)

    def shift(samples):
        total = [0.0, 0.0, 0.0]
        for rgb in samples:
            r, g, b = rgb
            x, y = (b % 8) * 64 + r, (b // 8) * 64 + g
            px = rows[y][x * 3:x * 3 + 3]
            for k in range(3):
                total[k] += px[k] / 255.0 - rgb[k] / 63.0
        return [t / len(samples) for t in total]

    grey = shift([(lv, lv, lv) for lv in GRAYS])
    cube = shift([(r, g, b) for r in CUBE for g in CUBE for b in CUBE])
    return [(a + c) / 2 for a, c in zip(grey, cube)]


def tint_for_ids(filter_ids):
    """ARGB body colour for a camera from the casts of its filters."""
    casts = [lut_cast(os.path.join(LUT_DIR, f"{fid}.png")) for fid in filter_ids]
    mean = [sum(c[k] for c in casts) / len(casts) for k in range(3)]
    # chroma direction of the cast: lift each channel above the cast's own mean, so the hue is what is left
    avg = sum(mean) / 3
    r, g, b = (0.5 + (m - avg) * CAST_GAIN for m in mean)
    r, g, b = (min(1.0, max(0.0, v)) for v in (r, g, b))
    h, s, _ = colorsys.rgb_to_hsv(r, g, b)
    s = min(MAX_SAT, max(MIN_SAT, s * 1.6))
    r, g, b = colorsys.hsv_to_rgb(h, s, BODY_VALUE)
    return 0xFF000000 | (round(r * 255) << 16) | (round(g * 255) << 8) | round(b * 255)


def main():
    with open(KOTLIN, encoding="utf-8") as f:
        src = f.read()
    a, b = src.index("// BEGIN GENERATED"), src.index("// END GENERATED")
    block = src[a:b]

    def per_camera(m):
        ids = re.findall(r'"(jp\d+_\d+)"', m.group("ids"))
        return f'{m.group("head")}0x{tint_for_ids(ids):08X}.toInt(),'

    block, n = re.subn(
        r'(?P<head>filterIds = listOf\((?P<ids>.*?)\),\s*(?:modelAsset = "[^"]*",\s*)?modelTint = )0x[0-9A-Fa-f]+\.toInt\(\),',
        per_camera, block, flags=re.S)
    with open(KOTLIN, "w", encoding="utf-8") as f:
        f.write(src[:a] + block + src[b:])
    print(f"retinted {n} cameras")


if __name__ == "__main__":
    sys.exit(main())
