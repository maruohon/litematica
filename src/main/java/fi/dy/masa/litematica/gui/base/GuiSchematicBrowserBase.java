package fi.dy.masa.litematica.gui.base;

import java.io.File;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntry;

public abstract class GuiSchematicBrowserBase extends GuiListBase<DirectoryEntry, WidgetDirectoryEntry, WidgetSchematicBrowser>
{
    public GuiSchematicBrowserBase(int browserX, int browserY)
    {
        super(browserX, browserY);
    }

    @Override
    protected WidgetSchematicBrowser createListWidget(int listX, int listY)
    {
        // The width and height will be set to the actual values in initGui()
        WidgetSchematicBrowser widget = new WidgetSchematicBrowser(listX, listY, 100, 100, this, this.getSelectionListener());
        widget.setParent(this.parent);
        return widget;
    }

    public File getInitialDirectory()
    {
        return DataManager.getCurrentSchematicDirectory();
    }

    public void storeCurrentDirectory(File dir)
    {
        DataManager.setCurrentSchematicDirectory(dir);
    }

    @Override
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return null;
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 80;
    }
}
