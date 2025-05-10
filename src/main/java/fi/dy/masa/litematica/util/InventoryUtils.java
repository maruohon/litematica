package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.MaLiLib;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.OverlayRenderer;

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
                int slotNum = Integer.parseInt(str) - 1;

                if (PlayerInventory.isValidHotbarIndex(slotNum) &&
                    PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
                {
                    PICK_BLOCKABLE_SLOTS.add(slotNum);
                }
            }
            catch (NumberFormatException ignore) {}
        }
    }

    public static void setPickedItemToHand(ItemStack stack, MinecraftClient mc)
    {
        int slotNum = mc.player.getInventory().getSlotWithStack(stack);
        setPickedItemToHand(slotNum, stack, mc);
    }

    public static void setPickedItemToHand(int sourceSlot, ItemStack stack, MinecraftClient mc)
    {
        PlayerEntity player = mc.player;
        PlayerInventory inventory = player.getInventory();

        if (PlayerInventory.isValidHotbarIndex(sourceSlot))
        {
            inventory.selectedSlot = sourceSlot;
        }
        else
        {
            if (PICK_BLOCKABLE_SLOTS.size() == 0)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
                return;
            }

            int hotbarSlot = sourceSlot;

            if (sourceSlot == -1 || PlayerInventory.isValidHotbarIndex(sourceSlot) == false)
            {
                hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);
            }

            if (hotbarSlot == -1)
            {
                hotbarSlot = getPickBlockTargetSlot(player);
            }

            if (hotbarSlot != -1)
            {
                inventory.selectedSlot = hotbarSlot;

                if (EntityUtils.isCreativeMode(player))
                {
                    inventory.main.set(hotbarSlot, stack.copy());
                }
                else
                {
                    fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack.copy(), mc);
                }

                WorldUtils.setEasyPlaceLastPickBlockTime();
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            }
        }
    }

    public static void schematicWorldPickBlock(ItemStack stack, BlockPos pos,
                                               World schematicWorld, MinecraftClient mc)
    {
        MaLiLib.logger.error("schematicWorldPickBlock stack.isEmpty() {} ", stack.isEmpty());
        if (stack.isEmpty() == false)
        {
            PlayerInventory inv = mc.player.getInventory();
            stack = stack.copy();
            MaLiLib.logger.error("schematicWorldPickBlock EntityUtils.isCreativeMode(mc.player) {} ", EntityUtils.isCreativeMode(mc.player));
            if (EntityUtils.isCreativeMode(mc.player))
            {
                BlockEntity te = schematicWorld.getBlockEntity(pos);

                // The creative mode pick block with NBT only works correctly
                // if the server world doesn't have a TileEntity in that position.
                // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                if (GuiBase.isCtrlDown() && te != null && mc.world.isAir(pos))
                {
                    te.setStackNbt(stack, schematicWorld.getRegistryManager());
                    //stack.set(DataComponentTypes.LORE, new LoreComponent(ImmutableList.of(Text.of("(+NBT)"))));
                }

                setPickedItemToHand(stack, mc);
                mc.interactionManager.clickCreativeStack(mc.player.getStackInHand(Hand.MAIN_HAND), 36 + inv.selectedSlot);

                //return true;
            }
            else
            {
                MaLiLib.logger.error("schematicWorldPickBlock getSlotWithStack invoking {} {}", stack.toString(), pos.toString());
                int slot = inv.getSlotWithStack(stack);
                MaLiLib.logger.error("schematicWorldPickBlock getSlotWithStack ret slot {} selected slot {}", slot, inv.selectedSlot);
                boolean shouldPick = inv.selectedSlot != slot;

                if (shouldPick && slot != -1)
                {
                    setPickedItemToHand(stack, mc);
                }
                else if (slot == -1 && Configs.Generic.PICK_BLOCK_SHULKERS.getBooleanValue())
                {

                    slot = findSlotWithBoxWithItem(mc.player.playerScreenHandler, stack, false);
                    MaLiLib.logger.error("schematicWorldPickBlock findSlotWithBoxWithItem ret slot {}", slot);
                    if (slot != -1)
                    {
                        ItemStack boxStack = mc.player.playerScreenHandler.slots.get(slot).getStack();
                        setPickedItemToHand(boxStack, mc);
                    }
                }

                //return shouldPick == false || canPick;
            }
        }
    }

    private static boolean canPickToSlot(PlayerInventory inventory, int slotNum)
    {
        if (PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
        {
            return false;
        }

        ItemStack stack = inventory.getStack(slotNum);

        if (stack.isEmpty())
        {
            return true;
        }

        return (Configs.Generic.PICK_BLOCK_AVOID_DAMAGEABLE.getBooleanValue() == false ||
                stack.isDamageable() == false) &&
               (Configs.Generic.PICK_BLOCK_AVOID_TOOLS.getBooleanValue() == false ||
                (stack.getItem() instanceof ToolItem) == false);
    }

    private static int getPickBlockTargetSlot(PlayerEntity player)
    {
        if (PICK_BLOCKABLE_SLOTS.isEmpty())
        {
            return -1;
        }

        int slotNum = player.getInventory().selectedSlot;

        if (canPickToSlot(player.getInventory(), slotNum))
        {
            return slotNum;
        }

        if (nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
        {
            nextPickSlotIndex = 0;
        }

        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            slotNum = PICK_BLOCKABLE_SLOTS.get(nextPickSlotIndex);

            if (++nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
            {
                nextPickSlotIndex = 0;
            }

            if (canPickToSlot(player.getInventory(), slotNum))
            {
                return slotNum;
            }
        }

        return -1;
    }

    private static int getEmptyPickBlockableHotbarSlot(PlayerInventory inventory)
    {
        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            int slotNum = PICK_BLOCKABLE_SLOTS.get(i);

            if (PlayerInventory.isValidHotbarIndex(slotNum))
            {
                ItemStack stack = inventory.getStack(slotNum);

                if (stack.isEmpty())
                {
                    return slotNum;
                }
            }
        }

        return -1;
    }

    public static boolean doesShulkerBoxContainItem(ItemStack stack, ItemStack referenceItem)
    {
        DefaultedList<ItemStack> items = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack);

        return doesListContainItem(items, referenceItem);
    }

    public static boolean doesBundleContainItem(ItemStack stack, ItemStack referenceItem)
    {
        DefaultedList<ItemStack> items = fi.dy.masa.malilib.util.InventoryUtils.getBundleItems(stack);

        return doesListContainItem(items, referenceItem);
    }

    private static boolean doesListContainItem(DefaultedList<ItemStack> items, ItemStack referenceItem)
    {
        if (items.size() > 0)
        {
            for (ItemStack item : items)
            {
                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt(item, referenceItem))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static int findSlotWithBoxWithItem(ScreenHandler container, ItemStack stackReference, boolean reverse)
    {
        final int startSlot = reverse ? container.slots.size() - 1 : 0;
        final int endSlot = reverse ? -1 : container.slots.size();
        final int increment = reverse ? -1 : 1;
        final boolean isPlayerInv = container instanceof PlayerScreenHandler;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment)
        {
            Slot slot = container.slots.get(slotNum);

            if ((isPlayerInv == false || fi.dy.masa.malilib.util.InventoryUtils.isRegularInventorySlot(slot.id, false)) &&
                doesShulkerBoxContainItem(slot.getStack(), stackReference))
            {
                return slot.id;
            }
        }

        return -1;
    }

    /**
     * Get a valid Inventory Object by any means necessary.
     *
     * @param world (Input ClientWorld)
     * @param pos (Pos of the Tile Entity)
     * @return (The result Inventory | NULL if not obtainable)
     */
    @Nullable
    public static Inventory getInventory(World world, BlockPos pos)
    {
        Inventory inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);

        if ((inv == null || inv.isEmpty()) && DataManager.getInstance().hasIntegratedServer() == false)
        {
            OverlayRenderer.getInstance().requestBlockEntityAt(world, pos);
        }

        return inv;
    }

    /**
     * Converts an NbtCompound representation of an ItemStack into a '/give' compatible string.
     * This is the format used by the ItemStringReader(), including Data Components.
     *
     * @param nbt (Nbt Input, must be valid ItemStack.encode() format)
     * @return (The String Result | NULL if the NBT is invalid)
     */
    @Nullable
    public static String convertItemNbtToString(NbtCompound nbt)
    {
        StringBuilder result = new StringBuilder();

        if (nbt.isEmpty())
        {
            return null;
        }

        if (nbt.contains("id"))
        {
            result.append(nbt.getString("id"));
        }
        else
        {
            return null;
        }
        if (nbt.contains("components"))
        {
            NbtCompound components = nbt.getCompound("components");
            int count = 0;

            result.append("[");

            for (String key : components.getKeys())
            {
                if (count > 0)
                {
                    result.append(", ");
                }

                result.append(key);
                result.append("=");
                result.append(components.get(key));
                count++;
            }

            result.append("]");
        }
        if (nbt.contains("count"))
        {
            int count = nbt.getInt("count");

            if (count > 1)
            {
                result.append(" ");
                result.append(count);
            }
        }

        return result.toString();
    }
}
