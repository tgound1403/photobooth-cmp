#!/usr/bin/env python3
"""Report near-duplicate Japan Pack LUTs (analysis only, changes nothing).

Usage (from the repository root):

    python3 -I tools/lut-gen/analyze_dupes.py [cache.json]

Each LUT is sampled on a 7x7x7 grid; the distance between two looks is the mean RGB
distance (0..441) over those samples. Looks are walked easiest first (catalog
`difficulty`) and one is kept only if it is farther than the threshold from every
kept look. Prints how many looks survive per threshold, and writes the sampled
vectors to the cache file so reruns skip the slow PNG decode.
"""

import importlib.util
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("generate_luts", os.path.join(HERE, "generate_luts.py"))
gl = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gl)

LEVELS = [0, 10, 21, 31, 42, 52, 63]
THRESHOLDS = [3, 5, 7, 10, 14]


def sample(path):
    with open(path, "rb") as f:
        _, _, px = gl.decode_png_rgb(f.read())
    vec = []
    for r in LEVELS:
        for g in LEVELS:
            for b in LEVELS:
                off = gl.lut_offset(r, g, b)
                vec.extend(px[off:off + 3])
    return vec


def main():
    cache_path = sys.argv[1] if len(sys.argv) > 1 else None
    with open(os.path.join(gl.ASSET_DIR, "catalog.json")) as f:
        entries = [e for e in json.load(f)["filters"] if e["group"] == "Japan Pack"]
    cache = {}
    if cache_path and os.path.exists(cache_path):
        with open(cache_path) as f:
            cache = json.load(f)
    for e in entries:
        if e["id"] not in cache:
            cache[e["id"]] = sample(os.path.join(gl.ASSET_DIR, e["lut"]))
    if cache_path:
        with open(cache_path, "w") as f:
            json.dump(cache, f)
    n_pts = len(LEVELS) ** 3

    def dist(a, b):
        return math.dist(cache[a], cache[b]) / math.sqrt(n_pts)  # RMS over points, 0..441 scale

    order = sorted(entries, key=lambda e: e["difficulty"])
    print(f"{len(entries)} Japan looks")
    for t in THRESHOLDS:
        kept = []
        for e in order:
            if all(dist(e["id"], k) > t for k in kept):
                kept.append(e["id"])
        print(f"threshold {t:>2}: keep {len(kept)}")
    print("difficulty percentiles:", [round(sorted(e["difficulty"] for e in entries)[int(len(entries) * p)], 2) for p in (0.1, 0.5, 0.9, 0.99)])


if __name__ == "__main__":
    main()
