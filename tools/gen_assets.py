#!/usr/bin/env python3
"""Generates every texture / model json / lang / recipe for the Shadow Monarch mod.
Run:  python3 tools/gen_assets.py   (from the project root)
Outputs go to src/main/resources (committed, so the build does not need Python)."""
import json
import os
import random
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "shadowmonarch")
DATA = os.path.join(ROOT, "data", "shadowmonarch")
TEX_ENTITY = os.path.join(ASSETS, "textures", "entity")
TEX_ITEM = os.path.join(ASSETS, "textures", "item")
TEX_ARMOR = os.path.join(ASSETS, "textures", "models", "armor")
MODELS_ITEM = os.path.join(ASSETS, "models", "item")
for d in (TEX_ENTITY, TEX_ITEM, TEX_ARMOR, MODELS_ITEM, os.path.join(ASSETS, "lang"),
          os.path.join(DATA, "recipes")):
    os.makedirs(d, exist_ok=True)

rnd = random.Random(1337)


# ----------------------------------------------------------------------------- helpers
def clamp(v):
    return max(0, min(255, int(v)))


def jitter(c, amt):
    d = rnd.randint(-amt, amt)
    return (clamp(c[0] + d), clamp(c[1] + d), clamp(c[2] + d), 255)


def fill(img, rect, color, noise=6):
    x, y, w, h = rect
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            if 0 <= xx < img.width and 0 <= yy < img.height:
                img.putpixel((xx, yy), jitter(color, noise))


def px(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), (color[0], color[1], color[2], 255))


