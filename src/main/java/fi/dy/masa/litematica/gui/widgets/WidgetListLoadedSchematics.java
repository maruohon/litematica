package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.util.GuiIconBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.HorizontalAlignment;

public class WidgetListLoadedSchematics extends WidgetListBase<ISchematic, WidgetSchematicEntry>
{
    public WidgetListLoadedSchematics(int x, int y, int width, int height,
            @Nullable ISelectionListener<ISchematic> selectionListener)
    {
        super(x, y, width, height, selectionListener);

        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, GuiIconBase.SEARCH, HorizontalAlignment.LEFT);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
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
            String fileName = FileUtils.getNameWithoutExtension(entry.getFile().getName().toLowerCase());
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
        return new WidgetSchematicEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, entry, listIndex, this);
    }
}
