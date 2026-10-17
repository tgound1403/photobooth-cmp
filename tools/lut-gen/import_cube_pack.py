#!/usr/bin/env python3
"""Imports the Japan preset pack: one camera per pack group, one filter per new look.

Run from the repository root:

    python3 -I tools/lut-gen/import_cube_pack.py <cube_root> <looks_csv>

<cube_root> is the pack's LUT/英文版 folder. <looks_csv> is the comparison report whose
rows have phan_loai == "mới" (look is new, not a duplicate of an existing filter or identity).
Column duong_dan holds each cube path relative to <cube_root>.

Writes:
  * app/src/main/assets/filters/luts/jp<NNN>_<KKK>.png   (512x512 LUT PNGs, via cube_to_png.py)
  * tools/lut-gen/japan_pack.json                       (catalog entries, read by generate_luts.py)
  * app/src/main/java/.../core/render/JapanCameras.kt   (one CameraProfile per pack group)

Standard library only. Conversion runs in a process pool.
"""

import csv
import colorsys
import importlib.util
import json
import os
import sys
from multiprocessing import Pool

HERE = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
LUT_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "filters", "luts")
PACK_JSON = os.path.join(HERE, "japan_pack.json")
KOTLIN_OUT = os.path.join(
    REPO_ROOT, "app", "src", "main", "java", "com", "example", "photogether", "core", "render",
    "JapanCameras.kt",
)
EXISTING_TINTS = {0xFFC9A15B, 0xFFEFE6D2, 0xFFE0A91C, 0xFF1F9AA3, 0xFFC9CED6}

# English camera names for the 37 pack groups, keyed by the pack's group number.
CAMERA_NAMES = {
    "001": "Soft Blue Yellow", "002": "Japanese Style", "003": "Fresh Natural",
    "004": "Bright Portrait", "005": "Japan Master Portrait", "006": "Cinema Dark Teal",
    "007": "Orange Blue Street", "008": "Insta Travel", "009": "Caramel Wedding",
    "010": "Classic Wedding", "011": "Forest Art", "012": "Hong Kong Cinema",
    "013": "Art Film Mood", "014": "Mood Film", "015": "Japan Warm Film",
    "016": "Night City Gold", "017": "Morandi Forest", "018": "Indoor Clean",
    "019": "Cafe Film", "020": "Cream Muted", "021": "Caramel Product",
    "022": "Morandi Cinema Blue", "023": "Travel Street Film", "024": "Bright Food",
    "025": "Airy Film", "026": "Teal Orange Landscape", "027": "Travel Cinema",
    "028": "Dark Documentary Film", "029": "Dreamy Kids", "030": "Premium Morandi Grey",
    "031": "Morandi Still Life", "032": "Beach Muted Film", "033": "Dark Chocolate",
    "034": "City Cinema Film", "035": "Elegant Grey Portrait", "036": "Japan Street Landscape",
    "037": "Film Roll",
}