def box_uv(u, v, w, h, d):
    """Minecraft box UV layout -> face rectangles (x, y, w, h)."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


def fill_box(img, u, v, w, h, d, color, noise=6):
    for rect in box_uv(u, v, w, h, d).values():
        fill(img, rect, color, noise)


def shade(c):
    """Turn any colour into a dark, purple-tinted 'shadow' colour keeping its luminance."""
    lum = (0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]) / 255.0
    return (int(10 + lum * 38), int(5 + lum * 16), int(30 + lum * 75))


SHADOW_EYES = (90, 170, 255)


# ----------------------------------------------------------------------------- entity painters
def paint_biped(p):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # head (0,0) 8x8x8
    fill_box(img, 0, 0, 8, 8, 8, p["skin"])
    hair = p.get("hair", p["skin"])
    f = box_uv(0, 0, 8, 8, 8)
    for face in ("top", "right", "left", "back"):
        fill(img, f[face], hair)
    if p.get("helmet"):
        for face in ("front", "right", "left", "back", "top"):
            fill(img, f[face], p["helmet"])
        fx, fy, fw, fh = f["front"]
        fill(img, (fx + 1, fy + 3, 6, 2), (10, 10, 12), 2)      # visor slit
        px(img, fx + 2, fy + 3, p["eyes"])
        px(img, fx + 5, fy + 3, p["eyes"])
        px(img, fx + 2, fy + 4, p["eyes"])
        px(img, fx + 5, fy + 4, p["eyes"])
        if p.get("crest"):
            fill(img, (f["top"][0] + 3, f["top"][1], 2, 8), p["crest"], 3)
    else:
        fx, fy, fw, fh = f["front"]
        fill(img, (fx, fy, 8, 3), hair, 6)                      # forehead / hair
        for ex in (1, 5):
            px(img, fx + ex, fy + 4, (255, 255, 255))
            px(img, fx + ex + 1, fy + 4, p["eyes"])
        fill(img, (fx + 3, fy + 6, 2, 1), (60, 20, 20), 3)       # mouth
        if p.get("tusks"):
            px(img, fx + 2, fy + 7, (240, 240, 220))
            px(img, fx + 5, fy + 7, (240, 240, 220))
    # body (16,16) 8x12x4
    fill_box(img, 16, 16, 8, 12, 4, p["torso"])
    bf = box_uv(16, 16, 8, 12, 4)
    for face in ("front", "back"):
        bx, by, bw, bh = bf[face]
        fill(img, (bx, by + 9, bw, 2), p.get("belt", (35, 25, 20)), 3)   # belt
        if p.get("chest"):
            fill(img, (bx + 2, by + 1, bw - 4, 6), p["chest"], 5)
    # arms (40,16) 4x12x4  (left arm mirrors right in the 64x32 layout)
    fill_box(img, 40, 16, 4, 12, 4, p["sleeve"])
    af = box_uv(40, 16, 4, 12, 4)
    for face in ("right", "front", "left", "back"):
        ax, ay, aw, ah = af[face]
        fill(img, (ax, ay + 9, aw, 3), p.get("hands", p["skin"]), 5)
    # legs (0,16) 4x12x4
    fill_box(img, 0, 16, 4, 12, 4, p["legs"])
    lf = box_uv(0, 16, 4, 12, 4)
    for face in ("right", "front", "left", "back"):
        lx, ly, lw, lh = lf[face]
        fill(img, (lx, ly + 9, lw, 3), p.get("boots", (30, 25, 22)), 4)
    return img


def paint_quad(p):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    fill(img, (0, 0, 64, 32), p["base"], 8)
    fill(img, (28, 8, 36, 16), p.get("body", p["base"]), 10)          # body panels
    f = box_uv(0, 0, 8, 8, 8)
    fx, fy, fw, fh = f["front"]
    fill(img, (fx, fy, 8, 8), p.get("face", p["base"]), 6)
    for ex in (1, 5):
        px(img, fx + ex, fy + 3, p["eyes"])
        px(img, fx + ex + 1, fy + 3, p["eyes"])
    fill(img, (fx + 2, fy + 5, 4, 3), p.get("muzzle", p["base"]), 5)
    px(img, fx + 3, fy + 6, (15, 10, 10))
    px(img, fx + 4, fy + 6, (15, 10, 10))
    lf = box_uv(0, 16, 4, 6, 4)
    for face in ("right", "front", "left", "back"):
        lx, ly, lw, lh = lf[face]
        fill(img, (lx, ly + 4, lw, 2), p.get("paws", (20, 20, 25)), 3)
    return img


def paint_insect(p):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    fill(img, (0, 0, 64, 32), p["base"], 8)
    fill(img, (0, 12, 32, 12), p.get("body", p["base"]), 10)
    f = box_uv(32, 4, 8, 8, 8)
    fx, fy, fw, fh = f["front"]
    fill(img, (fx, fy, 8, 8), p.get("face", p["base"]), 6)
    for (ex, ey) in ((1, 2), (2, 2), (5, 2), (6, 2), (3, 4), (4, 4)):
        px(img, fx + ex, fy + ey, p["eyes"])
    fill(img, (fx + 2, fy + 6, 4, 2), p.get("mandible", (60, 50, 60)), 4)
    # leg stripes
    for yy in range(0, 4, 2):
        fill(img, (18, yy, 16, 1), p.get("legs", p["base"]), 4)
    return img


def shadow_of(painter, p):
    sp = {}
    for k, v in p.items():
        if k == "eyes":
            sp[k] = SHADOW_EYES
        elif isinstance(v, tuple) and len(v) == 3:
            sp[k] = shade(v)
        else:
            sp[k] = v
    return painter(sp)


MOBS = {
    # ---- biped ----
    "goblin": ("biped", dict(skin=(84, 140, 70), hair=(60, 100, 50), torso=(110, 80, 50), sleeve=(90, 120, 60),
                             legs=(90, 70, 45), boots=(50, 35, 25), eyes=(255, 220, 40), belt=(60, 40, 25),
                             tusks=True)),
    "goblin_chief": ("biped", dict(skin=(58, 108, 52), hair=(210, 170, 40), torso=(125, 95, 80), sleeve=(105, 80, 70),
                                   legs=(80, 60, 50), boots=(45, 30, 25), eyes=(255, 60, 40), chest=(150, 120, 100),
                                   belt=(210, 170, 40), tusks=True)),
    "igris": ("biped", dict(skin=(120, 20, 25), helmet=(140, 18, 24), crest=(220, 190, 60), torso=(115, 14, 20),
                            sleeve=(125, 16, 22), hands=(60, 10, 12), legs=(70, 10, 14), boots=(35, 8, 10),
                            eyes=(255, 210, 210), chest=(165, 30, 35), belt=(220, 190, 60))),
    "iron": ("biped", dict(skin=(125, 130, 140), helmet=(135, 140, 150), crest=(90, 95, 105), torso=(120, 125, 135),
                           sleeve=(130, 135, 145), hands=(85, 90, 100), legs=(100, 105, 115), boots=(70, 72, 80),
                           eyes=(110, 190, 255), chest=(160, 165, 175), belt=(70, 72, 80))),
    # ---- quadruped ----
    "dire_wolf": ("quad", dict(base=(48, 54, 82), body=(60, 68, 100), face=(70, 78, 110), muzzle=(30, 34, 52),
                               eyes=(255, 220, 0))),
    "fang": ("quad", dict(base=(165, 175, 195), body=(190, 198, 215), face=(205, 212, 228), muzzle=(120, 128, 145),
                          eyes=(70, 150, 255))),
    "cerberus": ("quad", dict(base=(62, 14, 14), body=(85, 20, 18), face=(95, 25, 20), muzzle=(35, 8, 8),
                              eyes=(255, 140, 20), paws=(15, 5, 5))),
    "tank": ("quad", dict(base=(96, 66, 42), body=(110, 78, 50), face=(120, 88, 60), muzzle=(170, 140, 105),
                          eyes=(25, 18, 12), paws=(55, 36, 22))),
    # ---- insect ----
    "beru": ("insect", dict(base=(22, 20, 30), body=(34, 30, 46), face=(40, 34, 55), eyes=(255, 40, 40),
                            mandible=(90, 70, 90), legs=(45, 40, 60))),
}
PAINTERS = {"biped": paint_biped, "quad": paint_quad, "insect": paint_insect}
SHADOW_IDS = ["igris", "iron", "fang", "tank", "beru"]

for mob, (kind, pal) in MOBS.items():
    PAINTERS[kind](pal).save(os.path.join(TEX_ENTITY, mob + ".png"))
for mob in SHADOW_IDS:
    kind, pal = MOBS[mob]
    shadow_of(PAINTERS[kind], pal).save(os.path.join(TEX_ENTITY, "shadow_" + mob + ".png"))


# ----------------------------------------------------------------------------- item icons
def new_icon():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def draw_dagger(blade, edge, guard, hilt, length=8):
    img = new_icon()
    d = ImageDraw.Draw(img)
    x0, y0 = 14, 1
    for i in range(length):
        x, y = x0 - i, y0 + i
        px(img, x, y, blade)
        px(img, x - 1, y, edge)
        if i > 1:
            px(img, x, y - 1, edge)
    px(img, x0 + 0, y0 - 0, edge)
    gx, gy = x0 - length, y0 + length
    d.line([(gx - 1, gy - 2), (gx + 2, gy + 1)], fill=guard + (255,))
    d.line([(gx - 1, gy), (gx - 3, gy + 2)], fill=hilt + (255,))
    px(img, gx - 4, gy + 3, guard)
    return img


def draw_sword(blade, edge, guard, hilt):
    img = new_icon()
    d = ImageDraw.Draw(img)
    for i in range(11):
        x, y = 14 - i, 1 + i
        px(img, x, y, blade)
        px(img, x - 1, y, blade)
        px(img, x - 1, y - 1, edge)
        px(img, x + 1, y, edge)
    d.line([(2, 9), (8, 15)], fill=guard + (255,), width=1)
    d.line([(3, 9), (5, 11)], fill=guard + (255,), width=1)
    d.line([(1, 12), (2, 13)], fill=hilt + (255,), width=1)
    px(img, 0, 14, guard)
    return img


def double(img):
    out = new_icon()
    out.alpha_composite(img.transpose(Image.FLIP_LEFT_RIGHT).crop((0, 0, 16, 16)), (0, 0))
    out.alpha_composite(img, (0, 0))
    return out


WEAPON_ICONS = {
    "knight_killer": draw_dagger((190, 195, 205), (120, 125, 140), (90, 90, 100), (60, 40, 30)),
    "kasaka_fang": draw_dagger((90, 200, 110), (40, 120, 60), (60, 60, 60), (50, 35, 25)),
    "baruka_dagger": draw_dagger((150, 215, 255), (80, 140, 220), (200, 230, 255), (40, 60, 110)),
    "demon_king_dagger": double(draw_dagger((120, 40, 190), (60, 15, 110), (40, 40, 40), (20, 10, 30), 7)),
    "igris_sword": draw_sword((210, 30, 40), (120, 10, 20), (230, 200, 70), (60, 20, 20)),
    "kamish_wrath": double(draw_dagger((255, 120, 30), (200, 50, 10), (255, 220, 90), (60, 20, 10), 8)),
}
for name, icon in WEAPON_ICONS.items():
    icon.save(os.path.join(TEX_ITEM, name + ".png"))

ARMOR_MASKS = {
    "helmet": [
        "................",
        "....########....",
        "...##########...",
        "..############..",
        "..############..",
        "..##........##..",
        "..##........##..",
        "..##........##..",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "..###......###..",
        ".####......####.",
        ".##############.",
        ".##############.",
        ".##############.",
        ".##############.",
        "..############..",
        "..############..",
        "..############..",
        "..############..",
        "..############..",
        "................",
        "................",
        "................",
        "................",
    ],
    "leggings": [
        "................",
        "...##########...",
        "...##########...",
        "...##########...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "...####..####...",
        "...###....###...",
        "................",
        "................",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..####....####..",
        "..####....####..",
        "..####....####..",
        ".#####....#####.",
        ".#####....#####.",
        "................",
        "................",
        "................",
        "................",
    ],
}
ARMOR_SETS = {
    # set: (base, light, dark, trim, layer-uv-colours)
    "hunter": dict(base=(70, 85, 110), trim=(80, 200, 230), dark=(35, 42, 58)),
    "knight": dict(base=(175, 180, 190), trim=(190, 30, 40), dark=(95, 98, 108)),
    "monarch": dict(base=(28, 20, 44), trim=(150, 70, 255), dark=(12, 8, 22)),
}


def armor_icon(piece, s):
    img = new_icon()
    mask = ARMOR_MASKS[piece]
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch == "#":
                edge = (x == 0 or row[x - 1] != "#" or x == 15 or row[min(15, x + 1)] != "#"
                        or y == 0 or mask[y - 1][x] != "#" or y == 15 or mask[min(15, y + 1)][x] != "#")
                col = s["dark"] if edge else s["base"]
                if not edge and y % 4 == 1:
                    col = s["trim"]
                px(img, x, y, col)
    return img


for sname, s in ARMOR_SETS.items():
    for piece in ARMOR_MASKS:
        armor_icon(piece, s).save(os.path.join(TEX_ITEM, f"{sname}_{piece}.png"))


def gem(main, light, dark):
    img = new_icon()
    d = ImageDraw.Draw(img)
    pts = [(8, 1), (13, 6), (8, 14), (3, 6)]
    d.polygon(pts, fill=main + (255,), outline=dark + (255,))
    d.polygon([(8, 1), (13, 6), (8, 6)], fill=light + (255,))
    d.polygon([(8, 6), (13, 6), (8, 14)], fill=dark + (255,))
    px(img, 6, 5, (255, 255, 255))
    return img


gem((110, 60, 220), (190, 140, 255), (50, 20, 110)).save(os.path.join(TEX_ITEM, "magic_stone.png"))
gem((60, 160, 255), (180, 225, 255), (20, 70, 160)).save(os.path.join(TEX_ITEM, "mana_crystal.png"))

KEY_COLORS = {
    "e": (175, 175, 175), "d": (80, 190, 90), "c": (70, 130, 235),
    "b": (160, 90, 230), "a": (240, 150, 40), "s": (230, 50, 50),
}


def key_icon(col, outline=(30, 30, 30)):
    img = new_icon()
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 7, 7], outline=col + (255,), fill=None)
    d.ellipse([2, 2, 6, 6], outline=col + (255,), fill=None)
    px(img, 4, 4, (0, 0, 0))
    d.line([(6, 6), (13, 13)], fill=col + (255,))
    d.line([(7, 6), (14, 13)], fill=col + (255,))
    d.line([(10, 12), (12, 14)], fill=col + (255,))
    d.line([(12, 10), (14, 12)], fill=col + (255,))
    return img


for r, c in KEY_COLORS.items():
    key_icon(c).save(os.path.join(TEX_ITEM, f"gate_key_{r}.png"))
key_icon((200, 20, 40)).save(os.path.join(TEX_ITEM, "boss_gate_key.png"))


# ----------------------------------------------------------------------------- armor layers
def armor_layers(name, s):
    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # layer 1: head, body, arms, legs (boots)
    fill_box(l1, 0, 0, 8, 8, 8, s["base"], 8)
    hf = box_uv(0, 0, 8, 8, 8)["front"]
    fill(l1, (hf[0], hf[1] + 3, 8, 2), s["dark"], 2)
    for ex in (1, 6):
        px(l1, hf[0] + ex, hf[1] + 3, s["trim"])
    fill(l1, (box_uv(0, 0, 8, 8, 8)["top"][0] + 3, 0, 2, 8), s["trim"], 4)
    fill_box(l1, 16, 16, 8, 12, 4, s["base"], 8)
    cf = box_uv(16, 16, 8, 12, 4)["front"]
    fill(l1, (cf[0] + 3, cf[1], 2, 12), s["trim"], 4)
    fill(l1, (cf[0], cf[1] + 9, 8, 2), s["dark"], 3)
    fill_box(l1, 40, 16, 4, 12, 4, s["base"], 8)
    af = box_uv(40, 16, 4, 12, 4)
    for face in ("right", "front", "left", "back"):
        fill(l1, (af[face][0], af[face][1], af[face][2], 2), s["trim"], 4)
        fill(l1, (af[face][0], af[face][1] + 9, af[face][2], 3), s["dark"], 4)
    fill_box(l1, 0, 16, 4, 12, 4, s["base"], 8)
    lf = box_uv(0, 16, 4, 12, 4)
    for face in ("right", "front", "left", "back"):
        fill(l1, (lf[face][0], lf[face][1] + 8, lf[face][2], 4), s["dark"], 4)
    # layer 2: leggings (leg region) + belt (body region)
    fill_box(l2, 0, 16, 4, 12, 4, s["base"], 8)
    for face in ("right", "front", "left", "back"):
        fill(l2, (lf[face][0], lf[face][1], lf[face][2], 2), s["trim"], 4)
    fill_box(l2, 16, 16, 8, 12, 4, s["dark"], 6)
    bf = box_uv(16, 16, 8, 12, 4)
    for face in ("front", "back", "right", "left"):
        fill(l2, (bf[face][0], bf[face][1] + 9, bf[face][2], 3), s["trim"], 4)
    l1.save(os.path.join(TEX_ARMOR, f"{name}_layer_1.png"))
    l2.save(os.path.join(TEX_ARMOR, f"{name}_layer_2.png"))


for sname, s in ARMOR_SETS.items():
    armor_layers(sname, s)


# ----------------------------------------------------------------------------- models json
def write_json(path, obj):
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, indent=2, ensure_ascii=False)


for w in WEAPON_ICONS:
    write_json(os.path.join(MODELS_ITEM, w + ".json"),
               {"parent": "item/handheld", "textures": {"layer0": "shadowmonarch:item/" + w}})
flat = ["magic_stone", "mana_crystal", "boss_gate_key"] + [f"gate_key_{r}" for r in KEY_COLORS]
for sname in ARMOR_SETS:
    for piece in ARMOR_MASKS:
        flat.append(f"{sname}_{piece}")
for n in flat:
    write_json(os.path.join(MODELS_ITEM, n + ".json"),
               {"parent": "item/generated", "textures": {"layer0": "shadowmonarch:item/" + n}})
for mob in MOBS:
    write_json(os.path.join(MODELS_ITEM, mob + "_spawn_egg.json"), {"parent": "item/template_spawn_egg"})

# ----------------------------------------------------------------------------- recipes
def shapeless(name, ingredients, result, count=1):
    write_json(os.path.join(DATA, "recipes", name + ".json"), {
        "type": "crafting_shapeless",
        "ingredients": [{"item": i} for i in ingredients],
        "result": {"item": result, "count": count},
    })


MS = "shadowmonarch:magic_stone"
shapeless("gate_key_e", [MS] * 3 + ["minecraft:iron_ingot"], "shadowmonarch:gate_key_e")
shapeless("gate_key_d", [MS] * 4 + ["minecraft:gold_ingot"], "shadowmonarch:gate_key_d")
shapeless("gate_key_c", [MS] * 5 + ["minecraft:lapis_lazuli"], "shadowmonarch:gate_key_c")
shapeless("gate_key_b", [MS] * 6 + ["minecraft:emerald"], "shadowmonarch:gate_key_b")
shapeless("gate_key_a", [MS] * 7 + ["minecraft:diamond"], "shadowmonarch:gate_key_a")
shapeless("gate_key_s", [MS] * 8 + ["minecraft:nether_star"], "shadowmonarch:gate_key_s")
shapeless("mana_crystal", [MS, MS, "minecraft:lapis_lazuli"], "shadowmonarch:mana_crystal")

# ----------------------------------------------------------------------------- lang
lang = {
    "itemGroup.shadowmonarch": "Shadow Monarch",
    "key.categories.shadowmonarch": "Shadow Monarch",
    "key.shadowmonarch.summon": "Summon / Dismiss Shadows",
    "key.shadowmonarch.army": "Shadow Army Menu",
    "item.shadowmonarch.magic_stone": "Magic Stone",
    "item.shadowmonarch.mana_crystal": "Mana Crystal",
    "item.shadowmonarch.boss_gate_key": "Gate Master's Key",
    "item.shadowmonarch.knight_killer": "Knight Killer",
    "item.shadowmonarch.kasaka_fang": "Kasaka's Venom Fang",
    "item.shadowmonarch.baruka_dagger": "Baruka's Dagger",
    "item.shadowmonarch.demon_king_dagger": "Demon King's Daggers",
    "item.shadowmonarch.igris_sword": "Sword of the Red Knight",
    "item.shadowmonarch.kamish_wrath": "Kamish's Wrath",
}
for r, rank in zip("edcbas", ["E", "D", "C", "B", "A", "S"]):
    lang[f"item.shadowmonarch.gate_key_{r}"] = f"{rank}-Rank Gate Key"
for sname, title in (("hunter", "Hunter's"), ("knight", "Knight's"), ("monarch", "Shadow Monarch's")):
    lang[f"item.shadowmonarch.{sname}_helmet"] = f"{title} Helmet"
    lang[f"item.shadowmonarch.{sname}_chestplate"] = f"{title} Chestplate"
    lang[f"item.shadowmonarch.{sname}_leggings"] = f"{title} Leggings"
    lang[f"item.shadowmonarch.{sname}_boots"] = f"{title} Boots"
MOB_NAMES = {
    "goblin": "Goblin", "goblin_chief": "Goblin Chief", "dire_wolf": "Dire Wolf", "fang": "Fang, the Alpha",
    "cerberus": "Cerberus", "tank": "Tank, the Ice Bear", "iron": "Iron, the Knight",
    "igris": "Igris, the Blood-Red Knight", "beru": "Beru, the Ant King",
}
for mob, nm in MOB_NAMES.items():
    lang[f"entity.shadowmonarch.{mob}"] = nm
    lang[f"item.shadowmonarch.{mob}_spawn_egg"] = f"{nm} Spawn Egg"
for mob in SHADOW_IDS:
    lang[f"entity.shadowmonarch.shadow_{mob}"] = "Shadow " + MOB_NAMES[mob].split(",")[0]
write_json(os.path.join(ASSETS, "lang", "en_us.json"), lang)

print("assets generated:",
      len(os.listdir(TEX_ENTITY)), "entity textures,",
      len(os.listdir(TEX_ITEM)), "item icons,",
      len(os.listdir(TEX_ARMOR)), "armor layers,",
      len(os.listdir(MODELS_ITEM)), "item models")
