package fi.dy.masa.litematica.config;

import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiAreaSelectionManager;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.GuiPlacementGridSettings;
import fi.dy.masa.litematica.gui.GuiSchematicLoad;
import fi.dy.masa.litematica.gui.GuiSchematicLoadedList;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSubRegionConfiguration;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.util.InfoUtils;

public class HotkeyCallbackOpenGui implements IHotkeyCallback
{
    private final Minecraft mc;

    public HotkeyCallbackOpenGui(Minecraft mc)
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

        if (key == Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind())
        {
            GuiBase.openGui(new GuiMainMenu());
        }
        else if (key == Hotkeys.OPEN_GUI_SETTINGS.getKeybind())
        {
            GuiBase.openGui(new GuiConfigs());
        }

        else if (key == Hotkeys.OPEN_GUI_AREA_SETTINGS.getKeybind())
        {
            SelectionManager manager = DataManager.getSelectionManager();

            if (manager.getCurrentSelection() != null)
            {
                manager.openEditGui(null);
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
            }
        }
        else if (key == Hotkeys.OPEN_GUI_LOAD_SCHEMATICS.getKeybind())
        {
            GuiBase.openGui(new GuiSchematicLoad());
        }
        else if (key == Hotkeys.OPEN_GUI_LOADED_SCHEMATICS.getKeybind())
        {
            GuiBase.openGui(new GuiSchematicLoadedList());
        }
        else if (key == Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind())
        {
            MaterialListBase materialList = DataManager.getMaterialList();

            // No last-viewed material list currently stored, try to get one for the currently selected placement, if any
            if (materialList == null)
            {
                SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                if (schematicPlacement != null)
                {
                    materialList = schematicPlacement.getMaterialList();
                    materialList.reCreateMaterialList();
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                }
            }

            if (materialList != null)
            {
                GuiBase.openGui(new GuiMaterialList(materialList));
            }
        }
        else if (key == Hotkeys.OPEN_GUI_PLACEMENT_GRID_SETTINGS.getKeybind())
        {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (placement != null)
            {
                if (placement.isRepeatedPlacement() == false)
                {
                    GuiBase.openGui(new GuiPlacementGridSettings(placement, null));
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.placement_grid_settings.open_gui_selected_is_grid");
                }
            }
        }
        else if (key == Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind())
        {
            SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

                if (placement != null)
                {
                    GuiBase.openGui(new GuiSubRegionConfiguration(schematicPlacement, placement));
                }
                else
                {
                    GuiBase.openGui(new GuiPlacementConfiguration(schematicPlacement));
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
            }
        }
        else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_PLACEMENTS.getKeybind())
        {
            GuiBase.openGui(new GuiSchematicPlacementsList());
        }
        else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_PROJECTS.getKeybind())
        {
            DataManager.getSchematicProjectsManager().openSchematicProjectsGui();
        }
        else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_VERIFIER.getKeybind())
        {
            SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                GuiBase.openGui(new GuiSchematicVerifier(schematicPlacement));
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
            }
        }
        else if (key == Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind())
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen() == false)
            {
                GuiBase.openGui(new GuiAreaSelectionManager());
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.gui.button.hover.schematic_projects.area_browser_disabled_currently_in_projects_mode");
            }
        }

        return true;
    }
}
