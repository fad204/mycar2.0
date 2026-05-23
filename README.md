# MyCar — a Fabric 1.16.5 vehicles mod

Driveable, fuel-based (or pedal-powered) vehicles for Minecraft Fabric 1.16.5,
in nine colors and three body styles. Built from scratch because every existing
1.16.5 car mod is Forge-only.

## What's in the box

- **Car** — 4 seats (driver + 3 passengers in a 2×2 layout), ~200 km/h top speed, agile
- **Truck** — 2 seats, ~70 km/h, slower acceleration, with a **54-slot cargo box** (same capacity as a double chest)
- **Bicycle** — 1 seat, ~36 km/h, **no fuel needed**, narrow enough to thread through doorways (0.8 wide)
- **9 color/material variants** for every vehicle type:

  | Material | Color | Recipe primary | Color source |
  |---|---|---|---|
  | Metal | Red | iron ingots | red_dye |
  | Metal | Black | iron ingots | black_dye |
  | Metal | Gray | iron ingots | light_gray_dye |
  | Metal | White | iron ingots | white_dye |
  | Wood | Oak | oak_planks | — (the planks) |
  | Wood | Dark Oak | dark_oak_planks | — |
  | Wood | Birch | birch_planks | — |
  | Wood | Spruce | spruce_planks | — |
  | Gold | — | gold_ingots | — |

- **License plate** — for cars and trucks (not bicycles), set with a renamed name tag, **once** per vehicle. After that the plate is permanent.
- **Fuel Can** item for refueling cars and trucks. Coal, charcoal, blaze powder, and lava bucket also work.

## Controls

While driving:

| Key | Action |
|---|---|
| W / S | Accelerate / brake (S reverses when stopped) |
| Look around | Steer (the vehicle turns toward your camera direction) |
| X | Shift up a gear |
| Z | Shift down a gear |
| Space | Handbrake |
| Shift | Dismount |

- **Right-click empty hand** → mount
- **Right-click with fuel** → refuel (cars and trucks only)
- **Right-click with a renamed name tag** → set the license plate (cars + trucks, once each)
- **Sneak + right-click a truck** → open its cargo

First in becomes the driver; if the driver leaves, the next person in the passenger list becomes the new driver.

## License plates

1. Put a Name Tag in an anvil and rename it (e.g., "ROAD42") — standard vanilla Minecraft
2. Right-click your car or truck with that name tag
3. The text becomes the vehicle's display name and floats above it forever

That vehicle will now refuse further name tags ("This vehicle already has plate: ROAD42"). The plate persists across save/load — it's stored as the entity's custom name.

## Crafting

### Car recipes — `S` = saddle in the middle

**Metal car** (red shown — swap the dye for other colors)

```
I I I
I S I        I = iron ingot, S = saddle, W = black wool, D = red_dye
W D W
```

**Wooden car** (oak shown — swap the plank type for other woods)

```
P P P
P S P        P = oak planks, S = saddle, W = black wool
W . W
```

**Golden car**

```
G G G
G S G        G = gold ingot, S = saddle, W = black wool
W . W
```

### Truck recipes — `C` = chest in the middle (gives the truck its cargo bay)

Same patterns as the car, but the saddle slot is replaced with a chest.

### Bicycle recipes — cheaper, smaller pattern

**Metal bicycle**

```
P . P
P S P        P = iron ingot, S = saddle, I = red_dye (color varies)
. I .
```

**Wooden bicycle**

```
P . P
P S P        P = oak planks (or dark oak / birch / spruce), S = saddle, I = stick
. I .
```

**Golden bicycle**

```
P . P
P S P        P = gold ingot, S = saddle, I = stick
. I .
```

### Fuel can

```
. I .
C B C        I = iron, B = bucket, C = coal
. I .
```

## Driving stats

|  | Car | Truck | Bicycle |
|---|---|---|---|
| Size (W × H × L) | 2 × 1.5 × 4 | 3 × 2.5 × 6 | 0.8 × 1.5 × 2 |
| Top speed (gear 5) | 2.78 bpt (≈200 km/h) | 0.95 bpt (≈68 km/h) | 0.50 bpt (≈36 km/h) |
| Acceleration | 0.04 bpt² | 0.02 bpt² | 0.03 bpt² |
| Turn rate | 4°/tick | 2.5°/tick (wider radius) | 5°/tick (tightest) |
| Fuel tank | 1000 | 2000 | — |
| Seats | 4 | 2 | 1 |
| Cargo | — | 54 slots | — |
| License plate | yes | yes | no |

`bpt` = blocks per tick. Multiply by 72 for km/h.

## Building from source

Requires JDK 8+. Two paths:

### IntelliJ IDEA

1. `File → Open` the project root folder
2. IntelliJ detects Gradle, downloads dependencies
3. Run the `build` Gradle task (right-hand Gradle panel → `mycar → Tasks → build → build`)
4. Jar appears at `build/libs/mycar-1.0.0.jar`

