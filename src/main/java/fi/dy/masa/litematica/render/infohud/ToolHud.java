package fi.dy.masa.litematica.render.infohud;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.config.value.HudAlignment;
import fi.dy.masa.malilib.overlay.message.MessageHelpers;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.tool.ToolModeData;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.ReplaceBehavior;

public class ToolHud extends InfoHud
{
    private static final ToolHud INSTANCE = new ToolHud();

    public static final Date DATE = new Date();
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    protected ToolHud()
    {
        super();
    }

    public static ToolHud getInstance()
    {
        return INSTANCE;
    }

    @Override
    protected boolean shouldRender()
    {
        return true;
    }

    protected boolean hasEnabledTool()
    {
        return Configs.InfoOverlays.TOOL_HUD_ALWAYS_VISIBLE.getBooleanValue() ||
               (Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue() && EntityUtils.hasToolItem(this.mc.player));
    }

    @Override
    protected HudAlignment getHudAlignment()
    {
        return Configs.InfoOverlays.TOOL_HUD_ALIGNMENT.getValue();
    }

    @Override
    protected double getScaleFactor()
    {
        return Configs.InfoOverlays.TOOL_HUD_SCALE.getDoubleValue();
    }

    @Override
    protected int getOffsetX()
    {
        return Configs.InfoOverlays.TOOL_HUD_OFFSET_X.getIntegerValue();
    }

    @Override
    protected int getOffsetY()
    {
        return Configs.InfoOverlays.TOOL_HUD_OFFSET_Y.getIntegerValue();
    }

