package fi.dy.masa.litematica.gui.widgets;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.data.SchematicHolder.SchematicEntry;
import fi.dy.masa.litematica.gui.interfaces.IMessageConsumer;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.base.WidgetListBase;

public class WidgetLoadedSchematics extends WidgetListBase<SchematicEntry, WidgetSchematicEntry>
{
    private final IMessageConsumer messageConsumer;

    public WidgetLoadedSchematics(int x, int y, int width, int height,
            IMessageConsumer messageConsumer, @Nullable ISelectionListener<SchematicEntry> selectionListener)
    {
        super(x, y, width, height, selectionListener);

        this.messageConsumer = messageConsumer;
        this.browserEntryHeight = 22;
    }

    public IMessageConsumer getMessageConsumer()
    {
        return this.messageConsumer;
    }

    @Override
    public void refreshEntries()
    {
        this.listContents.clear();

        SchematicHolder holder = SchematicHolder.getInstance();
        this.listContents.addAll(holder.getAllSchematics());

        this.scrollBar.setMaxValue(this.listContents.size() - this.maxVisibleBrowserEntries);

        this.updateBrowserMaxVisibleEntries();
        this.recreateListWidgets();
    }

    @Override
    protected WidgetSchematicEntry createListWidget(int x, int y, boolean isOdd, SchematicEntry entry)
    {
        return new WidgetSchematicEntry(x, y, this.browserEntryWidth, this.browserEntryHeight, this.zLevel, entry, this, this.mc);
    }
}
