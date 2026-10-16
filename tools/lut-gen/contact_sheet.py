#!/usr/bin/env python3
"""Renders every LUT over a set of test images and writes an HTML contact sheet.

Usage (from the repository root):

    python3 -I tools/lut-gen/contact_sheet.py \
        --luts app/src/main/assets/filters/luts --out <dir> [--images <dir>]
    options: --catalog <json> (default: catalog.json next to the luts folder)
             --max-side N     (downscale inputs so the longest side is N px; default 320)

--images: a folder of 8-bit RGB or RGBA PNGs. Non-PNG files are skipped with a warning.
Without --images, four synthetic 256x170 images are generated: a grey ramp, an RGB hue
sweep, a skin-tone patch set (light to dark), and a sky/foliage gradient.

Outputs in --out:
  index.html          contact sheet: near-duplicate table at the top, then rows = LUTs
                      (original first), columns = images, with group, premium flag and
                      flags for banding / non-monotonic grey response
  inputs/*.png        the input images used
  results/<id>__<image>.png  each LUT applied to each image

Method:
  * LUT lookup is the renderer's: input (r, g, b) * 63 gives the 64-level lattice,
    blue selects the slice (tile = b, col = b % 8, row = b // 8), each slice is
    bilinear in r and g, and the two slices around b are blended. Output is rounded to 8 bits.
  * Near-duplicates: mean CIE76 dE between two LUTs over a 17^3 grid of sRGB inputs
    (sRGB -> XYZ D65 -> Lab). Pairs with mean dE < 3 are listed.
  * Monotonic check: the grey ramp v -> (v, v, v) for v in 0..255. Output luminance is
    Rec.709 on the 8-bit output. A drop of more than 1.0 level between neighbouring steps
    is flagged "banding/non-monotonic".

Standard library only. A minimal PNG decoder (RGB/RGBA, 8-bit, non-interlaced, all five
filter types) is included. The PNG encoder writes filter type 0 rows for speed.
"""

import argparse
import colorsys
import html
import json
import math
import os
import re
import struct
import sys
import time
import zlib

LEVELS = 64
LUT_SIZE = 512
DUP_THRESHOLD = 3.0
MONO_TOLERANCE = 1.0
GRID_N = 17


# ---------------------------------------------------------------------------
# PNG decode / encode
# ---------------------------------------------------------------------------


def decode_png(data):
    """Returns (width, height, rgb_bytes). RGBA input has its alpha channel dropped."""
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG file")
    pos = 8
    idat = bytearray()
    width = height = ctype = None
    while pos + 8 <= len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if tag == b"IHDR":
            width, height, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            if depth != 8 or ctype not in (2, 6):
                raise ValueError(
                    f"unsupported PNG (bit depth {depth}, colour type {ctype}); need 8-bit RGB or RGBA"
                )
            if interlace != 0:
                raise ValueError("interlaced PNG is not supported")
        elif tag == b"IDAT":
            idat.extend(body)
        elif tag == b"IEND":
            break
    if width is None:
        raise ValueError("missing IHDR chunk")

    ch = 3 if ctype == 2 else 4
    raw = zlib.decompress(bytes(idat))
    stride = width * ch
    if len(raw) < height * (stride + 1):
        raise ValueError("truncated image data")
    out = bytearray(stride * height)
    for y in range(height):
        ft = raw[y * (stride + 1)]
        src = y * (stride + 1) + 1
        dst = y * stride
        if ft == 0:
            out[dst:dst + stride] = raw[src:src + stride]
            continue
        if ft not in (1, 2, 3, 4):
            raise ValueError(f"bad PNG filter type {ft}")
        prev = dst - stride
        for i in range(stride):
            x = raw[src + i]
            a = out[dst + i - ch] if i >= ch else 0
            if y == 0:
                b = 0
                c = 0
            else:
                b = out[prev + i]
                c = out[prev + i - ch] if i >= ch else 0
            if ft == 1:
                p = a
            elif ft == 2:
                p = b
            elif ft == 3:
                p = (a + b) >> 1
            else:
                pp = a + b - c
                pa = abs(pp - a)
                pb = abs(pp - b)
                pc = abs(pp - c)
                if pa <= pb and pa <= pc:
                    p = a
                elif pb <= pc:
                    p = b
                else:
                    p = c
            out[dst + i] = (x + p) & 255

    if ch == 3:
        rgb = bytes(out)
    else:
        n = width * height
        rgb = bytearray(n * 3)
        rgb[0::3] = out[0::4]
        rgb[1::3] = out[1::4]
        rgb[2::3] = out[2::4]
        rgb = bytes(rgb)
    return width, height, rgb


