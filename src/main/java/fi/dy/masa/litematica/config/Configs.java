package fi.dy.masa.litematica.config;

import java.io.File;
import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.selection.CornerSelectionMode;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.malilib.config.option.ConfigOption;
import fi.dy.masa.malilib.config.category.BaseConfigOptionCategory;
import fi.dy.masa.malilib.config.category.ConfigOptionCategory;
import fi.dy.masa.malilib.config.option.BooleanConfig;
import fi.dy.masa.malilib.config.option.ColorConfig;
import fi.dy.masa.malilib.config.option.DirectoryConfig;
import fi.dy.masa.malilib.config.option.DoubleConfig;
import fi.dy.masa.malilib.config.option.IntegerConfig;
import fi.dy.masa.malilib.config.option.OptionListConfig;
import fi.dy.masa.malilib.config.option.StringConfig;
import fi.dy.masa.malilib.config.value.HudAlignment;
import fi.dy.masa.malilib.overlay.message.MessageOutput;
import fi.dy.masa.malilib.util.FileUtils;

public class Configs
{
    public static class Generic
    {
        public static final BooleanConfig AREAS_PER_WORLD                   = new BooleanConfig("areaSelectionsPerWorld", true);
        public static final BooleanConfig BETTER_RENDER_ORDER               = new BooleanConfig("betterRenderOrder", true);
        public static final BooleanConfig CHANGE_SELECTED_CORNER            = new BooleanConfig("changeSelectedCornerOnMove", true);
        public static final BooleanConfig CLONE_AT_ORIGINAL_POS             = new BooleanConfig("cloneAtOriginalPosition", false);
        public static final BooleanConfig CUSTOM_SCHEMATIC_DIR_ENABLED      = new BooleanConfig("useCustomSchematicDirectory", false);
        public static final DirectoryConfig CUSTOM_SCHEMATIC_DIRECTORY      = new DirectoryConfig("customSchematicDirectory", FileUtils.getCanonicalFileIfPossible(new File(FileUtils.getMinecraftDirectory(), "schematics")));
        public static final BooleanConfig DEBUG_MESSAGES                    = new BooleanConfig("debugMessages", false);
        public static final BooleanConfig EASY_PLACE_CLICK_ADJACENT         = new BooleanConfig("easyPlaceClickAdjacent", false);
        public static final BooleanConfig EASY_PLACE_MODE                   = new BooleanConfig("easyPlaceMode", false);
        public static final BooleanConfig EASY_PLACE_HOLD_ENABLED           = new BooleanConfig("easyPlaceHold", false);
        public static final BooleanConfig EXECUTE_REQUIRE_TOOL              = new BooleanConfig("executeRequireHoldingTool", true);
        public static final BooleanConfig FIX_RAIL_ROTATION                 = new BooleanConfig("fixRailRotation", true);
        public static final BooleanConfig GENERATE_LOWERCASE_NAMES          = new BooleanConfig("generateLowercaseNames", true);
        public static final BooleanConfig LOAD_ENTIRE_SCHEMATICS            = new BooleanConfig("loadEntireSchematics", false);
        public static final BooleanConfig MATERIAL_LIST_IGNORE_BLOCK_STATE  = new BooleanConfig("materialListIgnoreBlockState", false);
        public static final BooleanConfig MATERIALS_FROM_CONTAINER          = new BooleanConfig("materialListFromContainer", true);
        public static final IntegerConfig PASTE_COMMAND_INTERVAL            = new IntegerConfig("pasteCommandInterval", 1, 1, 1000);
        public static final IntegerConfig PASTE_COMMAND_LIMIT               = new IntegerConfig("pasteCommandLimit", 64, 1, 1000);
        public static final StringConfig PASTE_COMMAND_SETBLOCK             = new StringConfig("pasteCommandNameSetblock", "setblock");
        public static final BooleanConfig PICK_BLOCK_AUTO                   = new BooleanConfig("pickBlockAuto", false);
        public static final BooleanConfig PICK_BLOCK_ENABLED                = new BooleanConfig("pickBlockEnabled", true);
        public static final BooleanConfig PICK_BLOCK_IGNORE_NBT             = new BooleanConfig("pickBlockIgnoreNBT", true);
        public static final StringConfig PICK_BLOCKABLE_SLOTS               = new StringConfig("pickBlockableSlots", "6-9");
        public static final BooleanConfig PLACEMENT_RESTRICTION             = new BooleanConfig("placementRestriction", false);
        public static final BooleanConfig PLACEMENTS_INFRONT                = new BooleanConfig("placementInfrontOfPlayer", false);
        public static final BooleanConfig RENDER_MATERIALS_IN_GUI           = new BooleanConfig("renderMaterialListInGuis", true);
        public static final BooleanConfig RENDER_THREAD_NO_TIMEOUT          = new BooleanConfig("renderThreadNoTimeout", true);
        public static final BooleanConfig SIGN_TEXT_PASTE                   = new BooleanConfig("signTextPaste", true);
        public static final StringConfig TOOL_ITEM                          = new StringConfig("toolItem", "minecraft:stick");
        public static final BooleanConfig TOOL_ITEM_ENABLED                 = new BooleanConfig("toolItemEnabled", true);

