package litematica.config;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import malilib.input.ActionResult;
import malilib.input.KeyAction;
import malilib.input.KeyBind;
import malilib.input.callback.HotkeyCallback;
import malilib.util.game.wrap.GameUtils;
import litematica.data.DataManager;
import litematica.selection.AreaSelectionManager;
import litematica.selection.BoxCorner;
import litematica.selection.ToolSelectionMode;
import litematica.tool.ToolMode;
import litematica.util.EntityUtils;
import litematica.util.RayTraceUtils;
import litematica.util.ToolUtils;

public class HotkeyCallbackToolActions implements HotkeyCallback
{
    @Override
    public ActionResult onKeyAction(KeyAction action, KeyBind key)
    {
        World world = GameUtils.getClientWorld();

        if (GameUtils.getClientPlayer() == null || world == null)
        {
            return ActionResult.FAIL;
        }

        ToolMode mode = DataManager.getToolMode();
        boolean toolEnabled = Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();
        boolean hasTool = EntityUtils.hasToolItem();
        boolean isToolPrimary = key == Hotkeys.TOOL_PLACE_CORNER_1.getKeyBind();
        boolean isToolSecondary = key == Hotkeys.TOOL_PLACE_CORNER_2.getKeyBind();
        boolean isToolSelect = key == Hotkeys.TOOL_SELECT_ELEMENTS.getKeyBind();

        if (toolEnabled && isToolSelect)
        {
            if (mode.getUsesBlockPrimary() && Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_1.getKeyBind().isKeyBindHeld())
            {
                ToolUtils.setToolModeBlockState(mode, true);
                return ActionResult.SUCCESS;
            }
            else if (mode.getUsesBlockSecondary() && Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_2.getKeyBind().isKeyBindHeld())
            {
                ToolUtils.setToolModeBlockState(mode, false);
                return ActionResult.SUCCESS;
            }
        }

        if (toolEnabled && hasTool)
        {
            int maxDistance = 200;
            boolean projectMode =  DataManager.getSchematicProjectsManager().hasProjectOpen();

            if (isToolPrimary || isToolSecondary)
            {
                if (mode.getUsesAreaSelection() || projectMode)
                {
                    AreaSelectionManager sm = DataManager.getAreaSelectionManager();
                    boolean grabModifier = Hotkeys.SELECTION_GRAB_MODIFIER.getKeyBind().isKeyBindHeld();
                    boolean moveEverything = grabModifier;

                    if (grabModifier && mode == ToolMode.MOVE)
                    {
                        Entity entity = GameUtils.getCameraEntity();
                        BlockPos pos = RayTraceUtils.getTargetedPosition(world, entity, maxDistance, false);

                        if (pos != null)
                        {
                            ToolUtils.moveCurrentlySelectedWorldRegionTo(pos);
                        }
                    }
                    else if (Configs.Generic.TOOL_SELECTION_MODE.getValue() == ToolSelectionMode.CORNERS)
                    {
                        BoxCorner corner = isToolPrimary ? BoxCorner.CORNER_1 : BoxCorner.CORNER_2;
                        sm.setPositionOfCurrentSelectionToRayTrace(corner, moveEverything, maxDistance);
                    }
                    else if (Configs.Generic.TOOL_SELECTION_MODE.getValue() == ToolSelectionMode.EXPAND)
                    {
                        sm.handleCuboidModeMouseClick(maxDistance, isToolSecondary, moveEverything);
                    }
                }
                else if (mode.getUsesSchematic())
                {
                    DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionToRayTrace(maxDistance);
                }

                return ActionResult.SUCCESS;
            }
            else if (isToolSelect)
            {
                Entity entity = GameUtils.getCameraEntity();

                if (mode.getUsesAreaSelection() || projectMode)
                {
                    DataManager.getAreaSelectionManager().changeSelection(world, entity, maxDistance);
                }
                else if (mode.getUsesSchematic())
                {
                    DataManager.getSchematicPlacementManager().changeSelection(world, entity, maxDistance);
                }

                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.FAIL;
    }
}
