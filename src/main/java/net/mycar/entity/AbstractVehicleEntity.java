package net.mycar.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.mycar.item.RfcCoinItem;
import net.mycar.util.RfcAccountState;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared logic for all driveable vehicles in this mod (Car, Truck).
 *
 * Subclasses supply the constants (top speeds, accel, fuel size, seats, ...)
 * and override the drop-item and (optionally) the interact handler.
 *
 * Variants 0..8:
 *   0 metal_red   1 metal_black  2 metal_gray   3 metal_white
 *   4 wooden_oak  5 wooden_dark  6 wooden_birch 7 wooden_spruce
 *   8 golden
 */
public abstract class AbstractVehicleEntity extends Entity {

    public static final int NUM_VARIANTS = 12;
    public static final int NUM_GEARS    = 5;

    /** Variant constants — used for cars, trucks, and bicycles. */
    public static final int V_METAL_RED     = 0;
    public static final int V_METAL_BLACK   = 1;
    public static final int V_METAL_GRAY    = 2;
    public static final int V_METAL_WHITE   = 3;
    public static final int V_WOODEN_OAK    = 4;
    public static final int V_WOODEN_DARK   = 5;
    public static final int V_WOODEN_BIRCH  = 6;
    public static final int V_WOODEN_SPRUCE = 7;
    public static final int V_GOLDEN        = 8;
    public static final int V_POLICE        = 9;
    public static final int V_FIRE          = 10;
    public static final int V_AMBULANCE     = 11;

    public static final String[] VARIANT_NAMES = {
        "metal_red", "metal_black", "metal_gray", "metal_white",
        "wooden_oak", "wooden_dark", "wooden_birch", "wooden_spruce",
        "golden",
        "police", "fire", "ambulance"
    };

