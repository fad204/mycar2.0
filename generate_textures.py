"""
Generate all variant assets for the mod:
  - 9 entity textures for cars  (256x256)
  - 9 entity textures for trucks (256x256)
  - 9 item icons for cars        (16x16)
  - 9 item icons for trucks      (16x16)
  - 1 fuel can icon              (16x16)
  - 18 item model JSONs
  - 18 recipe JSONs
  -  1 lang file

Adding a 10th variant later = adding one entry to VARIANTS + VARIANT_INGREDIENTS
+ DISPLAY_NAMES, bumping NUM_VARIANTS in Java, then rerunning this script.
"""

from PIL import Image, ImageDraw
import pathlib, json

ROOT     = pathlib.Path(__file__).resolve().parent
RES_DIR  = ROOT / "src/main/resources"
TEX_DIR  = RES_DIR / "assets/mycar/textures"
MODEL_DIR= RES_DIR / "assets/mycar/models/item"
BLOCK_MODEL_DIR = RES_DIR / "assets/mycar/models/block"
BLOCKSTATE_DIR  = RES_DIR / "assets/mycar/blockstates"
LANG_DIR = RES_DIR / "assets/mycar/lang"
RECIPE_DIR = RES_DIR / "data/mycar/recipes"

# =============================================================
# Shared (variant-independent) colors
# =============================================================
WINDOW       = (130, 180, 220, 110)   # alpha < 255 => translucent glass
WINDOW_DARK  = ( 80, 130, 180, 110)
WINDOW_TINT  = (110, 170, 215, 110)
WHEEL        = (25,  25,  25, 255)
WHEEL_RIM    = (95,  95, 100, 255)
WHEEL_HUB    = (140, 140, 145, 255)
HEADLIGHT    = (255, 245, 170, 255)
HEADLIGHT_HL = (255, 255, 230, 255)
TAILLIGHT    = (200, 30, 30, 255)
TAILLIGHT_HL = (255, 100, 100, 255)
MIRROR_GLASS = (140, 180, 220, 255)
CHASSIS_BOT  = (35,  35,  40, 255)

# =============================================================
# Variant palettes
# =============================================================
VARIANTS = {
    # ---- METAL ----
    "metal_red": {
        "body": (178, 34, 34, 255),  "body_dark": (130, 22, 22, 255),  "body_light": (210, 60, 60, 255),
        "roof": (160, 30, 30, 255),  "hood": (170, 32, 32, 255),
        "bumper": (155, 158, 165, 255), "bumper_dark": (95, 98, 105, 255),
        "grain": False, "panel_line": (90, 15, 15, 255),
    },
    "metal_black": {
        "body": (40, 40, 44, 255),   "body_dark": (15, 15, 18, 255),   "body_light": (75, 75, 80, 255),
        "roof": (32, 32, 36, 255),   "hood": (38, 38, 42, 255),
        "bumper": (155, 158, 165, 255), "bumper_dark": (95, 98, 105, 255),
        "grain": False, "panel_line": (10, 10, 12, 255),
    },
    "metal_gray": {
        "body": (130, 130, 135, 255),"body_dark": (85, 85, 90, 255),   "body_light": (170, 170, 175, 255),
        "roof": (115, 115, 120, 255),"hood": (125, 125, 130, 255),
        "bumper": (155, 158, 165, 255), "bumper_dark": (95, 98, 105, 255),
        "grain": False, "panel_line": (70, 70, 75, 255),
    },
    "metal_white": {
        "body": (225, 225, 220, 255),"body_dark": (175, 175, 170, 255),"body_light": (250, 250, 245, 255),
        "roof": (215, 215, 210, 255),"hood": (222, 222, 217, 255),
        "bumper": (155, 158, 165, 255), "bumper_dark": (95, 98, 105, 255),
        "grain": False, "panel_line": (170, 170, 165, 255),
    },
    # ---- WOOD ----
    "wooden_oak": {
        "body": (160, 130, 75, 255), "body_dark": (115, 90, 50, 255),  "body_light": (190, 160, 100, 255),
        "roof": (175, 145, 90, 255), "hood": (155, 125, 70, 255),
        "bumper": (95, 65, 30, 255), "bumper_dark": (55, 38, 18, 255),
        "grain": True, "panel_line": (110, 85, 45, 255),
    },
    "wooden_dark": {
        "body": (78, 52, 28, 255),   "body_dark": (45, 30, 15, 255),   "body_light": (108, 78, 48, 255),
        "roof": (88, 62, 35, 255),   "hood": (72, 48, 25, 255),
        "bumper": (40, 28, 14, 255), "bumper_dark": (22, 15, 8, 255),
        "grain": True, "panel_line": (50, 32, 15, 255),
    },
    "wooden_birch": {
        "body": (215, 195, 145, 255),"body_dark": (175, 155, 105, 255),"body_light": (240, 225, 180, 255),
        "roof": (220, 200, 155, 255),"hood": (212, 192, 142, 255),
        "bumper": (130, 110, 70, 255), "bumper_dark": (85, 70, 40, 255),
        "grain": True, "panel_line": (160, 140, 90, 255),
    },
    "wooden_spruce": {
        "body": (110, 80, 50, 255),  "body_dark": (75, 50, 30, 255),   "body_light": (140, 110, 75, 255),
        "roof": (120, 88, 55, 255),  "hood": (105, 76, 47, 255),
        "bumper": (60, 40, 20, 255), "bumper_dark": (35, 22, 10, 255),
        "grain": True, "panel_line": (75, 55, 30, 255),
    },
    # ---- GOLD ----
    "golden": {
        "body": (238, 195, 60, 255), "body_dark": (190, 150, 30, 255), "body_light": (255, 220, 110, 255),
        "roof": (230, 188, 50, 255), "hood": (240, 200, 70, 255),
        "bumper": (210, 165, 35, 255), "bumper_dark": (150, 110, 18, 255),
        "grain": False, "panel_line": (160, 115, 15, 255),
    },
    # ---- EMERGENCY ----
    "police": {
        "body": (25, 28, 38, 255),    "body_dark": (8, 10, 18, 255),    "body_light": (60, 65, 80, 255),
        "roof": (240, 240, 240, 255), "hood": (25, 28, 38, 255),
        "bumper": (155, 158, 165, 255), "bumper_dark": (95, 98, 105, 255),
        "grain": False, "panel_line": (5, 8, 15, 255),
        "emergency": "police",
    },
    "fire": {
        "body": (200, 25, 25, 255),   "body_dark": (140, 15, 15, 255),  "body_light": (235, 50, 50, 255),
        "roof": (185, 20, 20, 255),   "hood": (195, 23, 23, 255),
        "bumper": (50, 50, 55, 255),  "bumper_dark": (25, 25, 30, 255),
        "grain": False, "panel_line": (100, 10, 10, 255),
        "emergency": "fire",
    },
    "ambulance": {
        "body": (240, 240, 240, 255), "body_dark": (195, 195, 195, 255),"body_light": (255, 255, 255, 255),
        "roof": (225, 225, 225, 255), "hood": (235, 235, 235, 255),
        "bumper": (200, 30, 30, 255), "bumper_dark": (140, 20, 20, 255),
        "grain": False, "panel_line": (175, 175, 175, 255),
        "emergency": "ambulance",
    },
}

VARIANT_ORDER = [
    "metal_red", "metal_black", "metal_gray", "metal_white",
    "wooden_oak", "wooden_dark", "wooden_birch", "wooden_spruce",
    "golden",
    "police", "fire", "ambulance",
]

# Recipe ingredient mapping
VARIANT_INGREDIENTS = {
    "metal_red":     {"primary": "iron_ingot",      "dye": "red_dye"},
    "metal_black":   {"primary": "iron_ingot",      "dye": "black_dye"},
    "metal_gray":    {"primary": "iron_ingot",      "dye": "light_gray_dye"},
    "metal_white":   {"primary": "iron_ingot",      "dye": "white_dye"},
    "wooden_oak":    {"primary": "oak_planks"},
    "wooden_dark":   {"primary": "dark_oak_planks"},
    "wooden_birch":  {"primary": "birch_planks"},
    "wooden_spruce": {"primary": "spruce_planks"},
    "golden":        {"primary": "gold_ingot"},
    "police":        {"primary": "iron_ingot",      "dye": "blue_dye"},
    "fire":          {"primary": "iron_ingot",      "dye": "orange_dye"},
    "ambulance":     {"primary": "iron_ingot",      "dye": "lime_dye"},
}

