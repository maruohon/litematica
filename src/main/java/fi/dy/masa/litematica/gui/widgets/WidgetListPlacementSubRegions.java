package fi.dy.masa.litematica.gui.widgets;

import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

public class WidgetListPlacementSubRegions extends WidgetListBase<SubRegionPlacement, WidgetPlacementSubRegion>
{
    private final GuiPlacementConfiguration parent;

    public WidgetListPlacementSubRegions(int x, int y, int width, int height, GuiPlacementConfiguration parent)
    {
        super(x, y, width, height, parent);

        this.parent = parent;
        this.browserEntryHeight = 22;
    }

    public GuiPlacementConfiguration getParentGui()
    {
        return this.parent;
    }

    @Override
    public void refreshEntries()
    {
        this.listContents.clear();
        this.listContents.addAll(this.parent.getSchematicPlacement().getAllSubRegionsPlacements());

        this.reCreateListEntryWidgets();
    }

    @Override
    protected WidgetPlacementSubRegion createListEntryWidget(int x, int y, int listIndex, boolean isOdd, SubRegionPlacement entry)
    {
        return new WidgetPlacementSubRegion(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), this.zLevel, isOdd,
                this.parent.getSchematicPlacement(), entry, this, this.mc);
    }
}
