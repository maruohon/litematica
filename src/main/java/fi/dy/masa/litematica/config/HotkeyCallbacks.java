package fi.dy.masa.litematica.config;

import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.world.SchematicWorldRenderingNotifier;
import fi.dy.masa.malilib.config.options.IConfigBoolean;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;

public class HotkeyCallbacks
{
    public static void init(Minecraft mc)
    {
        IHotkeyCallback hotkeyCallbackMisc = new HotkeyCallbackMisc(mc);
        IHotkeyCallback hotkeyCallbackOpenGui = new HotkeyCallbackOpenGui(mc);
        IHotkeyCallback hotkeyCallbackToolActions = new HotkeyCallbackToolActions(mc);

        Configs.Generic.PICK_BLOCKABLE_SLOTS.setValueChangeCallback((newValue, oldValue) -> { InventoryUtils.setPickBlockableSlots(newValue); });
        Configs.Generic.TOOL_ITEM.setValueChangeCallback((newValue, oldValue) -> { DataManager.setToolItem(newValue); });

        Hotkeys.OPEN_GUI_AREA_SETTINGS.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_LOAD_SCHEMATICS.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_LOADED_SCHEMATICS.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_PLACEMENT_GRID_SETTINGS.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_SCHEMATIC_PLACEMENTS.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_SCHEMATIC_PROJECTS.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_SCHEMATIC_VERIFIER.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind().setCallback(hotkeyCallbackOpenGui);
        Hotkeys.OPEN_GUI_SETTINGS.getKeybind().setCallback(hotkeyCallbackOpenGui);

        Hotkeys.TOOL_PLACE_CORNER_1.getKeybind().setCallback(hotkeyCallbackToolActions);
        Hotkeys.TOOL_PLACE_CORNER_2.getKeybind().setCallback(hotkeyCallbackToolActions);
        Hotkeys.TOOL_SELECT_ELEMENTS.getKeybind().setCallback(hotkeyCallbackToolActions);

        Hotkeys.ADD_SELECTION_BOX.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.CLONE_SELECTION.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.DELETE_SELECTION_BOX.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.EXECUTE_OPERATION.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.LAYER_MODE_NEXT.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.LAYER_MODE_PREVIOUS.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.LAYER_NEXT.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.LAYER_PREVIOUS.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.LAYER_SET_HERE.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.MOVE_ENTIRE_SELECTION.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.NUDGE_SELECTION_NEGATIVE.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.NUDGE_SELECTION_POSITIVE.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.PICK_BLOCK_FIRST.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.PICK_BLOCK_LAST.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.RERENDER_SCHEMATIC.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.ROTATE_PLACEMENT_CW.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.ROTATE_PLACEMENT_CCW.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SAVE_AREA_AS_IN_MEMORY_SCHEMATIC.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SAVE_AREA_AS_SCHEMATIC_TO_FILE.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SCHEMATIC_REBUILD_ACCEPT_REPLACEMENT.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SCHEMATIC_VERSION_CYCLE_NEXT.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SCHEMATIC_VERSION_CYCLE_PREVIOUS.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SELECTION_GROW_HOTKEY.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SELECTION_MODE_CYCLE.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SELECTION_SHRINK.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SET_AREA_ORIGIN.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SET_SCHEMATIC_PREVIEW.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.TOOL_MODE_CYCLE_BACKWARD.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.TOOL_MODE_CYCLE_FORWARD.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_1.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_2.getKeybind().setCallback(hotkeyCallbackMisc);
        Hotkeys.UNLOAD_CURRENT_SCHEMATIC.getKeybind().setCallback(hotkeyCallbackMisc);

        Hotkeys.TOGGLE_ALL_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.ENABLE_RENDERING));
        Hotkeys.TOGGLE_SCHEMATIC_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.ENABLE_SCHEMATIC_RENDERING));
        Hotkeys.TOGGLE_OVERLAY_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY));
        Hotkeys.TOGGLE_OVERLAY_OUTLINE_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES));
        Hotkeys.TOGGLE_OVERLAY_SIDE_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES));
        Hotkeys.TOGGLE_TRANSLUCENT_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT));

        Hotkeys.EASY_PLACE_TOGGLE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.EASY_PLACE_MODE));
        Hotkeys.TOGGLE_AREA_SELECTION_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING));
        Hotkeys.TOGGLE_INFO_OVERLAY_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ENABLED));
        Hotkeys.PICK_BLOCK_TOGGLE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.PICK_BLOCK_ENABLED));
        Hotkeys.PICK_BLOCK_TOGGLE_AUTO.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.PICK_BLOCK_AUTO));
        Hotkeys.TOGGLE_PLACEMENT_BOXES_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.ENABLE_PLACEMENT_BOXES_RENDERING));
        Hotkeys.TOGGLE_PLACEMENT_RESTRICTION.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.PLACEMENT_RESTRICTION));
        Hotkeys.TOGGLE_SCHEMATIC_BLOCK_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS));
        Hotkeys.TOGGLE_SIGN_TEXT_PASTE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.SIGN_TEXT_PASTE));
        Hotkeys.TOGGLE_VERIFIER_OVERLAY_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED));
        Hotkeys.TOOL_ENABLED_TOGGLE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.TOOL_ITEM_ENABLED));

        assignRendererRefreshCallbacks();
    }

    private static void assignRendererRefreshCallbacks()
    {
        IValueChangeCallback<Boolean> callbackBoolean = (newValue, oldValue) -> HotkeyCallbacks.refreshRenderer();
        IValueChangeCallback<Double> callbackDouble = (newValue, oldValue) -> HotkeyCallbacks.refreshRenderer();

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

    private static class RenderToggle extends KeyCallbackToggleBooleanConfigWithMessage
    {
        public RenderToggle(IConfigBoolean config)
        {
            super(config);
        }

        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            super.onKeyAction(action, key);

            if (this.config.getBooleanValue())
            {
                refreshRenderer();
            }

            return true;
        }
    }
}
