package fi.dy.masa.litematica.gui.widgets;

import java.util.Collections;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListSorter;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

public class WidgetListMaterialList extends WidgetListBase<MaterialListEntry, WidgetMaterialListEntry>
{
    private static int lastScrollbarPosition;

    private final GuiMaterialList gui;
    private final MaterialListSorter sorter;
    private boolean scrollbarRestored;

    public WidgetListMaterialList(int x, int y, int width, int height, GuiMaterialList parent)
    {
        super(x, y, width, height, null);

        this.browserEntryHeight = 22;
        this.gui = parent;
        this.sorter = new MaterialListSorter(parent.getMaterialList());

        this.setParent(parent);
        this.refreshData();
    }

    @Override
    public void drawContents(int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(mouseX, mouseY, partialTicks);
        lastScrollbarPosition = this.scrollBar.getValue();
    }

    @Override
    protected void offsetSelectionOrScrollbar(int amount, boolean changeSelection)
    {
        super.offsetSelectionOrScrollbar(amount, changeSelection);
        lastScrollbarPosition = this.scrollBar.getValue();
    }

    public void refreshData()
    {
        this.listContents.clear();
        this.listContents.addAll(this.gui.getMaterialList().getMaterialsFiltered(true));
        Collections.sort(this.listContents, this.sorter);
    }

    @Override
    protected int createAndAddHeaderWidget(int x, int y, int listIndexStart, int usableHeight, int usedHeight)
    {
        listIndexStart++;
        WidgetMaterialListEntry widget = this.createListEntryWidget(x, y, listIndexStart, true, null);
        int height = widget.getHeight();

        if ((usedHeight + height) > usableHeight)
        {
            return -1;
        }

        this.listWidgets.add(widget);
        this.maxVisibleBrowserEntries++;

        return height;
    }

    @Override
    public void refreshEntries()
    {
        this.refreshData();
        this.reCreateListEntryWidgets();

        if (this.scrollbarRestored == false && lastScrollbarPosition <= this.scrollBar.getMaxValue())
        {
            // This needs to happen after the setMaxValue() has been called in reCreateListEntryWidgets()
            this.scrollBar.setValue(lastScrollbarPosition);
            this.scrollbarRestored = true;
            this.reCreateListEntryWidgets();
        }
    }

    @Override
    protected WidgetMaterialListEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, @Nullable MaterialListEntry entry)
    {
        return new WidgetMaterialListEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                this.zLevel, isOdd, this.gui.getMaterialList(), entry, this);
    }
}
