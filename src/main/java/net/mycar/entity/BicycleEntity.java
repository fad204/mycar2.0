package net.mycar.entity;

import net.mycar.MyCarMod;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.world.World;

/**
 * Bicycle: 1 rider, no fuel, ~36 km/h top speed.
 *
 * Lighter and narrower than the car. License plate is intentionally
 * disabled — bicycles aren't registered :)
 */
public class BicycleEntity extends AbstractVehicleEntity {

    // All "gears" share the same top speed — bikes are gearless.
    // Bike behaves like a horse: one steady acceleration, one max speed.
    private static final double TOP_SPEED = 0.35;  // ~25 km/h
    private static final double[] GEAR_TOP_SPEED = {TOP_SPEED, TOP_SPEED, TOP_SPEED, TOP_SPEED, TOP_SPEED};

    private static final double[][] SEATS = {
        { 0.0, 0.0, 0.10 },  // single seat, slightly forward of center
    };

    public BicycleEntity(EntityType<? extends BicycleEntity> type, World world) {
        super(type, world);
    }

    @Override protected double[]   getGearTopSpeeds()        { return GEAR_TOP_SPEED; }
    @Override protected int        getMaxFuel()              { return 0; }
    @Override protected double     getAcceleration()         { return 0.03; }
    @Override protected double     getBrakeStrength()        { return 0.08; }
    @Override protected double     getFriction()             { return 0.020; }
    @Override protected double     getReverseFraction()      { return 0.40; }
    @Override protected float      getMaxTurnRate()          { return 5.0f; } // tighter than car
    @Override protected double     getMinTurnSpeed()         { return 0.02; }
    @Override protected double     getWaterSpeedCap()        { return 0.05; }
    @Override protected double     getFuelConsumptionAtTop() { return 0.0; }
    @Override protected int        getMaxPassengers()        { return 1; }
    @Override protected double[][] getSeatLocalPositions()   { return SEATS; }
    @Override public    double     getMountedHeightOffset()  { return 0.6; }

    @Override protected boolean    needsFuel()               { return false; }
    @Override protected boolean    canHaveLicensePlate()     { return false; }
    @Override protected boolean    hasGears()                { return false; }

    @Override
    protected Item getDropItem() {
        return MyCarMod.BICYCLE_ITEMS.getOrDefault(getVariant(), MyCarMod.BICYCLE_ITEMS.get(0));
    }
}
