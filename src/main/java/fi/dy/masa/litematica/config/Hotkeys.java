package fi.dy.masa.litematica.config;

import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;

public enum Hotkeys implements IHotkey
{
    ADD_SELECTION_BOX                   ("addSelectionBox",                 "M,A",  "Add a new selection box (position 1) here"),
    DELETE_SELECTION_BOX                ("deleteSelectionBox",              "M,D",  "Delete the currently selected box"),
    MOVE_ENTIRE_SELECTION               ("moveEntireSelection",             "M,V",  "Move the entire current selection here"),
    OPEN_GUI_AREA_SETTINGS              ("openGuiAreaSettings",             "M,P",  "Open the Area Settings GUI for the currently selected area"),
    OPEN_GUI_PLACEMENT_SETTINGS         ("openGuiPlacementSettings",        "M,P",  "Open the Placement Settings GUI for the currently selected placement"),
    OPEN_GUI_MAIN_MENU                  ("openGuiMainMenu",                 "M,C",  "Open the Litematica main GUI"),
    OPEN_GUI_SELECTION_MANAGER          ("openGuiSelectionManager",         "M,S",  "Open the area selection manager GUI"),
    OPERATION_MODE_CHANGE_MODIFIER      ("operationModeChangeModifier",     "LCONTROL", "The modifier key to quickly change the operation mode.\nHold this and scroll while holding the \"tool item\" to quickly cycle the mode."),
    SAVE_SCHEMATIC                      ("saveSchematic",                   "LCONTROL,LMENU,S",  "Save the current Area Selection as a Schematic"),
    SELECTION_GRAB_MODIFIER             ("selectionGrabModifier",           "LMENU", "The modifier key to be held while clicking\npick block to \"grab\" a selection box or corner."),
    SET_AREA_ORIGIN                     ("setAreaOrigin",                   "M,O",  "Set/move the origin point of the current selection here"),
    SET_SELECTION_BOX_POSITION_1        ("setSelectionBoxPosition1",        "M,1",  "Set the first position of the currently selected box to the player's position"),
    SET_SELECTION_BOX_POSITION_2        ("setSelectionBoxPosition2",        "M,2",  "Set the second position of the currently selected box to the player's position"),
    TOGGLE_ALL_RENDERING                ("toggleAllRendering",              "M,R",  "Toggle all rendering on/off"),
    TOGGLE_GHOST_BLOCK_RENDERING        ("toggleGhostBlockRendering",       "M,G",  "Toggle ghost block rendering on/off"),
    TOGGLE_SELECTION_BOXES_RENDERING    ("toggleSelectionBoxesRendering",   "M,B",  "Toggle selection boxes rendering on/off"),
    TOGGLE_TRANSLUCENT_RENDERING        ("toggleTranslucentRendering",      "M,U",  "Toggle translucent vs. opaque ghost block rendering"),
    TOGGLE_WIRE_FRAME_RENDERING         ("toggleWireFrameRendering",        "M,W",  "Toggle block wire frame rendering on/off"),
    TOOL_ENABLED_TOGGLE                 ("toolEnabledToggle",               "M,T",  "The keybind to toggle the \"tool\" item functionality on/off"),
    TOOL_PLACE_CORNER_1                 ("toolPlaceCorner1",                "BUTTON0", "The button to use while holding the \"tool\" item\nto place the primary/first corner"),
    TOOL_PLACE_CORNER_2                 ("toolPlaceCorner2",                "BUTTON1", "The button to use while holding the \"tool\" item\nto place the second corner"),
    TOOL_SELECT_ELEMENTS                ("toolSelectElements",              "BUTTON2", "The button to use to select corners or boxes while holding the \"tool\" item");

    private final String name;
    private final String comment;
    private final IKeybind keybind;

    private Hotkeys(String name, String defaultHotkey, String comment)
    {
        this.name = name;
        this.comment = comment;
        this.keybind = KeybindMulti.fromStorageString(defaultHotkey);
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getComment()
    {
        return comment != null ? this.comment : "";
    }

    @Override
    public IKeybind getKeybind()
    {
        return this.keybind;
    }
}
