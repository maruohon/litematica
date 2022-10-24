package fi.dy.masa.litematica.config;

import java.util.List;
import com.google.common.collect.ImmutableList;

import malilib.config.category.BaseConfigOptionCategory;
import malilib.config.category.ConfigOptionCategory;
import malilib.config.option.BooleanAndDoubleConfig;
import malilib.config.option.BooleanAndFileConfig;
import malilib.config.option.BooleanConfig;
import malilib.config.option.ColorConfig;
import malilib.config.option.ConfigOption;
import malilib.config.option.DoubleConfig;
import malilib.config.option.DualColorConfig;
import malilib.config.option.HotkeyedBooleanConfig;
import malilib.config.option.IntegerConfig;
import malilib.config.option.OptionListConfig;
import malilib.config.option.StringConfig;
import malilib.config.option.Vec2iConfig;
import malilib.config.value.FileBrowserColumns;
import malilib.config.value.HudAlignment;
import malilib.overlay.message.MessageOutput;
import malilib.util.position.Vec2i;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.gui.SaveSchematicFromAreaScreen.SaveSide;
import fi.dy.masa.litematica.selection.CornerSelectionMode;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.DefaultDirectories;
import fi.dy.masa.litematica.util.ReplaceBehavior;

public class Configs
{
    public static final int CURRENT_VERSION = 1;

    public static class Generic
    {
        public static final HotkeyedBooleanConfig EASY_PLACE_MODE                   = new HotkeyedBooleanConfig("easyPlaceMode", false, "");
        public static final HotkeyedBooleanConfig PICK_BLOCK_AUTO                   = new HotkeyedBooleanConfig("pickBlockAuto", false ,"");
        public static final HotkeyedBooleanConfig PICK_BLOCK_ENABLED                = new HotkeyedBooleanConfig("pickBlock", true, "");
        public static final HotkeyedBooleanConfig PLACEMENT_RESTRICTION             = new HotkeyedBooleanConfig("placementRestriction", false, "");
        public static final HotkeyedBooleanConfig SIGN_TEXT_PASTE                   = new HotkeyedBooleanConfig("signTextPaste", true, "");
        public static final HotkeyedBooleanConfig TOOL_ITEM_ENABLED                 = new HotkeyedBooleanConfig("toolItemEnabled", true, "M,T");

        public static final BooleanAndFileConfig CUSTOM_SCHEMATIC_DIRECTORY         = new BooleanAndFileConfig("customSchematicDirectory", false, DefaultDirectories.getDefaultSchematicDirectory());

        public static final BooleanConfig AREAS_PER_WORLD                           = new BooleanConfig("areaSelectionsPerWorld", true);
        public static final BooleanConfig BETTER_RENDER_ORDER                       = new BooleanConfig("betterRenderOrder", true);
        public static final BooleanConfig CHANGE_SELECTED_CORNER                    = new BooleanConfig("changeSelectedCornerOnMove", true);
        public static final BooleanConfig CLONE_AT_ORIGINAL_POS                     = new BooleanConfig("cloneAtOriginalPosition", true);
        public static final StringConfig  COMMAND_NAME_SETBLOCK                     = new StringConfig("commandNameSetblock", "setblock");
        public static final BooleanConfig DEBUG_MESSAGES                            = new BooleanConfig("debugMessages", false);
        public static final BooleanConfig EASY_PLACE_CLICK_ADJACENT                 = new BooleanConfig("easyPlaceClickAdjacent", false);
        public static final BooleanConfig EASY_PLACE_HOLD_ENABLED                   = new BooleanConfig("easyPlaceHold", false);
        public static final BooleanConfig EXECUTE_REQUIRE_TOOL                      = new BooleanConfig("executeRequireHoldingTool", true);
        public static final BooleanConfig FIX_RAIL_ROTATION                         = new BooleanConfig("fixRailRotation", true);
        public static final BooleanConfig GENERATE_LOWERCASE_NAMES                  = new BooleanConfig("generateLowerCaseNames", false);
        public static final BooleanConfig LOAD_ENTIRE_SCHEMATICS                    = new BooleanConfig("loadEntireSchematics", false);
        public static final BooleanConfig MATERIAL_LIST_IGNORE_BLOCK_STATE          = new BooleanConfig("materialListIgnoreBlockState", false);
        public static final BooleanConfig MATERIALS_FROM_CONTAINER                  = new BooleanConfig("materialListFromContainer", true);
        public static final IntegerConfig PASTE_COMMAND_INTERVAL                    = new IntegerConfig("pasteCommandInterval", 1, 1, 1000);
        public static final IntegerConfig PASTE_COMMAND_LIMIT                       = new IntegerConfig("pasteCommandLimit", 64, 1, 1000);
        public static final BooleanConfig PICK_BLOCK_IGNORE_NBT                     = new BooleanConfig("pickBlockIgnoreNBT", true);
        public static final StringConfig  PICK_BLOCKABLE_SLOTS                      = new StringConfig( "pickBlockableSlots", "6-9");
        public static final BooleanConfig PLACEMENTS_INFRONT                        = new BooleanConfig("placementInfrontOfPlayer", false);
        public static final BooleanConfig RENDER_MATERIALS_IN_GUI                   = new BooleanConfig("renderMaterialListInGuis", true);
        public static final BooleanConfig RENDER_THREAD_NO_TIMEOUT                  = new BooleanConfig("renderThreadNoTimeout", true);
        public static final BooleanConfig REQUIRE_ADJACENT_CHUNKS                   = new BooleanConfig("requireAdjacentChunks", true);
        public static final StringConfig  TOOL_ITEM                                 = new StringConfig( "toolItem", "minecraft:stick");

