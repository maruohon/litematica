package fi.dy.masa.litematica.util;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class EntityUtils
{
    public static boolean isHoldingItem(EntityLivingBase entity, ItemStack stackReference)
    {
        return getHeldItemOfType(entity, stackReference).isEmpty() == false;
    }

    public static ItemStack getHeldItemOfType(EntityLivingBase entity, ItemStack stackReference)
    {
        ItemStack stack = entity.getHeldItemMainhand();

        if (stack.isEmpty() == false && areStacksEqualIgnoreDurability(stack, stackReference))
        {
            return stack;
        }

        stack = entity.getHeldItemOffhand();

        if (stack.isEmpty() == false && areStacksEqualIgnoreDurability(stack, stackReference))
        {
            return stack;
        }

        return ItemStack.EMPTY;
    }

    public static boolean areStacksEqualIgnoreDurability(ItemStack stack1, ItemStack stack2)
    {
        return ItemStack.areItemsEqualIgnoreDurability(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2);
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

    @Nullable
    public static <T extends Entity> T findEntityByUUID(List<T> list, UUID uuid)
    {
        if (uuid == null)
        {
            return null;
        }

        for (T entity : list)
        {
            if (entity.getUniqueID().equals(uuid))
            {
                return entity;
            }
        }

        return null;
    }
}
