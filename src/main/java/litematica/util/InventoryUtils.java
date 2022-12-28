package litematica.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import malilib.gui.BaseScreen;
import malilib.registry.Registry;
import malilib.util.game.wrap.GameUtils;
import malilib.util.game.wrap.ItemWrap;
import litematica.config.Configs;
import litematica.materials.MaterialCache;
import litematica.world.SchematicWorldHandler;

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
                    int slotStart = Integer.parseInt(matcher.group("start")) - 1;
                    int slotEnd = Integer.parseInt(matcher.group("end")) - 1;

                    if (slotStart <= slotEnd &&
                        InventoryPlayer.isHotbar(slotStart) &&
                        InventoryPlayer.isHotbar(slotEnd))
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
                    int slotNum = Integer.parseInt(str) - 1;

                    if (InventoryPlayer.isHotbar(slotNum) &&
                        PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
                    {
                        PICK_BLOCKABLE_SLOTS.add(slotNum);
                    }
                }
            }
            catch (NumberFormatException ignore) {}
        }
    }

    public static boolean switchItemToHand(ItemStack stack, boolean ignoreNbt)
    {
        if (PICK_BLOCKABLE_SLOTS.size() == 0)
        {
            return false;
        }

        EntityPlayer player = GameUtils.getClientPlayer();
        InventoryPlayer inventory = GameUtils.getPlayerInventory();
        Container container = GameUtils.getCurrentInventoryContainer();
        boolean isCreativeMode = GameUtils.isCreativeMode();
        int slotWithItem = malilib.util.inventory.InventoryUtils.findSlotWithItemToPickBlock(container, stack, ignoreNbt);

        // No item or no place to put it
        if (slotWithItem == -1 && isCreativeMode == false)
        {
            return false;
        }

        if (slotWithItem >= 36 && slotWithItem < 45)
        {
            inventory.currentItem = slotWithItem - 36;
            return true;
        }

        int hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);

        if (hotbarSlot == -1)
        {
            hotbarSlot = getNextPickBlockableHotbarSlot(inventory);
        }

        if (slotWithItem != -1)
        {
            malilib.util.inventory.InventoryUtils.swapSlots(container, slotWithItem, hotbarSlot);
            inventory.currentItem = hotbarSlot;
            return true;
        }
        else if (isCreativeMode && InventoryPlayer.isHotbar(hotbarSlot))
        {
            int slotNum = hotbarSlot + 36;

            // First try to put the current hotbar item into an empty slot in the player's inventory
            if (ItemWrap.notEmpty(inventory.getStackInSlot(hotbarSlot)))
            {
                // Shift click the stack
                GameUtils.getInteractionManager().windowClick(container.windowId, slotNum, 0, ClickType.QUICK_MOVE, player);

                // Wasn't able to move the items out
                if (ItemWrap.notEmpty(inventory.getStackInSlot(hotbarSlot)))
                {
                    // TODO try to combine partial stacks

                    // The off-hand slot is empty, move the current stack to it
                    if (ItemWrap.isEmpty(player.getHeldItemOffhand()))
                    {
                        malilib.util.inventory.InventoryUtils.swapSlots(container, slotNum, 0);
                        malilib.util.inventory.InventoryUtils.swapSlots(container, 45, 0);
                        malilib.util.inventory.InventoryUtils.swapSlots(container, slotNum, 0);
                    }
                }
            }

            inventory.currentItem = hotbarSlot;
            inventory.mainInventory.set(hotbarSlot, stack.copy());
            GameUtils.getInteractionManager().sendSlotPacket(stack.copy(), slotNum);

            return true;
        }

        return false;
    }

    /**
     * Does a ray trace to the schematic world, and returns either the closest or the furthest hit block.
     * @return true if the correct item was or is in the player's hand after the pick block
     */
    public static boolean pickBlockFirst()
    {
        double reach = GameUtils.getInteractionManager().getBlockReachDistance();
        Entity entity = GameUtils.getCameraEntity();
        BlockPos pos = RayTraceUtils.getSchematicWorldTraceIfClosest(GameUtils.getClientWorld(), entity, reach);

        if (pos != null)
        {
            doPickBlockForPosition(pos);
            return true;
        }

        return false;
    }

    @Nullable
    public static EnumHand pickBlockLast(boolean adjacentOnly)
    {
        World world = GameUtils.getClientWorld();
        BlockPos pos = Registry.BLOCK_PLACEMENT_POSITION_HANDLER.getCurrentPlacementPosition();

        // No overrides by other mods
        if (pos == null)
        {
            double reach = GameUtils.getInteractionManager().getBlockReachDistance();
            Entity entity = GameUtils.getCameraEntity();
            pos = RayTraceUtils.getPickBlockLastTrace(world, entity, reach, adjacentOnly);
        }

        if (pos != null)
        {
            IBlockState state = world.getBlockState(pos);

            if (state.getBlock().isReplaceable(world, pos) || state.getMaterial().isReplaceable())
            {
                return doPickBlockForPosition(pos);
            }
        }

        return null;
    }

    @Nullable
    public static EnumHand doPickBlockForPosition(BlockPos pos)
    {
        World world = SchematicWorldHandler.getSchematicWorld();
        IBlockState state = world.getBlockState(pos);
        ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);
        boolean ignoreNbt = Configs.Generic.PICK_BLOCK_IGNORE_NBT.getBooleanValue();

        if (ItemWrap.notEmpty(stack))
        {
            EnumHand hand = EntityUtils.getUsedHandForItem(GameUtils.getClientPlayer(), stack, ignoreNbt);

            if (hand == null)
            {
                if (GameUtils.isCreativeMode())
                {
                    TileEntity te = world.getTileEntity(pos);

                    // The creative mode pick block with NBT only works correctly
                    // if the server world doesn't have a TileEntity in that position.
                    // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                    if (BaseScreen.isCtrlDown() && te != null && GameUtils.getClientWorld().isAirBlock(pos))
                    {
                        stack = stack.copy();
                        ItemUtils.storeBlockEntityInStack(stack, te);
                    }
                }

                return doPickBlockForStack(stack);
            }

            return hand;
        }

        return null;
    }

    @Nullable
    public static EnumHand doPickBlockForStack(ItemStack stack)
    {
        EntityPlayer player = GameUtils.getClientPlayer();
        boolean ignoreNbt = Configs.Generic.PICK_BLOCK_IGNORE_NBT.getBooleanValue();
        EnumHand hand = EntityUtils.getUsedHandForItem(player, stack, ignoreNbt);

        if (ItemWrap.notEmpty(stack) && hand == null)
        {
            switchItemToHand(stack, ignoreNbt);
            hand = EntityUtils.getUsedHandForItem(player, stack, ignoreNbt);
        }

        if (hand != null)
        {
            malilib.util.inventory.InventoryUtils.preRestockHand(player, hand, 6, true);
        }

        return hand;
    }

    private static int getEmptyPickBlockableHotbarSlot(InventoryPlayer inventory)
    {
        // First check the current slot
        if (PICK_BLOCKABLE_SLOTS.contains(inventory.currentItem) &&
            ItemWrap.isEmpty(inventory.mainInventory.get(inventory.currentItem)))
        {
            return inventory.currentItem;
        }

        // If the current slot was not empty, then try to find
        // an empty slot among the allowed pick-blockable slots.
        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            int slotNum = PICK_BLOCKABLE_SLOTS.get(i);

            if (slotNum >= 0 && slotNum < inventory.mainInventory.size())
            {
                ItemStack stack = inventory.mainInventory.get(slotNum);

                if (ItemWrap.isEmpty(stack))
                {
                    return slotNum;
                }
            }
        }

        return -1;
    }

    private static int getNextPickBlockableHotbarSlot(InventoryPlayer inventory)
    {
        if (PICK_BLOCKABLE_SLOTS.contains(inventory.currentItem))
        {
            ItemStack stack = inventory.mainInventory.get(inventory.currentItem);

            if (ItemWrap.isEmpty(stack) ||
                (stack.getItem() instanceof ItemTool) == false)
            {
                return inventory.currentItem;
            }
        }

        if (nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
        {
            nextPickSlotIndex = 0;
        }

        // Try to find the next pick-blockable slot that doesn't have a tool in it
        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            int slotNum = PICK_BLOCKABLE_SLOTS.get(nextPickSlotIndex);

            if (++nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
            {
                nextPickSlotIndex = 0;
            }

            ItemStack stack = inventory.mainInventory.get(slotNum);

            if (ItemWrap.isEmpty(stack) ||
                (stack.getItem() instanceof ItemTool) == false)
            {
                return slotNum;
            }
        }

        return -1;
    }
}
