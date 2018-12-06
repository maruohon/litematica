package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class InventoryUtils
{
    private static final List<Integer> PICK_BLOCKABLE_SLOTS = new ArrayList<>();

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
                slotNum = PICK_BLOCKABLE_SLOTS.get(0) - 1;
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
