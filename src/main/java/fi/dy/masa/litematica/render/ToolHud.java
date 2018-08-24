package fi.dy.masa.litematica.render;

import java.util.List;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.OperationMode;
import fi.dy.masa.malilib.config.HudAlignment;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class ToolHud extends InfoHud
{
    private static final ToolHud INSTANCE = new ToolHud();

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
        return EntityUtils.isHoldingItem(this.mc.player, DataManager.getToolItem()) &&
                Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();
    }

    @Override
    protected HudAlignment getHudAlignment()
    {
        return (HudAlignment) Configs.Generic.TOOL_HUD_ALIGNMENT.getOptionListValue();
    }

    @Override
    protected void updateHudText()
    {
        OperationMode mode = DataManager.getOperationMode();
        List<String> lines = this.lineList;
        lines.clear();
        String str;
        String strYes = GuiLitematicaBase.TXT_GREEN + I18n.format("litematica.label.yes") + GuiLitematicaBase.TXT_RST;
        String strNo = GuiLitematicaBase.TXT_RED + I18n.format("litematica.label.no") + GuiLitematicaBase.TXT_RST;

        if (mode == OperationMode.AREA_SELECTION)
        {
            SelectionManager sm = DataManager.getInstance().getSelectionManager();
            AreaSelection selection = sm.getCurrentSelection();

            if (selection != null)
            {
                str = I18n.format("litematica.hud.area_selection.selected_area");
                String strTmp = selection.getName();
                lines.add(String.format("%s: %s%s%s", str, GREEN, strTmp, RESET));

                str = I18n.format("litematica.hud.area_selection.box_count");
                int count = selection.getAllSubRegionBoxes().size();
                String strBoxes = String.format("%s: %s%d%s", str, GREEN, count, RESET);
                BlockPos or = selection.getOrigin();
                str = I18n.format("litematica.hud.area_selection.origin");
                String strOrigin = String.format("%s: %s%d%s, %s%d%s, %s%d%s", str, GREEN, or.getX(), WHITE, GREEN, or.getY(), WHITE, GREEN, or.getZ(), RESET);
                lines.add(strOrigin + " - " + strBoxes);

                String subRegionName = selection.getCurrentSubRegionBoxName();

                if (subRegionName != null)
                {
                    str = I18n.format("litematica.hud.area_selection.selected_sub_region");
                    lines.add(String.format("%s: %s%s%s", str, GREEN, subRegionName, RESET));
                }

                strTmp = GuiLitematicaBase.TXT_GREEN + Configs.Generic.SELECTION_MODE.getOptionListValue().getDisplayName() + GuiLitematicaBase.TXT_RST;
                lines.add(I18n.format("litematica.hud.area_selection.selection_mode", strTmp));
            }
        }
        else if (mode == OperationMode.SCHEMATIC_PLACEMENT)
        {
            SchematicPlacement schematicPlacement = DataManager.getInstance().getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                str = I18n.format("litematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, GREEN, schematicPlacement.getName(), RESET));

                str = I18n.format("litematica.hud.schematic_placement.sub_region_count");
                int count = schematicPlacement.getSubRegionCount();
                String strCount = String.format("%s: %s%d%s", str, GREEN, count, RESET);

                str = I18n.format("litematica.hud.schematic_placement.sub_regions_modified");
                String strTmp = schematicPlacement.isRegionPlacementModified() ? strYes : strNo;
                lines.add(strCount + String.format(" - %s: %s", str, strTmp));

                str = I18n.format("litematica.hud.area_selection.origin");
                BlockPos or = schematicPlacement.getOrigin();
                lines.add(String.format("%s: %s%d%s, %s%d%s, %s%d%s", str, GREEN, or.getX(), WHITE, GREEN, or.getY(), WHITE, GREEN, or.getZ(), RESET));

                Placement placement = schematicPlacement.getSelectedSubRegionPlacement();

                if (placement != null)
                {
                    String areaName = placement.getName();
                    str = I18n.format("litematica.hud.schematic_placement.selected_sub_region");
                    lines.add(String.format("%s: %s%s%s", str, GREEN, areaName, RESET));

                    str = I18n.format("litematica.hud.schematic_placement.sub_region_modified");
                    strTmp = placement.isRegionPlacementModified(schematicPlacement.getSchematic().getSubRegionPosition(areaName)) ? strYes : strNo;
                    lines.add(String.format("%s: %s", str, strTmp));
                }
            }
            else
            {
                String strTmp = "<" + I18n.format("litematica.label.none_lower") + ">";
                str = I18n.format("litematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, WHITE, strTmp, RESET));
            }
        }

        str = I18n.format("litematica.hud.selected_mode");
        lines.add(String.format("%s [%s%d%s/%s%d%s]: %s%s%s", str, GREEN, mode.ordinal() + 1, WHITE,
                GREEN, OperationMode.values().length, WHITE, GREEN, mode.getName(), RESET));
    }
}