        public static final OptionListConfig<ReplaceBehavior> PASTE_REPLACE_BEHAVIOR        = new OptionListConfig<>("pasteReplaceBehavior", ReplaceBehavior.NONE, ReplaceBehavior.VALUES);
        public static final OptionListConfig<CornerSelectionMode> SELECTION_CORNERS_MODE    = new OptionListConfig<>("selectionCornersMode", CornerSelectionMode.CORNERS, CornerSelectionMode.VALUES);

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                AREAS_PER_WORLD,
                BETTER_RENDER_ORDER,
                CHANGE_SELECTED_CORNER,
                CLONE_AT_ORIGINAL_POS,
                CUSTOM_SCHEMATIC_DIR_ENABLED,
                DEBUG_MESSAGES,
                EASY_PLACE_CLICK_ADJACENT,
                EASY_PLACE_MODE,
                EASY_PLACE_HOLD_ENABLED,
                EXECUTE_REQUIRE_TOOL,
                FIX_RAIL_ROTATION,
                GENERATE_LOWERCASE_NAMES,
                LOAD_ENTIRE_SCHEMATICS,
                MATERIALS_FROM_CONTAINER,
                PICK_BLOCK_AUTO,
                PICK_BLOCK_ENABLED,
                PICK_BLOCK_IGNORE_NBT,
                PLACEMENT_RESTRICTION,
                PLACEMENTS_INFRONT,
                RENDER_MATERIALS_IN_GUI,
                RENDER_THREAD_NO_TIMEOUT,
                SIGN_TEXT_PASTE,
                TOOL_ITEM_ENABLED,
                MATERIAL_LIST_IGNORE_BLOCK_STATE,

                PASTE_REPLACE_BEHAVIOR,
                SELECTION_CORNERS_MODE,

