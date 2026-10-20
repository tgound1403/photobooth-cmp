#!/usr/bin/env python3
"""Generates stylised camera bodies as GLB files (stdlib only; PIL optional for --preview).

Materials are named like nikon_s3.glb so the picker's tint works: "body" is recoloured by
CameraProfile.modelTint, "lens" (black glass/barrel) and "chrome" keep their own colours.
Axes: x right, y up, +z toward the viewer (lens points to +z).

Usage (repo root):
    python3 -I tools/glb-gen/generate_glb.py                 # writes app/src/main/assets/models/*.glb
    python3 -I tools/glb-gen/generate_glb.py --preview DIR   # also writes DIR/<id>_{3q,front}.png
    python3 -I tools/glb-gen/generate_glb.py --stills        # also rewrites the 2D stills in res/drawable-nodpi
"""
import json, math, struct, sys, os

MATS = [
    ("body", [0.8, 0.8, 0.8, 1.0], 0.0, 0.5),
    ("lens", [0.02, 0.02, 0.02, 1.0], 0.0, 0.15),
    ("chrome", [0.85, 0.85, 0.88, 1.0], 1.0, 0.2),
]
MI = {m[0]: i for i, m in enumerate(MATS)}


def norm(v):
    l = math.sqrt(sum(c * c for c in v)) or 1.0
    return tuple(c / l for c in v)


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


