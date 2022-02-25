package fi.dy.masa.litematica.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.option.HotkeyConfig;
import fi.dy.masa.malilib.input.KeyBindSettings;

public class Hotkeys
{
    public static final HotkeyConfig ADD_SELECTION_BOX                      = new HotkeyConfig("addSelectionBox", "M,A", "Add a new selection box (position 1) here");
    public static final HotkeyConfig CLONE_SELECTION                        = new HotkeyConfig("cloneSelection", "", "Quickly clone the current area selection.\nThis basically just creates an in-memory-only schematic,\nand then creates a placement of that schematic and selects it,\nand also switches the tool mode to the Paste mode.");
    public static final HotkeyConfig DELETE_SELECTION_BOX                   = new HotkeyConfig("deleteSelectionBox", "", "Delete the currently selected box");
    public static final HotkeyConfig DUPLICATE_PLACEMENT                    = new HotkeyConfig("duplicatePlacement", "", "Creates a duplicate of the currently selected placement,\nand selects that newly created placement");
    public static final HotkeyConfig EASY_PLACE_ACTIVATION                  = new HotkeyConfig("easyPlaceActivation", "", KeyBindSettings.INGAME_MODIFIER_EMPTY, "When the easyPlaceMode is enabled, this key must\nbe held to enable placing the blocks when\nusing the vanilla Use key");
    public static final HotkeyConfig EASY_PLACE_TOGGLE                      = new HotkeyConfig("easyPlaceToggle", "", "Allows quickly toggling on/off the Easy Place mode");
    public static final HotkeyConfig EXECUTE_OPERATION                      = new HotkeyConfig("executeOperation", "", "Execute the currently selected tool operation with the\ncurrent selection or placement in the Fill, Replace,\nPaste Schematic etc. modes");
    public static final HotkeyConfig INVERT_GHOST_BLOCK_RENDER_STATE        = new HotkeyConfig("invertGhostBlockRenderState", "", "Inverts the schematic/ghost block rendering status\nwhile this keybind is held down");
    public static final HotkeyConfig INVERT_OVERLAY_RENDER_STATE            = new HotkeyConfig("invertOverlayRenderState", "", "Inverts the Overlay rendering status while this keybind is held down");
    public static final HotkeyConfig LAYER_MODE_NEXT                        = new HotkeyConfig("layerModeNext", "M,PRIOR", "Cycle the rendering mode (all, layers) forward");
    public static final HotkeyConfig LAYER_MODE_PREVIOUS                    = new HotkeyConfig("layerModePrevious", "M,NEXT", "Cycle the rendering mode (all, layers) backwards");
    public static final HotkeyConfig LAYER_NEXT                             = new HotkeyConfig("layerNext", "PRIOR", "Move the rendered layer selection up");
    public static final HotkeyConfig LAYER_PREVIOUS                         = new HotkeyConfig("layerPrevious", "NEXT", "Move the rendered layer selection down");
    public static final HotkeyConfig LAYER_SET_HERE                         = new HotkeyConfig("layerSetHere", "", "Set the Render Layer to the player's current position");
    public static final HotkeyConfig NUDGE_SELECTION_NEGATIVE               = new HotkeyConfig("nudgeSelectionNegative", "", "Nudge the current selection in the \"negative\" direction\nThis is basically the same as mouse wheel down\nwith the Nudge modifier pressed");
    public static final HotkeyConfig NUDGE_SELECTION_POSITIVE               = new HotkeyConfig("nudgeSelectionPositive", "", "Nudge the current selection in the \"positive\" direction\nThis is basically the same as mouse wheel up\nwith the Nudge modifier pressed");
    public static final HotkeyConfig MOVE_ENTIRE_SELECTION                  = new HotkeyConfig("moveEntireSelection", "", "Move the entire current selection here");
    public static final HotkeyConfig OPEN_GUI_AREA_SETTINGS                 = new HotkeyConfig("openGuiAreaSettings", "MULTIPLY", "Open the Area Settings GUI for the currently selected area");
    public static final HotkeyConfig OPEN_GUI_LOAD_SCHEMATICS               = new HotkeyConfig("openGuiLoadSchematics", "", "Open the Load Schematics GUI");
    public static final HotkeyConfig OPEN_GUI_LOADED_SCHEMATICS             = new HotkeyConfig("openGuiLoadedSchematics", "", "Open the Loaded Schematics GUI");
    public static final HotkeyConfig OPEN_GUI_MAIN_MENU                     = new HotkeyConfig("openGuiMainMenu", "M", KeyBindSettings.INGAME_RELEASE_EXCLUSIVE, "Open the Litematica main menu");
    public static final HotkeyConfig OPEN_GUI_MATERIAL_LIST                 = new HotkeyConfig("openGuiMaterialList", "M,L", "Open the Material List GUI for the currently\nselected schematic placement");
    public static final HotkeyConfig OPEN_GUI_PLACEMENT_GRID_SETTINGS       = new HotkeyConfig("openGuiPlacementGridSettings", "", "Open the Grid Settings GUI for the currently selected schematic placement");
    public static final HotkeyConfig OPEN_GUI_PLACEMENT_SETTINGS            = new HotkeyConfig("openGuiPlacementSettings", "SUBTRACT", "Open the Placement Settings GUI for the currently\nselected placement or sub-region");
    public static final HotkeyConfig OPEN_GUI_SCHEMATIC_PLACEMENTS          = new HotkeyConfig("openGuiSchematicPlacements", "M,P", "Open the Schematic Placements GUI");
    public static final HotkeyConfig OPEN_GUI_SCHEMATIC_PROJECTS            = new HotkeyConfig("openGuiSchematicProjects", "", "Open the Schematic Projects GUI");
    public static final HotkeyConfig OPEN_GUI_SCHEMATIC_VERIFIER            = new HotkeyConfig("openGuiSchematicVerifier", "M,V", "Open the Schematic Verifier GUI for the currently\nselected schematic placement");
    public static final HotkeyConfig OPEN_GUI_SELECTION_MANAGER             = new HotkeyConfig("openGuiSelectionManager", "M,S", "Open the Area Selection manager GUI");
    public static final HotkeyConfig OPEN_GUI_SETTINGS                      = new HotkeyConfig("openGuiSettings", "M,C", "Open the Config GUI");
    public static final HotkeyConfig OPERATION_MODE_CHANGE_MODIFIER         = new HotkeyConfig("operationModeChangeModifier", "LCONTROL", KeyBindSettings.INGAME_MODIFIER, "The modifier key to quickly change the operation mode.\nHold this and scroll while holding the \"tool item\" to quickly cycle the mode.");
    public static final HotkeyConfig PICK_BLOCK_FIRST                       = new HotkeyConfig("pickBlockFirst", "BUTTON2", KeyBindSettings.INGAME_EXTRA, "A key to pick block the first\nschematic block ray traced to");
    public static final HotkeyConfig PICK_BLOCK_LAST                        = new HotkeyConfig("pickBlockLast", "", KeyBindSettings.INGAME_MODIFIER, "A key to pick block the last schematic block\nray traced to, before the first (possible) client world\nblock ray traced to. Basically this would get\nyou the block you could place against an existing block.");
    public static final HotkeyConfig PICK_BLOCK_TOGGLE                      = new HotkeyConfig("pickBlockToggle", "M,BUTTON2", "A hotkey to toggle the pick block toggle option in the\nGeneric configs. This is provided as a quick way to enable\nor disable the pick block keys, if they interfere with something.");
    public static final HotkeyConfig PICK_BLOCK_TOGGLE_AUTO                 = new HotkeyConfig("pickBlockToggleAuto", "", "A hotkey to toggle the pickBlockAuto option in the Generic configs.");
    public static final HotkeyConfig REMOVE_SELECTED_PLACEMENT              = new HotkeyConfig("removeSelectedPlacement", "", "Removes the currently selected placement\n(without unloading the schematic)");
    public static final HotkeyConfig RENDER_INFO_OVERLAY                    = new HotkeyConfig("renderInfoOverlay", "I", KeyBindSettings.INGAME_EXTRA, "The key that enables rendering the block info overlay.\nUse NONE for not requiring a key to be pressed.\nDisable the similarly named option in the Visuals\nconfigs to disable the overlay completely.");
    public static final HotkeyConfig RENDER_OVERLAY_THROUGH_BLOCKS          = new HotkeyConfig("renderOverlayThroughBlocks", "RCONTROL", KeyBindSettings.INGAME_EXTRA, "A hotkey to allow the overlays to render through blocks.\nThis is just a quicker way to temporarily enable\nthe same thing that the 'schematicOverlayRenderThroughBlocks' option in Visuals does.");
    public static final HotkeyConfig RERENDER_SCHEMATIC                     = new HotkeyConfig("rerenderSchematic", "F3,M", "Hotkey to refresh/redraw only the schematic, instead of\nhaving to refresh the vanilla terrain too with F3 + A");
    public static final HotkeyConfig ROTATE_PLACEMENT_CW                    = new HotkeyConfig("rotatePlacementCW", "", "Rotates the currently selected placement clockwise");
    public static final HotkeyConfig ROTATE_PLACEMENT_CCW                   = new HotkeyConfig("rotatePlacementCCW", "", "Rotates the currently selected placement counter-clockwise");
    public static final HotkeyConfig SAVE_AREA_AS_IN_MEMORY_SCHEMATIC       = new HotkeyConfig("saveAreaAsInMemorySchematic", "", "Save the current Area Selection as an in-memory Schematic");
    public static final HotkeyConfig SAVE_AREA_AS_SCHEMATIC_TO_FILE         = new HotkeyConfig("saveAreaAsSchematicToFile", "LCONTROL,LMENU,S", "Save the current Area Selection as a Schematic to a file");
    public static final HotkeyConfig SCHEMATIC_REBUILD_ACCEPT_REPLACEMENT   = new HotkeyConfig("schematicRebuildAcceptReplacement", "", "Does the Replace All action using the block that is currently in the targeted position");
    public static final HotkeyConfig SCHEMATIC_REBUILD_BREAK_ALL            = new HotkeyConfig("schematicRebuildBreakPlaceAll", "", KeyBindSettings.INGAME_MODIFIER, "Modifier key to activate the \"break all identical blocks\"\nfunction in the Schematic Rebuild tool mode");
    public static final HotkeyConfig SCHEMATIC_REBUILD_BREAK_DIRECTION      = new HotkeyConfig("schematicRebuildBreakPlaceDirection", "", KeyBindSettings.INGAME_MODIFIER, "Modifier key to activate the directional/continuous\nbreak or place function in the Schematic Rebuild tool mode");
    public static final HotkeyConfig SCHEMATIC_REBUILD_REPLACE_ALL          = new HotkeyConfig("schematicRebuildReplaceAll", "", KeyBindSettings.INGAME_MODIFIER, "Modifier key to activate the \"replace all identical\"\nreplace mode/function in the Schematic Rebuild tool mode");
    public static final HotkeyConfig SCHEMATIC_REBUILD_REPLACE_DIRECTION    = new HotkeyConfig("schematicRebuildReplaceDirection", "", KeyBindSettings.INGAME_MODIFIER, "Modifier key to activate the directional/continuous\nreplace mode/function in the Schematic Rebuild tool mode");
    public static final HotkeyConfig SCHEMATIC_VERSION_CYCLE_MODIFIER       = new HotkeyConfig("schematicVersionCycleModifier", "", KeyBindSettings.INGAME_MODIFIER, "The modifier key to hold to be able to use the mouse wheel\nto cycle through the schematic versions in the Version Control tool mode");
    public static final HotkeyConfig SCHEMATIC_VERSION_CYCLE_NEXT           = new HotkeyConfig("schematicVersionCycleNext", "", "A hotkey to cycle to the next schematic version in the Version Control tool mode");
    public static final HotkeyConfig SCHEMATIC_VERSION_CYCLE_PREVIOUS       = new HotkeyConfig("schematicVersionCyclePrevious", "", "A hotkey to cycle to the next schematic version in the Version Control tool mode");
    public static final HotkeyConfig SELECTION_EXPAND_MODIFIER              = new HotkeyConfig("selectionExpandModifier", "", KeyBindSettings.INGAME_MODIFIER, "The modifier key to hold to expand or shrink a selection box\nfrom the looking direction side while scrolling");
    public static final HotkeyConfig SELECTION_GRAB_MODIFIER                = new HotkeyConfig("selectionGrabModifier", "", KeyBindSettings.INGAME_MODIFIER, "The modifier key to hold to \"grab\" a selection\nbox or corner for cursor moving.");
    public static final HotkeyConfig SELECTION_GROW_HOTKEY                  = new HotkeyConfig("selectionGrow", "", "The action hotkey to auto-grow the selection box around\nany adjacent/diagonally connected blocks");
    public static final HotkeyConfig SELECTION_GROW_MODIFIER                = new HotkeyConfig("selectionGrowModifier", "", KeyBindSettings.INGAME_MODIFIER, "The modifier key to hold to grow or shrink\na selection box while scrolling");
    public static final HotkeyConfig SELECTION_NUDGE_MODIFIER               = new HotkeyConfig("selectionNudgeModifier", "LMENU", KeyBindSettings.INGAME_MODIFIER, "The modifier key to hold while scrolling\nto nudge the selected area or corner");
    public static final HotkeyConfig SELECTION_MODE_CYCLE                   = new HotkeyConfig("selectionModeCycle", "LCONTROL,M", "Change the mode between Corners and Cuboid\nin the Area Selection mode");
    public static final HotkeyConfig SELECTION_SHRINK                       = new HotkeyConfig("selectionShrink", "", "The action hotkey to shrink the selection box so that there\nisn't any empty space (empty layers) on any side");
    public static final HotkeyConfig SET_AREA_ORIGIN                        = new HotkeyConfig("setAreaOrigin", "", "Set/move the origin point of the current\narea selection to the player's position");
    public static final HotkeyConfig SET_HELD_ITEM_AS_TOOL                  = new HotkeyConfig("setHeldItemAsTool", "", "Sets the currently held item as the tool item");
    public static final HotkeyConfig SET_SCHEMATIC_PREVIEW                  = new HotkeyConfig("setSchematicPreview", "F2", "Sets the schematic thumbnail/preview image by taking a screenshot,\nwhen a preview task has been started via the Schematic Manager");
    public static final HotkeyConfig SET_SELECTION_BOX_POSITION_1           = new HotkeyConfig("setSelectionBoxPosition1", "", "Set the first position of the currently selected\nbox to the player's position");
    public static final HotkeyConfig SET_SELECTION_BOX_POSITION_2           = new HotkeyConfig("setSelectionBoxPosition2", "", "Set the second position of the currently selected\nbox to the player's position");
    public static final HotkeyConfig TOGGLE_ALL_RENDERING                   = new HotkeyConfig("toggleAllRendering", "M,R", "Toggle all rendering on/off", "All Rendering");
    public static final HotkeyConfig TOGGLE_AREA_SELECTION_RENDERING        = new HotkeyConfig("toggleAreaSelectionBoxesRendering", "", "Toggle Area Selection boxes rendering on/off");
    public static final HotkeyConfig TOGGLE_SCHEMATIC_RENDERING             = new HotkeyConfig("toggleSchematicRendering", "M,G", "Toggle schematic rendering (blocks & overlay) on/off");
    public static final HotkeyConfig TOGGLE_INFO_OVERLAY_RENDERING          = new HotkeyConfig("toggleInfoOverlayRendering", "", "Toggle the info overlay rendering (for hovered block info)");
    public static final HotkeyConfig TOGGLE_OVERLAY_RENDERING               = new HotkeyConfig("toggleOverlayRendering", "", "Toggle the block overlay rendering on/off");
    public static final HotkeyConfig TOGGLE_OVERLAY_OUTLINE_RENDERING       = new HotkeyConfig("toggleOverlayOutlineRendering", "", "Toggle the block overlay outline rendering on/off");
    public static final HotkeyConfig TOGGLE_OVERLAY_SIDE_RENDERING          = new HotkeyConfig("toggleOverlaySideRendering", "", "Toggle the block overlay side rendering on/off");
    public static final HotkeyConfig TOGGLE_PLACEMENT_BOXES_RENDERING       = new HotkeyConfig("togglePlacementBoxesRendering", "", "Toggle Schematic Placement boxes rendering on/off");
    public static final HotkeyConfig TOGGLE_PLACEMENT_RESTRICTION           = new HotkeyConfig("togglePlacementRestriction", "", "A hotkey to toggle the placement restriction mode");
    public static final HotkeyConfig TOGGLE_SCHEMATIC_BLOCK_RENDERING       = new HotkeyConfig("toggleSchematicBlockRendering", "", "Toggle schematic block rendering on/off");
    public static final HotkeyConfig TOGGLE_SIGN_TEXT_PASTE                 = new HotkeyConfig("toggleSignTextPaste", "", "Toggle the signTextPaste config value (in Generic category)");
    public static final HotkeyConfig TOGGLE_TRANSLUCENT_RENDERING           = new HotkeyConfig("toggleTranslucentRendering", "", "Toggle translucent vs. opaque ghost block rendering");
    public static final HotkeyConfig TOGGLE_VERIFIER_OVERLAY_RENDERING      = new HotkeyConfig("toggleVerifierOverlayRendering", "", "Toggle the Schematic Verifier overlay rendering");
    public static final HotkeyConfig TOOL_ENABLED_TOGGLE                    = new HotkeyConfig("toolEnabledToggle", "M,T", "The keybind to toggle the \"tool\" item functionality on/off");
    public static final HotkeyConfig TOOL_MODE_CYCLE_FORWARD                = new HotkeyConfig("toolModeCycleForward", "", "Cycles the current tool mode forward");
    public static final HotkeyConfig TOOL_MODE_CYCLE_BACKWARD               = new HotkeyConfig("toolModeCycleBackward", "", "Cycles the current tool mode backward");
    public static final HotkeyConfig TOOL_PLACE_CORNER_1                    = new HotkeyConfig("toolPlaceCorner1", "BUTTON0", KeyBindSettings.INGAME_EXTRA, "The button to use while holding the \"tool\" item\nto place the primary/first corner");
    public static final HotkeyConfig TOOL_PLACE_CORNER_2                    = new HotkeyConfig("toolPlaceCorner2", "BUTTON1", KeyBindSettings.INGAME_EXTRA, "The button to use while holding the \"tool\" item\nto place the second corner");
    public static final HotkeyConfig TOOL_SELECT_ELEMENTS                   = new HotkeyConfig("toolSelectElements", "BUTTON2", KeyBindSettings.INGAME_EXTRA, "The button to use to select corners or boxes\nwhile holding the \"tool\" item");
    public static final HotkeyConfig TOOL_SELECT_MODIFIER_BLOCK_1           = new HotkeyConfig("toolSelectModifierBlock1", "LMENU", KeyBindSettings.INGAME_MODIFIER, "The modifier key to hold while using the 'toolSelectElements'\nhotkey, to select the primary block type to use in some of the tool modes");
    public static final HotkeyConfig TOOL_SELECT_MODIFIER_BLOCK_2           = new HotkeyConfig("toolSelectModifierBlock2", "LSHIFT", KeyBindSettings.INGAME_MODIFIER, "The modifier key to hold while using the 'toolSelectElements'\nhotkey, to select the secondary block type to use in some of the tool modes");
    public static final HotkeyConfig UNLOAD_CURRENT_SCHEMATIC               = new HotkeyConfig("unloadCurrentSchematic", "", "Unloads the schematic of the currently selected placement,and thus also removes all placements created from it\n");
    public static final HotkeyConfig UPDATE_BLOCKS                          = new HotkeyConfig("updateBlocks", "", "Updates all the blocks inside the current area selection.\n(So basically causes a block update for each position.)");

