package net.mycar.entity;

import net.mycar.MyCarMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Truck: slower, larger, 2 seats, 54-slot cargo box.
 * Sneak + right-click opens the cargo. Plain right-click mounts.
 */
public class TruckEntity extends AbstractVehicleEntity implements Inventory {

    public static final int INVENTORY_SIZE = 54; // 6 rows of 9 - same as double chest

    private static final double[] GEAR_TOP_SPEED = {0.15, 0.35, 0.55, 0.75, 0.95};

    private static final double[][] SEATS = {
        { 0.45, 0.0, 2.00},  // 0 driver  - front-left of cab (US style)
        {-0.45, 0.0, 2.00},  // 1         - front-right of cab
    };

    private final DefaultedList<ItemStack> inventory =
        DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

    public TruckEntity(EntityType<? extends TruckEntity> type, World world) {
        super(type, world);
    }

    // -------- Tuning overrides --------
    @Override protected double[]   getGearTopSpeeds()        { return GEAR_TOP_SPEED; }
    @Override protected int        getMaxFuel()              { return 2000; } // bigger tank
    @Override protected double     getAcceleration()         { return 0.02; } // slower to pick up
    @Override protected double     getBrakeStrength()        { return 0.06; }
    @Override protected double     getFriction()             { return 0.020; }
    @Override protected double     getReverseFraction()      { return 0.30; }
    @Override protected float      getMaxTurnRate()          { return 2.5f; } // wider turning radius
    @Override protected double     getMinTurnSpeed()         { return 0.04; }
    @Override protected double     getWaterSpeedCap()        { return 0.04; }
    @Override protected double     getFuelConsumptionAtTop() { return 0.10; } // heavier = thirstier
    @Override protected int        getMaxPassengers()        { return 2; }
    @Override protected double[][] getSeatLocalPositions()   { return SEATS; }
    @Override public    double     getMountedHeightOffset()  { return 1.5; }  // head at new cab roof (cab height 22, roof ≈ world y 3.125)

    @Override
    protected Item getDropItem() {
        return MyCarMod.TRUCK_ITEMS.getOrDefault(getVariant(), MyCarMod.TRUCK_ITEMS.get(0));
    }

    // -------- Interact: sneak opens cargo, else fall through to mount/refuel --------
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (player.isSneaking()) {
            if (!this.world.isClient) {
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> GenericContainerScreenHandler.createGeneric9x6(syncId, inv, this),
                    new TranslatableText("entity.mycar.truck.cargo")
                ));
            }
            return ActionResult.success(this.world.isClient);
        }
        return super.interact(player, hand);
    }

    // -------- Save/Load with inventory --------
    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        this.inventory.clear();
        Inventories.readNbt(tag, this.inventory);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        Inventories.writeNbt(tag, this.inventory);
    }

    // -------- Drop cargo on destruction --------
    @Override
    protected void onDestroyed(DamageSource source) {
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) {
                this.dropStack(stack.copy());
            }
        }
        this.inventory.clear();
    }

    // ========== Inventory implementation ==========
    @Override public int size() { return INVENTORY_SIZE; }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : this.inventory) if (!s.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getStack(int slot) { return this.inventory.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(this.inventory, slot, amount);
        if (!result.isEmpty()) this.markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
    }

    @Override public void markDirty() { /* persisted via writeCustomDataToNbt */ }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return !this.removed && this.squaredDistanceTo(player) < 64.0;
    }

    @Override public void clear() { this.inventory.clear(); }
}
