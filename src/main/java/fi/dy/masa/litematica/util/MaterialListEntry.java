package fi.dy.masa.litematica.util;

import net.minecraft.item.ItemStack;

public class MaterialListEntry implements Comparable<MaterialListEntry>
{
    private static SortCriteria sortCriteria = SortCriteria.COUNT_TOTAL;
    private static boolean reverse;

    private final ItemStack stack;
    private final int countTotal;
    private final int countMissing;
    private final int countAvailable;

    public MaterialListEntry(ItemStack stack, int countTotal, int countMissing, int countAvailable)
    {
        this.stack = stack;
        this.countTotal = countTotal;
        this.countMissing = countMissing;
        this.countAvailable = countAvailable;
    }

    public ItemStack getStack()
    {
        return this.stack;
    }

    /**
     * Returns the total number of required items of this type in the counted area.
     * @return
     */
    public int getCountTotal()
    {
        return this.countTotal;
    }

    /**
     * Returns the number of items still missing (or having the wrong block state)
     * in the counted area for this item type.
     * @return
     */
    public int getCountMissing()
    {
        return this.countMissing;
    }

    public int getCountAvailable()
    {
        return this.countAvailable;
    }

    @Override
    public int compareTo(MaterialListEntry other)
    {
        if (sortCriteria == SortCriteria.COUNT_TOTAL)
        {
            return this.countTotal == other.countTotal ? 0 : ((this.countTotal > other.countTotal) != reverse ? -1 : 1);
        }
        else if (sortCriteria == SortCriteria.COUNT_MISSING)
        {
            return this.countMissing == other.countMissing ? 0 : ((this.countMissing > other.countMissing) != reverse ? -1 : 1);
        }
        else if (sortCriteria == SortCriteria.COUNT_AVAILABLE)
        {
            return this.countAvailable == other.countAvailable ? 0 : ((this.countAvailable > other.countAvailable) != reverse ? -1 : 1);
        }

        int val = this.stack.getDisplayName().getString().compareTo(other.stack.getDisplayName().getString());
        return reverse ? val * -1 : val;
    }

    public static void setSortCriteria(SortCriteria criteria)
    {
        if (sortCriteria == criteria)
        {
            reverse = ! reverse;
        }
        else
        {
            sortCriteria = criteria;
            reverse = false;
        }
    }

    public enum SortCriteria
    {
        NAME,
        COUNT_TOTAL,
        COUNT_MISSING,
        COUNT_AVAILABLE;
    }
}
