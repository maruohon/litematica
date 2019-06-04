package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.io.FileFilter;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiAreaSelectionManager;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;

public class WidgetAreaSelectionBrowser extends WidgetFileBrowserBase
{
    public static final FileFilter JSON_FILTER = new FileFilterJson();

    private final GuiAreaSelectionManager guiAreaSelectionManager;

    public WidgetAreaSelectionBrowser(int x, int y, int width, int height,
            GuiAreaSelectionManager parent, ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, DataManager.getDirectoryCache(), parent.getBrowserContext(),
                parent.getDefaultDirectory(), selectionListener, Icons.DUMMY);

        this.browserEntryHeight = 22;
        this.guiAreaSelectionManager = parent;
        this.allowKeyboardNavigation = false;
    }

    public GuiAreaSelectionManager getSelectionManagerGui()
    {
        return this.guiAreaSelectionManager;
    }

    @Override
    protected File getRootDirectory()
    {
        return DataManager.getAreaSelectionsBaseDirectory();
    }

    @Override
    protected FileFilter getFileFilter()
    {
        return JSON_FILTER;
    }

    @Override
    protected WidgetAreaSelectionEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, DirectoryEntry entry)
    {
        return new WidgetAreaSelectionEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), isOdd,
                entry, listIndex, this.guiAreaSelectionManager.getSelectionManager(), this, this.iconProvider);
    }

    public static class FileFilterJson implements FileFilter
    {
        @Override
        public boolean accept(File pathName)
        {
            return pathName.getName().endsWith(".json");
        }
    }
}
