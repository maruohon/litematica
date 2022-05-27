package fi.dy.masa.litematica.config;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.malilib.input.ActionResult;
import fi.dy.masa.malilib.input.KeyAction;
import fi.dy.masa.malilib.input.KeyBind;
import fi.dy.masa.malilib.input.callback.HotkeyCallback;
import fi.dy.masa.malilib.util.game.wrap.GameUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.CornerSelectionMode;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.ToolUtils;

public class HotkeyCallbackToolActions implements HotkeyCallback
{
    private final Minecraft mc;

    public HotkeyCallbackToolActions(Minecraft mc)
    {
        this.mc = mc;
    }

    @Override
    public ActionResult onKeyAction(KeyAction action, KeyBind key)
    {
        if (this.mc.player == null || this.mc.world == null)
        {
            return ActionResult.FAIL;
        }

        ToolMode mode = DataManager.getToolMode();
        boolean toolEnabled = Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();
        boolean hasTool = EntityUtils.hasToolItem(this.mc.player);
        boolean isToolPrimary = key == Hotkeys.TOOL_PLACE_CORNER_1.getKeyBind();
        boolean isToolSecondary = key == Hotkeys.TOOL_PLACE_CORNER_2.getKeyBind();
        boolean isToolSelect = key == Hotkeys.TOOL_SELECT_ELEMENTS.getKeyBind();

        if (toolEnabled && isToolSelect)
        {
            if (mode.getUsesBlockPrimary() && Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_1.getKeyBind().isKeyBindHeld())
            {
                ToolUtils.setToolModeBlockState(mode, true, this.mc);
                return ActionResult.SUCCESS;
            }
            else if (mode.getUsesBlockSecondary() && Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_2.getKeyBind().isKeyBindHeld())
            {
                ToolUtils.setToolModeBlockState(mode, false, this.mc);
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
                    SelectionManager sm = DataManager.getSelectionManager();
                    boolean grabModifier = Hotkeys.SELECTION_GRAB_MODIFIER.getKeyBind().isKeyBindHeld();
                    boolean moveEverything = grabModifier;

                    if (grabModifier && mode == ToolMode.MOVE)
                    {
                        Entity entity = GameUtils.getCameraEntity();
                        BlockPos pos = RayTraceUtils.getTargetedPosition(this.mc.world, entity, maxDistance, false);

                        if (pos != null)
                        {
                            ToolUtils.moveCurrentlySelectedWorldRegionTo(pos, mc);
                        }
                    }
                    else if (Configs.Generic.SELECTION_CORNERS_MODE.getValue() == CornerSelectionMode.CORNERS)
                    {
                        Corner corner = isToolPrimary ? Corner.CORNER_1 : Corner.CORNER_2;
                        sm.setPositionOfCurrentSelectionToRayTrace(this.mc, corner, moveEverything, maxDistance);
                    }
                    else if (Configs.Generic.SELECTION_CORNERS_MODE.getValue() == CornerSelectionMode.EXPAND)
                    {
                        sm.handleCuboidModeMouseClick(this.mc, maxDistance, isToolSecondary, moveEverything);
                    }
                }
                else if (mode.getUsesSchematic())
                {
                    DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionToRayTrace(this.mc, maxDistance);
                }

                return ActionResult.SUCCESS;
            }
            else if (isToolSelect)
            {
                Entity entity = GameUtils.getCameraEntity();

                if (mode.getUsesAreaSelection() || projectMode)
                {
                    SelectionManager sm = DataManager.getSelectionManager();

                    if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeyBind().isKeyBindHeld())
                    {
                        if (sm.hasGrabbedElement())
                        {
                            sm.releaseGrabbedElement();
                        }
                        else
                        {
                            sm.grabElement(this.mc, maxDistance);
                        }
                    }
                    else
                    {
                        sm.changeSelection(this.mc.world, entity, maxDistance);
                    }
                }
                else if (mode.getUsesSchematic())
                {
                    DataManager.getSchematicPlacementManager().changeSelection(this.mc.world, entity, maxDistance);
                }

                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.FAIL;
    }
}
