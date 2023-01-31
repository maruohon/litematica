package litematica.gui;

import java.nio.file.Path;
import java.util.function.Consumer;

import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import malilib.overlay.message.MessageDispatcher;
import litematica.Reference;
import litematica.data.SchematicHolder;
import litematica.schematic.ISchematic;

public class SchematicSelectorScreen extends BaseSchematicBrowserScreen
{
    protected final GenericButton cancelButton;
    protected final GenericButton loadButton;
    protected final Consumer<ISchematic> schematicConsumer;

    public SchematicSelectorScreen(Consumer<ISchematic> schematicConsumer)
    {
        super(10, 24, 20 + 170 + 2, 70, "schematic_browser");

        this.schematicConsumer = schematicConsumer;
        this.cancelButton = GenericButton.create("litematica.button.misc.cancel", this::openParentScreen);
        this.loadButton   = GenericButton.create("litematica.button.misc.select", this::selectSchematic);

        this.setTitle("litematica.title.screen.schematic_browser", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.cancelButton);
        this.addWidget(this.loadButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.loadButton.setX(this.x + 10);
        this.loadButton.setBottom(this.getBottom() - 6);
        this.cancelButton.setPosition(this.loadButton.getRight() + 2, this.loadButton.getY());
    }

    protected void selectSchematic()
    {
        DirectoryEntry entry = this.getListWidget().getEntrySelectionHandler().getLastSelectedEntry();
        Path file = entry != null && entry.getType() == DirectoryEntryType.FILE ? entry.getFullPath() : null;

        if (file != null)
        {
            ISchematic schematic = SchematicHolder.getInstance().getOrLoad(file);

            if (schematic != null)
            {
                this.schematicConsumer.accept(schematic);
            }
        }
        else
        {
            MessageDispatcher.error("litematica.message.error.schematic_load.no_schematic_selected");
        }

        this.openParentScreen();
    }
}
