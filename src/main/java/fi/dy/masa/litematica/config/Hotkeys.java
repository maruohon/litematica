package fi.dy.masa.litematica.config;

import fi.dy.masa.litematica.config.hotkeys.IHotkey;
import fi.dy.masa.litematica.config.hotkeys.IKeybind;
import fi.dy.masa.litematica.config.hotkeys.KeybindMulti;

public enum Hotkeys implements IHotkey
{
    ADD_POSITION_1                      ("addPosition1",                    "M,1",  "Add/set the first position of the currently selected box to the player's position"),
    ADD_POSITION_2                      ("addPosition2",                    "M,2",  "Add/set the second position of the currently selected box to the player's position"),
    OPEN_GUI_SELECTION_MANAGER          ("openGuiSelectionManager",         "M,S",  "Open the area selection manager GUI"),
    TOGGLE_ALL_RENDERING                ("toggleAllRendering",              "M,R",  "Toggle all rendering on/off"),
    TOGGLE_GHOST_BLOCK_RENDERING        ("toggleGhostBlockRendering",       "M,G",  "Toggle ghost block rendering on/off"),
    TOGGLE_WIRE_FRAME_RENDERING         ("toggleWireFrameRendering",        "M,W",  "Toggle block wire frame rendering on/off"),
    TOGGLE_SELECTION_BOXES_RENDERING    ("toggleSelectionBoxesRendering",   "M,B",  "Toggle selection boxes rendering on/off");

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
