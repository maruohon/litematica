package fi.dy.masa.litematica.gui.widgets;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.gui.interfaces.IMessageConsumer;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.base.WidgetListBase;
import net.minecraft.client.Minecraft;

public class WidgetSchematicPlacements extends WidgetListBase<SchematicPlacement, WidgetSchematicPlacement>
{
    private final IMessageConsumer messageConsumer;

    public WidgetSchematicPlacements(int x, int y, int width, int height,
            IMessageConsumer messageConsumer, @Nullable ISelectionListener<SchematicPlacement> selectionListener)
    {
        super(x, y, width, height, selectionListener);

        this.messageConsumer = messageConsumer;
        this.browserEntryHeight = 22;
    }

    public IMessageConsumer getMessageConsumer()
    {
        return this.messageConsumer;
    }

    @Override
    public void refreshEntries()
    {
        this.listContents.clear();

        Minecraft mc = Minecraft.getMinecraft();
        int dimension = mc.world.provider.getDimensionType().getId();
        this.listContents.addAll(DataManager.getInstance(dimension).getSchematicPlacementManager().getAllSchematicsPlacements());

        this.scrollBar.setMaxValue(this.listContents.size() - this.maxVisibleBrowserEntries);

        this.updateBrowserMaxVisibleEntries();
        this.recreateListWidgets();
    }

    @Override
    protected WidgetSchematicPlacement createListWidget(int x, int y, boolean isOdd, SchematicPlacement entry)
    {
        return new WidgetSchematicPlacement(x, y, this.browserEntryWidth, this.browserEntryHeight, this.zLevel, entry, this, this.mc);
    }
}
