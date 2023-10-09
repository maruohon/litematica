package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicSave extends GuiSchematicSaveBase implements ICompletionListener
{
    protected final SelectionManager selectionManager;

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
            this.mc.execute(GuiSchematicSave.this::refreshList);
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

                    if (schematic.writeToFile(dir, fileName, GuiBase.isShiftDown()))
                    {
                        schematic.getMetadata().clearModifiedSinceSaved();
                        this.gui.getListWidget().refreshEntries();
                        this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", fileName);
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
                            this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exists", fileNameTmp);
                            return;
                        }

                        String author = this.gui.mc.player.getName().getString();
                        boolean ignoreEntities = this.gui.checkboxIgnoreEntities.isChecked();
                        boolean visibleOnly = this.gui.checkboxVisibleOnly.isChecked();
                        boolean includeSupportBlocks = this.gui.checkboxIncludeSupportBlocks.isChecked();
                        boolean fromSchematicWorld = this.gui.checkboxSaveFromSchematicWorld.isChecked();
                        LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(visibleOnly, includeSupportBlocks, ignoreEntities, fromSchematicWorld);
                        LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, author);
                        TaskSaveSchematic task = new TaskSaveSchematic(dir, fileName, schematic, area, info, overwrite);
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
            LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false); // TODO
            String author = this.mc.player.getName().getString();
            LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(this.area, author);

            if (schematic != null)
            {
                schematic.getMetadata().setName(string);
                TaskSaveSchematic task = new TaskSaveSchematic(schematic, this.area, info);
                TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 10);
            }
        }
    }
}
