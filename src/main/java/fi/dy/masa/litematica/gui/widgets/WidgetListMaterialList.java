package fi.dy.masa.litematica.gui.widgets;

import java.util.Collections;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListSorter;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

public class WidgetListMaterialList extends WidgetListBase<MaterialListEntry, WidgetMaterialListEntry>
{
    private final GuiMaterialList gui;
    private final MaterialListSorter sorter;

    public WidgetListMaterialList(int x, int y, int width, int height, GuiMaterialList parent)
    {
        super(x, y, width, height, null);

        this.browserEntryHeight = 22;
        this.gui = parent;
        this.sorter = new MaterialListSorter(parent.getMaterialList());

        this.setParent(parent);
        this.refreshData();
    }

    public void refreshData()
    {
        this.listContents.clear();

        this.listContents.addAll(this.gui.getMaterialList().getMaterials());
        Collections.sort(this.listContents, this.sorter);

        this.listContents.add(0, null); // title row
    }

    @Override
    public void refreshEntries()
    {
        this.refreshData();
        this.reCreateListEntryWidgets();
    }

    @Override
    protected WidgetMaterialListEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, MaterialListEntry entry)
    {
        return new WidgetMaterialListEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                this.zLevel, isOdd, this.gui.getMaterialList(), entry, this);
    }
}
