package litematica.config;

import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

import malilib.config.value.LayerMode;
import malilib.input.ActionResult;
import malilib.input.KeyAction;
import malilib.input.KeyBind;
import malilib.input.callback.HotkeyCallback;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.ListUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameUtils;
import litematica.data.DataManager;
import litematica.input.MouseScrollHandlerImpl;
import litematica.scheduler.TaskScheduler;
import litematica.scheduler.tasks.SetSchematicPreviewTask;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;
import litematica.schematic.util.SchematicCreationUtils;
import litematica.schematic.util.SchematicEditUtils;
import litematica.schematic.util.SchematicPlacingUtils;
import litematica.selection.AreaSelection;
import litematica.selection.AreaSelectionManager;
import litematica.selection.BoxCorner;
import litematica.tool.ToolMode;
import litematica.tool.ToolModeData;
import litematica.util.EntityUtils;
import litematica.util.PickBlockUtils;
import litematica.util.PositionUtils;
import litematica.util.ToolUtils;
import litematica.world.SchematicWorldRenderingNotifier;

public class HotkeyCallbackMisc implements HotkeyCallback
{
    @Override
    public ActionResult onKeyAction(KeyAction action, KeyBind key)
    {
        if (GameUtils.getCameraEntity() == null || GameUtils.getClientWorld() == null)
        {
            return ActionResult.FAIL;
        }

        ToolMode mode = DataManager.getToolMode();

        if (key == Hotkeys.ADD_SELECTION_BOX.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                DataManager.getAreaSelectionManager().createNewSubRegion(true);
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.CLONE_SELECTION.getKeyBind())
        {
            if (DataManager.getAreaSelectionManager().getCurrentSelection() != null)
            {
                ToolUtils.cloneSelectionArea();
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.DELETE_SELECTION_BOX.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                AreaSelectionManager sm = DataManager.getAreaSelectionManager();
                AreaSelection selection = sm.getCurrentSelection();

                if (selection != null)
                {
                    if (selection.isOriginSelected())
                    {
                        selection.setManualOrigin(null);
                        selection.setOriginSelected(false);
                        MessageDispatcher.generic().customHotbar().translate("litematica.message.removed_area_origin");
                        return ActionResult.SUCCESS;
                    }
                    else
                    {
                        String name = selection.getSelectedSelectionBoxName();

                        if (name != null && selection.removeSelectedBox())
                        {
                            MessageDispatcher.generic().customHotbar().translate("litematica.message.removed_selection_box", name);
                            return ActionResult.SUCCESS;
                        }
                    }
                }
            }
        }
        else if (key == Hotkeys.DUPLICATE_PLACEMENT.getKeyBind())
        {
            if (DataManager.getSchematicPlacementManager().duplicateSelectedPlacement())
            {
                BlockPos pos = EntityWrap.getCameraEntityBlockPos();
                pos = PositionUtils.getPlacementPositionOffsetToInFrontOfPlayer(pos);
                DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionTo(pos);
                MessageDispatcher.generic().customHotbar().translate("litematica.message.duplicated_selected_placement");
            }

            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.EXECUTE_OPERATION.getKeyBind())
        {
            boolean toolEnabled = Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();

            if (Configs.Generic.EXECUTE_REQUIRE_TOOL.getBooleanValue()
                && (EntityUtils.hasToolItem() == false || toolEnabled == false))
            {
                String keyRenderToggle = Configs.Visuals.MAIN_RENDERING_TOGGLE.getKeyBind().getKeysDisplayString();
                String keyToolToggle = Configs.Generic.TOOL_ITEM_ENABLED.getKeyBind().getKeysDisplayString();
                MessageDispatcher.error(8000).translate("litematica.error.execute_operation_no_tool", keyRenderToggle, keyToolToggle);
                return ActionResult.SUCCESS;
            }

            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                DataManager.getSchematicProjectsManager().pasteCurrentVersionToWorld();
                return ActionResult.SUCCESS;
            }
            else if (mode == ToolMode.PASTE_SCHEMATIC)
            {
                SchematicPlacingUtils.pasteCurrentPlacementToWorld();
                return ActionResult.SUCCESS;
            }
            else if (mode == ToolMode.GRID_PASTE)
            {
                SchematicPlacingUtils.gridPasteCurrentPlacementToWorld();
                return ActionResult.SUCCESS;
            }
            else if (mode == ToolMode.FILL && mode.getPrimaryBlock() != null)
            {
                ToolUtils.fillSelectionVolumes(mode.getPrimaryBlock(), null);
                return ActionResult.SUCCESS;
            }
            else if (mode == ToolMode.REPLACE_BLOCK && mode.getPrimaryBlock() != null && mode.getSecondaryBlock() != null)
            {
                ToolUtils.fillSelectionVolumes(mode.getPrimaryBlock(), mode.getSecondaryBlock());
                return ActionResult.SUCCESS;
            }
            else if (mode == ToolMode.DELETE)
            {
                boolean removeEntities = true; // TODO
                ToolUtils.deleteSelectionVolumes(removeEntities);
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.LAYER_MODE_NEXT.getKeyBind())
        {
            LayerMode layerMode = DataManager.getRenderLayerRange().getLayerMode();
            layerMode = ListUtils.getNextEntry(LayerMode.VALUES, layerMode, true);
            DataManager.getRenderLayerRange().setLayerMode(layerMode);
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.LAYER_MODE_PREVIOUS.getKeyBind())
        {
            LayerMode layerMode = DataManager.getRenderLayerRange().getLayerMode();
            layerMode = ListUtils.getNextEntry(LayerMode.VALUES, layerMode, false);
            DataManager.getRenderLayerRange().setLayerMode(layerMode);
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.LAYER_NEXT.getKeyBind())
        {
            DataManager.getRenderLayerRange().moveLayer(1);
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.LAYER_PREVIOUS.getKeyBind())
        {
            DataManager.getRenderLayerRange().moveLayer(-1);
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.LAYER_SET_HERE.getKeyBind())
        {
            DataManager.getRenderLayerRange().setSingleBoundaryToPosition(GameUtils.getCameraEntity());
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.MOVE_ENTIRE_SELECTION.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                AreaSelectionManager sm = DataManager.getAreaSelectionManager();
                AreaSelection selection = sm.getCurrentSelection();

                if (selection != null)
                {
                    BlockPos pos = EntityWrap.getCameraEntityBlockPos();

                    if (mode == ToolMode.MOVE)
                    {
                        ToolUtils.moveCurrentlySelectedWorldRegionTo(pos);
                    }
                    else
                    {
                        selection.moveEntireAreaSelectionTo(pos, true);
                    }

                    return ActionResult.SUCCESS;
                }
            }
            else if (mode.getUsesSchematic())
            {
                BlockPos pos = EntityWrap.getCameraEntityBlockPos();
                pos = PositionUtils.getPlacementPositionOffsetToInFrontOfPlayer(pos);
                DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionTo(pos);
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.NUDGE_SELECTION_NEGATIVE.getKeyBind() ||
                 key == Hotkeys.NUDGE_SELECTION_POSITIVE.getKeyBind())
        {
            int amount = key == Hotkeys.NUDGE_SELECTION_POSITIVE.getKeyBind() ? 1 : -1;
            MouseScrollHandlerImpl.nudgeSelection(amount, mode, GameUtils.getCameraEntity());
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.PICK_BLOCK_FIRST.getKeyBind())
        {
            if (PickBlockUtils.shouldPickBlock() && PickBlockUtils.pickBlockFirst())
            {
                return ActionResult.SUCCESS;
            }

            return ActionResult.FAIL;
        }
        else if (key == Hotkeys.PICK_BLOCK_LAST.getKeyBind())
        {
            // Don't use pick block last in the Rebuild mode, as that will interfere
            // with all the rebuild actions...
            // Also don't do the pick block from here if pickBlockAuto is enabled,
            // since in that case it's done from the vanilla right click handling code.
            if (DataManager.getToolMode() != ToolMode.SCHEMATIC_EDIT &&
                Configs.Generic.PICK_BLOCK_AUTO.getBooleanValue() == false &&
                PickBlockUtils.shouldPickBlock())
            {
                PickBlockUtils.pickBlockLast();
                return ActionResult.SUCCESS;
            }

            return ActionResult.FAIL;
        }
        else if (key == Hotkeys.REMOVE_SELECTED_PLACEMENT.getKeyBind())
        {
            if (DataManager.getSchematicPlacementManager().removeSelectedSchematicPlacement())
            {
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.REFRESH_SCHEMATIC_RENDERER.getKeyBind())
        {
            SchematicWorldRenderingNotifier.INSTANCE.updateAll();
            MessageDispatcher.generic().customHotbar().translate("litematica.message.schematic_rendering_refreshed");
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.ROTATE_PLACEMENT_CW.getKeyBind())
        {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            SchematicPlacement placement = manager.getSelectedSchematicPlacement();

            if (placement != null)
            {
                manager.rotateBy(placement, Rotation.CLOCKWISE_90);
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.ROTATE_PLACEMENT_CCW.getKeyBind())
        {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            SchematicPlacement placement = manager.getSelectedSchematicPlacement();

            if (placement != null)
            {
                manager.rotateBy(placement, Rotation.COUNTERCLOCKWISE_90);
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.CREATE_SCHEMATIC_IN_MEMORY.getKeyBind())
        {
            return SchematicCreationUtils.saveSchematic(true);
        }
        else if (key == Hotkeys.SAVE_SCHEMATIC_TO_FILE.getKeyBind())
        {
            return SchematicCreationUtils.saveSchematic(false);
        }
        else if (key == Hotkeys.SCHEMATIC_EDIT_ACCEPT_REPLACEMENT.getKeyBind())
        {
            SchematicEditUtils.rebuildAcceptReplacement();
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.SCHEMATIC_VERSION_CYCLE_NEXT.getKeyBind())
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                DataManager.getSchematicProjectsManager().cycleVersion(1);
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.SCHEMATIC_VERSION_CYCLE_PREVIOUS.getKeyBind())
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                DataManager.getSchematicProjectsManager().cycleVersion(-1);
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.SELECTION_GROW_HOTKEY.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                PositionUtils.growOrShrinkCurrentSelection(true);
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.SUB_MODE_CYCLE.getKeyBind())
        {
            if (mode == ToolMode.DELETE)
            {
                ToolModeData.DELETE.toggleUsePlacement();
                return ActionResult.SUCCESS;
            }
            else if (mode == ToolMode.PASTE_SCHEMATIC || mode == ToolMode.GRID_PASTE)
            {
                Configs.Generic.PASTE_REPLACE_BEHAVIOR.cycleValue(false);
                return ActionResult.SUCCESS;
            }
            else if (mode.getUsesAreaSelection())
            {
                Configs.Generic.TOOL_SELECTION_MODE.cycleValue(false);
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.SELECTION_SHRINK.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                PositionUtils.growOrShrinkCurrentSelection(false);
                return ActionResult.SUCCESS;
            }
        }
        else if (key == Hotkeys.SET_AREA_ORIGIN.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                AreaSelectionManager sm = DataManager.getAreaSelectionManager();
                AreaSelection area = sm.getCurrentSelection();

                if (area != null)
                {
                    BlockPos pos = EntityWrap.getCameraEntityBlockPos();
                    area.setManualOrigin(pos);
                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    MessageDispatcher.generic().customHotbar().translate("litematica.message.set_area_origin", posStr);
                    return ActionResult.SUCCESS;
                }
            }
        }
        else if (key == Hotkeys.SET_HELD_ITEM_AS_TOOL.getKeyBind())
        {
            DataManager.setHeldItemAsTool();
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.SET_SCHEMATIC_PREVIEW.getKeyBind())
        {
            SetSchematicPreviewTask task = TaskScheduler.getInstanceClient().getFirstTaskOfType(SetSchematicPreviewTask.class);

            if (task != null)
            {
                task.setPreview();
                return ActionResult.SUCCESS;
            }

            return ActionResult.FAIL;
        }
        else if (key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeyBind() ||
                 key == Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                AreaSelectionManager sm = DataManager.getAreaSelectionManager();
                AreaSelection area = sm.getCurrentSelection();

                if (area != null && area.getSelectedSelectionBox() != null)
                {
                    BlockPos pos = EntityWrap.getCameraEntityBlockPos();
                    BoxCorner corner = key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeyBind() ? BoxCorner.CORNER_1 : BoxCorner.CORNER_2;
                    area.setSelectedSelectionBoxCornerPos(pos, corner);

                    if (Configs.Generic.CHANGE_SELECTED_CORNER.getBooleanValue())
                    {
                        area.getSelectedSelectionBox().setSelectedCorner(corner);
                    }

                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    MessageDispatcher.generic().customHotbar().translate("litematica.message.set_selection_box_point",
                                                                         corner.ordinal(), posStr);
                    return ActionResult.SUCCESS;
                }
            }
        }
        else if (key == Hotkeys.TOOL_MODE_CYCLE_BACKWARD.getKeyBind())
        {
            DataManager.setToolMode(DataManager.getToolMode().cycle(false));
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.TOOL_MODE_CYCLE_FORWARD.getKeyBind())
        {
            DataManager.setToolMode(DataManager.getToolMode().cycle(true));
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.UNLOAD_CURRENT_SCHEMATIC.getKeyBind())
        {
            DataManager.getSchematicPlacementManager().unloadCurrentlySelectedSchematic();
            return ActionResult.SUCCESS;
        }
        else if (key == Hotkeys.UPDATE_BLOCKS.getKeyBind())
        {
            ToolUtils.updateSelectionVolumes();
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }
}
