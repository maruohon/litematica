package fi.dy.masa.litematica.gui.widgets;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

public class WidgetListSchematicPlacements extends WidgetListBase<SchematicPlacement, WidgetSchematicPlacement>
{
    private final GuiSchematicPlacementsList parent;

    public WidgetListSchematicPlacements(int x, int y, int width, int height, GuiSchematicPlacementsList parent)
    {
        super(x, y, width, height, parent);

        this.parent = parent;
        this.browserEntryHeight = 22;
    }

    public GuiSchematicPlacementsList getParentGui()
    {
        return this.parent;
    }

    @Override
    protected void refreshBrowserEntries()
    {
        this.listContents.clear();
        this.listContents.addAll(DataManager.getSchematicPlacementManager().getAllSchematicsPlacements());

        this.reCreateListEntryWidgets();
    }

    @Override
    protected WidgetSchematicPlacement createListEntryWidget(int x, int y, int listIndex, boolean isOdd, SchematicPlacement entry)
    {
        return new WidgetSchematicPlacement(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                this.zLevel, isOdd, entry, listIndex, this, this.mc);
    }
}
