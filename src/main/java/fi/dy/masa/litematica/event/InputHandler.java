package fi.dy.masa.litematica.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.input.MouseInputHandler;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.EasyPlaceUtils;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.ToolUtils;

public class InputHandler implements MouseInputHandler
{
    private static final InputHandler INSTANCE = new InputHandler();

    private InputHandler()
    {
    }

    public static InputHandler getInstance()
    {
        return INSTANCE;
    }

    @Override
    public boolean onMouseInput(int eventButton, int wheelDelta, boolean eventButtonState)
    {
        Minecraft mc = Minecraft.getMinecraft();

        // Not in a GUI
        if (GuiUtils.getCurrentScreen() == null && mc.world != null && mc.player != null && wheelDelta != 0)
        {
            return this.handleMouseScroll(wheelDelta, mc);
        }

        return false;
    }

    private boolean handleMouseScroll(double dWheel, Minecraft mc)
    {
        EntityPlayer player = mc.player;
        boolean toolEnabled = Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();

        if (toolEnabled == false || EntityUtils.hasToolItem(player) == false)
        {
            return false;
        }

        final int amount = dWheel > 0 ? 1 : -1;
        ToolMode mode = DataManager.getToolMode();
        EnumFacing direction = fi.dy.masa.malilib.util.PositionUtils.getClosestLookingDirection(player);

        if (Hotkeys.SELECTION_EXPAND_MODIFIER.getKeyBind().isKeyBindHeld() && mode.getUsesAreaSelection())
        {
            return this.modifySelectionBox(amount, mode, direction, PositionUtils::expandOrShrinkBox);
        }

        if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeyBind().isKeyBindHeld() && mode.getUsesAreaSelection())
        {
            SelectionManager sm = DataManager.getSelectionManager();

            if (sm.hasGrabbedElement())
            {
                sm.changeGrabDistance(player, amount);
                return true;
            }
            else if (sm.hasSelectedOrigin())
            {
                AreaSelection area = sm.getCurrentSelection();
                BlockPos old = area.getEffectiveOrigin();
                area.moveEntireSelectionTo(old.offset(EntityUtils.getClosestLookingDirection(player), amount), false);
                return true;
            }
            else if (mode == ToolMode.MOVE)
            {
                ToolUtils.moveCurrentlySelectedWorldRegionToLookingDirection(amount, player, mc);
                return true;
            }
        }

        if (Hotkeys.SELECTION_GROW_MODIFIER.getKeyBind().isKeyBindHeld())
        {
            return this.modifySelectionBox(amount, mode, direction, (boxIn, amountIn, side) -> PositionUtils.growOrShrinkBox(boxIn, amountIn));
        }

        if (Hotkeys.SELECTION_NUDGE_MODIFIER.getKeyBind().isKeyBindHeld())
        {
            return nudgeSelection(amount, mode, player);
        }

        if (Hotkeys.OPERATION_MODE_CHANGE_MODIFIER.getKeyBind().isKeyBindHeld())
        {
            DataManager.setToolMode(DataManager.getToolMode().cycle(amount < 0));
            return true;
        }

        if (Hotkeys.SCHEMATIC_VERSION_CYCLE_MODIFIER.getKeyBind().isKeyBindHeld())
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                DataManager.getSchematicProjectsManager().cycleVersion(amount * -1);
            }
            return true;
        }

        return false;
    }

    public static boolean nudgeSelection(int amount, ToolMode mode, EntityPlayer player)
    {
        if (mode.getUsesAreaSelection())
        {
            DataManager.getSelectionManager().moveSelectedElement(EntityUtils.getClosestLookingDirection(player), amount);
            return true;
        }
        else if (mode.getUsesSchematic())
        {
            EnumFacing direction = EntityUtils.getClosestLookingDirection(player);
            DataManager.getSchematicPlacementManager().nudgePositionOfCurrentSelection(direction, amount);
            return true;
        }

        return false;
    }

    private boolean modifySelectionBox(int amount, ToolMode mode, EnumFacing direction, IBoxEditor editor)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null)
        {
            Box box = area.getSelectedSubRegionBox();

            if (box != null)
            {
                Box newBox = editor.editBox(box, amount, direction);
                area.setSelectedSubRegionCornerPos(newBox.getPos1(), Corner.CORNER_1);
                area.setSelectedSubRegionCornerPos(newBox.getPos2(), Corner.CORNER_2);
            }
            else
            {
                MessageDispatcher.error().translate("litematica.error.area_selection.no_sub_region_selected");
            }
        }
        else
        {
            MessageDispatcher.error().translate("litematica.message.error.no_area_selected");
        }

        return true;
    }

    public static void onTick(Minecraft mc)
    {
        SelectionManager sm = DataManager.getSelectionManager();

        if (sm.hasGrabbedElement())
        {
            sm.moveGrabbedElement(mc.player);
        }
        else if (GuiUtils.getCurrentScreen() == null)
        {
            EasyPlaceUtils.easyPlaceOnUseTick(mc);
        }
    }

    private interface IBoxEditor
    {
        Box editBox(Box boxIn, int amount, EnumFacing side);
    }
}
