package fi.dy.masa.litematica.util;

import net.minecraft.item.ItemStack;

public class MaterialListEntry implements Comparable<MaterialListEntry>
{
    private static SortCriteria sortCriteria = SortCriteria.COUNT_REQUIRED;
    private static boolean reverse;

    private final ItemStack stack;
    private int countRequired;
    private int countAvailable;

    public MaterialListEntry(ItemStack stack, int countRequired, int countAvailable)
    {
        this.stack = stack;
        this.countRequired = countRequired;
        this.countAvailable = countAvailable;
    }

    public ItemStack getStack()
    {
        return this.stack;
    }

    public int getCountRequired()
    {
        return this.countRequired;
    }

    public int getCountAvailable()
    {
        return this.countAvailable;
    }

    @Override
    public int compareTo(MaterialListEntry other)
    {
        if (sortCriteria == SortCriteria.COUNT_REQUIRED)
        {
            return this.countRequired == other.countRequired ? 0 : ((this.countRequired > other.countRequired) != reverse ? -1 : 1);
        }
        else if (sortCriteria == SortCriteria.COUNT_AVAILABLE)
        {
            return this.countAvailable == other.countAvailable ? 0 : ((this.countAvailable > other.countAvailable) != reverse ? -1 : 1);
        }

        int val = this.stack.getDisplayName().compareTo(other.stack.getDisplayName());
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
        COUNT_REQUIRED,
        COUNT_AVAILABLE;
    }
}
