package net.mycar.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

/**
 * World-saved RFC balances keyed by license plate text.
 *
 * Stored under {@code data/mycar_rfc_accounts.dat} alongside the world's
 * other persistent state. Auto-loaded by the persistent state manager on
 * world load; auto-saved when {@link #markDirty()} has been called.
 *
 * Toll cameras draw against this state first (per the Telepass model) and
 * fall back to the rider's coin inventory only if the plate runs short.
 * Players deposit by right-clicking the vehicle with an RFC coin (see
 * {@code AbstractVehicleEntity.interact}).
 */
public class RfcAccountState extends PersistentState {

    /** Identifier used both as the super-class key and as the file name on disk. */
    private static final String KEY = "mycar_rfc_accounts";

    /** Plate text → RFC balance. Zero-balance plates are removed so the map stays small. */
    private final Map<String, Integer> balances = new HashMap<>();

    public RfcAccountState() {
        super(KEY);
    }

    // ---------------- public API ----------------

    public int getBalance(String plate) {
        return this.balances.getOrDefault(plate, 0);
    }

    /** Set the balance for a plate. Negative or zero clears the entry. */
    public void setBalance(String plate, int amount) {
        if (amount <= 0) this.balances.remove(plate);
        else             this.balances.put(plate, amount);
        this.markDirty();
    }

    public void deposit(String plate, int amount) {
        if (amount <= 0) return;
        setBalance(plate, getBalance(plate) + amount);
    }

    /**
     * Withdraw up to {@code amount} from the plate's balance.
     * Returns how much was actually taken (≤ amount, ≥ 0).
     */
    public int takeUpTo(String plate, int amount) {
        if (amount <= 0) return 0;
        int curr = getBalance(plate);
        int taken = Math.min(curr, amount);
        if (taken > 0) setBalance(plate, curr - taken);
        return taken;
    }

    // ---------------- persistence ----------------

    @Override
    public void fromTag(NbtCompound tag) {
        this.balances.clear();
        if (tag.contains("balances")) {
            NbtCompound map = tag.getCompound("balances");
            for (String k : map.getKeys()) {
                this.balances.put(k, map.getInt(k));
            }
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        NbtCompound map = new NbtCompound();
        for (Map.Entry<String, Integer> e : this.balances.entrySet()) {
            map.putInt(e.getKey(), e.getValue());
        }
        tag.put("balances", map);
        return tag;
    }

    /** Get-or-create the single instance for this world's persistent state manager. */
    public static RfcAccountState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(RfcAccountState::new, KEY);
    }
}
