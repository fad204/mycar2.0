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

# =============================================================
# Truck entity texture (256x256) — uses TruckEntityModel UV layout
# =============================================================
def paint_truck(img, p):
    d = ImageDraw.Draw(img)
    grain = p["panel_line"] if p["grain"] else None

    # Body (32 x 16 x 96) at (0, 0)
    paint_cuboid(d, 0, 0, 32, 16, 96, {
        "top":   body_cfg(p, grain=grain),
        "bot":   {"base": CHASSIS_BOT, "highlight": False, "shadow": False},
        "west":  body_cfg(p, grain=grain),
        "north": {"base": p["body_dark"], "dark": CHASSIS_BOT, "light": p["body"], "grain_color": grain},
        "east":  body_cfg(p, grain=grain),
        "south": {"base": p["body_dark"], "dark": CHASSIS_BOT, "light": p["body"], "grain_color": grain},
    })

    # Cab (28 x 16 x 32) at (0, 112)
    paint_cuboid(d, 0, 112, 28, 16, 32, {
        "top":   roof_cfg(p, grain=grain),
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  window_cfg(),
        "north": windshield_cfg(),
        "east":  window_cfg(),
        "south": {"base": p["body_dark"], "dark": p["body_dark"], "light": p["body"]},  # back-of-cab panel
    })

    # Cargo box (28 x 16 x 56) at (0, 172) — closed delivery-van style
    paint_cuboid(d, 0, 172, 28, 16, 56, {
        "top":   roof_cfg(p, grain=grain),
        "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
        "west":  body_cfg(p, grain=grain, panel_lines=3),
        "north": {"base": p["body_dark"], "dark": p["body_dark"], "light": p["body"]},  # front of cargo (back of cab)
        "east":  body_cfg(p, grain=grain, panel_lines=3),
        "south": {"base": p["body"], "dark": p["body_dark"], "light": p["body_light"],
                  "grain_color": grain, "panel_lines": 1},  # back doors
    })

    # Front bumper (32 x 4 x 2) at (168, 172)
    paint_cuboid(d, 168, 172, 32, 4, 2, {
        "top": bumper_cfg(p), "bot": {"base": p["bumper_dark"], "highlight": False, "shadow": False},
        "west": bumper_cfg(p), "north": bumper_cfg(p), "east": bumper_cfg(p), "south": bumper_cfg(p),
    })

    # Rear bumper at (168, 178)
    paint_cuboid(d, 168, 178, 32, 4, 2, {
        "top": bumper_cfg(p), "bot": {"base": p["bumper_dark"], "highlight": False, "shadow": False},
        "west": bumper_cfg(p), "north": bumper_cfg(p), "east": bumper_cfg(p), "south": bumper_cfg(p),
    })

    # Headlights (3x3x1) — (168,184) and (178,184)
    for u in (168, 178):
        paint_cuboid(d, u, 184, 3, 3, 1, {
            "top":   {"base": HEADLIGHT, "highlight": False, "shadow": False},
            "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
            "west":  {"base": HEADLIGHT, "highlight": False, "shadow": False},
            "north": {"base": HEADLIGHT_HL, "highlight": False, "shadow": False},
            "east":  {"base": HEADLIGHT, "highlight": False, "shadow": False},
            "south": {"base": p["body_dark"], "highlight": False, "shadow": False},
        })

    # Taillights (3x3x1) — (188,184) and (198,184)
    for u in (188, 198):
        paint_cuboid(d, u, 184, 3, 3, 1, {
            "top":   {"base": TAILLIGHT, "highlight": False, "shadow": False},
            "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
            "west":  {"base": TAILLIGHT, "highlight": False, "shadow": False},
            "north": {"base": p["body_dark"], "highlight": False, "shadow": False},
            "east":  {"base": TAILLIGHT, "highlight": False, "shadow": False},
            "south": {"base": TAILLIGHT_HL, "highlight": False, "shadow": False},
        })

    # Mirrors (2x3x4) — (168,188) and (182,188)
    for u in (168, 182):
        paint_cuboid(d, u, 188, 2, 3, 4, {
            "top":   {"base": p["body"], "dark": p["body_dark"]},
            "bot":   {"base": p["body_dark"], "highlight": False, "shadow": False},
            "west":  {"base": MIRROR_GLASS, "highlight": False, "shadow": False},
            "north": {"base": p["body"], "highlight": False, "shadow": False},
            "east":  {"base": p["body"], "highlight": False, "shadow": False},
            "south": {"base": p["body"], "highlight": False, "shadow": False},
        })

    # 4 Wheels (5 x 12 x 18) at (120,112), (166,112), (120,142), (166,142).
    # West face at (uw, vw+18) size (18, 12); east face at (uw+23, vw+18) size (18, 12).
    for (uw, vw) in ((120, 112), (166, 112), (120, 142), (166, 142)):
        paint_cuboid(d, uw, vw, 5, 12, 18, {
            "top":   {"base": WHEEL, "highlight": False, "shadow": False},
            "bot":   {"base": WHEEL, "highlight": False, "shadow": False},
            "west":  {"base": WHEEL, "highlight": False, "shadow": False},
            "north": {"base": WHEEL, "highlight": False, "shadow": False},
            "east":  {"base": WHEEL, "highlight": False, "shadow": False},
            "south": {"base": WHEEL, "highlight": False, "shadow": False},
        })
        # Rim and hub on BOTH west and east faces so left+right wheels both look outward.
        for rx in (uw, uw + 23):
            ry = vw + 18
            d.rectangle([rx + 3, ry + 2, rx + 14, ry + 9], outline=WHEEL_RIM)
            d.rectangle([rx + 8, ry + 5, rx + 9, ry + 6], fill=WHEEL_HUB)
            d.point((rx + 8, ry + 2), fill=WHEEL_RIM)
            d.point((rx + 8, ry + 9), fill=WHEEL_RIM)
            d.point((rx + 3, ry + 5), fill=WHEEL_RIM)
            d.point((rx + 14, ry + 5), fill=WHEEL_RIM)
            # Round corners (4 extreme pixels of each 18x12 rim face).
            for dx, dy in [(0, 0), (17, 0), (0, 11), (17, 11)]:
                d.point((rx + dx, ry + dy), fill=(0, 0, 0, 0))

    # Emergency markings on cab roof + cab sides + cargo sides.
    if p.get("emergency"):
        paint_truck_emergency_decals(d, p)

