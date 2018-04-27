package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.WidgetLoadedSchematics.SchematicEntry;
import fi.dy.masa.litematica.gui.widgets.base.WidgetListBase;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicHolder;
import net.minecraft.client.resources.I18n;

public class WidgetLoadedSchematics extends WidgetListBase<SchematicEntry, WidgetSchematicEntry>
{
    public WidgetLoadedSchematics(int x, int y, int width, int height, @Nullable ISelectionListener<SchematicEntry> selectionListener)
    {
        super(x, y, width, height, selectionListener);

        this.browserEntryHeight = 22;
    }

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.loaded_schematics");
    }

    @Override
    public void refreshEntries()
    {
        this.listContents.clear();

        SchematicHolder holder = SchematicHolder.getInstance();
        List<Integer> list = new ArrayList<>(holder.getAllSchematicsIds());

        if (list.isEmpty() == false)
        {
            Collections.sort(list);

            for (int id : list)
            {
                this.listContents.add(new SchematicEntry(id, holder.getSchematic(id)));
            }
        }

        this.scrollBar.setMaxValue(this.listContents.size() - this.maxVisibleBrowserEntries);

        this.updateBrowserMaxVisibleEntries();
        this.recreateListWidgets();
    }

    @Override
    protected WidgetSchematicEntry createListWidget(int x, int y, boolean isOdd, SchematicEntry entry)
    {
        return new WidgetSchematicEntry(x, y, this.browserEntryWidth, this.browserEntryHeight, this.zLevel, entry, this, this.mc);
    }

    public static class SchematicEntry
    {
        public final LitematicaSchematic schematic;
        public final int schematicId;

        public SchematicEntry(int schematicId, LitematicaSchematic schematic)
        {
            this.schematicId = schematicId;
            this.schematic = schematic;
        }
    }
}
