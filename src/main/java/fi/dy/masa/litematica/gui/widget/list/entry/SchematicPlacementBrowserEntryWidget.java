package fi.dy.masa.litematica.gui.widget.list.entry;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import fi.dy.masa.malilib.gui.icon.FileBrowserIconProvider;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetData;
import fi.dy.masa.malilib.gui.widget.list.entry.DirectoryEntryWidget;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.FileType;

public class SchematicPlacementBrowserEntryWidget extends DirectoryEntryWidget
{
    protected final GenericButton loadButton;
    protected final GenericButton removeButton;
    protected final boolean isSelectionEntry;

    public SchematicPlacementBrowserEntryWidget(DirectoryEntry entry,
                                                DataListEntryWidgetData constructData,
                                                BaseFileBrowserWidget fileBrowserWidget,
                                                @Nullable FileBrowserIconProvider iconProvider)
    {
        super(entry, constructData, fileBrowserWidget, iconProvider);

        this.isSelectionEntry = FileType.fromFileName(entry.getFullPath()) == FileType.JSON && entry.getType() == DirectoryEntryType.FILE;
        this.loadButton   = GenericButton.create(18, "litematica.button.misc.load", this::loadSavedPlacement);
        this.removeButton = GenericButton.create(18, "litematica.button.misc.remove", this::removeSavedPlacement);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        if (this.isSelectionEntry)
        {
            this.addWidget(this.loadButton);
            this.addWidget(this.removeButton);
        }
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        if (this.isSelectionEntry)
        {
            this.loadButton.centerVerticallyInside(this);
            this.removeButton.centerVerticallyInside(this);
            this.removeButton.setRight(this.getRight() - 2);
            this.loadButton.setRight(this.removeButton.getX() - 1);
        }
    }

    protected void loadSavedPlacement()
    {
        File file = this.data.getFullPath();
        DataManager.getSchematicPlacementManager().loadPlacementFromFile(file);
    }

    protected void removeSavedPlacement()
    {
        this.scheduleTask(() -> {
            File file = this.data.getFullPath();
            FileUtils.deleteFiles(Collections.singletonList(file), MessageDispatcher::error);
            this.listWidget.clearSelection();
            this.listWidget.refreshEntries();
        });
    }

    public static boolean placementSearchFilter(DirectoryEntry entry, List<String> searchTerms)
    {
        String fileName = FileNameUtils.getFileNameWithoutExtension(entry.getName()).toLowerCase(Locale.ROOT);

        for (String searchTerm : searchTerms)
        {
            if (fileName.contains(searchTerm))
            {
                return true;
            }
        }

        return false;
    }
}
