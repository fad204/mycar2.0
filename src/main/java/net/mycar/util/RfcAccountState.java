package net.mycar.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

/**
 * World-saved RFC accounting keyed by license plate text. Tracks two values
 * per plate:
 *
 *  - {@code balance}: positive prepaid credit, drawn down by tolls and fines.
 *  - {@code debt}:    accrued unpaid charges. Plates with debt > 0 are rendered
 *                     in red on the vehicle (via the {@code HAS_DEBT} tracker
 *                     on {@link net.mycar.entity.AbstractVehicleEntity}).
 *
 * Toll/speed cameras bill in this order: plate balance → rider's coin
 * inventory → plate debt (any uncovered remainder). Deposits via right-click
 * vehicle pay down debt first, then add to balance, so the user can clear
 * fines just by topping up.
 */
public class RfcAccountState extends PersistentState {

    private static final String KEY = "mycar_rfc_accounts";

    private final Map<String, Integer> balances = new HashMap<>();
    private final Map<String, Integer> debts    = new HashMap<>();

    public RfcAccountState() {
        super(KEY);
    }

    // ---------------- balance ----------------

    public int getBalance(String plate) {
        return this.balances.getOrDefault(plate, 0);
    }

    public void setBalance(String plate, int amount) {
        if (amount <= 0) this.balances.remove(plate);
        else             this.balances.put(plate, amount);
        this.markDirty();
    }

    /** Top up the balance by `amount` (no-op if amount ≤ 0). */
    public void addBalance(String plate, int amount) {
        if (amount <= 0) return;
        setBalance(plate, getBalance(plate) + amount);
    }

    /**
     * Withdraw up to `amount` from the plate's balance. Returns how much was
     * actually taken (≤ amount, ≥ 0). Used by toll/speed cameras to draw
     * from the plate before falling back to the rider's inventory.
     */
    public int takeUpToBalance(String plate, int amount) {
        if (amount <= 0) return 0;
        int curr = getBalance(plate);
        int taken = Math.min(curr, amount);
        if (taken > 0) setBalance(plate, curr - taken);
        return taken;
    }

    // ---------------- debt ----------------

    public int getDebt(String plate) {
        return this.debts.getOrDefault(plate, 0);
    }

    public void setDebt(String plate, int amount) {
        if (amount <= 0) this.debts.remove(plate);
        else             this.debts.put(plate, amount);
        this.markDirty();
    }

    /** Add `amount` to the plate's debt (no-op if amount ≤ 0). */
    public void addDebt(String plate, int amount) {
        if (amount <= 0) return;
        setDebt(plate, getDebt(plate) + amount);
    }

    /**
     * Pay down up to `amount` of the plate's debt. Returns the leftover
     * (amount minus what the debt absorbed). Caller can apply the leftover
     * to balance.
     *
     * Example: debt=80, amount=100 → returns 20 (debt cleared, 20 unused).
     *          debt=200, amount=100 → returns 0 (debt now 100, all consumed).
     */
    public int payDebt(String plate, int amount) {
        if (amount <= 0) return 0;
        int curr = getDebt(plate);
        if (curr <= 0) return amount;
        int applied = Math.min(curr, amount);
        setDebt(plate, curr - applied);
        return amount - applied;
    }

    // ---------------- snapshots ----------------

    /**
     * Snapshot every plate that has ever held a balance or debt entry.
     * Returns {@code plate → [balance, debt]}. Used by the RFC Registry item
     * to print the full ledger.
     */
    public Map<String, int[]> snapshotAllPlates() {
        Map<String, int[]> out = new HashMap<>();
        for (String p : this.balances.keySet()) {
            out.put(p, new int[] { this.balances.get(p), this.debts.getOrDefault(p, 0) });
        }
        for (String p : this.debts.keySet()) {
            out.computeIfAbsent(p, k -> new int[] { 0, this.debts.get(p) });
        }
        return out;
    }

    // ---------------- persistence ----------------

    @Override
    public void fromTag(NbtCompound tag) {
        this.balances.clear();
        this.debts.clear();
        if (tag.contains("balances")) {
            NbtCompound m = tag.getCompound("balances");
            for (String k : m.getKeys()) this.balances.put(k, m.getInt(k));
        }
        if (tag.contains("debts")) {
            NbtCompound m = tag.getCompound("debts");
            for (String k : m.getKeys()) this.debts.put(k, m.getInt(k));
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        NbtCompound balMap = new NbtCompound();
        for (Map.Entry<String, Integer> e : this.balances.entrySet()) {
            balMap.putInt(e.getKey(), e.getValue());
        }
        tag.put("balances", balMap);
        NbtCompound debtMap = new NbtCompound();
        for (Map.Entry<String, Integer> e : this.debts.entrySet()) {
            debtMap.putInt(e.getKey(), e.getValue());
        }
        tag.put("debts", debtMap);
        return tag;
    }

    public static RfcAccountState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(RfcAccountState::new, KEY);
    }
}