class Mesh:
    def __init__(self):
        self.prims = {}  # material -> (pos, nrm, idx)

    def _p(self, mat):
        return self.prims.setdefault(mat, ([], [], []))

    def add_grid(self, mat, P, N, rows, cols):
        """P/N: flat lists row-major (rows x cols). Triangles oriented to agree with N."""
        pos, nrm, idx = self._p(mat)
        base = len(pos)
        pos.extend(P)
        nrm.extend(N)
        for r in range(rows - 1):
            for c in range(cols - 1):
                a, b, d, e = (base + r * cols + c, base + r * cols + c + 1,
                              base + (r + 1) * cols + c, base + (r + 1) * cols + c + 1)
                for t in ((a, b, e), (a, e, d)):
                    gn = cross(sub(pos[t[1]], pos[t[0]]), sub(pos[t[2]], pos[t[0]]))
                    an = [sum(nrm[i][k] for i in t) for k in range(3)]
                    if sum(g * h for g, h in zip(gn, an)) < 0:
                        t = (t[0], t[2], t[1])
                    if sum(c * c for c in gn) > 1e-14:
                        idx.extend(t)

    def rbox(self, mat, c, size, r):
        """Rounded box centred at c with full size (sx, sy, sz) and corner radius r."""
        h = [s / 2 for s in size]
        r = min(r, min(h) * 0.99)
        axes = [0, 1, 2]
        for ax in axes:
            for sgn in (-1, 1):
                u, v = [a for a in axes if a != ax]
                su = self._samples(h[u], r)
                sv = self._samples(h[v], r)
                P, N = [], []
                for a in su:
                    for b in sv:
                        q = [0.0] * 3
                        q[ax] = sgn * h[ax]
                        q[u] = a
                        q[v] = b
                        inner = [max(-(h[i] - r), min(h[i] - r, q[i])) for i in range(3)]
                        n = norm(sub(q, inner))
                        P.append(tuple(c[i] + inner[i] + n[i] * r for i in range(3)))
                        N.append(n)
                self.add_grid(mat, P, N, len(su), len(sv))

    @staticmethod
    def _samples(h, r):
        m = [0, 0.18, 0.4, 0.68, 1.0]
        left = [-h + r * t for t in m]
        right = [h - r * (1 - t) for t in m]
        return left + right

    def revolve(self, mat, profile, c=(0, 0, 0), axis="z", seg=56, wave=None):
        """Solid of revolution about an axis through c. profile = [(radius, along-axis)] front to back (hard bands).
        wave = (count, amplitude) ripples the radius (knurled rings)."""
        def point(rad, a, th, off):
            if wave and rad > 0:
                rad = rad * (1 + wave[1] * math.cos(wave[0] * th))
            x, y = rad * math.cos(th), rad * math.sin(th)
            q = {"z": (x, y, a), "y": (x, a, y), "x": (a, x, y)}[axis]
            return tuple(c[i] + q[i] for i in range(3))
        for i in range(len(profile) - 1):
            (r0, a0), (r1, a1) = profile[i], profile[i + 1]
            P, N = [], []
            for row, (rad, a) in enumerate(((r0, a0), (r1, a1))):
                for s in range(seg + 1):
                    th = 2 * math.pi * s / seg
                    p = point(rad, a, th, None)
                    # analytic normal of the band (radial part uses da, axial part uses -dr), then ripple-agnostic
                    dr, da = r1 - r0, a1 - a0
                    ln = math.hypot(dr, da) or 1.0
                    nr, na = da / ln, -dr / ln
                    x, y = nr * math.cos(th), nr * math.sin(th)
                    nq = {"z": (x, y, na), "y": (x, na, y), "x": (na, x, y)}[axis]
                    P.append(p)
                    N.append(nq)
            self.add_grid(mat, P, N, 2, seg + 1) if False else self._band(mat, P, N, seg + 1)

    def _band(self, mat, P, N, cols):
        # two rows of `cols` points
        self.add_grid(mat, P, N, 2, cols)

    def cyl(self, mat, c, r, h, axis="z", seg=48, wave=None):
        """Cylinder centred at c, length h along axis, flat caps."""
        self.revolve(mat, [(0, h / 2), (r, h / 2), (r, -h / 2), (0, -h / 2)], c, axis, seg, wave)

    def write(self, path):
        meshprims, bufs, views, accs = [], b"", [], []

        def push(data, target, comp, typ, count, mn=None, mx=None):
            nonlocal bufs
            while len(bufs) % 4:
                bufs += b"\0"
            views.append({"buffer": 0, "byteOffset": len(bufs), "byteLength": len(data), "target": target})
            bufs += data
            a = {"bufferView": len(views) - 1, "componentType": comp, "count": count, "type": typ}
            if mn is not None:
                a["min"], a["max"] = mn, mx
            accs.append(a)
            return len(accs) - 1
        for mat, (pos, nrm, idx) in self.prims.items():
            mn = [min(p[i] for p in pos) for i in range(3)]
            mx = [max(p[i] for p in pos) for i in range(3)]
            pa = push(b"".join(struct.pack("<3f", *p) for p in pos), 34962, 5126, "VEC3", len(pos), mn, mx)
            na = push(b"".join(struct.pack("<3f", *n) for n in nrm), 34962, 5126, "VEC3", len(nrm))
            ia = push(struct.pack("<%dI" % len(idx), *idx), 34963, 5125, "SCALAR", len(idx))
            meshprims.append({"attributes": {"POSITION": pa, "NORMAL": na}, "indices": ia, "material": MI[mat]})
        j = {
            "asset": {"version": "2.0", "generator": "tools/glb-gen/generate_glb.py"},
            "scene": 0, "scenes": [{"nodes": [0]}], "nodes": [{"name": "camera", "mesh": 0}],
            "meshes": [{"primitives": meshprims}],
            "materials": [{"name": n, "pbrMetallicRoughness": {"baseColorFactor": col, "metallicFactor": m, "roughnessFactor": ro},
                           "doubleSided": True} for n, col, m, ro in MATS],
            "accessors": accs, "bufferViews": views, "buffers": [{"byteLength": len(bufs)}],
        }
        js = json.dumps(j, separators=(",", ":")).encode()
        while len(js) % 4:
            js += b" "
        while len(bufs) % 4:
            bufs += b"\0"
        total = 12 + 8 + len(js) + 8 + len(bufs)
        with open(path, "wb") as f:
            f.write(struct.pack("<III", 0x46546C67, 2, total))
            f.write(struct.pack("<II", len(js), 0x4E4F534A) + js)
            f.write(struct.pack("<II", len(bufs), 0x004E4942) + bufs)


# ---------------------------------------------------------------- models