# Display names for lang file
DISPLAY_NAMES = {
    "metal_red":     "Red Metal",
    "metal_black":   "Black Metal",
    "metal_gray":    "Gray Metal",
    "metal_white":   "White Metal",
    "wooden_oak":    "Oak",
    "wooden_dark":   "Dark Oak",
    "wooden_birch":  "Birch",
    "wooden_spruce": "Spruce",
    "golden":        "Golden",
    "police":        "Police",
    "fire":          "Fire",
    "ambulance":     "Ambulance",
}

# =============================================================
# Low-level drawing helpers
# =============================================================
def fill_rect(d, x, y, w, h, color):
    if w <= 0 or h <= 0: return
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=color)

def draw_grain(d, x, y, w, h, line_color):
    for i in range(1, h, 3):
        d.line([(x, y + i), (x + w - 1, y + i)], fill=line_color)

def edge_highlight(d, x, y, w, h, color):
    d.line([(x, y), (x + w - 1, y)], fill=color)

def edge_shadow(d, x, y, w, h, color):
    d.line([(x, y + h - 1), (x + w - 1, y + h - 1)], fill=color)

def panel_split(d, x, y, w, h, color, count=2):
    for i in range(1, count + 1):
        lx = x + (i * w) // (count + 1)
        d.line([(lx, y), (lx, y + h - 1)], fill=color)

def draw_face(d, x, y, w, h, base, *, dark=None, light=None,
              grain_color=None, highlight=True, shadow=True, panel_lines=0):
    fill_rect(d, x, y, w, h, base)
    if highlight and light is not None:
        edge_highlight(d, x, y, w, h, light)
    if shadow and dark is not None:
        edge_shadow(d, x, y, w, h, dark)
    if grain_color is not None:
        draw_grain(d, x, y, w, h, grain_color)
    if panel_lines > 0 and dark is not None:
        panel_split(d, x, y, w, h, dark, count=panel_lines)

def paint_cuboid(d, u, v, sx, sy, sz, faces):
    """faces: dict with 'top','bot','west','north','east','south' -> draw_face kwargs."""
    layout = {
        "top":   (u + sz,         v,         sx, sz),
        "bot":   (u + sz + sx,    v,         sx, sz),
        "west":  (u,              v + sz,    sz, sy),
        "north": (u + sz,         v + sz,    sx, sy),
        "east":  (u + sz + sx,    v + sz,    sz, sy),
        "south": (u + 2*sz + sx,  v + sz,    sx, sy),
    }
    for name, (fx, fy, fw, fh) in layout.items():
        cfg = faces.get(name)
        if cfg is None: continue
        draw_face(d, fx, fy, fw, fh, **cfg)

# =============================================================
# Reusable face configs
# =============================================================
def body_cfg(p, grain=None, panel_lines=0):
    return {"base": p["body"], "dark": p["body_dark"], "light": p["body_light"],
            "grain_color": grain, "panel_lines": panel_lines}

def roof_cfg(p, grain=None):
    return {"base": p["roof"], "dark": p["body_dark"], "light": p["body_light"], "grain_color": grain}

def hood_cfg(p, grain=None):
    return {"base": p["hood"], "dark": p["body_dark"], "light": p["body_light"], "grain_color": grain}

def bumper_cfg(p):
    return {"base": p["bumper"], "dark": p["bumper_dark"]}

def window_cfg():
    return {"base": WINDOW, "dark": WINDOW_DARK, "light": (170, 210, 245, 110)}

def windshield_cfg():
    return {"base": WINDOW_TINT, "dark": WINDOW_DARK, "light": (150, 200, 235, 110)}

# =============================================================
# Car entity texture (256x256) — uses CarEntityModel UV layout
# =============================================================
def paint_car(img, p):
    d = ImageDraw.Draw(img)
    grain = p["panel_line"] if p["grain"] else None

    # Body (32 x 16 x 64) at (0, 0)
    paint_cuboid(d, 0, 0, 32, 16, 64, {
        "top":   body_cfg(p, grain=grain),
        "bot":   {"base": CHASSIS_BOT, "highlight": False, "shadow": False},
        "west":  body_cfg(p, grain=grain, panel_lines=2),
        "north": {"base": p["body_dark"], "dark": CHASSIS_BOT, "light": p["body"], "grain_color": grain},
        "east":  body_cfg(p, grain=grain, panel_lines=2),
        "south": {"base": p["body_dark"], "dark": CHASSIS_BOT, "light": p["body"], "grain_color": grain},
    })

    # Cabin (28 x 12 x 32) at (0, 80)
    paint_cuboid(d, 0, 80, 28, 12, 32, {
        "top":   roof_cfg(p, grain=grain),
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  window_cfg(),
        "north": windshield_cfg(),
        "east":  window_cfg(),
        "south": windshield_cfg(),
    })

    # Hood (28 x 2 x 20) at (0, 128)
    paint_cuboid(d, 0, 128, 28, 2, 20, {
        "top":   hood_cfg(p, grain=grain),
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  body_cfg(p), "north": body_cfg(p), "east": body_cfg(p), "south": body_cfg(p),
    })

    # Roof (26 x 2 x 28) at (0, 152)
    paint_cuboid(d, 0, 152, 26, 2, 28, {
        "top":   roof_cfg(p, grain=grain),
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  roof_cfg(p), "north": roof_cfg(p), "east": roof_cfg(p), "south": roof_cfg(p),
    })

    # Front bumper (30 x 4 x 2) at (0, 184)
    paint_cuboid(d, 0, 184, 30, 4, 2, {
        "top": bumper_cfg(p), "bot": {"base": p["bumper_dark"], "highlight": False, "shadow": False},
        "west": bumper_cfg(p), "north": bumper_cfg(p), "east": bumper_cfg(p), "south": bumper_cfg(p),
    })

    # Rear bumper at (66, 184)
    paint_cuboid(d, 66, 184, 30, 4, 2, {
        "top": bumper_cfg(p), "bot": {"base": p["bumper_dark"], "highlight": False, "shadow": False},
        "west": bumper_cfg(p), "north": bumper_cfg(p), "east": bumper_cfg(p), "south": bumper_cfg(p),
    })

    # Headlights (3x3x1) — left (0,192), right (10,192)
    for u in (0, 10):
        paint_cuboid(d, u, 192, 3, 3, 1, {
            "top":   {"base": HEADLIGHT, "highlight": False, "shadow": False},
            "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
            "west":  {"base": HEADLIGHT, "highlight": False, "shadow": False},
            "north": {"base": HEADLIGHT_HL, "highlight": False, "shadow": False},
            "east":  {"base": HEADLIGHT, "highlight": False, "shadow": False},
            "south": {"base": p["body_dark"], "highlight": False, "shadow": False},
        })

    # Taillights (3x3x1) — (20,192), (30,192)
    for u in (20, 30):
        paint_cuboid(d, u, 192, 3, 3, 1, {
            "top":   {"base": TAILLIGHT, "highlight": False, "shadow": False},
            "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
            "west":  {"base": TAILLIGHT, "highlight": False, "shadow": False},
            "north": {"base": p["body_dark"], "highlight": False, "shadow": False},
            "east":  {"base": TAILLIGHT, "highlight": False, "shadow": False},
            "south": {"base": TAILLIGHT_HL, "highlight": False, "shadow": False},
        })

    # Mirrors (2x3x4) — (40,196), (54,196)
    for u in (40, 54):
        paint_cuboid(d, u, 196, 2, 3, 4, {
            "top":   {"base": p["body"], "dark": p["body_dark"]},
            "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
            "west":  {"base": MIRROR_GLASS, "highlight": False, "shadow": False},
            "north": {"base": p["body"], "highlight": False, "shadow": False},
            "east":  {"base": p["body"], "highlight": False, "shadow": False},
            "south": {"base": p["body"], "highlight": False, "shadow": False},
        })

    # Wheels at column x=200, rows 0/20/40/60. Each wheel is 4 x 8 x 12 model units,
    # so the west and east faces (the round-tire side) are 12 wide x 8 tall each.
    for v_off in (0, 20, 40, 60):
        paint_cuboid(d, 200, v_off, 4, 8, 12, {
            "top":   {"base": WHEEL, "highlight": False, "shadow": False},
            "bot":   {"base": WHEEL, "highlight": False, "shadow": False},
            "west":  {"base": WHEEL, "highlight": False, "shadow": False},
            "north": {"base": WHEEL, "highlight": False, "shadow": False},
            "east":  {"base": WHEEL, "highlight": False, "shadow": False},
            "south": {"base": WHEEL, "highlight": False, "shadow": False},
        })

        # Rim and hub: paint on BOTH west (200, v_off+12) and east (216, v_off+12)
        # so left and right wheels both show the proper outside.
        for cx in (200, 216):
            cy = v_off + 12
            d.rectangle([cx + 2, cy + 1, cx + 9, cy + 6], outline=WHEEL_RIM)
            d.rectangle([cx + 5, cy + 3, cx + 6, cy + 4], fill=WHEEL_HUB)
            d.point((cx + 5, cy + 1), fill=WHEEL_RIM)
            d.point((cx + 5, cy + 6), fill=WHEEL_RIM)
            d.point((cx + 2, cy + 3), fill=WHEEL_RIM)
            d.point((cx + 9, cy + 3), fill=WHEEL_RIM)
            # Round the corners by erasing the 4 corner pixels of each rim face.
            for dx, dy in [(0, 0), (11, 0), (0, 7), (11, 7)]:
                d.point((cx + dx, cy + dy), fill=(0, 0, 0, 0))

    # Emergency-vehicle decals (police lights, fire stripes, ambulance cross).
    if p.get("emergency"):
        paint_car_emergency_decals(d, p)

