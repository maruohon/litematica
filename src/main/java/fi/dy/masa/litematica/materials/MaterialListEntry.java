package fi.dy.masa.litematica.materials;

import net.minecraft.item.ItemStack;

public class MaterialListEntry
{
    private final ItemStack stack;
    private final int countTotal;
    private final int countMissing;
    private int countAvailable;

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

    public void setCountAvailable(int countAvailable)
    {
        this.countAvailable = countAvailable;
    }
}