    @Override
    protected void updateHudText()
    {
        List<String> lines = this.lineList;
        boolean hasTool = this.hasEnabledTool();

        if (hasTool && DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            this.addSchematicVCSModeLines(lines);
            return;
        }

        ToolMode mode = DataManager.getToolMode();

        if (hasTool && mode.getUsesAreaSelection())
        {
            this.addAreaSelectionLines(lines);
            this.addSelectedBlocksLines(lines, mode);

            String str = Configs.Generic.SELECTION_CORNERS_MODE.getValue().getDisplayName();
            lines.add(StringUtils.translate("litematica.hud.tool_hud.area_selection.corners_mode", str));
        }
        else if ((hasTool || mode == ToolMode.SCHEMATIC_EDIT) && mode.getUsesSchematic())
        {
            this.addSchematicPlacementLines(lines, mode);

            if (mode == ToolMode.PASTE_SCHEMATIC || mode == ToolMode.GRID_PASTE)
            {
                ReplaceBehavior replace = Configs.Generic.PASTE_REPLACE_BEHAVIOR.getValue();

                if (replace == ReplaceBehavior.NONE)
                {
                    lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_paste.replace_mode.none",
                                                    replace.getDisplayName()));
                }
                else
                {
                    lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_paste.replace_mode.some",
                                                    replace.getDisplayName()));
                }
            }
        }

        if (hasTool && mode == ToolMode.DELETE)
        {
            String key = ToolModeData.DELETE.getUsePlacement() ? "litematica.hud.tool_hud.delete.target_mode.placement" :
                                                                 "litematica.hud.tool_hud.delete.target_mode.area";
            lines.add(StringUtils.translate("litematica.hud.tool_hud.delete.target_mode", StringUtils.translate(key)));
        }

        if (hasTool || mode == ToolMode.SCHEMATIC_EDIT)
        {
            lines.add(StringUtils.translate("litematica.hud.tool_hud.tool_mode",
                                            mode.ordinal() + 1, ToolMode.VALUES.size(), mode.getDisplayName()));
        }
    }

    protected void addAreaSelectionLines(List<String> lines)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection selection = sm.getCurrentSelection();

        if (selection != null)
        {
            if (sm.getSelectionMode() == SelectionMode.NORMAL)
            {
                lines.add(StringUtils.translate("litematica.hud.tool_hud.area_selection.selected_area.multi_region", selection.getName()));
            }
            else
            {
                lines.add(StringUtils.translate("litematica.hud.tool_hud.area_selection.selected_area.simple", selection.getName()));
            }

            String originMode;
            BlockPos o = selection.getExplicitOrigin();

            if (o == null)
            {
                o = selection.getEffectiveOrigin();
                originMode = StringUtils.translate("litematica.label.misc.origin.auto");
            }
            else
            {
                originMode = StringUtils.translate("litematica.label.misc.origin.manual");
            }

            if (sm.getSelectionMode() == SelectionMode.NORMAL)
            {
                int count = selection.getAllSubRegionBoxes().size();
                lines.add(StringUtils.translate("litematica.hud.tool_hud.area_selection.origin.multi_region",
                                                originMode, o.getX(), o.getY(), o.getZ(), count));
            }
            else
            {
                lines.add(StringUtils.translate("litematica.hud.tool_hud.area_selection.origin.simple",
                                                originMode, o.getX(), o.getY(), o.getZ()));
            }

            String subRegionName = selection.getCurrentSubRegionBoxName();
            Box box = selection.getSelectedSubRegionBox();

            if (subRegionName != null && box != null)
            {
                // Only show the sub-region name for Normal mode selections
                if (sm.getSelectionMode() == SelectionMode.NORMAL)
                {
                    lines.add(StringUtils.translate("litematica.hud.tool_hud.area_selection.selected_sub_region", subRegionName));
                }

                BlockPos p1 = box.getPos1();
                BlockPos p2 = box.getPos2();

                if (p1 != null && p2 != null)
                {
                    Vec3i size = PositionUtils.getAreaSizeFromRelativeEndPositionAbs(p2.subtract(p1));
                    lines.add(StringUtils.translate("litematica.hud.tool_hud.area_selection.dimensions_positions",
                                                    size.getX(), size.getY(), size.getZ(),
                                                    p1.getX(), p1.getY(), p1.getZ(),
                                                    p2.getX(), p2.getY(), p2.getZ()));
                }
            }
        }
    }

    protected void addSchematicPlacementLines(List<String> lines, ToolMode mode)
    {
        SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_placement.selected_placement",
                                            schematicPlacement.getName()));

            String strModified = MessageHelpers.getYesNoColored(schematicPlacement.isRegionPlacementModified(), false);
            lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_placement.region_count_and_modified",
                                            schematicPlacement.getSubRegionCount(), strModified));

            BlockPos or = schematicPlacement.getOrigin();
            lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_placement.origin",
                                            or.getX(), or.getY(), or.getZ()));

            IBlockState state = mode.getPrimaryBlock();
            ItemStack stack = this.mc.player.getHeldItemMainhand();

            if (state != null && mode == ToolMode.SCHEMATIC_EDIT &&
                (stack.isEmpty() || EntityUtils.hasToolItem(this.mc.player)))
            {
                lines.add(StringUtils.translate("litematica.hud.tool_hud.used_block", this.getBlockStateString(state)));
                this.getBlockStateProperties(state, lines);
            }

            SubRegionPlacement subRegion = schematicPlacement.getSelectedSubRegionPlacement();

            if (subRegion != null)
            {
                strModified = MessageHelpers.getYesNoColored(subRegion.isRegionPlacementModifiedFromDefault(), false);
                lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_placement.selected_sub_region",
                                                subRegion.getName(), strModified));

                or = subRegion.getPos();
                // FIXME: NASTY STUFF
                or = PositionUtils.getTransformedBlockPos(or, schematicPlacement.getMirror(), schematicPlacement.getRotation());
                or = or.add(schematicPlacement.getOrigin());
                lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_placement.sub_region_origin",
                                                or.getX(), or.getY(), or.getZ()));
            }
        }
        else
        {
            String str = StringUtils.translate("litematica.label.tool_hud.none_brackets");
            lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_placement.selected_placement", str));
        }
    }

    protected void addSelectedBlocksLines(List<String> lines, ToolMode mode)
    {
        if (mode.getUsesBlockPrimary() && mode.getPrimaryBlock() != null)
        {
            String block = this.getBlockStateString(mode.getPrimaryBlock());
            lines.add(StringUtils.translate("litematica.hud.tool_hud.used_block", block));
            this.getBlockStateProperties(mode.getPrimaryBlock(), lines);
        }

        if (mode.getUsesBlockSecondary() && mode.getSecondaryBlock() != null)
        {
            String block = this.getBlockStateString(mode.getSecondaryBlock());
            lines.add(StringUtils.translate("litematica.hud.tool_hud.replaced_block", block));
            this.getBlockStateProperties(mode.getSecondaryBlock(), lines);
        }
    }

    protected void addSchematicVCSModeLines(List<String> lines)
    {
        SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();
        SchematicVersion version = project.getCurrentVersion();

        lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_projects.project_name", project.getName()));

        if (version != null)
        {
            BlockPos o = project.getOrigin();
            DATE.setTime(version.getTimeStamp());

            lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_projects.current_version",
                                            version.getVersion(), project.getVersionCount(), version.getName()));
            lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_projects.current_version_date",
                                            SIMPLE_DATE_FORMAT.format(DATE)));

            String str = String.format("%d, %d, %d", o.getX(), o.getY(), o.getZ());
            lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_projects.origin", str));
        }
        else
        {
            lines.add(StringUtils.translate("litematica.hud.tool_hud.schematic_projects.no_versions"));
        }

        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection selection = sm.getCurrentSelection();

        if (selection != null && sm.getSelectionMode() == SelectionMode.NORMAL)
        {
            String subRegionName = selection.getCurrentSubRegionBoxName();

            if (subRegionName != null)
            {
                lines.add(StringUtils.translate("litematica.hud.tool_hud.area_selection.selected_sub_region", subRegionName));
            }
        }

        String str = Configs.Generic.SELECTION_CORNERS_MODE.getValue().getDisplayName();
        lines.add(StringUtils.translate("litematica.hud.tool_hud.area_selection.corners_mode", str));

        // The Projects Mode indicator gets rendered via the status info HUD, if it's enabled.
        // If it's not enabled, then it gets rendered here if the player is currently holding the tool
        if (StatusInfoRenderer.getInstance().shouldRenderStatusInfoHud() == false)
        {
            lines.add(StringUtils.translate("litematica.hud.status_info.schematic_vcs_mode"));
        }
    }

    protected String getBlockStateString(IBlockState state)
    {
        ResourceLocation id = Block.REGISTRY.getNameForObject(state.getBlock());
        String regName = id != null ? id.toString() : StringUtils.translate("litematica.label.misc.null.brackets");

        Optional<EnumFacing> facingOptional = BlockUtils.getFirstPropertyFacingValue(state);
        String strBlock;

        if (facingOptional.isPresent())
        {
            String strFacing = facingOptional.get().getName().toLowerCase();
            strBlock = StringUtils.translate("litematica.hud.tool_hud.block_state_string_with_facing",
                                             regName, state.getBlock().getLocalizedName(), strFacing);
        }
        else
        {
            strBlock = StringUtils.translate("litematica.hud.tool_hud.block_state_string_no_facing",
                                             regName, state.getBlock().getLocalizedName());
        }

        return strBlock;
    }

    protected void getBlockStateProperties(IBlockState state, List<String> lines)
    {
        if (state.getProperties().isEmpty() == false)
        {
            BlockUtils.getFormattedBlockStateProperties(state, " = ").forEach((str) -> lines.add(" > " + str)); 
        }
    }
}