# Police / fire / ambulance markings on the car's roof + hood + sides.
# Roof TOP face is at (28, 152), size 26x28 (sx=26, sz=28).
# Hood TOP face is at (20, 128), size 28x20.
# Car BODY south side (right-side panels) at (192, 64), size 64x16.
# Car BODY north side (left-side panels) at (96, 64), size 64x16.
def paint_car_emergency_decals(d, p):
    kind = p["emergency"]
    # Roof top (where a light-bar would sit) — center it on roof
    rx0, ry0 = 28, 152
    rcx, rcy = rx0 + 13, ry0 + 14  # center of 26x28 roof
    if kind == "police":
        # Light bar: 4 blue squares left, 4 red squares right, centered.
        BLUE = (35, 70, 230, 255)
        RED  = (220, 30, 30, 255)
        # Bar runs along width (X = sx axis = 26 wide), centered on rcy.
        bar_y0, bar_y1 = rcy - 1, rcy + 1
        # Left half: blue
        d.rectangle([rcx - 5, bar_y0, rcx - 1, bar_y1], fill=BLUE)
        # Right half: red
        d.rectangle([rcx + 1, bar_y0, rcx + 5, bar_y1], fill=RED)
        # Black separator pixel
        d.point((rcx, rcy), fill=(0, 0, 0, 255))
        # Side stripes on doors: thin blue+white stripe along the body sides.
        # Body north side (left flank) at (96, 64), size 64x16.
        # Body south side (right flank) at (192, 64), size 64x16.
        for side_x in (96, 192):
            d.rectangle([side_x + 4, 70, side_x + 59, 71], fill=(255, 255, 255, 255))
            d.rectangle([side_x + 4, 72, side_x + 59, 73], fill=BLUE)
    elif kind == "fire":
        # Yellow reflective stripe across roof + along body sides.
        YELLOW = (245, 215, 55, 255)
        d.rectangle([rx0 + 2, rcy - 1, rx0 + 23, rcy + 1], fill=YELLOW)
        for side_x in (96, 192):
            d.rectangle([side_x + 4, 71, side_x + 59, 73], fill=YELLOW)
    elif kind == "ambulance":
        # Red cross on the roof.
        RED = (220, 25, 25, 255)
        # Vertical bar of the cross
        d.rectangle([rcx - 1, rcy - 3, rcx + 1, rcy + 3], fill=RED)
        # Horizontal bar of the cross
        d.rectangle([rcx - 3, rcy - 1, rcx + 3, rcy + 1], fill=RED)
        # Red stripe along body sides
        for side_x in (96, 192):
            d.rectangle([side_x + 4, 71, side_x + 59, 73], fill=RED)

    # Sunflower decal on the body back face (south) — Minecraft-style 16×16.
    # Car body south is at (160, 64) size 32×16; centered 16-wide flower
    # starts at u=160+8=168.
    draw_sunflower(d, 168, 64)

