package fi.dy.masa.litematica.config;

import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.AreaSelectionBrowserScreen;
import fi.dy.masa.litematica.gui.ConfigScreen;
import fi.dy.masa.litematica.gui.LoadedSchematicsListScreen;
import fi.dy.masa.litematica.gui.MainMenuScreen;
import fi.dy.masa.litematica.gui.PlacementGridSettingsScreen;
import fi.dy.masa.litematica.gui.RenderLayerEditScreen;
import fi.dy.masa.litematica.gui.SchematicBrowserScreen;
import fi.dy.masa.litematica.gui.SchematicPlacementSettingsScreen;
import fi.dy.masa.litematica.gui.SchematicPlacementSubRegionSettingsScreen;
import fi.dy.masa.litematica.gui.SchematicPlacementsListScreen;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.config.BaseConfigScreen;
import fi.dy.masa.malilib.input.ActionResult;
import fi.dy.masa.malilib.input.KeyAction;
import fi.dy.masa.malilib.input.KeyBind;
import fi.dy.masa.malilib.input.callback.HotkeyCallback;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;

public class HotkeyCallbackOpenGui implements HotkeyCallback
{
    private final Minecraft mc;

    public HotkeyCallbackOpenGui(Minecraft mc)
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

        if (key == Hotkeys.OPEN_MAIN_MENU.getKeyBind())
        {
            BaseScreen.openScreen(new MainMenuScreen());
        }
        else if (key == Hotkeys.OPEN_CONFIG_SCREEN.getKeyBind())
        {
            BaseScreen screen = BaseConfigScreen.getCurrentTab(Reference.MOD_ID) == ConfigScreen.RENDER_LAYERS ? new RenderLayerEditScreen() : ConfigScreen.create();
            BaseScreen.openScreen(screen);
            return ActionResult.SUCCESS;
        }

        else if (key == Hotkeys.OPEN_AREA_EDITOR_SCREEN.getKeyBind())
        {
            SelectionManager manager = DataManager.getSelectionManager();

            if (manager.getCurrentSelection() != null)
            {
                manager.openEditGui(null);
            }
            else
            {
                MessageDispatcher.error().translate("litematica.message.error.no_area_selected");
            }
        }
        else if (key == Hotkeys.OPEN_LOAD_SCHEMATICS_SCREEN.getKeyBind())
        {
            BaseScreen.openScreen(new SchematicBrowserScreen());
        }
        else if (key == Hotkeys.OPEN_LOADED_SCHEMATICS_SCREEN.getKeyBind())
        {
            BaseScreen.openScreen(new LoadedSchematicsListScreen());
        }
        else if (key == Hotkeys.OPEN_MATERIAL_LIST_SCREEN.getKeyBind())
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
                    MessageDispatcher.error().translate("litematica.message.error.no_placement_selected");
                }
            }

            if (materialList != null)
            {
                // TODO FIXME malilib refactor
                //BaseScreen.openScreen(new GuiMaterialList(materialList));
            }
        }
        else if (key == Hotkeys.OPEN_PLACEMENT_GRID_SETTINGS.getKeyBind())
        {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (placement != null)
            {
                if (placement.isRepeatedPlacement() == false)
                {
                    BaseScreen.openScreen(new PlacementGridSettingsScreen(placement));
                }
                else
                {
                    MessageDispatcher.error().translate("litematica.message.error.placement_grid_settings.open_gui_selected_is_grid");
                }
            }
        }
        else if (key == Hotkeys.OPEN_PLACEMENT_SETTINGS_SCREEN.getKeyBind())
        {
            SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

                if (placement != null)
                {
                    BaseScreen.openScreen(new SchematicPlacementSubRegionSettingsScreen(schematicPlacement, placement));
                }
                else
                {
                    BaseScreen.openScreen(new SchematicPlacementSettingsScreen(schematicPlacement));
                }
            }
            else
            {
                MessageDispatcher.error().translate("litematica.message.error.no_placement_selected");
            }
        }
        else if (key == Hotkeys.OPEN_PLACEMENTS_LIST_SCREEN.getKeyBind())
        {
            BaseScreen.openScreen(new SchematicPlacementsListScreen());
        }
        else if (key == Hotkeys.OPEN_SCHEMATIC_VCS_SCREEN.getKeyBind())
        {
            MainMenuScreen.openSchematicProjectsScreen();
        }
        else if (key == Hotkeys.OPEN_SCHEMATIC_VERIFIER_SCREEN.getKeyBind())
        {
            SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                // TODO FIXME malilib refactor
                //BaseScreen.openScreen(new GuiSchematicVerifier(schematicPlacement));
            }
            else
            {
                MessageDispatcher.error().translate("litematica.message.error.no_placement_selected");
            }
        }
        else if (key == Hotkeys.OPEN_AREA_SELECTION_BROWSER.getKeyBind())
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen() == false)
            {
                BaseScreen.openScreen(new AreaSelectionBrowserScreen());
            }
            else
            {
                MessageDispatcher.warning().translate("litematica.hover.button.main_menu.area_browser_in_vcs_mode");
            }
        }

        return ActionResult.SUCCESS;
    }
}
