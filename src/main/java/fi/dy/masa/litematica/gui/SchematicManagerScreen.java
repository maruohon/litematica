package fi.dy.masa.litematica.gui;

import java.io.File;
import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.util.SchematicBrowserIconProvider;
import fi.dy.masa.litematica.gui.util.SchematicInfoCache.SchematicInfo;
import fi.dy.masa.litematica.gui.widget.SchematicInfoWidget;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.SetSchematicPreviewTask;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.SchematicType;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.ConfirmActionScreen;
import fi.dy.masa.malilib.gui.TextInputScreen;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.FileUtils;

public class SchematicManagerScreen extends BaseListScreen<BaseFileBrowserWidget>
{
    protected final SchematicBrowserIconProvider cachingIconProvider;
    protected final GenericButton convertSchematicButton;
    protected final GenericButton deleteFileButton;
    protected final GenericButton mainMenuScreenButton;
    protected final GenericButton removePreviewButton;
    protected final GenericButton renameFileButton;
    protected final GenericButton renameSchematicButton;
    protected final GenericButton setPreviewButton;
    protected final SchematicInfoWidget schematicInfoWidget;

    public SchematicManagerScreen()
    {
        super(10, 24, 20 + 170 + 2, 70);

        this.cachingIconProvider = new SchematicBrowserIconProvider();

        this.convertSchematicButton = GenericButton.create("litematica.button.schematic_manager.convert_format", this::convertSchematic);
        this.deleteFileButton       = GenericButton.create("litematica.button.schematic_manager.delete", this::deleteFile);
        this.mainMenuScreenButton   = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.removePreviewButton    = GenericButton.create("litematica.button.schematic_manager.remove_preview", this::removePreview);
        this.renameFileButton       = GenericButton.create("litematica.button.schematic_manager.rename_file", this::renameFile);
        this.renameSchematicButton  = GenericButton.create("litematica.button.schematic_manager.rename_schematic", this::renameSchematic);
        this.setPreviewButton       = GenericButton.create("litematica.button.schematic_manager.set_preview", this::setPreview);
        this.schematicInfoWidget = new SchematicInfoWidget(170, 290);

        this.convertSchematicButton.translateAndAddHoverString("litematica.hover.button.schematic_manager.convert_format");

        this.setTitle("litematica.title.screen.schematic_manager", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.mainMenuScreenButton);
        this.addWidget(this.schematicInfoWidget);

        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry != null)
        {
            this.addWidget(this.convertSchematicButton);
            this.addWidget(this.deleteFileButton);
            this.addWidget(this.renameFileButton);
            this.addWidget(this.renameSchematicButton);
            this.addWidget(this.setPreviewButton);
        }

        SchematicInfo info = this.schematicInfoWidget.getSelectedSchematicInfo();

        if (info != null && info.schematic.getMetadata().getPreviewImagePixelData() != null)
        {
            this.addWidget(this.removePreviewButton);
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.schematicInfoWidget.setHeight(this.getListHeight());
        this.schematicInfoWidget.setRight(this.getRight() - 10);
        this.schematicInfoWidget.setY(this.y + 24);

        int y = this.getBottom() - 44;
        this.renameSchematicButton.setPosition(this.x + 10, y);
        this.convertSchematicButton.setPosition(this.renameSchematicButton.getRight() + 2, y);
        this.setPreviewButton.setPosition(this.convertSchematicButton.getRight() + 2, y);

        y += 21;
        this.renameFileButton.setPosition(this.x + 10, y);
        this.deleteFileButton.setPosition(this.renameFileButton.getRight() + 2, y);
        this.removePreviewButton.setPosition(this.deleteFileButton.getRight() + 2, y);

        this.mainMenuScreenButton.setRight(this.getRight() - 10);
        this.mainMenuScreenButton.setY(y);
    }

    @Override
    protected void onScreenClosed()
    {
        super.onScreenClosed();
        this.schematicInfoWidget.clearCache();
    }

    @Override
    protected BaseFileBrowserWidget createListWidget()
    {
        File dir = DataManager.getSchematicsBaseDirectory();
        BaseFileBrowserWidget listWidget = new BaseFileBrowserWidget(dir, dir, DataManager.INSTANCE,
                                                                     "schematic_manager", this.cachingIconProvider);

        listWidget.setParentScreen(this.getParent());
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);
        SchematicBrowserScreen.setCommonSchematicBrowserSettings(listWidget);
        listWidget.setAllowFileOperations(true);