# =============================================================
# Truck entity texture (256x256) — uses TruckEntityModel UV layout
# =============================================================
def paint_truck(img, p):
    d = ImageDraw.Draw(img)
    grain = p["panel_line"] if p["grain"] else None

    # Body (32 x 16 x 96) at (0, 0) — fe1de6e baseline
    paint_cuboid(d, 0, 0, 32, 16, 96, {
        "top":   body_cfg(p, grain=grain),
        "bot":   {"base": CHASSIS_BOT, "highlight": False, "shadow": False},
        "west":  body_cfg(p, grain=grain),
        "north": {"base": p["body_dark"], "dark": CHASSIS_BOT, "light": p["body"], "grain_color": grain},
        "east":  body_cfg(p, grain=grain),
        "south": {"base": p["body_dark"], "dark": CHASSIS_BOT, "light": p["body"], "grain_color": grain},
    })

    # Body front face GRILLE: paint horizontal slats on the body's north face
    # (which is where the headlights protrude from). Body north face is at
    # (96, 96)-(128, 112), size 32x16. The grille reads as dark slats with the
    # headlights as bright accents on either side.
    GRILLE_BASE = (35, 35, 38, 255)
    GRILLE_SLAT = (15, 15, 18, 255)
    d.rectangle([96, 97, 127, 111], fill=GRILLE_BASE)
    for slat_y in (99, 101, 103, 105, 107, 109):
        d.line([(97, slat_y), (126, slat_y)], fill=GRILLE_SLAT)
    # Small center badge between slats.
    d.rectangle([110, 102, 113, 106], fill=p.get("body_light", (220, 220, 220, 255)))

    # Body Extension (32 x 16 x 24) at (112, 260) — UV shifted right to make
    # room for wider cargoExt (which is now 32 wide).
    paint_cuboid(d, 112, 260, 32, 16, 24, {
        "top":   body_cfg(p, grain=grain),
        "bot":   {"base": CHASSIS_BOT, "highlight": False, "shadow": False},
        "west":  body_cfg(p, grain=grain),
        "north": {"base": p["body_dark"], "dark": CHASSIS_BOT, "light": p["body"], "grain_color": grain},  # hidden against body
        "east":  body_cfg(p, grain=grain),
        "south": {"base": p["body_dark"], "dark": CHASSIS_BOT, "light": p["body"], "grain_color": grain},  # rear of chassis
    })

    # Cab (32 x 22 x 32) at (0, 112) — width 32 now (was 28), same as body.
    # Truck is no longer "skinny" — cab matches body chassis width.
    paint_cuboid(d, 0, 112, 32, 22, 32, {
        "top":   roof_cfg(p, grain=grain),
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  body_cfg(p, grain=grain),
        "north": windshield_cfg(),
        "east":  body_cfg(p, grain=grain),
        "south": {"base": p["body_dark"], "dark": p["body_dark"], "light": p["body"]},  # back-of-cab panel
    })

    # Side windows on cab west and east faces. With cab width 32, west face
    # is still at (0, 144)-(32, 166), but east face shifted to (64, 144)-(96, 166).
    WINDOW_GLASS = (140, 180, 220, 200)
    WINDOW_FRAME = p["body_dark"]
    for u_off in (0, 64):  # west at u=0, east at u=64 (= sz + sx = 32 + 32)
        # Filled window
        d.rectangle([u_off + 4, 147, u_off + 27, 156], fill=WINDOW_GLASS)
        # Dark frame outline
        d.rectangle([u_off + 4, 147, u_off + 27, 156], outline=WINDOW_FRAME)
        # Vertical door seam
        d.line([(u_off + 14, 144), (u_off + 14, 165)], fill=WINDOW_FRAME)

    # Cargo box (32 x 32 x 56) at (0, 172) — width 32, same as cab.
    paint_cuboid(d, 0, 172, 32, 32, 56, {
        "top":   roof_cfg(p, grain=grain),
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  body_cfg(p, grain=grain, panel_lines=3),
        "north": body_cfg(p, grain=grain),  # upper portion visible above cab roof
        "east":  body_cfg(p, grain=grain, panel_lines=3),
        "south": {"base": p["body_dark"], "dark": p["body_dark"], "light": p["body"]},  # hidden against cargoExt
    })

    # Cargo extension (32 x 32 x 24) at (0, 260) — width 32 to match cargo.
    paint_cuboid(d, 0, 260, 32, 32, 24, {
        "top":   roof_cfg(p, grain=grain),
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  body_cfg(p, grain=grain, panel_lines=1),
        "north": {"base": p["body_dark"], "dark": p["body_dark"], "light": p["body"]},  # hidden against cargo
        "east":  body_cfg(p, grain=grain, panel_lines=1),
        "south": {"base": p["body"], "dark": p["body_dark"], "light": p["body_light"],
                  "grain_color": grain, "panel_lines": 1},  # rear doors (back of truck)
    })

    # Front bumper (32 x 4 x 2) at (168, 184) — moved from v=172 to make room for bigger wheels
    paint_cuboid(d, 168, 184, 32, 4, 2, {
        "top": bumper_cfg(p), "bot": {"base": p["bumper_dark"], "highlight": False, "shadow": False},
        "west": bumper_cfg(p), "north": bumper_cfg(p), "east": bumper_cfg(p), "south": bumper_cfg(p),
    })

    # Rear bumper at (168, 190)
    paint_cuboid(d, 168, 190, 32, 4, 2, {
        "top": bumper_cfg(p), "bot": {"base": p["bumper_dark"], "highlight": False, "shadow": False},
        "west": bumper_cfg(p), "north": bumper_cfg(p), "east": bumper_cfg(p), "south": bumper_cfg(p),
    })

    # Headlights (3x3x1) — (168,196) and (176,196)
    for u in (168, 176):
        paint_cuboid(d, u, 196, 3, 3, 1, {
            "top":   {"base": HEADLIGHT, "highlight": False, "shadow": False},
            "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
            "west":  {"base": HEADLIGHT, "highlight": False, "shadow": False},
            "north": {"base": HEADLIGHT_HL, "highlight": False, "shadow": False},
            "east":  {"base": HEADLIGHT, "highlight": False, "shadow": False},
            "south": {"base": p["body_dark"], "highlight": False, "shadow": False},
        })

    # Taillights (3x3x1) — (184,196) and (192,196)
    for u in (184, 192):
        paint_cuboid(d, u, 196, 3, 3, 1, {
            "top":   {"base": TAILLIGHT, "highlight": False, "shadow": False},
            "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
            "west":  {"base": TAILLIGHT, "highlight": False, "shadow": False},
            "north": {"base": p["body_dark"], "highlight": False, "shadow": False},
            "east":  {"base": TAILLIGHT, "highlight": False, "shadow": False},
            "south": {"base": TAILLIGHT_HL, "highlight": False, "shadow": False},
        })

    # Mirrors (2x3x4) — (168,200) and (180,200)
    for u in (168, 180):
        paint_cuboid(d, u, 200, 2, 3, 4, {
            "top":   {"base": p["body"], "dark": p["body_dark"]},
            "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
            "west":  {"base": MIRROR_GLASS, "highlight": False, "shadow": False},
            "north": {"base": p["body"], "highlight": False, "shadow": False},
            "east":  {"base": p["body"], "highlight": False, "shadow": False},
            "south": {"base": p["body"], "highlight": False, "shadow": False},
        })

    # 6 Wheels (5 x 16 x 20) — dual rear axle for delivery-truck look. UV
    # positions shifted right by 8 to make room for the wider cab UV.
    for (uw, vw) in ((128, 112), (178, 112), (128, 148), (178, 148),
                     (112, 316), (162, 316)):
        paint_cuboid(d, uw, vw, 5, 16, 20, {
            "top":   {"base": WHEEL, "highlight": False, "shadow": False},
            "bot":   {"base": WHEEL, "highlight": False, "shadow": False},
            "west":  {"base": WHEEL, "highlight": False, "shadow": False},
            "north": {"base": WHEEL, "highlight": False, "shadow": False},
            "east":  {"base": WHEEL, "highlight": False, "shadow": False},
            "south": {"base": WHEEL, "highlight": False, "shadow": False},
        })
        # Rim and hub on BOTH west and east faces. West face is at (uw, vw+sz)
        # = (uw, vw+20), size sz x sy = 20 x 16. East face at (uw + sz + sx, vw+sz)
        # = (uw+25, vw+20), size 20 x 16.
        for rx in (uw, uw + 25):
            ry = vw + 20
            # Outer rim outline (16 wide, 14 tall rectangle within 20x16 face)
            d.rectangle([rx + 3, ry + 2, rx + 16, ry + 13], outline=WHEEL_RIM)
            # Center hub (small bright square)
            d.rectangle([rx + 9, ry + 7, rx + 10, ry + 8], fill=WHEEL_HUB)
            # Rim spokes (cardinal directions)
            d.point((rx + 9, ry + 2), fill=WHEEL_RIM)
            d.point((rx + 9, ry + 13), fill=WHEEL_RIM)
            d.point((rx + 3, ry + 7), fill=WHEEL_RIM)
            d.point((rx + 16, ry + 7), fill=WHEEL_RIM)
            # Round corners (4 extreme pixels of each 20x16 rim face).
            for dx, dy in [(0, 0), (19, 0), (0, 15), (19, 15)]:
                d.point((rx + dx, ry + dy), fill=(0, 0, 0, 0))

    # Emergency markings on cab roof + cab sides + cargo sides.
    if p.get("emergency"):
        paint_truck_emergency_decals(d, p)

# Truck emergency markings.
# Cab is at (0, 112), 28x16x32 → top face at (32, 112) size 28x32; west side at (0, 144)
# size 32x16; east side at (60, 144) size 32x16.
# Cargo is at (0, 172), 28x16x56 → west side at (0, 228) size 56x16; east side at (84, 228)
# size 56x16.
def draw_sunflower(d, x, y):
    """16×16 Minecraft-style sunflower decal, top-left corner at (x, y).
    Concentric rings: bright outer petals, orange transition, dark brown
    center disc. Matches the vanilla sunflower top texture in spirit."""
    PETAL_MID    = (255, 215, 50,  255)   # 'y' — outer ring of yellow petal tips
    PETAL_LIGHT  = (255, 240, 130, 255)   # 'Y' — petal highlights
    PETAL_DARK   = (220, 165, 40,  255)   # 'o' — petal core / shadow
    RIM_ORANGE   = (180, 120, 30,  255)   # 'O' — transition ring
    INNER_RING   = (130, 80,  25,  255)   # 'C' — inner orange ring around disc
    DISC         = (75,  45,  15,  255)   # 'b' — brown center disc / seeds
    colors = {'y': PETAL_MID, 'Y': PETAL_LIGHT, 'o': PETAL_DARK,
              'O': RIM_ORANGE, 'C': INNER_RING, 'b': DISC}
    pattern = [
        "................",  # 0
        "......yyyy......",  # 1
        "....yYYYYYYy....",  # 2
        "...yYooooooYy...",  # 3
        "..yYoOOOOOOoYy..",  # 4
        ".yYoOCCCCCCOoYy.",  # 5
        ".YoOCbbbbbbCOoY.",  # 6
        ".YoObbbbbbbbOoY.",  # 7
        ".YoObbbbbbbbOoY.",  # 8
        ".YoOCbbbbbbCOoY.",  # 9
        ".yYoOCCCCCCOoYy.",  # 10
        "..yYoOOOOOOoYy..",  # 11
        "...yYooooooYy...",  # 12
        "....yYYYYYYy....",  # 13
        "......yyyy......",  # 14
        "................",  # 15
    ]
    for row, line in enumerate(pattern):
        for col, ch in enumerate(line):
            if ch != '.':
                d.point((x + col, y + row), fill=colors[ch])


