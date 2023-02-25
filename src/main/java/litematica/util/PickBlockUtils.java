package litematica.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.IntArrayList;

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
import malilib.overlay.message.MessageDispatcher;
import malilib.registry.Registry;
import malilib.util.game.PlacementUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameUtils;
import malilib.util.game.wrap.ItemWrap;
import malilib.util.inventory.InventoryUtils;
import litematica.config.Configs;
import litematica.materials.MaterialCache;
import litematica.render.RenderUtils;
import litematica.world.SchematicWorldHandler;

public class PickBlockUtils
{
    private static final IntArrayList PICK_BLOCK_USABLE_SLOTS = new IntArrayList();
    private static int nextPickBlockSlotIndex;

    public static boolean shouldPickBlock()
    {
        return Configs.Generic.PICK_BLOCK_ENABLED.getBooleanValue() &&
               (Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue() == false || EntityUtils.hasToolItem() == false) &&
               RenderUtils.areSchematicBlocksCurrentlyRendered();
    }

    public static void setPickBlockUsableSlots(String configStr)
    {
        PICK_BLOCK_USABLE_SLOTS.clear();
        String[] parts = configStr.split(",");
        Pattern patternRange = Pattern.compile("^(?<start>[0-9])-(?<end>[0-9])$");

        for (String str : parts)
        {
            str = str.trim();

            try
            {
                Matcher matcher = patternRange.matcher(str);

                if (matcher.matches())
                {
                    int slotStart = Integer.parseInt(matcher.group("start")) - 1;
                    int slotEnd = Integer.parseInt(matcher.group("end")) - 1;

                    if (slotStart <= slotEnd &&
                        InventoryUtils.isHotbarSlot(slotStart) &&
                        InventoryUtils.isHotbarSlot(slotEnd))
                    {
                        for (int slotNum = slotStart; slotNum <= slotEnd; ++slotNum)
                        {
                            if (PICK_BLOCK_USABLE_SLOTS.contains(slotNum) == false)
                            {
                                PICK_BLOCK_USABLE_SLOTS.add(slotNum);
                            }
                        }
                    }
                }
                else
                {
                    int slotNum = Integer.parseInt(str) - 1;

                    if (InventoryUtils.isHotbarSlot(slotNum) && PICK_BLOCK_USABLE_SLOTS.contains(slotNum) == false)
                    {
                        PICK_BLOCK_USABLE_SLOTS.add(slotNum);
                    }
                }
            }
            catch (NumberFormatException ignore) {}
        }
    }

