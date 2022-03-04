package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Configs.Generic;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.util.SchematicBrowserIconProvider;
import fi.dy.masa.litematica.gui.widget.SchematicInfoWidget;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.SchematicType;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.config.value.FileBrowserColumns;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.StringUtils;

public class SchematicBrowserScreen extends BaseListScreen<BaseFileBrowserWidget>
{
    protected final SchematicBrowserIconProvider cachingIconProvider;
    protected final GenericButton loadButton;
    protected final GenericButton mainMenuScreenButton;
    protected final GenericButton materialListButton;
    protected final CheckBoxWidget createPlacementCheckbox;
    protected final SchematicInfoWidget schematicInfoWidget;

    public SchematicBrowserScreen()
    {
        super(10, 24, 20 + 170 + 2, 70);

        this.cachingIconProvider = new SchematicBrowserIconProvider();

        this.loadButton             = GenericButton.create("litematica.button.schematic_browser.load_schematic", this::loadSchematic);
        this.mainMenuScreenButton   = GenericButton.create("litematica.button.change_menu.main_menu");
        this.materialListButton     = GenericButton.create("litematica.button.schematic_browser.material_list", this::createMaterialList);
        this.materialListButton.translateAndAddHoverString("litematica.hover.button.schematic_browser.create_material_list");
        this.createPlacementCheckbox = new CheckBoxWidget("litematica.checkmark.schematic_browser.create_placement",
                                                          "litematica.hover.schematic_browser.create_placement");
        this.createPlacementCheckbox.setBooleanStorage(Configs.Internal.CREATE_PLACEMENT_ON_LOAD);
        this.mainMenuScreenButton.setActionListener(MainMenuScreen::openMainMenuScreen);
        this.schematicInfoWidget = new SchematicInfoWidget(170, 290);

        this.setTitle("litematica.title.screen.schematic_browser");
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.createPlacementCheckbox);
        this.addWidget(this.loadButton);
        this.addWidget(this.mainMenuScreenButton);
        this.addWidget(this.materialListButton);
        this.addWidget(this.schematicInfoWidget);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.schematicInfoWidget.setHeight(this.getListHeight());
        this.schematicInfoWidget.setRight(this.getRight() - 10);
        this.schematicInfoWidget.setY(this.y + 24);

        this.loadButton.setX(this.x + 10);
        this.loadButton.setBottom(this.getBottom() - 6);
        this.createPlacementCheckbox.setPosition(this.loadButton.getX(), this.loadButton.getY() - 14);
        this.materialListButton.setPosition(this.loadButton.getRight() + 4, this.loadButton.getY());

        this.mainMenuScreenButton.setRight(this.getRight() - 10);
        this.mainMenuScreenButton.setY(this.loadButton.getY());
    }

    @Override
    protected void onScreenClosed()
    {
        super.onScreenClosed();
        this.schematicInfoWidget.onScreenClosed();
    }

    @Override
    protected BaseFileBrowserWidget createListWidget()
    {
        File dir = DataManager.getSchematicsBaseDirectory();
        BaseFileBrowserWidget listWidget = new BaseFileBrowserWidget(dir, dir, DataManager.INSTANCE,
                                                                     "schematic_browser", this.cachingIconProvider);

        listWidget.setParentScreen(this.getParent());
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);
        setCommonSchematicBrowserSettings(listWidget);

        return listWidget;
    }

    public static void setCommonSchematicBrowserSettings(BaseFileBrowserWidget listWidget)
    {
        listWidget.setFileFilter(SchematicType.SCHEMATIC_FILE_FILTER);
        listWidget.setRootDirectoryDisplayName(StringUtils.translate("litematica.label.schematic_browser.schematics"));

        FileBrowserColumns mode = Generic.SCHEMATIC_BROWSER_COLUMNS.getValue();

        if (mode == FileBrowserColumns.MTIME || mode == FileBrowserColumns.SIZE_MTIME)
        {
            listWidget.setShowFileModificationTime(true);
        }

        if (mode == FileBrowserColumns.SIZE || mode == FileBrowserColumns.SIZE_MTIME)
        {
            listWidget.setShowFileSize(true);
        }
    }

    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        if (entry != null &&
            entry.getType() == DirectoryEntryType.FILE &&
            FileType.fromFileName(entry.getFullPath()).isSchematic())
        {
            this.schematicInfoWidget.onSelectionChange(entry);
        }
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