                CUSTOM_SCHEMATIC_DIRECTORY,
                PASTE_COMMAND_INTERVAL,
                PASTE_COMMAND_LIMIT,
                PASTE_COMMAND_SETBLOCK,
                PICK_BLOCKABLE_SLOTS,
                TOOL_ITEM
        );
    }

    public static class Visuals
    {
        public static final BooleanConfig ENABLE_AREA_SELECTION_RENDERING           = new BooleanConfig("enableAreaSelectionBoxesRendering", true);
        public static final BooleanConfig ENABLE_PLACEMENT_BOXES_RENDERING          = new BooleanConfig("enablePlacementBoxesRendering", true);
        public static final BooleanConfig ENABLE_RENDERING                          = new BooleanConfig("enableRendering", true);
        public static final BooleanConfig ENABLE_SCHEMATIC_BLOCKS                   = new BooleanConfig("enableSchematicBlocksRendering", true);
        public static final BooleanConfig ENABLE_SCHEMATIC_OVERLAY                  = new BooleanConfig("enableSchematicOverlay", true);
        public static final BooleanConfig ENABLE_SCHEMATIC_RENDERING                = new BooleanConfig("enableSchematicRendering", true);
        public static final BooleanConfig IGNORE_EXISTING_FLUIDS                    = new BooleanConfig("ignoreExistingFluids", false);
        public static final BooleanConfig OVERLAY_REDUCED_INNER_SIDES               = new BooleanConfig("overlayReducedInnerSides", false);
        public static final DoubleConfig PLACEMENT_BOX_SIDE_ALPHA                   = new DoubleConfig("placementBoxSideAlpha", 0.2, 0.0, 1.0);
        public static final BooleanConfig RENDER_AREA_SELECTION_BOX_SIDES           = new BooleanConfig("renderAreaSelectionBoxSides", true);
        public static final BooleanConfig RENDER_COLLIDING_BLOCK_AT_CURSOR          = new BooleanConfig("renderCollidingBlockAtCursor", false);
        public static final BooleanConfig RENDER_COLLIDING_SCHEMATIC_BLOCKS         = new BooleanConfig("renderCollidingSchematicBlocks", false);
        public static final BooleanConfig RENDER_ERROR_MARKER_CONNECTIONS           = new BooleanConfig("renderErrorMarkerConnections", false);
        public static final BooleanConfig RENDER_ERROR_MARKER_SIDES                 = new BooleanConfig("renderErrorMarkerSides", true);
        public static final BooleanConfig RENDER_PLACEMENT_BOX_SIDES                = new BooleanConfig("renderPlacementBoxSides", false);
        public static final BooleanConfig RENDER_PLACEMENT_ENCLOSING_BOX            = new BooleanConfig("renderPlacementEnclosingBox", true);
        public static final BooleanConfig RENDER_PLACEMENT_ENCLOSING_BOX_SIDES      = new BooleanConfig("renderPlacementEnclosingBoxSides", false);
        public static final BooleanConfig RENDER_TRANSLUCENT_INNER_SIDES            = new BooleanConfig("renderTranslucentBlockInnerSides", false);
        public static final BooleanConfig SCHEMATIC_OVERLAY_ENABLE_OUTLINES         = new BooleanConfig("schematicOverlayEnableOutlines", true);
        public static final BooleanConfig SCHEMATIC_OVERLAY_ENABLE_SIDES            = new BooleanConfig("schematicOverlayEnableSides", true);
        public static final BooleanConfig SCHEMATIC_OVERLAY_MODEL_OUTLINE           = new BooleanConfig("schematicOverlayModelOutline", true);
        public static final BooleanConfig SCHEMATIC_OVERLAY_MODEL_SIDES             = new BooleanConfig("schematicOverlayModelSides", true);
        public static final DoubleConfig SCHEMATIC_OVERLAY_OUTLINE_WIDTH            = new DoubleConfig("schematicOverlayOutlineWidth", 1.0, 0.1, 64.0);
        public static final DoubleConfig SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH    = new DoubleConfig("schematicOverlayOutlineWidthThrough", 3.0, 0.1, 64.0);
        public static final BooleanConfig SCHEMATIC_OVERLAY_RENDER_THROUGH          = new BooleanConfig("schematicOverlayRenderThroughBlocks", false);
        public static final BooleanConfig SCHEMATIC_OVERLAY_TYPE_EXTRA              = new BooleanConfig("schematicOverlayTypeExtra", true);
        public static final BooleanConfig SCHEMATIC_OVERLAY_TYPE_MISSING            = new BooleanConfig("schematicOverlayTypeMissing", true);
        public static final BooleanConfig SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK        = new BooleanConfig("schematicOverlayTypeWrongBlock", true);
        public static final BooleanConfig SCHEMATIC_OVERLAY_TYPE_WRONG_STATE        = new BooleanConfig("schematicOverlayTypeWrongState", true);
        public static final BooleanConfig SCHEMATIC_VERIFIER_BLOCK_MODELS           = new BooleanConfig("schematicVerifierUseBlockModels", false);
        public static final DoubleConfig GHOST_BLOCK_ALPHA                          = new DoubleConfig("translucentSchematicAlpha", 0.5, 0.0, 1.0);
        public static final BooleanConfig RENDER_BLOCKS_AS_TRANSLUCENT              = new BooleanConfig("translucentSchematicEnabled", false);

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                ENABLE_RENDERING,
                ENABLE_SCHEMATIC_RENDERING,

                ENABLE_AREA_SELECTION_RENDERING,
                ENABLE_PLACEMENT_BOXES_RENDERING,
                ENABLE_SCHEMATIC_BLOCKS,
                ENABLE_SCHEMATIC_OVERLAY,
                IGNORE_EXISTING_FLUIDS,
                OVERLAY_REDUCED_INNER_SIDES,
                RENDER_AREA_SELECTION_BOX_SIDES,
                RENDER_BLOCKS_AS_TRANSLUCENT,
                RENDER_COLLIDING_BLOCK_AT_CURSOR,
                RENDER_COLLIDING_SCHEMATIC_BLOCKS,
                RENDER_ERROR_MARKER_CONNECTIONS,
                RENDER_ERROR_MARKER_SIDES,
                RENDER_PLACEMENT_BOX_SIDES,
                RENDER_PLACEMENT_ENCLOSING_BOX,
                RENDER_PLACEMENT_ENCLOSING_BOX_SIDES,
                RENDER_TRANSLUCENT_INNER_SIDES,
                SCHEMATIC_OVERLAY_ENABLE_OUTLINES,
                SCHEMATIC_OVERLAY_ENABLE_SIDES,
                SCHEMATIC_OVERLAY_MODEL_OUTLINE,
                SCHEMATIC_OVERLAY_MODEL_SIDES,
                SCHEMATIC_OVERLAY_RENDER_THROUGH,
                SCHEMATIC_OVERLAY_TYPE_EXTRA,
                SCHEMATIC_OVERLAY_TYPE_MISSING,
                SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK,
                SCHEMATIC_OVERLAY_TYPE_WRONG_STATE,
                SCHEMATIC_VERIFIER_BLOCK_MODELS,

                GHOST_BLOCK_ALPHA,
                PLACEMENT_BOX_SIDE_ALPHA,
                SCHEMATIC_OVERLAY_OUTLINE_WIDTH,
                SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH
        );
    }

    public static class InfoOverlays
    {
        public static final BooleanConfig BLOCK_INFO_LINES_ENABLED                  = new BooleanConfig("blockInfoLinesEnabled", true);
        public static final DoubleConfig BLOCK_INFO_LINES_FONT_SCALE                = new DoubleConfig("blockInfoLinesFontScale", 0.5, 0.0, 10.0);
        public static final IntegerConfig BLOCK_INFO_LINES_OFFSET_X                 = new IntegerConfig("blockInfoLinesOffsetX", 4, 0, 2000);
        public static final IntegerConfig BLOCK_INFO_LINES_OFFSET_Y                 = new IntegerConfig("blockInfoLinesOffsetY", 4, 0, 2000);
        public static final IntegerConfig BLOCK_INFO_OVERLAY_OFFSET_Y               = new IntegerConfig("blockInfoOverlayOffsetY", 6, -2000, 2000);
        public static final BooleanConfig BLOCK_INFO_OVERLAY_ENABLED                = new BooleanConfig("blockInfoOverlayEnabled", true);
        public static final IntegerConfig INFO_HUD_MAX_LINES                        = new IntegerConfig("infoHudMaxLines", 10, 1, 128);
        public static final IntegerConfig INFO_HUD_OFFSET_X                         = new IntegerConfig("infoHudOffsetX", 1, 0, 8192);
        public static final IntegerConfig INFO_HUD_OFFSET_Y                         = new IntegerConfig("infoHudOffsetY", 1, 0, 8192);
        public static final DoubleConfig INFO_HUD_SCALE                             = new DoubleConfig("infoHudScale", 1.0, 0.1, 4.0);
        public static final IntegerConfig MATERIAL_LIST_HUD_MAX_LINES               = new IntegerConfig("materialListHudMaxLines", 10, 1, 128);
        public static final DoubleConfig MATERIAL_LIST_HUD_SCALE                    = new DoubleConfig("materialListHudScale", 1.0, 0.1, 4.0);
        public static final BooleanConfig MATERIAL_LIST_HUD_STACKS                  = new BooleanConfig("materialListHudStacks", true);
        public static final BooleanConfig MATERIAL_LIST_SLOT_HIGHLIGHT              = new BooleanConfig("materialListSlotHighlight", true);
        public static final BooleanConfig STATUS_INFO_HUD                           = new BooleanConfig("statusInfoHud", false);
        public static final BooleanConfig STATUS_INFO_HUD_AUTO                      = new BooleanConfig("statusInfoHudAuto", true);
        public static final BooleanConfig TOOL_HUD_ALWAYS_VISIBLE                   = new BooleanConfig("toolHudAlwaysVisible", false);
        public static final IntegerConfig TOOL_HUD_OFFSET_X                         = new IntegerConfig("toolHudOffsetX", 1, 0, 8192);
        public static final IntegerConfig TOOL_HUD_OFFSET_Y                         = new IntegerConfig("toolHudOffsetY", 1, 0, 8192);
        public static final DoubleConfig TOOL_HUD_SCALE                             = new DoubleConfig("toolHudScale", 1.0, 0.1, 4.0);
        public static final DoubleConfig VERIFIER_ERROR_HILIGHT_ALPHA               = new DoubleConfig("verifierErrorHighlightAlpha", 0.2, 0.0, 1.0);
        public static final IntegerConfig VERIFIER_ERROR_HILIGHT_MAX_POSITIONS      = new IntegerConfig("verifierErrorHighlightMaxPositions", 1000, 1, 1000000);
        public static final BooleanConfig VERIFIER_OVERLAY_ENABLED                  = new BooleanConfig("verifierOverlayEnabled", true);
        public static final BooleanConfig WARN_DISABLED_RENDERING                   = new BooleanConfig("warnDisabledRendering", true);

        public static final OptionListConfig<HudAlignment> BLOCK_INFO_LINES_ALIGNMENT           = new OptionListConfig<>("blockInfoLinesAlignment", HudAlignment.TOP_RIGHT, HudAlignment.VALUES);
        public static final OptionListConfig<BlockInfoAlignment> BLOCK_INFO_OVERLAY_ALIGNMENT   = new OptionListConfig<>("blockInfoOverlayAlignment", BlockInfoAlignment.TOP_CENTER, BlockInfoAlignment.VALUES);
        public static final OptionListConfig<MessageOutput> EASY_PLACE_WARNINGS                 = new OptionListConfig<>("easyPlaceWarnings", MessageOutput.MESSAGE_OVERLAY, MessageOutput.getValues());
        public static final OptionListConfig<HudAlignment> INFO_HUD_ALIGNMENT                   = new OptionListConfig<>("infoHudAlignment", HudAlignment.BOTTOM_RIGHT, HudAlignment.VALUES);
        public static final OptionListConfig<HudAlignment> TOOL_HUD_ALIGNMENT                   = new OptionListConfig<>("toolHudAlignment", HudAlignment.BOTTOM_LEFT, HudAlignment.VALUES);

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                BLOCK_INFO_LINES_ENABLED,
                BLOCK_INFO_OVERLAY_ENABLED,
                MATERIAL_LIST_HUD_STACKS,
                MATERIAL_LIST_SLOT_HIGHLIGHT,
                STATUS_INFO_HUD,
                STATUS_INFO_HUD_AUTO,
                TOOL_HUD_ALWAYS_VISIBLE,
                VERIFIER_OVERLAY_ENABLED,
                WARN_DISABLED_RENDERING,

                BLOCK_INFO_LINES_ALIGNMENT,
                BLOCK_INFO_OVERLAY_ALIGNMENT,
                EASY_PLACE_WARNINGS,
                INFO_HUD_ALIGNMENT,
                TOOL_HUD_ALIGNMENT,

                BLOCK_INFO_LINES_OFFSET_X,
                BLOCK_INFO_LINES_OFFSET_Y,
                BLOCK_INFO_LINES_FONT_SCALE,
                BLOCK_INFO_OVERLAY_OFFSET_Y,
                INFO_HUD_MAX_LINES,
                INFO_HUD_OFFSET_X,
                INFO_HUD_OFFSET_Y,
                INFO_HUD_SCALE,
                MATERIAL_LIST_HUD_MAX_LINES,
                MATERIAL_LIST_HUD_SCALE,
                TOOL_HUD_OFFSET_X,
                TOOL_HUD_OFFSET_Y,
                TOOL_HUD_SCALE,
                VERIFIER_ERROR_HILIGHT_ALPHA,
                VERIFIER_ERROR_HILIGHT_MAX_POSITIONS
        );
    }

    public static class Colors
    {
        public static final ColorConfig AREA_SELECTION_BOX_SIDE_COLOR           = new ColorConfig("areaSelectionBoxSideColor",              "#30FFFFFF");
        public static final ColorConfig MATERIAL_LIST_HUD_ITEM_COUNTS           = new ColorConfig("materialListHudItemCountsColor",         "#FFFFAA00");
        public static final ColorConfig MATERIAL_LIST_SLOT_HL_LT_STACK          = new ColorConfig("materialListSlotHighlightLessThanStack", "#80FF40D0");
        public static final ColorConfig MATERIAL_LIST_SLOT_HL_NONE              = new ColorConfig("materialListSlotHighlightNone",          "#80FF2000");
        public static final ColorConfig MATERIAL_LIST_SLOT_HL_NOT_ENOUGH        = new ColorConfig("materialListSlotHighlightNotEnough",     "#80FFE040");
        public static final ColorConfig REBUILD_BREAK_OVERLAY_COLOR             = new ColorConfig("schematicRebuildBreakPlaceOverlayColor", "#4C33CC33");
        public static final ColorConfig REBUILD_REPLACE_OVERLAY_COLOR           = new ColorConfig("schematicRebuildReplaceOverlayColor",    "#4CF0A010");
        public static final ColorConfig SCHEMATIC_OVERLAY_COLOR_EXTRA           = new ColorConfig("schematicOverlayColorExtra",             "#4CFF4CE6");
        public static final ColorConfig SCHEMATIC_OVERLAY_COLOR_MISSING         = new ColorConfig("schematicOverlayColorMissing",           "#2C33B3E6");
        public static final ColorConfig SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK     = new ColorConfig("schematicOverlayColorWrongBlock",        "#4CFF3333");
        public static final ColorConfig SCHEMATIC_OVERLAY_COLOR_WRONG_STATE     = new ColorConfig("schematicOverlayColorWrongState",        "#4CFF9010");

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                AREA_SELECTION_BOX_SIDE_COLOR,
                MATERIAL_LIST_HUD_ITEM_COUNTS,
                MATERIAL_LIST_SLOT_HL_LT_STACK,
                MATERIAL_LIST_SLOT_HL_NONE,
                MATERIAL_LIST_SLOT_HL_NOT_ENOUGH,
                REBUILD_BREAK_OVERLAY_COLOR,
                REBUILD_REPLACE_OVERLAY_COLOR,
                SCHEMATIC_OVERLAY_COLOR_EXTRA,
                SCHEMATIC_OVERLAY_COLOR_MISSING,
                SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK,
                SCHEMATIC_OVERLAY_COLOR_WRONG_STATE
        );
    }

    // Configs that are not shown in the config GUI
    public static class Internal
    {
        public static final BooleanConfig CREATE_PLACEMENT_ON_LOAD      = new BooleanConfig("createPlacementOnLoad", true);
        public static final BooleanConfig PLACEMENT_LIST_ICON_BUTTONS   = new BooleanConfig("placementListIconButtons", false);
        public static final BooleanConfig SCHEMATIC_LIST_ICON_BUTTONS   = new BooleanConfig("schematicListIconButtons", false);

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                CREATE_PLACEMENT_ON_LOAD,
                PLACEMENT_LIST_ICON_BUTTONS,
                SCHEMATIC_LIST_ICON_BUTTONS
        );
    }

    public static final List<ConfigOptionCategory> CATEGORIES = ImmutableList.of(
            BaseConfigOptionCategory.normal("Generic",      Generic.OPTIONS),
            BaseConfigOptionCategory.normal("InfoOverlays", InfoOverlays.OPTIONS),
            BaseConfigOptionCategory.normal("Internal",     Internal.OPTIONS),
            BaseConfigOptionCategory.normal("Visuals",      Visuals.OPTIONS),
            BaseConfigOptionCategory.normal("Colors",       Colors.OPTIONS),
            BaseConfigOptionCategory.normal("Hotkeys",      Hotkeys.HOTKEY_LIST)
    );
}
