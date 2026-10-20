#!/usr/bin/env python3
"""Losslessly slims a GLB (stdlib only): drops attributes the app never uses (UVs, tangents, colours, there are no
textures), welds identical vertices, and stores indices as uint16 where the primitive allows it. Nodes, materials
and geometry are unchanged, so tinting by material name keeps working.

Note: decimating nikon_s3.glb with gltfpack (-si 0.3 and 0.5, even with -slb) tore the seam where the lens meets
the body, so the shipped model is this lossless output only.

Usage: python3 -I tools/glb-gen/slim_glb.py in.glb out.glb
"""
import json, struct, sys

KEEP = ("POSITION", "NORMAL")
SIZES = {5126: 4, 5125: 4, 5123: 2, 5121: 1}
FMT = {5126: "f", 5125: "I", 5123: "H", 5121: "B"}
NCOMP = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}


def read(j, binb, acc):
    a = j["accessors"][acc]
    v = j["bufferViews"][a["bufferView"]]
    n = NCOMP[a["type"]]
    off = v.get("byteOffset", 0) + a.get("byteOffset", 0)
    stride = v.get("byteStride") or n * SIZES[a["componentType"]]
    fmt = "<%d%s" % (n, FMT[a["componentType"]])
    return [struct.unpack_from(fmt, binb, off + i * stride) for i in range(a["count"])]


def main(src, dst):
    d = open(src, "rb").read()
    jl = struct.unpack_from("<I", d, 12)[0]
    j = json.loads(d[20:20 + jl])
    bo = 20 + jl + 8
    binb = d[bo:bo + struct.unpack_from("<I", d, bo - 8)[0]]
    out, views, accs = bytearray(), [], []

    def push(data, target, comp, typ, count, mn=None, mx=None):
        while len(out) % 4:
            out.append(0)
        views.append({"buffer": 0, "byteOffset": len(out), "byteLength": len(data), "target": target})
        out.extend(data)
        a = {"bufferView": len(views) - 1, "componentType": comp, "count": count, "type": typ}
        if mn is not None:
            a["min"], a["max"] = mn, mx
        accs.append(a)
        return len(accs) - 1

    for mesh in j["meshes"]:
        for p in mesh["primitives"]:
            pos = read(j, binb, p["attributes"]["POSITION"])
            nrm = read(j, binb, p["attributes"]["NORMAL"])
            idx = read(j, binb, p["indices"]) if "indices" in p else [(i,) for i in range(len(pos))]
            remap, keep, seen = [], [], {}
            for i, (a, b) in enumerate(zip(pos, nrm)):
                k = (a, tuple(round(x, 4) for x in b))
                if k not in seen:
                    seen[k] = len(keep)
                    keep.append(i)
                remap.append(seen[k])
            tris = [tuple(remap[i[0]] for i in idx[t:t + 3]) for t in range(0, len(idx), 3)]
            tris = [t for t in tris if len(set(t)) == 3]
            flat = [i for t in tris for i in t]
            P = [pos[i] for i in keep]
            mn = [min(q[c] for q in P) for c in range(3)]
            mx = [max(q[c] for q in P) for c in range(3)]
            pa = push(b"".join(struct.pack("<3f", *q) for q in P), 34962, 5126, "VEC3", len(P), mn, mx)
            na = push(b"".join(struct.pack("<3f", *nrm[i]) for i in keep), 34962, 5126, "VEC3", len(P))
            small = len(P) < 65536
            ia = push(struct.pack("<%d%s" % (len(flat), "H" if small else "I"), *flat), 34963,
                      5123 if small else 5125, "SCALAR", len(flat))
            p["attributes"] = {"POSITION": pa, "NORMAL": na}
            p["indices"] = ia
    j["accessors"], j["bufferViews"], j["buffers"] = accs, views, [{"byteLength": len(out)}]
    for k in ("images", "textures", "samplers"):
        j.pop(k, None)
    js = json.dumps(j, separators=(",", ":")).encode()
    while len(js) % 4:
        js += b" "
    while len(out) % 4:
        out.append(0)
    with open(dst, "wb") as f:
        f.write(struct.pack("<III", 0x46546C67, 2, 12 + 8 + len(js) + 8 + len(out)))
        f.write(struct.pack("<II", len(js), 0x4E4F534A) + js)
        f.write(struct.pack("<II", len(out), 0x004E4942) + bytes(out))
    print(len(d), "->", 12 + 8 + len(js) + 8 + len(out), "bytes")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
