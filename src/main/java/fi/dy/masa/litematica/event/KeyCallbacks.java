package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.gui.GuiAreaSelectionManager;
import fi.dy.masa.litematica.gui.GuiAreaSelectionManager.SelectedBoxRenamer;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.GuiSchematicSave.InMemorySchematicCreator;
import fi.dy.masa.litematica.gui.GuiSubRegionConfiguration;
import fi.dy.masa.litematica.gui.GuiTextInput;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.AreaSelectionMode;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.OperationMode;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

public class KeyCallbacks
{
    public static void init(Minecraft mc)
    {
        IHotkeyCallback callbackHotkeys = new KeyCallbackHotkeys(mc);
        IHotkeyCallback callbackMessage = new KeyCallbackToggleMessage(mc);

        Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SAVE_AREA_AS_IN_MEMORY_SCHEMATIC.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SAVE_AREA_AS_SCHEMATIC_TO_FILE.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_SELECT_ELEMENTS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_PLACE_CORNER_1.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_PLACE_CORNER_2.getKeybind().setCallback(callbackHotkeys);

        Hotkeys.ADD_SELECTION_BOX.getKeybind().setCallback(callbackMessage);
        Hotkeys.DELETE_SELECTION_BOX.getKeybind().setCallback(callbackMessage);
        Hotkeys.MOVE_ENTIRE_SELECTION.getKeybind().setCallback(callbackMessage);
        Hotkeys.SELECTION_MODE_CYCLE.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_AREA_ORIGIN.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_ALL_RENDERING.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_GHOST_BLOCK_RENDERING.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_SELECTION_BOXES_RENDERING.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_TRANSLUCENT_RENDERING.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOOL_ENABLED_TOGGLE.getKeybind().setCallback(callbackMessage);
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
            if (action == KeyAction.RELEASE)
            {
                return false;
            }

            OperationMode mode = DataManager.getOperationMode();
            DataManager dataManager = DataManager.getInstance(this.mc.world);

            boolean toolEnabled = RenderEventHandler.getInstance().isEnabled() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();
            boolean hasTool = EntityUtils.isHoldingItem(this.mc.player, DataManager.getToolItem());
            boolean isToolPrimary = key == Hotkeys.TOOL_PLACE_CORNER_1.getKeybind();
            boolean isToolSecondary = key == Hotkeys.TOOL_PLACE_CORNER_2.getKeybind();
            boolean isToolSelect = key == Hotkeys.TOOL_SELECT_ELEMENTS.getKeybind();

            if (toolEnabled && hasTool)
            {
                int maxDistance = 200;

                if (isToolPrimary || isToolSecondary)
                {
                    if (mode == OperationMode.AREA_SELECTION)
                    {
                        SelectionManager sm = dataManager.getSelectionManager();

                        if (Configs.Generic.SELECTION_MODE.getOptionListValue() == AreaSelectionMode.CORNERS)
                        {
                            sm.setPositionOfCurrentSelectionToRayTrace(this.mc, isToolPrimary ? Corner.CORNER_1 : Corner.CORNER_2, maxDistance);
                        }
                        else if (Configs.Generic.SELECTION_MODE.getOptionListValue() == AreaSelectionMode.CUBOID)
                        {
                            // Left click in Cuboid mode: Grow the selection to contain each clicked position
                            if (isToolPrimary)
                            {
                                sm.growSelectionToContainClickedPosition(this.mc, maxDistance);
                            }
                            // Right click in Cuboid mode: Reset the area to the clicked position
                            else
                            {
                                sm.resetSelectionToClickedPosition(this.mc, maxDistance);
                            }
                        }
                    }
                    else if (mode == OperationMode.PLACEMENT)
                    {
                        dataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionToRayTrace(this.mc, maxDistance);
                    }

                    return true;
                }
                else if (isToolSelect)
                {
                    if (mode == OperationMode.AREA_SELECTION)
                    {
                        SelectionManager sm = dataManager.getSelectionManager();

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
                    else if (mode == OperationMode.PLACEMENT)
                    {
                        dataManager.getSchematicPlacementManager().changeSelection(this.mc.world, this.mc.player, maxDistance);
                    }

                    return true;
                }
            }

            if (mode == OperationMode.PLACEMENT && key == Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind())
            {
                SchematicPlacement schematicPlacement = dataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                if (schematicPlacement != null)
                {
                    Placement placement = schematicPlacement.getSelectedSubRegionPlacement();

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
            else if (mode == OperationMode.AREA_SELECTION && key == Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind())
            {
                SelectionManager sm = dataManager.getSelectionManager();
                AreaSelection sel = sm.getCurrentSelection();

                if (sel != null)
                {
                    String name = sel.getCurrentSubRegionBoxName();

                    if (name != null)
                    {
                        String title = "litematica.gui.title.rename_area_sub_region";
                        this.mc.displayGuiScreen(new GuiTextInput(128, title, name, null, new SelectedBoxRenamer(sm)));
                    }
                }

                return true;
            }
            else if (mode == OperationMode.AREA_SELECTION && key == Hotkeys.SAVE_AREA_AS_SCHEMATIC_TO_FILE.getKeybind())
            {
                DataManager.save();
                this.mc.displayGuiScreen(new GuiSchematicSave());
                return true;
            }
            else if (mode == OperationMode.AREA_SELECTION && key == Hotkeys.SAVE_AREA_AS_IN_MEMORY_SCHEMATIC.getKeybind())
            {
                DataManager.save();
                SelectionManager sm = dataManager.getSelectionManager();
                AreaSelection area = sm.getCurrentSelection();

                if (area != null)
                {
                    String title = "litematica.gui.title.create_in_memory_schematic";
                    this.mc.displayGuiScreen(new GuiTextInput(128, title, area.getName(), null, new InMemorySchematicCreator(area)));
                }

                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind())
            {
                this.mc.displayGuiScreen(new GuiMainMenu());
                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind())
            {
                this.mc.displayGuiScreen(new GuiAreaSelectionManager());
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
            if (action == KeyAction.PRESS)
            {
                if (key == Hotkeys.ADD_SELECTION_BOX.getKeybind())
                {
                    SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        selection.createNewSubRegionBox(pos);

                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        StringUtils.printActionbarMessage("litematica.message.added_selection_box", posStr);
                    }
                    return true;
                }
                else if (key == Hotkeys.DELETE_SELECTION_BOX.getKeybind())
                {
                    SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        String name = selection.getCurrentSubRegionBoxName();

                        if (name != null && selection.removeSelectedSubRegionBox())
                        {
                            StringUtils.printActionbarMessage("litematica.message.removed_selection_box", name);
                        }
                    }
                    return true;
                }
                else if (key == Hotkeys.MOVE_ENTIRE_SELECTION.getKeybind())
                {
                    SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        BlockPos old = selection.getOrigin();
                        selection.moveEntireSelectionTo(pos);
                        String oldStr = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        StringUtils.printActionbarMessage("litematica.message.moved_selection", oldStr, posStr);
                    }
                    return true;
                }
                else if (key == Hotkeys.SELECTION_MODE_CYCLE.getKeybind() && DataManager.getOperationMode() == OperationMode.AREA_SELECTION)
                {
                    Configs.Generic.SELECTION_MODE.setOptionListValue(Configs.Generic.SELECTION_MODE.getOptionListValue().cycle(false));
                    return true;
                }
                else if (key == Hotkeys.SET_AREA_ORIGIN.getKeybind())
                {
                    SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        selection.setOrigin(pos);
                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        StringUtils.printActionbarMessage("litematica.message.set_area_origin", posStr);
                    }
                    return true;
                }
                else if (key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() || key == Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind())
                {
                    SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
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
                    }
                    return true;
                }
                else if (key == Hotkeys.TOGGLE_ALL_RENDERING.getKeybind())
                {
                    boolean enabled = RenderEventHandler.getInstance().toggleAllRenderingEnabled();
                    String name = StringUtils.splitCamelCase(Hotkeys.TOGGLE_ALL_RENDERING.getName());
                    this.printToggleMessage(name, enabled);
                    return true;
                }
                else if (key == Hotkeys.TOGGLE_SELECTION_BOXES_RENDERING.getKeybind())
                {
                    boolean enabled = RenderEventHandler.getInstance().toggleRenderSelectionBoxes();
                    String name = StringUtils.splitCamelCase(Hotkeys.TOGGLE_SELECTION_BOXES_RENDERING.getName());
                    this.printToggleMessage(name, enabled);
                    return true;
                }
                else if (key == Hotkeys.TOGGLE_GHOST_BLOCK_RENDERING.getKeybind())
                {
                    boolean enabled = RenderEventHandler.getInstance().toggleRenderSchematics();
                    String name = StringUtils.splitCamelCase(Hotkeys.TOGGLE_GHOST_BLOCK_RENDERING.getName());
                    this.printToggleMessage(name, enabled);
                    return true;
                }
                else if (key == Hotkeys.TOGGLE_TRANSLUCENT_RENDERING.getKeybind())
                {
                    boolean enabled = ! Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
                    Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.setBooleanValue(enabled);
                    String name = StringUtils.splitCamelCase(Hotkeys.TOGGLE_TRANSLUCENT_RENDERING.getName());
                    this.printToggleMessage(name, enabled);
                    return true;
                }
                else if (key == Hotkeys.TOOL_ENABLED_TOGGLE.getKeybind())
                {
                    boolean enabled = ! Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();
                    Configs.Generic.TOOL_ITEM_ENABLED.setBooleanValue(enabled);
                    String name = StringUtils.splitCamelCase(Configs.Generic.TOOL_ITEM_ENABLED.getName());
                    this.printToggleMessage(name, enabled);
                    return true;
                }
            }

            return false;
        }

        protected void printToggleMessage(String name, boolean enabled)
        {
            // FIXME
            String pre = enabled ? TextFormatting.GREEN.toString() : TextFormatting.RED.toString();
            String status = I18n.format("litematica.message.value." + (enabled ? "on" : "off"));
            String message = I18n.format("litematica.message.toggled", name, pre + status + TextFormatting.RESET);
            StringUtils.printActionbarMessage(message);
        }
    }
}
