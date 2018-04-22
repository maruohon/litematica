package fi.dy.masa.litematica.config;

import fi.dy.masa.litematica.config.hotkeys.IHotkey;
import fi.dy.masa.litematica.config.hotkeys.IKeybind;
import fi.dy.masa.litematica.config.hotkeys.KeybindMulti;

public enum Hotkeys implements IHotkey
{
    ADD_SELECTION_BOX                   ("addSelectionBox",                 "M,A",  "Add a new selection box"),
    DELETE_SELECTION_BOX                ("deleteSelectionBox",              "M,D",  "Delete the currently selected selection box"),
    OPEN_GUI_SELECTION_MANAGER          ("openGuiSelectionManager",         "M,S",  "Open the area selection manager GUI"),
    SELECTION_GRAB_MODIFIER             ("selectionGrabModifier",           "LMENU", "The modifier key to be held while clicking\npick block to \"grab\" a selection box or corner."),
    SET_AREA_ORIGIN                     ("setAreaOrigin",                   "M,O",  "Set the origin point of the currently selected area"),
    SET_SELECTION_BOX_POSITION_1        ("setSelectionBoxPosition1",        "M,1",  "Set the first position of the currently selected box to the player's position"),
    SET_SELECTION_BOX_POSITION_2        ("setSelectionBoxPosition2",        "M,2",  "Set the second position of the currently selected box to the player's position"),
    TOGGLE_ALL_RENDERING                ("toggleAllRendering",              "M,R",  "Toggle all rendering on/off"),
    TOGGLE_GHOST_BLOCK_RENDERING        ("toggleGhostBlockRendering",       "M,G",  "Toggle ghost block rendering on/off"),
    TOGGLE_SELECTION_BOXES_RENDERING    ("toggleSelectionBoxesRendering",   "M,B",  "Toggle selection boxes rendering on/off"),
    TOGGLE_WIRE_FRAME_RENDERING         ("toggleWireFrameRendering",        "M,W",  "Toggle block wire frame rendering on/off");

    private final String name;
    private final String comment;
    private IKeybind keybind;

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

    @Override
    public void setKeybind(IKeybind keybind)
    {
        this.keybind = keybind;
    }
}
