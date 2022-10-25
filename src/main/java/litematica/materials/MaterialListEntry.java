package litematica.materials;

import net.minecraft.item.ItemStack;

import malilib.util.data.ItemType;

public class MaterialListEntry
{
    private final ItemType item;
    private final long countTotal;
    private final long countMissing;
    private final long countMismatched;
    private long countAvailable;

    public MaterialListEntry(ItemStack stack, long countTotal, long countMissing, long countMismatched, long countAvailable)
    {
        this.item = new ItemType(stack, false, false);
        this.countTotal = countTotal;
        this.countMissing = countMissing;
        this.countMismatched = countMismatched;
        this.countAvailable = countAvailable;
    }

    public ItemType getItemType()
    {
        return this.item;
    }

    public ItemStack getStack()
    {
        return this.item.getStack();
    }

    /**
     * Returns the total number of required items of this type in the counted area.
     * @return
     */
    public long getTotalCount()
    {
        return this.countTotal;
    }

    /**
     * Returns the number of items still missing (or having the wrong block state)
     * in the counted area for this item type.
     * @return
     */
    public long getMissingCount()
    {
        return this.countMissing;
    }

    public long getCountMismatched()
    {
        return this.countMismatched;
    }

    public long getAvailableCount()
    {
        return this.countAvailable;
    }

    public void setCountAvailable(long countAvailable)
    {
        this.countAvailable = countAvailable;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.item == null) ? 0 : this.item.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MaterialListEntry other = (MaterialListEntry) obj;
        if (this.item == null)
        {
            if (other.item != null)
                return false;
        }
        else if (! this.item.equals(other.item))
            return false;
        return true;
    }
}