def leica_m():
    m = Mesh()
    W, D = 5.2, 1.5
    m.rbox("chrome", (0, -1.41, 0), (W - 0.1, 0.2, D - 0.04), 0.08)          # base plate
    m.rbox("body", (0, -0.2, 0), (W, 2.3, D), 0.12)                            # leatherette body
    m.rbox("chrome", (0, 1.22, 0), (W, 0.58, D), 0.14)                         # top plate
    # finder windows
    m.rbox("lens", (-1.75, 1.2, D / 2 + 0.005), (0.62, 0.36, 0.06), 0.03)
    m.rbox("lens", (-0.95, 1.2, D / 2 + 0.005), (0.3, 0.2, 0.06), 0.03)
    m.rbox("lens", (1.65, 1.2, D / 2 + 0.005), (0.42, 0.3, 0.06), 0.03)
    # top controls
    m.cyl("chrome", (1.55, 1.58, 0.1), 0.46, 0.3, "y", wave=(36, 0.025))     # shutter speed dial
    m.cyl("chrome", (1.55, 1.76, 0.1), 0.3, 0.1, "y")
    m.cyl("chrome", (0.9, 1.56, 0.1), 0.16, 0.2, "y")                          # shutter button collar
    m.cyl("lens", (0.9, 1.7, 0.1), 0.1, 0.1, "y")
    m.cyl("chrome", (-2.05, 1.58, 0.05), 0.3, 0.3, "y", wave=(30, 0.03))     # rewind knob
    m.rbox("chrome", (-0.1, 1.55, 0.0), (0.9, 0.07, 0.5), 0.02)               # accessory shoe
    # strap lugs
    for sx in (-1, 1):
        m.cyl("chrome", (sx * (W / 2 + 0.05), 0.75, 0), 0.08, 0.3, "x")
    # lens (Summicron-ish)
    cx, cy = 0.0, -0.2
    z0 = D / 2
    m.revolve("chrome", [(0.98, z0), (0.98, z0 + 0.14), (0.9, z0 + 0.14), (0.9, z0)], (cx, cy, 0))
    m.revolve("lens", [(0.82, z0 + 0.14), (0.82, z0 + 1.15), (0.78, z0 + 1.15)], (cx, cy, 0))
    m.revolve("lens", [(0.9, z0 + 0.45), (0.9, z0 + 0.9)], (cx, cy, 0), wave=(48, 0.03))      # focus ring
    m.revolve("lens", [(0.9, z0 + 0.45), (0.82, z0 + 0.45)], (cx, cy, 0))
    m.revolve("lens", [(0.9, z0 + 0.9), (0.82, z0 + 0.9)], (cx, cy, 0))
    m.revolve("chrome", [(0.86, z0 + 0.95), (0.86, z0 + 1.1), (0.78, z0 + 1.1), (0.78, z0 + 0.95)], (cx, cy, 0), wave=(24, 0.02))
    m.revolve("chrome", [(0.78, z0 + 1.15), (0.78, z0 + 1.22), (0.64, z0 + 1.22), (0.64, z0 + 1.15)], (cx, cy, 0))
    m.revolve("lens", [(0.64, z0 + 1.2), (0.6, z0 + 1.1), (0.0, z0 + 1.0)], (cx, cy, 0))       # glass
    return m


def ricoh_gr():
    m = Mesh()
    W, H, D = 4.6, 2.5, 1.2
    m.rbox("body", (0, 0, 0), (W, H, D), 0.2)
    m.rbox("body", (1.7, -0.15, D / 2 - 0.1), (1.2, H - 0.3, 0.5), 0.16)       # grip
    m.rbox("chrome", (0, -H / 2 + 0.02, 0), (W - 0.7, 0.05, D - 0.4), 0.02)   # base
    z0 = D / 2
    cx, cy = -0.7, 0.1
    m.revolve("chrome", [(0.86, z0), (0.86, z0 + 0.09), (0.76, z0 + 0.09), (0.76, z0)], (cx, cy, 0))
    m.revolve("lens", [(0.7, z0 + 0.09), (0.7, z0 + 0.5), (0.66, z0 + 0.5)], (cx, cy, 0))
    m.revolve("chrome", [(0.7, z0 + 0.28), (0.7, z0 + 0.42), (0.64, z0 + 0.42), (0.64, z0 + 0.28)], (cx, cy, 0), wave=(40, 0.03))
    m.revolve("chrome", [(0.66, z0 + 0.5), (0.66, z0 + 0.54), (0.54, z0 + 0.54), (0.54, z0 + 0.5)], (cx, cy, 0))
    m.revolve("lens", [(0.54, z0 + 0.52), (0.5, z0 + 0.46), (0.0, z0 + 0.4)], (cx, cy, 0))
    # top controls
    m.cyl("chrome", (-1.55, H / 2 + 0.08, 0.05), 0.42, 0.24, "y", wave=(34, 0.03))   # mode dial
    m.cyl("chrome", (1.9, H / 2 + 0.05, 0.1), 0.26, 0.1, "y")                          # power ring
    m.cyl("lens", (1.9, H / 2 + 0.16, 0.1), 0.14, 0.12, "y")                           # shutter button
    m.cyl("lens", (0.9, H / 2 + 0.02, 0.1), 0.2, 0.06, "y")
    # grip lamp and flash window
    m.cyl("lens", (2.05, 0.7, D / 2 + 0.4), 0.07, 0.06, "z")
    m.rbox("lens", (0.8, 0.8, D / 2 + 0.005), (0.4, 0.22, 0.05), 0.03)
    m.cyl("chrome", (-W / 2 - 0.04, 0.85, 0), 0.08, 0.26, "x")
    return m