def paint_truck_emergency_decals(d, p):
    kind = p["emergency"]
    rx0, ry0 = 32, 112
    # Cab top face is now 32x32 (sx=32). Center is at rx0+16, ry0+16.
    rcx, rcy = rx0 + 16, ry0 + 16
    # Side-face east-U positions for the wider sx=32 cuboids:
    # cab east at u = sz + sx = 32 + 32 = 64
    # cargo east at u = sz + sx = 56 + 32 = 88
    # cargoExt east at u = sz + sx = 24 + 32 = 56
    if kind == "police":
        BLUE = (35, 70, 230, 255)
        RED  = (220, 30, 30, 255)
        WHITE = (255, 255, 255, 255)
        # Light bar on cab roof
        d.rectangle([rcx - 5, rcy - 1, rcx - 1, rcy + 1], fill=BLUE)
        d.rectangle([rcx + 1, rcy - 1, rcx + 5, rcy + 1], fill=RED)
        d.point((rcx, rcy), fill=(0, 0, 0, 255))
        # White + blue stripes on both sides of the cab and cargo.
        for side_x in (0, 64):
            d.rectangle([side_x + 3, 152, side_x + 28, 153], fill=WHITE)
            d.rectangle([side_x + 3, 154, side_x + 28, 155], fill=BLUE)
        for side_x in (0, 88):
            d.rectangle([side_x + 3, 234, side_x + 52, 235], fill=WHITE)
            d.rectangle([side_x + 3, 236, side_x + 52, 237], fill=BLUE)
        # Extend stripes onto cargoExt west(u=0)/east(u=56)
        for side_x in (0, 56):
            d.rectangle([side_x + 2, 284, side_x + 22, 285], fill=WHITE)
            d.rectangle([side_x + 2, 286, side_x + 22, 287], fill=BLUE)
    elif kind == "fire":
        YELLOW = (245, 215, 55, 255)
        # Yellow stripe along cab roof (cab top is now 32 wide)
        d.rectangle([rx0 + 2, rcy - 1, rx0 + 29, rcy + 1], fill=YELLOW)
        for side_x in (0, 64):
            d.rectangle([side_x + 3, 153, side_x + 28, 155], fill=YELLOW)
        for side_x in (0, 88):
            d.rectangle([side_x + 3, 235, side_x + 52, 237], fill=YELLOW)
        for side_x in (0, 56):
            d.rectangle([side_x + 2, 285, side_x + 22, 287], fill=YELLOW)
    elif kind == "ambulance":
        RED = (220, 25, 25, 255)
        # Big red cross on cab roof
        d.rectangle([rcx - 1, rcy - 4, rcx + 1, rcy + 4], fill=RED)
        d.rectangle([rcx - 4, rcy - 1, rcx + 4, rcy + 1], fill=RED)
        # Red side stripes on cab and cargo
        for side_x in (0, 64):
            d.rectangle([side_x + 3, 153, side_x + 28, 155], fill=RED)
        for side_x in (0, 88):
            d.rectangle([side_x + 3, 235, side_x + 52, 237], fill=RED)
        for side_x in (0, 56):
            d.rectangle([side_x + 2, 285, side_x + 22, 287], fill=RED)

    # Sunflower decal on the BACK of truck — cargoExt's south face. With
    # cargoExt now 32 wide (sx=32, sz=24), south face is at
    # (u + 2*sz + sx, v + sz) = (0 + 48 + 32, 260 + 24) = (80, 284), size 32x32.
    # 16-wide sunflower centered: u = 80 + (32-16)/2 = 88, v = 284 + (32-16)/2 = 292.
    draw_sunflower(d, 88, 292)

# =============================================================
# Bicycle entity texture (128x64) — uses BicycleEntityModel UV layout
# =============================================================
def paint_bicycle(img, p):
    d = ImageDraw.Draw(img)
    grain = p["panel_line"] if p["grain"] else None

    # Wheels (2 x 12 x 12) at u_off (0 = front, 28 = rear).
    # West face at (u_off, 12) size (12, 12); east face at (u_off+14, 12) size (12, 12).
    for u_off in (0, 28):
        paint_cuboid(d, u_off, 0, 2, 12, 12, {
            "top":   {"base": WHEEL, "highlight": False, "shadow": False},
            "bot":   {"base": WHEEL, "highlight": False, "shadow": False},
            "west":  {"base": WHEEL, "highlight": False, "shadow": False},
            "north": {"base": WHEEL, "highlight": False, "shadow": False},
            "east":  {"base": WHEEL, "highlight": False, "shadow": False},
            "south": {"base": WHEEL, "highlight": False, "shadow": False},
        })
        # Rim + spokes on BOTH west and east faces so both sides look right.
        for rx in (u_off, u_off + 14):
            ry = 12
            d.rectangle([rx + 1, ry + 1, rx + 10, ry + 10], outline=WHEEL_RIM)
            d.rectangle([rx + 5, ry + 5, rx + 6, ry + 6], fill=WHEEL_HUB)
            d.point((rx + 5, ry + 2), fill=WHEEL_RIM)
            d.point((rx + 5, ry + 9), fill=WHEEL_RIM)
            d.point((rx + 2, ry + 5), fill=WHEEL_RIM)
            d.point((rx + 9, ry + 5), fill=WHEEL_RIM)
            d.point((rx + 3, ry + 3), fill=WHEEL_RIM)
            d.point((rx + 8, ry + 8), fill=WHEEL_RIM)
            d.point((rx + 8, ry + 3), fill=WHEEL_RIM)
            d.point((rx + 3, ry + 8), fill=WHEEL_RIM)
            # Round corners of the 12x12 rim face.
            for dx, dy in [(0, 0), (11, 0), (0, 11), (11, 11)]:
                d.point((rx + dx, ry + dy), fill=(0, 0, 0, 0))

    # Top tube (2 x 2 x 16) at (56, 0)
    paint_cuboid(d, 56, 0, 2, 2, 16, {
        "top":   body_cfg(p, grain=grain),
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  body_cfg(p, grain=grain),
        "north": body_cfg(p),
        "east":  body_cfg(p, grain=grain),
        "south": body_cfg(p),
    })

    # Seat post (2 x 6 x 2) at (92, 0)
    paint_cuboid(d, 92, 0, 2, 6, 2, {
        "top":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  body_cfg(p),
        "north": body_cfg(p),
        "east":  body_cfg(p),
        "south": body_cfg(p),
    })

    # Saddle (4 x 1 x 6) at (100, 0) — black, like a real saddle
    SADDLE = (35, 30, 25, 255)
    SADDLE_DARK = (15, 12, 10, 255)
    SADDLE_LIGHT = (60, 50, 40, 255)
    paint_cuboid(d, 100, 0, 4, 1, 6, {
        "top":   {"base": SADDLE, "dark": SADDLE_DARK, "light": SADDLE_LIGHT},
        "bot":   {"base": SADDLE_DARK, "highlight": False, "shadow": False},
        "west":  {"base": SADDLE, "highlight": False, "shadow": False},
        "north": {"base": SADDLE, "highlight": False, "shadow": False},
        "east":  {"base": SADDLE, "highlight": False, "shadow": False},
        "south": {"base": SADDLE, "highlight": False, "shadow": False},
    })

    # Stem (2 x 4 x 2) at (0, 28)
    paint_cuboid(d, 0, 28, 2, 4, 2, {
        "top":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  body_cfg(p),
        "north": body_cfg(p),
        "east":  body_cfg(p),
        "south": body_cfg(p),
    })

    # Handlebars (8 x 1 x 2) at (8, 28) — black for grip feel
    HANDLE = (30, 30, 32, 255)
    paint_cuboid(d, 8, 28, 8, 1, 2, {
        "top":   {"base": HANDLE, "highlight": False, "shadow": False},
        "bot":   {"base": HANDLE, "highlight": False, "shadow": False},
        "west":  {"base": HANDLE, "highlight": False, "shadow": False},
        "north": {"base": HANDLE, "highlight": False, "shadow": False},
        "east":  {"base": HANDLE, "highlight": False, "shadow": False},
        "south": {"base": HANDLE, "highlight": False, "shadow": False},
    })


