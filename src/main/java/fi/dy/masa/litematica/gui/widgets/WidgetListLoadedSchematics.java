package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.widget.list.BaseListWidget;
import fi.dy.masa.malilib.gui.widget.SearchBarWidget;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.gui.position.HorizontalAlignment;

public class WidgetListLoadedSchematics extends BaseListWidget<ISchematic, WidgetSchematicEntry>
{
    public WidgetListLoadedSchematics(int x, int y, int width, int height)
    {
        super(x, y, width, height, null);

        this.entryWidgetFixedHeight = 22;
        this.addSearchBarWidget(new SearchBarWidget(x + 2, y + 4, width - 14, 14, 0, DefaultIcons.SEARCH, HorizontalAlignment.LEFT));
    }

    @Override
    protected Collection<ISchematic> getAllEntries()
    {
        return SchematicHolder.getInstance().getAllSchematics();
    }

    @Override
    protected List<String> getEntryStringsForFilter(ISchematic entry)
    {
        String metaName = entry.getMetadata().getName().toLowerCase();

        if (entry.getFile() != null)
        {
            String fileName = FileNameUtils.getFileNameWithoutExtension(entry.getFile().getName().toLowerCase());
            return ImmutableList.of(metaName, fileName);
        }
        else
        {
            return ImmutableList.of(metaName);
        }
    }

    @Override
    protected WidgetSchematicEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, ISchematic entry)
    {
        return new WidgetSchematicEntry(x, y, this.entryWidgetWidth, this.getBrowserEntryHeightFor(entry),
                                        isOdd, entry, listIndex, this);
    }
}