def slr():
    m = Mesh()
    W, H, D = 5.0, 3.1, 1.7
    m.rbox("chrome", (0, -1.45, 0), (W - 0.1, 0.22, D - 0.04), 0.08)          # base plate
    m.rbox("body", (0, -0.35, 0), (W, 2.1, D), 0.14)                           # leatherette body
    m.rbox("chrome", (0, 1.0, 0), (W, 0.62, D), 0.14)                          # top plate
    m.rbox("chrome", (0, 1.65, -0.05), (1.5, 0.8, 1.2), 0.3)                  # pentaprism
    m.rbox("lens", (0, 1.6, 0.55), (0.5, 0.2, 0.06), 0.03)                    # finder logo window
    m.cyl("chrome", (-1.75, 1.45, 0.1), 0.42, 0.3, "y", wave=(36, 0.025))    # rewind / speed dial
    m.cyl("chrome", (1.65, 1.42, 0.1), 0.4, 0.26, "y", wave=(36, 0.025))     # shutter speed dial
    m.cyl("chrome", (1.05, 1.4, 0.3), 0.14, 0.2, "y")
    m.cyl("lens", (1.05, 1.52, 0.3), 0.09, 0.08, "y")                         # shutter button
    m.rbox("chrome", (2.1, 1.35, -0.4), (0.7, 0.07, 0.2), 0.03)               # wind lever
    m.rbox("body", (-2.3, -0.3, 0.55), (0.55, 1.8, 0.6), 0.2)                # grip bulge
    for sx in (-1, 1):
        m.cyl("chrome", (sx * (W / 2 + 0.05), 0.85, 0), 0.08, 0.3, "x")
    cx, cy, z0 = 0.0, -0.3, D / 2
    m.revolve("chrome", [(1.28, z0), (1.28, z0 + 0.14), (1.2, z0 + 0.14), (1.2, z0)], (cx, cy, 0))     # mount
    m.revolve("lens", [(1.05, z0 + 0.14), (1.05, z0 + 1.5), (1.0, z0 + 1.5)], (cx, cy, 0))            # barrel
    m.revolve("lens", [(1.12, z0 + 0.5), (1.12, z0 + 1.1)], (cx, cy, 0), wave=(60, 0.025))              # focus ring
    m.revolve("lens", [(1.12, z0 + 0.5), (1.05, z0 + 0.5)], (cx, cy, 0))
    m.revolve("lens", [(1.12, z0 + 1.1), (1.05, z0 + 1.1)], (cx, cy, 0))
    m.revolve("chrome", [(1.08, z0 + 1.15), (1.08, z0 + 1.3), (1.0, z0 + 1.3), (1.0, z0 + 1.15)], (cx, cy, 0), wave=(30, 0.02))
    m.revolve("chrome", [(1.0, z0 + 1.5), (1.0, z0 + 1.6), (0.82, z0 + 1.6), (0.82, z0 + 1.5)], (cx, cy, 0))
    m.revolve("lens", [(0.82, z0 + 1.55), (0.76, z0 + 1.4), (0.0, z0 + 1.25)], (cx, cy, 0))
    return m


