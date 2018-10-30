package fi.dy.masa.litematica.gui.widgets;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

public class WidgetListSchematicPlacements extends WidgetListBase<SchematicPlacement, WidgetSchematicPlacement>
{
    private final GuiPlacementManager parent;

    public WidgetListSchematicPlacements(int x, int y, int width, int height, GuiPlacementManager parent)
    {
        super(x, y, width, height, parent);

        this.parent = parent;
        this.browserEntryHeight = 22;
    }

    public GuiPlacementManager getParentGui()
    {
        return this.parent;
    }

    @Override
    public void refreshEntries()
    {
        this.listContents.clear();
        this.listContents.addAll(DataManager.getSchematicPlacementManager().getAllSchematicsPlacements());

        this.reCreateListEntryWidgets();
    }

    @Override
    protected WidgetSchematicPlacement createListEntryWidget(int x, int y, boolean isOdd, SchematicPlacement entry)
    {
        return new WidgetSchematicPlacement(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), this.zLevel, isOdd, entry, this, this.mc);
    }
}
