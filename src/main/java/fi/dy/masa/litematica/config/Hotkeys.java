package fi.dy.masa.litematica.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;

public enum Hotkeys implements IHotkey
{
    ADD_SELECTION_BOX                   ("addSelectionBox",                 "M,A",  "Add a new selection box (position 1) here"),
    DELETE_SELECTION_BOX                ("deleteSelectionBox",              "M,D",  "Delete the currently selected box"),
    EXECUTE_OPERATION                   ("executeOperation",                "LCONTROL,LMENU,R",  "Execute the current operation with the current selection or placement\nin the Fill, Replace, Paste Schematic etc. modes"),
    LAYER_MODE_NEXT                     ("layerModeNext",                   "M,PRIOR", "Cycle the rendering mode (all, layers) forward"),
    LAYER_MODE_PREVIOUS                 ("layerModePrevious",               "M,NEXT",  "Cycle the rendering mode (all, layers) backwards"),
    LAYER_NEXT                          ("layerNext",                       "PRIOR", "Move the rendered layer selection up"),
    LAYER_PREVIOUS                      ("layerPrevious",                   "NEXT",  "Move the rendered layer selection down"),
    MOVE_ENTIRE_SELECTION               ("moveEntireSelection",             "M,V",  "Move the entire current selection here"),
    OPEN_GUI_AREA_SETTINGS              ("openGuiAreaSettings",             "M,P",  "Open the Area Settings GUI for the currently selected area"),
    OPEN_GUI_PLACEMENT_SETTINGS         ("openGuiPlacementSettings",        "M,P",  "Open the Placement Settings GUI for the currently selected placement"),
    OPEN_GUI_MAIN_MENU                  ("openGuiMainMenu",                 "M,C",  "Open the Litematica main GUI"),
    OPEN_GUI_SELECTION_MANAGER          ("openGuiSelectionManager",         "M,S",  "Open the area selection manager GUI"),
    OPERATION_MODE_CHANGE_MODIFIER      ("operationModeChangeModifier",     "LCONTROL", "The modifier key to quickly change the operation mode.\nHold this and scroll while holding the \"tool item\" to quickly cycle the mode."),
    RENDER_INFO_OVERLAY                 ("renderInfoOverlay",               "LMENU", false, "The key that enables rendering the block info overlay.\nUse NONE for not requiring a key to be pressed.\nDisable the similarly named option in Visuals to disable the overlay completely."),
    SAVE_AREA_AS_IN_MEMORY_SCHEMATIC    ("saveAreaAsInMemorySchematic",     "LCONTROL,S",  "Save the current Area Selection as an in-memory Schematic"),
    SAVE_AREA_AS_SCHEMATIC_TO_FILE      ("saveAreaAsSchematicToFile",       "LCONTROL,LMENU,S",  "Save the current Area Selection as a Schematic to a file"),
    SCHEMATIC_VERIFIER                  ("schematicVerifier",               "M,K",  "The keybind to open the Schematic Verifier GUI for the selected placement"),
    SELECTION_GRAB_MODIFIER             ("selectionGrabModifier",           "LMENU", "The modifier key to be held while clicking\npick block to \"grab\" a selection box or corner."),
    SELECTION_MODE_CYCLE                ("selectionModeCycle",              "LCONTROL,M", "The keybind to change the selection mode/type in the Area Selection mode"),
    SET_AREA_ORIGIN                     ("setAreaOrigin",                   "M,O",  "Set/move the origin point of the current selection here"),
    SET_SELECTION_BOX_POSITION_1        ("setSelectionBoxPosition1",        "M,1",  "Set the first position of the currently selected box to the player's position"),
    SET_SELECTION_BOX_POSITION_2        ("setSelectionBoxPosition2",        "M,2",  "Set the second position of the currently selected box to the player's position"),
    TOGGLE_ALL_RENDERING                ("toggleAllRendering",              "M,R",  "Toggle all rendering on/off"),
    TOGGLE_GHOST_BLOCK_RENDERING        ("toggleGhostBlockRendering",       "M,G",  "Toggle ghost block rendering on/off"),
    TOGGLE_MISMATCH_OVERLAY_RENDERING   ("toggleMismatchOverlayRendering",  "M,E",  "Toggle the mismatch overlay rendering (for Schematic Verifier)"),
    TOGGLE_SELECTION_BOXES_RENDERING    ("toggleSelectionBoxesRendering",   "M,B",  "Toggle selection boxes rendering on/off"),
    TOGGLE_TRANSLUCENT_RENDERING        ("toggleTranslucentRendering",      "M,U",  "Toggle translucent vs. opaque ghost block rendering"),
    TOGGLE_WIRE_FRAME_RENDERING         ("toggleWireFrameRendering",        "M,W",  "Toggle block wire frame rendering on/off"),
    TOOL_ENABLED_TOGGLE                 ("toolEnabledToggle",               "M,T",  "The keybind to toggle the \"tool\" item functionality on/off"),
    TOOL_PLACE_CORNER_1                 ("toolPlaceCorner1",                "BUTTON0", false, "The button to use while holding the \"tool\" item\nto place the primary/first corner"),
    TOOL_PLACE_CORNER_2                 ("toolPlaceCorner2",                "BUTTON1", false, "The button to use while holding the \"tool\" item\nto place the second corner"),
    TOOL_SELECT_ELEMENTS                ("toolSelectElements",              "BUTTON2", false, "The button to use to select corners or boxes while holding the \"tool\" item");

    private final String name;
    private final String comment;
    private final IKeybind keybind;

    private Hotkeys(String name, String defaultHotkey, String comment)
    {
        this(name, defaultHotkey, true, comment);
    }

    private Hotkeys(String name, String defaultHotkey, boolean isStrict, String comment)
    {
        this.name = name;
        this.comment = comment;
        this.keybind = KeybindMulti.fromStorageString(defaultHotkey);
        this.keybind.setIsStrict(isStrict);
    }

    @Override
    public ConfigType getType()
    {
        return ConfigType.HOTKEY;
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
    public JsonElement getAsJsonElement()
    {
        return new JsonPrimitive(this.keybind.getStringValue());
    }

    @Override
    public void setValueFromJsonElement(JsonElement element)
    {
        try
        {
            if (element.isJsonPrimitive())
            {
                this.keybind.setValueFromString(element.getAsString());
            }
            else
            {
                LiteModLitematica.logger.warn("Failed to set the keybinds for '{}' from the JSON element '{}'", this.getName(), element);
            }
        }
        catch (Exception e)
        {
            LiteModLitematica.logger.warn("Failed to set the keybinds for '{}' from the JSON element '{}'", this.getName(), element, e);
        }
    }
}
