package de.meinbuild.liteschem.gui;

import java.io.File;
import javax.annotation.Nullable;

import de.meinbuild.liteschem.data.DataManager;
import de.meinbuild.liteschem.scheduler.TaskScheduler;
import de.meinbuild.liteschem.scheduler.tasks.TaskSaveSchematic;
import de.meinbuild.liteschem.schematic.LitematicaSchematic;
import de.meinbuild.liteschem.selection.AreaSelection;
import de.meinbuild.liteschem.selection.SelectionManager;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.MinecraftClient;

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
            this.defaultText = area.getName();
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
    protected IButtonActionListener createButtonListener(ButtonType type)
    {
        return new ButtonListener(type, this.selectionManager, this);
    }

    @Override
    public void onTaskCompleted()
    {
        if (this.mc.isOnThread())
        {
            this.refreshList();
        }
        else
        {
            this.mc.execute(new Runnable()
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

    private static class ButtonListener implements IButtonActionListener
    {
        private final GuiSchematicSave gui;
        private final SelectionManager selectionManager;
        private final ButtonType type;

        public ButtonListener(ButtonType type, SelectionManager selectionManager, GuiSchematicSave gui)
        {
            this.type = type;
            this.selectionManager = selectionManager;
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == ButtonType.SAVE)
            {
                File dir = this.gui.getListWidget().getCurrentDirectory();
                String fileName = this.gui.getTextFieldText();

                if (dir.isDirectory() == false)
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_directory", dir.getAbsolutePath());
                    return;
                }

                if (fileName.isEmpty())
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_schematic_name", fileName);
                    return;
                }

                // Saving a schematic from memory
                if (this.gui.schematic != null)
                {
                    LitematicaSchematic schematic = this.gui.schematic;
                    schematic.getMetadata().setTimeModified(System.currentTimeMillis());

                    if (schematic.writeToFile(dir, fileName, isShiftDown()))
                    {
                        this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", fileName);
                        this.gui.getListWidget().refreshEntries();
                    }
                }
                else
                {
                    AreaSelection area = this.selectionManager.getCurrentSelection();

                    if (area != null)
                    {
                        boolean overwrite = isShiftDown();
                        String fileNameTmp = fileName;

                        // The file name extension gets added in the schematic write method, so need to add it here for the check
                        if (fileNameTmp.endsWith(LitematicaSchematic.FILE_EXTENSION) == false)
                        {
                            fileNameTmp += LitematicaSchematic.FILE_EXTENSION;
                        }

                        if (FileUtils.canWriteToFile(dir, fileNameTmp, overwrite) == false)
                        {
                            this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exists", fileNameTmp);
                            return;
                        }

                        String author = this.gui.mc.player.getName().getString();
                        boolean takeEntities = this.gui.checkboxIgnoreEntities.isChecked() == false;
                        LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, author);
                        TaskSaveSchematic task = new TaskSaveSchematic(dir, fileName, schematic, area, takeEntities, overwrite);
                        task.setCompletionListener(this.gui);
                        TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);
                        this.gui.addMessage(MessageType.INFO, "litematica.message.schematic_save_task_created");
                    }
                    else
                    {
                        this.gui.addMessage(MessageType.ERROR, "litematica.message.error.schematic_save_no_area_selected");
                    }
                }
            }
        }
    }

    public static class InMemorySchematicCreator implements IStringConsumer
    {
        private final AreaSelection area;
        private final MinecraftClient mc;

        public InMemorySchematicCreator(AreaSelection area)
        {
            this.area = area;
            this.mc = MinecraftClient.getInstance();
        }

        @Override
        public void setString(String string)
        {
            boolean takeEntities = true; // TODO
            String author = this.mc.player.getName().getString();
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