def encode_png_rgb(width, height, rgb):
    stride = width * 3
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        raw += rgb[y * stride:(y + 1) * stride]

    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", ihdr)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 6))
        + chunk(b"IEND", b"")
    )


# ---------------------------------------------------------------------------
# LUT lookup (same as the shader: blue slice tiles, bilinear in-slice, linear across slices)
# ---------------------------------------------------------------------------


def make_lut_sampler(rgb):
    """rgb: 512x512x3 LUT bytes. Returns f(r, g, b) with inputs 0..1 -> (R, G, B) floats 0..255."""
    d = rgb
    stride = LUT_SIZE * 3

    def sample(r, g, b):
        r = min(max(r, 0.0), 1.0) * 63.0
        g = min(max(g, 0.0), 1.0) * 63.0
        b = min(max(b, 0.0), 1.0) * 63.0
        x0 = min(int(r), 62)
        y0 = min(int(g), 62)
        z0 = min(int(b), 62)
        fx = r - x0
        fy = g - y0
        fz = b - z0
        fx1 = 1.0 - fx
        fy1 = 1.0 - fy
        acc0 = acc1 = acc2 = 0.0
        for k, wz in ((z0, 1.0 - fz), (z0 + 1, fz)):
            col = k & 7
            row = k >> 3
            o = (row * LEVELS + y0) * stride + (col * LEVELS + x0) * 3
            o10 = o + 3
            o01 = o + stride
            o11 = o01 + 3
            for c in range(3):
                top = d[o + c] * fx1 + d[o10 + c] * fx
                bot = d[o01 + c] * fx1 + d[o11 + c] * fx
                v = (top * fy1 + bot * fy) * wz
                if c == 0:
                    acc0 += v
                elif c == 1:
                    acc1 += v
                else:
                    acc2 += v
        return acc0, acc1, acc2

    return sample


def to_bytes3(v):
    return (int(v[0] + 0.5), int(v[1] + 0.5), int(v[2] + 0.5))


def apply_to_image(sample, width, height, rgb):
    """Applies the LUT to every pixel (8-bit output). Caches repeated input colours."""
    out = bytearray(len(rgb))
    cache = {}
    inv = 1.0 / 255.0
    for i in range(0, len(rgb), 3):
        key = (rgb[i], rgb[i + 1], rgb[i + 2])
        res = cache.get(key)
        if res is None:
            res = to_bytes3(sample(key[0] * inv, key[1] * inv, key[2] * inv))
            cache[key] = res
        out[i] = min(res[0], 255)
        out[i + 1] = min(res[1], 255)
        out[i + 2] = min(res[2], 255)
    return bytes(out)


# ---------------------------------------------------------------------------
# Colour science
# ---------------------------------------------------------------------------


def _srgb_to_linear(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def srgb_to_lab(r, g, b):
    rl = _srgb_to_linear(r)
    gl_ = _srgb_to_linear(g)
    bl = _srgb_to_linear(b)
    x = (0.4124564 * rl + 0.3575761 * gl_ + 0.1804375 * bl) / 0.95047
    y = 0.2126729 * rl + 0.7151522 * gl_ + 0.0721750 * bl
    z = (0.0193339 * rl + 0.1191920 * gl_ + 0.9503041 * bl) / 1.08883

    def f(t):
        return t ** (1.0 / 3.0) if t > 0.008856 else 7.787 * t + 16.0 / 116.0

    fx, fy, fz = f(x), f(y), f(z)
    return (116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))


def luminance709(rgb):
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]


# ---------------------------------------------------------------------------
# Inputs
# ---------------------------------------------------------------------------


