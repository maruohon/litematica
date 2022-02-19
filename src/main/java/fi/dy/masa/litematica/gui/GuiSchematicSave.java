package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.util.SchematicCreationUtils;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.data.ResultingStringConsumer;
import fi.dy.masa.malilib.listener.TaskCompletionListener;
import fi.dy.masa.malilib.overlay.message.MessageOutput;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicSave extends GuiSchematicSaveBase implements TaskCompletionListener
{
    private final SelectionManager selectionManager;
    protected CheckBoxWidget checkboxIgnoreEntities;

    public static String generateFilename(String nameIn)
    {
        String name = FileNameUtils.generateSafeFileName(nameIn);

        if (Configs.Generic.GENERATE_LOWERCASE_NAMES.getBooleanValue())
        {
            name = FileNameUtils.generateSimpleSafeFileName(name);
        }

        return name;
    }

    public GuiSchematicSave()
    {
        this(null);
    }

    public GuiSchematicSave(@Nullable ISchematic schematic)
    {
        super(schematic);

        if (schematic != null)
        {
            this.title = StringUtils.translate("litematica.gui.title.save_schematic_from_memory");
        }
        else
        {
            this.title = StringUtils.translate("litematica.gui.title.create_schematic_from_selection");
        }

        this.selectionManager = DataManager.getSelectionManager();

        AreaSelection area = this.selectionManager.getCurrentSelection();

        if (area != null)
        {
            this.defaultText = generateFilename(area.getName());
        }
    }

    @Override
    protected void createCustomElements()
    {
        int x = this.textField.getX() + this.textField.getWidth() + 12;
        int y = 32;

        String str = StringUtils.translate("litematica.gui.label.schematic_save.checkbox.ignore_entities");
        this.checkboxIgnoreEntities = new CheckBoxWidget(x, y + 24, LitematicaIcons.CHECKBOX_UNSELECTED, LitematicaIcons.CHECKBOX_SELECTED, str);
        this.addWidget(this.checkboxIgnoreEntities);

        this.createButton(x, y, ButtonType.SAVE);
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_save";
    }

    @Override
    public File getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    public void onTaskCompleted()
    {
        if (this.mc.isCallingFromMinecraftThread())
        {
            this.refreshList();
        }
        else
        {
            this.mc.addScheduledTask(GuiSchematicSave.this::refreshList);
        }
    }

    @Override
    protected void saveSchematic()
    {
        File dir = this.getListWidget().getCurrentDirectory();
        String fileName = this.getTextFieldText();

        if (FileNameUtils.doesFileNameContainIllegalCharacters(fileName))
        {
            this.addMessage(MessageOutput.ERROR, "malilib.error.illegal_characters_in_file_name", fileName);
            return;
        }

        if (dir.isDirectory() == false)
        {
            this.addMessage(MessageOutput.ERROR, "litematica.error.schematic_save.invalid_directory", dir.getAbsolutePath());
            return;
        }

        if (fileName.isEmpty())
        {
            this.addMessage(MessageOutput.ERROR, "litematica.error.schematic_save.invalid_schematic_name", fileName);
            return;
        }

        // Saving a schematic from memory
        if (this.schematic != null)
        {
            ISchematic schematic = this.schematic;

            if (schematic.writeToFile(dir, fileName, BaseScreen.isShiftDown()))
            {
                schematic.getMetadata().clearModifiedSinceSaved();

                this.getListWidget().refreshEntries();
                this.addMessage(MessageOutput.SUCCESS, "litematica.message.schematic_saved_as", fileName);
            }
        }
        else
        {
            AreaSelection area = this.selectionManager.getCurrentSelection();

            if (area != null)
            {
                boolean overwrite = BaseScreen.isShiftDown();
                String fileNameTmp = fileName;

                // The file name extension gets added in the schematic write method, so need to add it here for the check
                if (fileNameTmp.endsWith(LitematicaSchematic.FILE_NAME_EXTENSION) == false)
                {
                    fileNameTmp += LitematicaSchematic.FILE_NAME_EXTENSION;
                }

                if (FileUtils.canWriteToFile(dir, fileNameTmp, overwrite) == false)
                {
                    this.addMessage(MessageOutput.ERROR, "litematica.error.schematic_write_to_file_failed.exists", fileNameTmp);
                    return;
                }

                String author = this.mc.player.getName();
                boolean takeEntities = this.checkboxIgnoreEntities.isSelected() == false;
                LitematicaSchematic schematic = SchematicCreationUtils.createEmptySchematic(area, author);
                TaskSaveSchematic task = new TaskSaveSchematic(dir, fileName, schematic, area, takeEntities, overwrite);
                task.setCompletionListener(this);
                TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);
                this.addMessage(MessageOutput.INFO, "litematica.message.schematic_save_task_created");
            }
            else
            {
                this.addMessage(MessageOutput.ERROR, "litematica.message.error.schematic_save_no_area_selected");
            }
        }
    }

    public static class InMemorySchematicCreator implements ResultingStringConsumer
    {
        private final AreaSelection area;
        private final Minecraft mc;

        public InMemorySchematicCreator(AreaSelection area)
        {
            this.area = area;
            this.mc = Minecraft.getMinecraft();
        }

        @Override
        public boolean consumeString(String string)
        {
            boolean takeEntities = true; // TODO
            String author = this.mc.player.getName();
            LitematicaSchematic schematic = SchematicCreationUtils.createEmptySchematic(this.area, author);

            if (schematic != null)
            {
                schematic.getMetadata().setName(string);
                TaskSaveSchematic task = new TaskSaveSchematic(schematic, this.area, takeEntities);
                TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);

                return true;
            }

            return false;
        }
    }
}
