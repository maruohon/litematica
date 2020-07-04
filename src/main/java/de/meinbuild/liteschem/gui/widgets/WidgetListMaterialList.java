package de.meinbuild.liteschem.gui.widgets;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import de.meinbuild.liteschem.gui.Icons;
import de.meinbuild.liteschem.materials.MaterialListEntry;
import de.meinbuild.liteschem.materials.MaterialListSorter;
import de.meinbuild.liteschem.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

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
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 8, width - 16, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.RIGHT);
        this.widgetSearchBar.setZLevel(1);
        this.sorter = new MaterialListSorter(parent.getMaterialList());
        this.shouldSortList = true;

        this.setParent(parent);
    }

    @Override
    public void drawContents(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(matrixStack, mouseX, mouseY, partialTicks);
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
    protected List<String> getEntryStringsForFilter(MaterialListEntry entry)
    {
        ItemStack stack = entry.getStack();
        Identifier rl = Registry.ITEM.getId(stack.getItem());

        if (rl != null)
        {
            return ImmutableList.of(stack.getName().getString().toLowerCase(), rl.toString().toLowerCase());
        }
        else
        {
            return ImmutableList.of(stack.getName().getString().toLowerCase());
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
        return new WidgetMaterialListEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, this.gui.getMaterialList(), entry, listIndex, this);
    }
}
