package litematica.gui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import malilib.gui.BaseListScreen;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.DataListWidget;
import litematica.data.SchematicHolder;
import litematica.gui.widget.SchematicInfoWidgetBySchematic;
import litematica.gui.widget.list.entry.BaseSchematicEntryWidget;
import litematica.schematic.ISchematic;

public class SelectLoadedSchematicScreen extends BaseListScreen<DataListWidget<ISchematic>>
{
    protected final GenericButton selectSchematicButton;
    protected final SchematicInfoWidgetBySchematic schematicInfoWidget;
    protected final Consumer<ISchematic> schematicConsumer;

    public SelectLoadedSchematicScreen(Consumer<ISchematic> schematicConsumer)
    {
        super(10, 30, 192, 56);

        this.schematicConsumer = schematicConsumer;
        this.schematicInfoWidget = new SchematicInfoWidgetBySchematic(170, 290);
        this.selectSchematicButton = GenericButton.create("litematica.button.select_schematic.confirm", this::onSelectButtonClicked);

        this.setTitle("litematica.title.screen.select_loaded_schematic");
        this.addPreScreenCloseListener(this::clearSchematicInfoCache);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.schematicInfoWidget);
        this.addWidget(this.selectSchematicButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int y = this.getBottom() - 24;
        this.selectSchematicButton.setPosition(this.x + 10, y);

        this.schematicInfoWidget.setHeight(this.getListHeight());
        this.schematicInfoWidget.setRight(this.getRight() - 10);
        this.schematicInfoWidget.setY(this.getListY());
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
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);

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

    public void onSelectionChange(@Nullable ISchematic entry)
    {
        this.schematicInfoWidget.onSelectionChange(entry);
    }

    protected void clearSchematicInfoCache()
    {
        this.schematicInfoWidget.clearCache();
    }
}
