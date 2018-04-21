package fi.dy.masa.litematica.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class EntityUtils
{
    public static boolean isHoldingItem(EntityLivingBase entity, Item item)
    {
        return getHeldItemOfType(entity, item).isEmpty() == false;
    }

    public static ItemStack getHeldItemOfType(EntityLivingBase entity, Item item)
    {
        ItemStack stack = entity.getHeldItemMainhand();

        if (stack.isEmpty() == false && stack.getItem() == item)
        {
            return stack;
        }

        stack = entity.getHeldItemOffhand();

        if (stack.isEmpty() == false && stack.getItem() == item)
        {
            return stack;
        }

        return ItemStack.EMPTY;
    }

    public static EnumFacing getHorizontalLookingDirection(Entity entity)
    {
        return EnumFacing.fromAngle(entity.rotationYaw);
    }

    public static EnumFacing getVerticalLookingDirection(Entity entity)
    {
        return entity.rotationPitch > 0 ? EnumFacing.DOWN : EnumFacing.UP;
    }

    public static EnumFacing getClosestLookingDirection(Entity entity)
    {
        if (entity.rotationPitch > 60.0f)
        {
            return EnumFacing.DOWN;
        }
        else if (-entity.rotationPitch > 60.0f)
        {
            return EnumFacing.UP;
        }

        return getHorizontalLookingDirection(entity);
    }
}
