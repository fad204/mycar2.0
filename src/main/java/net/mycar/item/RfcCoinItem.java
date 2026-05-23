package net.mycar.item;

import net.minecraft.item.Item;

/**
 * A coin item denominated in RFC, the currency used by toll cameras.
 *
 * Each instance has a fixed {@link #denomination} (5, 10, 50, 100, 500, 1000),
 * crafted 1:1 from a vanilla item of corresponding rarity:
 *   5 RFC ←→ iron nugget
 *  10 RFC ←→ iron ingot
 *  50 RFC ←→ gold nugget
 * 100 RFC ←→ gold ingot
 * 500 RFC ←→ emerald
 * 1000 RFC ←→ diamond
 *
 * Coin stacks add normally; toll cameras consume them via {@link
 * net.mycar.util.RfcCurrency}.
 */
public class RfcCoinItem extends Item {

    private final int denomination;

    public RfcCoinItem(Settings settings, int denomination) {
        super(settings);
        this.denomination = denomination;
    }

    public int getDenomination() {
        return this.denomination;
    }
}