def compact():
    m = Mesh()
    W, H, D = 4.2, 2.4, 1.3
    m.rbox("body", (0, 0, 0), (W, H, D), 0.42)
    m.rbox("chrome", (0, 0, D / 2 - 0.02), (W - 0.35, H - 0.35, 0.1), 0.25)    # front faceplate
    z0 = D / 2 + 0.03
    cx, cy = 0.95, -0.1
    m.revolve("chrome", [(0.72, z0), (0.72, z0 + 0.1), (0.62, z0 + 0.1), (0.62, z0)], (cx, cy, 0))
    m.revolve("lens", [(0.62, z0 + 0.1), (0.62, z0 + 0.3), (0.5, z0 + 0.3), (0.0, z0 + 0.24)], (cx, cy, 0))
    m.rbox("lens", (-1.15, 0.45, z0 + 0.02), (1.0, 0.55, 0.06), 0.08)         # flash window
    m.rbox("lens", (-0.1, 0.55, z0 + 0.02), (0.4, 0.28, 0.06), 0.05)          # finder window
    m.cyl("lens", (-0.9, -0.55, z0 + 0.02), 0.09, 0.06, "z")                   # self-timer lamp
    m.cyl("chrome", (1.5, H / 2 + 0.05, 0.1), 0.2, 0.14, "y")                  # shutter button
    m.cyl("chrome", (0.7, H / 2 + 0.03, 0.1), 0.12, 0.08, "y")
    m.cyl("chrome", (-W / 2 - 0.03, 0.7, 0), 0.07, 0.2, "x")
    return m


def instant():
    """Instax Mini style: chunky rounded body, big telescoping lens barrel, flash and finder on top."""
    m = Mesh()
    W, H, D = 3.9, 3.4, 2.2
    m.rbox("body", (0, 0, 0), (W, H, D), 0.6)
    m.rbox("chrome", (0, 0.95, D / 2 - 0.02), (W - 0.7, 0.05, 0.06), 0.02)     # trim line above the lens
    z0 = D / 2
    cx, cy = 0.0, -0.35
    m.revolve("chrome", [(1.3, z0), (1.3, z0 + 0.12), (1.2, z0 + 0.12), (1.2, z0)], (cx, cy, 0))
    m.revolve("body", [(1.2, z0 + 0.1), (1.2, z0 + 0.85), (1.05, z0 + 0.85), (1.05, z0 + 0.1)], (cx, cy, 0), wave=(40, 0.02))
    m.revolve("chrome", [(1.05, z0 + 0.85), (1.05, z0 + 0.95), (0.88, z0 + 0.95), (0.88, z0 + 0.85)], (cx, cy, 0))
    m.revolve("lens", [(0.88, z0 + 0.95), (0.88, z0 + 0.7), (0.7, z0 + 0.55), (0.0, z0 + 0.45)], (cx, cy, 0))
    m.rbox("lens", (1.05, 1.28, z0 + 0.005), (0.85, 0.42, 0.06), 0.07)         # flash window
    m.rbox("lens", (-1.1, 1.28, z0 + 0.005), (0.45, 0.32, 0.06), 0.06)         # viewfinder
    m.cyl("lens", (-0.35, 1.28, z0 + 0.005), 0.1, 0.06, "z")                   # selfie mirror dot
    m.cyl("chrome", (1.55, 0.2, z0 - 0.12), 0.24, 0.3, "z")                    # shutter button
    m.rbox("lens", (0, H / 2 - 0.02, 0.2), (1.9, 0.05, 0.1), 0.02)             # film exit slot on top
    m.rbox("chrome", (-W / 2 - 0.03, 0.8, -0.2), (0.08, 0.3, 0.3), 0.03)       # strap lug
    return m


def kodak():
    """Kodak Instamatic style: slim 126 cartridge camera with a flash cube on top."""
    m = Mesh()
    W, H, D = 4.4, 2.3, 1.5
    m.rbox("body", (0, 0, 0), (W, H, D), 0.22)
    m.rbox("chrome", (0.1, 0, D / 2 - 0.01), (W - 0.9, H - 0.4, 0.07), 0.06)  # front plate
    z0 = D / 2 + 0.03
    cx, cy = 0.2, -0.1
    m.revolve("chrome", [(0.78, z0), (0.78, z0 + 0.1), (0.66, z0 + 0.1), (0.66, z0)], (cx, cy, 0))
    m.revolve("body", [(0.66, z0 + 0.08), (0.66, z0 + 0.38), (0.54, z0 + 0.38), (0.54, z0 + 0.08)], (cx, cy, 0))
    m.revolve("lens", [(0.54, z0 + 0.38), (0.5, z0 + 0.3), (0.0, z0 + 0.22)], (cx, cy, 0))
    m.rbox("lens", (-1.5, 0.52, z0 + 0.01), (0.55, 0.42, 0.06), 0.06)         # viewfinder
    m.cyl("lens", (1.5, -0.45, z0 + 0.01), 0.16, 0.06, "z")                   # exposure window
    # flash cube on top, offset to the right
    fx = 1.3
    m.rbox("chrome", (fx, H / 2 + 0.55, 0), (1.0, 1.1, 1.0), 0.08)
    for sz in (-1, 1):
        m.rbox("lens", (fx, H / 2 + 0.58, sz * 0.5), (0.7, 0.7, 0.04), 0.05)
    for sx in (-1, 1):
        m.rbox("lens", (fx + sx * 0.5, H / 2 + 0.58, 0), (0.04, 0.7, 0.7), 0.05)
    m.rbox("chrome", (fx, H / 2 + 0.08, 0), (1.2, 0.16, 1.1), 0.05)           # cube socket
    m.cyl("chrome", (-1.4, H / 2 + 0.1, 0.1), 0.22, 0.2, "y", wave=(24, 0.03)) # film advance wheel
    m.cyl("chrome", (-0.5, H / 2 + 0.06, 0.1), 0.16, 0.12, "y")                # shutter button
    return m


