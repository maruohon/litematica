package litematica.gui;

import java.nio.file.Path;
import javax.annotation.Nullable;

import malilib.config.value.FileBrowserColumns;
import malilib.gui.BaseListScreen;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.util.StringUtils;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.gui.util.SchematicBrowserIconProvider;
import litematica.gui.widget.SchematicInfoWidgetByPath;
import litematica.schematic.ISchematic;
import litematica.schematic.SchematicType;
import litematica.util.LitematicaDirectories;

public class BaseSchematicBrowserScreen extends BaseListScreen<BaseFileBrowserWidget>
{
    protected final SchematicBrowserIconProvider cachingIconProvider;
    protected final GenericButton mainMenuScreenButton;
    protected final SchematicInfoWidgetByPath schematicInfoWidget;
    protected final String browserContext;
    @Nullable protected Path lastSelectedSchematicFile;

    public BaseSchematicBrowserScreen(int listX, int listY,
                                      int totalListMarginX, int totalListMarginY,
                                      String browserContext)
    {
        super(listX, listY, totalListMarginX, totalListMarginY);

        this.browserContext = browserContext;
        this.mainMenuScreenButton = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.cachingIconProvider = new SchematicBrowserIconProvider();
        this.schematicInfoWidget = new SchematicInfoWidgetByPath(170, 290);

        Runnable clearTask = this::clearSchematicInfoCache;
        this.addPreInitListener(clearTask);
        this.addPostInitListener(() -> this.getListWidget().clearSelection());
        this.addPreScreenCloseListener(clearTask);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();
        this.addWidget(this.schematicInfoWidget);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.schematicInfoWidget.setHeight(this.getListHeight());
        this.schematicInfoWidget.setRight(this.getRight() - 10);
        this.schematicInfoWidget.setY(this.getListY());

        this.mainMenuScreenButton.setRight(this.getRight() - 10);
        this.mainMenuScreenButton.setBottom(this.getBottom() - 6);
    }

    @Override
    protected BaseFileBrowserWidget createListWidget()
    {
        Path dir = LitematicaDirectories.getSchematicsBaseDirectory();
        BaseFileBrowserWidget listWidget = new BaseFileBrowserWidget(dir, dir, DataManager.INSTANCE,
                                                                     this.browserContext, this.cachingIconProvider);

        listWidget.setParentScreen(this.getParent());
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);
        setCommonSchematicBrowserSettings(listWidget);

        return listWidget;
    }

    public static void setCommonSchematicBrowserSettings(BaseFileBrowserWidget listWidget)
    {
        listWidget.setFileFilter(SchematicType.SCHEMATIC_FILE_FILTER);
        listWidget.setRootDirectoryDisplayName(StringUtils.translate("litematica.label.schematic_browser.schematics"));

        FileBrowserColumns mode = Configs.Generic.SCHEMATIC_BROWSER_COLUMNS.getValue();

        if (mode == FileBrowserColumns.MTIME || mode == FileBrowserColumns.SIZE_MTIME)
        {
            listWidget.setShowFileModificationTime(true);
        }

        if (mode == FileBrowserColumns.SIZE || mode == FileBrowserColumns.SIZE_MTIME)
        {
            listWidget.setShowFileSize(true);
        }
    }

    protected void clearSchematicInfoCache()
    {
        this.schematicInfoWidget.clearCache();
    }

    protected void onSchematicChange()
    {
        this.clearSchematicInfoCache();
        this.getListWidget().clearSelection();
        this.getListWidget().refreshEntries();
    }

    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        Path fullPath = entry != null ? entry.getFullPath() : null;
        this.lastSelectedSchematicFile = fullPath;
        this.schematicInfoWidget.onSelectionChange(fullPath);
    }

    @Nullable
    protected ISchematic getLastSelectedSchematic()
    {
        if (this.lastSelectedSchematicFile == null)
        {
            return null;
        }

        return SchematicType.tryCreateSchematicFrom(this.lastSelectedSchematicFile);
    }
}