# Vietnamese picker descriptions, keyed like CAMERA_NAMES: the look's character, then the scenes it suits.
# Written from the measured traits of each group's LUTs (brightness, warmth, contrast, saturation, lifted
# shadows); the group names are only hints and a few of them are misleading. No quotes or dollar signs.
CAMERA_DESCRIPTIONS = {
    "001": "Vàng ấm đậm, sáng mờ dịu. Hợp nắng chiều, chân dung ấm áp.",
    "002": "Kho 105 look sáng, mát, màu nhạt kiểu Nhật. Hợp đời thường, phố, du lịch.",
    "003": "Sáng trong, mát nhẹ, màu sạch. Hợp ảnh hằng ngày, cây cối, chân dung",
    "004": "Sáng mịn, da sáng, màu nhạt. Hợp chân dung, selfie, trong nhà sáng.",
    "005": "Da sáng trong, tông mát dịu. Hợp chân dung ngoài trời.",
    "006": "Điện ảnh trầm, bóng ấm, vùng sáng mát. Hợp cảnh tối, đèn đường, phim ngắn",
    "007": "Cam – xanh kiểu điện ảnh, cân bằng. Hợp đường phố, kiến trúc, hoàng hôn.",
    "008": "Sáng, nét, màu sạch kiểu Instagram. Hợp du lịch, check-in, đồ ăn.",
    "009": "Sáng ấm màu caramel, màu nhạt mịn. Hợp đám cưới, chân dung, trong nhà.",
    "010": "Sáng ấm cổ điển, da mịn. Hợp đám cưới, tiệc, chân dung.",
    "011": "Sáng mờ, nâng bóng, màu dịu. Hợp cây cối, rừng, ảnh nghệ thuật.",
    "012": "Sáng, tương phản, hơi mát kiểu phim Hồng Kông. Hợp phố đêm, đường phố.",
    "013": "Sáng mờ như phim nghệ thuật, màu nhạt. Hợp ảnh tâm trạng, nội thất.",
    "014": "Phim mờ, bóng nâng, tương phản thấp. Hợp cà phê, ngày nhiều mây, tâm trạng.",
    "015": "Phim Nhật sáng, tông mát dịu. Hợp đời thường, trời xanh, phố.",
    "016": "Vàng ấm đậm, sáng và rực. Hợp phố đêm, đèn vàng, quán ăn.",
    "017": "Morandi mờ ấm, bão hòa thấp. Hợp cây cối, nội thất, tĩnh vật.",
    "018": "Rất sáng, sạch, trắng mát. Hợp trong nhà, sản phẩm, phòng trắng.",
    "019": "Phim sáng nhẹ, màu dịu. Hợp quán cà phê, đồ ăn, ảnh đời thường.",
    "020": "Kem sáng, màu nhạt mịn. Hợp chân dung, đồ ăn, nội thất ấm.",
    "021": "Caramel ấm, bóng nâng. Hợp đồ ăn, sản phẩm, đồ handmade.",
    "022": "Morandi mờ, bóng ấm sáng mát. Hợp phong cảnh, nội thất, tâm trạng.",
    "023": "Sáng, nét, màu dịu kiểu phim du lịch. Hợp đường phố, kiến trúc, chuyến đi",
    "024": "Mát, sạch, màu tươi tương phản. Hợp ảnh sản phẩm, đường phố nắng, bầu trời xanh",
    "025": "Cực sáng, thoáng, mờ. Hợp chân dung, ngày nắng, ảnh trong veo.",
    "026": "Cam – xanh điện ảnh, sáng và nét. Hợp phong cảnh, biển núi, du lịch.",
    "027": "Điện ảnh du lịch, sáng, nét. Hợp phong cảnh, thành phố, chuyến đi.",
    "028": "Tối, trầm, ấm kiểu phim tài liệu. Hợp phố, chân dung chân thực, thiếu sáng.",
    "029": "Nhẹ, mát, mơ. Hợp trẻ em, ngày nắng, ảnh vui.",
    "030": "Xám Morandi ấm, nhạt dịu. Hợp tĩnh vật, nội thất, chân dung tối giản.",
    "031": "Morandi sáng nhẹ, nâng bóng. Hợp tĩnh vật, hoa, đồ vật.",
    "032": "Sáng mát, màu nhạt như nắng biển. Hợp biển, trời nắng, du lịch.",
    "033": "Nâu chocolate trầm, tối, ấm. Hợp chân dung ấm, cà phê, thiếu sáng.",
    "034": "Sáng mạnh, tương phản cao, rất điện ảnh. Hợp thành phố, phố đêm.",
    "035": "Xám thanh lịch, màu rất nhạt. Hợp chân dung, thời trang, tối giản.",
    "036": "Sáng, mát, nét kiểu phố Nhật. Hợp đường phố, phong cảnh, trời xanh.",
    "037": "Phim sáng mạnh, mờ nhẹ, chất cuộn film. Hợp ngày nắng, chân dung, kỷ niệm.",
}
assert set(CAMERA_DESCRIPTIONS) == set(CAMERA_NAMES)


def _load_cube_to_png():
    spec = importlib.util.spec_from_file_location(
        "cube_to_png", os.path.join(HERE, "cube_to_png.py")
    )
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _convert(job):
    cube_path, out_path = job
    with open(cube_path, encoding="utf-8") as f:
        text = f.read()
    png = _cube_to_png.cube_to_lut_png(text)
    with open(out_path, "wb") as f:
        f.write(png)
    return out_path, len(png)


