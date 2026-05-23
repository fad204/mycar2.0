package net.mycar.item;

import net.mycar.entity.CarEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class FuelCanItem extends Item {
    /** Fuel restored per fuel can used. */
    public static final int FUEL_PER_CAN = 500;

    public FuelCanItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand) {
        // useOnEntity only fires for LivingEntity; CarEntity is just Entity,
        // so refueling actually flows through CarEntity#interact (which already accepts FUEL_CAN).
        // This override is kept for completeness.
        return ActionResult.PASS;
    }
}
