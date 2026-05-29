package net.mycar;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.mycar.block.TollCameraBlock;
import net.mycar.block.TollCameraBlockEntity;
import net.mycar.block.SpeedCameraBlock;
import net.mycar.block.SpeedCameraBlockEntity;
import net.mycar.entity.AbstractVehicleEntity;
import net.mycar.entity.BicycleEntity;
import net.mycar.entity.CarEntity;
import net.mycar.entity.TruckEntity;
import net.mycar.item.BicycleItem;
import net.mycar.item.CarItem;
import net.mycar.item.FuelCanItem;
import net.mycar.item.RfcCoinItem;
import net.mycar.item.RfcRegistryItem;
import net.mycar.item.TruckItem;
import net.mycar.network.Networking;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.sound.SoundEvent;
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

    // ---------------- RFC currency coins ----------------
    // Field-static so they can be referenced from RfcCurrency without an extra map.
    public static final RfcCoinItem RFC_5    = new RfcCoinItem(coinSettings(), 5);
    public static final RfcCoinItem RFC_10   = new RfcCoinItem(coinSettings(), 10);
    public static final RfcCoinItem RFC_50   = new RfcCoinItem(coinSettings(), 50);
    public static final RfcCoinItem RFC_100  = new RfcCoinItem(coinSettings(), 100);
    public static final RfcCoinItem RFC_500  = new RfcCoinItem(coinSettings(), 500);
    public static final RfcCoinItem RFC_1000 = new RfcCoinItem(coinSettings(), 1000);

    private static FabricItemSettings coinSettings() {
        return new FabricItemSettings().group(ItemGroup.MISC).maxCount(64);
    }

    // ---------------- Toll camera block ----------------
    public static final Block TOLL_CAMERA = new TollCameraBlock(
        FabricBlockSettings.of(Material.METAL).strength(3.0f, 6.0f).requiresTool().nonOpaque()
    );

    public static final BlockItem TOLL_CAMERA_ITEM = new BlockItem(
        TOLL_CAMERA, new FabricItemSettings().group(ItemGroup.REDSTONE)
    );

    public static final BlockEntityType<TollCameraBlockEntity> TOLL_CAMERA_BE =
        BlockEntityType.Builder.create(TollCameraBlockEntity::new, TOLL_CAMERA).build(null);

    // ---------------- Speed camera block ----------------
    public static final Block SPEED_CAMERA = new SpeedCameraBlock(
        FabricBlockSettings.of(Material.METAL).strength(3.0f, 6.0f).requiresTool().nonOpaque()
    );

    public static final BlockItem SPEED_CAMERA_ITEM = new BlockItem(
        SPEED_CAMERA, new FabricItemSettings().group(ItemGroup.REDSTONE)
    );

    public static final BlockEntityType<SpeedCameraBlockEntity> SPEED_CAMERA_BE =
        BlockEntityType.Builder.create(SpeedCameraBlockEntity::new, SPEED_CAMERA).build(null);

    // ---------------- RFC Registry book ----------------
    public static final RfcRegistryItem RFC_REGISTRY = new RfcRegistryItem(
        new FabricItemSettings().group(ItemGroup.MISC).maxCount(1)
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

    // ---------------- Sounds ----------------
    // Police siren — played periodically by emergency vehicles when they have
    // a driver. See AbstractVehicleEntity.tick().
    public static final SoundEvent SIREN_SOUND = Registry.register(
        Registry.SOUND_EVENT, id("siren"), new SoundEvent(id("siren"))
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

        // RFC coins
        Registry.register(Registry.ITEM, id("rfc_5"),    RFC_5);
        Registry.register(Registry.ITEM, id("rfc_10"),   RFC_10);
        Registry.register(Registry.ITEM, id("rfc_50"),   RFC_50);
        Registry.register(Registry.ITEM, id("rfc_100"),  RFC_100);
        Registry.register(Registry.ITEM, id("rfc_500"),  RFC_500);
        Registry.register(Registry.ITEM, id("rfc_1000"), RFC_1000);

        // Toll camera block + its item form + its block-entity type
        Registry.register(Registry.BLOCK, id("toll_camera"), TOLL_CAMERA);
        Registry.register(Registry.ITEM,  id("toll_camera"), TOLL_CAMERA_ITEM);
        Registry.register(Registry.BLOCK_ENTITY_TYPE, id("toll_camera"), TOLL_CAMERA_BE);

        // Speed camera block + item + BE type
        Registry.register(Registry.BLOCK, id("speed_camera"), SPEED_CAMERA);
        Registry.register(Registry.ITEM,  id("speed_camera"), SPEED_CAMERA_ITEM);
        Registry.register(Registry.BLOCK_ENTITY_TYPE, id("speed_camera"), SPEED_CAMERA_BE);

        // RFC registry book
        Registry.register(Registry.ITEM, id("rfc_registry"), RFC_REGISTRY);

        // Network handlers (shared between car and truck)
        ServerPlayNetworking.registerGlobalReceiver(Networking.SHIFT_GEAR, Networking::handleShiftGear);
        ServerPlayNetworking.registerGlobalReceiver(Networking.HANDBRAKE, Networking::handleHandbrake);
    }
}
