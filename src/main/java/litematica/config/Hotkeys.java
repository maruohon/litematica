package litematica.config;

import java.util.List;
import com.google.common.collect.ImmutableList;

import malilib.config.option.HotkeyConfig;
import malilib.input.KeyBindSettings;

public class Hotkeys
{
    public static final HotkeyConfig ADD_SELECTION_BOX                      = new HotkeyConfig("addSelectionBox",                   "");
    public static final HotkeyConfig CLONE_SELECTION                        = new HotkeyConfig("cloneSelection",                    "");
    public static final HotkeyConfig CREATE_SCHEMATIC_IN_MEMORY             = new HotkeyConfig("createSchematicInMemory",           "");
    public static final HotkeyConfig DELETE_SELECTION_BOX                   = new HotkeyConfig("deleteSelectionBox",                "");
    public static final HotkeyConfig DUPLICATE_PLACEMENT                    = new HotkeyConfig("duplicatePlacement",                "");
    public static final HotkeyConfig EASY_PLACE_ACTIVATION                  = new HotkeyConfig("easyPlaceActivation",               "", KeyBindSettings.INGAME_MODIFIER_EMPTY);
    public static final HotkeyConfig EXECUTE_OPERATION                      = new HotkeyConfig("executeOperation",                  "");
    public static final HotkeyConfig INVERT_OVERLAY_RENDER_STATE            = new HotkeyConfig("invertOverlayRenderState",          "", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig INVERT_SCHEMATIC_RENDER_STATE          = new HotkeyConfig("invertSchematicRenderState",        "", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig LAYER_MODE_NEXT                        = new HotkeyConfig("layerModeNext",                     "M,PAGE_UP");
    public static final HotkeyConfig LAYER_MODE_PREVIOUS                    = new HotkeyConfig("layerModePrevious",                 "M,PAGE_DOWN");
    public static final HotkeyConfig LAYER_NEXT                             = new HotkeyConfig("layerNext",                         "PAGE_UP");
    public static final HotkeyConfig LAYER_PREVIOUS                         = new HotkeyConfig("layerPrevious",                     "PAGE_DOWN");
    public static final HotkeyConfig LAYER_SET_HERE                         = new HotkeyConfig("layerSetHere",                      "");
    public static final HotkeyConfig NUDGE_SELECTION_NEGATIVE               = new HotkeyConfig("nudgeSelectionNegative",            "");
    public static final HotkeyConfig NUDGE_SELECTION_POSITIVE               = new HotkeyConfig("nudgeSelectionPositive",            "");
    public static final HotkeyConfig MOVE_ENTIRE_SELECTION                  = new HotkeyConfig("moveEntireSelection",               "");
    public static final HotkeyConfig OPEN_AREA_EDITOR_SCREEN                = new HotkeyConfig("openAreaEditorScreen",              "MULTIPLY");
    public static final HotkeyConfig OPEN_AREA_SELECTION_BROWSER            = new HotkeyConfig("openAreaSelectionBrowserScreen",    "M,S");
    public static final HotkeyConfig OPEN_CONFIG_SCREEN                     = new HotkeyConfig("openConfigScreen",                  "M,C");
    public static final HotkeyConfig OPEN_LOAD_SCHEMATICS_SCREEN            = new HotkeyConfig("openLoadSchematicsScreen",          "");
    public static final HotkeyConfig OPEN_LOADED_SCHEMATICS_SCREEN          = new HotkeyConfig("openLoadedSchematicsListScreen",    "");
    public static final HotkeyConfig OPEN_MAIN_MENU                         = new HotkeyConfig("openMainMenuScreen",                "M", KeyBindSettings.INGAME_RELEASE_EXCLUSIVE);
    public static final HotkeyConfig OPEN_MATERIAL_LIST_SCREEN              = new HotkeyConfig("openMaterialListScreen",            "M,L");
    public static final HotkeyConfig OPEN_PLACEMENT_GRID_SETTINGS           = new HotkeyConfig("openPlacementGridSettingsScreen",   "");
    public static final HotkeyConfig OPEN_PLACEMENT_SETTINGS_SCREEN         = new HotkeyConfig("openPlacementSettingsScreen",       "SUBTRACT");
    public static final HotkeyConfig OPEN_PLACEMENTS_LIST_SCREEN            = new HotkeyConfig("openPlacementsListScreen",          "M,P");
    public static final HotkeyConfig OPEN_SCHEMATIC_VCS_SCREEN              = new HotkeyConfig("openSchematicVCSScreen",            "");
    public static final HotkeyConfig OPEN_SCHEMATIC_VERIFIER_SCREEN         = new HotkeyConfig("openSchematicVerifierScreen",       "M,V");
    public static final HotkeyConfig OPERATION_MODE_CHANGE_MODIFIER         = new HotkeyConfig("operationModeChangeModifier",       "L_CTRL", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig PICK_BLOCK_FIRST                       = new HotkeyConfig("pickBlockFirst",                    "MIDDLE_MOUSE", KeyBindSettings.INGAME_EXTRA);
    public static final HotkeyConfig PICK_BLOCK_LAST                        = new HotkeyConfig("pickBlockLast",                     "", KeyBindSettings.builder().extra().noCancel().build());
    public static final HotkeyConfig REMOVE_SELECTED_PLACEMENT              = new HotkeyConfig("removeSelectedPlacement",           "");
    public static final HotkeyConfig RENDER_BLOCK_INFO_OVERLAY              = new HotkeyConfig("renderBlockInfoOverlay",            "I", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig RENDER_OVERLAY_THROUGH_BLOCKS          = new HotkeyConfig("renderOverlayThroughBlocks",        "R_CTRL", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig REFRESH_SCHEMATIC_RENDERER             = new HotkeyConfig("refreshSchematicRenderer",          "F3,M");
    public static final HotkeyConfig ROTATE_PLACEMENT_CW                    = new HotkeyConfig("rotatePlacementCW",                 "");
    public static final HotkeyConfig ROTATE_PLACEMENT_CCW                   = new HotkeyConfig("rotatePlacementCCW",                "");
    public static final HotkeyConfig SAVE_SCHEMATIC_TO_FILE                 = new HotkeyConfig("saveSchematicToFile",               "L_CTRL,L_ALT,S");
    public static final HotkeyConfig SCHEMATIC_EDIT_ACCEPT_REPLACEMENT      = new HotkeyConfig("schematicEditAcceptReplacement",    "");
    public static final HotkeyConfig SCHEMATIC_EDIT_BREAK_ALL               = new HotkeyConfig("schematicEditBreakPlaceAll",        "", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig SCHEMATIC_EDIT_BREAK_DIRECTION         = new HotkeyConfig("schematicEditBreakPlaceDirection",  "", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig SCHEMATIC_EDIT_REPLACE_ALL             = new HotkeyConfig("schematicEditReplaceAll",           "", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig SCHEMATIC_EDIT_REPLACE_DIRECTION       = new HotkeyConfig("schematicEditReplaceDirection",     "", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig SCHEMATIC_VERSION_CYCLE_MODIFIER       = new HotkeyConfig("schematicVersionCycleModifier",     "", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig SCHEMATIC_VERSION_CYCLE_NEXT           = new HotkeyConfig("schematicVersionCycleNext",         "");
    public static final HotkeyConfig SCHEMATIC_VERSION_CYCLE_PREVIOUS       = new HotkeyConfig("schematicVersionCyclePrevious",     "");
    public static final HotkeyConfig SELECTION_EXPAND_MODIFIER              = new HotkeyConfig("selectionExpandModifier",           "", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig SELECTION_GRAB_MODIFIER                = new HotkeyConfig("selectionGrabModifier",             "", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig SELECTION_GROW_HOTKEY                  = new HotkeyConfig("selectionGrow",                     "");
    public static final HotkeyConfig SELECTION_GROW_MODIFIER                = new HotkeyConfig("selectionGrowModifier",             "", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig SELECTION_NUDGE_MODIFIER               = new HotkeyConfig("selectionNudgeModifier",            "L_ALT", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig SELECTION_SHRINK                       = new HotkeyConfig("selectionShrink",                   "");
    public static final HotkeyConfig SET_AREA_ORIGIN                        = new HotkeyConfig("setAreaOrigin",                     "");
    public static final HotkeyConfig SET_HELD_ITEM_AS_TOOL                  = new HotkeyConfig("setHeldItemAsTool",                 "");
    public static final HotkeyConfig SET_SCHEMATIC_PREVIEW                  = new HotkeyConfig("setSchematicPreview",               "F2");
    public static final HotkeyConfig SET_SELECTION_BOX_POSITION_1           = new HotkeyConfig("setSelectionBoxPosition1",          "");
    public static final HotkeyConfig SET_SELECTION_BOX_POSITION_2           = new HotkeyConfig("setSelectionBoxPosition2",          "");
    public static final HotkeyConfig SUB_MODE_CYCLE                         = new HotkeyConfig("subModeCycle",                      "L_CTRL,M");
    public static final HotkeyConfig TOOL_MODE_CYCLE_FORWARD                = new HotkeyConfig("toolModeCycleForward",              "");
    public static final HotkeyConfig TOOL_MODE_CYCLE_BACKWARD               = new HotkeyConfig("toolModeCycleBackward",             "");
    public static final HotkeyConfig TOOL_PLACE_CORNER_1                    = new HotkeyConfig("toolPlaceCorner1",                  "LEFT_MOUSE", KeyBindSettings.INGAME_EXTRA);
    public static final HotkeyConfig TOOL_PLACE_CORNER_2                    = new HotkeyConfig("toolPlaceCorner2",                  "RIGHT_MOUSE", KeyBindSettings.INGAME_EXTRA);
    public static final HotkeyConfig TOOL_SELECT_ELEMENTS                   = new HotkeyConfig("toolSelectElements",                "MIDDLE_MOUSE", KeyBindSettings.INGAME_EXTRA);
    public static final HotkeyConfig TOOL_SELECT_MODIFIER_BLOCK_1           = new HotkeyConfig("toolSelectModifierBlock1",          "L_ALT", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig TOOL_SELECT_MODIFIER_BLOCK_2           = new HotkeyConfig("toolSelectModifierBlock2",          "L_SHIFT", KeyBindSettings.INGAME_MODIFIER);
    public static final HotkeyConfig TRANSLUCENT_SCHEMATIC_RENDERING        = new HotkeyConfig("translucentSchematicRendering",     "");
    public static final HotkeyConfig UNLOAD_CURRENT_SCHEMATIC               = new HotkeyConfig("unloadCurrentSchematic",            "");
    public static final HotkeyConfig UPDATE_BLOCKS                          = new HotkeyConfig("updateBlocks",                      "");


    public static final List<HotkeyConfig> HOTKEY_LIST = ImmutableList.of(
            ADD_SELECTION_BOX,
            CLONE_SELECTION,
            CREATE_SCHEMATIC_IN_MEMORY,
            DELETE_SELECTION_BOX,
            DUPLICATE_PLACEMENT,
            EASY_PLACE_ACTIVATION,
            EXECUTE_OPERATION,
            INVERT_SCHEMATIC_RENDER_STATE,
            INVERT_OVERLAY_RENDER_STATE,
            LAYER_MODE_NEXT,
            LAYER_MODE_PREVIOUS,
            LAYER_NEXT,
            LAYER_PREVIOUS,
            LAYER_SET_HERE,
            NUDGE_SELECTION_NEGATIVE,
            NUDGE_SELECTION_POSITIVE,
            MOVE_ENTIRE_SELECTION,
            OPEN_AREA_EDITOR_SCREEN,
            OPEN_LOAD_SCHEMATICS_SCREEN,
            OPEN_LOADED_SCHEMATICS_SCREEN,
            OPEN_MAIN_MENU,
            OPEN_MATERIAL_LIST_SCREEN,
            OPEN_PLACEMENT_GRID_SETTINGS,
            OPEN_PLACEMENT_SETTINGS_SCREEN,
            OPEN_PLACEMENTS_LIST_SCREEN,
            OPEN_SCHEMATIC_VCS_SCREEN,
            OPEN_SCHEMATIC_VERIFIER_SCREEN,
            OPEN_AREA_SELECTION_BROWSER,
            OPEN_CONFIG_SCREEN,
            OPERATION_MODE_CHANGE_MODIFIER,
            PICK_BLOCK_FIRST,
            PICK_BLOCK_LAST,
            REMOVE_SELECTED_PLACEMENT,
            RENDER_BLOCK_INFO_OVERLAY,
            RENDER_OVERLAY_THROUGH_BLOCKS,
            REFRESH_SCHEMATIC_RENDERER,
            ROTATE_PLACEMENT_CW,
            ROTATE_PLACEMENT_CCW,
            SAVE_SCHEMATIC_TO_FILE,
            SCHEMATIC_EDIT_ACCEPT_REPLACEMENT,
            SCHEMATIC_EDIT_BREAK_ALL,
            SCHEMATIC_EDIT_BREAK_DIRECTION,
            SCHEMATIC_EDIT_REPLACE_ALL,
            SCHEMATIC_EDIT_REPLACE_DIRECTION,
            SCHEMATIC_VERSION_CYCLE_MODIFIER,
            SCHEMATIC_VERSION_CYCLE_NEXT,
            SCHEMATIC_VERSION_CYCLE_PREVIOUS,
            SELECTION_EXPAND_MODIFIER,
            SELECTION_GRAB_MODIFIER,
            SELECTION_GROW_HOTKEY,
            SELECTION_GROW_MODIFIER,
            SELECTION_NUDGE_MODIFIER,
            SELECTION_SHRINK,
            SET_AREA_ORIGIN,
            SET_HELD_ITEM_AS_TOOL,
            SET_SCHEMATIC_PREVIEW,
            SET_SELECTION_BOX_POSITION_1,
            SET_SELECTION_BOX_POSITION_2,
            SUB_MODE_CYCLE,
            TOOL_MODE_CYCLE_FORWARD,
            TOOL_MODE_CYCLE_BACKWARD,
            TOOL_SELECT_ELEMENTS,
            TOOL_SELECT_MODIFIER_BLOCK_1,
            TOOL_SELECT_MODIFIER_BLOCK_2,
            TOOL_PLACE_CORNER_1,
            TOOL_PLACE_CORNER_2,
            TRANSLUCENT_SCHEMATIC_RENDERING,
            UNLOAD_CURRENT_SCHEMATIC,
            UPDATE_BLOCKS
    );
}