def synthetic_images():
    """Returns [(name, width, height, rgb_bytes)] for the four built-in test images."""
    w, h = 256, 170
    images = []

    ramp = bytearray(w * h * 3)
    for y in range(h):
        for x in range(w):
            o = (y * w + x) * 3
            ramp[o] = ramp[o + 1] = ramp[o + 2] = x
    images.append(("grey_ramp", w, h, bytes(ramp)))

    hue = bytearray(w * h * 3)
    for x in range(w):
        r, g, b = colorsys.hsv_to_rgb(x / w, 1.0, 1.0)
        px = (int(r * 255 + 0.5), int(g * 255 + 0.5), int(b * 255 + 0.5))
        for y in range(h):
            o = (y * w + x) * 3
            hue[o], hue[o + 1], hue[o + 2] = px
    images.append(("hue_sweep", w, h, bytes(hue)))

    skin = [
        (255, 224, 196),
        (241, 194, 160),
        (224, 172, 130),
        (198, 140, 96),
        (141, 85, 54),
        (86, 50, 33),
    ]
    patches = bytearray(w * h * 3)
    for x in range(w):
        px = skin[min(x * len(skin) // w, len(skin) - 1)]
        for y in range(h):
            o = (y * w + x) * 3
            patches[o], patches[o + 1], patches[o + 2] = px
    images.append(("skin_patches", w, h, bytes(patches)))

    sky = (135, 190, 235)
    foliage = (58, 104, 38)
    grad = bytearray(w * h * 3)
    for y in range(h):
        t = y / (h - 1)
        px = tuple(int(sky[c] + (foliage[c] - sky[c]) * t + 0.5) for c in range(3))
        for x in range(w):
            o = (y * w + x) * 3
            grad[o], grad[o + 1], grad[o + 2] = px
    images.append(("sky_foliage", w, h, bytes(grad)))
    return images


def load_images(folder, max_side):
    images = []
    for name in sorted(os.listdir(folder)):
        path = os.path.join(folder, name)
        if not os.path.isfile(path):
            continue
        if not name.lower().endswith(".png"):
            print(f"warning: skipping non-PNG input {name}")
            continue
        try:
            with open(path, "rb") as f:
                w, h, rgb = decode_png(f.read())
        except (ValueError, zlib.error, struct.error) as e:
            print(f"warning: skipping {name}: {e}")
            continue
        if max(w, h) > max_side:
            step = math.ceil(max(w, h) / max_side)
            nw, nh = max(1, w // step), max(1, h // step)
            small = bytearray(nw * nh * 3)
            for y in range(nh):
                sy = y * step
                for x in range(nw):
                    so = (sy * w + x * step) * 3
                    o = (y * nw + x) * 3
                    small[o:o + 3] = rgb[so:so + 3]
            w, h, rgb = nw, nh, bytes(small)
        stem = os.path.splitext(name)[0]
        images.append((stem, w, h, rgb))
    return images


# ---------------------------------------------------------------------------
# Catalog
# ---------------------------------------------------------------------------


def load_catalog(path):
    """Returns {lut_basename: entry} and the entry order. Missing catalog gives empty results."""
    if not os.path.isfile(path):
        print(f"warning: catalog not found at {path}; using LUT file names")
        return {}, []
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    by_file = {}
    order = []
    for entry in data.get("filters", []):
        lut = entry.get("lut")
        if not lut:
            continue
        base = os.path.basename(lut)
        by_file[base] = entry
        order.append(base)
    return by_file, order


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def slug(text):
    return re.sub(r"[^A-Za-z0-9_-]+", "_", text).strip("_") or "image"


def main(argv=None):
    ap = argparse.ArgumentParser(description="Render LUTs over test images as an HTML contact sheet.")
    ap.add_argument("--luts", required=True, help="folder of LUT PNGs")
    ap.add_argument("--out", required=True, help="output folder")
    ap.add_argument("--images", help="folder of input PNGs (default: synthetic set)")
    ap.add_argument("--catalog", help="catalog.json (default: next to the luts folder)")
    ap.add_argument("--max-side", type=int, default=320, help="downscale inputs to this longest side")
    args = ap.parse_args(argv)

    t0 = time.time()
    luts_dir = os.path.abspath(args.luts)
    out_dir = os.path.abspath(args.out)
    catalog_path = args.catalog or os.path.join(os.path.dirname(luts_dir), "catalog.json")
    os.makedirs(os.path.join(out_dir, "inputs"), exist_ok=True)
    os.makedirs(os.path.join(out_dir, "results"), exist_ok=True)

    by_file, catalog_order = load_catalog(catalog_path)

    # ---- LUTs ----
    lut_files = sorted(f for f in os.listdir(luts_dir) if f.lower().endswith(".png"))
    if not lut_files:
        sys.exit(f"error: no LUT PNGs in {luts_dir}")
    luts = []
    for fname in lut_files:
        with open(os.path.join(luts_dir, fname), "rb") as f:
            try:
                w, h, rgb = decode_png(f.read())
            except (ValueError, zlib.error, struct.error) as e:
                print(f"warning: skipping LUT {fname}: {e}")
                continue
        if (w, h) != (LUT_SIZE, LUT_SIZE):
            print(f"warning: skipping LUT {fname}: expected {LUT_SIZE}x{LUT_SIZE}, got {w}x{h}")
            continue
        entry = by_file.get(fname, {})
        luts.append({
            "file": fname,
            "id": os.path.splitext(fname)[0],
            "name": entry.get("name", os.path.splitext(fname)[0]),
            "group": entry.get("group", "not in catalog"),
            "premium": bool(entry.get("premium", False)),
            "order": catalog_order.index(fname) if fname in by_file else 10 ** 6,
            "sample": make_lut_sampler(rgb),
        })
    luts.sort(key=lambda L: (L["order"], L["file"]))
    print(f"loaded {len(luts)} LUTs from {luts_dir} ({time.time() - t0:.1f}s)")

    # ---- Inputs ----
    if args.images:
        images = load_images(os.path.abspath(args.images), args.max_side)
        if not images:
            sys.exit(f"error: no usable PNG inputs in {args.images}")
        source = args.images
    else:
        images = synthetic_images()
        source = "synthetic"
    names = []
    used = set()
    for stem, w, h, rgb in images:
        base = slug(stem)
        name = base
        n = 2
        while name in used:
            name = f"{base}_{n}"
            n += 1
        used.add(name)
        names.append(name)
        with open(os.path.join(out_dir, "inputs", f"{name}.png"), "wb") as f:
            f.write(encode_png_rgb(w, h, rgb))
    print(f"inputs: {len(images)} images from {source}")

    # ---- Near-duplicates: mean dE76 over a 17^3 grid ----
    grid_inputs = []
    for i in range(GRID_N):
        for j in range(GRID_N):
            for k in range(GRID_N):
                grid_inputs.append((i / (GRID_N - 1), j / (GRID_N - 1), k / (GRID_N - 1)))
    labs = []
    for L in luts:
        sample = L["sample"]
        labs.append([srgb_to_lab(*to_bytes3(sample(*p))) for p in grid_inputs])
    dups = []
    for a in range(len(luts)):
        la = labs[a]
        for b in range(a + 1, len(luts)):
            lb = labs[b]
            total = 0.0
            for p, q in zip(la, lb):
                dl = p[0] - q[0]
                da = p[1] - q[1]
                db = p[2] - q[2]
                total += math.sqrt(dl * dl + da * da + db * db)
            mean = total / len(la)
            if mean < DUP_THRESHOLD:
                dups.append((mean, luts[a]["id"], luts[b]["id"]))
    dups.sort()
    dup_partners = {}
    for mean, a, b in dups:
        dup_partners.setdefault(a, []).append(f"{b} ({mean:.2f})")
        dup_partners.setdefault(b, []).append(f"{a} ({mean:.2f})")
    print(f"near-duplicates (mean dE76 < {DUP_THRESHOLD}): {len(dups)}")
    for mean, a, b in dups:
        print(f"  {a} ~ {b}  mean dE76 = {mean:.2f}")

    # ---- Monotonic grey ramp ----
    grey_inputs = [(v / 255.0, v / 255.0, v / 255.0) for v in range(256)]
    nonmono = {}
    for L in luts:
        sample = L["sample"]
        lum = [luminance709(to_bytes3(sample(*p))) for p in grey_inputs]
        worst_drop = 0.0
        drop_at = None
        for v in range(1, 256):
            drop = lum[v - 1] - lum[v]
            if drop > worst_drop:
                worst_drop = drop
                drop_at = v
        if worst_drop > MONO_TOLERANCE:
            nonmono[L["id"]] = (worst_drop, drop_at)
    print(f"non-monotonic grey response (drop > {MONO_TOLERANCE} level): {len(nonmono)}")
    for L in luts:
        if L["id"] in nonmono:
            drop, at = nonmono[L["id"]]
            print(f"  {L['id']}: max drop {drop:.2f} at grey {at - 1} -> {at}")

    # ---- Apply LUTs to images ----
    originals = []
    for (stem, w, h, rgb), name in zip(images, names):
        originals.append((name, w, h, rgb))
    results = {}
    for L in luts:
        row = []
        for (stem, w, h, rgb), name in zip(images, names):
            res = apply_to_image(L["sample"], w, h, rgb)
            rel = f"results/{L['id']}__{name}.png"
            with open(os.path.join(out_dir, rel), "wb") as f:
                f.write(encode_png_rgb(w, h, res))
            row.append(rel)
        results[L["id"]] = row
    print(f"rendered {len(luts)} LUTs x {len(images)} images ({time.time() - t0:.1f}s)")

    # ---- HTML ----
    def img_tag(src, alt):
        return f'<img src="{html.escape(src)}" alt="{html.escape(alt)}" loading="lazy">'

    dup_rows = "".join(
        f"<tr><td>{html.escape(a)}</td><td>{html.escape(b)}</td><td>{mean:.2f}</td></tr>"
        for mean, a, b in dups
    )
    dup_block = (
        f"<table><thead><tr><th>LUT A</th><th>LUT B</th><th>mean dE76</th></tr></thead>"
        f"<tbody>{dup_rows}</tbody></table>"
        if dups
        else f"<p>None below dE76 {DUP_THRESHOLD:g}.</p>"
    )

    header_cells = "".join(f"<th>{html.escape(n)}</th>" for n in names)
    body_rows = [
        "<tr class=\"orig\"><td class=\"label\"><strong>Original</strong>"
        "<div class=\"meta\">no LUT</div></td>"
        + "".join(f"<td>{img_tag(f'inputs/{n}.png', n)}</td>" for n in names)
        + "</tr>"
    ]
    for L in luts:
        flags = []
        if L["id"] in nonmono:
            drop, at = nonmono[L["id"]]
            flags.append(f'<span class="flag">banding/non-monotonic (drop {drop:.1f} at grey {at})</span>')
        if L["id"] in dup_partners:
            flags.append(f'<span class="dup">near-dup: {html.escape(", ".join(dup_partners[L["id"]]))}</span>')
        premium = '<span class="premium">premium</span>' if L["premium"] else ""
        label = (
            f"<strong>{html.escape(L['name'])}</strong> {premium}"
            f"<div class=\"meta\">{html.escape(L['id'])} &middot; {html.escape(L['group'])}</div>"
            + (f"<div class=\"flags\">{''.join(flags)}</div>" if flags else "")
        )
        cells = "".join(
            f"<td>{img_tag(rel, L['name'] + ' / ' + n)}</td>"
            for rel, n in zip(results[L["id"]], names)
        )
        body_rows.append(f"<tr><td class=\"label\">{label}</td>{cells}</tr>")

    generated = time.strftime("%Y-%m-%d %H:%M:%S")
    page = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>LUT contact sheet</title>
<style>
  :root {{ --bg: #fafafa; --fg: #1b1b1b; --muted: #666; --line: #ddd; --flag: #b3261e; --dup: #8a5a00; }}
  @media (prefers-color-scheme: dark) {{
    :root:not([data-theme="light"]) {{ --bg: #141414; --fg: #eee; --muted: #999; --line: #333; --flag: #ff8a80; --dup: #ffcc80; }}
  }}
  :root[data-theme="dark"] {{ --bg: #141414; --fg: #eee; --muted: #999; --line: #333; --flag: #ff8a80; --dup: #ffcc80; }}
  body {{ background: var(--bg); color: var(--fg); font: 14px/1.4 system-ui, sans-serif; margin: 16px; }}
  h1 {{ font-size: 20px; margin: 0 0 4px; }}
  h2 {{ font-size: 16px; margin: 24px 0 8px; }}
  .meta {{ color: var(--muted); font-size: 12px; }}
  table {{ border-collapse: collapse; }}
  th, td {{ border: 1px solid var(--line); padding: 4px; vertical-align: top; }}
  th {{ text-align: left; font-weight: 600; }}
  td.label {{ min-width: 190px; max-width: 240px; }}
  img {{ display: block; width: 256px; height: auto; image-rendering: auto; }}
  .wrap {{ overflow-x: auto; }}
  .flag {{ color: var(--flag); font-size: 12px; display: block; }}
  .dup {{ color: var(--dup); font-size: 12px; display: block; }}
  .premium {{ font-size: 11px; color: var(--muted); border: 1px solid var(--line); border-radius: 3px; padding: 0 3px; }}
  tr.orig td {{ background: rgba(127,127,127,0.08); }}
</style>
</head>
<body>
<h1>LUT contact sheet</h1>
<div class="meta">{len(luts)} LUTs &middot; {len(images)} images ({html.escape(source)}) &middot; generated {generated}</div>

<h2>Near-duplicates (mean dE76 &lt; {DUP_THRESHOLD:g}, 17^3 grid)</h2>
{dup_block}

<h2>Monotonic grey ramp</h2>
<p>{'None flagged.' if not nonmono else html.escape(str(len(nonmono)) + ' LUT(s) flagged: ' + ', '.join(sorted(nonmono)))}
 Tolerance: a drop of more than {MONO_TOLERANCE:g} level in Rec.709 luma between neighbouring grey steps.</p>

<h2>Grid</h2>
<div class="wrap">
<table>
<thead><tr><th>LUT</th>{header_cells}</tr></thead>
<tbody>
{chr(10).join(body_rows)}
</tbody>
</table>
</div>
</body>
</html>
"""
    index_path = os.path.join(out_dir, "index.html")
    with open(index_path, "w", encoding="utf-8") as f:
        f.write(page)
    print(f"wrote {index_path} ({time.time() - t0:.1f}s total)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
