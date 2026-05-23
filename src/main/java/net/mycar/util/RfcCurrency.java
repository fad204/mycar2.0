package net.mycar.util;

import net.mycar.MyCarMod;
import net.mycar.item.RfcCoinItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Inventory helpers for the RFC coin currency. Toll cameras call these to
 * inspect a rider's wallet and deduct the toll cost.
 *
 * Coin denominations are {@code [5, 10, 50, 100, 500, 1000]}. Every toll
 * amount that can be set via the toll-camera UX is a multiple of 5, so an
 * exact-change algorithm is always possible given enough small coins. When
 * the player has only large coins, we take one and refund change in
 * largest-first denominations.
 */
public final class RfcCurrency {

    /** Ordered smallest → largest. */
    public static final int[] DENOMINATIONS = {5, 10, 50, 100, 500, 1000};

    private RfcCurrency() {}

    /** Sum every RFC coin in the player's inventory by face value. */
    public static int sumInventory(PlayerEntity player) {
        int total = 0;
        for (int i = 0; i < player.inventory.size(); i++) {
            ItemStack stack = player.inventory.getStack(i);
            if (stack.getItem() instanceof RfcCoinItem) {
                total += ((RfcCoinItem) stack.getItem()).getDenomination() * stack.getCount();
            }
        }
        return total;
    }

    /**
     * Attempt to deduct {@code amount} RFC from the player's inventory.
     *
     * Returns true on success, false if the player doesn't have enough total
     * RFC value. The algorithm prefers using the smallest denominations first
     * (so the wallet keeps a flexible mix); if a fractional amount remains
     * that no available coin can cover exactly, we take one larger coin and
     * refund the change as a mix of largest-first denominations.
     */
    public static boolean tryCharge(PlayerEntity player, int amount) {
        if (amount <= 0) return true;
        if (sumInventory(player) < amount) return false;

        int remaining = amount;

        // Pass 1: greedy small → large. Only take coins that fit cleanly.
        for (int denom : DENOMINATIONS) {
            if (remaining <= 0) break;
            if (denom > remaining) break; // smallest larger coin handled in Pass 2
            for (int i = 0; i < player.inventory.size() && remaining >= denom; i++) {
                ItemStack stack = player.inventory.getStack(i);
                if (stack.getItem() instanceof RfcCoinItem
                        && ((RfcCoinItem) stack.getItem()).getDenomination() == denom) {
                    int canTake = Math.min(stack.getCount(), remaining / denom);
                    if (canTake > 0) {
                        stack.decrement(canTake);
                        remaining -= canTake * denom;
                    }
                }
            }
        }

        // Pass 2: if remaining > 0, find smallest coin >= remaining to break.
        if (remaining > 0) {
            for (int denom : DENOMINATIONS) {
                if (denom < remaining) continue;
                if (consumeOneCoin(player, denom)) {
                    int change = denom - remaining;
                    remaining = 0;
                    if (change > 0) giveChange(player, change);
                    break;
                }
            }
        }

        return remaining == 0;
    }

    /** Try to consume exactly one coin of the given denomination. */
    private static boolean consumeOneCoin(PlayerEntity player, int denom) {
        for (int i = 0; i < player.inventory.size(); i++) {
            ItemStack stack = player.inventory.getStack(i);
            if (stack.getItem() instanceof RfcCoinItem
                    && ((RfcCoinItem) stack.getItem()).getDenomination() == denom
                    && stack.getCount() > 0) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }

    /** Give the player {@code amount} RFC back as coins, largest first. */
    private static void giveChange(PlayerEntity player, int amount) {
        // Iterate denominations largest → smallest.
        for (int i = DENOMINATIONS.length - 1; i >= 0 && amount > 0; i--) {
            int denom = DENOMINATIONS[i];
            int count = amount / denom;
            if (count <= 0) continue;
            Item coinItem = coinItemFor(denom);
            if (coinItem == null) continue;
            ItemStack stack = new ItemStack(coinItem, count);
            // offerOrDrop inserts in inventory if room, drops at player feet otherwise.
            player.inventory.offerOrDrop(player.world, stack);
            amount -= count * denom;
        }
    }

    /** Resolve a denomination to the registered coin Item. */
    private static Item coinItemFor(int denom) {
        switch (denom) {
            case 5:    return MyCarMod.RFC_5;
            case 10:   return MyCarMod.RFC_10;
            case 50:   return MyCarMod.RFC_50;
            case 100:  return MyCarMod.RFC_100;
            case 500:  return MyCarMod.RFC_500;
            case 1000: return MyCarMod.RFC_1000;
            default:   return null;
        }
    }
}
