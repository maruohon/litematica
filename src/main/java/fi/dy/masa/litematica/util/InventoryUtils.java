package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class InventoryUtils
{
    private static final List<Integer> PICK_BLOCKABLE_SLOTS = new ArrayList<>();
    private static int nextPickSlotIndex;

    public static void setPickBlockableSlots(String configStr)
    {
        PICK_BLOCKABLE_SLOTS.clear();
        String[] parts = configStr.split(",");

        for (String str : parts)
        {
            try
            {
                int slotNum = Integer.parseInt(str);

                if (InventoryPlayer.isHotbar(slotNum) && PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
                {
                    PICK_BLOCKABLE_SLOTS.add(slotNum);
                }
            }
            catch (NumberFormatException e)
            {
            }
        }
    }

    /**
     * Returns the inventory at the requested location, if any.
     * If the target is a double chest, then the combined inventory is returned.
     * @param world
     * @param pos
     * @return
     */
    @Nullable
    public static IInventory getInventory(World world, BlockPos pos)
    {
        IInventory inv = null;
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof IInventory)
        {
            // Prevent loot generation attempt from crashing due to NPEs
            if (te instanceof TileEntityLockableLoot && (world instanceof WorldServer) == false)
            {
                ((TileEntityLockableLoot) te).setLootTable(null, 0);
            }

            inv = (IInventory) te;
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (block instanceof BlockChest)
            {
                ILockableContainer cont = ((BlockChest) block).getContainer(state, world, pos, true);

                if (cont instanceof InventoryLargeChest)
                {
                    inv = (InventoryLargeChest) cont;
                }
            }
        }

        return inv;
    }

    public static void setPickedItemToHand(ItemStack stack, Minecraft mc)
    {
        EntityPlayer player = mc.player;
        InventoryPlayer inventory = player.inventory;
        int slotNum = inventory.getSlotFor(stack);

        if (InventoryPlayer.isHotbar(slotNum))
        {
            player.inventory.currentItem = slotNum;
        }
        else
        {
            if (PICK_BLOCKABLE_SLOTS.size() == 0)
            {
                return;
            }

            if (slotNum == -1 || InventoryPlayer.isHotbar(slotNum) == false)
            {
                slotNum = getEmptyPickBlockableHotbarSlot(inventory);
            }

            if (slotNum == -1)
            {
                if (PICK_BLOCKABLE_SLOTS.contains(player.inventory.currentItem + 1))
                {
                    slotNum = player.inventory.currentItem;
                }
                else
                {
                    if (nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
                    {
                        nextPickSlotIndex = 0;
                    }

                    slotNum = PICK_BLOCKABLE_SLOTS.get(nextPickSlotIndex) - 1;

                    if (++nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
                    {
                        nextPickSlotIndex = 0;
                    }
                }
            }

            if (slotNum != -1)
            {
                inventory.currentItem = slotNum;

                if (player.abilities.isCreativeMode)
                {
                    inventory.mainInventory.set(slotNum, stack.copy());
                }
                else
                {
                    fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack, mc);
                }
            }
        }
    }

    private static int getEmptyPickBlockableHotbarSlot(InventoryPlayer inventory)
    {
        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            int slotNum = PICK_BLOCKABLE_SLOTS.get(i) - 1;

            if (slotNum >= 0 && slotNum < inventory.mainInventory.size())
            {
                ItemStack stack = inventory.mainInventory.get(slotNum);

                if (stack.isEmpty())
                {
                    return slotNum;
                }
            }
        }

        return -1;
    }
}
