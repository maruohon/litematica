package fi.dy.masa.litematica.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.SchematicUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.util.GuiUtils;
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
    public void addKeysToMap(IKeybindManager manager)
    {
        for (IHotkey hotkey : Hotkeys.HOTKEY_LIST)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager)
    {
        manager.addHotkeysForCategory(Reference.MOD_NAME, "litematica.hotkeys.category.generic_hotkeys", Hotkeys.HOTKEY_LIST);
    }

    @Override
    public boolean onKeyInput(int keyCode, int scanCode, int modifiers, boolean eventKeyState)
    {
        if (eventKeyState)
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc.options.keyUse.matchesKey(keyCode, scanCode))
            {
                return this.handleUseKey(mc);
            }
            else if (mc.options.keyAttack.matchesKey(keyCode, scanCode))
            {
                return this.handleAttackKey(mc);
            }
            else if (mc.options.keyScreenshot.matchesKey(keyCode, scanCode) && GuiSchematicManager.hasPendingPreviewTask())
            {
                return GuiSchematicManager.setPreviewImage();
            }
        }

        return false;
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int eventButton, boolean eventButtonState)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Tool enabled, and not in a GUI
        if (GuiUtils.getCurrentScreen() == null && mc.world != null && mc.player != null && eventButtonState)
        {
            if (eventButtonState && mc.options.keyUse.matchesMouse(eventButton))
            {
                return this.handleUseKey(mc);
            }
            else if (eventButtonState && mc.options.keyAttack.matchesMouse(eventButton))
            {
                return this.handleAttackKey(mc);
            }
        }

        return false;
    }

    @Override
    public boolean onMouseScroll(int mouseX, int mouseY, double dWheel)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Not in a GUI
        if (GuiUtils.getCurrentScreen() == null && mc.world != null && mc.player != null)
        {
            return this.handleMouseScroll(dWheel, mc);
        }

        return false;
    }

    private boolean handleMouseScroll(double dWheel, MinecraftClient mc)
    {
        PlayerEntity player = mc.player;
        boolean toolEnabled = Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();

        if (toolEnabled == false || EntityUtils.hasToolItem(player) == false)
        {
            return false;
        }

        final int amount = dWheel > 0 ? 1 : -1;
        ToolMode mode = DataManager.getToolMode();

        if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld())
        {
            if (mode.getUsesAreaSelection())
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
                    SchematicUtils.moveCurrentlySelectedWorldRegionToLookingDirection(amount, player, mc);
                    return true;
                }
            }
        }

        if (Hotkeys.SELECTION_GROW_MODIFIER.getKeybind().isKeybindHeld())
        {
            return this.growOrShrinkSelection(amount, mode);
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

    public static boolean nudgeSelection(int amount, ToolMode mode, PlayerEntity player)
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
            Direction direction = EntityUtils.getClosestLookingDirection(player);
            DataManager.getSchematicPlacementManager().nudgePositionOfCurrentSelection(direction, amount);
            return true;
        }

        return false;
    }

    private boolean growOrShrinkSelection(int amount, ToolMode mode)
    {
        if (mode.getUsesAreaSelection())
        {
            SelectionManager sm = DataManager.getSelectionManager();
            AreaSelection area = sm.getCurrentSelection();

            if (area != null)
            {
                Box box = area.getSelectedSubRegionBox();

                if (box != null)
                {
                    Box newBox = PositionUtils.growOrShrinkBox(box, amount);
                    area.setSelectedSubRegionCornerPos(newBox.getPos1(), Corner.CORNER_1);
                    area.setSelectedSubRegionCornerPos(newBox.getPos2(), Corner.CORNER_2);
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.area_selection.grow.no_sub_region_selected");
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
            }
        }

        return true;
    }

    private boolean handleAttackKey(MinecraftClient mc)
    {
        if (mc.player != null && DataManager.getToolMode() == ToolMode.REBUILD && KeybindMulti.getTriggeredCount() == 0)
        {
            if (Hotkeys.SCHEMATIC_REBUILD_BREAK_DIRECTION.getKeybind().isKeybindHeld())
            {
                return SchematicUtils.breakSchematicBlocks(mc);
            }
            else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_ALL_EXCEPT.getKeybind().isKeybindHeld())
            {
                return SchematicUtils.breakAllSchematicBlocksExceptTargeted(mc);
            }
            else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_ALL.getKeybind().isKeybindHeld())
            {
                return SchematicUtils.breakAllIdenticalSchematicBlocks(mc);
            }
            else
            {
                return SchematicUtils.breakSchematicBlock(mc);
            }
        }

        return false;
    }

    private boolean handleUseKey(MinecraftClient mc)
    {
        if (mc.player != null)
        {
            if (DataManager.getToolMode() == ToolMode.REBUILD && KeybindMulti.getTriggeredCount() == 0)
            {
                if (Hotkeys.SCHEMATIC_REBUILD_REPLACE_DIRECTION.getKeybind().isKeybindHeld())
                {
                    return SchematicUtils.replaceSchematicBlocksInDirection(mc);
                }
                else if (Hotkeys.SCHEMATIC_REBUILD_REPLACE_ALL.getKeybind().isKeybindHeld())
                {
                    return SchematicUtils.replaceAllIdenticalSchematicBlocks(mc);
                }
                else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_DIRECTION.getKeybind().isKeybindHeld())
                {
                    return SchematicUtils.placeSchematicBlocksInDirection(mc);
                }
                else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_ALL.getKeybind().isKeybindHeld())
                {
                    return SchematicUtils.fillAirWithBlocks(mc);
                }
                else
                {
                    return SchematicUtils.placeSchematicBlock(mc);
                }
            }
            else if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
                     Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld())
            {
                return WorldUtils.handleEasyPlace(mc);
            }
            else if (Configs.Generic.PICK_BLOCK_ENABLED.getBooleanValue())
            {
                if (KeybindMulti.hotkeyMatchesKeybind(Hotkeys.PICK_BLOCK_LAST, mc.options.keyUse))
                {
                    WorldUtils.doSchematicWorldPickBlock(false, mc);
                }
            }

            if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
            {
                return WorldUtils.handlePlacementRestriction(mc);
            }
        }

        return false;
    }

    public static void onTick(MinecraftClient mc)
    {
        SelectionManager sm = DataManager.getSelectionManager();

        if (sm.hasGrabbedElement())
        {
            sm.moveGrabbedElement(mc.player);
        }
        else
        {
            WorldUtils.easyPlaceOnUseTick(mc);
        }
    }
}
