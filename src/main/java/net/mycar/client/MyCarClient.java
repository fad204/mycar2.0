package net.mycar.client;

import net.mycar.MyCarMod;
import net.mycar.client.render.BicycleEntityRenderer;
import net.mycar.client.render.CarEntityRenderer;
import net.mycar.client.render.TruckEntityRenderer;
import net.mycar.entity.AbstractVehicleEntity;
import net.mycar.network.Networking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.lwjgl.glfw.GLFW;

public class MyCarClient implements ClientModInitializer {

    public static KeyBinding KEY_GEAR_UP;
    public static KeyBinding KEY_GEAR_DOWN;
    public static KeyBinding KEY_HANDBRAKE;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(MyCarMod.CAR,     (d, c) -> new CarEntityRenderer(d));
        EntityRendererRegistry.INSTANCE.register(MyCarMod.TRUCK,   (d, c) -> new TruckEntityRenderer(d));
        EntityRendererRegistry.INSTANCE.register(MyCarMod.BICYCLE, (d, c) -> new BicycleEntityRenderer(d));

        // Custom spawn-vehicle handler: vanilla EntitySpawnS2CPacket sometimes fails for
        // non-living custom entities. This explicit handler is the recommended approach.
        ClientPlayNetworking.registerGlobalReceiver(Networking.SPAWN_VEHICLE, (client, handler, buf, responseSender) -> {
            int typeId = buf.readVarInt();
            java.util.UUID uuid = buf.readUuid();
            int entityId = buf.readVarInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float pitch = (buf.readByte() * 360f) / 256f;
            float yaw   = (buf.readByte() * 360f) / 256f;
            client.execute(() -> {
                if (client.world == null) return;
                net.minecraft.entity.EntityType<?> type =
                    net.minecraft.util.registry.Registry.ENTITY_TYPE.get(typeId);
                Entity entity = type.create(client.world);
                if (entity == null) return;
                // Match what vanilla EntitySpawnS2CPacket does — without these,
                // the entity is on the client but not fully interactable.
                entity.updateTrackedPosition(x, y, z);
                entity.setHeadYaw(yaw);
                entity.setBodyYaw(yaw);
                entity.pitch = pitch;
                entity.yaw   = yaw;
                entity.setEntityId(entityId);
                entity.setUuid(uuid);
                client.world.addEntity(entityId, entity);
            });
        });

        KEY_GEAR_UP = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mycar.gear_up", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.mycar.driving"));
        KEY_GEAR_DOWN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mycar.gear_down", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.mycar.driving"));
        KEY_HANDBRAKE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mycar.handbrake", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, "category.mycar.driving"));

        ClientTickEvents.END_CLIENT_TICK.register(MyCarClient::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        Entity vehicle = client.player.getVehicle();
        if (!(vehicle instanceof AbstractVehicleEntity)) return;

        AbstractVehicleEntity v = (AbstractVehicleEntity) vehicle;
        if (v.getPrimaryPassenger() != client.player) return;

        while (KEY_GEAR_UP.wasPressed()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(true);
            ClientPlayNetworking.send(Networking.SHIFT_GEAR, buf);
        }
        while (KEY_GEAR_DOWN.wasPressed()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(false);
            ClientPlayNetworking.send(Networking.SHIFT_GEAR, buf);
        }
        while (KEY_HANDBRAKE.wasPressed()) {
            // Apply locally — in boat-style movement the client owns the
            // physics step for ridden vehicles, so the server-side packet
            // alone wouldn't take effect on currentSpeed.
            v.triggerHandbrake();
            ClientPlayNetworking.send(Networking.HANDBRAKE, PacketByteBufs.empty());
        }
    }
}