_cube_to_png = None


def _init_worker():
    global _cube_to_png
    _cube_to_png = _load_cube_to_png()


def camera_number(folder):
    # "002 - 日系风格 109款" -> "002"
    return folder.split(" ", 1)[0]


def tint_for(index, total):
    r, g, b = colorsys.hsv_to_rgb(index / total, 0.45, 0.85)
    return 0xFF000000 | (round(r * 255) << 16) | (round(g * 255) << 8) | round(b * 255)


def kotlin_hex(argb):
    return f"0x{argb:08X}.toInt()"


def main(argv):
    if len(argv) != 3:
        sys.exit(__doc__)
    cube_root, looks_csv = argv[1], argv[2]

    with open(looks_csv, encoding="utf-8-sig", newline="") as f:
        looks = [row for row in csv.DictReader(f) if row["phan_loai"] == "mới"]

    # group new looks by pack folder (first path segment), keeping the report order
    groups = {}
    for row in looks:
        folder = row["nhom"]
        groups.setdefault(folder, []).append(row)

    cameras = []  # (camera_number, name, folder, [filter ids])
    filters = []  # catalog entries
    jobs = []
    for folder in sorted(groups):
        num = camera_number(folder)
        if num not in CAMERA_NAMES:
            sys.exit(f"no camera name for pack group {folder!r}")
        rows = sorted(groups[folder], key=lambda r: r["duong_dan"])
        ids = []
        for k, row in enumerate(rows, 1):
            fid = f"jp{num}_{k:03d}"
            ids.append(fid)
            name = row["ten_file"]
            filters.append({"id": fid, "name": name, "group": "Japan",
                            "lut": f"luts/{fid}.png", "premium": False})
            jobs.append((os.path.join(cube_root, row["duong_dan"]),
                         os.path.join(LUT_DIR, f"{fid}.png")))
        cameras.append((num, CAMERA_NAMES[num], folder, ids))

    os.makedirs(LUT_DIR, exist_ok=True)
    with Pool(initializer=_init_worker) as pool:
        total = 0
        for done, (out, size) in enumerate(pool.imap_unordered(_convert, jobs), 1):
            total += size
            if done % 100 == 0 or done == len(jobs):
                print(f"converted {done}/{len(jobs)}", flush=True)

    with open(PACK_JSON, "w", encoding="utf-8") as f:
        json.dump({"version": 1, "filters": filters}, f, ensure_ascii=False, indent=1)
        f.write("\n")

    tints = [tint_for(i, len(cameras)) for i in range(len(cameras))]
    clash = EXISTING_TINTS.intersection(tints)
    assert not clash, f"tint clashes with existing cameras: {clash}"
    assert len(set(tints)) == len(tints), "duplicate tints"

    lines = [
        "package com.example.photogether.core.render",
        "",
        "// GENERATED by tools/lut-gen/import_cube_pack.py. Do not edit by hand; re-run the importer.",
        "",
        "/** One camera per Japan preset pack group. Their looks are listed in japan_pack.json. */",
        "internal val JAPAN_CAMERAS: List<CameraProfile> =",
        "    listOf(",
    ]
    for (num, name, folder, ids), tint in zip(cameras, tints):
        lines += [
            "        CameraProfile(",
            f'            id = "jp_{num}",',
            f'            name = "{name}",',
            f'            subtitle = "{CAMERA_DESCRIPTIONS[num]}",',
            "            filterIds = listOf(",
        ]
        lines += [f'                "{fid}",' for fid in ids]
        lines += [
            "            ),",
            f"            modelTint = {kotlin_hex(tint)},",
            "        ),",
        ]
    lines += ["    )", ""]
    with open(KOTLIN_OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print(f"cameras: {len(cameras)}, filters: {len(filters)}, LUT PNG total: {total / 1024 / 1024:.1f} MB")
    print(f"wrote {PACK_JSON}")
    print(f"wrote {KOTLIN_OUT}")


if __name__ == "__main__":
    main(sys.argv)
