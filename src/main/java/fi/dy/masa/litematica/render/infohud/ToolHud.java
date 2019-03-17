package fi.dy.masa.litematica.render.infohud;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
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
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.BlockUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

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
        return Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue() && EntityUtils.hasToolItem(this.mc.player);
    }

    @Override
    protected HudAlignment getHudAlignment()
    {
        return (HudAlignment) Configs.Generic.TOOL_HUD_ALIGNMENT.getOptionListValue();
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
        String str;
        String green = GuiBase.TXT_GREEN;
        String rst = GuiBase.TXT_RST;
        boolean hasTool = this.hasEnabledTool();

        List<String> lines = this.lineList;

        if (hasTool && DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

            lines.add(I18n.format("litematica.hud.schematic_projects.project_name", green + project.getName() + rst));
            SchematicVersion version = project.getCurrentVersion();

            if (version != null)
            {
                lines.add(I18n.format("litematica.hud.schematic_projects.current_version", green + version.getVersion() + rst, green + project.getVersionCount() + rst, green + version.getName() + rst));
                DATE.setTime(version.getTimeStamp());
                lines.add(I18n.format("litematica.hud.schematic_projects.current_version_date", green + SIMPLE_DATE_FORMAT.format(DATE) + rst));
                BlockPos o = project.getOrigin();
                str = String.format("%d, %d, %d", o.getX(), o.getY(), o.getZ());
                lines.add(I18n.format("litematica.hud.schematic_projects.origin", green + str + rst));
            }
            else
            {
                lines.add(I18n.format("litematica.hud.schematic_projects.no_versions"));
            }

            SelectionManager sm = DataManager.getSelectionManager();
            AreaSelection selection = sm.getCurrentSelection();

            if (selection != null && sm.getSelectionMode() == SelectionMode.NORMAL)
            {
                String subRegionName = selection.getCurrentSubRegionBoxName();

                if (subRegionName != null)
                {
                    lines.add(I18n.format("litematica.hud.area_selection.selected_sub_region", green + subRegionName + rst));
                }
            }

            str = green + Configs.Generic.SELECTION_CORNERS_MODE.getOptionListValue().getDisplayName() + rst;
            lines.add(I18n.format("litematica.hud.area_selection.selection_corners_mode", str));

            // The Projects Mode indicator gets rendered via the status info HUD, if it's enabled.
            // If it's not enabled, then it gets rendered here if the player is currently holding the tool
            if (Configs.InfoOverlays.STATUS_INFO_HUD.getBooleanValue() == false)
            {
                lines.add(I18n.format("litematica.hud.schematic_projects_mode"));
            }

            return;
        }

        // The Status Info HUD renders as part of the Tool HUD renderer, so the main shouldRender()
        // always retusn true, and we need to check here if the player actually has a tool
        if (hasTool == false)
        {
            return;
        }

        ToolMode mode = DataManager.getToolMode();
        String orange = GuiBase.TXT_GOLD;
        String white = GuiBase.TXT_WHITE;
        String strYes = green + I18n.format("litematica.label.yes") + rst;
        String strNo = GuiBase.TXT_RED + I18n.format("litematica.label.no") + rst;

        if (mode == ToolMode.DELETE)
        {
            String strp = ToolModeData.DELETE.getUsePlacement() ? "litematica.hud.delete.target_mode.placement" : "litematica.hud.delete.target_mode.area";
            lines.add(I18n.format("litematica.hud.delete.target_mode", green + I18n.format(strp) + rst));
        }

        if (mode.getUsesAreaSelection())
        {
            SelectionManager sm = DataManager.getSelectionManager();
            AreaSelection selection = sm.getCurrentSelection();

            if (selection != null)
            {
                String name = green + selection.getName() + rst;

                if (sm.getSelectionMode() == SelectionMode.NORMAL)
                {
                    lines.add(I18n.format("litematica.hud.area_selection.selected_area_normal", name));
                }
                else
                {
                    lines.add(I18n.format("litematica.hud.area_selection.selected_area_simple", name));
                }

                String strOr;
                BlockPos o = selection.getExplicitOrigin();

                if (o == null)
                {
                    o = selection.getEffectiveOrigin();
                    strOr = I18n.format("litematica.gui.label.origin.auto");
                }
                else
                {
                    strOr = I18n.format("litematica.gui.label.origin.manual");
                }
                int count = selection.getAllSubRegionBoxes().size();

                str = String.format("%d, %d, %d %s[%s%s%s]", o.getX(), o.getY(), o.getZ(), rst, orange, strOr, rst);
                String strOrigin = I18n.format("litematica.hud.area_selection.origin", green + str + rst);
                String strBoxes = I18n.format("litematica.hud.area_selection.box_count", green + count + rst);

                lines.add(strOrigin + " - " + strBoxes);

                String subRegionName = selection.getCurrentSubRegionBoxName();
                Box box = selection.getSelectedSubRegionBox();

                if (subRegionName != null && box != null)
                {
                    lines.add(I18n.format("litematica.hud.area_selection.selected_sub_region", green + subRegionName + rst));
                    BlockPos p1 = box.getPos1();
                    BlockPos p2 = box.getPos2();

                    if (p1 != null && p2 != null)
                    {
                        BlockPos size = PositionUtils.getAreaSizeFromRelativeEndPositionAbs(p2.subtract(p1));
                        String strDim = green + String.format("%dx%dx%d", size.getX(), size.getY(), size.getZ()) + rst;
                        String strp1 = green + String.format("%d, %d, %d", p1.getX(), p1.getY(), p1.getZ()) + rst;
                        String strp2 = green + String.format("%d, %d, %d", p2.getX(), p2.getY(), p2.getZ()) + rst;
                        lines.add(I18n.format("litematica.hud.area_selection.dimensions_position", strDim, strp1, strp2));
                    }
                }
            }

            if (mode.getUsesBlockPrimary())
            {
                IBlockState state = mode.getPrimaryBlock();

                if (state != null)
                {
                    lines.add(I18n.format("litematica.tool_hud.block_1", this.getBlockString(state)));
                }
            }

            if (mode.getUsesBlockSecondary())
            {
                IBlockState state = mode.getSecondaryBlock();

                if (state != null)
                {
                    lines.add(I18n.format("litematica.tool_hud.block_2", this.getBlockString(state)));
                }
            }

            str = green + Configs.Generic.SELECTION_CORNERS_MODE.getOptionListValue().getDisplayName() + rst;
            lines.add(I18n.format("litematica.hud.area_selection.selection_corners_mode", str));
        }
        else if (mode.getUsesSchematic())
        {
            SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                str = I18n.format("litematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, green, schematicPlacement.getName(), rst));

                str = I18n.format("litematica.hud.schematic_placement.sub_region_count");
                int count = schematicPlacement.getSubRegionCount();
                String strCount = String.format("%s: %s%d%s", str, green, count, rst);

                str = I18n.format("litematica.hud.schematic_placement.sub_regions_modified");
                String strTmp = schematicPlacement.isRegionPlacementModified() ? strYes : strNo;
                lines.add(strCount + String.format(" - %s: %s", str, strTmp));

                BlockPos or = schematicPlacement.getOrigin();
                str = String.format("%d, %d, %d", or.getX(), or.getY(), or.getZ());

                lines.add(I18n.format("litematica.hud.area_selection.origin", green + str + rst));

                SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

                if (placement != null)
                {
                    String areaName = placement.getName();
                    str = I18n.format("litematica.hud.schematic_placement.selected_sub_region");
                    String str2 = I18n.format("litematica.hud.schematic_placement.sub_region_modified");
                    strTmp = placement.isRegionPlacementModifiedFromDefault() ? strYes : strNo;
                    lines.add(String.format("%s: %s%s%s - %s: %s", str, green, areaName, rst, str2, strTmp));

                    or = placement.getPos();
                    or = PositionUtils.getTransformedBlockPos(or, schematicPlacement.getMirror(), schematicPlacement.getRotation());
                    or = or.add(schematicPlacement.getOrigin());
                    str = String.format("%d, %d, %d", or.getX(), or.getY(), or.getZ());
                    lines.add(I18n.format("litematica.hud.schematic_placement.sub_region_origin", green + str + rst));
                }
            }
            else
            {
                String strTmp = "<" + I18n.format("litematica.label.none_lower") + ">";
                str = I18n.format("litematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, white, strTmp, rst));
            }
        }

        str = I18n.format("litematica.hud.selected_mode");
        lines.add(String.format("%s [%s%d%s/%s%d%s]: %s%s%s", str, green, mode.ordinal() + 1, white,
                green, ToolMode.values().length, white, green, mode.getName(), rst));
    }

    protected String getBlockString(IBlockState state)
    {
        ItemStack stack = MaterialCache.getInstance().getItemForState(state);
        String strBlock;

        String green = GuiBase.TXT_GREEN;
        String rst = GuiBase.TXT_RST;

        strBlock = green + stack.getDisplayName() + rst;
        EnumFacing facing = BlockUtils.getFirstPropertyFacingValue(state);

        if (facing != null)
        {
            String gold = GuiBase.TXT_GOLD;
            String strFacing = gold + facing.getName().toLowerCase() + rst;
            strBlock += " - " + I18n.format("litematica.tool_hud.facing", strFacing);
        }

        return strBlock;
    }
}
