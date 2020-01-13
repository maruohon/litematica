package fi.dy.masa.litematica.util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InventoryUtils;

public class EntityUtils
{
    public static final Predicate<Entity> NOT_PLAYER = new Predicate<Entity>()
    {
        @Override
        public boolean apply(@Nullable Entity entity)
        {
            return (entity instanceof PlayerEntity) == false;
        }
    };

    public static boolean hasToolItem(LivingEntity entity)
    {
        ItemStack toolItem = DataManager.getToolItem();

        if (toolItem.isEmpty())
        {
            return entity.getMainHandStack().isEmpty();
        }

        return isHoldingItem(entity, toolItem);
    }

    public static boolean isHoldingItem(LivingEntity entity, ItemStack stackReference)
    {
        return getHeldItemOfType(entity, stackReference).isEmpty() == false;
    }

    public static ItemStack getHeldItemOfType(LivingEntity entity, ItemStack stackReference)
    {
        ItemStack stack = entity.getMainHandStack();

        if (stack.isEmpty() == false && areStacksEqualIgnoreDurability(stack, stackReference))
        {
            return stack;
        }

        stack = entity.getOffHandStack();

        if (stack.isEmpty() == false && areStacksEqualIgnoreDurability(stack, stackReference))
        {
            return stack;
        }

        return ItemStack.EMPTY;
    }

    /**
     * Checks if the requested item is currently in the player's hand such that it would be used for using/placing.
     * This means, that it must either be in the main hand, or the main hand must be empty and the item is in the offhand.
     * @param player
     * @param stack
     * @return
     */
    @Nullable
    public static Hand getUsedHandForItem(PlayerEntity player, ItemStack stack)
    {
        Hand hand = null;

        if (InventoryUtils.areStacksEqual(player.getMainHandStack(), stack))
        {
            hand = Hand.MAIN_HAND;
        }
        else if (player.getMainHandStack().isEmpty() &&
                 InventoryUtils.areStacksEqual(player.getOffHandStack(), stack))
        {
            hand = Hand.OFF_HAND;
        }

        return hand;
    }

    public static boolean areStacksEqualIgnoreDurability(ItemStack stack1, ItemStack stack2)
    {
        return ItemStack.areItemsEqualIgnoreDamage(stack1, stack2) && ItemStack.areTagsEqual(stack1, stack2);
    }

    public static Direction getHorizontalLookingDirection(Entity entity)
    {
        return Direction.fromRotation(entity.yaw);
    }

    public static Direction getVerticalLookingDirection(Entity entity)
    {
        return entity.pitch > 0 ? Direction.DOWN : Direction.UP;
    }

    public static Direction getClosestLookingDirection(Entity entity)
    {
        if (entity.pitch > 60.0f)
        {
            return Direction.DOWN;
        }
        else if (-entity.pitch > 60.0f)
        {
            return Direction.UP;
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
            if (entity.getUuid().equals(uuid))
            {
                return entity;
            }
        }

        return null;
    }

    @Nullable
    public static String getEntityId(Entity entity)
    {
        EntityType<?> entitytype = entity.getType();
        Identifier resourcelocation = EntityType.getId(entitytype);
        return entitytype.isSaveable() && resourcelocation != null ? resourcelocation.toString() : null;
    }

    @Nullable
    private static Entity createEntityFromNBTSingle(CompoundTag nbt, World world)
    {
        try
        {
            Optional<Entity> optional = EntityType.getEntityFromTag(nbt, world);

            if (optional.isPresent())
            {
                Entity entity = optional.get();
                entity.setUuid(UUID.randomUUID());
                return entity;
            }
        }
        catch (Exception e)
        {
        }

        return null;
    }

    /**
     * Note: This does NOT spawn any of the entities in the world!
     * @param nbt
     * @param world
     * @return
     */
    @Nullable
    public static Entity createEntityAndPassengersFromNBT(CompoundTag nbt, World world)
    {
        Entity entity = createEntityFromNBTSingle(nbt, world);

        if (entity == null)
        {
            return null;
        }
        else
        {
            if (nbt.contains("Passengers", Constants.NBT.TAG_LIST))
            {
                ListTag taglist = nbt.getList("Passengers", Constants.NBT.TAG_COMPOUND);

                for (int i = 0; i < taglist.size(); ++i)
                {
                    Entity passenger = createEntityAndPassengersFromNBT(taglist.getCompound(i), world);

                    if (passenger != null)
                    {
                        passenger.startRiding(entity, true);
                    }
                }
            }

            return entity;
        }
    }

    public static void spawnEntityAndPassengersInWorld(Entity entity, World world)
    {
        if (world.spawnEntity(entity) && entity.hasPassengers())
        {
            for (Entity passenger : entity.getPassengerList())
            {
                passenger.setPosition(entity.getX(), entity.getY() + entity.getMountedHeightOffset() + passenger.getHeightOffset(), entity.getZ());
                spawnEntityAndPassengersInWorld(passenger, world);
            }
        }
    }

    public static List<Entity> getEntitiesWithinSubRegion(World world, BlockPos origin, BlockPos regionPos, BlockPos regionSize,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement)
    {
        // These are the untransformed relative positions
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        BlockPos posEndAbs = PositionUtils.getTransformedPlacementPosition(regionSize.add(-1, -1, -1), schematicPlacement, placement).add(regionPosRelTransformed).add(origin);
        BlockPos regionPosAbs = regionPosRelTransformed.add(origin);
        net.minecraft.util.math.Box bb = PositionUtils.createEnclosingAABB(regionPosAbs, posEndAbs);

        return world.getEntities((Entity) null, bb, null);
    }

    public static boolean shouldPickBlock(PlayerEntity player)
    {
        return Configs.Generic.PICK_BLOCK_ENABLED.getBooleanValue() &&
                (Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue() == false ||
                hasToolItem(player) == false) &&
                Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue();
    }
}