    // ---------------- Tracked data ----------------
    protected static final TrackedData<Integer> FUEL =
        DataTracker.registerData(AbstractVehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    protected static final TrackedData<Integer> GEAR =
        DataTracker.registerData(AbstractVehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    protected static final TrackedData<Float> DAMAGE_TAKEN =
        DataTracker.registerData(AbstractVehicleEntity.class, TrackedDataHandlerRegistry.FLOAT);
    protected static final TrackedData<Integer> VARIANT =
        DataTracker.registerData(AbstractVehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Synced flag: this plate has accrued unpaid toll/speed-camera debt.
     *  Read by the entity renderers to color the plate label red. */
    protected static final TrackedData<Boolean> HAS_DEBT =
        DataTracker.registerData(AbstractVehicleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    /** True while the driver has flipped on lights+siren on an emergency
     *  variant. Synced server→client so all viewers see the same state and
     *  the client-side {@code SirenSoundInstance} can start/stop. */
    protected static final TrackedData<Boolean> SIREN_ACTIVE =
        DataTracker.registerData(AbstractVehicleEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // ---------------- Server-side runtime state ----------------
    protected double currentSpeed = 0.0;
    protected double fuelFraction = 0.0;
    protected boolean handbrakePending = false;

    // Manual position tracking for HUD speed and fuel. Entity.prevX is reset
    // to current X at the start of each tick's baseTick(), so on the server
    // with client-side physics it ends up equal to X, giving a delta of 0.
    // These fields capture the position at the END of a tick so we can compute
    // a real delta against the next tick's start.
    private double lastTickX = Double.NaN;
    private double lastTickZ = Double.NaN;

    public AbstractVehicleEntity(EntityType<?> type, World world) {
        super(type, world);
        this.stepHeight = 1.0F;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(FUEL, 0);
        this.dataTracker.startTracking(GEAR, 1);
        this.dataTracker.startTracking(DAMAGE_TAKEN, 0.0f);
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(HAS_DEBT, false);
        this.dataTracker.startTracking(SIREN_ACTIVE, false);
    }

    // ---------------- Subclass-supplied tuning ----------------
    /** Top forward speed (blocks/tick) for each gear, length must equal NUM_GEARS. */
    protected abstract double[] getGearTopSpeeds();
    protected abstract int     getMaxFuel();
    protected abstract double  getAcceleration();
    protected abstract double  getBrakeStrength();
    protected abstract double  getFriction();
    protected abstract double  getReverseFraction();
    protected abstract float   getMaxTurnRate();
    protected abstract double  getMinTurnSpeed();
    protected abstract double  getWaterSpeedCap();
    protected abstract double  getFuelConsumptionAtTop();
    protected abstract int     getMaxPassengers();
    /** Seat positions in vehicle-local space; each row is {rightX, extraUp, forwardZ}. */
    protected abstract double[][] getSeatLocalPositions();
    /** Vertical offset above vehicle origin for ALL seats (per-passenger extras come from the seat row). */
    @Override
    public abstract double getMountedHeightOffset();
    /** The item to drop when destroyed; usually variant-specific. */
    protected abstract Item getDropItem();

    /** Whether this vehicle consumes fuel. Bicycles override to false. */
    protected boolean needsFuel() { return true; }

    /** Whether this vehicle accepts a name tag as a one-time license plate. Default: yes. */
    protected boolean canHaveLicensePlate() { return true; }

    /** Whether the vehicle has a multi-gear transmission. False for bikes — they then
     *  ignore X/Z/Space keybinds, all "gears" share the top speed, and the HUD omits
     *  the gear prefix. */
    protected boolean hasGears() { return true; }

    /** Accepted alternate fuels (besides the Fuel Can item, handled separately). */
    protected Map<Item, Integer> getAcceptedFuels() {
        Map<Item, Integer> m = new HashMap<>();
        m.put(Items.COAL,        100);
        m.put(Items.CHARCOAL,     80);
        m.put(Items.BLAZE_POWDER,200);
        m.put(Items.LAVA_BUCKET, 800);
        return m;
    }

    // ---------------- Public state accessors ----------------
    public int getFuel()       { return this.dataTracker.get(FUEL); }
    public void setFuel(int v) { this.dataTracker.set(FUEL, MathHelper.clamp(v, 0, getMaxFuel())); }
    public int getGear()       { return this.dataTracker.get(GEAR); }
    public void setGear(int v) { this.dataTracker.set(GEAR, MathHelper.clamp(v, 1, NUM_GEARS)); }
    public int getVariant()    { return this.dataTracker.get(VARIANT); }

    /** True if this vehicle is an emergency variant (police, fire, or
     *  ambulance). Emergency vehicles are exempt from toll fees and speed
     *  camera fines. */
    public boolean isEmergency() { return getVariant() >= V_POLICE; }

    /** True if the driver has flipped on the emergency lights and siren.
     *  Only meaningful on emergency variants; non-emergency variants ignore
     *  the value. Synced via DataTracker so clients can react. */
    public boolean isSirenActive() { return this.dataTracker.get(SIREN_ACTIVE); }
    public void setSirenActive(boolean on) { this.dataTracker.set(SIREN_ACTIVE, on); }

    /** Server-side toggle for the emergency-mode keybind. No-op on non-
     *  emergency variants so a regular truck can't trigger sirens. */
    public void toggleSiren() {
        if (!this.isEmergency()) return;
        this.dataTracker.set(SIREN_ACTIVE, !this.dataTracker.get(SIREN_ACTIVE));
    }
    public void setVariant(int v) { this.dataTracker.set(VARIANT, MathHelper.clamp(v, 0, NUM_VARIANTS - 1)); }
    /** Raw velocity along the heading axis, in blocks per tick. Used by the
     *  speed camera (which converts to km/h via {@code |speed| * 20 * 3.6}). */
    public double getCurrentSpeed() { return this.currentSpeed; }
    /** Synced flag mirroring {@link net.mycar.util.RfcAccountState#getDebt(String)}
     *  for this vehicle's plate. Used by the renderer to color the plate red. */
    public boolean hasDebt() { return this.dataTracker.get(HAS_DEBT); }

    public void shiftGearUp()   { if (hasGears()) setGear(getGear() + 1); }
    public void shiftGearDown() { if (hasGears()) setGear(getGear() - 1); }
    public void triggerHandbrake() { if (hasGears()) this.handbrakePending = true; }

    public int addFuel(int amount) {
        int before = getFuel();
        int after  = Math.min(getMaxFuel(), before + amount);
        setFuel(after);
        return after - before;
    }

    // ---------------- Save / Load ----------------
    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
        this.dataTracker.set(FUEL,    MathHelper.clamp(tag.getInt("Fuel"),    0, getMaxFuel()));
        this.dataTracker.set(GEAR,    MathHelper.clamp(tag.getInt("Gear"),    1, NUM_GEARS));
        this.dataTracker.set(VARIANT, MathHelper.clamp(tag.getInt("Variant"), 0, NUM_VARIANTS - 1));
        this.currentSpeed = tag.getDouble("CurrentSpeed");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
        tag.putInt("Fuel", getFuel());
        tag.putInt("Gear", getGear());
        tag.putInt("Variant", getVariant());
        tag.putDouble("CurrentSpeed", this.currentSpeed);
    }

    // ---------------- Physics tick (boat-style) ----------------
    // Vanilla boats run physics on the side that "owns movement" — the rider's
    // client when ridden, the server when empty. The client then auto-sends a
    // VehicleMoveC2SPacket so the server's position stays authoritative for
    // other observers. We mirror that exactly here; it's the only known way
    // to get the rider to actually follow the vehicle in their own view.
    @Override
    public void tick() {
        super.tick();

        // Server-side: refresh the synced HAS_DEBT flag once per second from
        // the world's RFC account state. Cheap (single map lookup) and keeps
        // the red plate visualization in sync without per-tick overhead.
        if (!this.world.isClient && this.age % 20 == 0 && this.hasCustomName()) {
            String plate = this.getCustomName().getString();
            boolean hasDebt = net.mycar.util.RfcAccountState.get((ServerWorld) this.world)
                                  .getDebt(plate) > 0;
            if (this.dataTracker.get(HAS_DEBT) != hasDebt) {
                this.dataTracker.set(HAS_DEBT, hasDebt);
            }
        }

        // Server-side: auto-clear SIREN_ACTIVE when the vehicle has no driver,
        // so a parked emergency vehicle never sits with its lights blaring.
        if (!this.world.isClient && !this.hasPassengers()
                && this.dataTracker.get(SIREN_ACTIVE)) {
            this.dataTracker.set(SIREN_ACTIVE, false);
        }

        if (this.isLogicalSideForUpdatingMovement()) {
            // Gravity
            Vec3d vel = this.getVelocity();
            if (!this.onGround) {
                this.setVelocity(vel.x, vel.y - 0.08, vel.z);
            } else {
                this.setVelocity(vel.x, Math.max(0, vel.y), vel.z);
            }

            boolean inWater = this.isTouchingWater()
                || this.world.getFluidState(this.getBlockPos()).isIn(FluidTags.WATER);

            Entity primary = this.getPrimaryPassenger();
            boolean hasDriver = primary instanceof LivingEntity;

            float forwardInput = 0;
            float sidewaysInput = 0;
            if (hasDriver) {
                LivingEntity driver = (LivingEntity) primary;
                forwardInput = driver.forwardSpeed;
                sidewaysInput = driver.sidewaysSpeed;
                if (Math.abs(this.currentSpeed) >= getMinTurnSpeed()) {
                    // Mouse-look component: pull yaw toward where the driver is looking.
                    float targetYaw = driver.yaw;
                    float diff = MathHelper.wrapDegrees(targetYaw - this.yaw);
                    float clamped = MathHelper.clamp(diff, -getMaxTurnRate(), getMaxTurnRate());
                    this.yaw += clamped;
                    // A/D component: additive yaw change on top, for car-game feel.
                    // A → positive sidewaysSpeed → turn left → yaw decreases.
                    if (Math.abs(sidewaysInput) > 0.1f) {
                        float adDelta = -Math.signum(sidewaysInput) * getMaxTurnRate();
                        this.yaw += adDelta;
                        // Rotate the driver's view by the same delta so the
                        // mouse-look pass above doesn't undo this change on
                        // the next tick. Without this, driver.yaw stays put,
                        // the "pull toward driver.yaw" pass sees the new gap,
                        // and the A/D rotation gets dragged back.
                        driver.yaw         += adDelta;
                        driver.prevYaw     += adDelta;
                        driver.headYaw     += adDelta;
                        driver.prevHeadYaw += adDelta;
                    }
                }
            }

            int gear = getGear();
            double topSpeed = getGearTopSpeeds()[gear - 1];
            int fuel = getFuel();
            boolean noFuel = needsFuel() && fuel <= 0;

            if (this.handbrakePending) {
                this.currentSpeed *= 0.4;
                if (Math.abs(this.currentSpeed) < 0.02) this.currentSpeed = 0;
                this.handbrakePending = false;
            }

            if (hasDriver && !noFuel && forwardInput > 0.1f) {
                this.currentSpeed = Math.min(topSpeed, this.currentSpeed + getAcceleration());
            } else if (hasDriver && forwardInput < -0.1f) {
                if (this.currentSpeed > 0.05) {
                    this.currentSpeed = Math.max(0, this.currentSpeed - getBrakeStrength());
                } else if (!noFuel) {
                    double maxReverse = -topSpeed * getReverseFraction();
                    this.currentSpeed = Math.max(maxReverse, this.currentSpeed - getAcceleration());
                }
            } else {
                if (this.currentSpeed > 0) {
                    this.currentSpeed = Math.max(0, this.currentSpeed - getFriction());
                } else if (this.currentSpeed < 0) {
                    this.currentSpeed = Math.min(0, this.currentSpeed + getFriction());
                }
            }

            if (inWater) {
                double sign = Math.signum(this.currentSpeed);
                double mag = Math.min(Math.abs(this.currentSpeed), getWaterSpeedCap());
                this.currentSpeed = sign * mag;
            }

            double rad = Math.toRadians(this.yaw);
            double moveX = -Math.sin(rad) * this.currentSpeed;
            double moveZ =  Math.cos(rad) * this.currentSpeed;
            this.setVelocity(moveX, this.getVelocity().y, moveZ);

            this.move(MovementType.SELF, this.getVelocity());
        } else {
            // Non-controlling side: clear velocity so it doesn't drift; the
            // tracker / VehicleMoveC2SPacket handles the real position updates.
            this.setVelocity(Vec3d.ZERO);
        }

        // Pin passengers to their seats every tick on every side, using whatever
        // vehicle position this side currently has (locally-moved on the rider's
        // client, packet-applied on the server, tracker-interpolated elsewhere).
        for (Entity p : this.getPassengerList()) {
            this.updatePassengerPosition(p);
        }

        // Server-only state: fuel consumption + HUD message.
        if (!this.world.isClient) {
            double dx, dz;
            if (Double.isNaN(this.lastTickX)) {
                // First tick after construction/load: no baseline yet.
                dx = 0;
                dz = 0;
            } else {
                dx = this.getX() - this.lastTickX;
                dz = this.getZ() - this.lastTickZ;
                // Clamp out teleport-sized jumps (portals, /tp, etc.)
                if (Math.abs(dx) > 5 || Math.abs(dz) > 5) {
                    dx = 0;
                    dz = 0;
                }
            }
            double observedSpeed = Math.sqrt(dx * dx + dz * dz);

            if (needsFuel() && getFuel() > 0 && observedSpeed > 0.01) {
                double frac = observedSpeed / getGearTopSpeeds()[NUM_GEARS - 1];
                this.fuelFraction += getFuelConsumptionAtTop() * frac;
                if (this.fuelFraction >= 1.0) {
                    int whole = (int) this.fuelFraction;
                    this.setFuel(getFuel() - whole);
                    this.fuelFraction -= whole;
                }
            }

            Entity primary = this.getPrimaryPassenger();
            if (primary instanceof PlayerEntity) {
                PlayerEntity pl = (PlayerEntity) primary;
                double kmh = observedSpeed * 20 * 3.6;
                int gear = getGear();
                int fuel = getFuel();
                String msg;
                if (hasGears() && needsFuel()) {
                    msg = String.format("Gear %d  |  Fuel %d/%d  |  %.0f km/h", gear, fuel, getMaxFuel(), kmh);
                } else if (hasGears()) {
                    msg = String.format("Gear %d  |  %.0f km/h", gear, kmh);
                } else if (needsFuel()) {
                    msg = String.format("Fuel %d/%d  |  %.0f km/h", fuel, getMaxFuel(), kmh);
                } else {
                    msg = String.format("%.0f km/h", kmh);
                }
                pl.sendMessage(new LiteralText(msg), true);
            }
        }

        // Record current position at end of tick so next tick can compute a delta.
        this.lastTickX = this.getX();
        this.lastTickZ = this.getZ();
    }

    // ---------------- Interaction (mount / refuel) ----------------
    /** Default: refuel if holding fuel, otherwise mount. Subclasses can extend (truck adds sneak->cargo). */
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // ----- License plate (right-click with a renamed name tag, once) -----
        if (canHaveLicensePlate() && stack.getItem() == net.minecraft.item.Items.NAME_TAG && stack.hasCustomName()) {
            if (this.hasCustomName()) {
                if (!this.world.isClient) {
                    player.sendMessage(new LiteralText(
                        "This vehicle already has plate: " + this.getCustomName().getString()
                    ), true);
                }
            } else {
                if (!this.world.isClient) {
                    this.setCustomName(stack.getName());
                    this.setCustomNameVisible(true);
                    if (!player.abilities.creativeMode) stack.decrement(1);
                }
            }
            return ActionResult.success(this.world.isClient);
        }

        // ----- RFC deposit (right-click with coin; vehicle must have a plate) -----
        // Adds the coin's denomination to the plate's account. Pays down debt
        // first (so fines clear before new balance accrues), then any leftover
        // tops up the balance.
        if (stack.getItem() instanceof RfcCoinItem && this.hasCustomName()) {
            if (!this.world.isClient) {
                int denom = ((RfcCoinItem) stack.getItem()).getDenomination();
                String plate = this.getCustomName().getString();
                RfcAccountState state = RfcAccountState.get((ServerWorld) this.world);
                int leftover = state.payDebt(plate, denom);
                int paidToDebt = denom - leftover;
                state.addBalance(plate, leftover);
                if (!player.abilities.creativeMode) stack.decrement(1);
                int newBal  = state.getBalance(plate);
                int newDebt = state.getDebt(plate);
                StringBuilder msg = new StringBuilder("§e[Plate " + plate + "] §f+");
                msg.append(denom).append(" RFC §7(");
                if (paidToDebt > 0) {
                    msg.append("§c-").append(paidToDebt).append(" debt§7, ");
                }
                msg.append("balance: ").append(newBal);
                if (newDebt > 0) msg.append(", §cdebt: ").append(newDebt).append("§7");
                msg.append(")");
                player.sendMessage(new LiteralText(msg.toString()), false);
            }
            return ActionResult.success(this.world.isClient);
        }

        // ----- RFC balance check (right-click with paper; vehicle must have a plate) -----
        // Paper acts as a "balance receipt": prints the plate's current RFC
        // balance and outstanding debt to chat. Doesn't consume the paper.
        if (stack.getItem() == Items.PAPER && this.hasCustomName()) {
            if (!this.world.isClient) {
                String plate = this.getCustomName().getString();
                RfcAccountState state = RfcAccountState.get((ServerWorld) this.world);
                int bal  = state.getBalance(plate);
                int debt = state.getDebt(plate);
                String tail = (debt > 0) ? "  §cDebt: §6" + debt + " RFC" : "";
                player.sendMessage(new LiteralText(
                    "§e[Plate " + plate + "] §fBalance: §6" + bal + " RFC" + tail
                ), false);
            }
            return ActionResult.success(this.world.isClient);
        }

        // ----- Refuelling (only if this vehicle uses fuel) -----
        if (needsFuel()) {
            Integer fuelValue = getAcceptedFuels().get(stack.getItem());
            if (fuelValue != null && getFuel() < getMaxFuel()) {
                if (!this.world.isClient) {
                    int added = this.addFuel(fuelValue);
                    if (added > 0 && !player.abilities.creativeMode) {
                        if (stack.getItem() == Items.LAVA_BUCKET) {
                            player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                        } else {
                            stack.decrement(1);
                        }
                    }
                }
                return ActionResult.success(this.world.isClient);
            }

            if (stack.getItem() == net.mycar.MyCarMod.FUEL_CAN && getFuel() < getMaxFuel()) {
                if (!this.world.isClient) {
                    int added = this.addFuel(net.mycar.item.FuelCanItem.FUEL_PER_CAN);
                    if (added > 0 && !player.abilities.creativeMode) stack.decrement(1);
                }
                return ActionResult.success(this.world.isClient);
            }
        }

        // ----- Mount -----
        if (!this.world.isClient) {
            if (this.canAddPassenger(player) && !player.isSneaking()) {
                player.startRiding(this);
            }
        }
        return ActionResult.success(this.world.isClient);
    }

    // ---------------- Passenger handling ----------------
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().size() < getMaxPassengers();
    }

    @Override
    public void updatePassengerPosition(Entity passenger) {
        if (!this.hasPassenger(passenger)) return;

        int idx = this.getPassengerList().indexOf(passenger);
        double[][] seats = getSeatLocalPositions();
        if (idx < 0 || idx >= seats.length) idx = 0;

        double localX = seats[idx][0];
        double localY = seats[idx][1];
        double localZ = seats[idx][2];

        double rad = Math.toRadians(this.yaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double worldX = localX * cos - localZ * sin;
        double worldZ = localX * sin + localZ * cos;

        passenger.setPos(
            this.getX() + worldX,
            this.getY() + this.getMountedHeightOffset() + localY + passenger.getHeightOffset(),
            this.getZ() + worldZ
        );
    }

    // ---------------- Damage / destruction ----------------
    @Override public boolean isCollidable() { return true; }
    @Override public boolean collides()     { return !this.removed; }
    @Override public boolean isPushable()   { return false; }

    /**
     * The default Entity.getPrimaryPassenger() returns null, which would mean
     * our entity has no "driver" even when a player is mounted. We define the
     * first passenger as the driver, which the tick() method then reads
     * forwardSpeed / yaw inputs from.
     */
    @Override
    public Entity getPrimaryPassenger() {
        return this.getPassengerList().isEmpty() ? null : this.getPassengerList().get(0);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.world.isClient || this.removed) return false;

        boolean creativeAttacker = source.getAttacker() instanceof PlayerEntity
            && ((PlayerEntity) source.getAttacker()).abilities.creativeMode;

        float newDmg = this.dataTracker.get(DAMAGE_TAKEN) + amount;
        boolean willDestroy = creativeAttacker || newDmg > 30;

        if (willDestroy) {
            this.onDestroyed(source);
            if (!creativeAttacker) {
                this.dropItem(getDropItem());
            }
            this.remove();
            return true;
        }

        this.dataTracker.set(DAMAGE_TAKEN, newDmg);
        return true;
    }

    /** Hook for subclasses (e.g. truck dropping cargo). */
    protected void onDestroyed(DamageSource source) {}

    @Override protected boolean canClimb() { return false; }

    @Override
    public Packet<?> createSpawnPacket() {
        net.minecraft.network.PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeVarInt(net.minecraft.util.registry.Registry.ENTITY_TYPE.getRawId(this.getType()));
        buf.writeUuid(this.getUuid());
        buf.writeVarInt(this.getEntityId());
        buf.writeDouble(this.getX());
        buf.writeDouble(this.getY());
        buf.writeDouble(this.getZ());
        buf.writeByte((byte) MathHelper.floor(this.pitch * 256.0F / 360.0F));
        buf.writeByte((byte) MathHelper.floor(this.yaw  * 256.0F / 360.0F));
        return net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.createS2CPacket(
            net.mycar.network.Networking.SPAWN_VEHICLE, buf);
    }
}