def tlr():
    """Twin-lens reflex: tall box, two stacked lenses, waist-level hood on top."""
    m = Mesh()
    W, H, D = 3.0, 4.0, 2.4
    m.rbox("body", (0, 0, 0), (W, H, D), 0.22)
    m.rbox("chrome", (0, H / 2 + 0.2, 0), (W - 0.3, 0.4, D - 0.3), 0.1)       # viewing hood
    m.rbox("lens", (0, H / 2 + 0.43, 0), (W - 0.7, 0.05, D - 0.7), 0.03)      # ground glass
    z0 = D / 2
    for cy, r in ((1.0, 0.62), (-0.85, 0.7)):                                    # viewing lens, taking lens
        m.revolve("chrome", [(r + 0.14, z0), (r + 0.14, z0 + 0.1), (r, z0 + 0.1), (r, z0)], (0, cy, 0))
        m.revolve("lens", [(r, z0 + 0.1), (r, z0 + 0.45), (r - 0.06, z0 + 0.45)], (0, cy, 0))
        m.revolve("lens", [(r - 0.06, z0 + 0.45), (r - 0.18, z0 + 0.3), (0.0, z0 + 0.22)], (0, cy, 0))
    m.cyl("chrome", (W / 2 + 0.12, -0.2, 0.3), 0.3, 0.26, "x", wave=(24, 0.03))  # focus knob
    m.cyl("chrome", (-W / 2 - 0.1, 0.9, 0.3), 0.18, 0.2, "x")                  # winding knob
    m.cyl("lens", (-W / 2 - 0.02, -1.3, 0.2), 0.1, 0.12, "x")                  # shutter release
    return m


def box():
    """Box camera: plain body, small lens, big wire viewfinder-like window and top handle."""
    m = Mesh()
    W, H, D = 3.6, 3.0, 3.0
    m.rbox("body", (0, 0, 0), (W, H, D), 0.3)
    m.rbox("chrome", (0, 0, D / 2 + 0.01), (W - 0.2, 0.08, 0.06), 0.02)        # front seam
    m.rbox("chrome", (0, H / 2 + 0.25, 0), (1.8, 0.1, 0.25), 0.04)             # handle bar
    for sx in (-1, 1):
        m.rbox("chrome", (sx * 0.85, H / 2 + 0.12, 0), (0.12, 0.3, 0.25), 0.03)
    z0 = D / 2
    m.revolve("chrome", [(0.7, z0), (0.7, z0 + 0.12), (0.52, z0 + 0.12), (0.52, z0)], (0, 0.2, 0))
    m.revolve("lens", [(0.52, z0 + 0.12), (0.52, z0 + 0.3), (0.4, z0 + 0.3), (0.0, z0 + 0.22)], (0, 0.2, 0))
    m.rbox("lens", (-1.0, 1.0, z0 + 0.005), (0.55, 0.5, 0.06), 0.05)           # finder
    m.cyl("chrome", (1.3, 1.2, z0 - 0.1), 0.14, 0.4, "z")                      # shutter lever
    return m


