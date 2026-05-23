package net.mycar.block;

import net.mycar.item.RfcCoinItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A ceiling-mounted speed camera. Detects vehicles in a 3×3×6 box below itself
 * and fines any rider whose vehicle is moving faster than the configured
 * speed limit (in km/h).
 *
 * Right-click configuration:
 *  - Empty hand            → display current "Limit: X km/h | Fine: Y RFC"
 *  - Sneak + empty hand    → reset both to 0 (inactive)
 *  - Redstone dust         → +5 km/h to the speed limit
 *  - Glowstone dust        → +25 km/h to the speed limit (faster ramp)
 *  - RFC coin              → add coin's denomination to the fine amount
 *
 * A camera with limit=0 or fine=0 is inactive — useful for placing the block
 * before tuning the values.
 */
public class SpeedCameraBlock extends Block implements BlockEntityProvider {

    public SpeedCameraBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new SpeedCameraBlockEntity();
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof SpeedCameraBlockEntity)) return ActionResult.PASS;
        SpeedCameraBlockEntity camera = (SpeedCameraBlockEntity) be;
        ItemStack stack = player.getStackInHand(hand);

        // Sneak + empty hand → reset both
        if (stack.isEmpty() && player.isSneaking()) {
            camera.setSpeedLimitKmh(0);
            camera.setFineAmount(0);
            player.sendMessage(new LiteralText(
                "§e[Speed Camera] §fReset — limit and fine both 0"), true);
            return ActionResult.CONSUME;
        }

        // Redstone dust → +5 km/h
        if (stack.getItem() == Items.REDSTONE) {
            int next = camera.getSpeedLimitKmh() + 5;
            camera.setSpeedLimitKmh(next);
            if (!player.abilities.creativeMode) stack.decrement(1);
            player.sendMessage(new LiteralText(
                "§e[Speed Camera] §fLimit +5 km/h §7(now " + next + " km/h)"), true);
            return ActionResult.CONSUME;
        }

        // Glowstone dust → +25 km/h
        if (stack.getItem() == Items.GLOWSTONE_DUST) {
            int next = camera.getSpeedLimitKmh() + 25;
            camera.setSpeedLimitKmh(next);
            if (!player.abilities.creativeMode) stack.decrement(1);
            player.sendMessage(new LiteralText(
                "§e[Speed Camera] §fLimit +25 km/h §7(now " + next + " km/h)"), true);
            return ActionResult.CONSUME;
        }

        // RFC coin → add to fine amount
        if (stack.getItem() instanceof RfcCoinItem) {
            int denom = ((RfcCoinItem) stack.getItem()).getDenomination();
            int next = camera.getFineAmount() + denom;
            camera.setFineAmount(next);
            if (!player.abilities.creativeMode) stack.decrement(1);
            player.sendMessage(new LiteralText(
                "§e[Speed Camera] §fFine +" + denom + " RFC §7(now " + next + " RFC)"), true);
            return ActionResult.CONSUME;
        }

        // Empty hand → display
        if (stack.isEmpty()) {
            player.sendMessage(new LiteralText(
                "§e[Speed Camera] §fLimit: §6" + camera.getSpeedLimitKmh()
                + " km/h §f| Fine: §6" + camera.getFineAmount() + " RFC"), true);
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }
}
