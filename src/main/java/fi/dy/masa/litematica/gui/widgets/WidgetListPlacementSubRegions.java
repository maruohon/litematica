package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.malilib.gui.util.BaseGuiIcon;
import fi.dy.masa.malilib.gui.widget.list.BaseListWidget;
import fi.dy.masa.malilib.gui.widget.WidgetSearchBar;
import fi.dy.masa.malilib.util.AlphaNumComparator;
import fi.dy.masa.malilib.gui.util.HorizontalAlignment;

public class WidgetListPlacementSubRegions extends BaseListWidget<SubRegionPlacement, WidgetPlacementSubRegion>
{
    private final GuiPlacementConfiguration parent;

    public WidgetListPlacementSubRegions(int x, int y, int width, int height, GuiPlacementConfiguration parent)
    {
        super(x, y, width, height, parent);

        this.parent = parent;
        this.entryWidgetFixedHeight = 22;
        this.shouldSortList = true;

        this.addSearchBarWidget(new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, BaseGuiIcon.SEARCH, HorizontalAlignment.LEFT));
    }

    @Override
    public GuiPlacementConfiguration getParentScreen()
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
        return new WidgetPlacementSubRegion(x, y, this.entryWidgetWidth, this.getBrowserEntryHeightFor(entry),
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