def make_bicycle_item_icon(p, out_path):
    """Side view bike, 16x16."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Frame (top tube)
    d.rectangle([4, 9, 11, 10], fill=p["body"])

    # Seat post + saddle
    d.rectangle([10, 7, 10, 9], fill=p["body_dark"])
    d.rectangle([9, 6, 12, 7], fill=(35, 30, 25, 255))

    # Stem + handlebars
    d.rectangle([5, 7, 5, 9], fill=p["body_dark"])
    d.rectangle([4, 6, 7, 7], fill=(30, 30, 32, 255))

    # Front + rear wheels (rings of black with hub)
    for cx in (3, 12):
        d.ellipse([cx - 2, 10, cx + 2, 14], outline=WHEEL, fill=None)
        d.point((cx, 12), fill=WHEEL_RIM)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)


def make_car_item_icon(p, out_path):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    grain_col = p["panel_line"] if p["grain"] else None

    # Body
    d.rectangle([1, 9, 14, 12], fill=p["body"])
    d.rectangle([1, 9, 14, 9], fill=p["body_light"])
    d.rectangle([1, 12, 14, 12], fill=p["body_dark"])

    # Cabin
    d.rectangle([4, 5, 11, 9], fill=p["roof"])
    d.rectangle([5, 6, 10, 8], fill=WINDOW)

    # Wood grain
    if grain_col:
        d.line([(1, 10), (14, 10)], fill=grain_col)
        d.line([(1, 11), (14, 11)], fill=grain_col)

    # Bumpers
    d.point((0, 11), fill=p["bumper"])
    d.point((0, 12), fill=p["bumper"])
    d.point((15, 11), fill=p["bumper"])
    d.point((15, 12), fill=p["bumper"])

    # Wheels
    d.rectangle([2, 13, 4, 14], fill=WHEEL)
    d.rectangle([11, 13, 13, 14], fill=WHEEL)
    d.point((3, 13), fill=WHEEL_RIM)
    d.point((12, 13), fill=WHEEL_RIM)

    # Lights
    d.point((1, 10), fill=HEADLIGHT_HL)
    d.point((14, 10), fill=TAILLIGHT)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)

def make_truck_item_icon(p, out_path):
    """A side-on truck silhouette: long body, cab forward, cargo box behind."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    grain_col = p["panel_line"] if p["grain"] else None

    # Main body strip
    d.rectangle([1, 10, 14, 12], fill=p["body"])
    d.rectangle([1, 10, 14, 10], fill=p["body_light"])
    d.rectangle([1, 12, 14, 12], fill=p["body_dark"])

    # Cab (front, smaller box)
    d.rectangle([1, 6, 5, 10], fill=p["roof"])
    d.rectangle([2, 7, 4, 9], fill=WINDOW)

    # Cargo box (back, larger box)
    d.rectangle([5, 4, 14, 10], fill=p["body"])
    d.rectangle([5, 4, 14, 4], fill=p["body_light"])
    d.rectangle([5, 10, 14, 10], fill=p["body_dark"])

    if grain_col:
        d.line([(5, 6), (14, 6)], fill=grain_col)
        d.line([(5, 8), (14, 8)], fill=grain_col)
        d.line([(1, 11), (14, 11)], fill=grain_col)

    # Bumpers
    d.point((0, 11), fill=p["bumper"])
    d.point((0, 12), fill=p["bumper"])
    d.point((15, 11), fill=p["bumper"])
    d.point((15, 12), fill=p["bumper"])

    # Wheels (3 axles for trucky feel — front, mid, rear)
    d.rectangle([1, 13, 3, 14], fill=WHEEL)
    d.rectangle([12, 13, 14, 14], fill=WHEEL)
    d.point((2, 13), fill=WHEEL_RIM)
    d.point((13, 13), fill=WHEEL_RIM)

    # Lights
    d.point((1, 11), fill=HEADLIGHT_HL)
    d.point((14, 11), fill=TAILLIGHT)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)

# =============================================================
# Fuel can icon (unchanged from original)
# =============================================================
def make_fuel_can_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    CAN_RED = (170, 30, 30, 255)
    CAN_DARK = (110, 20, 20, 255)
    CAN_LIGHT = (210, 60, 60, 255)
    CAP_BLK = (35, 35, 35, 255)
    STRIPE = (240, 200, 60, 255)

    d.rectangle([3, 4, 12, 14], fill=CAN_RED)
    d.line([(3, 4), (12, 4)], fill=CAN_LIGHT)
    d.line([(3, 14), (12, 14)], fill=CAN_DARK)
    d.line([(3, 4), (3, 14)], fill=CAN_DARK)
    d.line([(12, 4), (12, 14)], fill=CAN_DARK)
    d.rectangle([6, 2, 9, 4], fill=CAP_BLK)
    d.rectangle([10, 3, 12, 4], fill=CAN_DARK)
    d.rectangle([4, 9, 11, 10], fill=STRIPE)
    d.point((11, 13), fill=(60, 60, 60, 255))

    out = TEX_DIR / "item/fuel_can.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)

# =============================================================
# JSON generation
# =============================================================
def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)

def generate_item_models():
    """Write item model JSON for each variant of car, truck, and bicycle."""
    for variant in VARIANT_ORDER:
        for vehicle in ("car", "truck", "bicycle"):
            obj = {
                "parent": "item/generated",
                "textures": {"layer0": f"mycar:item/{vehicle}_{variant}"}
            }
            write_json(MODEL_DIR / f"{vehicle}_{variant}.json", obj)

def generate_recipes():
    """Write recipe JSONs. Car uses saddle in middle; truck uses chest in middle."""
    for variant in VARIANT_ORDER:
        ingr = VARIANT_INGREDIENTS[variant]
        primary = ingr["primary"]
        dye = ingr.get("dye")

        if dye:
            pattern = ["III", "I{}I", "WDW"]
            key_base = {
                "I": {"item": f"minecraft:{primary}"},
                "W": {"item": "minecraft:black_wool"},
                "D": {"item": f"minecraft:{dye}"},
            }
        else:
            pattern = ["III", "I{}I", "W W"]
            key_base = {
                "I": {"item": f"minecraft:{primary}"},
                "W": {"item": "minecraft:black_wool"},
            }

        # ---- Car: middle slot = saddle ----
        car_pattern = [row.format("S") for row in pattern]
        car_key = dict(key_base)
        car_key["S"] = {"item": "minecraft:saddle"}
        write_json(RECIPE_DIR / f"car_{variant}.json", {
            "type": "minecraft:crafting_shaped",
            "pattern": car_pattern,
            "key": car_key,
            "result": {"item": f"mycar:car_{variant}", "count": 1},
        })

        # ---- Truck: middle slot = chest ----
        truck_pattern = [row.format("C") for row in pattern]
        truck_key = dict(key_base)
        truck_key["C"] = {"item": "minecraft:chest"}
        write_json(RECIPE_DIR / f"truck_{variant}.json", {
            "type": "minecraft:crafting_shaped",
            "pattern": truck_pattern,
            "key": truck_key,
            "result": {"item": f"mycar:truck_{variant}", "count": 1},
        })

        # ---- Bicycle: simpler 3-row pattern, dye for metals / stick for others ----
        #   P P
        #   PSP    P=primary (iron/planks/gold), S=saddle, I=dye (metal) or stick (other)
        #   _I_
        bike_key = {
            "P": {"item": f"minecraft:{primary}"},
            "S": {"item": "minecraft:saddle"},
        }
        if dye:
            bike_key["I"] = {"item": f"minecraft:{dye}"}
        else:
            bike_key["I"] = {"item": "minecraft:stick"}
        write_json(RECIPE_DIR / f"bicycle_{variant}.json", {
            "type": "minecraft:crafting_shaped",
            "pattern": ["P P", "PSP", " I "],
            "key": bike_key,
            "result": {"item": f"mycar:bicycle_{variant}", "count": 1},
        })

def generate_lang():
    """Write the en_us.json lang file with all variant names + entity + keybind labels."""
    obj = {}
    for variant in VARIANT_ORDER:
        dn = DISPLAY_NAMES[variant]
        obj[f"item.mycar.car_{variant}"]     = f"{dn} Car"
        obj[f"item.mycar.truck_{variant}"]   = f"{dn} Truck"
        obj[f"item.mycar.bicycle_{variant}"] = f"{dn} Bicycle"
    obj["item.mycar.fuel_can"]      = "Fuel Can"
    obj["entity.mycar.car"]         = "Car"
    obj["entity.mycar.truck"]       = "Truck"
    obj["entity.mycar.bicycle"]     = "Bicycle"
    obj["entity.mycar.truck.cargo"] = "Truck Cargo"
    obj["key.mycar.gear_up"]        = "Shift Gear Up"
    obj["key.mycar.gear_down"]      = "Shift Gear Down"
    obj["key.mycar.handbrake"]      = "Handbrake"
    obj["category.mycar.driving"]   = "MyCar: Driving"
    add_toll_lang_entries(obj)
    write_json(LANG_DIR / "en_us.json", obj)

# =============================================================
# Main
# =============================================================
# =============================================================
# Toll system: RFC coins + toll camera block
# =============================================================
COIN_PALETTES = {
    5: {  # iron nugget — silver coin
        "face":  (200, 205, 215, 255),
        "edge":  (95, 100, 115, 255),
        "shine": (240, 245, 250, 255),
        "label": (50, 55, 70, 255),
    },
    10: {  # iron ingot — brighter silver
        "face":  (215, 220, 230, 255),
        "edge":  (110, 115, 130, 255),
        "shine": (250, 252, 255, 255),
        "label": (55, 60, 75, 255),
    },
    50: {  # gold nugget — pale gold
        "face":  (240, 200, 95, 255),
        "edge":  (150, 110, 35, 255),
        "shine": (255, 235, 165, 255),
        "label": (95, 60, 15, 255),
    },
    100: {  # gold ingot — rich gold
        "face":  (255, 215, 70, 255),
        "edge":  (160, 115, 25, 255),
        "shine": (255, 240, 145, 255),
        "label": (105, 65, 15, 255),
    },
    500: {  # emerald — green
        "face":  (80, 200, 115, 255),
        "edge":  (30, 110, 60, 255),
        "shine": (170, 245, 190, 255),
        "label": (15, 65, 35, 255),
    },
    1000: {  # diamond — cyan
        "face":  (140, 230, 240, 255),
        "edge":  (45, 130, 165, 255),
        "shine": (210, 250, 255, 255),
        "label": (20, 75, 100, 255),
    },
}

