package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.gui.GuiAreaSelectionEditorNormal;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.malilib.gui.util.BaseGuiIcon;
import fi.dy.masa.malilib.gui.widget.list.BaseListWidget;
import fi.dy.masa.malilib.gui.widget.WidgetSearchBar;
import fi.dy.masa.malilib.util.AlphaNumComparator.AlphaNumStringComparator;
import fi.dy.masa.malilib.gui.util.HorizontalAlignment;

public class WidgetListSelectionSubRegions extends BaseListWidget<String, WidgetSelectionSubRegion>
{
    private final GuiAreaSelectionEditorNormal gui;
    private final AreaSelection selection;

    public WidgetListSelectionSubRegions(int x, int y, int width, int height,
            AreaSelection selection, GuiAreaSelectionEditorNormal gui)
    {
        super(x, y, width, height, gui);

        this.gui = gui;
        this.selection = selection;
        this.entryWidgetFixedHeight = 22;
        this.shouldSortList = true;

        this.addSearchBarWidget(new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, BaseGuiIcon.SEARCH, HorizontalAlignment.LEFT));
    }

    public GuiAreaSelectionEditorNormal getEditorGui()
    {
        return this.gui;
    }

    @Override
    protected Collection<String> getAllEntries()
    {
        return this.selection.getAllSubRegionNames();
    }

    @Override
    protected Comparator<String> getComparator()
    {
        return new AlphaNumStringComparator();
    }

    @Override
    protected List<String> getEntryStringsForFilter(String entry)
    {
        return ImmutableList.of(entry.toLowerCase());
    }

    @Override
    protected WidgetSelectionSubRegion createListEntryWidget(int x, int y, int listIndex, boolean isOdd, String entry)
    {
        return new WidgetSelectionSubRegion(x, y, this.entryWidgetWidth, this.entryWidgetFixedHeight,
                                            isOdd, entry, listIndex, this.selection, this);
    }
}
