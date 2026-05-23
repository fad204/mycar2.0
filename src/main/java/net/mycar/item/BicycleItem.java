package net.mycar.item;

import net.mycar.MyCarMod;
import net.mycar.entity.BicycleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class BicycleItem extends Item {
    private final int variant;

    public BicycleItem(Settings settings, int variant) {
        super(settings);
        this.variant = variant;
    }

    public int getVariant() { return this.variant; }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;

        if (!world.isClient) {
            Direction side = context.getSide();
            BlockPos spawnPos = context.getBlockPos().offset(side);

            BicycleEntity bike = new BicycleEntity(MyCarMod.BICYCLE, world);
            bike.setVariant(this.variant);
            bike.refreshPositionAndAngles(
                spawnPos.getX() + 0.5,
                spawnPos.getY() + 0.1,
                spawnPos.getZ() + 0.5,
                player.yaw + 180.0f,
                0.0f
            );
            world.spawnEntity(bike);

            ItemStack stack = context.getStack();
            if (!player.abilities.creativeMode) stack.decrement(1);
        }
        return ActionResult.success(world.isClient);
    }
}
