package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.malilib.gui.util.GuiIconBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.HorizontalAlignment;

public class WidgetListSchematicPlacements extends WidgetListBase<SchematicPlacementUnloaded, WidgetSchematicPlacementEntry>
{
    private final GuiSchematicPlacementsList parent;

    public WidgetListSchematicPlacements(int x, int y, int width, int height, GuiSchematicPlacementsList parent)
    {
        super(x, y, width, height, parent);

        this.parent = parent;
        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 17, 14, 0, GuiIconBase.SEARCH, HorizontalAlignment.LEFT);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
    }

    public GuiSchematicPlacementsList getParentGui()
    {
        return this.parent;
    }

    @Override
    protected Collection<SchematicPlacementUnloaded> getAllEntries()
    {
        return DataManager.getSchematicPlacementManager().getAllSchematicPlacements();
    }

    @Override
    protected List<String> getEntryStringsForFilter(SchematicPlacementUnloaded entry)
    {
        if (entry.getSchematicFile() != null)
        {
            String fileName = FileUtils.getNameWithoutExtension(entry.getSchematicFile().getName().toLowerCase());
            return ImmutableList.of(entry.getName().toLowerCase(), fileName);
        }
        else
        {
            return ImmutableList.of(entry.getName().toLowerCase());
        }
    }

    @Override
    protected WidgetSchematicPlacementEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, SchematicPlacementUnloaded entry)
    {
        return new WidgetSchematicPlacementEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, entry, listIndex, this);
    }
}
