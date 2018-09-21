package fi.dy.masa.litematica.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

public class Hotkeys
{
    public static final ConfigHotkey ADD_SELECTION_BOX                  = new ConfigHotkey("addSelectionBox",                   "M,A",  "Add a new selection box (position 1) here");
    public static final ConfigHotkey DELETE_SELECTION_BOX               = new ConfigHotkey("deleteSelectionBox",                "DELETE", "Delete the currently selected box");
    public static final ConfigHotkey EXECUTE_OPERATION                  = new ConfigHotkey("executeOperation",                  "LCONTROL,LMENU,R",  "Execute the currently selected tool operation with the\ncurrent selection or placement in the Fill, Replace,\nPaste Schematic etc. modes");
    public static final ConfigHotkey LAYER_MODE_NEXT                    = new ConfigHotkey("layerModeNext",                     "M,PRIOR", "Cycle the rendering mode (all, layers) forward");
    public static final ConfigHotkey LAYER_MODE_PREVIOUS                = new ConfigHotkey("layerModePrevious",                 "M,NEXT",  "Cycle the rendering mode (all, layers) backwards");
    public static final ConfigHotkey LAYER_NEXT                         = new ConfigHotkey("layerNext",                         "PRIOR", "Move the rendered layer selection up");
    public static final ConfigHotkey LAYER_PREVIOUS                     = new ConfigHotkey("layerPrevious",                     "NEXT",  "Move the rendered layer selection down");
    public static final ConfigHotkey MOVE_ENTIRE_SELECTION              = new ConfigHotkey("moveEntireSelection",               "M,H",  "Move the entire current selection here");
    public static final ConfigHotkey OPEN_GUI_AREA_SETTINGS             = new ConfigHotkey("openGuiAreaSettings",               "SUBTRACT",  KeybindSettings.EXCLUSIVE, "Open the Area Settings GUI for the currently selected area");
    public static final ConfigHotkey OPEN_GUI_PLACEMENT_SETTINGS        = new ConfigHotkey("openGuiPlacementSettings",          "SUBTRACT",  KeybindSettings.EXCLUSIVE, "Open the Placement Settings GUI for the currently\nselected placement or sub-region");
    public static final ConfigHotkey OPEN_GUI_MAIN_MENU                 = new ConfigHotkey("openGuiMainMenu",                   "M",    KeybindSettings.RELEASE_EXCLUSIVE, "Open the Litematica main menu");
    public static final ConfigHotkey OPEN_GUI_MATERIAL_LIST             = new ConfigHotkey("openGuiMaterialList",               "M,L",  "Open the Material List GUI for the currently\nselected schematic placement");
    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_PLACEMENTS      = new ConfigHotkey("openGuiSchematicPlacements",        "M,P",  KeybindSettings.EXCLUSIVE, "Open the Schematic Placements GUI");
    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_VERIFIER        = new ConfigHotkey("openGuiSchematicVerifier",          "M,V",  "Open the Schematic Verifier GUI for the currently\nselected schematic placement");
    public static final ConfigHotkey OPEN_GUI_SELECTION_MANAGER         = new ConfigHotkey("openGuiSelectionManager",           "M,S",  "Open the Area Selection manager GUI");
    public static final ConfigHotkey OPEN_GUI_SETTINGS                  = new ConfigHotkey("openGuiSettings",                   "M,C",  "Open the Config GUI");
    public static final ConfigHotkey OPERATION_MODE_CHANGE_MODIFIER     = new ConfigHotkey("operationModeChangeModifier",       "LCONTROL", KeybindSettings.PRESS_ALLOWEXTRA, "The modifier key to quickly change the operation mode.\nHold this and scroll while holding the \"tool item\" to quickly cycle the mode.");
    public static final ConfigHotkey PICK_BLOCK_FIRST                   = new ConfigHotkey("pickBlockFirst",                    "LSHIFT,BUTTON2", KeybindSettings.PRESS_ALLOWEXTRA, "A key to pick block the first\nschematic block ray traced to");
    public static final ConfigHotkey PICK_BLOCK_LAST                    = new ConfigHotkey("pickBlockLast",                     "BUTTON1", KeybindSettings.NOCANCEL, "A key to pick block the last schematic block\nray traced to, before the first (possible) client world\nblock ray traced to. Basically this would get\nyou the block you could place against an existing block.");
    public static final ConfigHotkey PICK_BLOCK_TOGGLE                  = new ConfigHotkey("pickBlockToggle",                   "M,BUTTON2", "A hotkey to toggle the pick block toggle option in the\nGeneric configs. This is provided as a quick way to enable\nor disable the pick block keys, if they interfere with something.");
    public static final ConfigHotkey RENDER_INFO_OVERLAY                = new ConfigHotkey("renderInfoOverlay",                 "LMENU", KeybindSettings.PRESS_ALLOWEXTRA, "The key that enables rendering the block info overlay.\nUse NONE for not requiring a key to be pressed.\nDisable the similarly named option in the Visuals\nconfigs to disable the overlay completely.");
    public static final ConfigHotkey SAVE_AREA_AS_IN_MEMORY_SCHEMATIC   = new ConfigHotkey("saveAreaAsInMemorySchematic",       "LCONTROL,S",  "Save the current Area Selection as an in-memory Schematic");
    public static final ConfigHotkey SAVE_AREA_AS_SCHEMATIC_TO_FILE     = new ConfigHotkey("saveAreaAsSchematicToFile",         "LCONTROL,LMENU,S",  "Save the current Area Selection as a Schematic to a file");
    public static final ConfigHotkey SELECTION_GRAB_MODIFIER            = new ConfigHotkey("selectionGrabModifier",             "LMENU", "The modifier key to be held while clicking\npick block to \"grab\" a selection box or corner.");
    public static final ConfigHotkey SELECTION_MODE_CYCLE               = new ConfigHotkey("selectionModeCycle",                "LCONTROL,M", "Change the mode between Corners and Cuboid\nin the Area Selection mode");
    public static final ConfigHotkey SET_AREA_ORIGIN                    = new ConfigHotkey("setAreaOrigin",                     "M,Z",  "Set/move the origin point of the current\narea selection to the player's position");
    public static final ConfigHotkey SET_SELECTION_BOX_POSITION_1       = new ConfigHotkey("setSelectionBoxPosition1",          "M,1",  "Set the first position of the currently selected\nbox to the player's position");
    public static final ConfigHotkey SET_SELECTION_BOX_POSITION_2       = new ConfigHotkey("setSelectionBoxPosition2",          "M,2",  "Set the second position of the currently selected\nbox to the player's position");
    public static final ConfigHotkey TOGGLE_ALL_RENDERING               = new ConfigHotkey("toggleAllRendering",                "M,R",  "Toggle all rendering on/off", "All Rendering");
    public static final ConfigHotkey TOGGLE_GHOST_BLOCK_RENDERING       = new ConfigHotkey("toggleGhostBlockRendering",         "M,G",  "Toggle ghost block rendering on/off", "Ghost Block Rendering");
    public static final ConfigHotkey TOGGLE_INFO_OVERLAY_RENDERING      = new ConfigHotkey("toggleInfoOverlayRendering",        "M,I",  "Toggle the info overlay rendering (for hovered block info)", "Info Overlay");
    public static final ConfigHotkey TOGGLE_MISMATCH_OVERLAY_RENDERING  = new ConfigHotkey("toggleMismatchOverlayRendering",    "M,E",  "Toggle the mismatch overlay rendering (for Schematic Verifier)", "Verifier Mismatch Overlay");
    public static final ConfigHotkey TOGGLE_OVERLAY_RENDERING           = new ConfigHotkey("toggleOverlayRendering",            "M,O",  "Toggle the block overlay rendering on/off", "All Block Overlay Rendering");
    public static final ConfigHotkey TOGGLE_OVERLAY_OUTLINE_RENDERING   = new ConfigHotkey("toggleOverlayOutlineRendering",     "M,B",  "Toggle the block overlay outline rendering on/off", "Block Overlay Outline Rendering");
    public static final ConfigHotkey TOGGLE_OVERLAY_SIDE_RENDERING      = new ConfigHotkey("toggleOverlaySideRendering",        "M,D",  "Toggle the block overlay side rendering on/off", "Block Overlay Sides/Quads Rendering");
    public static final ConfigHotkey TOGGLE_SELECTION_BOXES_RENDERING   = new ConfigHotkey("toggleSelectionBoxesRendering",     "M,X",  "Toggle selection boxes rendering on/off", "Selection Boxes Rendering");
    public static final ConfigHotkey TOGGLE_TRANSLUCENT_RENDERING       = new ConfigHotkey("toggleTranslucentRendering",        "M,U",  "Toggle translucent vs. opaque ghost block rendering", "Translucent Schematic Blocks Rendering");
    public static final ConfigHotkey TOOL_ENABLED_TOGGLE                = new ConfigHotkey("toolEnabledToggle",                 "M,T",  "The keybind to toggle the \"tool\" item functionality on/off");
    public static final ConfigHotkey TOOL_PLACE_CORNER_1                = new ConfigHotkey("toolPlaceCorner1",                  "BUTTON0", KeybindSettings.PRESS_ALLOWEXTRA, "The button to use while holding the \"tool\" item\nto place the primary/first corner");
    public static final ConfigHotkey TOOL_PLACE_CORNER_2                = new ConfigHotkey("toolPlaceCorner2",                  "BUTTON1", KeybindSettings.PRESS_ALLOWEXTRA, "The button to use while holding the \"tool\" item\nto place the second corner");
    public static final ConfigHotkey TOOL_SELECT_ELEMENTS               = new ConfigHotkey("toolSelectElements",                "BUTTON2", KeybindSettings.PRESS_ALLOWEXTRA, "The button to use to select corners or boxes\nwhile holding the \"tool\" item");

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            ADD_SELECTION_BOX,
            DELETE_SELECTION_BOX,
            EXECUTE_OPERATION,
            LAYER_MODE_NEXT,
            LAYER_MODE_PREVIOUS,
            LAYER_NEXT,
            LAYER_PREVIOUS,
            MOVE_ENTIRE_SELECTION,
            OPEN_GUI_AREA_SETTINGS,
            OPEN_GUI_PLACEMENT_SETTINGS,
            OPEN_GUI_MAIN_MENU,
            OPEN_GUI_MATERIAL_LIST,
            OPEN_GUI_SCHEMATIC_PLACEMENTS,
            OPEN_GUI_SCHEMATIC_VERIFIER,
            OPEN_GUI_SELECTION_MANAGER,
            OPEN_GUI_SETTINGS,
            OPERATION_MODE_CHANGE_MODIFIER,
            PICK_BLOCK_FIRST,
            PICK_BLOCK_LAST,
            PICK_BLOCK_TOGGLE,
            RENDER_INFO_OVERLAY,
            SAVE_AREA_AS_IN_MEMORY_SCHEMATIC,
            SAVE_AREA_AS_SCHEMATIC_TO_FILE,
            SELECTION_GRAB_MODIFIER,
            SELECTION_MODE_CYCLE,
            SET_AREA_ORIGIN,
            SET_SELECTION_BOX_POSITION_1,
            SET_SELECTION_BOX_POSITION_2,
            TOGGLE_ALL_RENDERING,
            TOGGLE_GHOST_BLOCK_RENDERING,
            TOGGLE_INFO_OVERLAY_RENDERING,
            TOGGLE_MISMATCH_OVERLAY_RENDERING,
            TOGGLE_OVERLAY_RENDERING,
            TOGGLE_OVERLAY_OUTLINE_RENDERING,
            TOGGLE_OVERLAY_SIDE_RENDERING,
            TOGGLE_SELECTION_BOXES_RENDERING,
            TOGGLE_TRANSLUCENT_RENDERING,
            TOOL_ENABLED_TOGGLE,
            TOOL_PLACE_CORNER_1,
            TOOL_PLACE_CORNER_2,
            TOOL_SELECT_ELEMENTS
    );
}
