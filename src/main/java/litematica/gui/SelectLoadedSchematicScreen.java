package litematica.gui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import malilib.gui.BaseListScreen;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.DataListWidget;
import litematica.data.SchematicHolder;
import litematica.gui.widget.list.entry.BaseSchematicEntryWidget;
import litematica.schematic.ISchematic;

public class SelectLoadedSchematicScreen extends BaseListScreen<DataListWidget<ISchematic>>
{
    protected final GenericButton selectSchematicButton;
    protected final Consumer<ISchematic> schematicConsumer;

    public SelectLoadedSchematicScreen(Consumer<ISchematic> schematicConsumer)
    {
        super(10, 30, 20, 56);

        this.schematicConsumer = schematicConsumer;
        this.selectSchematicButton = GenericButton.create("litematica.button.select_schematic.confirm", this::onSelectButtonClicked);

        this.setTitle("litematica.title.screen.select_loaded_schematic");
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.selectSchematicButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int y = this.getBottom() - 24;
        this.selectSchematicButton.setPosition(this.x + 10, y);
    }

    @Override
    protected DataListWidget<ISchematic> createListWidget()
    {
        Supplier<List<ISchematic>> supplier = SchematicHolder.getInstance()::getAllSchematics;
        DataListWidget<ISchematic> listWidget = new DataListWidget<>(supplier, true);
        listWidget.addDefaultSearchBar();
        listWidget.setEntryFilter(BaseSchematicEntryWidget::schematicSearchFilter);
        listWidget.setDataListEntryWidgetFactory(BaseSchematicEntryWidget::new);
        listWidget.setAllowSelection(true);

        return listWidget;
    }

    protected void onSelectButtonClicked()
    {
        ISchematic schematic = this.getListWidget().getLastSelectedEntry();

        if (schematic != null)
        {
            this.openParentScreen();
            this.schematicConsumer.accept(schematic);
        }
    }
}
