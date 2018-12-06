package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiAreaSelectionManager;
import fi.dy.masa.litematica.gui.GuiAreaSelectionManager.SelectedBoxRenamer;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.litematica.gui.GuiConfigs.ConfigGuiTab;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.GuiPlacementManager;
import fi.dy.masa.litematica.gui.GuiRenderLayer;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.GuiSchematicSave.InMemorySchematicCreator;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSubRegionConfiguration;
import fi.dy.masa.litematica.mixin.IMixinKeyBinding;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.AreaSelectionMode;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.LayerMode;
import fi.dy.masa.litematica.util.OperationMode;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.GuiTextInputFeedback;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

public class KeyCallbacks
{
    public static void init()
    {
        Minecraft mc = Minecraft.getInstance();
        IHotkeyCallback callbackHotkeys = new KeyCallbackHotkeys(mc);
        IHotkeyCallback callbackMessage = new KeyCallbackToggleMessage(mc);
        ValueChangeCallback valueChangeCallback = new ValueChangeCallback();

        Configs.Generic.PICK_BLOCKABLE_SLOTS.setValueChangeCallback(valueChangeCallback);

        Hotkeys.EXECUTE_OPERATION.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.LAYER_MODE_NEXT.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.LAYER_MODE_PREVIOUS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.LAYER_NEXT.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.LAYER_PREVIOUS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.LAYER_SET_HERE.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_AREA_SETTINGS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_SCHEMATIC_PLACEMENTS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_SCHEMATIC_VERIFIER.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_SETTINGS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.PICK_BLOCK_FIRST.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.PICK_BLOCK_LAST.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.PICK_BLOCK_TOGGLE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.PICK_BLOCK_ENABLED));
        Hotkeys.RERENDER_SCHEMATIC.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SAVE_AREA_AS_IN_MEMORY_SCHEMATIC.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SAVE_AREA_AS_SCHEMATIC_TO_FILE.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_PLACE_CORNER_1.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_PLACE_CORNER_2.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_SELECT_ELEMENTS.getKeybind().setCallback(callbackHotkeys);

        Hotkeys.ADD_SELECTION_BOX.getKeybind().setCallback(callbackMessage);
        Hotkeys.DELETE_SELECTION_BOX.getKeybind().setCallback(callbackMessage);
        Hotkeys.EASY_PLACE_TOGGLE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.EASY_PLACE_MODE));
        Hotkeys.MOVE_ENTIRE_SELECTION.getKeybind().setCallback(callbackMessage);
        Hotkeys.SELECTION_MODE_CYCLE.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_AREA_ORIGIN.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_ALL_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.ENABLE_RENDERING));
        Hotkeys.TOGGLE_GHOST_BLOCK_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.ENABLE_GHOST_BLOCK_RENDERING));
        Hotkeys.TOGGLE_INFO_OVERLAY_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.InfoOverlays.ENABLE_INFO_OVERLAY_RENDERING));
        Hotkeys.TOGGLE_MISMATCH_OVERLAY_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.InfoOverlays.ENABLE_VERIFIER_OVERLAY_RENDERING));
        Hotkeys.TOGGLE_OVERLAY_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.SCHEMATIC_OVERLAY_ENABLED));
        Hotkeys.TOGGLE_OVERLAY_OUTLINE_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES));
        Hotkeys.TOGGLE_OVERLAY_SIDE_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES));
        Hotkeys.TOGGLE_SELECTION_BOXES_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.ENABLE_SELECTION_BOXES_RENDERING));
        Hotkeys.TOGGLE_TRANSLUCENT_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT));
        Hotkeys.TOOL_ENABLED_TOGGLE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.TOOL_ITEM_ENABLED));
    }

    private static class ValueChangeCallback implements IValueChangeCallback
    {
        @Override
        public void onValueChanged(IConfigBase config)
        {
            if (config == Configs.Generic.PICK_BLOCKABLE_SLOTS)
            {
                InventoryUtils.setPickBlockableSlots(Configs.Generic.PICK_BLOCKABLE_SLOTS.getStringValue());
            }
        }
    }

    private static class KeyCallbackHotkeys implements IHotkeyCallback
    {
        private final Minecraft mc;

        public KeyCallbackHotkeys(Minecraft mc)
        {
            this.mc = mc;
        }

        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            OperationMode mode = DataManager.getOperationMode();

            boolean toolEnabled = Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();
            boolean hasTool = EntityUtils.isHoldingItem(this.mc.player, DataManager.getToolItem());
            boolean isToolPrimary = key == Hotkeys.TOOL_PLACE_CORNER_1.getKeybind();
            boolean isToolSecondary = key == Hotkeys.TOOL_PLACE_CORNER_2.getKeybind();
            boolean isToolSelect = key == Hotkeys.TOOL_SELECT_ELEMENTS.getKeybind();

            if (toolEnabled && hasTool)
            {
                int maxDistance = 200;

                if (isToolPrimary || isToolSecondary)
                {
                    if (mode.getUsesAreaSelection())
                    {
                        SelectionManager sm = DataManager.getSelectionManager();
                        boolean moveEverything = Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld();

                        if (Configs.Generic.SELECTION_MODE.getOptionListValue() == AreaSelectionMode.CORNERS)
                        {
                            Corner corner = isToolPrimary ? Corner.CORNER_1 : Corner.CORNER_2;
                            sm.setPositionOfCurrentSelectionToRayTrace(this.mc, corner, moveEverything, maxDistance);
                        }
                        else if (Configs.Generic.SELECTION_MODE.getOptionListValue() == AreaSelectionMode.CUBOID)
                        {
                            sm.handleCuboidModeMouseClick(this.mc, maxDistance, isToolSecondary, moveEverything);
                        }
                    }
                    else if (mode.getUsesSchematic())
                    {
                        DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionToRayTrace(this.mc, maxDistance);
                    }

                    return true;
                }
                else if (isToolSelect)
                {
                    if (mode.getUsesSchematic())
                    {
                        DataManager.getSchematicPlacementManager().changeSelection(this.mc.world, this.mc.player, maxDistance);
                    }
                    else if (mode.getUsesAreaSelection())
                    {
                        SelectionManager sm = DataManager.getSelectionManager();

                        if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld())
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
                            sm.changeSelection(this.mc.world, this.mc.player, maxDistance);
                        }
                    }

                    return true;
                }
            }

            if (mode.getUsesSchematic())
            {
                if (key == Hotkeys.LAYER_NEXT.getKeybind())
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
                    DataManager.getRenderLayerRange().setToPosition(this.mc.player);
                    return true;
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
                else if (key == Hotkeys.PICK_BLOCK_FIRST.getKeybind())
                {
                    if (Configs.Generic.PICK_BLOCK_ENABLED.getBooleanValue())
                    {
                        return WorldUtils.doSchematicWorldPickBlock(true, this.mc);
                    }

                    return false;
                }
                else if (key == Hotkeys.PICK_BLOCK_LAST.getKeybind())
                {
                    if (Configs.Generic.PICK_BLOCK_ENABLED.getBooleanValue())
                    {
                        int keyCode = ((IMixinKeyBinding) mc.gameSettings.keyBindUseItem).getInput().getKeyCode();
                        String keyStrUse = KeybindMulti.getStorageStringForKeyCode(keyCode);
                        String keyStrPick = Hotkeys.PICK_BLOCK_LAST.getKeybind().getStringValue();

                        // Only do the pick block here, if it's not bound to the use button.
                        // If it's bound to the use button, then it will be done from the input handling.
                        if (keyStrUse.equals(keyStrPick) == false)
                        {
                            WorldUtils.doSchematicWorldPickBlock(false, this.mc);
                        }
                    }

                    return false;
                }
                else if (key == Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind())
                {
                    SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                    if (schematicPlacement != null)
                    {
                        SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

                        if (placement != null)
                        {
                            this.mc.displayGuiScreen(new GuiSubRegionConfiguration(schematicPlacement, placement));
                        }
                        else
                        {
                            this.mc.displayGuiScreen(new GuiPlacementConfiguration(schematicPlacement));
                        }
                    }
                    else
                    {
                        StringUtils.printActionbarMessage("litematica.message.no_placement_selected");
                    }

                    return true;
                }
                else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_VERIFIER.getKeybind())
                {
                    SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                    if (schematicPlacement != null)
                    {
                        this.mc.displayGuiScreen(new GuiSchematicVerifier(schematicPlacement));
                    }
                    else
                    {
                        StringUtils.printActionbarMessage("litematica.message.no_placement_selected");
                    }

                    return true;
                }
                else if (key == Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind())
                {
                    SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                    if (schematicPlacement != null)
                    {
                        this.mc.displayGuiScreen(new GuiMaterialList(schematicPlacement));
                    }
                    else
                    {
                        StringUtils.printActionbarMessage("litematica.message.no_placement_selected");
                    }

                    return true;
                }
            }
            else if (mode.getUsesAreaSelection())
            {
                if (key == Hotkeys.OPEN_GUI_AREA_SETTINGS.getKeybind())
                {
                    SelectionManager sm = DataManager.getSelectionManager();
                    AreaSelection sel = sm.getCurrentSelection();

                    if (sel != null)
                    {
                        String name = sel.getCurrentSubRegionBoxName();

                        if (name != null)
                        {
                            String title = "litematica.gui.title.rename_area_sub_region";
                            this.mc.displayGuiScreen(new GuiTextInputFeedback(128, title, name, null, new SelectedBoxRenamer(sm)));
                            return true;
                        }
                    }
                }
                else if (key == Hotkeys.SAVE_AREA_AS_SCHEMATIC_TO_FILE.getKeybind())
                {
                    SelectionManager sm = DataManager.getSelectionManager();
                    AreaSelection area = sm.getCurrentSelection();

                    if (area != null)
                    {
                        this.mc.displayGuiScreen(new GuiSchematicSave());
                        return true;
                    }
                }
                else if (key == Hotkeys.SAVE_AREA_AS_IN_MEMORY_SCHEMATIC.getKeybind())
                {
                    SelectionManager sm = DataManager.getSelectionManager();
                    AreaSelection area = sm.getCurrentSelection();

                    if (area != null)
                    {
                        String title = "litematica.gui.title.create_in_memory_schematic";
                        this.mc.displayGuiScreen(new GuiTextInput(128, title, area.getName(), null, new InMemorySchematicCreator(area)));
                        return true;
                    }
                }
            }

            if (key == Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind())
            {
                this.mc.displayGuiScreen(new GuiMainMenu());
                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind())
            {
                this.mc.displayGuiScreen(new GuiAreaSelectionManager());
                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_PLACEMENTS.getKeybind())
            {
                this.mc.displayGuiScreen(new GuiPlacementManager());
                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_SETTINGS.getKeybind())
            {
                if (DataManager.getConfigGuiTab() == ConfigGuiTab.RENDER_LAYERS)
                {
                    this.mc.displayGuiScreen(new GuiRenderLayer());
                }
                else
                {
                    this.mc.displayGuiScreen(new GuiConfigs());
                }

                return true;
            }
            else if (key == Hotkeys.EXECUTE_OPERATION.getKeybind() && hasTool && toolEnabled)
            {
                if (mode == OperationMode.PASTE_SCHEMATIC)
                {
                    DataManager.getSchematicPlacementManager().pasteCurrentPlacementToWorld(this.mc);
                    return true;
                }
                else if (mode == OperationMode.DELETE)
                {
                    WorldUtils.deleteSelectionVolumes(this.mc);
                    return true;
                }
            }
            else if (key == Hotkeys.RERENDER_SCHEMATIC.getKeybind())
            {
                if (mode.getUsesSchematic())
                {
                    WorldUtils.markAllSchematicChunksForRenderUpdate();
                    StringUtils.printActionbarMessage("litematica.message.schematic_rendering_refreshed");
                }
                return true;
            }

            return false;
        }
    }

    private static class KeyCallbackToggleMessage implements IHotkeyCallback
    {
        private final Minecraft mc;

        public KeyCallbackToggleMessage(Minecraft mc)
        {
            this.mc = mc;
        }

        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            OperationMode mode = DataManager.getOperationMode();

            if (key == Hotkeys.ADD_SELECTION_BOX.getKeybind())
            {
                if (mode.getUsesAreaSelection())
                {
                    SelectionManager sm = DataManager.getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        selection.createNewSubRegionBox(pos, selection.getName());

                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        StringUtils.printActionbarMessage("litematica.message.added_selection_box", posStr);

                        return true;
                    }
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
                        String name = selection.getCurrentSubRegionBoxName();

                        if (name != null && selection.removeSelectedSubRegionBox())
                        {
                            StringUtils.printActionbarMessage("litematica.message.removed_selection_box", name);
                            return true;
                        }
                    }
                }
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
                        selection.moveEntireSelectionTo(pos, true);
                        return true;
                    }
                }
                else if (mode.getUsesSchematic())
                {
                    BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                    DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionTo(pos, this.mc);
                    return true;
                }
            }
            else if (key == Hotkeys.SELECTION_MODE_CYCLE.getKeybind())
            {
                if (mode.getUsesAreaSelection())
                {
                    Configs.Generic.SELECTION_MODE.setOptionListValue(Configs.Generic.SELECTION_MODE.getOptionListValue().cycle(false));
                    return true;
                }
            }
            else if (key == Hotkeys.SET_AREA_ORIGIN.getKeybind())
            {
                if (mode.getUsesAreaSelection())
                {
                    SelectionManager sm = DataManager.getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        selection.setOrigin(pos);
                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        StringUtils.printActionbarMessage("litematica.message.set_area_origin", posStr);
                        return true;
                    }
                }
            }
            else if (key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() ||
                     key == Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind())
            {
                if (mode.getUsesAreaSelection())
                {
                    SelectionManager sm = DataManager.getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null && selection.getSelectedSubRegionBox() != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        int p = key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() ? 1 : 2;

                        if (p == 1)
                        {
                            selection.getSelectedSubRegionBox().setPos1(pos);
                        }
                        else
                        {
                            selection.getSelectedSubRegionBox().setPos2(pos);
                        }

                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        StringUtils.printActionbarMessage("litematica.message.set_selection_box_point", p, posStr);
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
