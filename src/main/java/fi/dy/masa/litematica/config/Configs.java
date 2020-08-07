package fi.dy.masa.litematica.config;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.CornerSelectionMode;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.malilib.config.ModConfig;
import fi.dy.masa.malilib.config.option.BooleanConfig;
import fi.dy.masa.malilib.config.option.ColorConfig;
import fi.dy.masa.malilib.config.option.ConfigOption;
import fi.dy.masa.malilib.config.option.DirectoryConfig;
import fi.dy.masa.malilib.config.option.DoubleConfig;
import fi.dy.masa.malilib.config.option.IntegerConfig;
import fi.dy.masa.malilib.config.option.OptionListConfig;
import fi.dy.masa.malilib.config.option.StringConfig;
import fi.dy.masa.malilib.config.value.HudAlignment;
import fi.dy.masa.malilib.config.value.InfoType;
import fi.dy.masa.malilib.util.FileUtils;

public class Configs implements ModConfig
{
    public static class Generic
    {
        public static final BooleanConfig AREAS_PER_WORLD               = new BooleanConfig("areaSelectionsPerWorld", true, "Use per-world or server root directories for the area selections\n§6NOTE: Don't switch this OFF while you are live streaming,\n§6as then the Area Selection browser will show the server IP\n§6in the navigation widget and also in the current selection name/path\n§6until you change the current directory and selection again");
        public static final BooleanConfig BETTER_RENDER_ORDER           = new BooleanConfig("betterRenderOrder", true, "If enabled, then the schematic rendering is done\nby injecting the different render call into the vanilla\nrendering code. This should result in better translucent block\nrendering/ordering and schematic blocks not getting rendered\nthrough the client world blocks/terrain.\nIf the rendering doesn't work (for example with Optifine),\ntry disabling this option.");
        public static final BooleanConfig CHANGE_SELECTED_CORNER        = new BooleanConfig("changeSelectedCornerOnMove", true, "If true, then the selected corner of an area selection\nis always set to the last moved corner,\nwhen using the set corner hotkeys");
        public static final BooleanConfig CLONE_AT_ORIGINAL_POS         = new BooleanConfig("cloneAtOriginalPosition", false, "If enabled, then using the Clone Area hotkey will create\nthe placement at the original area selection position,\ninstead of at the player's current position");
        public static final BooleanConfig CUSTOM_SCHEMATIC_DIR_ENABLED  = new BooleanConfig("customSchematicDirectoryEnabled", false, "If enabled, then the directory set in 'customSchematicDirectory'\nwill be used as the root/base schematic directory,\ninstead of the normal '.minecraft/schematics/' directory");
        public static final DirectoryConfig CUSTOM_SCHEMATIC_DIRECTORY  = new DirectoryConfig("customSchematicDirectory", FileUtils.getCanonicalFileIfPossible(new File(FileUtils.getMinecraftDirectory(), "schematics")), "The root/base schematic directory to use, if 'customSchematicDirectoryEnabled' is enabled");
        public static final BooleanConfig DEBUG_MESSAGES                = new BooleanConfig("debugMessages", false, "Enables some debug messages in the game console");
        public static final BooleanConfig EASY_PLACE_CLICK_ADJACENT     = new BooleanConfig("easyPlaceClickAdjacent", false, "If enabled, then the Easy Place mode will try to\nclick on existing adjacent blocks. This may help on Spigot\nor similar servers, which don't allow clicking on air blocks.");
        public static final BooleanConfig EASY_PLACE_MODE               = new BooleanConfig("easyPlaceMode", false, "When enabled, then simply trying to use an item/place a block\non schematic blocks will place that block in that position");
        public static final BooleanConfig EASY_PLACE_HOLD_ENABLED       = new BooleanConfig("easyPlaceHoldEnabled", false, "When enabled, then simply holding down the use key\nand looking at different schematic blocks will place them");
        public static final BooleanConfig EXECUTE_REQUIRE_TOOL          = new BooleanConfig("executeRequireHoldingTool", true, "Require holding an enabled tool item\nfor the executeOperation hotkey to work");
        public static final BooleanConfig FIX_RAIL_ROTATION             = new BooleanConfig("fixRailRotation", true, "If true, then a fix is applied for the vanilla bug in rails,\nwhere the 180 degree rotations of straight north-south and\neast-west rails rotate 90 degrees counterclockwise instead >_>");
        public static final BooleanConfig GENERATE_LOWERCASE_NAMES      = new BooleanConfig("generateLowercaseNames", true, "If enabled, then by default the suggested schematic names\nwill be lowercase and using underscores instead of spaces");
        public static final BooleanConfig LOAD_ENTIRE_SCHEMATICS        = new BooleanConfig("loadEntireSchematics", false, "If true, then the entire schematic is always loaded at once.\nIf false, then only the part that is within the client's view distance is loaded.");
        public static final BooleanConfig MATERIAL_LIST_IGNORE_BLOCK_STATE = new BooleanConfig("materialListIgnoreBlockState", false, "Ignore block state when generating material list.\nUseful for redstone components where block state may be\ndifferent while building or constantly changing.\nBe aware that this might ignore more than you want, use with caution.");
        public static final BooleanConfig MATERIALS_FROM_CONTAINER      = new BooleanConfig("materialListFromContainer", true, "WHen enabled, the schematic-based Material List is\nfetched directly from the block state container. Normally you want this.\nOnly disable this if there is an issue where it gets it wrong for some reason\n(and then also report the issue and send the affected schematic).");
        public static final IntegerConfig PASTE_COMMAND_INTERVAL        = new IntegerConfig("pasteCommandInterval", 1, 1, 1000, "The interval in game ticks the Paste schematic task runs at,\nin the command-based mode");
        public static final IntegerConfig PASTE_COMMAND_LIMIT           = new IntegerConfig("pasteCommandLimit", 64, 1, 1000000, "Max number of commands sent per game tick,\nwhen using the Paste schematic feature in the\ncommand mode on a server");
        public static final StringConfig PASTE_COMMAND_SETBLOCK         = new StringConfig("pasteCommandNameSetblock", "setblock", "The setblock command name to use for the\nPaste schematic feature on servers, when\nusing the command-based paste mode");
        public static final BooleanConfig PICK_BLOCK_AUTO               = new BooleanConfig("pickBlockAuto", false, "Automatically pick block before every placed block");
        public static final BooleanConfig PICK_BLOCK_ENABLED            = new BooleanConfig("pickBlockEnabled", true, "Enables the schematic world pick block hotkeys.\nThere is also a hotkey for toggling this option to toggle those hotkeys... o.o", "Pick Block Hotkeys");
        public static final BooleanConfig PICK_BLOCK_IGNORE_NBT         = new BooleanConfig("pickBlockIgnoreNBT", true, "Ignores the NBT data on the expected vs. found items for pick block.\nAllows the pick block to work for example with renamed items.");
        public static final StringConfig PICK_BLOCKABLE_SLOTS           = new StringConfig("pickBlockableSlots", "1-9", "The hotbar slots that are allowed to be\nused for the schematic pick block.\nCan use comma separated individual slots and dash\nseparated slot ranges (no spaces anywhere).\nExample: 2,4-6,9");
        public static final BooleanConfig PLACEMENT_RESTRICTION         = new BooleanConfig("placementRestriction", false, "When enabled, the use key can only be used\nwhen holding the correct item for the targeted position,\nand the targeted position must have a missing block in the schematic", "Placement Restriction");
        public static final BooleanConfig PLACEMENTS_INFRONT            = new BooleanConfig("placementInfrontOfPlayer", false, "When enabled, created placements or moved placements are\npositioned so that they are fully infront of the player,\ninstead of the placement's origin point being at the player's location");
        public static final BooleanConfig RENDER_MATERIALS_IN_GUI       = new BooleanConfig("renderMaterialListInGuis", true, "Whether or not the material list should\nbe rendered inside GUIs");
        public static final BooleanConfig RENDER_THREAD_NO_TIMEOUT      = new BooleanConfig("renderThreadNoTimeout", true, "Removes the timeout from the rendering worker threads.\nIf you get very stuttery rendering when moving around\nor dealing with large schematics, try disabling this. It will however make\nthe schematic rendering a lot slower in some cases.");
        public static final BooleanConfig SIGN_TEXT_PASTE               = new BooleanConfig("signTextPaste", true, "Automatically set the text in the sign GUIs from the schematic");
        public static final StringConfig TOOL_ITEM                      = new StringConfig("toolItem", "minecraft:stick", "The item to use as the \"tool\" for selections etc.");
        public static final BooleanConfig TOOL_ITEM_ENABLED             = new BooleanConfig("toolItemEnabled", true, "If true, then the \"tool\" item can be used to control selections etc.", "Tool Item Enabled");

