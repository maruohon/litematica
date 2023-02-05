package litematica.gui;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
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
    protected final GenericButton editDescriptionButton;
    protected final GenericButton removePreviewButton;
    protected final GenericButton renameFileButton;
    protected final GenericButton renameSchematicButton;
    protected final GenericButton setPreviewButton;

    public SchematicManagerScreen()
    {
        super(10, 24, 20 + 170 + 2, 70, "schematic_manager");

        this.convertSchematicButton = GenericButton.create("litematica.button.schematic_manager.convert_format", this::convertSchematic);
        this.deleteFileButton       = GenericButton.create("litematica.button.schematic_manager.delete", this::deleteFile);
        this.editDescriptionButton  = GenericButton.create("litematica.button.schematic_manager.edit_description", this::editDescription);
        this.removePreviewButton    = GenericButton.create("litematica.button.schematic_manager.remove_preview", this::removePreview);
        this.renameFileButton       = GenericButton.create("litematica.button.schematic_manager.rename_file", this::renameFile);
        this.renameSchematicButton  = GenericButton.create("litematica.button.schematic_manager.rename_schematic", this::renameSchematic);
        this.setPreviewButton       = GenericButton.create("litematica.button.schematic_manager.set_preview", this::setPreview);

        this.convertSchematicButton.translateAndAddHoverString("litematica.hover.button.schematic_manager.convert_format");
        this.editDescriptionButton.translateAndAddHoverString("litematica.hover.button.schematic_manager.set_description");
        this.renameSchematicButton.translateAndAddHoverString("litematica.hover.button.schematic_manager.rename_schematic");
        this.setPreviewButton.translateAndAddHoverString("litematica.hover.button.schematic_manager.set_preview");

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
            this.addWidget(this.editDescriptionButton);
            this.addWidget(this.renameFileButton);
            this.addWidget(this.renameSchematicButton);
            this.addWidget(this.setPreviewButton);

            ISchematic schematic = this.getLastSelectedSchematic();
    
            if (schematic != null && schematic.getMetadata().getPreviewImagePixelData() != null)
            {
                this.addWidget(this.removePreviewButton);
            }
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int y = this.getBottom() - 44;
        this.renameSchematicButton.setPosition(this.x + 10, y);
        this.setPreviewButton.setPosition(this.renameSchematicButton.getRight() + 2, y);
        this.editDescriptionButton.setPosition(this.setPreviewButton.getRight() + 2, y);

        y += 21;
        this.renameFileButton.setPosition(this.x + 10, y);
        this.convertSchematicButton.setPosition(this.renameFileButton.getRight() + 2, y);
        this.deleteFileButton.setPosition(this.convertSchematicButton.getRight() + 2, y);
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
        ISchematic schematic = this.getLastSelectedSchematic();

        if (schematic != null)
        {
            SaveConvertSchematicScreen screen = new SaveConvertSchematicScreen(schematic, false);
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

    protected void editDescription()
    {
        ISchematic schematic = this.getLastSelectedSchematic();

        if (schematic != null)
        {
            SchematicMetadata meta = schematic.getMetadata();
            String title = "litematica.title.screen.schematic_manager.edit_description";
            // TODO use a TextAreaWidget once one has been implemented in malilib
            TextInputScreen screen = new TextInputScreen(title, meta.getDescription(), str -> this.setSchematicDescription(str, schematic));
            screen.setParent(this);
            openPopupScreen(screen);
        }
    }

    protected void removePreview()
    {
        ISchematic schematic = this.getLastSelectedSchematic();
        Path file = this.lastSelectedSchematicFile;

        if (schematic != null && file != null && file.getFileName() != null)
        {
            String title = "litematica.title.screen.schematic_manager.confirm_preview_removal";
            String msg = "litematica.info.schematic_manager.confirm_preview_removal";
            String name = file.getFileName().toString();
            ConfirmActionScreen screen = new ConfirmActionScreen(320, title, () -> this.removeSchematicPreviewImage(schematic), msg, name);
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

    protected void setPreview()
    {
        ISchematic schematic = this.getLastSelectedSchematic();

        if (schematic != null)
        {
            if (schematic.getType() == SchematicType.LITEMATICA)
            {
                SetSchematicPreviewTask task = new SetSchematicPreviewTask(schematic);
                TaskScheduler.getInstanceClient().scheduleTask(task, 1);

                String hotkeyName = Hotkeys.SET_SCHEMATIC_PREVIEW.getDisplayName();
                String hotkeyValue = Hotkeys.SET_SCHEMATIC_PREVIEW.getKeyBind().getKeysDisplayString();
                MessageDispatcher.generic(6000).translate("litematica.message.info.schematic_manager.set_preview",
                                                          hotkeyName, hotkeyValue);
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.schematic_manager.schematic_type_has_no_preview",
                                        schematic.getType().getDisplayName());
            }
        }
    }

    protected void renameSchematic()
    {
        ISchematic schematic = this.getLastSelectedSchematic();

        if (schematic != null)
        {
            if (schematic.getType().getHasName() == false)
            {
                MessageDispatcher.error("litematica.message.error.schematic_manager.schematic_type_has_no_name");
                return;
            }

            String oldName = schematic.getMetadata().getName();
            String title = "litematica.title.screen.schematic_manager.rename_schematic";
            TextInputScreen screen = new TextInputScreen(title, oldName, str -> this.renameSchematicToName(str, schematic));
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

    protected boolean renameSchematicToName(String newName, ISchematic schematic)
    {
        SchematicMetadata meta = schematic.getMetadata();
        String oldName = meta.getName();
        Path file = schematic.getFile();

        meta.setName(newName);
        meta.setTimeModifiedToNowIfNotRecentlyCreated();

        if (schematic.writeToFile(file, true))
        {
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            List<ISchematic> list = SchematicHolder.getInstance().getAllOf(file);

            for (ISchematic schematicTmp : list)
            {
                schematicTmp.getMetadata().setName(newName);
                schematicTmp.getMetadata().setTimeModifiedToNowIfNotRecentlyCreated();

                // Rename all placements that used the old schematic name (ie. were not manually renamed)
                for (SchematicPlacement placement : manager.getAllPlacementsOfSchematic(schematicTmp))
                {
                    if (placement.getName().equals(oldName))
                    {
                        placement.setName(newName);
                    }
                }
            }

            MessageDispatcher.success("litematica.message.info.schematic_manager.schematic_renamed");
            this.onSchematicChange();

            return true;
        }
        else
        {
            MessageDispatcher.error("litematica.message.error.schematic_manager.failed_to_save_schematic");
        }

        return false;
    }

    protected boolean setSchematicDescription(String description, ISchematic schematic)
    {
        SchematicMetadata meta = schematic.getMetadata();

        if (Objects.equals(description, meta.getDescription()) == false)
        {
            meta.setDescription(description);
            meta.setTimeModifiedToNowIfNotRecentlyCreated();

            if (schematic.writeToFile(schematic.getFile(), true))
            {
                MessageDispatcher.success("litematica.message.info.schematic_manager.description_set");
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.schematic_manager.failed_to_save_schematic");
            }

            this.onSchematicChange();
        }

        return true;
    }

    protected void removeSchematicPreviewImage(ISchematic schematic)
    {
        SchematicMetadata meta = schematic.getMetadata();

        if (meta.getPreviewImagePixelData() != null)
        {
            meta.setPreviewImagePixelData(null);
            meta.setTimeModifiedToNowIfNotRecentlyCreated();

            if (schematic.writeToFile(schematic.getFile(), true))
            {
                MessageDispatcher.success("litematica.message.info.schematic_manager.preview_removed");
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.schematic_manager.failed_to_save_schematic");
            }

            this.onSchematicChange();
        }
    }
}
