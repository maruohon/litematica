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
import fi.dy.masa.malilib.input.IHotkeyCallback;
import fi.dy.masa.malilib.input.IKeyBind;
import fi.dy.masa.malilib.input.KeyAction;
import fi.dy.masa.malilib.message.MessageType;
import fi.dy.masa.malilib.message.MessageUtils;

public class HotkeyCallbackOpenGui implements IHotkeyCallback
{
    private final Minecraft mc;

    public HotkeyCallbackOpenGui(Minecraft mc)
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

        if (key == Hotkeys.OPEN_GUI_MAIN_MENU.getKeyBind())
        {
            GuiBase.openGui(new GuiMainMenu());
        }
        else if (key == Hotkeys.OPEN_GUI_SETTINGS.getKeyBind())
        {
            GuiBase.openGui(new GuiConfigs());
        }

        else if (key == Hotkeys.OPEN_GUI_AREA_SETTINGS.getKeyBind())
        {
            SelectionManager manager = DataManager.getSelectionManager();

            if (manager.getCurrentSelection() != null)
            {
                manager.openEditGui(null);
            }
            else
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
            }
        }
        else if (key == Hotkeys.OPEN_GUI_LOAD_SCHEMATICS.getKeyBind())
        {
            GuiBase.openGui(new GuiSchematicLoad());
        }
        else if (key == Hotkeys.OPEN_GUI_LOADED_SCHEMATICS.getKeyBind())
        {
            GuiBase.openGui(new GuiSchematicLoadedList());
        }
        else if (key == Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeyBind())
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
                    MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                }
            }

            if (materialList != null)
            {
                GuiBase.openGui(new GuiMaterialList(materialList));
            }
        }
        else if (key == Hotkeys.OPEN_GUI_PLACEMENT_GRID_SETTINGS.getKeyBind())
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
                    MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.placement_grid_settings.open_gui_selected_is_grid");
                }
            }
        }
        else if (key == Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeyBind())
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
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
            }
        }
        else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_PLACEMENTS.getKeyBind())
        {
            GuiBase.openGui(new GuiSchematicPlacementsList());
        }
        else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_PROJECTS.getKeyBind())
        {
            DataManager.getSchematicProjectsManager().openSchematicProjectsGui();
        }
        else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_VERIFIER.getKeyBind())
        {
            SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                GuiBase.openGui(new GuiSchematicVerifier(schematicPlacement));
            }
            else
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
            }
        }
        else if (key == Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeyBind())
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen() == false)
            {
                GuiBase.openGui(new GuiAreaSelectionManager());
            }
            else
            {
                MessageUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.gui.button.hover.schematic_projects.area_browser_disabled_currently_in_projects_mode");
            }
        }

        return true;
    }
}
