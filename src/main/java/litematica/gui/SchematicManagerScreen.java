package litematica.gui;

import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;

import malilib.gui.ConfirmActionScreen;
import malilib.gui.TextInputScreen;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.FileNameUtils;
import malilib.util.FileUtils;
import litematica.Reference;
import litematica.config.Hotkeys;
import litematica.data.DataManager;
import litematica.data.SchematicHolder;
import litematica.gui.util.SchematicInfoCache.SchematicInfo;
import litematica.scheduler.TaskScheduler;
import litematica.scheduler.tasks.SetSchematicPreviewTask;
import litematica.schematic.ISchematic;
import litematica.schematic.SchematicMetadata;
import litematica.schematic.SchematicType;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;

public class SchematicManagerScreen extends BaseSchematicBrowserScreen
{
    protected final GenericButton convertSchematicButton;
    protected final GenericButton deleteFileButton;
    protected final GenericButton removePreviewButton;
    protected final GenericButton renameFileButton;
    protected final GenericButton renameSchematicButton;
    protected final GenericButton setPreviewButton;

    public SchematicManagerScreen()
    {
        super(10, 24, 20 + 170 + 2, 70, "schematic_manager");

        this.convertSchematicButton = GenericButton.create("litematica.button.schematic_manager.convert_format", this::convertSchematic);
        this.deleteFileButton       = GenericButton.create("litematica.button.schematic_manager.delete", this::deleteFile);
        this.removePreviewButton    = GenericButton.create("litematica.button.schematic_manager.remove_preview", this::removePreview);
        this.renameFileButton       = GenericButton.create("litematica.button.schematic_manager.rename_file", this::renameFile);
        this.renameSchematicButton  = GenericButton.create("litematica.button.schematic_manager.rename_schematic", this::renameSchematic);
        this.setPreviewButton       = GenericButton.create("litematica.button.schematic_manager.set_preview", this::setPreview);

        this.convertSchematicButton.translateAndAddHoverString("litematica.hover.button.schematic_manager.convert_format");

        this.setTitle("litematica.title.screen.schematic_manager", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

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

        int y = this.getBottom() - 44;
        this.renameSchematicButton.setPosition(this.x + 10, y);
        this.convertSchematicButton.setPosition(this.renameSchematicButton.getRight() + 2, y);
        this.setPreviewButton.setPosition(this.convertSchematicButton.getRight() + 2, y);

        y += 21;
        this.renameFileButton.setPosition(this.x + 10, y);
        this.deleteFileButton.setPosition(this.renameFileButton.getRight() + 2, y);
        this.removePreviewButton.setPosition(this.deleteFileButton.getRight() + 2, y);
    }

    @Override
    protected BaseFileBrowserWidget createListWidget()
    {
        BaseFileBrowserWidget listWidget = super.createListWidget();
        listWidget.setAllowFileOperations(true);
        return listWidget;
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        super.onSelectionChange(entry);
        this.reAddActiveWidgets();
    }

    protected void convertSchematic()
    {
        SchematicInfo info = this.schematicInfoWidget.getSelectedSchematicInfo();

        if (info != null)
        {
            SaveConvertSchematicScreen screen = new SaveConvertSchematicScreen(info.schematic, false);
            screen.setParent(this);
            openScreen(screen);
        }
    }

    protected void deleteFile()
    {
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry != null)
        {
            String title = "litematica.title.screen.schematic_manager.confirm_file_deletion";
            String msg = "litematica.info.schematic_manager.confirm_file_deletion";
            String fileName = entry.getFullPath().toAbsolutePath().toString();
            ConfirmActionScreen screen = new ConfirmActionScreen(320, title, this::executeFileDelete, msg, fileName);
            screen.setParent(this);
            openPopupScreen(screen);
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
            openPopupScreen(screen);
        }
    }

    protected boolean executeFileDelete()
    {
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry != null)
        {
            Path file = entry.getFullPath();

            try
            {
                boolean success = FileUtils.delete(file);
                this.onSchematicChange();
                return success;
            }
            catch (Exception e)
            {
                MessageDispatcher.error("malilib.message.error.failed_to_delete_file", file.toAbsolutePath().toString());
            }
        }

        return false;
    }

    protected void renameFile()
    {
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry != null)
        {
            Path oldFile = entry.getFullPath();
            String oldName = FileNameUtils.getFileNameWithoutExtension(oldFile.getFileName().toString());
            String title = "litematica.title.screen.schematic_manager.rename_file";
            TextInputScreen screen = new TextInputScreen(title, oldName,
                                                         (str) -> this.renameFileToName(oldFile, str));
            screen.setParent(this);
            openPopupScreen(screen);
        }
    }

    protected boolean renameFileToName(Path oldFile, String newName)
    {
        boolean success = FileUtils.renameFileToName(oldFile, newName, MessageDispatcher::error);
        this.onSchematicChange();
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
            TextInputScreen screen = new TextInputScreen(title, oldName, this::renameSchematicToName);
            screen.setParent(this);
            openPopupScreen(screen);
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
            Path file = schematic.getFile();

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

                this.onSchematicChange();

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

                this.clearSchematicInfoCache();
            }
        }
    }
}