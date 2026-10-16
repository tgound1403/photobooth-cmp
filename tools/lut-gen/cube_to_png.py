#!/usr/bin/env python3
"""Converts an Adobe/Resolve .cube 3D LUT into a 512x512 LUT PNG for the photobooth filters.

Usage (from the repository root):

    python3 -I tools/lut-gen/cube_to_png.py input.cube output.png
    python3 -I tools/lut-gen/cube_to_png.py --selftest

Input: .cube text with LUT_3D_SIZE N (2..256), optional DOMAIN_MIN / DOMAIN_MAX
(default 0 0 0 and 1 1 1), TITLE, and '#' comments. Entries are "r g b" floats with
red varying fastest. LUT_1D_SIZE is rejected.

The cube is resampled onto the 64-level grid used by generate_luts.py with trilinear
interpolation, and written with the same layout the renderer samples (see the
docstring in generate_luts.py). A PNG input value v in 0..1 is read as a cube input
in DOMAIN units, so the lattice coordinate is (v - DOMAIN_MIN) / (DOMAIN_MAX - DOMAIN_MIN),
clamped to 0..1.

--selftest builds an identity .cube of size 17 in memory, converts it, and asserts that
every LUT entry matches the identity LUT within 1/255 per channel.

Standard library only. The layout and PNG writer come from generate_luts.py, which is
loaded by file path because `python3 -I` keeps the script directory off sys.path.
"""

import argparse
import importlib.util
import os
import sys


def _load_generate_luts():
    here = os.path.dirname(os.path.abspath(__file__))
    spec = importlib.util.spec_from_file_location(
        "generate_luts", os.path.join(here, "generate_luts.py")
    )
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)  # module-level code only defines helpers; main() is guarded
    return module


gl = _load_generate_luts()


