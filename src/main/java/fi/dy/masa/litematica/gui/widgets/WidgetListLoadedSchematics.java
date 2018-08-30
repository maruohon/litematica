package fi.dy.masa.litematica.gui.widgets;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.interfaces.IMessageConsumer;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

public class WidgetListLoadedSchematics extends WidgetListBase<LitematicaSchematic, WidgetSchematicEntry>
{
    private final IMessageConsumer messageConsumer;

    public WidgetListLoadedSchematics(int x, int y, int width, int height,
            IMessageConsumer messageConsumer, @Nullable ISelectionListener<LitematicaSchematic> selectionListener)
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

        this.reCreateListEntryWidgets();
    }

    @Override
    protected WidgetSchematicEntry createListEntryWidget(int x, int y, boolean isOdd, LitematicaSchematic entry)
    {
        return new WidgetSchematicEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), this.zLevel, isOdd, entry, this, this.mc);
    }
}
