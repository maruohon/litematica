package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import net.minecraft.client.Minecraft;

public class WidgetListSchematicPlacements extends WidgetListBase<SchematicPlacement, WidgetSchematicPlacement>
{
    private final GuiSchematicPlacementsList parent;

    public WidgetListSchematicPlacements(int x, int y, int width, int height, float zLevel, GuiSchematicPlacementsList parent)
    {
        super(x, y, width, height, parent);

        this.parent = parent;
        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, zLevel, 0, Icons.FILE_ICON_SEARCH, LeftRight.LEFT, Minecraft.getMinecraft());
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
    }

    public GuiSchematicPlacementsList getParentGui()
    {
        return this.parent;
    }

    @Override
    protected Collection<SchematicPlacement> getAllEntries()
    {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements();
    }

    @Override
    protected boolean entryMatchesFilter(SchematicPlacement entry, String filterText)
    {
        return entry.getName().toLowerCase().indexOf(filterText) != -1 ||
               entry.getSchematic().getMetadata().getName().toLowerCase().indexOf(filterText) != -1 ||
               (entry.getSchematic().getFile() != null && entry.getSchematic().getFile().getName().toLowerCase().indexOf(filterText) != -1);
    }

    @Override
    protected WidgetSchematicPlacement createListEntryWidget(int x, int y, int listIndex, boolean isOdd, SchematicPlacement entry)
    {
        return new WidgetSchematicPlacement(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                this.zLevel, isOdd, entry, listIndex, this, this.mc);
    }
}
