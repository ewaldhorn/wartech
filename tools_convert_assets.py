#!/usr/bin/env python3
"""Rebuild assets_data.js from the source PNGs in assets/sprites/.

Basically, I didn't want to have to deal with tons of little images, so I turn the images into
data and load it at runtime. Not efficient, but simple.

Array order and sizes come from the declarations in src/main/java/wartech/render/AssetData.java;
each array maps to one PNG by naming convention (see png_for). Fails loudly
on any missing file or size mismatch instead of writing a corrupt payload.

Requires Pillow (pip install pillow). Normal builds do NOT need this: the
generated assets_data.js is committed so `mvn package` works standalone.
"""
import base64
import re
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
SPR = ROOT / "assets/sprites"

# Array-name -> png path (relative to assets/sprites) for names the
# conventions below cannot derive.
NAMED = {
    "BLDG_WIND_TRAP": "buildings/wind_trap.png",
    "BLDG_REFINERY": "buildings/refinery.png",
    "BLDG_BARRACKS": "buildings/barracks.png",
    "BLDG_LIGHT_FACTORY": "buildings/light_factory.png",
    "BLDG_HEAVY_FACTORY": "buildings/heavy_factory.png",
    "BLDG_REPAIR_VANGUARD": "buildings/repair_facility_vanguard.png",
    "BLDG_REPAIR_SYNDICATE": "buildings/repair_facility_syndicate.png",
    "BLDG_RADAR": "buildings/radar.png",
    "BLDG_TURRET": "buildings/turret.png",
    "BLDG_CON_YARD": "buildings/con_yard.png",
    "UNIT_MECHANIC_VANGUARD": "units/mechanic_vanguard.png",
    "UNIT_MECHANIC_SYNDICATE": "units/mechanic_syndicate.png",
    "UNIT_MEDIC_VANGUARD": "units/medic_vanguard.png",
    "UNIT_MEDIC_SYNDICATE": "units/medic_syndicate.png",
}


def png_for(name):
    if name in NAMED:
        return SPR / NAMED[name]
    if name.startswith("UNIT_"):
        return SPR / "units" / (name[5:].lower() + ".png")
    m = re.fullmatch(r"COAST([01])_M(\d+)", name)
    if m:
        return SPR / "terrain" / f"coast_{m.group(1)}_{int(m.group(2)):02d}.png"
    m = re.fullmatch(r"CORN([01])_(NE|SE|SW|NW)", name)
    if m:
        return SPR / "terrain" / f"corner_{m.group(1)}_{m.group(2)}.png"
    return SPR / "terrain" / (name.lower() + ".png")


def main():
    text = (ROOT / "src/main/java/wartech/render/AssetData.java").read_text()
    decls = re.findall(
        r"public\s+static\s+final\s+int\[\]\s+([A-Za-z0-9_]+)\s*=\s*new\s+int\[(\d+)\];",
        text,
    )
    if not decls:
        print("error: no array declarations found in src/main/java/wartech/render/AssetData.java", file=sys.stderr)
        return 1
    print(f"Found {len(decls)} array declarations")

    bin_data = bytearray()
    for name, size in decls:
        size = int(size)
        p = png_for(name)
        if not p.exists():
            print(f"error: {name}: missing {p.relative_to(ROOT)}", file=sys.stderr)
            return 1
        img = Image.open(p).convert("RGBA")
        if img.width * img.height != size:
            print(f"error: {name}: {p.name} is {img.width}x{img.height}, "
                  f"expected {size} pixels", file=sys.stderr)
            return 1
        # RGBA bytes are already R | G<<8 | B<<16 | A<<24 per pixel: the exact
        # packed format AssetData.loadFromBytes decodes.
        bin_data.extend(img.tobytes())

    b64_str = base64.b64encode(bin_data).decode("ascii")
    out = ROOT / "assets_data.js"
    out.write_text(f"// Auto-generated assets data\n"
                   f"globalThis.__assetsB64 = '{b64_str}';\n")
    print(f"Wrote {out.name} ({len(b64_str)} chars, {len(bin_data)} bytes)")

    # Every PNG must back an array; anything else is dead art.
    used = {png_for(n).resolve() for n, _ in decls}
    orphans = sorted(p for p in SPR.rglob("*.png") if p.resolve() not in used)
    for p in orphans:
        print(f"warning: orphan png with no array: {p.relative_to(ROOT)}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
