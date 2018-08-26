package fi.dy.masa.litematica.render;

import java.util.List;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.OperationMode;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
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
        String strYes = GuiBase.TXT_GREEN + I18n.format("litematica.label.yes") + GuiBase.TXT_RST;
        String strNo = GuiBase.TXT_RED + I18n.format("litematica.label.no") + GuiBase.TXT_RST;

        if (mode == OperationMode.AREA_SELECTION)
        {
            SelectionManager sm = DataManager.getInstance().getSelectionManager();
            AreaSelection selection = sm.getCurrentSelection();

            if (selection != null)
            {
                str = I18n.format("litematica.hud.area_selection.selected_area");
                String strTmp = selection.getName();
                lines.add(String.format("%s: %s%s%s", str, GuiBase.TXT_GREEN, strTmp, GuiBase.TXT_RST));

                str = I18n.format("litematica.hud.area_selection.box_count");
                int count = selection.getAllSubRegionBoxes().size();
                String strBoxes = String.format("%s: %s%d%s", str, GuiBase.TXT_GREEN, count, GuiBase.TXT_RST);
                BlockPos or = selection.getOrigin();
                str = I18n.format("litematica.hud.area_selection.origin");
                String strOrigin = String.format("%s: %s%d%s, %s%d%s, %s%d%s", str,
                        GuiBase.TXT_GREEN, or.getX(), GuiBase.TXT_WHITE,
                        GuiBase.TXT_GREEN, or.getY(), GuiBase.TXT_WHITE,
                        GuiBase.TXT_GREEN, or.getZ(), GuiBase.TXT_RST);
                lines.add(strOrigin + " - " + strBoxes);

                String subRegionName = selection.getCurrentSubRegionBoxName();

                if (subRegionName != null)
                {
                    str = I18n.format("litematica.hud.area_selection.selected_sub_region");
                    lines.add(String.format("%s: %s%s%s", str, GuiBase.TXT_GREEN, subRegionName, GuiBase.TXT_RST));
                }

                strTmp = GuiBase.TXT_GREEN + Configs.Generic.SELECTION_MODE.getOptionListValue().getDisplayName() + GuiBase.TXT_RST;
                lines.add(I18n.format("litematica.hud.area_selection.selection_mode", strTmp));
            }
        }
        else if (mode.getUsesSchematic())
        {
            SchematicPlacement schematicPlacement = DataManager.getInstance().getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                str = I18n.format("litematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, GuiBase.TXT_GREEN, schematicPlacement.getName(), GuiBase.TXT_RST));

                str = I18n.format("litematica.hud.schematic_placement.sub_region_count");
                int count = schematicPlacement.getSubRegionCount();
                String strCount = String.format("%s: %s%d%s", str, GuiBase.TXT_GREEN, count, GuiBase.TXT_RST);

                str = I18n.format("litematica.hud.schematic_placement.sub_regions_modified");
                String strTmp = schematicPlacement.isRegionPlacementModified() ? strYes : strNo;
                lines.add(strCount + String.format(" - %s: %s", str, strTmp));

                str = I18n.format("litematica.hud.area_selection.origin");
                BlockPos or = schematicPlacement.getOrigin();
                lines.add(String.format("%s: %s%d%s, %s%d%s, %s%d%s", str,
                        GuiBase.TXT_GREEN, or.getX(), GuiBase.TXT_WHITE,
                        GuiBase.TXT_GREEN, or.getY(), GuiBase.TXT_WHITE,
                        GuiBase.TXT_GREEN, or.getZ(), GuiBase.TXT_RST));

                Placement placement = schematicPlacement.getSelectedSubRegionPlacement();

                if (placement != null)
                {
                    String areaName = placement.getName();
                    str = I18n.format("litematica.hud.schematic_placement.selected_sub_region");
                    lines.add(String.format("%s: %s%s%s", str, GuiBase.TXT_GREEN, areaName, GuiBase.TXT_RST));

                    str = I18n.format("litematica.hud.schematic_placement.sub_region_modified");
                    strTmp = placement.isRegionPlacementModified(schematicPlacement.getSchematic().getSubRegionPosition(areaName)) ? strYes : strNo;
                    lines.add(String.format("%s: %s", str, strTmp));
                }
            }
            else
            {
                String strTmp = "<" + I18n.format("litematica.label.none_lower") + ">";
                str = I18n.format("litematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, GuiBase.TXT_WHITE, strTmp, GuiBase.TXT_RST));
            }
        }

        str = I18n.format("litematica.hud.selected_mode");
        lines.add(String.format("%s [%s%d%s/%s%d%s]: %s%s%s", str, GuiBase.TXT_GREEN, mode.ordinal() + 1, GuiBase.TXT_WHITE,
                GuiBase.TXT_GREEN, OperationMode.values().length, GuiBase.TXT_WHITE, GuiBase.TXT_GREEN, mode.getName(), GuiBase.TXT_RST));
    }
}
