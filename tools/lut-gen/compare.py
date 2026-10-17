#!/usr/bin/env python3
"""Builds "original | filtered | reference" sheets for each look.

Usage (repo root):

    python3 -I tools/lut-gen/compare.py <images_dir> <out_dir>

Needs Pillow. Each row: the original photo, the same photo through the look, the reference image.
Looks and their test/reference images are listed in PAIRS below.
"""

import importlib.util
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROW_H = 360

# look id -> (original, reference, optional reference crop box)
PAIRS = {
    "fuji_classic_neg": ("14.webp", "10.webp", (0, 20, 1545, 520)),
    "fuji_forest": ("13.webp", "4.webp", None),
    "kodak_colorplus": ("14.webp", "5.png", None),
    "neon_night": ("18.webp", "3.png", None),
    "tokyo_rain": ("14.webp", "2.webp", None),
    "seoul_soft": ("18.webp", "15.png", None),
    "airy_cream": ("18.webp", "17.png", None),
    "pastel_sea": ("11.webp", "16.png", None),
}


def load_generator():
    spec = importlib.util.spec_from_file_location("g", os.path.join(HERE, "generate_luts.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def fit(im, h=ROW_H):
    return im.resize((max(1, round(im.width * h / im.height)), h), Image.LANCZOS)


def apply(g, fn, im):
    out = im.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, gg, b = px[x, y]
            px[x, y] = tuple(g.to_byte(v) for v in fn(r / 255, gg / 255, b / 255))
    return out


def main(images_dir, out_dir):
    g = load_generator()
    fns = {f[0]: f[5]() for f in g.FILTERS}
    names = {f[0]: f[1] for f in g.FILTERS}
    os.makedirs(out_dir, exist_ok=True)
    for fid, (orig, ref, box) in PAIRS.items():
        src = fit(Image.open(os.path.join(images_dir, orig)).convert("RGB"), 300)
        ref_im = Image.open(os.path.join(images_dir, ref)).convert("RGB")
        if box:
            ref_im = ref_im.crop(box)
        cols = [src, apply(g, fns[fid], src), fit(ref_im, 300)]
        labels = ["Original", names[fid], "Reference"]
        sheet = Image.new("RGB", (sum(c.width for c in cols) + 12 * 4, 300 + 28 + 8), "white")
        d = ImageDraw.Draw(sheet)
        x = 12
        for c, lab in zip(cols, labels):
            d.text((x, 8), lab, fill="black")
            sheet.paste(c, (x, 28))
            x += c.width + 12
        sheet.save(os.path.join(out_dir, f"{fid}_compare.png"))
        print("wrote", fid)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