# 3-wide × 5-tall pixel font for coin denomination labels. Each glyph row
# uses 'X' for filled and '.' for empty. Compact, no descenders.
COIN_DIGIT_FONT = {
    '0': ['XXX', 'X.X', 'X.X', 'X.X', 'XXX'],
    '1': ['.X.', 'XX.', '.X.', '.X.', 'XXX'],
    '2': ['XXX', '..X', 'XXX', 'X..', 'XXX'],
    '3': ['XXX', '..X', '.XX', '..X', 'XXX'],
    '4': ['X.X', 'X.X', 'XXX', '..X', '..X'],
    '5': ['XXX', 'X..', 'XXX', '..X', 'XXX'],
    '6': ['XXX', 'X..', 'XXX', 'X.X', 'XXX'],
    '7': ['XXX', '..X', '.X.', 'X..', 'X..'],
    '8': ['XXX', 'X.X', 'XXX', 'X.X', 'XXX'],
    '9': ['XXX', 'X.X', 'XXX', '..X', 'XXX'],
    'H': ['X.X', 'X.X', 'XXX', 'X.X', 'X.X'],
    'K': ['X.X', 'X.X', 'XX.', 'X.X', 'X.X'],
}

# Coin face labels. Full numeric "100"/"500" are too wide at 3-pixel font;
# "1H"/"5H" (one hundred / five hundred) and "1K" (one thousand) are the
# legible abbreviations that fit on the 12-pixel coin face.
COIN_LABELS = {
    5:    "5",
    10:   "10",
    50:   "50",
    100:  "1H",
    500:  "5H",
    1000: "1K",
}

def _draw_coin_label(img, text, color):
    """Draw `text` centered on the 16x16 coin face using the 3x5 pixel font,
    with 1-pixel inter-glyph spacing for readability."""
    n = len(text)
    width = n * 3 + (n - 1)  # 3 wide glyphs + 1px spacing
    start_x = (16 - width) // 2
    start_y = 6
    for ci, ch in enumerate(text):
        glyph = COIN_DIGIT_FONT.get(ch)
        if glyph is None:
            continue
        for ry in range(5):
            row = glyph[ry]
            for cx in range(3):
                if row[cx] == 'X':
                    img.putpixel((start_x + ci * 4 + cx, start_y + ry), color)

def make_coin_icon(denom, palette, out_path):
    """16x16 coin with discrete edge ring, inner face, shine quadrant, and
    pixel-font denomination label."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Outer rim — 14×14 disc filled with the dark edge color.
    d.ellipse([1, 1, 14, 14], fill=palette["edge"])
    # Inner face — 12×12 disc on top of the rim.
    d.ellipse([2, 2, 13, 13], fill=palette["face"])
    # Shine quadrant in the upper-left (slight 3D effect).
    d.ellipse([3, 3, 7, 7], fill=palette["shine"])
    # Soften the shine boundary so it doesn't look like a separate sticker.
    img.putpixel((7, 4), palette["face"])
    img.putpixel((4, 7), palette["face"])
    # Darker pixels bottom-right give depth opposite the shine.
    img.putpixel((11, 11), palette["edge"])
    img.putpixel((12, 10), palette["edge"])
    img.putpixel((10, 12), palette["edge"])
    img.putpixel((11, 10), palette["edge"])
    img.putpixel((10, 11), palette["edge"])
    # Denomination label centered, drawn last so it stays on top of shine/rim.
    _draw_coin_label(img, COIN_LABELS[denom], palette["label"])
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)

def make_toll_camera_lens(out_path):
    """The downward-facing camera lens — concentric circles with a red 'active' dot."""
    img = Image.new("RGBA", (16, 16), (55, 58, 65, 255))   # dark casing
    d = ImageDraw.Draw(img)
    # Frame border
    d.rectangle([0, 0, 15, 15], outline=(35, 38, 45, 255))
    # Lens housing (large dark circle)
    d.ellipse([2, 2, 13, 13], fill=(25, 27, 32, 255), outline=(15, 16, 20, 255))
    # Lens glass (inner)
    d.ellipse([5, 5, 10, 10], fill=(18, 20, 26, 255))
    # Pupil reflection / shine
    img.putpixel((6, 6), (140, 150, 175, 255))
    img.putpixel((7, 6), (90, 100, 130, 255))
    # Active-light LED (small red dot in corner)
    img.putpixel((2, 13), (220, 30, 30, 255))
    img.putpixel((3, 13), (180, 25, 25, 255))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)

def make_toll_camera_side(out_path):
    """Side face — gray casing with mounting bands top and bottom."""
    img = Image.new("RGBA", (16, 16), (95, 100, 108, 255))
    d = ImageDraw.Draw(img)
    # Top + bottom darker bands (mounting brackets)
    d.rectangle([0, 0, 15, 2], fill=(70, 74, 82, 255))
    d.rectangle([0, 13, 15, 15], fill=(70, 74, 82, 255))
    # Side rivets
    for x in (2, 13):
        img.putpixel((x, 1), (40, 42, 48, 255))
        img.putpixel((x, 14), (40, 42, 48, 255))
    # Vertical seam down the middle
    for y in range(3, 13):
        img.putpixel((7, y), (75, 80, 86, 255))
        img.putpixel((8, y), (115, 120, 128, 255))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)

def make_toll_camera_top(out_path):
    """Top face — mounting plate (this is what touches the ceiling block above)."""
    img = Image.new("RGBA", (16, 16), (115, 120, 128, 255))
    d = ImageDraw.Draw(img)
    # Border
    d.rectangle([0, 0, 15, 15], outline=(75, 80, 88, 255))
    # Four corner mounting screws
    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        img.putpixel((x, y), (40, 42, 48, 255))
        img.putpixel((x + 1, y), (60, 64, 72, 255))
        img.putpixel((x, y + 1), (60, 64, 72, 255))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)

# RFC: coin item ↔ vanilla item used to craft it (1:1 in both directions).
COIN_VANILLA_SOURCE = {
    5:    "iron_nugget",
    10:   "iron_ingot",
    50:   "gold_nugget",
    100:  "gold_ingot",
    500:  "emerald",
    1000: "diamond",
}

def generate_toll_assets():
    """Textures, models, blockstate, recipes, and lang entries for the toll system."""

    # ---- Coin textures + item models + 1:1 recipes both directions ----
    for denom, palette in COIN_PALETTES.items():
        coin_id   = f"rfc_{denom}"
        coin_path = TEX_DIR / f"item/{coin_id}.png"
        make_coin_icon(denom, palette, coin_path)

        # Item model — standard "generated" 2D icon
        write_json(MODEL_DIR / f"{coin_id}.json", {
            "parent": "item/generated",
            "textures": {"layer0": f"mycar:item/{coin_id}"},
        })

        # Recipe: 1 vanilla source → 1 coin
        vanilla = COIN_VANILLA_SOURCE[denom]
        write_json(RECIPE_DIR / f"{coin_id}.json", {
            "type": "minecraft:crafting_shapeless",
            "ingredients": [{"item": f"minecraft:{vanilla}"}],
            "result": {"item": f"mycar:{coin_id}", "count": 1},
        })

        # Reverse recipe: 1 coin → 1 vanilla source (refund / melt)
        write_json(RECIPE_DIR / f"{coin_id}_to_{vanilla}.json", {
            "type": "minecraft:crafting_shapeless",
            "ingredients": [{"item": f"mycar:{coin_id}"}],
            "result": {"item": f"minecraft:{vanilla}", "count": 1},
        })

    # ---- Toll camera block textures ----
    make_toll_camera_lens(TEX_DIR / "block/toll_camera_lens.png")
    make_toll_camera_side(TEX_DIR / "block/toll_camera_side.png")
    make_toll_camera_top (TEX_DIR / "block/toll_camera_top.png")

    # ---- Block model: per-face textures, lens on the bottom ----
    write_json(BLOCK_MODEL_DIR / "toll_camera.json", {
        "parent": "block/cube",
        "textures": {
            "particle": "mycar:block/toll_camera_side",
            "down":     "mycar:block/toll_camera_lens",
            "up":       "mycar:block/toll_camera_top",
            "north":    "mycar:block/toll_camera_side",
            "south":    "mycar:block/toll_camera_side",
            "east":     "mycar:block/toll_camera_side",
            "west":     "mycar:block/toll_camera_side",
        },
    })

    # ---- Blockstate: single variant, no rotation (for now). ----
    write_json(BLOCKSTATE_DIR / "toll_camera.json", {
        "variants": {"": {"model": "mycar:block/toll_camera"}},
    })

    # ---- Item form of the block (uses the block model) ----
    write_json(MODEL_DIR / "toll_camera.json", {
        "parent": "mycar:block/toll_camera",
    })

    # ---- Toll camera recipe ----
    # 7 iron + 1 observer + 1 redstone — mid-tier infrastructure cost.
    write_json(RECIPE_DIR / "toll_camera.json", {
        "type": "minecraft:crafting_shaped",
        "pattern": ["III", "IOI", "IRI"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "O": {"item": "minecraft:observer"},
            "R": {"item": "minecraft:redstone"},
        },
        "result": {"item": "mycar:toll_camera", "count": 1},
    })

    # ============================================================
    # Speed camera — same shape as toll camera but red lens & body
    # ============================================================
    make_speed_camera_lens(TEX_DIR / "block/speed_camera_lens.png")
    make_speed_camera_side(TEX_DIR / "block/speed_camera_side.png")
    make_speed_camera_top (TEX_DIR / "block/speed_camera_top.png")

    write_json(BLOCK_MODEL_DIR / "speed_camera.json", {
        "parent": "block/cube",
        "textures": {
            "particle": "mycar:block/speed_camera_side",
            "down":     "mycar:block/speed_camera_lens",
            "up":       "mycar:block/speed_camera_top",
            "north":    "mycar:block/speed_camera_side",
            "south":    "mycar:block/speed_camera_side",
            "east":     "mycar:block/speed_camera_side",
            "west":     "mycar:block/speed_camera_side",
        },
    })
    write_json(BLOCKSTATE_DIR / "speed_camera.json", {
        "variants": {"": {"model": "mycar:block/speed_camera"}},
    })
    write_json(MODEL_DIR / "speed_camera.json", {
        "parent": "mycar:block/speed_camera",
    })

    # Speed camera recipe — uses a REDSTONE BLOCK (vs dust for toll) to
    # visually differentiate at the crafting bench.
    write_json(RECIPE_DIR / "speed_camera.json", {
        "type": "minecraft:crafting_shaped",
        "pattern": ["III", "IOI", "IBI"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "O": {"item": "minecraft:observer"},
            "B": {"item": "minecraft:redstone_block"},
        },
        "result": {"item": "mycar:speed_camera", "count": 1},
    })

    # ============================================================
    # RFC Registry — single book-like item, prints plate ledger
    # ============================================================
    make_rfc_registry_icon(TEX_DIR / "item/rfc_registry.png")
    write_json(MODEL_DIR / "rfc_registry.json", {
        "parent": "item/generated",
        "textures": {"layer0": "mycar:item/rfc_registry"},
    })
    # Shapeless: 1 book + 1 RFC 10 coin → 1 registry
    write_json(RECIPE_DIR / "rfc_registry.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "minecraft:book"},
            {"item": "mycar:rfc_10"},
        ],
        "result": {"item": "mycar:rfc_registry", "count": 1},
    })

def make_speed_camera_lens(out_path):
    """Speed-camera lens — red-tinted housing with a glowing red center
    (radar-gun aesthetic) so players distinguish at a glance from toll camera."""
    img = Image.new("RGBA", (16, 16), (75, 32, 32, 255))  # dark red casing
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(45, 18, 18, 255))
    # Lens housing (dark)
    d.ellipse([2, 2, 13, 13], fill=(30, 12, 12, 255), outline=(15, 5, 5, 255))
    # Inner glowing red lens
    d.ellipse([5, 5, 10, 10], fill=(220, 30, 30, 255))
    # Hot center
    img.putpixel((7, 7), (255, 200, 200, 255))
    img.putpixel((8, 7), (255, 120, 120, 255))
    img.putpixel((7, 8), (255, 120, 120, 255))
    # Yellow corner warning indicator
    img.putpixel((2, 2), (255, 220, 60, 255))
    img.putpixel((13, 13), (255, 220, 60, 255))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)

def make_speed_camera_side(out_path):
    """Side face — gray with red and yellow warning stripes."""
    img = Image.new("RGBA", (16, 16), (95, 100, 108, 255))
    d = ImageDraw.Draw(img)
    # Top + bottom darker mounting bands
    d.rectangle([0, 0, 15, 2], fill=(70, 74, 82, 255))
    d.rectangle([0, 13, 15, 15], fill=(70, 74, 82, 255))
    # Red warning stripe across the middle
    d.rectangle([0, 6, 15, 9], fill=(160, 50, 50, 255))
    # Diagonal yellow hazard slashes within the red stripe
    for x in range(0, 16, 4):
        for dy in range(3):
            xi = (x + dy) % 16
            img.putpixel((xi, 6 + dy), (255, 220, 60, 255))
    # Rivets
    for x in (2, 13):
        img.putpixel((x, 1), (40, 42, 48, 255))
        img.putpixel((x, 14), (40, 42, 48, 255))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)

def make_speed_camera_top(out_path):
    """Top face — matches toll camera's mounting plate (this is what touches the ceiling)."""
    img = Image.new("RGBA", (16, 16), (115, 120, 128, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(75, 80, 88, 255))
    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        img.putpixel((x, y), (40, 42, 48, 255))
        img.putpixel((x + 1, y), (60, 64, 72, 255))
        img.putpixel((x, y + 1), (60, 64, 72, 255))
    # Subtle red corner accent to mirror the lens color from below
    img.putpixel((7, 7), (160, 50, 50, 255))
    img.putpixel((8, 8), (160, 50, 50, 255))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)