    /**
     * @return true if the ray trace hit a schematic block first and thus a vanilla pick block should not happen,
     * regardless of whether the pick block was successful or not (i.e. whether the required item was found in
     * the player's inventory)
     */
    public static boolean pickBlockFirst()
    {
        double reach = GameUtils.getPlayerReachDistance();
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
    public static EnumHand pickBlockLast()
    {
        World world = GameUtils.getClientWorld();
        BlockPos pos = Registry.BLOCK_PLACEMENT_POSITION_HANDLER.getCurrentPlacementPosition();

        // No overrides by other mods
        if (pos == null)
        {
            double reach = GameUtils.getPlayerReachDistance();
            Entity entity = GameUtils.getCameraEntity();
            pos = RayTraceUtils.getPickBlockLastTrace(world, entity, reach, true);
        }

        if (pos != null && PlacementUtils.isReplaceable(world, pos, true))
        {
            return doPickBlockForPosition(pos);
        }

        return null;
    }

    @Nullable
    public static EnumHand doPickBlockForStack(ItemStack stack)
    {
        EntityPlayer player = GameUtils.getClientPlayer();
        boolean ignoreNbt = Configs.Generic.PICK_BLOCK_IGNORE_NBT.getBooleanValue();
        EnumHand hand = EntityWrap.getUsedHandForItem(player, stack, ignoreNbt);

        if (ItemWrap.notEmpty(stack) && hand == null)
        {
            switchItemToHand(stack, ignoreNbt);
            hand = EntityWrap.getUsedHandForItem(player, stack, ignoreNbt);
        }

        if (hand != null)
        {
            InventoryUtils.preRestockHand(player, hand, 6, true);
        }

        return hand;
    }

    @Nullable
    private static EnumHand doPickBlockForPosition(BlockPos pos)
    {
        World world = SchematicWorldHandler.getSchematicWorld();
        IBlockState state = world.getBlockState(pos);
        ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);
        boolean ignoreNbt = Configs.Generic.PICK_BLOCK_IGNORE_NBT.getBooleanValue();

        if (ItemWrap.notEmpty(stack))
        {
            EnumHand hand = EntityWrap.getUsedHandForItem(GameUtils.getClientPlayer(), stack, ignoreNbt);

            if (hand == null)
            {
                if (GameUtils.isCreativeMode() && BaseScreen.isCtrlDown())
                {
                    TileEntity te = world.getTileEntity(pos);

                    // The creative mode pick block with NBT only works correctly
                    // if the server world doesn't have a TileEntity in that position.
                    // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                    if (te != null && GameUtils.getClientWorld().isAirBlock(pos))
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

    private static boolean canPickToSlot(InventoryPlayer inventory, int slotNum)
    {
        if (PICK_BLOCK_USABLE_SLOTS.contains(slotNum) == false)
        {
            return false;
        }

        ItemStack stack = inventory.getStackInSlot(slotNum);

        if (ItemWrap.isEmpty(stack))
        {
            return true;
        }

        return (Configs.Generic.PICK_BLOCK_AVOID_DAMAGEABLE.getBooleanValue() == false ||
                    stack.getItem().isDamageable() == false) &&
                (Configs.Generic.PICK_BLOCK_AVOID_TOOLS.getBooleanValue() == false ||
                    (stack.getItem() instanceof ItemTool) == false);
    }

    private static int getEmptyUsableHotbarSlot(InventoryPlayer inventory)
    {
        int currentHotbarSlot = InventoryUtils.getSelectedHotbarSlot();

        // First check the current slot
        if (PICK_BLOCK_USABLE_SLOTS.contains(currentHotbarSlot) &&
            ItemWrap.isEmpty(inventory.mainInventory.get(currentHotbarSlot)))
        {
            return currentHotbarSlot;
        }

        // If the current slot was not empty, then try to find
        // an empty slot among the allowed pick-block usable slots.
        for (int slotNum : PICK_BLOCK_USABLE_SLOTS)
        {
            if (InventoryUtils.isHotbarSlot(slotNum))
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

    private static int getNextUsableHotbarSlot(InventoryPlayer inventory)
    {
        if (PICK_BLOCK_USABLE_SLOTS.isEmpty())
        {
            return -1;
        }

        int hotbarSlot = getEmptyUsableHotbarSlot(inventory);

        if (hotbarSlot != -1)
        {
            return hotbarSlot;
        }

        for (int i = 0; i < PICK_BLOCK_USABLE_SLOTS.size(); ++i)
        {
            if (nextPickBlockSlotIndex >= PICK_BLOCK_USABLE_SLOTS.size())
            {
                nextPickBlockSlotIndex = 0;
            }

            hotbarSlot = PICK_BLOCK_USABLE_SLOTS.getInt(nextPickBlockSlotIndex++);

            if (canPickToSlot(inventory, hotbarSlot))
            {
                return hotbarSlot;
            }
        }

        return -1;
    }

    private static boolean switchItemToHand(ItemStack stack, boolean ignoreNbt)
    {
        if (PICK_BLOCK_USABLE_SLOTS.size() == 0)
        {
            MessageDispatcher.warning().translate("litematica.message.warn.pick_block.no_valid_slots_configured");
            return false;
        }

        EntityPlayer player = GameUtils.getClientPlayer();
        InventoryPlayer inventory = GameUtils.getPlayerInventory();
        Container container = GameUtils.getCurrentInventoryContainer();
        boolean isCreativeMode = GameUtils.isCreativeMode();
        int slotWithItem = InventoryUtils.findSlotWithItemToPickBlock(container, stack, ignoreNbt);

        // Item not found in the player's inventory, and not in creative mode so no way to just add it
        if (slotWithItem == -1 && isCreativeMode == false)
        {
            return false;
        }

        // The item is already in the hotbar, just switch to that slot
        if (InventoryUtils.isHotbarSlotIndex(slotWithItem))
        {
            InventoryUtils.setSelectedHotbarSlot(slotWithItem - 36);
            return true;
        }

        int hotbarSlot = getNextUsableHotbarSlot(inventory);

        if (hotbarSlot == -1)
        {
            MessageDispatcher.warning(8000).translate("litematica.message.warn.pick_block.no_suitable_slot_found");
            return false;
        }

        if (slotWithItem != -1)
        {
            InventoryUtils.swapSlots(container, slotWithItem, hotbarSlot);
            InventoryUtils.setSelectedHotbarSlot(hotbarSlot);
            return true;
        }
        else if (isCreativeMode && InventoryUtils.isHotbarSlot(hotbarSlot))
        {
            int slotNum = hotbarSlot + 36;

            // First try to put the current hotbar item into an empty slot in the player's inventory
            if (ItemWrap.notEmpty(inventory.getStackInSlot(hotbarSlot)))
            {
                // Shift click the stack
                InventoryUtils.clickSlot(container, slotNum, 0, ClickType.QUICK_MOVE);

                // Wasn't able to move the items out
                if (ItemWrap.notEmpty(inventory.getStackInSlot(hotbarSlot)))
                {
                    // TODO try to combine partial stacks

                    // The off-hand slot is empty, move the current stack to it
                    if (ItemWrap.isEmpty(player.getHeldItemOffhand()))
                    {
                        InventoryUtils.swapSlots(container, slotNum, 0);
                        InventoryUtils.swapSlots(container, 45, 0);
                        InventoryUtils.swapSlots(container, slotNum, 0);
                    }
                }
            }

            InventoryUtils.setSelectedHotbarSlot(hotbarSlot);
            inventory.mainInventory.set(hotbarSlot, stack.copy());
            GameUtils.getInteractionManager().sendSlotPacket(stack.copy(), slotNum);

            return true;
        }

        return false;
    }
}
