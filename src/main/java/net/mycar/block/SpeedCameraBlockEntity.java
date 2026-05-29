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

import java.util.ArrayDeque;
import java.util.Deque;
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
    /** Number of recent position samples kept per vehicle. Speed is measured
     *  across the OLDEST and NEWEST samples (i.e., over up to WINDOW_SAMPLES-1
     *  ticks). A 5-sample window means up to 4 ticks of averaging, which is
     *  enough to smooth out the 1-3 tick position-batching artifacts that
     *  client-authoritative vehicle movement produces when network packets
     *  are delayed or batched. Over any window that spans the lag, the
     *  total position change equals total actual motion (client always
     *  catches up), so the averaged speed is correct regardless of lag. */
    private static final int WINDOW_SAMPLES = 5;
    /** Maximum age (ticks) of the OLDEST sample in the window. If the queue
     *  has gone stale (vehicle left and re-entered), we still measure speed
     *  but with the freshest sample available. */
    private static final int MAX_SAMPLE_AGE_TICKS = 15;

    /** Conversion: blocks per tick → km/h. (×20 ticks/sec, ×3.6 m/s→km/h.) */
    private static final double BLOCKS_PER_TICK_TO_KMH = 20.0 * 3.6;

    private int speedLimitKmh = 0;
    private int fineAmount    = 0;
    private int tickCounter   = 0;
    private final Map<UUID, Long> recentlyTicketed = new HashMap<>();
    /** Rolling history of recent positions for each vehicle. Each entry is
     *  a deque of up to WINDOW_SAMPLES samples, each {@code [x, z, tick]}.
     *  We measure speed across the oldest and newest in the deque, which
     *  smooths over single-tick lag spikes. */
    private final Map<UUID, Deque<double[]>> positionHistory = new HashMap<>();
    /** Number of CONSECUTIVE scans on which this vehicle measured over the
     *  speed limit (using the smoothed window speed above). Requires
     *  MIN_CONSECUTIVE_OVER_LIMIT in a row before firing a ticket, as an
     *  extra defense against transient measurement artifacts. */
    private final Map<UUID, Integer> consecutiveOverLimit = new HashMap<>();
    /** Minimum consecutive over-limit scans before a ticket is fired. */
    private static final int MIN_CONSECUTIVE_OVER_LIMIT = 2;

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
        // Sweep positionHistory entries that haven't been refreshed for a
        // while (vehicles that left and didn't return). Sweep
        // consecutiveOverLimit for the same UUIDs so a re-entering vehicle
        // starts fresh.
        this.positionHistory.entrySet().removeIf(e -> {
            Deque<double[]> q = e.getValue();
            if (q.isEmpty()) return true;
            boolean stale = now - (long) q.peekLast()[2] > 200;
            if (stale) this.consecutiveOverLimit.remove(e.getKey());
            return stale;
        });

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

            // Push this scan's sample to the vehicle's rolling history.
            // Drop the oldest sample once the queue exceeds WINDOW_SAMPLES,
            // so the window slides forward each tick.
            Deque<double[]> history = this.positionHistory.computeIfAbsent(
                uuid, k -> new ArrayDeque<>(WINDOW_SAMPLES + 1));
            history.addLast(new double[]{ vehicle.getX(), vehicle.getZ(), now });
            while (history.size() > WINDOW_SAMPLES) {
                history.pollFirst();
            }

            // Compute speed across the oldest and newest samples in the
            // window. A single-tick lag spike gets diluted across the window
            // (typically 4 ticks at WINDOW_SAMPLES=5), so the reading reflects
            // sustained motion rather than a one-tick measurement artifact.
            double kmh = -1.0;
            if (history.size() >= 2) {
                double[] oldest = history.peekFirst();
                double[] newest = history.peekLast();
                long elapsed = (long) newest[2] - (long) oldest[2];
                if (elapsed > 0 && elapsed <= MAX_SAMPLE_AGE_TICKS) {
                    double dx = newest[0] - oldest[0];
                    double dz = newest[1] - oldest[1];
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

            // Track consecutive over-limit readings. A single one-tick spike
            // (lag artifact) won't accumulate; only a vehicle that's
            // genuinely over the limit will hit the threshold across
            // multiple back-to-back scans.
            if (kmh <= this.speedLimitKmh) {
                this.consecutiveOverLimit.remove(uuid);
                continue;
            }
            int overCount = this.consecutiveOverLimit.getOrDefault(uuid, 0) + 1;
            this.consecutiveOverLimit.put(uuid, overCount);
            if (overCount < MIN_CONSECUTIVE_OVER_LIMIT) continue;

            Entity primary = vehicle.getPrimaryPassenger();
            if (!(primary instanceof PlayerEntity)) continue;
            PlayerEntity player = (PlayerEntity) primary;

            this.recentlyTicketed.put(uuid, now + COOLDOWN_TICKS);
            // Clear the over-limit counter once we've fired so the cooldown
            // is the only gate keeping us from re-ticketing immediately.
            this.consecutiveOverLimit.remove(uuid);

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
