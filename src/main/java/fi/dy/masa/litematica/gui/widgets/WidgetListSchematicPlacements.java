package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.widget.list.BaseListWidget;
import fi.dy.masa.malilib.gui.widget.SearchBarWidget;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.config.value.HorizontalAlignment;

public class WidgetListSchematicPlacements extends BaseListWidget<SchematicPlacementUnloaded, WidgetSchematicPlacementEntry>
{
    private final GuiSchematicPlacementsList gui;

    public WidgetListSchematicPlacements(int x, int y, int width, int height, GuiSchematicPlacementsList gui)
    {
        super(x, y, width, height, gui);

        this.gui = gui;
        this.entryWidgetFixedHeight = 22;

        this.addSearchBarWidget(new SearchBarWidget(x + 2, y + 4, width - 17, 14, 0, DefaultIcons.SEARCH, HorizontalAlignment.LEFT));
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
            String fileName = FileNameUtils.getFileNameWithoutExtension(entry.getSchematicFile().getName().toLowerCase());
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
        return new WidgetSchematicPlacementEntry(x, y, this.entryWidgetWidth, this.getBrowserEntryHeightFor(entry),
                                                 isOdd, entry, listIndex, this, this.gui);
    }
}
