package fi.dy.masa.litematica.config;

import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.world.SchematicWorldRenderingNotifier;
import fi.dy.masa.malilib.config.ValueChangeCallback;
import fi.dy.masa.malilib.config.option.BooleanConfig;
import fi.dy.masa.malilib.input.ActionResult;
import fi.dy.masa.malilib.input.KeyAction;
import fi.dy.masa.malilib.input.KeyBind;
import fi.dy.masa.malilib.input.callback.HotkeyCallback;
import fi.dy.masa.malilib.input.callback.ToggleBooleanWithMessageKeyCallback;

public class HotkeyCallbacks
{
    public static void init(Minecraft mc)
    {
        HotkeyCallback hotkeyCallbackMisc = new HotkeyCallbackMisc(mc);
        HotkeyCallback hotkeyCallbackOpenGui = new HotkeyCallbackOpenGui(mc);
        HotkeyCallback hotkeyCallbackToolActions = new HotkeyCallbackToolActions(mc);

        Configs.Generic.PICK_BLOCKABLE_SLOTS.setValueChangeCallback((newValue, oldValue) -> InventoryUtils.setPickBlockableSlots(newValue));
        Configs.Generic.PICK_BLOCKABLE_SLOTS.setValueLoadCallback(InventoryUtils::setPickBlockableSlots);
        Configs.Generic.TOOL_ITEM.setValueChangeCallback((newValue, oldValue) -> DataManager.setToolItem(newValue));
        Configs.Generic.TOOL_ITEM.setValueLoadCallback(DataManager::setToolItem);

        Hotkeys.OPEN_GUI_AREA_SETTINGS.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_LOAD_SCHEMATICS.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_LOADED_SCHEMATICS.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_MAIN_MENU.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_PLACEMENT_GRID_SETTINGS.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_SCHEMATIC_PLACEMENTS.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_SCHEMATIC_PROJECTS.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_SCHEMATIC_VERIFIER.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeyBind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_SETTINGS.getKeyBind().setCallback(hotkeyCallbackOpenGui);

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
        Hotkeys.RERENDER_SCHEMATIC.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.ROTATE_PLACEMENT_CW.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.ROTATE_PLACEMENT_CCW.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SAVE_AREA_AS_IN_MEMORY_SCHEMATIC.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SAVE_AREA_AS_SCHEMATIC_TO_FILE.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SCHEMATIC_REBUILD_ACCEPT_REPLACEMENT.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SCHEMATIC_VERSION_CYCLE_NEXT.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SCHEMATIC_VERSION_CYCLE_PREVIOUS.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SELECTION_GROW_HOTKEY.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SELECTION_MODE_CYCLE.getKeyBind().setCallback(hotkeyCallbackMisc);
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
        Hotkeys.UNLOAD_CURRENT_SCHEMATIC.getKeyBind().setCallback(hotkeyCallbackMisc);
        Hotkeys.UPDATE_BLOCKS.getKeyBind().setCallback(hotkeyCallbackMisc);

        Hotkeys.TOGGLE_ALL_RENDERING.getKeyBind().setCallback(new RenderToggle(Configs.Visuals.ENABLE_RENDERING));
        Hotkeys.TOGGLE_SCHEMATIC_RENDERING.getKeyBind().setCallback(new RenderToggle(Configs.Visuals.ENABLE_SCHEMATIC_RENDERING));
        Hotkeys.TOGGLE_OVERLAY_RENDERING.getKeyBind().setCallback(new RenderToggle(Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY));
        Hotkeys.TOGGLE_OVERLAY_OUTLINE_RENDERING.getKeyBind().setCallback(new RenderToggle(Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES));
        Hotkeys.TOGGLE_OVERLAY_SIDE_RENDERING.getKeyBind().setCallback(new RenderToggle(Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES));
        Hotkeys.TOGGLE_TRANSLUCENT_RENDERING.getKeyBind().setCallback(new RenderToggle(Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT));

        Hotkeys.EASY_PLACE_TOGGLE.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.Generic.EASY_PLACE_MODE));
        Hotkeys.TOGGLE_AREA_SELECTION_RENDERING.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING));
        Hotkeys.TOGGLE_INFO_OVERLAY_RENDERING.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ENABLED));
        Hotkeys.PICK_BLOCK_TOGGLE.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.Generic.PICK_BLOCK_ENABLED));
        Hotkeys.PICK_BLOCK_TOGGLE_AUTO.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.Generic.PICK_BLOCK_AUTO));
        Hotkeys.TOGGLE_PLACEMENT_BOXES_RENDERING.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.Visuals.ENABLE_PLACEMENT_BOXES_RENDERING));
        Hotkeys.TOGGLE_PLACEMENT_RESTRICTION.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.Generic.PLACEMENT_RESTRICTION));
        Hotkeys.TOGGLE_SCHEMATIC_BLOCK_RENDERING.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS));
        Hotkeys.TOGGLE_SIGN_TEXT_PASTE.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.Generic.SIGN_TEXT_PASTE));
        Hotkeys.TOGGLE_VERIFIER_OVERLAY_RENDERING.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED));
        Hotkeys.TOOL_ENABLED_TOGGLE.getKeyBind().setCallback(new ToggleBooleanWithMessageKeyCallback(Configs.Generic.TOOL_ITEM_ENABLED));

        assignRendererRefreshCallbacks();
    }

    private static void assignRendererRefreshCallbacks()
    {
        ValueChangeCallback<Boolean> callbackBoolean = (newValue, oldValue) -> HotkeyCallbacks.refreshRenderer();
        ValueChangeCallback<Double> callbackDouble = (newValue, oldValue) -> HotkeyCallbacks.refreshRenderer();

        Configs.Visuals.ENABLE_RENDERING.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.IGNORE_EXISTING_FLUIDS.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_EXTRA.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_MISSING.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK.setValueChangeCallback(callbackBoolean);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_STATE.setValueChangeCallback(callbackBoolean);

        Configs.Visuals.GHOST_BLOCK_ALPHA.setValueChangeCallback(callbackDouble);
        Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.setValueChangeCallback(callbackDouble);
        Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.setValueChangeCallback(callbackDouble);
    }

    private static void refreshRenderer()
    {
        SchematicWorldRenderingNotifier.INSTANCE.updateAll();
    }

    private static class RenderToggle extends ToggleBooleanWithMessageKeyCallback
    {
        public RenderToggle(BooleanConfig config)
        {
            super(config);
        }

        @Override
        public ActionResult onKeyAction(KeyAction action, KeyBind key)
        {
            super.onKeyAction(action, key);

            if (this.config.getBooleanValue())
            {
                refreshRenderer();
            }

            return ActionResult.SUCCESS;
        }
    }
}