        public static final OptionListConfig<ReplaceBehavior> PASTE_REPLACE_BEHAVIOR        = new OptionListConfig<>("pasteReplaceBehavior", ReplaceBehavior.NONE, "The behavior of replacing existing blocks\nin the Paste schematic tool mode");
        public static final OptionListConfig<CornerSelectionMode> SELECTION_CORNERS_MODE    = new OptionListConfig<>("selectionCornersMode", CornerSelectionMode.CORNERS, "The Area Selection corners mode to use (Corners, or Expand)");

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
        public static final BooleanConfig ENABLE_AREA_SELECTION_RENDERING       = new BooleanConfig("enableAreaSelectionBoxesRendering", true, "Enable Area Selection boxes rendering", "Area Selection Boxes Rendering");
        public static final BooleanConfig ENABLE_PLACEMENT_BOXES_RENDERING      = new BooleanConfig("enablePlacementBoxesRendering", true, "Enable Schematic Placement boxes rendering", "Schematic Placement Boxes Rendering");
        public static final BooleanConfig ENABLE_RENDERING                      = new BooleanConfig("enableRendering", true, "Main rendering toggle option. Enables/disables ALL mod rendering.", "All Rendering");
        public static final BooleanConfig ENABLE_SCHEMATIC_BLOCKS               = new BooleanConfig("enableSchematicBlocksRendering", true, "Enables schematic block rendering.\nDisabling this allows you to only see the color overlay", "Schematic Blocks Rendering");
        public static final BooleanConfig ENABLE_SCHEMATIC_OVERLAY              = new BooleanConfig("enableSchematicOverlay", true, "The main toggle option for the schematic\nblock overlay rendering", "Schematic Overlay Rendering");
        public static final BooleanConfig ENABLE_SCHEMATIC_RENDERING            = new BooleanConfig("enableSchematicRendering", true, "Enable rendering the schematic and overlay", "Schematic Rendering");
        public static final DoubleConfig GHOST_BLOCK_ALPHA                      = new DoubleConfig("ghostBlockAlpha", 0.5, 0, 1, "The alpha value of the ghost blocks,\nwhen rendering them as translucent.\n§6Note: §7You also need to enable the translucent rendering separately,\nusing the 'renderBlocksAsTranslucent' option!");
        public static final BooleanConfig IGNORE_EXISTING_FLUIDS                = new BooleanConfig("ignoreExistingFluids", false, "If enabled, then any fluid blocks are ignored as \"extra blocks\"\nand as \"wrong blocks\", ie. where the schematic has air or other blocks.\nBasically this makes building stuff under water a whole lot less annoying.\nNote: You will most likely also want to enable the 'renderCollidingSchematicBlocks'\noption at the same time, to allow the blocks to get rendered inside fluids.");
        public static final BooleanConfig OVERLAY_REDUCED_INNER_SIDES           = new BooleanConfig("overlayReducedInnerSides", false, "If enabled, then the adjacent/touching inner sides\nfor the block overlays are removed/not rendered");
        public static final DoubleConfig PLACEMENT_BOX_SIDE_ALPHA               = new DoubleConfig("placementBoxSideAlpha", 0.2, 0, 1, "The alpha value of the sub-region boxes' side");
        public static final BooleanConfig RENDER_AREA_SELECTION_BOX_SIDES       = new BooleanConfig("renderAreaSelectionBoxSides", true, "If enabled, then the area selection boxes will\nhave their side quads rendered");
        public static final BooleanConfig RENDER_BLOCKS_AS_TRANSLUCENT          = new BooleanConfig("renderBlocksAsTranslucent", false, "If enabled, then the schematics are rendered\nusing translucent \"ghost blocks\"", "Translucent Schematic Block Rendering");
        public static final BooleanConfig RENDER_COLLIDING_BLOCK_AT_CURSOR      = new BooleanConfig("renderCollidingBlockAtCursor", false, "If enabled, then the expected block in the schematic is rendered\nat the hovered block, if it's not currently correct");
        public static final BooleanConfig RENDER_COLLIDING_SCHEMATIC_BLOCKS     = new BooleanConfig("renderCollidingSchematicBlocks", false, "If enabled, then blocks in the schematics are rendered\nalso when there is already a (wrong) block in the client world.\nProbably mostly useful when trying to build\nsomething where there are snow layers or water in the way.");
        public static final BooleanConfig RENDER_ERROR_MARKER_CONNECTIONS       = new BooleanConfig("renderErrorMarkerConnections", false, "Render connecting lines between subsequent verifier hilight box corners.\nThis was a rendering bug that some people experienced, but at least some players\nliked it and requested for it to stay, so this options \"restores\" it");
        public static final BooleanConfig RENDER_ERROR_MARKER_SIDES             = new BooleanConfig("renderErrorMarkerSides", true, "If enabled, then the error markers in the Schematic Verifier\nwill have (translucent) sides rendered instead of just the outline");
        public static final BooleanConfig RENDER_PLACEMENT_BOX_SIDES            = new BooleanConfig("renderPlacementBoxSides", false, "If enabled, then the placed schematic sub-region boxes\nwill have their side quads rendered");
        public static final BooleanConfig RENDER_PLACEMENT_ENCLOSING_BOX        = new BooleanConfig("renderPlacementEnclosingBox", true, "If enabled, then an enclosing box is rendered around\nall the sub-regions in a schematic (placement)");
        public static final BooleanConfig RENDER_PLACEMENT_ENCLOSING_BOX_SIDES  = new BooleanConfig("renderPlacementEnclosingBoxSides", false, "If enabled, then the enclosing box around\na schematic placement will have its side quads rendered");
        public static final BooleanConfig RENDER_TRANSLUCENT_INNER_SIDES        = new BooleanConfig("renderTranslucentBlockInnerSides", false, "If enabled, then the model sides are also rendered\nfor inner sides in the translucent mode");
        public static final BooleanConfig SCHEMATIC_OVERLAY_ENABLE_OUTLINES     = new BooleanConfig("schematicOverlayEnableOutlines", true, "Enables rendering a wire frame outline for\nthe schematic block overlay", "Schematic Overlay Outlines");
        public static final BooleanConfig SCHEMATIC_OVERLAY_ENABLE_SIDES        = new BooleanConfig("schematicOverlayEnableSides", true, "Enables rendering translucent boxes/sides for\nthe schematic block overlay", "Schematic Overlay Sides");
        public static final BooleanConfig SCHEMATIC_OVERLAY_MODEL_OUTLINE       = new BooleanConfig("schematicOverlayModelOutline", true, "If enabled, then the schematic overlay will use the\nblock model quads/vertices instead of the\ntraditional full block overlay");
        public static final BooleanConfig SCHEMATIC_OVERLAY_MODEL_SIDES         = new BooleanConfig("schematicOverlayModelSides", true, "If enabled, then the schematic overlay will use the\nblock model quads/vertices instead of the\ntraditional full block overlay");
        public static final DoubleConfig SCHEMATIC_OVERLAY_OUTLINE_WIDTH        = new DoubleConfig("schematicOverlayOutlineWidth", 1.0, 0.000001, 64, "The line width of the block (model) outlines");
        public static final DoubleConfig SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH = new DoubleConfig("schematicOverlayOutlineWidthThrough", 3.0, 0.000001, 64, "The line width of the block (model) outlines,\nwhen the overlay is rendered through blocks");
        public static final BooleanConfig SCHEMATIC_OVERLAY_RENDER_THROUGH      = new BooleanConfig("schematicOverlayRenderThroughBlocks", false, "If enabled, then the schematic overlay will be rendered\nthrough blocks. This is probably only useful once you are\nfinished building and want to see any errors easier");
        public static final BooleanConfig SCHEMATIC_OVERLAY_TYPE_EXTRA          = new BooleanConfig("schematicOverlayTypeExtra", true, "Enables the schematic overlay for extra blocks");
        public static final BooleanConfig SCHEMATIC_OVERLAY_TYPE_MISSING        = new BooleanConfig("schematicOverlayTypeMissing", true, "Enables the schematic overlay for missing blocks");
        public static final BooleanConfig SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK    = new BooleanConfig("schematicOverlayTypeWrongBlock", true, "Enables the schematic overlay for wrong blocks");
        public static final BooleanConfig SCHEMATIC_OVERLAY_TYPE_WRONG_STATE    = new BooleanConfig("schematicOverlayTypeWrongState", true, "Enables the schematic overlay for wrong states");
        public static final BooleanConfig SCHEMATIC_VERIFIER_BLOCK_MODELS       = new BooleanConfig("schematicVerifierUseBlockModels", false, "Forces using blocks models for everything in the Schematic Verifier\nresult list. Normally item models are used for anything\nthat has an item, and block models are only used for blocks\nthat don't have an item, plus for Flower Pots to see the contained item.");

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
        public static final BooleanConfig BLOCK_INFO_LINES_ENABLED              = new BooleanConfig("blockInfoLinesEnabled", true, "If enabled, then MiniHUD-style block info overlay\nis rendered for the looked-at block");
        public static final DoubleConfig BLOCK_INFO_LINES_FONT_SCALE            = new DoubleConfig("blockInfoLinesFontScale", 0.5, 0, 10, "The font scale for the block info lines");
        public static final IntegerConfig BLOCK_INFO_LINES_OFFSET_X             = new IntegerConfig("blockInfoLinesOffsetX", 4, 0, 2000, "The x offset of the block info lines from the selected edge");
        public static final IntegerConfig BLOCK_INFO_LINES_OFFSET_Y             = new IntegerConfig("blockInfoLinesOffsetY", 4, 0, 2000, "The y offset of the block info lines from the selected edge");
        public static final IntegerConfig BLOCK_INFO_OVERLAY_OFFSET_Y           = new IntegerConfig("blockInfoOverlayOffsetY", 6, -2000, 2000, "The y offset of the block info overlay from the selected edge");
        public static final BooleanConfig BLOCK_INFO_OVERLAY_ENABLED            = new BooleanConfig("blockInfoOverlayEnabled", true, "Enable Block Info Overlay rendering to show info\nabout the looked-at block or verifier error marker,\nwhile holding the 'renderInfoOverlay' key", "Block Info Overlay Rendering");
        public static final IntegerConfig INFO_HUD_MAX_LINES                    = new IntegerConfig("infoHudMaxLines", 10, 1, 128, "The maximum number of info lines to show on the HUD at once");
        public static final IntegerConfig INFO_HUD_OFFSET_X                     = new IntegerConfig("infoHudOffsetX", 1, 0, 32000, "The X offset of the Info HUD from the screen edge");
        public static final IntegerConfig INFO_HUD_OFFSET_Y                     = new IntegerConfig("infoHudOffsetY", 1, 0, 32000, "The Y offset of the Info HUD from the screen edge");
        public static final DoubleConfig INFO_HUD_SCALE                         = new DoubleConfig("infoHudScale", 1, 0.1, 4, "Scale factor for the generic Info HUD text");
        public static final IntegerConfig MATERIAL_LIST_HUD_MAX_LINES           = new IntegerConfig("materialListHudMaxLines", 10, 1, 128, "The maximum number of items to show on\nthe Material List Info HUD at once");
        public static final DoubleConfig MATERIAL_LIST_HUD_SCALE                = new DoubleConfig("materialListHudScale", 1, 0.1, 4, "Scale factor for the Material List Info HUD");
        public static final BooleanConfig MATERIAL_LIST_HUD_STACKS              = new BooleanConfig("materialListHudStacks", true, "Whether or not the number of stacks should be shown\non the Material List HUD, or only the total count");
        public static final BooleanConfig MATERIAL_LIST_SLOT_HIGHLIGHT          = new BooleanConfig("materialListSlotHighlight", true, "Highlight inventory slots containing items that are\ncurrently missing or running low in the player's inventory\naccording to the currently active Material List");
        public static final BooleanConfig STATUS_INFO_HUD                       = new BooleanConfig("statusInfoHud", false, "Enable a status info HUD renderer,\nwhich renders a few bits of status info, such as\nthe current layer mode and renderers enabled state");
        public static final BooleanConfig STATUS_INFO_HUD_AUTO                  = new BooleanConfig("statusInfoHudAuto", true, "Allow automatically momentarily enabling the status info HUD \"when needed\",\nfor example when creating a placement and having rendering disabled");
        public static final BooleanConfig TOOL_HUD_ALWAYS_VISIBLE               = new BooleanConfig("toolHudAlwaysVisible", false, "Whether or not the tool HUD should always be rendered,\neven when not holding the tool item");
        public static final IntegerConfig TOOL_HUD_OFFSET_X                     = new IntegerConfig("toolHudOffsetX", 1, 0, 32000, "The X offset of the Info HUD from the screen edge");
        public static final IntegerConfig TOOL_HUD_OFFSET_Y                     = new IntegerConfig("toolHudOffsetY", 1, 0, 32000, "The X offset of the Info HUD from the screen edge");
        public static final DoubleConfig TOOL_HUD_SCALE                         = new DoubleConfig("toolHudScale", 1, 0.1, 4, "Scale factor for the Tool HUD text");
        public static final DoubleConfig VERIFIER_ERROR_HILIGHT_ALPHA           = new DoubleConfig("verifierErrorHilightAlpha", 0.2, 0, 1, "The alpha value of the error marker box sides");
        public static final IntegerConfig VERIFIER_ERROR_HILIGHT_MAX_POSITIONS  = new IntegerConfig("verifierErrorHilightMaxPositions", 1000, 1, 1000000, "The maximum number of mismatched positions to render\nat once in the Schematic Verifier overlay.");
        public static final BooleanConfig VERIFIER_OVERLAY_ENABLED              = new BooleanConfig("verifierOverlayEnabled", true, "Enable Schematic Verifier marker overlay rendering", "Verifier Overlay Rendering");
        public static final BooleanConfig WARN_DISABLED_RENDERING               = new BooleanConfig("warnDisabledRendering", true, "Should the warning message about being in a layer mode\nor having some of the rendering options disabled\nbe shown when loading a new schematic\nor creating a new placement");

