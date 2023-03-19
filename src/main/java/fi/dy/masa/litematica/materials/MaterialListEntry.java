package fi.dy.masa.litematica.materials;

import fi.dy.masa.malilib.util.ItemType;
import net.minecraft.item.ItemStack;

public class MaterialListEntry
{
    private final ItemType item;
    private final int countTotal;
    private final int countMissing;
    private final int countMismatched;
    private int countAvailable;

    public MaterialListEntry(ItemStack stack, int countTotal, int countMissing, int countMismatched, int countAvailable)
    {
        this.item = new ItemType(stack, true, false);
        this.countTotal = countTotal;
        this.countMissing = countMissing;
        this.countMismatched = countMismatched;
        this.countAvailable = countAvailable;
    }

    public ItemStack getStack()
    {
        return this.item.getStack();
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

    public int getCountMismatched()
    {
        return this.countMismatched;
    }

    public int getCountAvailable()
    {
        return this.countAvailable;
    }

    public void setCountAvailable(int countAvailable)
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
