package fi.dy.masa.litematica.config;

import java.io.File;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mumfrey.liteloader.core.LiteLoader;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelectionMode;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigString;

public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

    public static class Generic
    {
        public static final ConfigBoolean       LOAD_ENTIRE_SCHEMATICS  = new ConfigBoolean(    "loadEntireSchematics", true, "If true, then the entire schematic is always loaded at once.\nIf false, then only the part that is within the client's view distance is loaded.");
        public static final ConfigBoolean       PICK_BLOCK_ENABLED      = new ConfigBoolean(    "pickBlockEnabled", true, "Enables the schematic world pick block hotkeys.\nThere is also a hotkey for toggling this option to toggle those hotkeys... o.o", "Pick Block Hotkeys");
        public static final ConfigOptionList    SELECTION_MODE          = new ConfigOptionList( "selectionMode", AreaSelectionMode.CORNERS, "The area selection mode to use");
        public static final ConfigOptionList    TOOL_HUD_ALIGNMENT      = new ConfigOptionList( "toolHudAlignment", HudAlignment.BOTTOM_LEFT, "The alignment of the \"tool HUD\", when holding the configured \"tool\"");
        public static final ConfigString        TOOL_ITEM               = new ConfigString(     "toolItem", "minecraft:stick", "The item to use as the \"tool\" for selections etc.");
        public static final ConfigBoolean       TOOL_ITEM_ENABLED       = new ConfigBoolean(    "toolItemEnabled", true, "If true, then the \"tool\" item can be used to control selections etc.");
        public static final ConfigBoolean       VERBOSE_LOGGING         = new ConfigBoolean(    "verboseLogging", false, "If enabled, a bunch of debug messages will be printed to the console");

        public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(
                LOAD_ENTIRE_SCHEMATICS,
                PICK_BLOCK_ENABLED,
                SELECTION_MODE,
                TOOL_HUD_ALIGNMENT,
                TOOL_ITEM,
                TOOL_ITEM_ENABLED,
                VERBOSE_LOGGING
        );
    }

    public static class Visuals
    {
        public static final ConfigColor         AREA_SELECTION_BOX_SIDE_COLOR   = new ConfigColor(      "areaSelectionBoxSideColor", "0x30FFFFFF", "The color of the area selection boxes, when they are unselected");
        public static final ConfigDouble        ERROR_HILIGHT_ALPHA             = new ConfigDouble(     "errorHilightAlpha", 0.2, 0, 1, "The alpha value of the error marker box sides");
        public static final ConfigInteger       ERROR_HILIGHT_MAX_POSITIONS     = new ConfigInteger(    "errorHilightMaxPositions", 1000, 1, 1000000, "The maximum number of mismatched positions to render at once");
        public static final ConfigDouble        GHOST_BLOCK_ALPHA               = new ConfigDouble(     "ghostBlockAlpha", 0.5, 0, 1, "The alpha value of the ghost blocks, when rendering them as translucent");
        public static final ConfigOptionList    INFO_HUD_ALIGNMENT              = new ConfigOptionList( "infoHudAlignment", HudAlignment.BOTTOM_RIGHT, "The alignment of the \"info HUD\", used for schematic verifier mismatch positions etc.");
        public static final ConfigInteger       INFO_HUD_MAX_LINES              = new ConfigInteger(    "infoHudMaxLines", 10, 1, 32, "The maximum number of info lines to show on the HUD at once");
        public static final ConfigDouble        PLACEMENT_BOX_SIDE_ALPHA        = new ConfigDouble(     "placementBoxSideAlpha", 0.2, 0, 1, "The alpha value of the sub-region boxes' side");
        public static final ConfigBoolean       RENDER_AREA_SELECTION_BOX_SIDES = new ConfigBoolean(    "renderAreaSelectionBoxSides", true, "If enabled, then the area selection boxes will have their side quads rendered");
        public static final ConfigBoolean       RENDER_BLOCKS_AS_TRANSLUCENT    = new ConfigBoolean(    "renderBlocksAsTranslucent", false, "If enabled, then the schematics are rendered using translucent \"ghost blocks\"");
        public static final ConfigBoolean       RENDER_INFO_OVERLAY             = new ConfigBoolean(    "renderInfoOverlay", true, "If enabled, then an info overlay is rendered while looking at schematic blocks\nor schematic verifier error markers, and holding the key for it");
        public static final ConfigBoolean       RENDER_ERROR_MARKER_SIDES       = new ConfigBoolean(    "renderErrorMarkerSides", true, "If enabled, then the error markers in the Schematic Verifier will have\n(translucent) sides rendered instead of just the outline");
        public static final ConfigBoolean       RENDER_PLACEMENT_BOX_SIDES      = new ConfigBoolean(    "renderPlacementBoxSides", false, "If enabled, then the placed schematic sub-region boxes will have their side quads rendered");
        public static final ConfigBoolean       RENDER_TRANSLUCENT_INNER_SIDES  = new ConfigBoolean(    "renderTranslucentBlockInnerSides", false, "If enabled, then the model sides are also rendered for inner sides in the translucent mode");
        public static final ConfigColor         SCHEMATIC_OVERLAY_COLOR_EXTRA       = new ConfigColor(  "schematicOverlayColorExtra",      "0x4CFF4CE6", "The color of the blocks overlay for extra blocks");
        public static final ConfigColor         SCHEMATIC_OVERLAY_COLOR_MISSING     = new ConfigColor(  "schematicOverlayColorMissing",    "0x2C33B3E6", "The color of the blocks overlay for missing blocks");
        public static final ConfigColor         SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK = new ConfigColor(  "schematicOverlayColorWrongBlock", "0x4CFF3333", "The color of the blocks overlay for wrong blocks");
        public static final ConfigColor         SCHEMATIC_OVERLAY_COLOR_WRONG_STATE = new ConfigColor(  "schematicOverlayColorWrongState", "0x4CFF9010", "The color of the blocks overlay for wrong block states");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_ENABLE_OUTLINES   = new ConfigBoolean("schematicOverlayEnableOutlines",  true, "Enables rendering a wire frame outline for the schematic block overlay");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_ENABLE_SIDES      = new ConfigBoolean("schematicOverlayEnableSides",     true, "Enables rendering translucent boxes/sides for the schematic block overlay");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_MODEL_OUTLINE     = new ConfigBoolean("schematicOverlayModelOutline",    true, "If enabled, then the schematic overlay will use the block model\nquads/vertices instead of the traditional full block overlay");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_MODEL_SIDES       = new ConfigBoolean("schematicOverlayModelSides",      true, "If enabled, then the schematic overlay will use the block model\nquads/vertices instead of the traditional full block overlay");
        public static final ConfigDouble        SCHEMATIC_OVERLAY_OUTLINE_WIDTH     = new ConfigDouble( "schematicOverlayOutlineWidth",  1.5, 0, 5, "The line width of the block (model) outlines");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_RENDER_THROUGH    = new ConfigBoolean("schematicOverlayRenderThroughBlocks", false, "If enabled, then the schematic overlay will be rendered through blocks\nThis is probably only useful once you are finished building\nand want to see any errors easier");

        public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(
                AREA_SELECTION_BOX_SIDE_COLOR,
                ERROR_HILIGHT_ALPHA,
                ERROR_HILIGHT_MAX_POSITIONS,
                GHOST_BLOCK_ALPHA,
                INFO_HUD_ALIGNMENT,
                INFO_HUD_MAX_LINES,
                PLACEMENT_BOX_SIDE_ALPHA,
                RENDER_AREA_SELECTION_BOX_SIDES,
                RENDER_BLOCKS_AS_TRANSLUCENT,
                RENDER_INFO_OVERLAY,
                RENDER_ERROR_MARKER_SIDES,
                RENDER_PLACEMENT_BOX_SIDES,
                RENDER_TRANSLUCENT_INNER_SIDES,
                SCHEMATIC_OVERLAY_COLOR_EXTRA,
                SCHEMATIC_OVERLAY_COLOR_MISSING,
                SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK,
                SCHEMATIC_OVERLAY_COLOR_WRONG_STATE,
                SCHEMATIC_OVERLAY_ENABLE_OUTLINES,
                SCHEMATIC_OVERLAY_ENABLE_SIDES,
                SCHEMATIC_OVERLAY_MODEL_OUTLINE,
                SCHEMATIC_OVERLAY_MODEL_SIDES,
                SCHEMATIC_OVERLAY_OUTLINE_WIDTH,
                SCHEMATIC_OVERLAY_RENDER_THROUGH
        );
    }

    public static void loadFromFile()
    {
        File configFile = new File(LiteLoader.getCommonConfigFolder(), CONFIG_FILE_NAME);

        if (configFile.exists() && configFile.isFile() && configFile.canRead())
        {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();

                ConfigUtils.readConfigValues(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigValues(root, "Visuals", Visuals.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", ImmutableList.copyOf(Hotkeys.values()));
            }
        }

        DataManager.setToolItem(Generic.TOOL_ITEM.getStringValue());
    }

    public static void saveToFile()
    {
        File dir = LiteLoader.getCommonConfigFolder();

        if (dir.exists() && dir.isDirectory())
        {
            JsonObject root = new JsonObject();

            ConfigUtils.writeConfigValues(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigValues(root, "Visuals", Visuals.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", ImmutableList.copyOf(Hotkeys.values()));

            JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
        }
    }

    @Override
    public void onConfigsChanged()
    {
        saveToFile();
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }
}
