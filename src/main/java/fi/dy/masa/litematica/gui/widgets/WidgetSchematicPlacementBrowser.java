package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementFileBrowser;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;

public class WidgetSchematicPlacementBrowser extends BaseFileBrowserWidget
{
    protected final Map<File, SchematicPlacementUnloaded> cache = new HashMap<>();

    public WidgetSchematicPlacementBrowser(int x, int y, int width, int height, GuiSchematicPlacementFileBrowser parent)
    {
        super(x, y, width, height, parent.getDefaultDirectory(), SchematicPlacementUnloaded.getSaveDirectory(),
                DataManager.getDirectoryCache(), parent.getBrowserContext(), null);

        this.entryWidgetFixedHeight = 22;
        this.allowKeyboardNavigation = false;
        this.setParentScreen(parent);
    }

    @Nullable
    public SchematicPlacementUnloaded getOrLoadPlacement(File file)
    {
        SchematicPlacementUnloaded placement = this.cache.get(file);

        if (placement == null && this.cache.containsKey(file) == false)
        {
            placement = SchematicPlacementUnloaded.fromFile(file);
            this.cache.put(file, placement);
        }

        return placement;
    }

    @Override
    protected Comparator<DirectoryEntry> getComparator()
    {
        return (e1, e2) -> {
            SchematicPlacementUnloaded p1 = this.getOrLoadPlacement(e1.getFullPath());
            SchematicPlacementUnloaded p2 = this.getOrLoadPlacement(e2.getFullPath());

            if (p1 != null && p2 != null)
            {
                return p1.getName().compareTo(p2.getName());
            }

            return super.getComparator().compare(e1, e2);
        };
    }

    @Override
    protected List<String> getFilterTargetStringsForEntry(DirectoryEntry entry)
    {
        String fileName = entry.getName().toLowerCase(Locale.ROOT);
        SchematicPlacementUnloaded placement = this.getOrLoadPlacement(entry.getFullPath());

        if (placement != null)
        {
            return ImmutableList.of(placement.getName().toLowerCase(Locale.ROOT), fileName);
        }
        else
        {
            return ImmutableList.of(fileName);
        }
    }

    @Override
    protected FileFilter getFileFilter()
    {
        return WidgetAreaSelectionBrowser.JSON_FILTER;
    }

    @Override
    protected WidgetSchematicPlacementFileEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, DirectoryEntry entry)
    {
        return new WidgetSchematicPlacementFileEntry(x, y, this.entryWidgetWidth,
                this.getBrowserEntryHeightFor(entry), isOdd, entry, listIndex, this);
    }
}
