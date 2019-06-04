package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.util.FileUtils;

public class WidgetListLoadedSchematics extends WidgetListBase<LitematicaSchematic, WidgetSchematicEntry>
{
    public WidgetListLoadedSchematics(int x, int y, int width, int height,
            @Nullable ISelectionListener<LitematicaSchematic> selectionListener)
    {
        super(x, y, width, height, selectionListener);

        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.LEFT);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
    }

    @Override
    protected Collection<LitematicaSchematic> getAllEntries()
    {
        return SchematicHolder.getInstance().getAllSchematics();
    }

    @Override
    protected List<String> getEntryStringsForFilter(LitematicaSchematic entry)
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
    protected WidgetSchematicEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, LitematicaSchematic entry)
    {
        return new WidgetSchematicEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, entry, listIndex, this);
    }
}
