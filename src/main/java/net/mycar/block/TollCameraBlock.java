package net.mycar.block;

import net.mycar.item.RfcCoinItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A ceiling-mounted toll camera. Detects vehicles in a 3×3×6 box below itself
 * and deducts an operator-configured RFC toll from the rider.
 *
 * Right-click interactions (configuring the toll cost):
 *  - Empty hand            → display current toll in the action bar
 *  - Sneak + empty hand    → reset toll to 0
 *  - RFC coin              → add coin's value to the toll (consumes the coin)
 *
 * Toll cost is stored on the {@link TollCameraBlockEntity}.
 */
public class TollCameraBlock extends Block implements BlockEntityProvider {

    public TollCameraBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new TollCameraBlockEntity();
    }

    /** Standard model rendering — without this BlockEntityProvider blocks render invisible. */
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Configuration is server-side only.
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof TollCameraBlockEntity)) {
            return ActionResult.PASS;
        }
        TollCameraBlockEntity camera = (TollCameraBlockEntity) be;
        ItemStack stack = player.getStackInHand(hand);

        // Sneak + empty hand → reset to 0
        if (stack.isEmpty() && player.isSneaking()) {
            camera.setTollAmount(0);
            player.sendMessage(new LiteralText("§e[Toll Camera] §fReset to 0 RFC"), true);
            return ActionResult.CONSUME;
        }

        // RFC coin → add its value
        if (stack.getItem() instanceof RfcCoinItem) {
            int denom = ((RfcCoinItem) stack.getItem()).getDenomination();
            int newAmount = camera.getTollAmount() + denom;
            camera.setTollAmount(newAmount);
            if (!player.abilities.creativeMode) {
                stack.decrement(1);
            }
            player.sendMessage(new LiteralText(
                "§e[Toll Camera] §fToll +" + denom + " RFC  (now " + newAmount + " RFC)"), true);
            return ActionResult.CONSUME;
        }

        // Empty hand (no sneak) or any other item → display current toll
        if (stack.isEmpty()) {
            player.sendMessage(new LiteralText(
                "§e[Toll Camera] §fCurrent toll: " + camera.getTollAmount() + " RFC"), true);
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }
}
