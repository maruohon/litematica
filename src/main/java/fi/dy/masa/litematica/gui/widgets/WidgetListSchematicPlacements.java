package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.util.FileUtils;

public class WidgetListSchematicPlacements extends WidgetListBase<SchematicPlacement, WidgetSchematicPlacement>
{
    public final GuiSchematicPlacementsList parent;

    public WidgetListSchematicPlacements(int x, int y, int width, int height, GuiSchematicPlacementsList parent)
    {
        super(x, y, width, height, parent);

        this.parent = parent;
        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.LEFT);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
    }

    public GuiSchematicPlacementsList getParentGui()
    {
        return this.parent;
    }

    @Override
    protected Collection<SchematicPlacement> getAllEntries()
    {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements();
    }

    @Override
    protected List<String> getEntryStringsForFilter(SchematicPlacement entry)
    {
        if (entry.getSchematic().getFile() != null)
        {
            String fileName = FileUtils.getNameWithoutExtension(entry.getSchematic().getFile().getName().toLowerCase());
            return ImmutableList.of(entry.getName().toLowerCase(), fileName);
        }
        else
        {
            return ImmutableList.of(entry.getName().toLowerCase());
        }
    }

    @Override
    protected WidgetSchematicPlacement createListEntryWidget(int x, int y, int listIndex, boolean isOdd, SchematicPlacement entry)
    {
        return new WidgetSchematicPlacement(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, entry, listIndex, this);
    }
}
