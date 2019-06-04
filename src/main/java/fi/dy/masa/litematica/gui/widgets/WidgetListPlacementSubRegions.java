package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.util.AlphaNumComparator;

public class WidgetListPlacementSubRegions extends WidgetListBase<SubRegionPlacement, WidgetPlacementSubRegion>
{
    private final GuiPlacementConfiguration parent;

    public WidgetListPlacementSubRegions(int x, int y, int width, int height, GuiPlacementConfiguration parent)
    {
        super(x, y, width, height, parent);

        this.parent = parent;
        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.LEFT);
        //this.widgetSearchBar.setSearchOpen(true);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
        this.shouldSortList = true;
    }

    public GuiPlacementConfiguration getParentGui()
    {
        return this.parent;
    }

    @Override
    protected Collection<SubRegionPlacement> getAllEntries()
    {
        return this.parent.getSchematicPlacement().getAllSubRegionsPlacements();
    }

    @Override
    protected Comparator<SubRegionPlacement> getComparator()
    {
        return new PlacementComparator();
    }

    @Override
    protected List<String> getEntryStringsForFilter(SubRegionPlacement entry)
    {
        return ImmutableList.of(entry.getName().toLowerCase());
    }

    @Override
    protected WidgetPlacementSubRegion createListEntryWidget(int x, int y, int listIndex, boolean isOdd, SubRegionPlacement entry)
    {
        return new WidgetPlacementSubRegion(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, this.parent.getSchematicPlacement(), entry, listIndex, this);
    }

    protected static class PlacementComparator extends AlphaNumComparator implements Comparator<SubRegionPlacement>
    {
        @Override
        public int compare(SubRegionPlacement placement1, SubRegionPlacement placement2)
        {
            return this.compare(placement1.getName(), placement2.getName());
        }
    }
}