### Command line

This project does not include a `gradlew` wrapper jar. To build without it:

1. Install Gradle 7.x: <https://gradle.org/install/>
2. From the project root, run `gradle wrapper` once to generate the wrapper files
3. Then `./gradlew build`
4. Jar in `build/libs/`

Drop the jar into your `.minecraft/mods` folder alongside Fabric API and Fabric Loader for 1.16.5.

## Adding more variants

The texture script is the single source of truth for all per-variant assets. To add a 10th variant:

1. Bump `NUM_VARIANTS = 10` in `AbstractVehicleEntity.java` and add a constant + entry in `VARIANT_NAMES`
2. In `generate_textures.py`, add the variant to `VARIANT_ORDER`, the palette to `VARIANTS`, an ingredient mapping to `VARIANT_INGREDIENTS`, and a display name to `DISPLAY_NAMES`
3. Run `python3 generate_textures.py`

The script regenerates every entity texture, item icon, item model JSON, recipe JSON, and the lang file for all three vehicle types.

## Architecture

```
src/main/java/net/mycar/
├── MyCarMod.java                      # Mod entrypoint, item & entity-type registry
├── client/
│   ├── MyCarClient.java               # Client entrypoint, key bindings, renderer registration
│   └── render/
│       ├── CarEntityModel.java        # 16-part car model     (256×256 texture)
│       ├── CarEntityRenderer.java
│       ├── TruckEntityModel.java      # 15-part truck model   (256×256 texture)
│       ├── TruckEntityRenderer.java
│       ├── BicycleEntityModel.java    #  7-part bicycle model (128×64  texture)
│       └── BicycleEntityRenderer.java
├── entity/
│   ├── AbstractVehicleEntity.java     # Shared physics: fuel, gears, mount, damage, license plate
│   ├── CarEntity.java
│   ├── TruckEntity.java               # + 54-slot Inventory + screen factory
│   └── BicycleEntity.java             # + needsFuel()=false  + canHaveLicensePlate()=false
├── item/
│   ├── CarItem.java
│   ├── TruckItem.java
│   ├── BicycleItem.java
│   └── FuelCanItem.java
└── network/
    └── Networking.java                # Gear-shift / handbrake packets — share between all vehicles
```

`AbstractVehicleEntity` carries all the shared driving logic plus the license-plate
hook (`canHaveLicensePlate()`) and the fuel-system toggle (`needsFuel()`). Each
concrete class only supplies its numeric tuning and a few small overrides.

## Tuning

Per-vehicle constants live at the top of `CarEntity.java`, `TruckEntity.java`,
`BicycleEntity.java`:

```java
// Bicycle excerpt
private static final double[] GEAR_TOP_SPEED = {0.10, 0.20, 0.30, 0.40, 0.50};
@Override protected boolean   needsFuel()           { return false; }
@Override protected boolean   canHaveLicensePlate() { return false; }
```

Speed is in blocks/tick; multiply by 72 for km/h.

## Texture layouts

PNG sizes: cars and trucks are 256×256, bicycles 128×64. UV regions are
documented at the top of each `*EntityModel.java`. Open them in BlockBench
(`File → Open → Resource Pack`) or any pixel editor to repaint.

Standard Minecraft cuboid UV formula (for a cuboid at offset `(u, v)` with
dimensions `(sx, sy, sz)`):

```
top  : (u+sz, v)         size (sx, sz)
bot  : (u+sz+sx, v)      size (sx, sz)
west : (u, v+sz)         size (sz, sy)
north: (u+sz, v+sz)      size (sx, sy)
east : (u+sz+sx, v+sz)   size (sz, sy)
south: (u+2*sz+sx, v+sz) size (sx, sy)
```

## Known caveats

- **Untested by me** — written in an environment that can't run the Minecraft compile. Logic and structure should be sound but a small Yarn-mapping fix on first build is possible. If anything fails to compile, share the error.
- The `ItemGroup.TRANSPORTATION` tab now has 27 vehicles plus the fuel can in `MISC`. Cluttered but functional.
- **Square hitboxes** of 2.5 × 2.5 (car), 3 × 3 (truck), 0.8 × 0.8 (bicycle) even though the models are rectangular. Standard for vehicle entities.
- **No wheel-spin animation yet** — wheels are static. Wire it up in `setAngles()` by rotating the wheel `ModelPart`s based on `entity.getVelocity().horizontalLength()`.
- **No engine audio** — the vehicles are silent.
- **License plate is plain text** — no fancy yellow/blue background. Floats above the entity using vanilla nameplate rendering. If you'd like text rendered onto an actual plate cuboid on the vehicle's back, the renderers have everything they need to add that (entity yaw, position, custom name); it's a render-only enhancement.

## License

Do whatever — this is a starter scaffolding, refine as you like.
