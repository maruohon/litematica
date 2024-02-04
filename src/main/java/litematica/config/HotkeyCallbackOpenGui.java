package litematica.config;

import malilib.gui.BaseScreen;
import malilib.gui.config.BaseConfigScreen;
import malilib.input.ActionResult;
import malilib.input.KeyAction;
import malilib.input.KeyBind;
import malilib.input.callback.HotkeyCallback;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.game.wrap.GameWrap;
import litematica.Reference;
import litematica.data.DataManager;
import litematica.gui.AreaSelectionBrowserScreen;
import litematica.gui.ConfigScreen;
import litematica.gui.LoadedSchematicsListScreen;
import litematica.gui.MainMenuScreen;
import litematica.gui.MaterialListScreen;
import litematica.gui.PlacementGridSettingsScreen;
import litematica.gui.RenderLayerEditScreen;
import litematica.gui.SchematicBrowserScreen;
import litematica.gui.SchematicPlacementSettingsScreen;
import litematica.gui.SchematicPlacementSubRegionSettingsScreen;
import litematica.gui.SchematicPlacementsListScreen;
import litematica.gui.SchematicVerifierScreen;
import litematica.materials.MaterialListBase;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SubRegionPlacement;
import litematica.schematic.verifier.SchematicVerifier;
import litematica.schematic.verifier.SchematicVerifierManager;
import litematica.selection.AreaSelectionManager;

public class HotkeyCallbackOpenGui implements HotkeyCallback
{
    @Override
    public ActionResult onKeyAction(KeyAction action, KeyBind key)
    {
        if (GameWrap.getClientPlayer() == null || GameWrap.getClientWorld() == null)
        {
            return ActionResult.FAIL;
        }

        if (key == Hotkeys.OPEN_MAIN_MENU.getKeyBind())
        {
            BaseScreen.openScreen(new MainMenuScreen());
        }
        else if (key == Hotkeys.OPEN_CONFIG_SCREEN.getKeyBind())
        {
            // TODO Shouldn't this be handled by the tabbed screen now?
            BaseScreen screen = BaseConfigScreen.getCurrentTab(Reference.MOD_ID) == ConfigScreen.RENDER_LAYERS ? new RenderLayerEditScreen() : ConfigScreen.create();
            BaseScreen.openScreen(screen);
            return ActionResult.SUCCESS;
        }

        else if (key == Hotkeys.OPEN_AREA_EDITOR_SCREEN.getKeyBind())
        {
            AreaSelectionManager manager = DataManager.getAreaSelectionManager();

            if (manager.getCurrentSelection() != null)
            {
                manager.openEditGui(null);
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.no_area_selected");
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
                    MessageDispatcher.error("litematica.message.error.no_placement_selected");
                }
            }

            if (materialList != null)
            {
                BaseScreen.openScreen(new MaterialListScreen(materialList));
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
                    MessageDispatcher.error("litematica.message.error.placement_grid_settings.open_gui_selected_is_grid");
                }
            }
        }
        else if (key == Hotkeys.OPEN_PLACEMENT_SETTINGS_SCREEN.getKeyBind())
        {
            SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement == null)
            {
                MessageDispatcher.error("litematica.message.error.no_placement_selected");
                return ActionResult.FAIL;
            }

            if (schematicPlacement.isSchematicLoaded() == false)
            {
                MessageDispatcher.error("litematica.message.error.schematic_placement_configure_schematic_not_loaded",
                                        schematicPlacement.getName());
                return ActionResult.FAIL;
            }

            SubRegionPlacement subRegionPlacement = schematicPlacement.getSelectedSubRegionPlacement();

            if (subRegionPlacement != null)
            {
                BaseScreen.openScreen(new SchematicPlacementSubRegionSettingsScreen(schematicPlacement, subRegionPlacement));
            }
            else
            {
                BaseScreen.openScreen(new SchematicPlacementSettingsScreen(schematicPlacement));
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
            SchematicVerifier verifier = SchematicVerifierManager.INSTANCE.getSelectedVerifierOrCreateForPlacement();

            if (verifier != null)
            {
                BaseScreen.openScreen(new SchematicVerifierScreen(verifier));
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.schematic_verifier.no_placement_selected");
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
                MessageDispatcher.warning("litematica.hover.button.main_menu.area_browser_in_vcs_mode");
            }
        }

        return ActionResult.SUCCESS;
    }
}
