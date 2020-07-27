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
import fi.dy.masa.malilib.input.IHotkeyCallback;
import fi.dy.masa.malilib.input.IKeyBind;
import fi.dy.masa.malilib.input.KeyAction;
import fi.dy.masa.malilib.message.MessageType;
import fi.dy.masa.malilib.message.MessageUtils;

public class HotkeyCallbackMisc implements IHotkeyCallback
{
    private final Minecraft mc;

    public HotkeyCallbackMisc(Minecraft mc)
    {
        this.mc = mc;
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeyBind key)
    {
        if (this.mc.player == null || this.mc.world == null)
        {
            return false;
        }

        ToolMode mode = DataManager.getToolMode();

        if (key == Hotkeys.ADD_SELECTION_BOX.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                DataManager.getSelectionManager().createNewSubRegion(this.mc, true);
                return true;
            }
        }
        else if (key == Hotkeys.CLONE_SELECTION.getKeyBind())
        {
            if (DataManager.getSelectionManager().getCurrentSelection() != null)
            {
                ToolUtils.cloneSelectionArea(this.mc);
                return true;
            }
        }
        else if (key == Hotkeys.DELETE_SELECTION_BOX.getKeyBind())
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
                        MessageUtils.printActionbarMessage("litematica.message.removed_area_origin");
                        return true;
                    }
                    else
                    {
                        String name = selection.getCurrentSubRegionBoxName();

                        if (name != null && selection.removeSelectedSubRegionBox())
                        {
                            MessageUtils.printActionbarMessage("litematica.message.removed_selection_box", name);
                            return true;
                        }
                    }
                }
            }
        }
        else if (key == Hotkeys.DUPLICATE_PLACEMENT.getKeyBind())
        {
            if (DataManager.getSchematicPlacementManager().duplicateSelectedPlacement())
            {
                BlockPos pos = new BlockPos(this.mc.player);
                pos = PositionUtils.getPlacementPositionOffsetToInfrontOfPlayer(pos);
                DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionTo(pos);
                MessageUtils.printActionbarMessage("litematica.message.duplicated_selected_placement");
            }

            return true;
        }
        else if (key == Hotkeys.EXECUTE_OPERATION.getKeyBind())
        {
            boolean toolEnabled = Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();

            if (Configs.Generic.EXECUTE_REQUIRE_TOOL.getBooleanValue()
                && (EntityUtils.hasToolItem(this.mc.player) == false || toolEnabled == false))
            {
                String keyRenderToggle = Hotkeys.TOGGLE_ALL_RENDERING.getKeyBind().getKeysDisplayString();
                String keyToolToggle = Hotkeys.TOOL_ENABLED_TOGGLE.getKeyBind().getKeysDisplayString();
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, 8000, "litematica.error.execute_operation_no_tool", keyRenderToggle, keyToolToggle);
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
        else if (key == Hotkeys.LAYER_MODE_NEXT.getKeyBind())
        {
            DataManager.getRenderLayerRange().setLayerMode(DataManager.getRenderLayerRange().getLayerMode().cycle(true));
            return true;
        }
        else if (key == Hotkeys.LAYER_MODE_PREVIOUS.getKeyBind())
        {
            DataManager.getRenderLayerRange().setLayerMode(DataManager.getRenderLayerRange().getLayerMode().cycle(false));
            return true;
        }
        else if (key == Hotkeys.LAYER_NEXT.getKeyBind())
        {
            DataManager.getRenderLayerRange().moveLayer(1);
            return true;
        }
        else if (key == Hotkeys.LAYER_PREVIOUS.getKeyBind())
        {
            DataManager.getRenderLayerRange().moveLayer(-1);
            return true;
        }
        else if (key == Hotkeys.LAYER_SET_HERE.getKeyBind())
        {
            DataManager.getRenderLayerRange().setSingleBoundaryToPosition(this.mc.player);
            return true;
        }
        else if (key == Hotkeys.MOVE_ENTIRE_SELECTION.getKeyBind())
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
                DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionTo(pos);
                return true;
            }
        }
        else if (key == Hotkeys.NUDGE_SELECTION_NEGATIVE.getKeyBind() ||
                 key == Hotkeys.NUDGE_SELECTION_POSITIVE.getKeyBind())
        {
            int amount = key == Hotkeys.NUDGE_SELECTION_POSITIVE.getKeyBind() ? 1 : -1;
            InputHandler.nudgeSelection(amount, mode, this.mc.player);
            return true;
        }
        else if (key == Hotkeys.PICK_BLOCK_FIRST.getKeyBind())
        {
            if (EntityUtils.shouldPickBlock(this.mc.player))
            {
                return InventoryUtils.pickBlockFirst(this.mc);
            }

            return false;
        }
        else if (key == Hotkeys.PICK_BLOCK_LAST.getKeyBind())
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
        else if (key == Hotkeys.REMOVE_SELECTED_PLACEMENT.getKeyBind())
        {
            DataManager.getSchematicPlacementManager().removeSelectedSchematicPlacement();
            return true;
        }
        else if (key == Hotkeys.RERENDER_SCHEMATIC.getKeyBind())
        {
            SchematicWorldRenderingNotifier.INSTANCE.updateAll();
            MessageUtils.printActionbarMessage("litematica.message.schematic_rendering_refreshed");
            return true;
        }
        else if (key == Hotkeys.ROTATE_PLACEMENT_CW.getKeyBind())
        {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            SchematicPlacement placement = manager.getSelectedSchematicPlacement();

            if (placement != null)
            {
                manager.rotateBy(placement, Rotation.CLOCKWISE_90);
                return true;
            }
        }
        else if (key == Hotkeys.ROTATE_PLACEMENT_CCW.getKeyBind())
        {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            SchematicPlacement placement = manager.getSelectedSchematicPlacement();

            if (placement != null)
            {
                manager.rotateBy(placement, Rotation.COUNTERCLOCKWISE_90);
                return true;
            }
        }
        else if (key == Hotkeys.SAVE_AREA_AS_IN_MEMORY_SCHEMATIC.getKeyBind())
        {
            return SchematicUtils.saveSchematic(true);
        }
        else if (key == Hotkeys.SAVE_AREA_AS_SCHEMATIC_TO_FILE.getKeyBind())
        {
            return SchematicUtils.saveSchematic(false);
        }
        else if (key == Hotkeys.SCHEMATIC_REBUILD_ACCEPT_REPLACEMENT.getKeyBind())
        {
            SchematicEditUtils.rebuildAcceptReplacement(this.mc);
            return true;
        }
        else if (key == Hotkeys.SCHEMATIC_VERSION_CYCLE_NEXT.getKeyBind())
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                DataManager.getSchematicProjectsManager().cycleVersion(1);
                return true;
            }
        }
        else if (key == Hotkeys.SCHEMATIC_VERSION_CYCLE_PREVIOUS.getKeyBind())
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                DataManager.getSchematicProjectsManager().cycleVersion(-1);
                return true;
            }
        }
        else if (key == Hotkeys.SELECTION_GROW_HOTKEY.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                PositionUtils.growOrShrinkCurrentSelection(true);
                return true;
            }
        }
        else if (key == Hotkeys.SELECTION_MODE_CYCLE.getKeyBind())
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
        else if (key == Hotkeys.SELECTION_SHRINK.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                PositionUtils.growOrShrinkCurrentSelection(false);
                return true;
            }
        }
        else if (key == Hotkeys.SET_AREA_ORIGIN.getKeyBind())
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
                    MessageUtils.printActionbarMessage("litematica.message.set_area_origin", posStr);
                    return true;
                }
            }
        }
        else if (key == Hotkeys.SET_HELD_ITEM_AS_TOOL.getKeyBind())
        {
            DataManager.setHeldItemAsTool();
            return true;
        }
        else if (key == Hotkeys.SET_SCHEMATIC_PREVIEW.getKeyBind())
        {
            if (GuiSchematicManager.hasPendingPreviewTask())
            {
                GuiSchematicManager.setPreviewImage();
                return true;
            }
        }
        else if (key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeyBind() ||
                 key == Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeyBind())
        {
            if (mode.getUsesAreaSelection())
            {
                SelectionManager sm = DataManager.getSelectionManager();
                AreaSelection area = sm.getCurrentSelection();

                if (area != null && area.getSelectedSubRegionBox() != null)
                {
                    BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                    Corner corner = key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeyBind() ? Corner.CORNER_1 : Corner.CORNER_2;
                    area.setSelectedSubRegionCornerPos(pos, corner);

                    if (Configs.Generic.CHANGE_SELECTED_CORNER.getBooleanValue())
                    {
                        area.getSelectedSubRegionBox().setSelectedCorner(corner);
                    }

                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    MessageUtils.printActionbarMessage("litematica.message.set_selection_box_point", corner.ordinal(), posStr);
                    return true;
                }
            }
        }
        else if (key == Hotkeys.TOOL_MODE_CYCLE_BACKWARD.getKeyBind())
        {
            DataManager.setToolMode(DataManager.getToolMode().cycle(this.mc.player, false));
            return true;
        }
        else if (key == Hotkeys.TOOL_MODE_CYCLE_FORWARD.getKeyBind())
        {
            DataManager.setToolMode(DataManager.getToolMode().cycle(this.mc.player, true));
            return true;
        }
        else if (key == Hotkeys.UNLOAD_CURRENT_SCHEMATIC.getKeyBind())
        {
            DataManager.getSchematicPlacementManager().unloadCurrentlySelectedSchematic();
            return true;
        }
        else if (key == Hotkeys.UPDATE_BLOCKS.getKeyBind())
        {
            ToolUtils.updateSelectionVolumes(this.mc);
            return true;
        }

        return false;
    }
}
