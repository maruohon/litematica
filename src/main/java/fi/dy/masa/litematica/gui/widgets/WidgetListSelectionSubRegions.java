package fi.dy.masa.litematica.gui.widgets;

import java.util.Collections;
import fi.dy.masa.litematica.gui.GuiAreaSelectionEditorNormal;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

public class WidgetListSelectionSubRegions extends WidgetListBase<String, WidgetSelectionSubRegion>
{
    private final GuiAreaSelectionEditorNormal gui;
    private final AreaSelection selection;

    public WidgetListSelectionSubRegions(int x, int y, int width, int height,
            AreaSelection selection, GuiAreaSelectionEditorNormal gui)
    {
        super(x, y, width, height, gui);

        this.gui = gui;
        this.selection = selection;
        this.browserEntryHeight = 22;
    }

    public GuiAreaSelectionEditorNormal getEditorGui()
    {
        return this.gui;
    }

    @Override
    public void refreshEntries()
    {
        this.listContents.clear();
        this.listContents.addAll(this.selection.getAllSubRegionNames());
        Collections.sort(this.listContents);

        this.reCreateListEntryWidgets();
    }

    @Override
    protected WidgetSelectionSubRegion createListEntryWidget(int x, int y, int listIndex, boolean isOdd, String entry)
    {
        return new WidgetSelectionSubRegion(x, y, this.browserEntryWidth, this.browserEntryHeight,
                this.zLevel, isOdd, entry, listIndex, this.mc, this.selection, this);
    }
}
