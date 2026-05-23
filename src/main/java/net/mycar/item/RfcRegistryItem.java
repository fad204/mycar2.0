package net.mycar.item;

import net.mycar.util.RfcAccountState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Right-click in hand to print every plate in the world's RFC account state
 * to chat, with balance and debt. Debt plates are listed first (most urgent)
 * with a red prefix; debt-free plates follow in alphabetical order.
 *
 * Crafted from a vanilla book + an RFC 10 coin (shapeless recipe).
 */
public class RfcRegistryItem extends Item {

    public RfcRegistryItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient) {
            RfcAccountState state = RfcAccountState.get((ServerWorld) world);
            Map<String, int[]> all = state.snapshotAllPlates();

            player.sendMessage(new LiteralText("§6=== RFC Account Registry ==="), false);

            if (all.isEmpty()) {
                player.sendMessage(new LiteralText("§7No plates registered yet."), false);
            } else {
                // Sort: debt descending (worst offenders first), then plate name asc.
                List<Map.Entry<String, int[]>> sorted = new ArrayList<>(all.entrySet());
                sorted.sort(Comparator
                    .<Map.Entry<String, int[]>>comparingInt(e -> -e.getValue()[1])
                    .thenComparing(Map.Entry::getKey));

                for (Map.Entry<String, int[]> e : sorted) {
                    String plate = e.getKey();
                    int bal  = e.getValue()[0];
                    int debt = e.getValue()[1];
                    StringBuilder line = new StringBuilder();
                    if (debt > 0) {
                        line.append("§c⚠ ").append(plate);
                        line.append("§7  balance §f").append(bal);
                        line.append("§7  debt §c").append(debt).append(" RFC");
                    } else {
                        line.append("§a✓ ").append(plate);
                        line.append("§7  balance §f").append(bal).append(" RFC");
                    }
                    player.sendMessage(new LiteralText(line.toString()), false);
                }
                player.sendMessage(new LiteralText(
                    "§7" + sorted.size() + " plate" + (sorted.size() == 1 ? "" : "s") + " registered"
                ), false);
            }
        }
        return TypedActionResult.success(player.getStackInHand(hand));
    }
}
