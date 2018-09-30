package fi.dy.masa.litematica.util;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.data.SchematicPlacement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

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

    public static List<Entity> getEntitiesWithinSubRegion(World world, BlockPos origin, BlockPos regionPos, BlockPos regionSize,
            SchematicPlacement schematicPlacement, Placement placement)
    {
        // These are the untransformed relative positions
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        BlockPos posEndAbs = PositionUtils.getTransformedPlacementPosition(regionSize.add(-1, -1, -1), schematicPlacement, placement).add(regionPosRelTransformed).add(origin);
        BlockPos regionPosAbs = regionPosRelTransformed.add(origin);
        AxisAlignedBB bb = PositionUtils.createEnclosingAABB(regionPosAbs, posEndAbs);

        return world.getEntitiesInAABBexcluding(null, bb, null);
    }

    /**
     * First checks if there are existing entities in the given list by the same UUID,
     * and if so, removes that old entity from the world.
     * If there are entities by the same UUID in the world after that,
     * then the passed entity will be assigned a new random UUID.
     * @param world
     * @param entity
     * @param existingEntitiesInArea
     */
    public static void handleSchematicPlacementEntityUUIDCollision(World world, Entity entity, List<Entity> existingEntitiesInArea)
    {
        // Use the original UUID if possible. If there is an entity with the same UUID within the pasted area,
        // then the old one will be killed. Otherwise if there is no entity currently in the world with
        // the same UUID, then the original UUID will be used.
        UUID uuidOriginal = entity.getUniqueID();

        // An existing entity with the same UUID is somewhere else in the world, use a new random UUID for the new entity.
        if (world instanceof WorldServer && ((WorldServer) world).getEntityFromUuid(uuidOriginal) != null)
        {
            Entity existingEntityWithinArea = EntityUtils.findEntityByUUID(existingEntitiesInArea, uuidOriginal);

            // An existing entity by with the same UUID is within the same sub-region area, remove the old entity.
            if (existingEntityWithinArea != null)
            {
                world.removeEntityDangerously(existingEntityWithinArea);
            }
            else
            {
                entity.setUniqueId(UUID.randomUUID());
            }
        }
    }
}