    public static final List<HotkeyConfig> HOTKEY_LIST = ImmutableList.of(
            ADD_SELECTION_BOX,
            CLONE_SELECTION,
            DELETE_SELECTION_BOX,
            DUPLICATE_PLACEMENT,
            EASY_PLACE_ACTIVATION,
            EASY_PLACE_TOGGLE,
            EXECUTE_OPERATION,
            INVERT_GHOST_BLOCK_RENDER_STATE,
            INVERT_OVERLAY_RENDER_STATE,
            LAYER_MODE_NEXT,
            LAYER_MODE_PREVIOUS,
            LAYER_NEXT,
            LAYER_PREVIOUS,
            LAYER_SET_HERE,
            NUDGE_SELECTION_NEGATIVE,
            NUDGE_SELECTION_POSITIVE,
            MOVE_ENTIRE_SELECTION,
            OPEN_GUI_AREA_SETTINGS,
            OPEN_GUI_LOAD_SCHEMATICS,
            OPEN_GUI_LOADED_SCHEMATICS,
            OPEN_GUI_MAIN_MENU,
            OPEN_GUI_MATERIAL_LIST,
            OPEN_GUI_PLACEMENT_GRID_SETTINGS,
            OPEN_GUI_PLACEMENT_SETTINGS,
            OPEN_GUI_SCHEMATIC_PLACEMENTS,
            OPEN_GUI_SCHEMATIC_PROJECTS,
            OPEN_GUI_SCHEMATIC_VERIFIER,
            OPEN_GUI_SELECTION_MANAGER,
            OPEN_GUI_SETTINGS,
            OPERATION_MODE_CHANGE_MODIFIER,
            PICK_BLOCK_FIRST,
            PICK_BLOCK_LAST,
            PICK_BLOCK_TOGGLE,
            PICK_BLOCK_TOGGLE_AUTO,
            REMOVE_SELECTED_PLACEMENT,
            RENDER_INFO_OVERLAY,
            RENDER_OVERLAY_THROUGH_BLOCKS,
            RERENDER_SCHEMATIC,
            ROTATE_PLACEMENT_CW,
            ROTATE_PLACEMENT_CCW,
            SAVE_AREA_AS_IN_MEMORY_SCHEMATIC,
            SAVE_AREA_AS_SCHEMATIC_TO_FILE,
            SCHEMATIC_REBUILD_ACCEPT_REPLACEMENT,
            SCHEMATIC_REBUILD_BREAK_ALL,
            SCHEMATIC_REBUILD_BREAK_DIRECTION,
            SCHEMATIC_REBUILD_REPLACE_ALL,
            SCHEMATIC_REBUILD_REPLACE_DIRECTION,
            SCHEMATIC_VERSION_CYCLE_MODIFIER,
            SCHEMATIC_VERSION_CYCLE_NEXT,
            SCHEMATIC_VERSION_CYCLE_PREVIOUS,
            SELECTION_EXPAND_MODIFIER,
            SELECTION_GRAB_MODIFIER,
            SELECTION_GROW_HOTKEY,
            SELECTION_GROW_MODIFIER,
            SELECTION_NUDGE_MODIFIER,
            SELECTION_MODE_CYCLE,
            SELECTION_SHRINK,
            SET_AREA_ORIGIN,
            SET_HELD_ITEM_AS_TOOL,
            SET_SCHEMATIC_PREVIEW,
            SET_SELECTION_BOX_POSITION_1,
            SET_SELECTION_BOX_POSITION_2,
            TOGGLE_ALL_RENDERING,
            TOGGLE_AREA_SELECTION_RENDERING,
            TOGGLE_INFO_OVERLAY_RENDERING,
            TOGGLE_OVERLAY_RENDERING,
            TOGGLE_OVERLAY_OUTLINE_RENDERING,
            TOGGLE_OVERLAY_SIDE_RENDERING,
            TOGGLE_PLACEMENT_BOXES_RENDERING,
            TOGGLE_PLACEMENT_RESTRICTION,
            TOGGLE_SCHEMATIC_BLOCK_RENDERING,
            TOGGLE_SCHEMATIC_RENDERING,
            TOGGLE_SIGN_TEXT_PASTE,
            TOGGLE_TRANSLUCENT_RENDERING,
            TOGGLE_VERIFIER_OVERLAY_RENDERING,
            TOOL_ENABLED_TOGGLE,
            TOOL_MODE_CYCLE_FORWARD,
            TOOL_MODE_CYCLE_BACKWARD,
            TOOL_PLACE_CORNER_1,
            TOOL_PLACE_CORNER_2,
            TOOL_SELECT_ELEMENTS,
            TOOL_SELECT_MODIFIER_BLOCK_1,
            TOOL_SELECT_MODIFIER_BLOCK_2,
            UNLOAD_CURRENT_SCHEMATIC,
            UPDATE_BLOCKS
    );
}
