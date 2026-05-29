package net.mycar.network;

import net.mycar.MyCarMod;
import net.mycar.entity.AbstractVehicleEntity;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Networking {
    public static final Identifier SHIFT_GEAR     = MyCarMod.id("shift_gear");
    public static final Identifier HANDBRAKE      = MyCarMod.id("handbrake");
    public static final Identifier SPAWN_VEHICLE  = MyCarMod.id("spawn_vehicle");
    public static final Identifier TOGGLE_SIREN   = MyCarMod.id("toggle_siren");

    public static void handleShiftGear(MinecraftServer server, ServerPlayerEntity player,
                                       ServerPlayNetworkHandler handler, PacketByteBuf buf,
                                       PacketSender responseSender) {
        boolean up = buf.readBoolean();
        server.execute(() -> {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof AbstractVehicleEntity) {
                AbstractVehicleEntity v = (AbstractVehicleEntity) vehicle;
                if (v.getPrimaryPassenger() == player) {
                    if (up) v.shiftGearUp(); else v.shiftGearDown();
                }
            }
        });
    }

    public static void handleHandbrake(MinecraftServer server, ServerPlayerEntity player,
                                       ServerPlayNetworkHandler handler, PacketByteBuf buf,
                                       PacketSender responseSender) {
        server.execute(() -> {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof AbstractVehicleEntity) {
                AbstractVehicleEntity v = (AbstractVehicleEntity) vehicle;
                if (v.getPrimaryPassenger() == player) {
                    v.triggerHandbrake();
                }
            }
        });
    }

    public static void handleToggleSiren(MinecraftServer server, ServerPlayerEntity player,
                                         ServerPlayNetworkHandler handler, PacketByteBuf buf,
                                         PacketSender responseSender) {
        server.execute(() -> {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof AbstractVehicleEntity) {
                AbstractVehicleEntity v = (AbstractVehicleEntity) vehicle;
                if (v.getPrimaryPassenger() == player) {
                    v.toggleSiren();
                }
            }
        });
    }
}
