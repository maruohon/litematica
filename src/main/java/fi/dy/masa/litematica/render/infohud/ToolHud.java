package fi.dy.masa.litematica.render.infohud;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import fi.dy.masa.litematica.data.EntitiesDataStorage;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.StringUtils;

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
        return (HudAlignment) Configs.InfoOverlays.TOOL_HUD_ALIGNMENT.getOptionListValue();
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

            lines.add(StringUtils.translate("litematica.hud.schematic_projects.project_name", green + project.getName() + rst));
            SchematicVersion version = project.getCurrentVersion();

            if (version != null)
            {
                lines.add(StringUtils.translate("litematica.hud.schematic_projects.current_version", green + version.getVersion() + rst, green + project.getVersionCount() + rst, green + version.getName() + rst));
                DATE.setTime(version.getTimeStamp());
                lines.add(StringUtils.translate("litematica.hud.schematic_projects.current_version_date", green + SIMPLE_DATE_FORMAT.format(DATE) + rst));
                BlockPos o = project.getOrigin();
                str = String.format("%d, %d, %d", o.getX(), o.getY(), o.getZ());
                lines.add(StringUtils.translate("litematica.hud.schematic_projects.origin", green + str + rst));
            }
            else
            {
                lines.add(StringUtils.translate("litematica.hud.schematic_projects.no_versions"));
            }

            SelectionManager sm = DataManager.getSelectionManager();
            AreaSelection selection = sm.getCurrentSelection();

            if (selection != null && sm.getSelectionMode() == SelectionMode.NORMAL)
            {
                String subRegionName = selection.getCurrentSubRegionBoxName();

                if (subRegionName != null)
                {
                    lines.add(StringUtils.translate("litematica.hud.area_selection.selected_sub_region", green + subRegionName + rst));
                }
            }

            str = green + Configs.Generic.SELECTION_CORNERS_MODE.getOptionListValue().getDisplayName() + rst;
            lines.add(StringUtils.translate("litematica.hud.area_selection.selection_corners_mode", str));

            // The Projects Mode indicator gets rendered via the status info HUD, if it's enabled.
            // If it's not enabled, then it gets rendered here if the player is currently holding the tool
            if (StatusInfoRenderer.getInstance().shouldRenderStatusInfoHud() == false)
            {
                lines.add(StringUtils.translate("litematica.hud.schematic_projects_mode"));
            }

            return;
        }

        ToolMode mode = DataManager.getToolMode();
        String orange = GuiBase.TXT_GOLD;
        String red = GuiBase.TXT_RED;
        String white = GuiBase.TXT_WHITE;
        String strYes = green + StringUtils.translate("litematica.label.yes") + rst;
        String strNo = GuiBase.TXT_RED + StringUtils.translate("litematica.label.no") + rst;

        if (hasTool && mode == ToolMode.DELETE)
        {
            String strp = ToolModeData.DELETE.getUsePlacement() ? "litematica.hud.delete.target_mode.placement" : "litematica.hud.delete.target_mode.area";
            lines.add(StringUtils.translate("litematica.hud.delete.target_mode", green + StringUtils.translate(strp) + rst));
        }

        if (hasTool && mode.getUsesAreaSelection())
        {
            SelectionManager sm = DataManager.getSelectionManager();
            AreaSelection selection = sm.getCurrentSelection();

            if (selection != null)
            {
                String name = green + selection.getName() + rst;

                if (sm.getSelectionMode() == SelectionMode.NORMAL)
                {
                    lines.add(StringUtils.translate("litematica.hud.area_selection.selected_area_normal", name));
                }
                else
                {
                    lines.add(StringUtils.translate("litematica.hud.area_selection.selected_area_simple", name));
                }

                String strOr;
                BlockPos o = selection.getExplicitOrigin();

                if (o == null)
                {
                    o = selection.getEffectiveOrigin();
                    strOr = StringUtils.translate("litematica.gui.label.origin.auto");
                }
                else
                {
                    strOr = StringUtils.translate("litematica.gui.label.origin.manual");
                }
                int count = selection.getAllSubRegionBoxes().size();

                str = String.format("%d, %d, %d %s[%s%s%s]", o.getX(), o.getY(), o.getZ(), rst, orange, strOr, rst);
                String strOrigin = StringUtils.translate("litematica.hud.area_selection.origin", green + str + rst);
                String strBoxes = StringUtils.translate("litematica.hud.area_selection.box_count", green + count + rst);

                lines.add(strOrigin + " - " + strBoxes);

                String subRegionName = selection.getCurrentSubRegionBoxName();
                Box box = selection.getSelectedSubRegionBox();

                if (subRegionName != null && box != null)
                {
                    lines.add(StringUtils.translate("litematica.hud.area_selection.selected_sub_region", green + subRegionName + rst));
                    BlockPos p1 = box.getPos1();
                    BlockPos p2 = box.getPos2();

                    if (p1 != null && p2 != null)
                    {
                        BlockPos size = PositionUtils.getAreaSizeFromRelativeEndPositionAbs(p2.subtract(p1));
                        String strDim = green + String.format("%dx%dx%d", size.getX(), size.getY(), size.getZ()) + rst;
                        String strp1 = green + String.format("%d, %d, %d", p1.getX(), p1.getY(), p1.getZ()) + rst;
                        String strp2 = green + String.format("%d, %d, %d", p2.getX(), p2.getY(), p2.getZ()) + rst;
                        lines.add(StringUtils.translate("litematica.hud.area_selection.dimensions_position", strDim, strp1, strp2));
                    }
                }
            }

            if (mode.getUsesBlockPrimary())
            {
                BlockState state = mode.getPrimaryBlock();

                if (state != null)
                {
                    lines.add(StringUtils.translate("litematica.tool_hud.block_1", this.getBlockString(state)));
                }
            }

            if (mode.getUsesBlockSecondary())
            {
                BlockState state = mode.getSecondaryBlock();

                if (state != null)
                {
                    lines.add(StringUtils.translate("litematica.tool_hud.block_2", this.getBlockString(state)));
                }
            }

            str = green + Configs.Generic.SELECTION_CORNERS_MODE.getOptionListValue().getDisplayName() + rst;
            lines.add(StringUtils.translate("litematica.hud.area_selection.selection_corners_mode", str));
        }
        else if ((hasTool || mode == ToolMode.REBUILD) && mode.getUsesSchematic())
        {
            SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                str = StringUtils.translate("litematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, green, schematicPlacement.getName(), rst));

                str = StringUtils.translate("litematica.hud.schematic_placement.sub_region_count");
                int count = schematicPlacement.getSubRegionCount();
                String strCount = String.format("%s: %s%d%s", str, green, count, rst);

                str = StringUtils.translate("litematica.hud.schematic_placement.sub_regions_modified");
                String strTmp = schematicPlacement.isRegionPlacementModified() ? strYes : strNo;
                lines.add(strCount + String.format(" - %s: %s", str, strTmp));

                BlockPos or = schematicPlacement.getOrigin();
                str = String.format("%d, %d, %d", or.getX(), or.getY(), or.getZ());

                lines.add(StringUtils.translate("litematica.hud.area_selection.origin", green + str + rst));

                BlockState state = mode.getPrimaryBlock();
                ItemStack stack = this.mc.player.getMainHandStack();

                if (state != null && mode == ToolMode.REBUILD &&
                    (stack.isEmpty() || EntityUtils.hasToolItemInHand(this.mc.player, Hand.MAIN_HAND)))
                {
                    lines.add(StringUtils.translate("litematica.tool_hud.block_1", this.getBlockString(state)));
                }

                SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

                if (placement != null)
                {
                    String areaName = placement.getName();
                    str = StringUtils.translate("litematica.hud.schematic_placement.selected_sub_region");
                    String str2 = StringUtils.translate("litematica.hud.schematic_placement.sub_region_modified");
                    strTmp = placement.isRegionPlacementModifiedFromDefault() ? strYes : strNo;
                    lines.add(String.format("%s: %s%s%s - %s: %s", str, green, areaName, rst, str2, strTmp));

                    or = placement.getPos();
                    or = PositionUtils.getTransformedBlockPos(or, schematicPlacement.getMirror(), schematicPlacement.getRotation());
                    or = or.add(schematicPlacement.getOrigin());
                    str = String.format("%d, %d, %d", or.getX(), or.getY(), or.getZ());
                    lines.add(StringUtils.translate("litematica.hud.schematic_placement.sub_region_origin", green + str + rst));
                }

                if (mode == ToolMode.PASTE_SCHEMATIC)
                {
                    ReplaceBehavior replace = (ReplaceBehavior) Configs.Generic.PASTE_REPLACE_BEHAVIOR.getOptionListValue();
                    str = replace.getDisplayName();

                    if (replace == ReplaceBehavior.NONE)
                    {
                        str = red + str + rst;
                    }
                    else
                    {
                        str = orange + str + rst;
                    }

                    lines.add(StringUtils.translate("litematica.hud.misc.schematic_paste.replace_mode", str));

                    str = Configs.Generic.PASTE_NBT_BEHAVIOR.getOptionListValue().getDisplayName();

                    if (EntitiesDataStorage.getInstance().hasServuxServer()
                            && Configs.Generic.PASTE_USING_SERVUX.getBooleanValue()
                            && !Configs.Generic.PASTE_USING_COMMANDS_IN_SP.getBooleanValue())
                    {
                        str = orange + "Servux" + rst;
                    }
                    lines.add(StringUtils.translate("litematica.hud.misc.schematic_paste.data_restore_mode", str));

                    String strVal = Configs.Generic.PASTE_IGNORE_INVENTORY.getBooleanValue() ? strYes : strNo;
                    lines.add(String.format("Ignore inventory contents: %s", strVal));
                }
            }
            else
            {
                String strTmp = "<" + StringUtils.translate("litematica.label.none_lower") + ">";
                str = StringUtils.translate("litematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, white, strTmp, rst));
            }
        }

        if (hasTool || mode == ToolMode.REBUILD)
        {
            str = StringUtils.translate("litematica.hud.selected_mode");
            String modeName = mode.getName();

            if (mode == ToolMode.REBUILD)
            {
                modeName = orange + modeName + rst;
            }

            lines.add(String.format("%s [%s%d%s/%s%d%s]: %s%s%s", str, green, mode.ordinal() + 1, white,
                    green, ToolMode.values().length, white, green, modeName, rst));
        }
    }

    protected String getBlockString(BlockState state)
    {
        String strBlock;

        String green = GuiBase.TXT_GREEN;
        String rst = GuiBase.TXT_RST;

        strBlock = green + state.getBlock().getName().getString() + rst;
        Direction facing = BlockUtils.getFirstPropertyFacingValue(state);

        if (facing != null)
        {
            String gold = GuiBase.TXT_GOLD;
            String strFacing = gold + facing.getName().toLowerCase() + rst;
            strBlock += " - " + StringUtils.translate("litematica.tool_hud.facing", strFacing);
        }

        return strBlock;
    }
}
