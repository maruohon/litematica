package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widget.list.entry.DirectoryEntryWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;

public abstract class GuiSchematicBrowserBase extends BaseListScreen<DirectoryEntry, DirectoryEntryWidget, WidgetSchematicBrowser>
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
        widget.setParentScreen(this.getParent());
        return widget;
    }

    /**
     * This is the string the DataManager uses for saving/loading/storing the last used directory
     * for each browser GUI type/contet.
     * @return
     */
    public abstract String getBrowserContext();

    public abstract File getDefaultDirectory();

    @Override
    @Nullable
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return null;
    }

    @Override
    protected int getListWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getListHeight()
    {
        return this.height - 70;
    }

    public int getMaxInfoHeight()
    {
        return this.getListHeight();
    }
}
