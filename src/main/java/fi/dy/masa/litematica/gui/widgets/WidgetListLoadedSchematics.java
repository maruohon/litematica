package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import net.minecraft.client.Minecraft;

public class WidgetListLoadedSchematics extends WidgetListBase<LitematicaSchematic, WidgetSchematicEntry>
{
    public WidgetListLoadedSchematics(int x, int y, int width, int height, float zLevel,
            @Nullable ISelectionListener<LitematicaSchematic> selectionListener)
    {
        super(x, y, width, height, selectionListener);

        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, zLevel, 0, Icons.FILE_ICON_SEARCH, LeftRight.LEFT, Minecraft.getMinecraft());
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
    }

    @Override
    protected Collection<LitematicaSchematic> getAllEntries()
    {
        return SchematicHolder.getInstance().getAllSchematics();
    }

    @Override
    protected boolean entryMatchesFilter(LitematicaSchematic entry, String filterText)
    {
        return entry.getMetadata().getName().toLowerCase().indexOf(filterText) != -1 ||
               (entry.getFile() != null && entry.getFile().getName().toLowerCase().indexOf(filterText) != -1);
    }

    @Override
    protected WidgetSchematicEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, LitematicaSchematic entry)
    {
        return new WidgetSchematicEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                this.zLevel, isOdd, entry, listIndex, this, this.mc);
    }
}
