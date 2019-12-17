package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicSave extends GuiSchematicSaveBase implements ICompletionListener
{
    private final SelectionManager selectionManager;

    public GuiSchematicSave()
    {
        this(null);
    }

    public GuiSchematicSave(@Nullable LitematicaSchematic schematic)
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
            this.defaultText = FileUtils.generateSafeFileName(area.getName());

            if (Configs.Generic.GENERATE_LOWERCASE_NAMES.getBooleanValue())
            {
                this.defaultText = FileUtils.generateSimpleSafeFileName(this.defaultText);
            }
        }
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
            this.mc.addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    GuiSchematicSave.this.refreshList();
                }
            });
        }
    }

    private void refreshList()
    {
        if (GuiUtils.getCurrentScreen() == this)
        {
            this.getListWidget().refreshEntries();
            this.getListWidget().clearSchematicMetadataCache();
        }
    }

    @Override
    protected void saveSchematic()
    {
        File dir = this.getListWidget().getCurrentDirectory();
        String fileName = this.getTextFieldText();

        if (FileUtils.doesFilenameContainIllegalCharacters(fileName))
        {
            this.addMessage(MessageType.ERROR, "litematica.error.illegal_characters_in_file_name", fileName);
            return;
        }

        if (dir.isDirectory() == false)
        {
            this.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_directory", dir.getAbsolutePath());
            return;
        }

        if (fileName.isEmpty())
        {
            this.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_schematic_name", fileName);
            return;
        }

        // Saving a schematic from memory
        if (this.schematic != null)
        {
            LitematicaSchematic schematic = this.schematic;

            if (schematic.writeToFile(dir, fileName, GuiBase.isShiftDown()))
            {
                schematic.getMetadata().clearModifiedSinceSaved();

                this.getListWidget().refreshEntries();
                this.addMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", fileName);
            }
        }
        else
        {
            AreaSelection area = this.selectionManager.getCurrentSelection();

            if (area != null)
            {
                boolean overwrite = GuiBase.isShiftDown();
                String fileNameTmp = fileName;

                // The file name extension gets added in the schematic write method, so need to add it here for the check
                if (fileNameTmp.endsWith(LitematicaSchematic.FILE_EXTENSION) == false)
                {
                    fileNameTmp += LitematicaSchematic.FILE_EXTENSION;
                }

                if (FileUtils.canWriteToFile(dir, fileNameTmp, overwrite) == false)
                {
                    this.addMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exists", fileNameTmp);
                    return;
                }

                String author = this.mc.player.getName();
                boolean takeEntities = this.checkboxIgnoreEntities.isChecked() == false;
                LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, author);
                TaskSaveSchematic task = new TaskSaveSchematic(dir, fileName, schematic, area, takeEntities, overwrite);
                task.setCompletionListener(this);
                TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);
                this.addMessage(MessageType.INFO, "litematica.message.schematic_save_task_created");
            }
            else
            {
                this.addMessage(MessageType.ERROR, "litematica.message.error.schematic_save_no_area_selected");
            }
        }
    }

    public static class InMemorySchematicCreator implements IStringConsumer
    {
        private final AreaSelection area;
        private final Minecraft mc;

        public InMemorySchematicCreator(AreaSelection area)
        {
            this.area = area;
            this.mc = Minecraft.getMinecraft();
        }

        @Override
        public void setString(String string)
        {
            boolean takeEntities = true; // TODO
            String author = this.mc.player.getName();
            LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(this.area, author);

            if (schematic != null)
            {
                schematic.getMetadata().setName(string);
                TaskSaveSchematic task = new TaskSaveSchematic(schematic, this.area, takeEntities);
                TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);
            }
        }
    }
}
