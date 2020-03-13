package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
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
        Pattern patternRange = Pattern.compile("^(?<start>[0-9])-(?<end>[0-9])$");

        for (String str : parts)
        {
            try
            {
                Matcher matcher = patternRange.matcher(str);

                if (matcher.matches())
                {
                    int slotStart = Integer.parseInt(matcher.group("start"));
                    int slotEnd = Integer.parseInt(matcher.group("end"));

                    if (slotStart <= slotEnd &&
                        InventoryPlayer.isHotbar(slotStart - 1) &&
                        InventoryPlayer.isHotbar(slotEnd - 1))
                    {
                        for (int slotNum = slotStart; slotNum <= slotEnd; ++slotNum)
                        {
                            if (PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
                            {
                                PICK_BLOCKABLE_SLOTS.add(slotNum);
                            }
                        }
                    }
                }
                else
                {
                    int slotNum = Integer.parseInt(str);

                    if (InventoryPlayer.isHotbar(slotNum - 1) &&
                        PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
                    {
                        PICK_BLOCKABLE_SLOTS.add(slotNum);
                    }
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
            Block block = world.getBlockState(pos).getBlock();

            if (block instanceof BlockChest)
            {
                ILockableContainer cont = ((BlockChest) block).getLockableContainer(world, pos);

                if (cont instanceof InventoryLargeChest)
                {
                    inv = (InventoryLargeChest) cont;
                }
            }
        }

        return inv;
    }

    public static void switchItemToHand(ItemStack stack, boolean ignoreNbt, Minecraft mc)
    {
        EntityPlayer player = mc.player;
        InventoryPlayer inventory = player.inventory;
        boolean isCreativeMode = player.capabilities.isCreativeMode;
        int slotWithItem = fi.dy.masa.malilib.util.InventoryUtils.findPlayerInventorySlotWithItem(player.openContainer, stack, ignoreNbt, false);

        if (slotWithItem >= 36 && slotWithItem < 45)
        {
            inventory.currentItem = slotWithItem - 36;
            return;
        }

        // No item or no place to put it
        if ((slotWithItem == -1 && isCreativeMode == false) || PICK_BLOCKABLE_SLOTS.size() == 0)
        {
            return;
        }

        int hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);

        if (hotbarSlot == -1)
        {
            hotbarSlot = getNextPickBlockableHotbarSlot(inventory);
        }

        int slotNum = hotbarSlot + 36;

        if (slotWithItem != -1)
        {
            fi.dy.masa.malilib.util.InventoryUtils.swapSlots(player.openContainer, slotWithItem, hotbarSlot);
        }
        else if (isCreativeMode && InventoryPlayer.isHotbar(hotbarSlot))
        {
            // First try to put the current hotbar item into an empty slot in the player's inventory
            if (inventory.getStackInSlot(hotbarSlot).isEmpty() == false)
            {
                // Shift click the stack
                mc.playerController.windowClick(player.openContainer.windowId, slotNum, 0, ClickType.QUICK_MOVE, player);

                // Wasn't able to move the items out, but the off hand is empty
                if (inventory.getStackInSlot(hotbarSlot).isEmpty() == false && player.getHeldItemOffhand().isEmpty())
                {
                    fi.dy.masa.malilib.util.InventoryUtils.swapSlots(player.openContainer, slotNum, 0);
                    fi.dy.masa.malilib.util.InventoryUtils.swapSlots(player.openContainer, 45, 0);
                    fi.dy.masa.malilib.util.InventoryUtils.swapSlots(player.openContainer, slotNum, 0);
                }
            }

            inventory.currentItem = hotbarSlot;

            inventory.mainInventory.set(hotbarSlot, stack.copy());
            mc.playerController.sendSlotPacket(stack.copy(), slotNum);
        }
    }

    private static int getEmptyPickBlockableHotbarSlot(InventoryPlayer inventory)
    {
        // First check the current slot
        if (PICK_BLOCKABLE_SLOTS.contains(inventory.currentItem + 1) &&
            inventory.mainInventory.get(inventory.currentItem).isEmpty())
        {
            return inventory.currentItem;
        }

        // If the current slot was not empty, then try to find
        // an empty slot among the allowed pick-blockable slots.
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

    private static int getNextPickBlockableHotbarSlot(InventoryPlayer inventory)
    {
        if (PICK_BLOCKABLE_SLOTS.contains(inventory.currentItem + 1))
        {
            ItemStack stack = inventory.mainInventory.get(inventory.currentItem);

            if (stack.isEmpty() || (stack.getItem() instanceof ItemTool) == false)
            {
                return inventory.currentItem;
            }
        }

        if (nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
        {
            nextPickSlotIndex = 0;
        }

        int slotNum = -1;

        // Try to find the next pick-blockable slot that doesn't have a tool in it
        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            slotNum = PICK_BLOCKABLE_SLOTS.get(nextPickSlotIndex) - 1;

            if (++nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
            {
                nextPickSlotIndex = 0;
            }

            ItemStack stack = inventory.mainInventory.get(slotNum);

            if (stack.isEmpty() || (stack.getItem() instanceof ItemTool) == false)
            {
                return slotNum;
            }
        }

        return -1;
    }
}
