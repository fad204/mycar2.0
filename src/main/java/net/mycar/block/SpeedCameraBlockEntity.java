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
 * #DETECTION_INTERVAL_TICKS} ticks for vehicles in a 3×3×{@link #DETECTION_DEPTH}
 * box below; for each vehicle whose km/h exceeds {@link #speedLimitKmh},
 * issues a ticket (charges {@link #fineAmount} via the same plate→inventory→debt
 * pipeline as the toll camera) and plays a low buzzer.
 *
 * Inactive when limit=0 or fine=0 (newly placed default).
 */
public class SpeedCameraBlockEntity extends BlockEntity implements Tickable {

    private static final int DETECTION_INTERVAL_TICKS = 10;
    private static final int DETECTION_DEPTH = 6;
    private static final int DETECTION_HALF_WIDTH = 1;
    private static final int COOLDOWN_TICKS = 60;

    /** Conversion: blocks per tick → km/h. (×20 ticks/sec, ×3.6 m/s→km/h.) */
    private static final double BLOCKS_PER_TICK_TO_KMH = 20.0 * 3.6;

    private int speedLimitKmh = 0;
    private int fineAmount    = 0;
    private int tickCounter   = 0;
    private final Map<UUID, Long> recentlyTicketed = new HashMap<>();

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

        BlockPos pos = this.getPos();
        Box scan = new Box(
            pos.getX() - DETECTION_HALF_WIDTH, pos.getY() - DETECTION_DEPTH, pos.getZ() - DETECTION_HALF_WIDTH,
            pos.getX() + DETECTION_HALF_WIDTH + 1, pos.getY(), pos.getZ() + DETECTION_HALF_WIDTH + 1
        );
        List<AbstractVehicleEntity> vehicles = this.world.getEntitiesByClass(
            AbstractVehicleEntity.class, scan, e -> true);

        for (AbstractVehicleEntity vehicle : vehicles) {
            UUID uuid = vehicle.getUuid();
            if (this.recentlyTicketed.containsKey(uuid)) continue;

            // Compute vehicle's actual speed in km/h — uses |currentSpeed| so
            // reversing fast also triggers a ticket.
            double kmh = Math.abs(vehicle.getCurrentSpeed()) * BLOCKS_PER_TICK_TO_KMH;
            if (kmh <= this.speedLimitKmh) continue;

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
