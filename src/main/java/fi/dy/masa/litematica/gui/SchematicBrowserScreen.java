package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.SchematicType;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;

public class SchematicBrowserScreen extends BaseSchematicBrowserScreen
{
    protected final GenericButton loadButton;
    protected final GenericButton materialListButton;
    protected final CheckBoxWidget createPlacementCheckbox;

    public SchematicBrowserScreen()
    {
        super(10, 24, 20 + 170 + 2, 70, "schematic_browser");

        this.loadButton             = GenericButton.create("litematica.button.schematic_browser.load_schematic", this::loadSchematic);
        this.materialListButton     = GenericButton.create("litematica.button.misc.material_list", this::createMaterialList);
        this.materialListButton.translateAndAddHoverString("litematica.hover.button.schematic_browser.create_material_list");
        this.createPlacementCheckbox = new CheckBoxWidget("litematica.checkmark.schematic_browser.create_placement",
                                                          "litematica.hover.schematic_browser.create_placement");
        this.createPlacementCheckbox.setBooleanStorage(Configs.Internal.CREATE_PLACEMENT_ON_LOAD);

        this.setTitle("litematica.title.screen.schematic_browser", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.createPlacementCheckbox);
        this.addWidget(this.loadButton);
        this.addWidget(this.mainMenuScreenButton);
        this.addWidget(this.materialListButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.loadButton.setX(this.x + 10);
        this.loadButton.setBottom(this.getBottom() - 6);
        this.createPlacementCheckbox.setPosition(this.loadButton.getX(), this.loadButton.getY() - 14);
        this.materialListButton.setPosition(this.loadButton.getRight() + 4, this.loadButton.getY());

        this.mainMenuScreenButton.setRight(this.getRight() - 10);
        this.mainMenuScreenButton.setY(this.loadButton.getY());
    }

    protected void loadSchematic()
    {
        ISchematic schematic = tryLoadSchematic(this.getListWidget().getEntrySelectionHandler().getLastSelectedEntry());

        if (schematic == null)
        {
            return;
        }

        SchematicHolder.getInstance().addSchematic(schematic, true);
        MessageDispatcher.success("litematica.message.info.schematic_loaded_to_memory", schematic.getFile().getName());

        // Clear the parent after loading as schematic, as presumably in most cases
        // the user would just want to close the screen at that point.
        this.setParent(null);

        if (Configs.Internal.CREATE_PLACEMENT_ON_LOAD.getBooleanValue())
        {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.createPlacementForNewlyLoadedSchematic(schematic, BaseScreen.isShiftDown() == false);
        }
    }

    protected void createMaterialList()
    {
        ISchematic schematic = tryLoadSchematic(this.getListWidget().getEntrySelectionHandler().getLastSelectedEntry());

        if (schematic != null)
        {
            MaterialListUtils.openMaterialListScreenFor(schematic);
        }
    }

    public static ISchematic tryLoadSchematic(DirectoryEntry entry)
    {
        File file = entry != null && entry.getType() == DirectoryEntryType.FILE ? entry.getFullPath() : null;

        if (file == null)
        {
            MessageDispatcher.error("litematica.message.error.schematic_load.no_schematic_selected");
            return null;
        }

        ISchematic schematic = SchematicType.tryCreateSchematicFrom(file);

        if (schematic == null)
        {
            MessageDispatcher.error("litematica.message.error.schematic_load.invalid_schematic_file");
        }

        return schematic;
    }
}
