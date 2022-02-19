package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListSorter;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.widget.list.BaseListWidget;
import fi.dy.masa.malilib.gui.widget.SearchBarWidget;
import fi.dy.masa.malilib.config.value.HorizontalAlignment;

public class WidgetListMaterialList extends BaseListWidget<MaterialListEntry, WidgetMaterialListEntry>
{
    private static int lastScrollbarPosition;

    private final GuiMaterialList gui;
    private final MaterialListSorter sorter;
    private boolean scrollbarRestored;

    public WidgetListMaterialList(int x, int y, int width, int height, GuiMaterialList parent)
    {
        super(x, y, width, height, null);

        this.gui = parent;
        this.entryWidgetFixedHeight = 22;
        this.shouldSortList = true;
        this.sorter = new MaterialListSorter(parent.getMaterialList());

        this.addSearchBarWidget(new SearchBarWidget(x + 2, y + 8, width - 16, 14, 0, DefaultIcons.SEARCH, HorizontalAlignment.RIGHT)).setZLevel(1);
        this.setParentScreen(parent);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
    {
        super.render(mouseX, mouseY, isActiveGui, hoveredWidgetId);
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
        int height = this.entryWidgetFixedHeight;

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
    protected List<String> getEntryStringsForFilter(MaterialListEntry entry)
    {
        ItemStack stack = entry.getStack();
        ResourceLocation rl = Item.REGISTRY.getNameForObject(stack.getItem());

        if (rl != null)
        {
            return ImmutableList.of(stack.getDisplayName().toLowerCase(), rl.toString().toLowerCase());
        }
        else
        {
            return ImmutableList.of(stack.getDisplayName().toLowerCase());
        }
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
        return new WidgetMaterialListEntry(x, y, this.entryWidgetWidth, this.getBrowserEntryHeightFor(entry),
                                           isOdd, this.gui.getMaterialList(), entry, listIndex, this);
    }
}
