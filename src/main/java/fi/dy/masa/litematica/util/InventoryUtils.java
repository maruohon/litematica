package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.systems.BlockPlacementPositionHandler;

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

    public static boolean switchItemToHand(ItemStack stack, boolean ignoreNbt, Minecraft mc)
    {
        if (PICK_BLOCKABLE_SLOTS.size() == 0)
        {
            return false;
        }

        EntityPlayer player = mc.player;
        InventoryPlayer inventory = player.inventory;
        boolean isCreativeMode = player.capabilities.isCreativeMode;
        int slotWithItem = fi.dy.masa.malilib.util.InventoryUtils.findSlotWithItemToPickBlock(player.openContainer, stack, ignoreNbt);

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
            fi.dy.masa.malilib.util.InventoryUtils.swapSlots(player.openContainer, slotWithItem, hotbarSlot);
            inventory.currentItem = hotbarSlot;
            return true;
        }
        else if (isCreativeMode && InventoryPlayer.isHotbar(hotbarSlot))
        {
            int slotNum = hotbarSlot + 36;

            // First try to put the current hotbar item into an empty slot in the player's inventory
            if (inventory.getStackInSlot(hotbarSlot).isEmpty() == false)
            {
                // Shift click the stack
                mc.playerController.windowClick(player.openContainer.windowId, slotNum, 0, ClickType.QUICK_MOVE, player);

                // Wasn't able to move the items out
                if (inventory.getStackInSlot(hotbarSlot).isEmpty() == false)
                {
                    // TODO try to combine partial stacks

                    // The off-hand slot is empty, move the current stack to it
                    if (player.getHeldItemOffhand().isEmpty())
                    {
                        fi.dy.masa.malilib.util.InventoryUtils.swapSlots(player.openContainer, slotNum, 0);
                        fi.dy.masa.malilib.util.InventoryUtils.swapSlots(player.openContainer, 45, 0);
                        fi.dy.masa.malilib.util.InventoryUtils.swapSlots(player.openContainer, slotNum, 0);
                    }
                }
            }

            inventory.currentItem = hotbarSlot;

            inventory.mainInventory.set(hotbarSlot, stack.copy());
            mc.playerController.sendSlotPacket(stack.copy(), slotNum);
            return true;
        }

        return false;
    }

    /**
     * Does a ray trace to the schematic world, and returns either the closest or the furthest hit block.
     * @param adjacentOnly whether to only accept traced schematic world position that are adjacent to a client world block, ie. normally placeable
     * @param mc
     * @return true if the correct item was or is in the player's hand after the pick block
     */
    public static boolean pickBlockFirst(Minecraft mc)
    {
        double reach = mc.playerController.getBlockReachDistance();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        BlockPos pos = RayTraceUtils.getSchematicWorldTraceIfClosest(mc.world, entity, reach);

        if (pos != null)
        {
            doPickBlockForPosition(pos, mc);
            return true;
        }

        return false;
    }

    public static boolean pickBlockLast(boolean adjacentOnly, Minecraft mc)
    {
        BlockPos pos = BlockPlacementPositionHandler.INSTANCE.getCurrentPlacementPosition();

        // No overrides by other mods
        if (pos == null)
        {
            double reach = mc.playerController.getBlockReachDistance();
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            pos = RayTraceUtils.getPickBlockLastTrace(mc.world, entity, reach, adjacentOnly);
        }

        if (pos != null)
        {
            IBlockState state = mc.world.getBlockState(pos);

            if (state.getBlock().isReplaceable(mc.world, pos) || state.getMaterial().isReplaceable())
            {
                return doPickBlockForPosition(pos, mc);
            }
        }

        return false;
    }

    private static boolean doPickBlockForPosition(BlockPos pos, Minecraft mc)
    {
        World world = SchematicWorldHandler.getSchematicWorld();
        EntityPlayer player = mc.player;
        IBlockState state = world.getBlockState(pos);
        ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);
        boolean ignoreNbt = Configs.Generic.PICK_BLOCK_IGNORE_NBT.getBooleanValue();
        boolean picked = false;

        if (stack.isEmpty() == false && EntityUtils.getUsedHandForItem(player, stack, ignoreNbt) == null)
        {
            if (mc.player.capabilities.isCreativeMode)
            {
                TileEntity te = world.getTileEntity(pos);

                // The creative mode pick block with NBT only works correctly
                // if the server world doesn't have a TileEntity in that position.
                // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                if (GuiBase.isCtrlDown() && te != null && mc.world.isAirBlock(pos))
                {
                    stack = stack.copy();
                    ItemUtils.storeTEInStack(stack, te);
                }
            }

            picked = switchItemToHand(stack, ignoreNbt, mc);
        }

        EnumHand hand = EntityUtils.getUsedHandForItem(player, stack, ignoreNbt);

        if (hand != null)
        {
            fi.dy.masa.malilib.util.InventoryUtils.preRestockHand(player, hand, 6, true);
        }

        return picked;
    }

    private static int getEmptyPickBlockableHotbarSlot(InventoryPlayer inventory)
    {
        // First check the current slot
        if (PICK_BLOCKABLE_SLOTS.contains(inventory.currentItem) &&
            inventory.mainInventory.get(inventory.currentItem).isEmpty())
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
        if (PICK_BLOCKABLE_SLOTS.contains(inventory.currentItem))
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
            slotNum = PICK_BLOCKABLE_SLOTS.get(nextPickSlotIndex);

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
