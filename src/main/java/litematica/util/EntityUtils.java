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
import litematica.data.DataManager;

public class EntityUtils
{
    public static boolean testNotPlayer(Entity entity)
    {
        return (entity instanceof EntityPlayer) == false;
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

    public static EnumFacing getClosestLookingDirection(Entity entity)
    {
        return EntityWrap.getClosestLookingDirection(entity, 60F);
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
