package net.mycar.entity;

import net.mycar.MyCarMod;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.world.World;

/**
 * Car: nimble, 4 seats, ~200 km/h top speed.
 */
public class CarEntity extends AbstractVehicleEntity {

    private static final double[] GEAR_TOP_SPEED = {0.35, 0.75, 1.30, 2.0, 2.3};

    private static final double[][] SEATS = {
        { 0.45, 0.0,  0.20},  // 0 driver  - front-left  (US style)
        {-0.45, 0.0,  0.20},  // 1         - front-right
        { 0.45, 0.0, -0.90},  // 2         - rear-left
        {-0.45, 0.0, -0.90},  // 3         - rear-right
    };

    public CarEntity(EntityType<? extends CarEntity> type, World world) {
        super(type, world);
    }

    @Override protected double[]   getGearTopSpeeds()        { return GEAR_TOP_SPEED; }
    @Override protected int        getMaxFuel()              { return 1000; }
    @Override protected double     getAcceleration()         { return 0.04; }
    @Override protected double     getBrakeStrength()        { return 0.07; }
    @Override protected double     getFriction()             { return 0.015; }
    @Override protected double     getReverseFraction()      { return 0.35; }
    @Override protected float      getMaxTurnRate()          { return 4.0f; }
    @Override protected double     getMinTurnSpeed()         { return 0.04; }
    @Override protected double     getWaterSpeedCap()        { return 0.06; }
    @Override protected double     getFuelConsumptionAtTop() { return 0.06; }
    @Override protected int        getMaxPassengers()        { return 4; }
    @Override protected double[][] getSeatLocalPositions()   { return SEATS; }
    @Override public    double     getMountedHeightOffset()  { return 0.75; }

    @Override
    protected Item getDropItem() {
        return MyCarMod.CAR_ITEMS.getOrDefault(getVariant(), MyCarMod.CAR_ITEMS.get(0));
    }
}