        public static final OptionListConfig<HudAlignment> BLOCK_INFO_LINES_ALIGNMENT           = new OptionListConfig<>("blockInfoLinesAlignment", HudAlignment.TOP_RIGHT, "The alignment of the block info lines overlay");
        public static final OptionListConfig<BlockInfoAlignment> BLOCK_INFO_OVERLAY_ALIGNMENT   = new OptionListConfig<>("blockInfoOverlayAlignment", BlockInfoAlignment.TOP_CENTER, "The alignment of the Block Info Overlay");
        public static final OptionListConfig<InfoType> EASY_PLACE_WARNINGS                      = new OptionListConfig<>("easyPlaceWarnings", InfoType.MESSAGE_OVERLAY, "Whether to show the \"Action prevented by *\"\nwarnings for the Easy Place and Placement Restriction modes");
        public static final OptionListConfig<HudAlignment> INFO_HUD_ALIGNMENT                   = new OptionListConfig<>("infoHudAlignment", HudAlignment.BOTTOM_RIGHT, "The alignment of the \"Info HUD\",\nused for the Material List, Schematic Verifier mismatch positions etc.");
        public static final OptionListConfig<HudAlignment> TOOL_HUD_ALIGNMENT                   = new OptionListConfig<>("toolHudAlignment", HudAlignment.BOTTOM_LEFT, "The alignment of the \"tool HUD\", when holding the configured \"tool\"");

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
        public static final ColorConfig AREA_SELECTION_BOX_SIDE_COLOR       = new ColorConfig("areaSelectionBoxSideColor", "0x30FFFFFF", "The color of the area selection boxes, when they are unselected");
        public static final ColorConfig MATERIAL_LIST_HUD_ITEM_COUNTS       = new ColorConfig("materialListHudItemCountsColor", "0xFFFFAA00", "The color of the item count text in the Material List info HUD");
        public static final ColorConfig MATERIAL_LIST_SLOT_HL_LT_STACK      = new ColorConfig("materialListSlotHighlightLessThanStack", "0x80FF40D0", "The color for the \"less than one stack available\" slot highlight");
        public static final ColorConfig MATERIAL_LIST_SLOT_HL_NONE          = new ColorConfig("materialListSlotHighlightNone", "0x80FF2000", "The color for the \"completely out\" slot highlight");
        public static final ColorConfig MATERIAL_LIST_SLOT_HL_NOT_ENOUGH    = new ColorConfig("materialListSlotHighlightNotEnough", "0x80FFE040", "The color for the \"not enough\" slot highlight");
        public static final ColorConfig REBUILD_BREAK_OVERLAY_COLOR         = new ColorConfig("schematicRebuildBreakPlaceOverlayColor", "0x4C33CC33", "The color of Schematic Rebuild mode's break or place blocks selector overlay");
        public static final ColorConfig REBUILD_REPLACE_OVERLAY_COLOR       = new ColorConfig("schematicRebuildReplaceOverlayColor", "0x4CF0A010", "The color of Schematic Rebuild mode's replace selector overlay");
        public static final ColorConfig SCHEMATIC_OVERLAY_COLOR_EXTRA       = new ColorConfig("schematicOverlayColorExtra", "0x4CFF4CE6", "The color of the blocks overlay for extra blocks");
        public static final ColorConfig SCHEMATIC_OVERLAY_COLOR_MISSING     = new ColorConfig("schematicOverlayColorMissing", "0x2C33B3E6", "The color of the blocks overlay for missing blocks");
        public static final ColorConfig SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK = new ColorConfig("schematicOverlayColorWrongBlock", "0x4CFF3333", "The color of the blocks overlay for wrong blocks");
        public static final ColorConfig SCHEMATIC_OVERLAY_COLOR_WRONG_STATE = new ColorConfig("schematicOverlayColorWrongState", "0x4CFF9010", "The color of the blocks overlay for wrong block states");

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
        public static final BooleanConfig CREATE_PLACEMENT_ON_LOAD      = new BooleanConfig("createPlacementOnLoad", true, "A Schematic Placement is created automatically when loading a schematic");
        public static final BooleanConfig PLACEMENT_LIST_ICON_BUTTONS   = new BooleanConfig("placementListIconButtons", false, "Show smaller, icon-only buttons in the Schematic Placements list");
        public static final BooleanConfig SCHEMATIC_LIST_ICON_BUTTONS   = new BooleanConfig("schematicListIconButtons", false, "Show smaller, icon-only buttons in the Loaded Schematics list");

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                CREATE_PLACEMENT_ON_LOAD,
                PLACEMENT_LIST_ICON_BUTTONS,
                SCHEMATIC_LIST_ICON_BUTTONS
        );
    }

    @Override
    public String getModId()
    {
        return Reference.MOD_ID;
    }

    @Override
    public String getModName()
    {
        return Reference.MOD_NAME;
    }

    @Override
    public String getConfigFileName()
    {
        return Reference.MOD_ID + ".json";
    }

    @Override
    public Map<String, List<? extends ConfigOption<?>>> getConfigsPerCategories()
    {
        Map<String, List<? extends ConfigOption<?>>> map = new LinkedHashMap<>();

        map.put("Generic", Generic.OPTIONS);
        map.put("InfoOverlays", InfoOverlays.OPTIONS);
        map.put("Internal", Internal.OPTIONS);
        map.put("Visuals", Visuals.OPTIONS);
        map.put("Colors", Colors.OPTIONS);
        map.put("Hotkeys", Hotkeys.HOTKEY_LIST);

        return map;
    }

    @Override
    public boolean shouldShowCategoryOnConfigGuis(String category)
    {
        return category.equals("Internal") == false;
    }

    @Override
    public void onPostLoad()
    {
        DataManager.setToolItem(Generic.TOOL_ITEM.getStringValue());
        InventoryUtils.setPickBlockableSlots(Generic.PICK_BLOCKABLE_SLOTS.getStringValue());
    }
}