def parse_cube(text):
    """Returns (size, values, domain_min, domain_max). values[(b*N + g)*N + r] = (r, g, b) floats."""
    size = None
    domain_min = (0.0, 0.0, 0.0)
    domain_max = (1.0, 1.0, 1.0)
    values = []
    for lineno, raw in enumerate(text.splitlines(), 1):
        line = raw.split("#", 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        key = parts[0].upper()
        if key == "TITLE":
            continue
        if key == "LUT_3D_SIZE":
            if len(parts) != 2:
                raise ValueError(f"line {lineno}: LUT_3D_SIZE needs one value")
            size = int(parts[1])
            continue
        if key == "LUT_1D_SIZE":
            raise ValueError("1D LUTs are not supported; expected LUT_3D_SIZE")
        if key in ("DOMAIN_MIN", "DOMAIN_MAX"):
            if len(parts) != 4:
                raise ValueError(f"line {lineno}: {key} needs three values")
            vals = tuple(float(x) for x in parts[1:4])
            if key == "DOMAIN_MIN":
                domain_min = vals
            else:
                domain_max = vals
            continue
        if key[0].isalpha():
            raise ValueError(f"line {lineno}: unknown keyword {parts[0]!r}")
        if len(parts) != 3:
            raise ValueError(f"line {lineno}: expected three values, got {len(parts)}")
        values.append(tuple(float(x) for x in parts))

    if size is None:
        raise ValueError("missing LUT_3D_SIZE")
    if size < 2 or size > 256:
        raise ValueError(f"LUT_3D_SIZE {size} out of range 2..256")
    if len(values) != size ** 3:
        raise ValueError(f"expected {size ** 3} entries for size {size}, found {len(values)}")
    for c in range(3):
        if domain_max[c] <= domain_min[c]:
            raise ValueError("DOMAIN_MAX must be greater than DOMAIN_MIN on every channel")
    return size, values, domain_min, domain_max


def make_cube_sampler(size, values):
    """Trilinear lookup over the cube. Takes lattice coordinates in 0..1 per channel."""
    n = size
    scale = n - 1

    def sample(u0, u1, u2):
        p0 = min(max(u0 * scale, 0.0), scale)
        p1 = min(max(u1 * scale, 0.0), scale)
        p2 = min(max(u2 * scale, 0.0), scale)
        i0 = min(int(p0), n - 2)
        j0 = min(int(p1), n - 2)
        k0 = min(int(p2), n - 2)
        f0 = p0 - i0
        f1 = p1 - j0
        f2 = p2 - k0
        out0 = out1 = out2 = 0.0
        for dk in (0, 1):
            wk = f2 if dk else 1.0 - f2
            for dj in (0, 1):
                wj = f1 if dj else 1.0 - f1
                for di in (0, 1):
                    wi = f0 if di else 1.0 - f0
                    w = wi * wj * wk
                    v = values[((k0 + dk) * n + (j0 + dj)) * n + (i0 + di)]
                    out0 += w * v[0]
                    out1 += w * v[1]
                    out2 += w * v[2]
        return out0, out1, out2

    return sample


def cube_to_lut_png(text):
    """Parses .cube text and returns the 512x512 LUT PNG bytes."""
    size, values, domain_min, domain_max = parse_cube(text)
    sample = make_cube_sampler(size, values)
    spans = [domain_max[c] - domain_min[c] for c in range(3)]

    def fn(r, g, b):
        out = sample(
            min(max((r - domain_min[0]) / spans[0], 0.0), 1.0),
            min(max((g - domain_min[1]) / spans[1], 0.0), 1.0),
            min(max((b - domain_min[2]) / spans[2], 0.0), 1.0),
        )
        return out

    return gl.encode_png(gl.SIZE, gl.SIZE, gl.build_lut(fn))


def identity_cube_text(size):
    lines = ["TITLE \"identity\"", f"LUT_3D_SIZE {size}"]
    denom = size - 1
    for b in range(size):
        for g in range(size):
            for r in range(size):
                lines.append(f"{r / denom:.6f} {g / denom:.6f} {b / denom:.6f}")
    return "\n".join(lines) + "\n"


def selftest():
    png = cube_to_lut_png(identity_cube_text(17))
    width, height, pixels = gl.decode_png_rgb(png)
    if (width, height) != (gl.SIZE, gl.SIZE):
        raise SystemExit(f"selftest FAILED: output is {width}x{height}, expected 512x512")

    worst = 0
    checked = 0
    for b in range(gl.LEVELS):
        for g in range(gl.LEVELS):
            for r in range(gl.LEVELS):
                off = gl.lut_offset(r, g, b)
                want = (gl.identity_expected(r), gl.identity_expected(g), gl.identity_expected(b))
                for c in range(3):
                    err = abs(pixels[off + c] - want[c])
                    worst = max(worst, err)
                checked += 1
                if worst > 1:
                    raise SystemExit(
                        f"selftest FAILED at input {(r, g, b)}: got "
                        f"{tuple(pixels[off:off + 3])}, want {want}"
                    )
    print(
        f"selftest PASSED: identity .cube (size 17) -> 512x512 LUT PNG, "
        f"{checked} entries checked, max abs error {worst}/255 (limit 1/255)"
    )


def main(argv=None):
    ap = argparse.ArgumentParser(description="Convert a .cube 3D LUT to a 512x512 LUT PNG.")
    ap.add_argument("--selftest", action="store_true", help="run the identity round-trip test")
    ap.add_argument("input", nargs="?", help="input .cube file")
    ap.add_argument("output", nargs="?", help="output .png file")
    args = ap.parse_args(argv)

    if args.selftest:
        selftest()
        return 0
    if not args.input or not args.output:
        ap.error("input.cube and output.png are required (or use --selftest)")

    with open(args.input, encoding="utf-8") as f:
        text = f.read()
    try:
        png = cube_to_lut_png(text)
    except ValueError as e:
        print(f"error: {args.input}: {e}", file=sys.stderr)
        return 1
    with open(args.output, "wb") as f:
        f.write(png)
    print(f"wrote {args.output} ({len(png) / 1024:.1f} KB) from {args.input}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