def bridge():
    """Bridge camera: chunky grip body with a long zoom barrel."""
    m = Mesh()
    W, H, D = 4.4, 2.7, 1.6
    m.rbox("body", (0, 0, 0), (W, H, D), 0.28)
    m.rbox("body", (1.6, -0.1, D / 2 - 0.1), (1.3, H - 0.2, 0.7), 0.28)        # grip
    m.rbox("chrome", (-1.0, H / 2 + 0.2, 0), (0.9, 0.4, 0.8), 0.12)            # EVF hump
    m.rbox("lens", (-1.0, H / 2 + 0.3, -D / 2 + 0.08), (0.6, 0.22, 0.06), 0.03)
    m.cyl("chrome", (0.5, H / 2 + 0.06, 0.1), 0.38, 0.14, "y", wave=(30, 0.03)) # mode dial
    m.cyl("lens", (1.7, H / 2 + 0.06, 0.3), 0.16, 0.14, "y")                    # shutter
    z0 = D / 2
    cx, cy = -0.5, 0.0
    m.revolve("chrome", [(1.05, z0), (1.05, z0 + 0.1), (0.95, z0 + 0.1), (0.95, z0)], (cx, cy, 0))
    m.revolve("lens", [(0.92, z0 + 0.1), (0.92, z0 + 1.4), (0.86, z0 + 1.4)], (cx, cy, 0))
    m.revolve("lens", [(0.98, z0 + 0.4), (0.98, z0 + 1.0)], (cx, cy, 0), wave=(54, 0.025))
    m.revolve("lens", [(0.98, z0 + 0.4), (0.92, z0 + 0.4)], (cx, cy, 0))
    m.revolve("lens", [(0.98, z0 + 1.0), (0.92, z0 + 1.0)], (cx, cy, 0))
    m.revolve("chrome", [(0.86, z0 + 1.4), (0.86, z0 + 1.5), (0.68, z0 + 1.5), (0.68, z0 + 1.4)], (cx, cy, 0))
    m.revolve("lens", [(0.68, z0 + 1.45), (0.62, z0 + 1.3), (0.0, z0 + 1.15)], (cx, cy, 0))
    return m


def pocket():
    """Pocket film camera: thin slab, off-centre lens, flash and finder windows."""
    m = Mesh()
    W, H, D = 4.0, 2.2, 0.95
    m.rbox("body", (0, 0, 0), (W, H, D), 0.2)
    m.rbox("chrome", (-0.6, 0, D / 2 - 0.02), (W - 1.7, H - 0.2, 0.06), 0.1)   # brushed front plate
    m.rbox("lens", (-1.2, 0.65, D / 2 + 0.04), (0.8, 0.4, 0.05), 0.05)         # flash
    m.rbox("lens", (0.0, 0.7, D / 2 + 0.04), (0.35, 0.25, 0.05), 0.04)         # finder
    z0 = D / 2 + 0.02
    cx, cy = 1.3, 0.0
    m.revolve("chrome", [(0.62, z0), (0.62, z0 + 0.08), (0.52, z0 + 0.08), (0.52, z0)], (cx, cy, 0))
    m.revolve("lens", [(0.52, z0 + 0.08), (0.52, z0 + 0.3), (0.0, z0 + 0.24)], (cx, cy, 0))
    m.cyl("chrome", (0.6, H / 2 + 0.05, 0), 0.18, 0.1, "y")                    # shutter button
    m.cyl("lens", (-1.2, -0.7, z0 + 0.01), 0.08, 0.05, "z")                    # lamp
    return m

MODELS = {"leica_m": leica_m, "ricoh_gr": ricoh_gr, "slr": slr, "compact": compact, "instant": instant, "kodak": kodak,
          "tlr": tlr, "box": box, "bridge": bridge, "pocket": pocket}


# ---------------------------------------------------------------- preview renderer

