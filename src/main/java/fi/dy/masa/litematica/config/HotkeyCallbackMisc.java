package fi.dy.masa.litematica.config;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.litematica.gui.GuiSchematicManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.util.SchematicEditUtils;
import fi.dy.masa.litematica.schematic.util.SchematicPlacingUtils;
import fi.dy.masa.litematica.schematic.util.SchematicUtils;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.tool.ToolModeData;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.ToolUtils;
import fi.dy.masa.litematica.world.SchematicWorldRenderingNotifier;
import fi.dy.masa.malilib.config.values.LayerMode;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.util.InfoUtils;

public class HotkeyCallbackMisc implements IHotkeyCallback
{
    private final Minecraft mc;

    public HotkeyCallbackMisc(Minecraft mc)
    {
        this.mc = mc;
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key)
    {
        if (this.mc.player == null || this.mc.world == null)
        {
            return false;
        }

        ToolMode mode = DataManager.getToolMode();

        if (key == Hotkeys.ADD_SELECTION_BOX.getKeybind())
        {
            if (mode.getUsesAreaSelection())
            {
                DataManager.getSelectionManager().createNewSubRegion(this.mc, true);
                return true;
            }
        }
        else if (key == Hotkeys.CLONE_SELECTION.getKeybind())
        {
            if (DataManager.getSelectionManager().getCurrentSelection() != null)
            {
                ToolUtils.cloneSelectionArea(this.mc);
                return true;
            }
        }
        else if (key == Hotkeys.DELETE_SELECTION_BOX.getKeybind())
        {
            if (mode.getUsesAreaSelection())
            {
                SelectionManager sm = DataManager.getSelectionManager();
                AreaSelection selection = sm.getCurrentSelection();

                if (selection != null)
                {
                    if (selection.isOriginSelected())
                    {
                        selection.setExplicitOrigin(null);
                        selection.setOriginSelected(false);
                        InfoUtils.printActionbarMessage("litematica.message.removed_area_origin");
                        return true;
                    }
                    else
                    {
                        String name = selection.getCurrentSubRegionBoxName();

                        if (name != null && selection.removeSelectedSubRegionBox())
                        {
                            InfoUtils.printActionbarMessage("litematica.message.removed_selection_box", name);
                            return true;
                        }
                    }
                }
            }
        }
        else if (key == Hotkeys.DUPLICATE_PLACEMENT.getKeybind())
        {
            if (DataManager.getSchematicPlacementManager().duplicateSelectedPlacement())
            {
                BlockPos pos = new BlockPos(this.mc.player);
                pos = PositionUtils.getPlacementPositionOffsetToInfrontOfPlayer(pos);
                DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionTo(pos, this.mc);
                InfoUtils.printActionbarMessage("litematica.message.duplicated_selected_placement");
            }

            return true;
        }
        else if (key == Hotkeys.EXECUTE_OPERATION.getKeybind())
        {
            boolean toolEnabled = Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();

            if (Configs.Generic.EXECUTE_REQUIRE_TOOL.getBooleanValue()
                && (EntityUtils.hasToolItem(this.mc.player) == false || toolEnabled == false))
            {
                String keyRenderToggle = Hotkeys.TOGGLE_ALL_RENDERING.getKeybind().getKeysDisplayString();
                String keyToolToggle = Hotkeys.TOOL_ENABLED_TOGGLE.getKeybind().getKeysDisplayString();
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, 8000, "litematica.error.execute_operation_no_tool", keyRenderToggle, keyToolToggle);
                return true;
            }

            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                DataManager.getSchematicProjectsManager().pasteCurrentVersionToWorld();
                return true;
            }
            else if (mode == ToolMode.PASTE_SCHEMATIC)
            {
                SchematicPlacingUtils.pasteCurrentPlacementToWorld(this.mc);
                return true;
            }
            else if (mode == ToolMode.GRID_PASTE)
            {
                SchematicPlacingUtils.gridPasteCurrentPlacementToWorld(this.mc);
                return true;
            }
            else if (mode == ToolMode.FILL && mode.getPrimaryBlock() != null)
            {
                ToolUtils.fillSelectionVolumes(this.mc, mode.getPrimaryBlock(), null);
                return true;
            }
            else if (mode == ToolMode.REPLACE_BLOCK && mode.getPrimaryBlock() != null && mode.getSecondaryBlock() != null)
            {
                ToolUtils.fillSelectionVolumes(this.mc, mode.getPrimaryBlock(), mode.getSecondaryBlock());
                return true;
            }
            else if (mode == ToolMode.DELETE)
            {
                boolean removeEntities = true; // TODO
                ToolUtils.deleteSelectionVolumes(removeEntities, this.mc);
                return true;
            }
        }
        else if (key == Hotkeys.LAYER_MODE_NEXT.getKeybind())
        {
            DataManager.getRenderLayerRange().setLayerMode((LayerMode) DataManager.getRenderLayerRange().getLayerMode().cycle(true));
            return true;
        }
        else if (key == Hotkeys.LAYER_MODE_PREVIOUS.getKeybind())
        {
            DataManager.getRenderLayerRange().setLayerMode((LayerMode) DataManager.getRenderLayerRange().getLayerMode().cycle(false));
            return true;
        }
        else if (key == Hotkeys.LAYER_NEXT.getKeybind())
        {
            DataManager.getRenderLayerRange().moveLayer(1);
            return true;
        }
        else if (key == Hotkeys.LAYER_PREVIOUS.getKeybind())
        {
            DataManager.getRenderLayerRange().moveLayer(-1);
            return true;
        }
        else if (key == Hotkeys.LAYER_SET_HERE.getKeybind())
        {
            DataManager.getRenderLayerRange().setSingleBoundaryToPosition(this.mc.player);
            return true;
        }
        else if (key == Hotkeys.MOVE_ENTIRE_SELECTION.getKeybind())
        {
            if (mode.getUsesAreaSelection())
            {
                SelectionManager sm = DataManager.getSelectionManager();
                AreaSelection selection = sm.getCurrentSelection();

                if (selection != null)
                {
                    BlockPos pos = new BlockPos(this.mc.player.getPositionVector());

                    if (mode == ToolMode.MOVE)
                    {
                        ToolUtils.moveCurrentlySelectedWorldRegionTo(pos, this.mc);
                    }
                    else
                    {
                        selection.moveEntireSelectionTo(pos, true);
                    }

                    return true;
                }
            }
            else if (mode.getUsesSchematic())
            {
                BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                pos = PositionUtils.getPlacementPositionOffsetToInfrontOfPlayer(pos);
                DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionTo(pos, this.mc);
                return true;
            }
        }
        else if (key == Hotkeys.NUDGE_SELECTION_NEGATIVE.getKeybind() ||
                 key == Hotkeys.NUDGE_SELECTION_POSITIVE.getKeybind())
        {
            int amount = key == Hotkeys.NUDGE_SELECTION_POSITIVE.getKeybind() ? 1 : -1;
            InputHandler.nudgeSelection(amount, mode, this.mc.player);
            return true;
        }
        else if (key == Hotkeys.PICK_BLOCK_FIRST.getKeybind())
        {
            if (EntityUtils.shouldPickBlock(this.mc.player))
            {
                return InventoryUtils.pickBlockFirst(this.mc);
            }

            return false;
        }
        else if (key == Hotkeys.PICK_BLOCK_LAST.getKeybind())
        {
            // Don't use pick block last in the Rebuild mode, as that will interfere
            // with all the rebuild actions...
            // Also don't do the pick block from here if pickBlockAuto is enabled,
            // since in that case it's done from the vanilla right click handling code.
            if (DataManager.getToolMode() != ToolMode.REBUILD &&
                Configs.Generic.PICK_BLOCK_AUTO.getBooleanValue() == false &&
                EntityUtils.shouldPickBlock(this.mc.player))
            {
                InventoryUtils.pickBlockLast(true, this.mc);
                return true;
            }

            return false;
        }
        else if (key == Hotkeys.REMOVE_SELECTED_PLACEMENT.getKeybind())
        {
            DataManager.getSchematicPlacementManager().removeSelectedSchematicPlacement();
            return true;
        }
        else if (key == Hotkeys.RERENDER_SCHEMATIC.getKeybind())
        {
            SchematicWorldRenderingNotifier.INSTANCE.updateAll();
            InfoUtils.printActionbarMessage("litematica.message.schematic_rendering_refreshed");
            return true;
        }
        else if (key == Hotkeys.ROTATE_PLACEMENT_CW.getKeybind())
        {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            SchematicPlacement placement = manager.getSelectedSchematicPlacement();

            if (placement != null)
            {
                manager.rotateBy(placement, Rotation.CLOCKWISE_90);
                return true;
            }
        }
        else if (key == Hotkeys.ROTATE_PLACEMENT_CCW.getKeybind())
        {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            SchematicPlacement placement = manager.getSelectedSchematicPlacement();

            if (placement != null)
            {
                manager.rotateBy(placement, Rotation.COUNTERCLOCKWISE_90);
                return true;
            }
        }
        else if (key == Hotkeys.SAVE_AREA_AS_IN_MEMORY_SCHEMATIC.getKeybind())
        {
            return SchematicUtils.saveSchematic(true);
        }
        else if (key == Hotkeys.SAVE_AREA_AS_SCHEMATIC_TO_FILE.getKeybind())
        {
            return SchematicUtils.saveSchematic(false);
        }
        else if (key == Hotkeys.SCHEMATIC_REBUILD_ACCEPT_REPLACEMENT.getKeybind())
        {
            SchematicEditUtils.rebuildAcceptReplacement(this.mc);
            return true;
        }
        else if (key == Hotkeys.SCHEMATIC_VERSION_CYCLE_NEXT.getKeybind())
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                DataManager.getSchematicProjectsManager().cycleVersion(1);
                return true;
            }
        }
        else if (key == Hotkeys.SCHEMATIC_VERSION_CYCLE_PREVIOUS.getKeybind())
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                DataManager.getSchematicProjectsManager().cycleVersion(-1);
                return true;
            }
        }
        else if (key == Hotkeys.SELECTION_GROW_HOTKEY.getKeybind())
        {
            if (mode.getUsesAreaSelection())
            {
                PositionUtils.growOrShrinkCurrentSelection(true);
                return true;
            }
        }
        else if (key == Hotkeys.SELECTION_MODE_CYCLE.getKeybind())
        {
            if (mode == ToolMode.DELETE)
            {
                ToolModeData.DELETE.toggleUsePlacement();
                return true;
            }
            else if (mode == ToolMode.PASTE_SCHEMATIC)
            {
                Configs.Generic.PASTE_REPLACE_BEHAVIOR.cycleValue(false);
                return true;
            }
            else if (mode.getUsesAreaSelection())
            {
                Configs.Generic.SELECTION_CORNERS_MODE.cycleValue(false);
                return true;
            }
        }
        else if (key == Hotkeys.SELECTION_SHRINK.getKeybind())
        {
            if (mode.getUsesAreaSelection())
            {
                PositionUtils.growOrShrinkCurrentSelection(false);
                return true;
            }
        }
        else if (key == Hotkeys.SET_AREA_ORIGIN.getKeybind())
        {
            if (mode.getUsesAreaSelection())
            {
                SelectionManager sm = DataManager.getSelectionManager();
                AreaSelection area = sm.getCurrentSelection();

                if (area != null)
                {
                    BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                    area.setExplicitOrigin(pos);
                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    InfoUtils.printActionbarMessage("litematica.message.set_area_origin", posStr);
                    return true;
                }
            }
        }
        else if (key == Hotkeys.SET_HELD_ITEM_AS_TOOL.getKeybind())
        {
            DataManager.setHeldItemAsTool();
            return true;
        }
        else if (key == Hotkeys.SET_SCHEMATIC_PREVIEW.getKeybind())
        {
            if (GuiSchematicManager.hasPendingPreviewTask())
            {
                GuiSchematicManager.setPreviewImage();
                return true;
            }
        }
        else if (key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() ||
                 key == Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind())
        {
            if (mode.getUsesAreaSelection())
            {
                SelectionManager sm = DataManager.getSelectionManager();
                AreaSelection area = sm.getCurrentSelection();

                if (area != null && area.getSelectedSubRegionBox() != null)
                {
                    BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                    Corner corner = key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() ? Corner.CORNER_1 : Corner.CORNER_2;
                    area.setSelectedSubRegionCornerPos(pos, corner);

                    if (Configs.Generic.CHANGE_SELECTED_CORNER.getBooleanValue())
                    {
                        area.getSelectedSubRegionBox().setSelectedCorner(corner);
                    }

                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    InfoUtils.printActionbarMessage("litematica.message.set_selection_box_point", corner.ordinal(), posStr);
                    return true;
                }
            }
        }
        else if (key == Hotkeys.TOOL_MODE_CYCLE_BACKWARD.getKeybind())
        {
            DataManager.setToolMode(DataManager.getToolMode().cycle(this.mc.player, false));
            return true;
        }
        else if (key == Hotkeys.TOOL_MODE_CYCLE_FORWARD.getKeybind())
        {
            DataManager.setToolMode(DataManager.getToolMode().cycle(this.mc.player, true));
            return true;
        }
        else if (key == Hotkeys.UNLOAD_CURRENT_SCHEMATIC.getKeybind())
        {
            DataManager.getSchematicPlacementManager().unloadCurrentlySelectedSchematic();
            return true;
        }

        return false;
    }
}
