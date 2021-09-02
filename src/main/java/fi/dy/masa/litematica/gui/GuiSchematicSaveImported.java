package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicSaveImported extends GuiSchematicSaveBase
{
    private final DirectoryEntryType type;
    private final File dirSource;
    private final String inputFileName;

    public GuiSchematicSaveImported(DirectoryEntryType type, File dirSource, String inputFileName)
    {
        super(null);

        this.type = type;
        this.dirSource = dirSource;
        this.inputFileName = inputFileName;
        this.defaultText = FileUtils.getNameWithoutExtension(inputFileName);
        this.title = StringUtils.translate("litematica.gui.title.save_imported_schematic");
        this.useTitleHierarchy = false;
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_save_imported";
    }

    @Override
    public File getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected IButtonActionListener createButtonListener(ButtonType type)
    {
        return new ButtonListener(type, this);
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final GuiSchematicSaveImported gui;
        private final ButtonType type;

        public ButtonListener(ButtonType type, GuiSchematicSaveImported gui)
        {
            this.type = type;
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

                if (this.gui.type == DirectoryEntryType.FILE)
                {
                    File inDir = this.gui.dirSource;
                    String inFile = this.gui.inputFileName;
                    boolean override = GuiBase.isShiftDown();
                    boolean ignoreEntities = this.gui.checkboxIgnoreEntities.isChecked();
                    FileType fileType = FileType.fromFile(new File(inDir, inFile));

                    if (fileType == FileType.SCHEMATICA_SCHEMATIC)
                    {
                        if (WorldUtils.convertSchematicaSchematicToLitematicaSchematic(inDir, inFile, dir, fileName, ignoreEntities, override, this.gui))
                        {
                            this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", fileName);
                            this.gui.getListWidget().refreshEntries();
                        }

                        return;
                    }
                    else if (fileType == FileType.VANILLA_STRUCTURE)
                    {
                        if (WorldUtils.convertStructureToLitematicaSchematic(inDir, inFile, dir, fileName, override))
                        {
                            this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", fileName);
                            this.gui.getListWidget().refreshEntries();
                        }

                        return;
                    }
                }

                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.unsupported_type", this.gui.inputFileName);
            }
        }
    }
}