        public static final OptionListConfig<SelectionMode> DEFAULT_AREA_SELECTION_MODE   = new OptionListConfig<>("defaultAreaSelectionMode", SelectionMode.SIMPLE, SelectionMode.VALUES);
        public static final OptionListConfig<ReplaceBehavior> PASTE_REPLACE_BEHAVIOR        = new OptionListConfig<>("pasteReplaceBehavior", ReplaceBehavior.NONE, ReplaceBehavior.VALUES);
        public static final OptionListConfig<FileBrowserColumns> SCHEMATIC_BROWSER_COLUMNS  = new OptionListConfig<>("schematicBrowserColumns", FileBrowserColumns.MTIME, FileBrowserColumns.VALUES);
        public static final OptionListConfig<CornerSelectionMode> SELECTION_CORNERS_MODE    = new OptionListConfig<>("selectionCornersMode", CornerSelectionMode.CORNERS, CornerSelectionMode.VALUES);

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                AREAS_PER_WORLD,
                BETTER_RENDER_ORDER,
                CHANGE_SELECTED_CORNER,
                CLONE_AT_ORIGINAL_POS,
                COMMAND_NAME_SETBLOCK,
                CUSTOM_SCHEMATIC_DIRECTORY,
                DEBUG_MESSAGES,
                DEFAULT_AREA_SELECTION_MODE,
                EASY_PLACE_CLICK_ADJACENT,
                EASY_PLACE_HOLD_ENABLED,
                EASY_PLACE_MODE,
                EXECUTE_REQUIRE_TOOL,
                FIX_RAIL_ROTATION,
                GENERATE_LOWERCASE_NAMES,
                LOAD_ENTIRE_SCHEMATICS,
                MATERIAL_LIST_IGNORE_BLOCK_STATE,
                MATERIALS_FROM_CONTAINER,
                PASTE_REPLACE_BEHAVIOR,
                PASTE_COMMAND_INTERVAL,
                PASTE_COMMAND_LIMIT,
                PICK_BLOCK_AUTO,
                PICK_BLOCK_ENABLED,
                PICK_BLOCK_IGNORE_NBT,
                PICK_BLOCKABLE_SLOTS,
                PLACEMENT_RESTRICTION,
                PLACEMENTS_INFRONT,
                RENDER_MATERIALS_IN_GUI,
                RENDER_THREAD_NO_TIMEOUT,
                REQUIRE_ADJACENT_CHUNKS,
                SCHEMATIC_BROWSER_COLUMNS,
                SELECTION_CORNERS_MODE,
                SIGN_TEXT_PASTE,
                TOOL_ITEM_ENABLED,
                TOOL_ITEM
        );

