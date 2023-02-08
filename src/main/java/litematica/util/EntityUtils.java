package litematica.util;

import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameUtils;
import malilib.util.game.wrap.ItemWrap;
import malilib.util.game.wrap.NbtWrap;
import malilib.util.inventory.InventoryUtils;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.render.RenderUtils;

public class EntityUtils
{
    public static boolean testNotPlayer(Entity entity)
    {
        return (entity instanceof EntityPlayer) == false;
    }

    public static boolean shouldPickBlock()
    {
        return Configs.Generic.PICK_BLOCK_ENABLED.getBooleanValue() &&
               (Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue() == false || hasToolItem() == false) &&
               RenderUtils.areSchematicBlocksCurrentlyRendered();
    }

    public static boolean hasToolItem()
    {
        // If the configured tool item has NBT data, then the NBT is compared, otherwise it's ignored

        EntityLivingBase entity = GameUtils.getClientPlayer();
        ItemStack toolItem = DataManager.getToolItem();

        if (ItemWrap.isEmpty(toolItem))
        {
            return ItemWrap.isEmpty(EntityWrap.getMainHandItem(entity));
        }

        return hasToolItemInHand(entity, EnumHand.MAIN_HAND, toolItem) ||
               hasToolItemInHand(entity, EnumHand.OFF_HAND, toolItem);
    }

    protected static boolean hasToolItemInHand(EntityLivingBase entity, EnumHand hand, ItemStack toolItem)
    {
        ItemStack stack = EntityWrap.getHeldItem(entity, hand);

        if (ItemStack.areItemsEqualIgnoreDurability(toolItem, stack))
        {
            return toolItem.hasTagCompound() == false || ItemStack.areItemStackTagsEqual(toolItem, stack);
        }

        return false;
    }

    /**
     * Checks if the requested item is currently in the player's hand such that it would be used for using/placing.
     * This means, that it must either be in the main hand, or the main hand must be empty and the item is in the offhand.
     * @param lenient if true, then NBT tags and also damage of damageable items are ignored
     */
    @Nullable
    public static EnumHand getUsedHandForItem(EntityPlayer player, ItemStack stack, boolean lenient)
    {
        EnumHand hand = null;
        EnumHand tmpHand = ItemWrap.isEmpty(EntityWrap.getMainHandItem(player)) ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        ItemStack handStack = EntityWrap.getHeldItem(player, tmpHand);

        if ((lenient          && ItemStack.areItemsEqualIgnoreDurability(handStack, stack)) ||
            (lenient == false && InventoryUtils.areStacksEqual(handStack, stack)))
        {
            hand = tmpHand;
        }

        return hand;
    }

    public static EnumFacing getHorizontalLookingDirection(Entity entity)
    {
        return EnumFacing.fromAngle(EntityWrap.getYaw(entity));
    }

    public static EnumFacing getClosestLookingDirection(Entity entity)
    {
        float pitch = EntityWrap.getPitch(entity);

        if (pitch > 60.0f)
        {
            return EnumFacing.DOWN;
        }
        else if (-pitch > 60.0f)
        {
            return EnumFacing.UP;
        }

        return getHorizontalLookingDirection(entity);
    }

    public static boolean setFakedSneakingState(boolean sneaking)
    {
        EntityPlayerSP player = GameUtils.getClientPlayer();

        if (player != null && player.isSneaking() != sneaking)
        {
            CPacketEntityAction.Action action = sneaking ? CPacketEntityAction.Action.START_SNEAKING : CPacketEntityAction.Action.STOP_SNEAKING;
            player.connection.sendPacket(new CPacketEntityAction(player, action));
            player.movementInput.sneak = sneaking;
            return true;
        }

        return false;
    }

    @Nullable
    private static Entity createSingleEntityFromNbt(NBTTagCompound nbt, World world)
    {
        try
        {
            Entity entity = EntityList.createEntityFromNBT(nbt, world);

            if (entity != null)
            {
                entity.setUniqueId(UUID.randomUUID());
            }

            return entity;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Note: This does NOT spawn any of the entities in the world!
     */
    @Nullable
    public static Entity createEntityAndPassengersFromNbt(NBTTagCompound nbt, World world)
    {
        Entity entity = createSingleEntityFromNbt(nbt, world);

        if (entity == null)
        {
            return null;
        }
        else
        {
            if (NbtWrap.containsList(nbt, "Passengers"))
            {
                NBTTagList list = NbtWrap.getListOfCompounds(nbt, "Passengers");
                final int size = NbtWrap.getListSize(list);

                for (int i = 0; i < size; ++i)
                {
                    Entity passenger = createEntityAndPassengersFromNbt(NbtWrap.getCompoundAt(list, i), world);

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
        if (world.spawnEntity(entity) && entity.isBeingRidden())
        {
            for (Entity passenger : entity.getPassengers())
            {
                passenger.setPosition(EntityWrap.getX(entity),
                                      EntityWrap.getY(entity) + entity.getMountedYOffset() + passenger.getYOffset(),
                                      EntityWrap.getZ(entity));
                spawnEntityAndPassengersInWorld(passenger, world);
            }
        }
    }
}
