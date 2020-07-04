package de.meinbuild.liteschem.gui;

import java.io.File;

import de.meinbuild.liteschem.util.FileType;
import de.meinbuild.liteschem.util.WorldUtils;
import de.meinbuild.liteschem.data.DataManager;
import de.meinbuild.liteschem.gui.GuiSchematicManager.ExportType;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicSaveExported extends GuiSchematicSaveBase
{
    private final ExportType exportType;
    private final DirectoryEntryType type;
    private final File dirSource;
    private final String inputFileName;

    public GuiSchematicSaveExported(DirectoryEntryType type, File dirSource, String inputFileName, ExportType exportType)
    {
        super(null);

        this.exportType = exportType;
        this.type = type;
        this.dirSource = dirSource;
        this.inputFileName = inputFileName;
        this.defaultText = FileUtils.getNameWithoutExtension(inputFileName);
        this.title = StringUtils.translate("litematica.gui.title.save_exported_schematic", exportType.getDisplayName(), inputFileName);
        this.useTitleHierarchy = false;
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_save_exported";
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
        private final GuiSchematicSaveExported gui;
        private final ButtonType type;

        public ButtonListener(ButtonType type, GuiSchematicSaveExported gui)
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
                    boolean override = isShiftDown();
                    boolean ignoreEntities = this.gui.checkboxIgnoreEntities.isChecked();
                    FileType fileType = FileType.fromFile(new File(inDir, inFile));

                    if (fileType == FileType.LITEMATICA_SCHEMATIC)
                    {
                        if (this.gui.exportType == ExportType.SCHEMATIC)
                        {
                            if (WorldUtils.convertLitematicaSchematicToSchematicaSchematic(inDir, inFile, dir, fileName, ignoreEntities, override, this.gui))
                            {
                                this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_exported_as", fileName);
                                this.gui.getListWidget().refreshEntries();
                            }
                        }
                        else if (this.gui.exportType == ExportType.VANILLA)
                        {
                            if (WorldUtils.convertLitematicaSchematicToVanillaStructure(inDir, inFile, dir, fileName, ignoreEntities, override, this.gui))
                            {
                                this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_exported_as", fileName);
                                this.gui.getListWidget().refreshEntries();
                            }
                        }

                        return;
                    }
                }

                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_export.unsupported_type", this.gui.inputFileName);
            }
        }
    }
}
