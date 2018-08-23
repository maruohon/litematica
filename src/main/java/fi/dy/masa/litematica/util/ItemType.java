package fi.dy.masa.litematica.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ItemType
{
    private ItemStack stack;
    private boolean checkNBT;

    public ItemType(ItemStack stack)
    {
        this(stack, true, true);
    }

    public ItemType(ItemStack stack, boolean copy, boolean checkNBT)
    {
        this.stack = stack.isEmpty() ? ItemStack.EMPTY : (copy ? stack.copy() : stack);
        this.checkNBT = checkNBT;
    }

    public ItemStack getStack()
    {
        return this.stack;
    }

    public boolean checkNBT()
    {
        return this.checkNBT;
    }

    public void setStack(ItemStack stack)
    {
        this.stack = stack;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.stack.getMetadata();
        result = prime * result + this.stack.getItem().hashCode();

        if (this.checkNBT())
        {
            result = prime * result + (this.stack.getTagCompound() != null ? this.stack.getTagCompound().hashCode() : 0);
        }

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

        ItemType other = (ItemType) obj;

        if (this.stack.isEmpty() || other.stack.isEmpty())
        {
            if (this.stack.isEmpty() != other.stack.isEmpty())
            {
                return false;
            }
        }
        else
        {
            if (this.stack.getMetadata() != other.stack.getMetadata())
            {
                return false;
            }

            if (this.stack.getItem() != other.stack.getItem())
            {
                return false;
            }

            return this.checkNBT() == false || ItemStack.areItemStackTagsEqual(this.stack, other.stack);
        }

        return true;
    }

    @Override
    public String toString()
    {
        if (this.checkNBT())
        {
            ResourceLocation rl = Item.REGISTRY.getNameForObject(this.stack.getItem());
            return rl.toString() + "@" + this.stack.getMetadata() + this.stack.getTagCompound();
        }
        else
        {
            ResourceLocation rl = Item.REGISTRY.getNameForObject(this.stack.getItem());
            return rl.toString() + "@" + this.stack.getMetadata();
        }
    }
}
