#!/usr/bin/env python3
"""Shrinks every shipped 64-level LUT PNG (512x512) to a 32-level one (256x128), in place.

Usage (from the repository root):

    python3 -I tools/lut-gen/shrink_luts.py --check   # report error only, write nothing
    python3 -I tools/lut-gen/shrink_luts.py           # rewrite luts/*.png

Each 32-level node is the trilinear interpolation of the 64-level LUT at the same position
in the cube, so the look is preserved and the renderer (LutLayout.LEVELS_32) samples it the
same way. The error reported is the mean and max channel difference (0..255) between the
64-level and 32-level LUTs, measured at a 9x9x9 set of positions across the cube.

Standard library only. Layouts and PNG codec come from generate_luts.py, loaded by path.
"""

import importlib.util
import json
import multiprocessing
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location("generate_luts", os.path.join(HERE, "generate_luts.py"))
gl = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(gl)

OUT_LEVELS = 32
OUT_TILES_PER_ROW = 8
OUT_W = OUT_TILES_PER_ROW * OUT_LEVELS  # 256
OUT_H = (OUT_LEVELS // OUT_TILES_PER_ROW) * OUT_LEVELS  # 4 tile rows of 32 = 128
SRC_MAX = gl.LEVELS - 1  # 63


def out_offset(r, g, b):
    col = b % OUT_TILES_PER_ROW
    row = b // OUT_TILES_PER_ROW
    x = col * OUT_LEVELS + r
    y = row * OUT_LEVELS + g
    return (y * OUT_W + x) * 3


def sample_src(px, x, y, z):
    """Trilinear sample of the 64-level LUT at lattice position (x, y, z) in 0..63."""
    x0, y0, z0 = int(x), int(y), int(z)
    x1, y1, z1 = min(x0 + 1, SRC_MAX), min(y0 + 1, SRC_MAX), min(z0 + 1, SRC_MAX)
    fx, fy, fz = x - x0, y - y0, z - z0
    out = [0.0, 0.0, 0.0]
    for zi, wz in ((z0, 1 - fz), (z1, fz)):
        for yi, wy in ((y0, 1 - fy), (y1, fy)):
            for xi, wx in ((x0, 1 - fx), (x1, fx)):
                w = wx * wy * wz
                if w:
                    off = gl.lut_offset(xi, yi, zi)
                    out[0] += px[off] * w
                    out[1] += px[off + 1] * w
                    out[2] += px[off + 2] * w
    return out


def sample_out(px32, x, y, z):
    """Trilinear sample of the 32-level LUT at lattice position (x, y, z) in 0..31."""
    x0, y0, z0 = int(x), int(y), int(z)
    x1, y1, z1 = min(x0 + 1, OUT_LEVELS - 1), min(y0 + 1, OUT_LEVELS - 1), min(z0 + 1, OUT_LEVELS - 1)
    fx, fy, fz = x - x0, y - y0, z - z0
    out = [0.0, 0.0, 0.0]
    for zi, wz in ((z0, 1 - fz), (z1, fz)):
        for yi, wy in ((y0, 1 - fy), (y1, fy)):
            for xi, wx in ((x0, 1 - fx), (x1, fx)):
                w = wx * wy * wz
                if w:
                    off = out_offset(xi, yi, zi)
                    out[0] += px32[off] * w
                    out[1] += px32[off + 1] * w
                    out[2] += px32[off + 2] * w
    return out


def convert(px):
    """Returns the 256x128 RGB buffer for the 32-level LUT resampled from the 64-level pixels."""
    buf = bytearray(OUT_W * OUT_H * 3)
    scale = SRC_MAX / (OUT_LEVELS - 1)
    for b in range(OUT_LEVELS):
        for g in range(OUT_LEVELS):
            for r in range(OUT_LEVELS):
                v = sample_src(px, r * scale, g * scale, b * scale)
                off = out_offset(r, g, b)
                for c in range(3):
                    buf[off + c] = min(255, max(0, int(v[c] + 0.5)))
    return bytes(buf)


def error(px, px32):
    """Mean and max channel difference between the 64-level and 32-level LUTs over a 9^3 probe grid."""
    total, count, worst = 0.0, 0, 0.0
    ratio = (OUT_LEVELS - 1) / SRC_MAX
    for i in range(9):
        for j in range(9):
            for k in range(9):
                p = (i * SRC_MAX / 8, j * SRC_MAX / 8, k * SRC_MAX / 8)
                a = sample_src(px, *p)
                q = (p[0] * ratio, p[1] * ratio, p[2] * ratio)
                b = sample_out(px32, *q)
                for c in range(3):
                    d = abs(a[c] - b[c])
                    total += d
                    count += 1
                    worst = max(worst, d)
    return total / count, worst


def process(name):
    src_path = os.path.join(gl.LUT_DIR, name)
    with open(src_path, "rb") as f:
        w, h, px = gl.decode_png_rgb(f.read())
    assert (w, h) == (gl.SIZE, gl.SIZE), f"{name} is not 512x512"
    px32 = convert(px)
    mean_err, max_err = error(px, px32)
    return name, gl.encode_png(OUT_W, OUT_H, px32), mean_err, max_err


def lut_names():
    names = sorted(n for n in os.listdir(gl.LUT_DIR) if n.endswith(".png"))
    with open(os.path.join(gl.ASSET_DIR, "catalog.json")) as f:
        used = {os.path.basename(e["lut"]) for e in json.load(f)["filters"] if e.get("lut")}
    missing = used - set(names)
    assert not missing, f"catalog references missing LUTs: {sorted(missing)[:5]}"
    return names


def main():
    check = "--check" in sys.argv
    names = lut_names()
    before = sum(os.path.getsize(os.path.join(gl.LUT_DIR, n)) for n in names)
    with multiprocessing.Pool(os.cpu_count()) as pool:
        results = pool.map(process, names, chunksize=4)
    means = [m for _, _, m, _ in results]
    maxes = [x for _, _, _, x in results]
    print(f"{len(results)} LUTs. mean channel error {sum(means) / len(means):.3f}, "
          f"worst max {max(maxes):.1f} ({results[maxes.index(max(maxes))][0]})")
    if check:
        print(f"before {before / 1024 / 1024:.1f} MB; no files written")
        return
    after = 0
    for name, png, _, _ in results:
        path = os.path.join(gl.LUT_DIR, name)
        with open(path, "wb") as f:
            f.write(png)
        after += len(png)
    print(f"before {before / 1024 / 1024:.1f} MB, after {after / 1024 / 1024:.1f} MB")


if __name__ == "__main__":
    main()