def make_rfc_registry_icon(out_path):
    """Closed book icon, brown cover with gold RFC seal in the center."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Book body — brown leather rectangle
    d.rectangle([2, 2, 13, 14], fill=(120, 70, 35, 255), outline=(70, 40, 20, 255))
    # Spine on the left
    d.rectangle([2, 2, 4, 14], fill=(90, 50, 25, 255))
    # Page edges on the right side (lighter strip)
    d.rectangle([12, 3, 13, 13], fill=(240, 230, 200, 255))
    for y in (5, 8, 11):
        img.putpixel((12, y), (200, 190, 160, 255))
    # Gold seal — small disc centered on the cover, suggests an RFC coin
    d.ellipse([6, 6, 10, 10], fill=(255, 215, 70, 255), outline=(160, 115, 25, 255))
    img.putpixel((7, 7), (255, 240, 145, 255))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)

def add_toll_lang_entries(obj):
    """Mutate the lang dict to include toll-system entries."""
    for denom in COIN_PALETTES:
        obj[f"item.mycar.rfc_{denom}"] = f"RFC {denom} Coin"
    obj["block.mycar.toll_camera"]  = "Toll Camera"
    obj["block.mycar.speed_camera"] = "Speed Camera"
    obj["item.mycar.rfc_registry"]  = "RFC Registry"

if __name__ == "__main__":
    print("Generating textures and assets...\n")

    # Entity textures + item icons for all three vehicle types
    for variant in VARIANT_ORDER:
        p = VARIANTS[variant]

        car_img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        paint_car(car_img, p)
        car_path = TEX_DIR / f"entity/car_{variant}.png"
        car_path.parent.mkdir(parents=True, exist_ok=True)
        car_img.save(car_path)
        print(f"  car      {variant:14}  -> {car_path.relative_to(ROOT)}")

        truck_img = Image.new("RGBA", (256, 384), (0, 0, 0, 0))
        paint_truck(truck_img, p)
        truck_path = TEX_DIR / f"entity/truck_{variant}.png"
        truck_path.parent.mkdir(parents=True, exist_ok=True)
        truck_img.save(truck_path)
        print(f"  truck    {variant:14}  -> {truck_path.relative_to(ROOT)}")

        bike_img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
        paint_bicycle(bike_img, p)
        bike_path = TEX_DIR / f"entity/bicycle_{variant}.png"
        bike_path.parent.mkdir(parents=True, exist_ok=True)
        bike_img.save(bike_path)
        print(f"  bicycle  {variant:14}  -> {bike_path.relative_to(ROOT)}")

        make_car_item_icon(p,     TEX_DIR / f"item/car_{variant}.png")
        make_truck_item_icon(p,   TEX_DIR / f"item/truck_{variant}.png")
        make_bicycle_item_icon(p, TEX_DIR / f"item/bicycle_{variant}.png")

    make_fuel_can_icon()
    print()

    generate_item_models()
    generate_recipes()
    generate_lang()
    print("Item models, recipes, lang written.")

    generate_toll_assets()
    print("Toll system assets (coins + camera block) written.")

    print("\nDone.")
