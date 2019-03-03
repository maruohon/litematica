package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.Comparator;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListSorter;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

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
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 8, width - 16, 14, 1, 0, Icons.FILE_ICON_SEARCH, LeftRight.RIGHT, Minecraft.getMinecraft());
        this.sorter = new MaterialListSorter(parent.getMaterialList());
        this.shouldSortList = true;

        this.setParent(parent);
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

    @Override
    protected WidgetMaterialListEntry createHeaderWidget(int x, int y, int listIndexStart, int usableHeight, int usedHeight)
    {
        int height = this.browserEntryHeight;

        if ((usedHeight + height) > usableHeight)
        {
            return null;
        }

        return this.createListEntryWidget(x, y, listIndexStart, true, null);
    }

    @Override
    protected Collection<MaterialListEntry> getAllEntries()
    {
        return this.gui.getMaterialList().getMaterialsFiltered(true);
    }

    @Override
    protected Comparator<MaterialListEntry> getComparator()
    {
        return this.sorter;
    }

    @Override
    protected boolean entryMatchesFilter(MaterialListEntry entry, String filterText)
    {
        ItemStack stack = entry.getStack();
        ResourceLocation rl = Item.REGISTRY.getNameForObject(stack.getItem());
        String regName = rl != null ? rl.toString() : "";

        return stack.getDisplayName().toLowerCase().indexOf(filterText) != -1 ||
               regName.indexOf(filterText) != -1;
    }

    @Override
    protected void refreshBrowserEntries()
    {
        super.refreshBrowserEntries();

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
                this.zLevel, isOdd, this.gui.getMaterialList(), entry, listIndex, this);
    }
}
