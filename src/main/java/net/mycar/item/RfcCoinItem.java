package net.mycar.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;

import org.jetbrains.annotations.Nullable;

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

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world,
                              List<Text> tooltip, TooltipContext context) {
        // Gold-on-gray hover line clarifying the denomination, since the
        // 16x16 icon has limited room for the full numeric value.
        tooltip.add(new LiteralText("§7Value: §6" + this.denomination + " RFC"));
    }
}
