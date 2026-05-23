package net.mycar.block;

import net.mycar.MyCarMod;
import net.mycar.entity.AbstractVehicleEntity;
import net.mycar.util.RfcAccountState;
import net.mycar.util.RfcCurrency;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ticking detection logic for {@link SpeedCameraBlock}. Scans every {@link
 * #DETECTION_INTERVAL_TICKS} ticks for vehicles in a {@link #DETECTION_WIDTH}×{@link
 * #DETECTION_LENGTH}×{@link #DETECTION_DEPTH} box below; for each vehicle whose km/h exceeds {@link #speedLimitKmh},
 * issues a ticket (charges {@link #fineAmount} via the same plate→inventory→debt
 * pipeline as the toll camera) and plays a low buzzer.
 *
 * Inactive when limit=0 or fine=0 (newly placed default).
 */
public class SpeedCameraBlockEntity extends BlockEntity implements Tickable {

    /** Scan rate (server ticks between detection sweeps). At 1 tick (20 Hz),
     *  even a vehicle at 200 km/h (2.78 b/t) gets sampled enough times to
     *  measure a position delta. The scan is cheap (one entity query). */
    private static final int DETECTION_INTERVAL_TICKS = 1;
    /** Detection box dimensions: 10 wide × 10 long × 7 down, centered
     *  horizontally on the camera block and extending downward from it. */
    private static final double DETECTION_WIDTH  = 10.0;
    private static final double DETECTION_LENGTH = 10.0;
    private static final int    DETECTION_DEPTH  = 7;
    private static final int COOLDOWN_TICKS = 60;
    /** Max age of a prior position sample to still count as "fresh enough"
     *  for speed measurement. At 1-tick scans the typical age is 1; allow a
     *  few missed ticks (5) but discard anything older — it means the vehicle
     *  left and re-entered, and averaging across that gap gives wrong speed. */
    private static final int MAX_PRIOR_AGE_TICKS = 5;

    /** Conversion: blocks per tick → km/h. (×20 ticks/sec, ×3.6 m/s→km/h.) */
    private static final double BLOCKS_PER_TICK_TO_KMH = 20.0 * 3.6;

    private int speedLimitKmh = 0;
    private int fineAmount    = 0;
    private int tickCounter   = 0;
    private final Map<UUID, Long> recentlyTicketed = new HashMap<>();
    /** Last-seen position for each vehicle, used to compute speed across
     *  scans. Server-side {@code prevX}/{@code getX} delta is always 0 by
     *  the time BEs tick (baseTick already ran), so we measure motion
     *  ourselves over the interval between scans. Format: uuid → [x, z, tick]. */
    private final Map<UUID, double[]> lastSeen = new HashMap<>();

    /** Debug info — shown on right-click with empty hand. Lets the user verify
     *  the camera is actually detecting vehicles (and at what speed) without
     *  needing to drive them over the limit. */
    private String lastDetectionSummary = null;
    private long   lastDetectionTick    = 0;

    public String getLastDetectionSummary() { return this.lastDetectionSummary; }
    public long   getLastDetectionTick()    { return this.lastDetectionTick;    }

    public SpeedCameraBlockEntity() {
        super(MyCarMod.SPEED_CAMERA_BE);
    }

    // ---------------- public API ----------------

    public int getSpeedLimitKmh() { return this.speedLimitKmh; }
    public int getFineAmount()    { return this.fineAmount;    }

    public void setSpeedLimitKmh(int limit) {
        this.speedLimitKmh = Math.max(0, limit);
        this.markDirty();
    }

    public void setFineAmount(int amount) {
        this.fineAmount = Math.max(0, amount);
        this.markDirty();
    }

    // ---------------- persistence ----------------

    @Override
    public void fromTag(BlockState state, NbtCompound tag) {
        super.fromTag(state, tag);
        if (tag.contains("SpeedLimit")) this.speedLimitKmh = tag.getInt("SpeedLimit");
        if (tag.contains("Fine"))       this.fineAmount    = tag.getInt("Fine");
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        tag.putInt("SpeedLimit", this.speedLimitKmh);
        tag.putInt("Fine",       this.fineAmount);
        return tag;
    }

    // ---------------- ticking ----------------

    @Override
    public void tick() {
        if (this.world == null || this.world.isClient) return;

        this.tickCounter++;
        if (this.tickCounter % DETECTION_INTERVAL_TICKS != 0) return;

        // Inactive if either knob is at 0 — saves scan work for unplaced/unconfigured cameras.
        if (this.speedLimitKmh <= 0 || this.fineAmount <= 0) return;

        long now = this.world.getTime();

        // Sweep expired cooldowns.
        Iterator<Map.Entry<UUID, Long>> it = this.recentlyTicketed.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= now) it.remove();
        }
        // Sweep lastSeen entries that haven't been refreshed for a while
        // (vehicles that left and didn't return).
        this.lastSeen.entrySet().removeIf(e -> now - (long) e.getValue()[2] > 200);

        BlockPos pos = this.getPos();
        double cx = pos.getX() + 0.5;  // horizontal center of camera block
        double cz = pos.getZ() + 0.5;
        Box scan = new Box(
            cx - DETECTION_WIDTH  / 2.0, pos.getY() - DETECTION_DEPTH, cz - DETECTION_LENGTH / 2.0,
            cx + DETECTION_WIDTH  / 2.0, pos.getY(),                   cz + DETECTION_LENGTH / 2.0
        );
        List<AbstractVehicleEntity> vehicles = this.world.getEntitiesByClass(
            AbstractVehicleEntity.class, scan, e -> true);

        for (AbstractVehicleEntity vehicle : vehicles) {
            UUID uuid = vehicle.getUuid();
            String name = vehicle.hasCustomName()
                ? vehicle.getCustomName().getString()
                : "(unnamed " + vehicle.getType().toString() + ")";

            // Record current position for this vehicle BEFORE we decide whether
            // to ticket — that way the next scan has data to compute speed from
            // even if this scan is the vehicle's first appearance.
            double[] prior = this.lastSeen.get(uuid);
            this.lastSeen.put(uuid, new double[]{ vehicle.getX(), vehicle.getZ(), now });

            // Compute speed if we have prior data. We still want this even
            // when below the limit / in cooldown, so the debug "last seen"
            // readout reflects every detection.
            double kmh = -1.0; // sentinel for "no measurement yet"
            if (prior != null) {
                long elapsed = now - (long) prior[2];
                if (elapsed > 0 && elapsed <= MAX_PRIOR_AGE_TICKS) {
                    double dx = vehicle.getX() - prior[0];
                    double dz = vehicle.getZ() - prior[1];
                    double bpt = Math.sqrt(dx * dx + dz * dz) / (double) elapsed;
                    kmh = bpt * BLOCKS_PER_TICK_TO_KMH;
                }
            }

            // Always update the debug readout (regardless of cooldown / limit).
            this.lastDetectionSummary = (kmh >= 0)
                ? name + " @ " + (int) Math.round(kmh) + " km/h"
                : name + " (first sighting — speed pending)";
            this.lastDetectionTick = now;

            if (this.recentlyTicketed.containsKey(uuid)) continue;
            if (kmh < 0) continue;                              // no measurement yet
            if (kmh <= this.speedLimitKmh) continue;            // under limit

            Entity primary = vehicle.getPrimaryPassenger();
            if (!(primary instanceof PlayerEntity)) continue;
            PlayerEntity player = (PlayerEntity) primary;

            this.recentlyTicketed.put(uuid, now + COOLDOWN_TICKS);

            // Three-stage bill (plate → inventory → debt). Identical pattern
            // to the toll camera; the result is summarized as a "ticket".
            final int totalDue = this.fineAmount;
            int remaining = totalDue;

            String plate = null;
            RfcAccountState state = null;
            int drawnFromPlate = 0;
            if (vehicle.hasCustomName() && this.world instanceof ServerWorld) {
                plate = vehicle.getCustomName().getString();
                state = RfcAccountState.get((ServerWorld) this.world);
                drawnFromPlate = state.takeUpToBalance(plate, remaining);
                remaining -= drawnFromPlate;
            }

            int drawnFromInv = 0;
            if (remaining > 0 && RfcCurrency.tryCharge(player, remaining)) {
                drawnFromInv = remaining;
                remaining = 0;
            }

            int addedToDebt = 0;
            if (remaining > 0 && state != null) {
                addedToDebt = remaining;
                state.addDebt(plate, addedToDebt);
                remaining = 0;
            }

            int overBy = (int) Math.round(kmh - this.speedLimitKmh);
            StringBuilder msg = new StringBuilder(
                "§c[Speed Ticket] §fOver by " + overBy + " km/h §7— §f-" + totalDue + " RFC §7(");
            boolean firstField = true;
            if (drawnFromPlate > 0) {
                msg.append("plate ").append(plate).append(": -").append(drawnFromPlate);
                firstField = false;
            }
            if (drawnFromInv > 0) {
                if (!firstField) msg.append("; ");
                msg.append("inv: -").append(drawnFromInv);
                firstField = false;
            }
            if (addedToDebt > 0) {
                if (!firstField) msg.append("; ");
                msg.append("§cdebt +").append(addedToDebt).append("§7");
            }
            msg.append(")");
            player.sendMessage(new LiteralText(msg.toString()), false);

            // Low buzzer — deliberately joyless to differentiate from toll's ka-ching.
            this.world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_BASS,
                SoundCategory.PLAYERS,
                1.0f, 0.6f);
        }
    }
}