        public static final ImmutableList<HotkeyedBooleanConfig> HOTKEYS = ImmutableList.of(
                EASY_PLACE_MODE,
                PICK_BLOCK_AUTO,
                PICK_BLOCK_ENABLED,
                PLACEMENT_RESTRICTION,
                SIGN_TEXT_PASTE,
                TOOL_ITEM_ENABLED
        );
    }

    public static class Visuals
    {
        public static final HotkeyedBooleanConfig AREA_SELECTION_BOX_SIDES              = new HotkeyedBooleanConfig("areaSelectionBoxSides", true, "");
        public static final HotkeyedBooleanConfig AREA_SELECTION_RENDERING              = new HotkeyedBooleanConfig("areaSelectionRendering", true, "");
        public static final HotkeyedBooleanConfig MAIN_RENDERING_TOGGLE                 = new HotkeyedBooleanConfig("mainRenderingToggle", true, "M,R");
        public static final HotkeyedBooleanConfig PLACEMENT_BOX_RENDERING               = new HotkeyedBooleanConfig("placementBoundingBoxRendering", true, "");
        public static final HotkeyedBooleanConfig RENDER_COLLIDING_BLOCK_AT_CURSOR      = new HotkeyedBooleanConfig("renderCollidingBlockAtCursor", false, "");
        public static final HotkeyedBooleanConfig RENDER_COLLIDING_SCHEMATIC_BLOCKS     = new HotkeyedBooleanConfig("renderCollidingSchematicBlocks", false, "");
        public static final HotkeyedBooleanConfig SCHEMATIC_BLOCKS_RENDERING            = new HotkeyedBooleanConfig("schematicBlocksRendering", true, "M,B");
        public static final HotkeyedBooleanConfig SCHEMATIC_OVERLAY                     = new HotkeyedBooleanConfig("schematicOverlayRendering", true, "");
        public static final HotkeyedBooleanConfig SCHEMATIC_RENDERING                   = new HotkeyedBooleanConfig("schematicRendering", true, "M,G");
        public static final HotkeyedBooleanConfig SCHEMATIC_OVERLAY_RENDER_THROUGH      = new HotkeyedBooleanConfig("schematicOverlayRenderThrough", false, "");
        public static final HotkeyedBooleanConfig SCHEMATIC_OVERLAY_TYPE_EXTRA          = new HotkeyedBooleanConfig("schematicOverlayTypeExtra", true, "");
        public static final HotkeyedBooleanConfig SCHEMATIC_OVERLAY_TYPE_MISSING        = new HotkeyedBooleanConfig("schematicOverlayTypeMissing", true, "");
        public static final HotkeyedBooleanConfig SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK    = new HotkeyedBooleanConfig("schematicOverlayTypeWrongBlock", true, "");
        public static final HotkeyedBooleanConfig SCHEMATIC_OVERLAY_TYPE_WRONG_STATE    = new HotkeyedBooleanConfig("schematicOverlayTypeWrongState", true, "");

        public static final BooleanAndDoubleConfig PLACEMENT_BOX_SIDES                  = new BooleanAndDoubleConfig("placementBoxSides", false, 0.2, 0.0, 1.0);
        public static final BooleanAndDoubleConfig PLACEMENT_ENCLOSING_BOX_SIDES        = new BooleanAndDoubleConfig("placementEnclosingBoxSides", false, 0.2, 0.0, 1.0);
        public static final BooleanAndDoubleConfig TRANSLUCENT_SCHEMATIC_RENDERING      = new BooleanAndDoubleConfig("translucentSchematicRendering", false, 0.5, 0.0, 1.0);

        public static final BooleanConfig IGNORE_EXISTING_FLUIDS                    = new BooleanConfig("ignoreExistingFluids", true);
        public static final BooleanConfig OVERLAY_REDUCED_INNER_SIDES               = new BooleanConfig("overlayReducedInnerSides", false);
        public static final BooleanConfig SCHEMATIC_OVERLAY_MODEL_OUTLINE           = new BooleanConfig("schematicOverlayModelOutline", true);
        public static final BooleanConfig SCHEMATIC_OVERLAY_MODEL_SIDES             = new BooleanConfig("schematicOverlayModelSides", true);
        public static final BooleanConfig SCHEMATIC_OVERLAY_OUTLINES                = new BooleanConfig("schematicOverlayOutlines", true);
        public static final DoubleConfig  SCHEMATIC_OVERLAY_OUTLINE_WIDTH           = new DoubleConfig( "schematicOverlayOutlineWidth", 1.0, 0.1, 64.0);
        public static final DoubleConfig  SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH   = new DoubleConfig( "schematicOverlayOutlineWidthThrough", 3.0, 0.1, 64.0);
        public static final BooleanConfig SCHEMATIC_OVERLAY_SIDES                   = new BooleanConfig("schematicOverlaySides", true);
        public static final BooleanConfig SCHEMATIC_VERIFIER_BLOCK_MODELS           = new BooleanConfig("schematicVerifierUseBlockModels", false);
        public static final BooleanConfig TRANSLUCENT_INNER_SIDES                   = new BooleanConfig("translucentBlockInnerSides", false);
        public static final BooleanConfig VERIFIER_HIGHLIGHT_CONNECTIONS            = new BooleanConfig("verifierHighlightConnections", false);
        public static final BooleanConfig VERIFIER_HIGHLIGHT_SIDES                  = new BooleanConfig("verifierHighlightSides", true);

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                AREA_SELECTION_BOX_SIDES,
                AREA_SELECTION_RENDERING,
                MAIN_RENDERING_TOGGLE,
                RENDER_COLLIDING_BLOCK_AT_CURSOR,
                RENDER_COLLIDING_SCHEMATIC_BLOCKS,
                SCHEMATIC_BLOCKS_RENDERING,
                SCHEMATIC_OVERLAY,
                SCHEMATIC_RENDERING,
                SCHEMATIC_OVERLAY_RENDER_THROUGH,
                SCHEMATIC_OVERLAY_TYPE_EXTRA,
                SCHEMATIC_OVERLAY_TYPE_MISSING,
                SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK,
                SCHEMATIC_OVERLAY_TYPE_WRONG_STATE,

                IGNORE_EXISTING_FLUIDS,
                OVERLAY_REDUCED_INNER_SIDES,
                PLACEMENT_BOX_RENDERING,
                PLACEMENT_BOX_SIDES,
                PLACEMENT_ENCLOSING_BOX_SIDES,
                SCHEMATIC_OVERLAY_MODEL_OUTLINE,
                SCHEMATIC_OVERLAY_MODEL_SIDES,
                SCHEMATIC_OVERLAY_OUTLINES,
                SCHEMATIC_OVERLAY_OUTLINE_WIDTH,
                SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH,
                SCHEMATIC_OVERLAY_SIDES,
                SCHEMATIC_VERIFIER_BLOCK_MODELS,
                TRANSLUCENT_INNER_SIDES,
                TRANSLUCENT_SCHEMATIC_RENDERING,
                VERIFIER_HIGHLIGHT_CONNECTIONS,
                VERIFIER_HIGHLIGHT_SIDES
        );

        public static final ImmutableList<HotkeyedBooleanConfig> HOTKEYS = ImmutableList.of(
                AREA_SELECTION_BOX_SIDES,
                AREA_SELECTION_RENDERING,
                MAIN_RENDERING_TOGGLE,
                PLACEMENT_BOX_RENDERING,
                RENDER_COLLIDING_BLOCK_AT_CURSOR,
                RENDER_COLLIDING_SCHEMATIC_BLOCKS,
                SCHEMATIC_BLOCKS_RENDERING,
                SCHEMATIC_OVERLAY,
                SCHEMATIC_RENDERING,
                SCHEMATIC_OVERLAY_RENDER_THROUGH,
                SCHEMATIC_OVERLAY_TYPE_EXTRA,
                SCHEMATIC_OVERLAY_TYPE_MISSING,
                SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK,
                SCHEMATIC_OVERLAY_TYPE_WRONG_STATE
        );
    }

    public static class InfoOverlays
    {
        public static final HotkeyedBooleanConfig BLOCK_INFO_LINES_RENDERING        = new HotkeyedBooleanConfig("blockInfoLines", true, "");
        public static final HotkeyedBooleanConfig BLOCK_INFO_OVERLAY_RENDERING      = new HotkeyedBooleanConfig("blockInfoOverlay", true, "");
        public static final HotkeyedBooleanConfig STATUS_INFO_HUD_RENDERING         = new HotkeyedBooleanConfig("statusInfoHud", false, "");
        public static final HotkeyedBooleanConfig VERIFIER_OVERLAY_RENDERING        = new HotkeyedBooleanConfig("verifierOverlay", true, "");

        public static final DoubleConfig  BLOCK_INFO_LINES_FONT_SCALE               = new DoubleConfig( "blockInfoLinesFontScale", 0.5, 0.0, 10.0);
        public static final Vec2iConfig   BLOCK_INFO_LINES_OFFSET                   = new Vec2iConfig(  "blockInfoLinesOffset", new Vec2i(4, 4));
        public static final IntegerConfig BLOCK_INFO_OVERLAY_OFFSET_Y               = new IntegerConfig("blockInfoOverlayOffsetY", 6, -2000, 2000);
        public static final IntegerConfig INFO_HUD_MAX_LINES                        = new IntegerConfig("infoHudMaxLines", 10, 1, 128);
        public static final Vec2iConfig   INFO_HUD_OFFSET                           = new Vec2iConfig(  "infoHudOffset", new Vec2i(1, 1));
        public static final DoubleConfig  INFO_HUD_SCALE                            = new DoubleConfig( "infoHudScale", 1.0, 0.1, 4.0);
        public static final IntegerConfig MATERIAL_LIST_HUD_MAX_LINES               = new IntegerConfig("materialListHudMaxLines", 10, 1, 128);
        public static final DoubleConfig  MATERIAL_LIST_HUD_SCALE                   = new DoubleConfig( "materialListHudScale", 1.0, 0.1, 4.0);
        public static final BooleanConfig MATERIAL_LIST_HUD_STACKS                  = new BooleanConfig("materialListHudStacks", true);
        public static final BooleanConfig MATERIAL_LIST_SLOT_HIGHLIGHT              = new BooleanConfig("materialListSlotHighlight", true);
        public static final BooleanConfig STATUS_INFO_HUD_AUTO                      = new BooleanConfig("statusInfoHudAuto", true);
        public static final BooleanConfig TOOL_HUD_ALWAYS_VISIBLE                   = new BooleanConfig("toolHudAlwaysVisible", false);
        public static final Vec2iConfig   TOOL_HUD_OFFSET                           = new Vec2iConfig(  "toolHudOffset", new Vec2i(1, 1));
        public static final DoubleConfig  TOOL_HUD_SCALE                            = new DoubleConfig( "toolHudScale", 1.0, 0.1, 4.0);
        public static final DoubleConfig  VERIFIER_ERROR_HIGHLIGHT_ALPHA            = new DoubleConfig("verifierErrorHighlightAlpha", 0.2, 0.0, 1.0);
        public static final IntegerConfig VERIFIER_ERROR_HIGHLIGHT_MAX_POSITIONS    = new IntegerConfig("verifierErrorHighlightMaxPositions", 1000, 1, 1000000);
        public static final BooleanConfig WARN_DISABLED_RENDERING                   = new BooleanConfig("warnDisabledRendering", true);

        public static final OptionListConfig<HudAlignment> BLOCK_INFO_LINES_ALIGNMENT           = new OptionListConfig<>("blockInfoLinesAlignment", HudAlignment.TOP_RIGHT, HudAlignment.VALUES);
        public static final OptionListConfig<BlockInfoAlignment> BLOCK_INFO_OVERLAY_ALIGNMENT   = new OptionListConfig<>("blockInfoOverlayAlignment", BlockInfoAlignment.TOP_CENTER, BlockInfoAlignment.VALUES);
        public static final OptionListConfig<MessageOutput> EASY_PLACE_WARNINGS                 = new OptionListConfig<>("easyPlaceWarnings", MessageOutput.MESSAGE_OVERLAY, MessageOutput.getValues());
        public static final OptionListConfig<HudAlignment> INFO_HUD_ALIGNMENT                   = new OptionListConfig<>("infoHudAlignment", HudAlignment.BOTTOM_RIGHT, HudAlignment.VALUES);
        public static final OptionListConfig<HudAlignment> TOOL_HUD_ALIGNMENT                   = new OptionListConfig<>("toolHudAlignment", HudAlignment.BOTTOM_LEFT, HudAlignment.VALUES);

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                BLOCK_INFO_LINES_RENDERING,
                BLOCK_INFO_OVERLAY_RENDERING,
                STATUS_INFO_HUD_RENDERING,
                VERIFIER_OVERLAY_RENDERING,

                MATERIAL_LIST_HUD_STACKS,
                MATERIAL_LIST_SLOT_HIGHLIGHT,
                STATUS_INFO_HUD_AUTO,
                TOOL_HUD_ALWAYS_VISIBLE,
                WARN_DISABLED_RENDERING,

                BLOCK_INFO_LINES_ALIGNMENT,
                BLOCK_INFO_OVERLAY_ALIGNMENT,
                EASY_PLACE_WARNINGS,
                INFO_HUD_ALIGNMENT,
                TOOL_HUD_ALIGNMENT,

                BLOCK_INFO_LINES_OFFSET,
                BLOCK_INFO_LINES_FONT_SCALE,
                BLOCK_INFO_OVERLAY_OFFSET_Y,
                INFO_HUD_MAX_LINES,
                INFO_HUD_OFFSET,
                INFO_HUD_SCALE,
                MATERIAL_LIST_HUD_MAX_LINES,
                MATERIAL_LIST_HUD_SCALE,
                TOOL_HUD_OFFSET,
                TOOL_HUD_SCALE,
                VERIFIER_ERROR_HIGHLIGHT_ALPHA,
                VERIFIER_ERROR_HIGHLIGHT_MAX_POSITIONS
        );

        public static final ImmutableList<HotkeyedBooleanConfig> HOTKEYS = ImmutableList.of(
                BLOCK_INFO_LINES_RENDERING,
                BLOCK_INFO_OVERLAY_RENDERING,
                STATUS_INFO_HUD_RENDERING,
                VERIFIER_OVERLAY_RENDERING
        );
    }

    public static class Colors
    {
        public static final ColorConfig AREA_SELECTION_BOX_SIDE             = new ColorConfig("areaSelectionBoxSide",                   "#30FFFFFF");
        public static final ColorConfig MATERIAL_LIST_HUD_ITEM_COUNTS       = new ColorConfig("materialListHudItemCounts",              "#FFFFAA00");
        public static final ColorConfig MATERIAL_LIST_SLOT_HL_LT_STACK      = new ColorConfig("materialListSlotHighlightUnderStack",    "#80FF40D0");
        public static final ColorConfig MATERIAL_LIST_SLOT_HL_NONE          = new ColorConfig("materialListSlotHighlightNone",          "#80FF2000");
        public static final ColorConfig MATERIAL_LIST_SLOT_HL_NOT_ENOUGH    = new ColorConfig("materialListSlotHighlightNotEnough",     "#80FFE040");
        public static final ColorConfig REBUILD_BREAK_OVERLAY               = new ColorConfig("schematicEditBreakPlaceOverlay",         "#4C33CC33");
        public static final ColorConfig REBUILD_REPLACE_OVERLAY             = new ColorConfig("schematicEditReplaceOverlay",            "#4CF0A010");
        public static final ColorConfig SCHEMATIC_OVERLAY_EXTRA             = new ColorConfig("schematicOverlayExtra",                  "#4CFF4CE6");
        public static final ColorConfig SCHEMATIC_OVERLAY_MISSING           = new ColorConfig("schematicOverlayMissing",                "#2C33B3E6");
        public static final ColorConfig SCHEMATIC_OVERLAY_WRONG_BLOCK       = new ColorConfig("schematicOverlayWrongBlock",             "#4CFF3333");
        public static final ColorConfig SCHEMATIC_OVERLAY_WRONG_STATE       = new ColorConfig("schematicOverlayWrongState",             "#4CFF9010");
        public static final DualColorConfig VERIFIER_CORRECT                = new DualColorConfig("verifierCorrect",                    "#4C11FF11", "#FF55FF55");
        public static final DualColorConfig VERIFIER_EXTRA                  = new DualColorConfig("verifierExtra",                      "#4CFF00CF", "#FFFF55FF");
        public static final DualColorConfig VERIFIER_MISSING                = new DualColorConfig("verifierMissing",                    "#4C00FFFF", "#FF55FFFF");
        public static final DualColorConfig VERIFIER_WRONG_BLOCK            = new DualColorConfig("verifierWrongBlock",                 "#4CFF0000", "#FFFF5555");
        public static final DualColorConfig VERIFIER_WRONG_STATE            = new DualColorConfig("verifierWrongState",                 "#4CFFAF00", "#FFFFAA00");

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                AREA_SELECTION_BOX_SIDE,
                MATERIAL_LIST_HUD_ITEM_COUNTS,
                MATERIAL_LIST_SLOT_HL_LT_STACK,
                MATERIAL_LIST_SLOT_HL_NONE,
                MATERIAL_LIST_SLOT_HL_NOT_ENOUGH,
                REBUILD_BREAK_OVERLAY,
                REBUILD_REPLACE_OVERLAY,
                SCHEMATIC_OVERLAY_EXTRA,
                SCHEMATIC_OVERLAY_MISSING,
                SCHEMATIC_OVERLAY_WRONG_BLOCK,
                SCHEMATIC_OVERLAY_WRONG_STATE,
                VERIFIER_CORRECT,
                VERIFIER_EXTRA,
                VERIFIER_MISSING,
                VERIFIER_WRONG_BLOCK,
                VERIFIER_WRONG_STATE
        );
    }

    // Configs that are not shown in the config GUI
    public static class Internal
    {
        public static final BooleanConfig CREATE_PLACEMENT_ON_LOAD      = new BooleanConfig("createPlacementOnLoad", true);
        public static final BooleanConfig PLACEMENT_LIST_ICON_BUTTONS   = new BooleanConfig("placementListIconButtons", false);
        public static final BooleanConfig SAVE_WITH_CUSTOM_SETTINGS     = new BooleanConfig("saveWithCustomSettings", false);
        public static final BooleanConfig SCHEMATIC_LIST_ICON_BUTTONS   = new BooleanConfig("schematicListIconButtons", false);
        public static final OptionListConfig<SaveSide> SAVE_SIDE        = new OptionListConfig<>("saveSide", SaveSide.AUTO, SaveSide.VALUES);

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                CREATE_PLACEMENT_ON_LOAD,
                PLACEMENT_LIST_ICON_BUTTONS,
                SAVE_WITH_CUSTOM_SETTINGS,
                SAVE_SIDE,
                SCHEMATIC_LIST_ICON_BUTTONS
        );
    }

    public static final List<ConfigOptionCategory> CATEGORIES = ImmutableList.of(
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "Generic",      Generic.OPTIONS),
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "InfoOverlays", InfoOverlays.OPTIONS),
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "Internal",     Internal.OPTIONS),
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "Visuals",      Visuals.OPTIONS),
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "Colors",       Colors.OPTIONS),
            BaseConfigOptionCategory.normal(Reference.MOD_INFO, "Hotkeys",      Hotkeys.HOTKEY_LIST)
    );

    public static void init()
    {
        setVerifierColorTooltips(Colors.VERIFIER_CORRECT);
        setVerifierColorTooltips(Colors.VERIFIER_EXTRA);
        setVerifierColorTooltips(Colors.VERIFIER_MISSING);
        setVerifierColorTooltips(Colors.VERIFIER_WRONG_BLOCK);
        setVerifierColorTooltips(Colors.VERIFIER_WRONG_STATE);
    }

    private static void setVerifierColorTooltips(DualColorConfig config)
    {
        config.setFirstColorHoverInfoKey("litematica.hover.schematic_verifier.color_config.overlay_color");
        config.setSecondColorHoverInfoKey("litematica.hover.schematic_verifier.color_config.text_color");
    }
}
