package fi.dy.masa.litematica.gui.widgets;

import java.io.FileFilter;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiAreaSelectionManager;
import fi.dy.masa.malilib.gui.widget.list.entry.SelectionListener;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetAreaSelectionBrowser extends BaseFileBrowserWidget
{
    public static final FileFilter JSON_FILTER = (file) -> file.getName().endsWith(".json");

    private final GuiAreaSelectionManager guiAreaSelectionManager;

    public WidgetAreaSelectionBrowser(int x, int y, int width, int height,
            GuiAreaSelectionManager parent, SelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, parent.getDefaultDirectory(), DataManager.getAreaSelectionsBaseDirectory(),
                DataManager.getDirectoryCache(), parent.getBrowserContext(), selectionListener);

        this.entryWidgetFixedHeight = 22;
        this.guiAreaSelectionManager = parent;
        this.allowKeyboardNavigation = false;
    }

    public GuiAreaSelectionManager getSelectionManagerGui()
    {
        return this.guiAreaSelectionManager;
    }

    @Override
    protected FileFilter getFileFilter()
    {
        return JSON_FILTER;
    }

    @Override
    protected String getRootDirectoryDisplayName()
    {
        return StringUtils.translate("litematica.gui.label.area_selection_browser.selections");
    }

    @Override
    protected WidgetAreaSelectionEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, DirectoryEntry entry)
    {
        return new WidgetAreaSelectionEntry(x, y, this.entryWidgetWidth, this.getBrowserEntryHeightFor(entry), isOdd,
                                            entry, listIndex, this.guiAreaSelectionManager.getSelectionManager(), this, this.iconProvider);
    }
}
