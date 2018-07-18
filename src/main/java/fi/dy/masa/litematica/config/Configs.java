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
        public static final ConfigOptionList    INFO_HUD_ALIGNMENT      = new ConfigOptionList( "infoHudAlignment", HudAlignment.BOTTOM_RIGHT, "The alignment of the \"info HUD\", used for schematic verifier mismatch positions etc.");
        public static final ConfigOptionList    SELECTION_MODE          = new ConfigOptionList( "selectionMode", AreaSelectionMode.CORNERS, "The area selection mode to use");
        public static final ConfigOptionList    TOOL_HUD_ALIGNMENT      = new ConfigOptionList( "toolHudAlignment", HudAlignment.BOTTOM_LEFT, "The alignment of the \"tool HUD\", when holding the configured \"tool\"");
        public static final ConfigString        TOOL_ITEM               = new ConfigString(     "toolItem", "minecraft:stick", "The item to use as the \"tool\" for selections etc.");
        public static final ConfigBoolean       TOOL_ITEM_ENABLED       = new ConfigBoolean(    "toolItemEnabled", true, "If true, then the \"tool\" item can be used to control selections etc.");
        public static final ConfigBoolean       VERBOSE_LOGGING         = new ConfigBoolean(    "verboseLogging", false, "If enabled, a bunch of debug messages will be printed to the console");

        public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(
                INFO_HUD_ALIGNMENT,
                SELECTION_MODE,
                TOOL_HUD_ALIGNMENT,
                TOOL_ITEM,
                TOOL_ITEM_ENABLED,
                VERBOSE_LOGGING
        );
    }

    public static class Visuals
    {
        public static final ConfigDouble        ERROR_HILIGHT_ALPHA             = new ConfigDouble(     "errorHilightAlpha", 0.2, 0, 1, "The alpha value of the error marker box sides");
        public static final ConfigInteger       ERROR_HILIGHT_MAX_POSITIONS     = new ConfigInteger(    "errorHilightMaxPositions", 1000, 1, 1000000, "The maximum number of mismatched positions to render at once");
        public static final ConfigDouble        GHOST_BLOCK_ALPHA               = new ConfigDouble(     "ghostBlockAlpha", 0.5, 0, 1, "The alpha value of the ghost blocks, when rendering them as translucent");
        public static final ConfigDouble        PLACEMENT_BOX_SIDE_ALPHA        = new ConfigDouble(     "placementBoxSideAlpha", 0.2, 0, 1, "The alpha value of the sub-region boxes' side");
        public static final ConfigBoolean       RENDER_BLOCKS_AS_TRANSLUCENT    = new ConfigBoolean(    "renderBlocksAsTranslucent", false, "If enabled, then the schematics are rendered using translucent \"ghost blocks\"");
        public static final ConfigBoolean       RENDER_ERROR_INFO_OVERLAY       = new ConfigBoolean(    "renderErrorInfoOverlay", true, "If enabled, then an info overlay is rendered while looking at an error marker, and holding the key for it");
        public static final ConfigBoolean       RENDER_ERROR_MARKER_SIDES       = new ConfigBoolean(    "renderErrorMarkerSides", true, "If enabled, then the error markers in the Schematic Verifier will have\n(translucent) sides rendered instead of just the outline");
        public static final ConfigBoolean       RENDER_SELECTION_BOX_SIDES      = new ConfigBoolean(    "renderSelectionBoxSides", true, "If enabled, then the area selection boxes will have their side quads rendered");
        public static final ConfigBoolean       RENDER_PLACEMENT_BOX_SIDES      = new ConfigBoolean(    "renderPlacementBoxSides", false, "If enabled, then the placed schematic sub-region boxes will have their side quads rendered");
        public static final ConfigColor         SELECTION_BOX_SIDE_COLOR        = new ConfigColor(      "selectionBoxSideColor", "0x30FFFFFF", "If enabled, then the area selection boxes will have their side quads rendered");

        public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(
                ERROR_HILIGHT_ALPHA,
                ERROR_HILIGHT_MAX_POSITIONS,
                GHOST_BLOCK_ALPHA,
                PLACEMENT_BOX_SIDE_ALPHA,
                RENDER_BLOCKS_AS_TRANSLUCENT,
                RENDER_ERROR_INFO_OVERLAY,
                RENDER_ERROR_MARKER_SIDES,
                RENDER_SELECTION_BOX_SIDES,
                RENDER_PLACEMENT_BOX_SIDES,
                SELECTION_BOX_SIDE_COLOR
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
