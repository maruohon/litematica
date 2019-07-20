package fi.dy.masa.litematica.gui;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.config.options.IConfigOptionList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.GuiTextInputFeedback;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonOptionList;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicManager extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    private static PreviewGenerator previewGenerator;
    private ExportType exportType = ExportType.SCHEMATIC;

    public GuiSchematicManager()
    {
        super(10, 24);

        this.title = StringUtils.translate("litematica.gui.title.schematic_manager");
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_manager";
    }

    @Override
    public File getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected int getBrowserHeight()
    {
        if (this.width < 520)
        {
            return this.height - 78;
        }

        return this.height - 56;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.createButtons();
    }

    private void createButtons()
    {
        int x = 10;
        int y = this.height - 26;

        DirectoryEntry selected = this.getListWidget().getLastSelectedEntry();

        if (selected != null)
        {
            FileType type = FileType.fromFile(selected.getFullPath());

            if (type == FileType.LITEMATICA_SCHEMATIC)
            {
                if (this.width < 520)
                {
                    y -= 22;
                }

                x = this.createButton(x, y, ButtonListener.Type.RENAME_SCHEMATIC);
                x = this.createButton(x, y, ButtonListener.Type.RENAME_FILE);
                x = this.createButton(x, y, ButtonListener.Type.DELETE_SCHEMATIC);

                if (this.width < 520)
                {
                    x = 10;
                    y += 22;
                }

                x = this.createButton(x, y, ButtonListener.Type.EXPORT_SCHEMATIC);
                x = this.createButton(x, y, ButtonListener.Type.EXPORT_TYPE);
                x = this.createButton(x, y, ButtonListener.Type.SET_PREVIEW);
            }
            else if (type == FileType.SCHEMATICA_SCHEMATIC || type == FileType.VANILLA_STRUCTURE)
            {
                x = this.createButton(x, y, ButtonListener.Type.RENAME_FILE);
                x = this.createButton(x, y, ButtonListener.Type.IMPORT_SCHEMATIC);
                x = this.createButton(x, y, ButtonListener.Type.DELETE_SCHEMATIC);
            }
        }

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        String label = StringUtils.translate(type.getLabelKey());
        int buttonWidth = this.getStringWidth(label) + 20;
        this.addButton(new ButtonGeneric(this.width - buttonWidth - 10, y, buttonWidth, 20, label), new ButtonListenerChangeMenu(type, null));
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        this.clearButtons();
        this.createButtons();
    }

    private int createButton(int x, int y, ButtonListener.Type type)
    {
        String label = type.getLabel();
        String hover = type.getHoverText();
        int buttonWidth = this.getStringWidth(label) + 10;
        ButtonGeneric button;

        if (type == ButtonListener.Type.EXPORT_TYPE)
        {
            buttonWidth = this.getStringWidth(this.exportType.getDisplayName()) + 10;
            button = new ConfigButtonOptionList(x, y, buttonWidth, 20, new ConfigWrapper());
        }
        else
        {
            button = new ButtonGeneric(x, y, buttonWidth, 20, label);
        }

        if (hover != null)
        {
            button.setHoverStrings(hover);
        }

        this.addButton(button, new ButtonListener(type, this));

        return x + buttonWidth + 2;
    }

    @Override
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return this;
    }

    public static boolean setPreviewImage()
    {
        if (previewGenerator != null)
        {
            previewGenerator.createAndSetPreviewImage();
            previewGenerator = null;
            return true;
        }

        return false;
    }

    public static boolean hasPendingPreviewTask()
    {
        return previewGenerator != null;
    }

    private class ConfigWrapper implements IConfigOptionList
    {
        @Override
        public IConfigOptionListEntry getOptionListValue()
        {
            return GuiSchematicManager.this.exportType;
        }

        @Override
        public IConfigOptionListEntry getDefaultOptionListValue()
        {
            return ExportType.SCHEMATIC;
        }

        @Override
        public void setOptionListValue(IConfigOptionListEntry value)
        {
            GuiSchematicManager.this.exportType = (ExportType) value;
            GuiSchematicManager.this.clearButtons();
            GuiSchematicManager.this.createButtons();
        }
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final Type type;
        private final GuiSchematicManager gui;

        public ButtonListener(Type type, GuiSchematicManager gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == Type.SET_PREVIEW && mouseButton == 1)
            {
                if (previewGenerator != null)
                {
                    previewGenerator = null;
                    this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_preview_cancelled");
                }

                return;
            }

            DirectoryEntry entry = this.gui.getListWidget().getLastSelectedEntry();

            if (entry == null)
            {
                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_selected");
                return;
            }

            File file = entry.getFullPath();

            if (file.exists() == false || file.isFile() == false || file.canRead() == false)
            {
                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getName());
                return;
            }

            FileType fileType = FileType.fromFile(entry.getFullPath());

            if (this.type == Type.EXPORT_SCHEMATIC)
            {
                if (fileType == FileType.LITEMATICA_SCHEMATIC)
                {
                    GuiSchematicSaveExported gui = new GuiSchematicSaveExported(entry.getType(), entry.getDirectory(), entry.getName(), this.gui.exportType);
                    gui.setParent(this.gui);
                    GuiBase.openGui(gui);
                }
                else
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_manager.schematic_export.unsupported_type", file.getName());
                }
            }
            else if (this.type == Type.IMPORT_SCHEMATIC)
            {
                if (fileType == FileType.SCHEMATICA_SCHEMATIC ||
                    fileType == FileType.VANILLA_STRUCTURE)
                {
                    GuiSchematicSaveImported gui = new GuiSchematicSaveImported(entry.getType(), entry.getDirectory(), entry.getName());
                    gui.setParent(this.gui);
                    GuiBase.openGui(gui);
                }
                else
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_manager.schematic_import.unsupported_type", file.getName());
                }
            }
            else if (this.type == Type.RENAME_SCHEMATIC)
            {
                LitematicaSchematic schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.getName());
                String oldName = schematic != null ? schematic.getMetadata().getName() : "";
                GuiBase.openGui(new GuiTextInputFeedback(256, "litematica.gui.title.rename_schematic", oldName, this.gui, new SchematicRenamer(entry.getDirectory(), entry.getName(), schematic, this.gui)));
            }
            else if (this.type == Type.RENAME_FILE)
            {
                String oldName = FileUtils.getNameWithoutExtension(entry.getName());
                GuiBase.openGui(new GuiTextInputFeedback(256, "litematica.gui.title.rename_file", oldName, this.gui, new FileRenamer(entry.getDirectory(), entry.getName(), entry.getName(), this.gui)));
            }
            else if (this.type == Type.DELETE_SCHEMATIC)
            {
                FileDeleter deleter = new FileDeleter(entry.getFullPath());
                GuiBase.openGui(new GuiConfirmAction(400, "litematica.gui.title.confirm_file_deletion", deleter, this.gui, "litematica.gui.message.confirm_file_deletion", entry.getName()));
            }
            else if (this.type == Type.SET_PREVIEW)
            {
                previewGenerator = new PreviewGenerator(entry.getDirectory(), entry.getName());
                GuiBase.openGui(null);
                InfoUtils.showGuiAndInGameMessage(MessageType.INFO, "litematica.info.schematic_manager.preview.set_preview_by_taking_a_screenshot");
            }
        }

        public enum Type
        {
            IMPORT_SCHEMATIC            ("litematica.gui.button.import"),
            EXPORT_SCHEMATIC            ("litematica.gui.button.schematic_manager.export_as"),
            RENAME_SCHEMATIC            ("litematica.gui.button.rename_schematic"),
            RENAME_FILE                 ("litematica.gui.button.rename_file"),
            DELETE_SCHEMATIC            ("litematica.gui.button.delete"),
            SET_PREVIEW                 ("litematica.gui.button.set_preview", "litematica.info.schematic_manager.preview.right_click_to_cancel"),
            EXPORT_TYPE                 ("");

            private final String label;
            @Nullable
            private final String hoverText;

            private Type(String label)
            {
                this(label, null);
            }

            private Type(String label, String hoverText)
            {
                this.label = label;
                this.hoverText = hoverText;
            }

            public String getLabel()
            {
                return StringUtils.translate(this.label);
            }

            @Nullable
            public String getHoverText()
            {
                return this.hoverText != null ? StringUtils.translate(this.hoverText) : null;
            }
        }
    }

    private static class SchematicRenamer implements IStringConsumerFeedback
    {
        private final File dir;
        private final String fileName;
        private final LitematicaSchematic schematic;
        private final GuiSchematicManager gui;

        public SchematicRenamer(File dir, String fileName, LitematicaSchematic schematic, GuiSchematicManager gui)
        {
            this.dir = dir;
            this.fileName = fileName;
            this.schematic = schematic;
            this.gui = gui;
        }

        @Override
        public boolean setString(String string)
        {
            if (this.schematic != null)
            {
                this.schematic.getMetadata().setName(string);
                this.schematic.getMetadata().setTimeModifiedToNow();

                if (this.schematic.writeToFile(this.dir, this.fileName, true))
                {
                    this.gui.getListWidget().clearSchematicMetadataCache();
                    return true;
                }
            }
            else
            {
                this.gui.addMessage(MessageType.ERROR,"litematica.error.schematic_rename.read_failed", this.fileName);
            }

            return false;
        }
    }

    public static class FileRenamer implements IStringConsumerFeedback
    {
        private final File dir;
        private final String oldName;
        private final String fileName;
        private final GuiBase gui;

        public FileRenamer(File dir, String fileName, String oldName, GuiBase gui)
        {
            this.dir = dir;
            this.fileName = fileName;
            this.oldName = oldName;
            this.gui = gui;
        }

        @Override
        public boolean setString(String string)
        {
            if (FileUtils.doesFilenameContainIllegalCharacters(string))
            {
                this.gui.addMessage(MessageType.ERROR, "litematica.error.illegal_characters_in_file_name", string);
                return false;
            }

            File oldFile = new File(this.dir, this.fileName);
            int indexExt = this.oldName.lastIndexOf('.');
            String newName = indexExt > 0 ? string + this.oldName.substring(indexExt) : string;
            File newFile = new File(this.dir, newName);

            if (newFile.exists() == false)
            {
                if (oldFile.exists() && oldFile.canRead() && oldFile.renameTo(newFile))
                {
                    return true;
                }
                else
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_file_rename.read_failed", this.fileName);
                }
            }
            else
            {
                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_save.file_already_exists", newName);
            }

            return false;
        }
    }

    public static class FileDeleter implements IConfirmationListener
    {
        protected final File file;

        public FileDeleter(File file)
        {
           this.file = file;
        }

        @Override
        public boolean onActionCancelled()
        {
            return false;
        }

        @Override
        public boolean onActionConfirmed()
        {
            try
            {
                this.file.delete();
                return true;
            }
            catch (Exception e)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.failed_to_delete_file", file.getAbsolutePath());
            }

            return false;
        }
    }

    public static class PreviewGenerator
    {
        private final File dir;
        private final String fileName;

        public PreviewGenerator(File dir, String fileName)
        {
            this.dir = dir;
            this.fileName = fileName;
        }

        public void createAndSetPreviewImage()
        {
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(this.dir, this.fileName);

            if (schematic != null)
            {
                try
                {
                    Minecraft mc = Minecraft.getMinecraft();
                    BufferedImage screenshot = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());

                    int x = screenshot.getWidth() >= screenshot.getHeight() ? (screenshot.getWidth() - screenshot.getHeight()) / 2 : 0;
                    int y = screenshot.getHeight() >= screenshot.getWidth() ? (screenshot.getHeight() - screenshot.getWidth()) / 2 : 0;
                    int longerSide = Math.min(screenshot.getWidth(), screenshot.getHeight());
                    //System.out.printf("w: %d, h: %d, x: %d, y: %d\n", screenshot.getWidth(), screenshot.getHeight(), x, y);

                    int previewDimensions = 140;
                    double scaleFactor = (double) previewDimensions / longerSide;
                    BufferedImage scaled = new BufferedImage(previewDimensions, previewDimensions, BufferedImage.TYPE_INT_ARGB);
                    AffineTransform at = new AffineTransform();
                    at.scale(scaleFactor, scaleFactor);
                    AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);

                    Graphics2D graphics = scaled.createGraphics();
                    graphics.drawImage(screenshot.getSubimage(x, y, longerSide, longerSide), scaleOp, 0, 0);

                    int[] pixels = scaled.getRGB(0, 0, previewDimensions, previewDimensions, null, 0, scaled.getWidth());

                    schematic.getMetadata().setPreviewImagePixelData(pixels);
                    schematic.getMetadata().setTimeModifiedToNow();

                    schematic.writeToFile(this.dir, this.fileName, true);

                    InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.info.schematic_manager.preview.success");
                }
                catch (Exception e)
                {
                    Litematica.logger.warn("Exception while creating preview image", e);
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_rename.read_failed");
            }
        }
    }

    public enum ExportType implements IConfigOptionListEntry
    {
        SCHEMATIC   ("Schematic"),
        VANILLA     ("Vanilla Structure");

        private final String displayName;

        private ExportType(String displayName)
        {
            this.displayName = displayName;
        }

        @Override
        public String getStringValue()
        {
            return this.name().toLowerCase();
        }

        @Override
        public String getDisplayName()
        {
            return this.displayName;
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward)
        {
            int id = this.ordinal();

            if (forward)
            {
                if (++id >= values().length)
                {
                    id = 0;
                }
            }
            else
            {
                if (--id < 0)
                {
                    id = values().length - 1;
                }
            }

            return values()[id % values().length];
        }

        @Override
        public ExportType fromString(String name)
        {
            return fromStringStatic(name);
        }

        public static ExportType fromStringStatic(String name)
        {
            for (ExportType al : ExportType.values())
            {
                if (al.name().equalsIgnoreCase(name))
                {
                    return al;
                }
            }

            return ExportType.SCHEMATIC;
        }
    }
}
