package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

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

                if (PlayerInventory.isValidHotbarIndex(slotNum) && PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
                {
                    PICK_BLOCKABLE_SLOTS.add(slotNum);
                }
            }
            catch (NumberFormatException e)
            {
            }
        }
    }

    public static void setPickedItemToHand(ItemStack stack, MinecraftClient mc)
    {
        PlayerEntity player = mc.player;
        PlayerInventory inventory = player.inventory;
        int slotNum = inventory.getSlotWithStack(stack);

        if (PlayerInventory.isValidHotbarIndex(slotNum))
        {
            player.inventory.selectedSlot = slotNum;
        }
        else
        {
            if (PICK_BLOCKABLE_SLOTS.size() == 0)
            {
                return;
            }

            if (slotNum == -1 || PlayerInventory.isValidHotbarIndex(slotNum) == false)
            {
                slotNum = getEmptyPickBlockableHotbarSlot(inventory);
            }

            if (slotNum == -1)
            {
                if (PICK_BLOCKABLE_SLOTS.contains(player.inventory.selectedSlot + 1))
                {
                    slotNum = player.inventory.selectedSlot;
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
                inventory.selectedSlot = slotNum;

                if (player.abilities.creativeMode)
                {
                    inventory.main.set(slotNum, stack.copy());
                }
                else
                {
                    fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack.copy(), mc);
                }
            }
        }
    }

    private static int getEmptyPickBlockableHotbarSlot(PlayerInventory inventory)
    {
        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            int slotNum = PICK_BLOCKABLE_SLOTS.get(i) - 1;

            if (slotNum >= 0 && slotNum < inventory.main.size())
            {
                ItemStack stack = inventory.main.get(slotNum);

                if (stack.isEmpty())
                {
                    return slotNum;
                }
            }
        }

        return -1;
    }
}
