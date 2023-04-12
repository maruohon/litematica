package litematica.config;

import malilib.input.callback.HotkeyCallback;
import malilib.input.callback.ToggleBooleanWithMessageKeyCallback;
import malilib.listener.EventListener;
import litematica.data.DataManager;
import litematica.util.PickBlockUtils;
import litematica.world.SchematicWorldRenderingNotifier;

public class HotkeyCallbacks
{
    public static void init()
    {
        HotkeyCallback hotkeyCallbackMisc = new HotkeyCallbackMisc();
        HotkeyCallback hotkeyCallbackOpenGui = new HotkeyCallbackOpenGui();
        HotkeyCallback hotkeyCallbackToolActions = new HotkeyCallbackToolActions();

        Configs.Generic.DATE_FORMAT.setValueChangeCallback((n, o) -> DataManager.checkDateFormat(n));
        Configs.Generic.PICK_BLOCK_USABLE_SLOTS.setValueChangeCallback((newValue, oldValue) -> PickBlockUtils.setPickBlockUsableSlots(newValue));
        Configs.Generic.PICK_BLOCK_USABLE_SLOTS.setValueLoadCallback(PickBlockUtils::setPickBlockUsableSlots);
        Configs.Generic.TOOL_ITEM.setValueChangeCallback((newValue, oldValue) -> DataManager.setToolItem(newValue));
        Configs.Generic.TOOL_ITEM.setValueLoadCallback(DataManager::setToolItem);

        Hotkeys.OPEN_AREA_EDITOR_SCREEN.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_LOAD_SCHEMATICS_SCREEN.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_LOADED_SCHEMATICS_SCREEN.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_MAIN_MENU.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_MATERIAL_LIST_SCREEN.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_PLACEMENT_GRID_SETTINGS.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_PLACEMENT_SETTINGS_SCREEN.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_PLACEMENTS_LIST_SCREEN.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_SCHEMATIC_VCS_SCREEN.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_SCHEMATIC_VERIFIER_SCREEN.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_AREA_SELECTION_BROWSER.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_CONFIG_SCREEN.getKeyBind().setCallback(hotkeyCallbackOpenGui);

        Hotkeys.TOOL_PLACE_CORNER_1.getKeyBind().setCallback(hotkeyCallbackToolActions);
        Hotkeys.TOOL_PLACE_CORNER_2.getKeyBind().setCallback(hotkeyCallbackToolActions);
        Hotkeys.TOOL_SELECT_ELEMENTS.getKeyBind().setCallback(hotkeyCallbackToolActions);

        Hotkeys.ADD_SELECTION_BOX.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.CLONE_SELECTION.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.DELETE_SELECTION_BOX.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.DUPLICATE_PLACEMENT.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.EXECUTE_OPERATION.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.LAYER_MODE_NEXT.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.LAYER_MODE_PREVIOUS.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.LAYER_NEXT.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.LAYER_PREVIOUS.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.LAYER_SET_HERE.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.MOVE_ENTIRE_SELECTION.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.NUDGE_SELECTION_NEGATIVE.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.NUDGE_SELECTION_POSITIVE.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.PICK_BLOCK_FIRST.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.PICK_BLOCK_LAST.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.REMOVE_SELECTED_PLACEMENT.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.REFRESH_SCHEMATIC_RENDERER.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.ROTATE_PLACEMENT_CW.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.ROTATE_PLACEMENT_CCW.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.CREATE_SCHEMATIC_IN_MEMORY.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SAVE_SCHEMATIC_TO_FILE.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SCHEMATIC_EDIT_ACCEPT_REPLACEMENT.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SCHEMATIC_VERSION_CYCLE_NEXT.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SCHEMATIC_VERSION_CYCLE_PREVIOUS.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SELECTION_GROW_HOTKEY.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SUB_MODE_CYCLE.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SELECTION_SHRINK.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SET_AREA_ORIGIN.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SET_HELD_ITEM_AS_TOOL.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SET_SCHEMATIC_PREVIEW.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.TOOL_MODE_CYCLE_BACKWARD.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.TOOL_MODE_CYCLE_FORWARD.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_1.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_2.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.TRANSLUCENT_SCHEMATIC_RENDERING.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.Visuals.TRANSLUCENT_SCHEMATIC_RENDERING));
        Hotkeys.UNLOAD_CURRENT_SCHEMATIC.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.UPDATE_BLOCKS.getKeyBind().setCallback(hotkeyCallbackMisc);

        assignRendererRefreshCallbacks();
    }

    private static void assignRendererRefreshCallbacks()
    {
        EventListener refreshCallback = HotkeyCallbacks::refreshRenderer;

        Configs.Visuals.IGNORE_EXISTING_FLUIDS.addValueChangeListener(refreshCallback);
        Configs.Visuals.MAIN_RENDERING_TOGGLE.addValueChangeListener(refreshCallback);
        Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.addValueChangeListener(refreshCallback);
        Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.addValueChangeListener(refreshCallback);
        Configs.Visuals.TRANSLUCENT_INNER_SIDES.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_BLOCKS_RENDERING.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINES.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_SIDES.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_EXTRA.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_MISSING.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_STATE.addValueChangeListener(refreshCallback);
        Configs.Visuals.SCHEMATIC_RENDERING.addValueChangeListener(refreshCallback);
        Configs.Visuals.TRANSLUCENT_SCHEMATIC_RENDERING.addValueChangeListener(refreshCallback);
    }

    private static void refreshRenderer()
    {
        SchematicWorldRenderingNotifier.INSTANCE.updateAll();
    }
}
