package fi.dy.masa.litematica.event;

import java.util.List;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicManager;
import fi.dy.masa.litematica.schematic.util.SchematicEditUtils;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.ToolUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import fi.dy.masa.malilib.hotkeys.KeybindCategory;
import fi.dy.masa.malilib.util.InfoUtils;

public class InputHandler implements IKeybindProvider, IKeyboardInputHandler, IMouseInputHandler
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
    public List<? extends IHotkey> getAllHotkeys()
    {
        return Hotkeys.HOTKEY_LIST;
    }

    @Override
    public List<KeybindCategory> getHotkeyCategoriesForCombinedView()
    {
        return ImmutableList.of(new KeybindCategory(Reference.MOD_NAME, "litematica.hotkeys.category.generic_hotkeys", Hotkeys.HOTKEY_LIST));
    }

    @Override
    public boolean onKeyInput(int eventKey, boolean eventKeyState)
    {
        if (eventKeyState)
        {
            Minecraft mc = Minecraft.getMinecraft();

            if (eventKey == mc.gameSettings.keyBindUseItem.getKeyCode())
            {
                return this.handleUseKey(mc);
            }
            else if (eventKey == mc.gameSettings.keyBindAttack.getKeyCode())
            {
                return this.handleAttackKey(mc);
            }
            else if (eventKey == mc.gameSettings.keyBindScreenshot.getKeyCode() && GuiSchematicManager.hasPendingPreviewTask())
            {
                return GuiSchematicManager.setPreviewImage();
            }
        }

        return false;
    }

    @Override
    public boolean onMouseInput(int eventButton, int dWheel, boolean eventButtonState)
    {
        Minecraft mc = Minecraft.getMinecraft();

        // Not in a GUI
        if (GuiUtils.getCurrentScreen() == null && mc.world != null && mc.player != null)
        {
            if (eventButtonState && eventButton == mc.gameSettings.keyBindUseItem.getKeyCode() + 100)
            {
                return this.handleUseKey(mc);
            }
            else if (eventButtonState && eventButton == mc.gameSettings.keyBindAttack.getKeyCode() + 100)
            {
                return this.handleAttackKey(mc);
            }

            if (dWheel != 0)
            {
                return this.handleMouseScroll(dWheel, mc);
            }
        }

        return false;
    }

    private boolean handleMouseScroll(double dWheel, Minecraft mc)
    {
        EntityPlayer player = mc.player;
        boolean toolEnabled = Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();

        if (toolEnabled == false || EntityUtils.hasToolItem(player) == false)
        {
            return false;
        }

        final int amount = dWheel > 0 ? 1 : -1;
        ToolMode mode = DataManager.getToolMode();
        EnumFacing direction = fi.dy.masa.malilib.util.PositionUtils.getClosestLookingDirection(player);

        if (Hotkeys.SELECTION_EXPAND_MODIFIER.getKeybind().isKeybindHeld() && mode.getUsesAreaSelection())
        {
            return this.modifySelectionBox(amount, mode, direction, (boxIn, amountIn, side) -> PositionUtils.expandOrShrinkBox(boxIn, amountIn, side));
        }

        if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld() && mode.getUsesAreaSelection())
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

        if (Hotkeys.SELECTION_GROW_MODIFIER.getKeybind().isKeybindHeld())
        {
            return this.modifySelectionBox(amount, mode, direction, (boxIn, amountIn, side) -> PositionUtils.growOrShrinkBox(boxIn, amountIn));
        }

        if (Hotkeys.SELECTION_NUDGE_MODIFIER.getKeybind().isKeybindHeld())
        {
            return nudgeSelection(amount, mode, player);
        }

        if (Hotkeys.OPERATION_MODE_CHANGE_MODIFIER.getKeybind().isKeybindHeld())
        {
            DataManager.setToolMode(DataManager.getToolMode().cycle(player, amount < 0));
            return true;
        }

        if (Hotkeys.SCHEMATIC_VERSION_CYCLE_MODIFIER.getKeybind().isKeybindHeld())
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
            SelectionManager sm = DataManager.getSelectionManager();

            if (sm.hasSelectedElement())
            {
                sm.moveSelectedElement(EntityUtils.getClosestLookingDirection(player), amount);
                return true;
            }
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
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.area_selection.no_sub_region_selected");
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
        }

        return true;
    }

    private boolean handleAttackKey(Minecraft mc)
    {
        if (mc.player != null && DataManager.getToolMode() == ToolMode.REBUILD)
        {
            if (Hotkeys.SCHEMATIC_REBUILD_BREAK_DIRECTION.getKeybind().isKeybindHeld())
            {
                return SchematicEditUtils.breakSchematicBlocks(mc);
            }
            else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_ALL.getKeybind().isKeybindHeld())
            {
                return SchematicEditUtils.breakAllIdenticalSchematicBlocks(mc);
            }
            else
            {
                return SchematicEditUtils.breakSchematicBlock(mc);
            }
        }

        return false;
    }

    private boolean handleUseKey(Minecraft mc)
    {
        if (mc.player != null)
        {
            if (DataManager.getToolMode() == ToolMode.REBUILD)
            {
                if (Hotkeys.SCHEMATIC_REBUILD_REPLACE_DIRECTION.getKeybind().isKeybindHeld())
                {
                    return SchematicEditUtils.replaceSchematicBlocksInDirection(mc);
                }
                else if (Hotkeys.SCHEMATIC_REBUILD_REPLACE_ALL.getKeybind().isKeybindHeld())
                {
                    return SchematicEditUtils.replaceAllIdenticalSchematicBlocks(mc);
                }
                else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_DIRECTION.getKeybind().isKeybindHeld())
                {
                    return SchematicEditUtils.placeSchematicBlocksInDirection(mc);
                }
                else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_ALL.getKeybind().isKeybindHeld())
                {
                    return SchematicEditUtils.fillAirWithBlocks(mc);
                }
                else
                {
                    return SchematicEditUtils.placeSchematicBlock(mc);
                }
            }
            else if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
                     Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld())
            {
                return WorldUtils.handleEasyPlace(mc);
            }

            if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
            {
                return WorldUtils.handlePlacementRestriction(mc);
            }
        }

        return false;
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
            WorldUtils.easyPlaceOnUseTick(mc);
        }
    }

    private interface IBoxEditor
    {
        Box editBox(Box boxIn, int amount, EnumFacing side);
    }
}