# Truck emergency markings.
# Cab is at (0, 112), 28x16x32 → top face at (32, 112) size 28x32; west side at (0, 144)
# size 32x16; east side at (60, 144) size 32x16.
# Cargo is at (0, 172), 28x16x56 → west side at (0, 228) size 56x16; east side at (84, 228)
# size 56x16.
def paint_truck_emergency_decals(d, p):
    kind = p["emergency"]
    rx0, ry0 = 32, 112
    rcx, rcy = rx0 + 14, ry0 + 16
    if kind == "police":
        BLUE = (35, 70, 230, 255)
        RED  = (220, 30, 30, 255)
        WHITE = (255, 255, 255, 255)
        # Light bar on cab roof
        d.rectangle([rcx - 5, rcy - 1, rcx - 1, rcy + 1], fill=BLUE)
        d.rectangle([rcx + 1, rcy - 1, rcx + 5, rcy + 1], fill=RED)
        d.point((rcx, rcy), fill=(0, 0, 0, 255))
        # White + blue stripes on both sides of the cab and cargo.
        for side_x in (0, 60):
            d.rectangle([side_x + 3, 152, side_x + 28, 153], fill=WHITE)
            d.rectangle([side_x + 3, 154, side_x + 28, 155], fill=BLUE)
        for side_x in (0, 84):
            d.rectangle([side_x + 3, 234, side_x + 52, 235], fill=WHITE)
            d.rectangle([side_x + 3, 236, side_x + 52, 237], fill=BLUE)
    elif kind == "fire":
        YELLOW = (245, 215, 55, 255)
        # Yellow stripe along cab roof
        d.rectangle([rx0 + 2, rcy - 1, rx0 + 25, rcy + 1], fill=YELLOW)
        for side_x in (0, 60):
            d.rectangle([side_x + 3, 153, side_x + 28, 155], fill=YELLOW)
        for side_x in (0, 84):
            d.rectangle([side_x + 3, 235, side_x + 52, 237], fill=YELLOW)
    elif kind == "ambulance":
        RED = (220, 25, 25, 255)
        # Big red cross on cab roof
        d.rectangle([rcx - 1, rcy - 4, rcx + 1, rcy + 4], fill=RED)
        d.rectangle([rcx - 4, rcy - 1, rcx + 4, rcy + 1], fill=RED)
        # Red side stripes on cab and cargo
        for side_x in (0, 60):
            d.rectangle([side_x + 3, 153, side_x + 28, 155], fill=RED)
        for side_x in (0, 84):
            d.rectangle([side_x + 3, 235, side_x + 52, 237], fill=RED)

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
    write_json(LANG_DIR / "en_us.json", obj)

# =============================================================
# Main
# =============================================================
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

        truck_img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
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

    print("\nDone.")
