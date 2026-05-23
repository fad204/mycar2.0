package net.mycar;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.mycar.entity.AbstractVehicleEntity;
import net.mycar.entity.BicycleEntity;
import net.mycar.entity.CarEntity;
import net.mycar.entity.TruckEntity;
import net.mycar.item.BicycleItem;
import net.mycar.item.CarItem;
import net.mycar.item.FuelCanItem;
import net.mycar.item.TruckItem;
import net.mycar.network.Networking;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public class MyCarMod implements ModInitializer {

    public static final String MOD_ID = "mycar";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    /** Maps variant id (0..8) -> registered car item. Populated in onInitialize. */
    public static final Map<Integer, Item> CAR_ITEMS     = new HashMap<>();
    /** Maps variant id (0..8) -> registered truck item. */
    public static final Map<Integer, Item> TRUCK_ITEMS   = new HashMap<>();
    /** Maps variant id (0..8) -> registered bicycle item. */
    public static final Map<Integer, Item> BICYCLE_ITEMS = new HashMap<>();

    public static final Item FUEL_CAN = new FuelCanItem(
        new FabricItemSettings().group(ItemGroup.MISC).maxCount(16)
    );

    // ---------------- Entity types ----------------
    public static final EntityType<CarEntity> CAR = Registry.register(
        Registry.ENTITY_TYPE,
        id("car"),
        FabricEntityTypeBuilder.<CarEntity>create(SpawnGroup.MISC, CarEntity::new)
            .dimensions(EntityDimensions.fixed(2.5f, 1.5f))
            .trackRangeBlocks(80).trackedUpdateRate(3).build()
    );

    public static final EntityType<TruckEntity> TRUCK = Registry.register(
        Registry.ENTITY_TYPE,
        id("truck"),
        FabricEntityTypeBuilder.<TruckEntity>create(SpawnGroup.MISC, TruckEntity::new)
            .dimensions(EntityDimensions.fixed(3.0f, 2.5f))
            .trackRangeBlocks(120).trackedUpdateRate(3).build()
    );

    public static final EntityType<BicycleEntity> BICYCLE = Registry.register(
        Registry.ENTITY_TYPE,
        id("bicycle"),
        FabricEntityTypeBuilder.<BicycleEntity>create(SpawnGroup.MISC, BicycleEntity::new)
            .dimensions(EntityDimensions.fixed(0.8f, 1.5f))
            .trackRangeBlocks(80).trackedUpdateRate(3).build()
    );

    @Override
    public void onInitialize() {
        // Register all 9 car variants
        for (int v = 0; v < AbstractVehicleEntity.NUM_VARIANTS; v++) {
            Item car = new CarItem(
                new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(1), v);
            Registry.register(Registry.ITEM, id("car_" + AbstractVehicleEntity.VARIANT_NAMES[v]), car);
            CAR_ITEMS.put(v, car);
        }
        // Register all 9 truck variants
        for (int v = 0; v < AbstractVehicleEntity.NUM_VARIANTS; v++) {
            Item truck = new TruckItem(
                new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(1), v);
            Registry.register(Registry.ITEM, id("truck_" + AbstractVehicleEntity.VARIANT_NAMES[v]), truck);
            TRUCK_ITEMS.put(v, truck);
        }
        // Register all 9 bicycle variants
        for (int v = 0; v < AbstractVehicleEntity.NUM_VARIANTS; v++) {
            Item bike = new BicycleItem(
                new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(1), v);
            Registry.register(Registry.ITEM, id("bicycle_" + AbstractVehicleEntity.VARIANT_NAMES[v]), bike);
            BICYCLE_ITEMS.put(v, bike);
        }
        Registry.register(Registry.ITEM, id("fuel_can"), FUEL_CAN);

        // Network handlers (shared between car and truck)
        ServerPlayNetworking.registerGlobalReceiver(Networking.SHIFT_GEAR, Networking::handleShiftGear);
        ServerPlayNetworking.registerGlobalReceiver(Networking.HANDBRAKE, Networking::handleHandbrake);
    }
}
