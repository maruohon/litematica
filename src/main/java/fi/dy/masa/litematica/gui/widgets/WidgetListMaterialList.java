package fi.dy.masa.litematica.gui.widgets;

import java.util.Collections;
import java.util.List;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.util.MaterialListEntry;
import fi.dy.masa.litematica.util.SchematicUtils;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

public class WidgetListMaterialList extends WidgetListBase<MaterialListEntry, WidgetMaterialListEntry>
{
    private final GuiMaterialList gui;
    private final List<MaterialListEntry> cachedList;

    public WidgetListMaterialList(int x, int y, int width, int height, GuiMaterialList parent)
    {
        super(x, y, width, height, null);

        this.browserEntryHeight = 22;
        this.gui = parent;
        this.setParent(parent);

        this.cachedList = SchematicUtils.createMaterialListFor(parent.getSchematicPlacement());
        this.listContents.addAll(this.cachedList);
    }

    @Override
    public void refreshEntries()
    {
        this.listContents.clear();

        this.listContents.addAll(this.cachedList);
        Collections.sort(this.listContents);

        this.listContents.add(0, null); // title row

        this.reCreateListEntryWidgets();
    }

    @Override
    protected WidgetMaterialListEntry createListEntryWidget(int x, int y, boolean isOdd, MaterialListEntry entry)
    {
        return new WidgetMaterialListEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), this.zLevel, isOdd, entry, this.gui);
    }
}
