package fi.dy.masa.litematica.gui.widgets;

import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.widgets.base.WidgetListBase;

public class WidgetPlacementSubRegions extends WidgetListBase<Placement, WidgetPlacementSubRegion>
{
    private final GuiPlacementConfiguration parent;

    public WidgetPlacementSubRegions(int x, int y, int width, int height, GuiPlacementConfiguration parent)
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
        this.scrollBar.setMaxValue(this.listContents.size() - this.maxVisibleBrowserEntries);

        this.updateBrowserMaxVisibleEntries();
        this.recreateListWidgets();
    }

    @Override
    protected WidgetPlacementSubRegion createListWidget(int x, int y, boolean isOdd, Placement entry)
    {
        return new WidgetPlacementSubRegion(x, y, this.browserEntryWidth, this.browserEntryHeight, this.zLevel, isOdd,
                this.parent.getSchematicPlacement(), entry, this, this.mc);
    }
}