        return listWidget;
    }

    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        this.schematicInfoWidget.onSelectionChange(entry);
        this.reAddActiveWidgets();
    }

    protected void convertSchematic()
    {
        SchematicInfo info = this.schematicInfoWidget.getSelectedSchematicInfo();

        if (info != null)
        {
            // TODO FIXME malilib refactor
            /*
            SchematicConvertScreen screen = new SchematicConvertScreen(info.schematic, entry.getName());
            screen.setParent(this);
            BaseScreen.openScreen(screen);
            */
        }
    }

    protected void deleteFile()
    {
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry != null)
        {
            String title = "litematica.title.screen.schematic_manager.confirm_file_deletion";
            String msg = "litematica.info.schematic_manager.confirm_file_deletion";
            String fileName = entry.getFullPath().getAbsolutePath();
            ConfirmActionScreen screen = new ConfirmActionScreen(320, title, this::executeFileDelete, msg, fileName);
            screen.setParent(this);
            BaseScreen.openPopupScreen(screen);
        }
    }

    protected void removePreview()
    {
        SchematicInfo info = this.schematicInfoWidget.getSelectedSchematicInfo();

        if (info != null)
        {
            String title = "litematica.title.screen.schematic_manager.confirm_preview_removal";
            String msg = "litematica.info.schematic_manager.confirm_preview_removal";
            String name = info.schematic.getMetadata().getName();
            ConfirmActionScreen screen = new ConfirmActionScreen(320, title, this::executeRemovePreview, msg, name);
            screen.setParent(this);
            BaseScreen.openPopupScreen(screen);
        }
    }

    protected boolean executeFileDelete()
    {
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry != null)
        {
            File file = entry.getFullPath();

            try
            {
                boolean success = file.delete();

                this.schematicInfoWidget.clearCache();
                this.getListWidget().clearSelection();
                this.getListWidget().refreshEntries();

                return success;
            }
            catch (Exception e)
            {
                MessageDispatcher.error("malilib.message.error.failed_to_delete_file", file.getAbsolutePath());
            }
        }

        return false;
    }

    protected void renameFile()
    {
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry != null)
        {
            File oldFile = entry.getFullPath();
            String oldName = FileNameUtils.getFileNameWithoutExtension(oldFile.getName());
            String title = "litematica.title.screen.schematic_manager.rename_file";
            TextInputScreen screen = new TextInputScreen(title, oldName,
                                                         (str) -> this.renameFileToName(oldFile, str), this);
            BaseScreen.openPopupScreen(screen);
        }
    }

    protected boolean renameFileToName(File oldFile, String newName)
    {
        boolean success = FileUtils.renameFileToName(oldFile, newName, MessageDispatcher::error);

        this.schematicInfoWidget.clearCache();
        this.getListWidget().clearSelection();
        this.getListWidget().refreshEntries();

        return success;
    }

    protected void renameSchematic()
    {
        SchematicInfo info = this.schematicInfoWidget.getSelectedSchematicInfo();

        if (info != null)
        {
            if (info.schematic.getType().getHasName() == false)
            {
                MessageDispatcher.error("litematica.message.error.schematic_manager.schematic_type_has_no_name");
                return;
            }

            String oldName = info.schematic.getMetadata().getName();
            String title = "litematica.title.screen.schematic_manager.rename_schematic";
            TextInputScreen screen = new TextInputScreen(title, oldName, this::renameSchematicToName, this);
            BaseScreen.openPopupScreen(screen);
        }
    }

    protected boolean renameSchematicToName(String newName)
    {
        SchematicInfo info = this.schematicInfoWidget.getSelectedSchematicInfo();

        if (info != null)
        {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            ISchematic schematic = info.schematic;
            String oldName = schematic.getMetadata().getName();
            schematic.getMetadata().setName(newName);
            schematic.getMetadata().setTimeModifiedToNow();
            File file = schematic.getFile();

            if (schematic.writeToFile(file, true))
            {
                List<ISchematic> list = SchematicHolder.getInstance().getAllOf(file);

                for (ISchematic schematicTmp : list)
                {
                    schematicTmp.getMetadata().setName(newName);
                    schematicTmp.getMetadata().setTimeModifiedToNow();

                    // Rename all placements that used the old schematic name (ie. were not manually renamed)
                    for (SchematicPlacement placement : manager.getAllPlacementsOfSchematic(schematicTmp))
                    {
                        if (placement.getName().equals(oldName))
                        {
                            placement.setName(newName);
                        }
                    }
                }

                this.schematicInfoWidget.clearCache();
                this.getListWidget().refreshEntries();

                return true;
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.schematic_manager.failed_to_save_schematic");
            }
        }

        return false;
    }

    protected void setPreview()
    {
        SchematicInfo info = this.schematicInfoWidget.getSelectedSchematicInfo();

        if (info != null)
        {
            ISchematic schematic = info.schematic;

            if (schematic.getType() == SchematicType.LITEMATICA)
            {
                SetSchematicPreviewTask task = new SetSchematicPreviewTask(schematic);
                TaskScheduler.getInstanceClient().scheduleTask(task, 1);

                String hotkeyName = Hotkeys.SET_SCHEMATIC_PREVIEW.getDisplayName();
                String hotkeyValue = Hotkeys.SET_SCHEMATIC_PREVIEW.getKeyBind().getKeysDisplayString();
                MessageDispatcher.generic("litematica.message.info.schematic_manager.set_preview",
                                          hotkeyName, hotkeyValue);
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.schematic_manager.schematic_type_has_no_preview",
                                        schematic.getType().getDisplayName());
            }
        }
    }

    protected void executeRemovePreview()
    {
        SchematicInfo info = this.schematicInfoWidget.getSelectedSchematicInfo();

        if (info != null)
        {
            ISchematic schematic = info.schematic;
            SchematicMetadata meta = schematic.getMetadata();

            if (meta.getPreviewImagePixelData() != null)
            {
                meta.setPreviewImagePixelData(null);
                meta.setTimeModifiedToNow();

                if (schematic.writeToFile(schematic.getFile(), true))
                {
                    MessageDispatcher.success("litematica.message.info.schematic_manager.preview_removed");
                }
                else
                {
                    MessageDispatcher.error("litematica.message.error.schematic_manager.failed_to_save_schematic");
                }

                this.schematicInfoWidget.clearCache();
            }
        }
    }
}
