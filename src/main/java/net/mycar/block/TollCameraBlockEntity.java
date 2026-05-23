package net.mycar.block;

import net.mycar.MyCarMod;
import net.mycar.entity.AbstractVehicleEntity;
import net.mycar.util.RfcCurrency;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
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
 * Ticks every server tick. Once every {@link #DETECTION_INTERVAL_TICKS} ticks
 * it scans a 3×3×{@link #DETECTION_DEPTH} box below itself for vehicles, and
 * charges the rider's RFC wallet for each one that hasn't been seen recently.
 *
 * Per-camera state:
 *  - {@code tollAmount}      operator-configured toll cost in RFC
 *  - {@code recentlyCharged} sliding window of vehicle UUIDs and the tick at
 *                            which their cooldown expires (prevents a single
 *                            pass-through from being charged multiple times)
 */
public class TollCameraBlockEntity extends BlockEntity implements Tickable {

    /** How often to scan, in ticks. 10 ticks = twice per second. */
    private static final int DETECTION_INTERVAL_TICKS = 10;
    /** Vertical depth of the detection box, in blocks below the camera. */
    private static final int DETECTION_DEPTH = 6;
    /** Horizontal half-width: 1 = 3-block-wide footprint (camera + 1 on each side). */
    private static final int DETECTION_HALF_WIDTH = 1;
    /** Charge cooldown per vehicle, in ticks. 60 ticks = 3 seconds. */
    private static final int COOLDOWN_TICKS = 60;
    /** Default toll cost when the block is first placed. */
    private static final int DEFAULT_TOLL = 100;

    private int tollAmount = DEFAULT_TOLL;
    private int tickCounter = 0;
    private final Map<UUID, Long> recentlyCharged = new HashMap<>();

    public TollCameraBlockEntity() {
        super(MyCarMod.TOLL_CAMERA_BE);
    }

    // ----------------- public API -----------------

    public int getTollAmount() {
        return this.tollAmount;
    }

    public void setTollAmount(int amount) {
        this.tollAmount = Math.max(0, amount);
        this.markDirty();
    }

    // ----------------- persistence -----------------

    @Override
    public void fromTag(BlockState state, NbtCompound tag) {
        super.fromTag(state, tag);
        if (tag.contains("TollAmount")) {
            this.tollAmount = tag.getInt("TollAmount");
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        tag.putInt("TollAmount", this.tollAmount);
        return tag;
    }

    // ----------------- ticking -----------------

    @Override
    public void tick() {
        if (this.world == null || this.world.isClient) return;

        this.tickCounter++;
        if (this.tickCounter % DETECTION_INTERVAL_TICKS != 0) return;

        // Camera with no toll set is inactive — skip the scan to save work.
        if (this.tollAmount <= 0) return;

        long now = this.world.getTime();

        // Drop cooldowns that have already expired so the map doesn't grow.
        Iterator<Map.Entry<UUID, Long>> it = this.recentlyCharged.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= now) it.remove();
        }

        // Detection box: 3×3 horizontally centered on the camera, extending
        // DETECTION_DEPTH blocks downward from the camera's bottom face.
        BlockPos pos = this.getPos();
        Box scan = new Box(
            pos.getX() - DETECTION_HALF_WIDTH, pos.getY() - DETECTION_DEPTH, pos.getZ() - DETECTION_HALF_WIDTH,
            pos.getX() + DETECTION_HALF_WIDTH + 1, pos.getY(), pos.getZ() + DETECTION_HALF_WIDTH + 1
        );
        List<AbstractVehicleEntity> vehicles = this.world.getEntitiesByClass(
            AbstractVehicleEntity.class, scan, e -> true);

        for (AbstractVehicleEntity vehicle : vehicles) {
            UUID uuid = vehicle.getUuid();
            if (this.recentlyCharged.containsKey(uuid)) continue;

            // Only charge if there's a player rider — empty vehicles pass for free.
            Entity primary = vehicle.getPrimaryPassenger();
            if (!(primary instanceof PlayerEntity)) continue;
            PlayerEntity player = (PlayerEntity) primary;

            // Always set the cooldown (even on failure) so we don't spam the
            // "insufficient" message every half-second while the vehicle sits.
            this.recentlyCharged.put(uuid, now + COOLDOWN_TICKS);

            if (RfcCurrency.tryCharge(player, this.tollAmount)) {
                int balance = RfcCurrency.sumInventory(player);
                player.sendMessage(new LiteralText(
                    "§a[Toll] §f-" + this.tollAmount + " RFC  (balance: " + balance + ")"), true);
                // Toll-booth confirmation beep — clean electronic blip, audible
                // to anyone nearby (so passengers and bystanders hear the charge).
                this.world.playSound(
                    null, pos,
                    SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.BLOCKS,
                    0.6f, 1.6f);
            } else {
                int balance = RfcCurrency.sumInventory(player);
                player.sendMessage(new LiteralText(
                    "§c[Toll UNPAID] §fNeed " + this.tollAmount + " RFC, you have " + balance), true);
            }
        }
    }
}