def render(mesh, tint, yaw, pitch, size=(984, 664), dist=7.2, fov=1.0, fit=0.0):
    from PIL import Image, ImageDraw
    W, H = size
    S = 2
    img = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    cy, sy, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)

    def rot(v):
        x, y, z = v
        x, z = cy * x + sy * z, -sy * x + cy * z
        y, z = cp * y - sp * z, sp * y + cp * z
        return (x, y, z)
    f = (H * S / 2) / math.tan(fov / 2)
    ox = oy = 0.0
    if fit:
        xs, ys = [], []
        for pos, _, _ in mesh.prims.values():
            for p in pos:
                x, y, z = rot(p)
                xs.append(f * x / (dist - z))
                ys.append(f * y / (dist - z))
        k = min(fit * W * S / (max(xs) - min(xs)), fit * H * S / (max(ys) - min(ys)))
        f *= k
        ox = -(max(xs) + min(xs)) / 2 * k
        oy = (max(ys) + min(ys)) / 2 * k
    cols = {"body": tint, "lens": (24, 24, 26), "chrome": (205, 207, 212)}
    tris = []
    light = norm((-0.4, 0.8, 0.6))
    for mat, (pos, nrm, idx) in mesh.prims.items():
        base = cols[mat]
        rp = [rot(p) for p in pos]
        rn = [rot(n) for n in nrm]
        for i in range(0, len(idx), 3):
            a, b, c = idx[i:i + 3]
            n = norm(tuple(sum(rn[k][j] for k in (a, b, c)) for j in range(3)))
            pts = []
            zs = 0
            for k in (a, b, c):
                x, y, z = rp[k]
                zz = dist - z
                pts.append((W * S / 2 + ox + f * x / zz, H * S / 2 + oy - f * y / zz))
                zs += zz
            area = (pts[1][0] - pts[0][0]) * (pts[2][1] - pts[0][1]) - (pts[1][1] - pts[0][1]) * (pts[2][0] - pts[0][0])
            if area > 0:  # back-facing (screen y is flipped)
                continue
            d = max(0.0, sum(n[j] * light[j] for j in range(3)))
            spec = 0.0
            if mat == "chrome":
                h = norm((light[0], light[1], light[2] + 1))
                spec = max(0.0, sum(n[j] * h[j] for j in range(3))) ** 20 * 0.7
                shade = 0.45 + 0.35 * d + (0.15 if n[1] > 0.7 else 0) + spec
            elif mat == "lens":
                h = norm((light[0], light[1], light[2] + 1))
                spec = max(0.0, sum(n[j] * h[j] for j in range(3))) ** 30 * 0.5
                shade = 0.7 + 0.3 * d + spec * 3
            else:
                shade = 0.35 + 0.65 * d
            col = tuple(max(0, min(255, int(v * shade + (255 * spec if mat == "chrome" else 0)))) for v in base)
            tris.append((zs / 3, pts, col))
    tris.sort(key=lambda t: -t[0])
    for _, pts, col in tris:
        dr.polygon(pts, fill=col + (255,), outline=col + (255,))
    return img.resize((W, H), Image.LANCZOS)


def main():
    out_dir = os.path.join("app", "src", "main", "assets", "models")
    os.makedirs(out_dir, exist_ok=True)
    preview = None
    if "--preview" in sys.argv:
        preview = sys.argv[sys.argv.index("--preview") + 1]
        os.makedirs(preview, exist_ok=True)
    tints = {"leica_m": (58, 58, 60), "ricoh_gr": (62, 66, 72), "slr": (224, 169, 28),
             "compact": (31, 154, 163), "instant": (239, 230, 210), "kodak": (201, 161, 91),
             "tlr": (74, 74, 78), "box": (96, 72, 52), "bridge": (50, 52, 56), "pocket": (190, 190, 194)}
    # model -> profile whose still (res/drawable-nodpi/camera_<id>.png) is rendered from it
    stills = {"leica_m": "leica_m", "ricoh_gr": "ricoh_gr", "slr": "golden", "compact": "night", "instant": "fuji_mini", "kodak": "classic35"}
    for name, fn in MODELS.items():
        mesh = fn()
        mesh.write(os.path.join(out_dir, name + ".glb"))
        print(name, os.path.getsize(os.path.join(out_dir, name + ".glb")), "bytes")
        if "--stills" in sys.argv and name in stills:
            render(mesh, tints[name], math.radians(-18), math.radians(10), size=(492, 332), fit=0.92).save(
                os.path.join("app", "src", "main", "res", "drawable-nodpi", "camera_%s.png" % stills[name]))
        if preview:
            render(mesh, tints[name], math.radians(-30), math.radians(18)).save(os.path.join(preview, name + "_3q.png"))
            render(mesh, tints[name], 0, 0).save(os.path.join(preview, name + "_front.png"))
            render(mesh, tints[name], math.radians(-18), math.radians(10), size=(492, 332), fit=0.92).save(
                os.path.join(preview, "camera_" + name + ".png"))


if __name__ == "__main__":
    main()
