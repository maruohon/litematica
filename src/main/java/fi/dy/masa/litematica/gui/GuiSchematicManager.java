package fi.dy.masa.litematica.gui;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.gui.base.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntryType;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.InfoUtils;
import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonHoverText;
import fi.dy.masa.malilib.gui.button.ConfigButtonOptionList;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ScreenShotHelper;

public class GuiSchematicManager extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    private static PreviewGenerator previewGenerator;
    private ExportType exportType = ExportType.SCHEMATICA;

    public GuiSchematicManager()
    {
        super(10, 40);

        this.title = I18n.format("litematica.gui.title.schematic_manager");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int id = 0;
        int x = 10;
        int y = this.height - 36;

        DirectoryEntry selected = this.widget.getSelectedEntry();

        if (selected != null)
        {
            if (selected.getType() == DirectoryEntryType.LITEMATICA_SCHEMATIC)
            {
                x = this.createButton(id++, x, y, ButtonListener.Type.RENAME_SCHEMATIC);
                x = this.createButton(id++, x, y, ButtonListener.Type.SET_PREVIEW);
                x = this.createButton(id++, x, y, ButtonListener.Type.EXPORT_SCHEMATIC);
                x = this.createButton(id++, x, y, ButtonListener.Type.EXPORT_TYPE);
            }
            else if (selected.getType() == DirectoryEntryType.SCHEMATICA_SCHEMATIC || selected.getType() == DirectoryEntryType.VANILLA_STRUCTURE)
            {
                x = this.createButton(id++, x, y, ButtonListener.Type.IMPORT_SCHEMATIC);
            }
        }

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        String label = I18n.format(type.getLabelKey());
        int buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        this.addButton(new ButtonGeneric(id++, this.width - buttonWidth - 10, y, buttonWidth, 20, label), new ButtonListenerChangeMenu(type, null));
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        this.initGui();
    }

    private int createButton(int id, int x, int y, ButtonListener.Type type)
    {
        String label = type.getLabel();
        String hover = type.getHoverText();
        int buttonWidth = this.mc.fontRenderer.getStringWidth(label) + 10;
        ButtonBase button;

        if (type == ButtonListener.Type.EXPORT_TYPE)
        {
            int w1 = this.mc.fontRenderer.getStringWidth(ExportType.SCHEMATICA.getDisplayName()) + 10;
            int w2 = this.mc.fontRenderer.getStringWidth(ExportType.VANILLA.getDisplayName()) + 10;
            buttonWidth = Math.max(w1, w2);
            button = new ConfigButtonOptionList(id, x, y, buttonWidth, 20, new ConfigWrapper());
        }
        else if (hover != null)
        {
            button = new ButtonHoverText(id, x, y, buttonWidth, 20, label, hover);
        }
        else
        {
            button = new ButtonGeneric(id, x, y, buttonWidth, 20, label);
        }

        this.addButton(button, new ButtonListener(type, this));

        return x + buttonWidth + 4;
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
            return ExportType.SCHEMATICA;
        }

        @Override
        public void setOptionListValue(IConfigOptionListEntry value)
        {
            GuiSchematicManager.this.exportType = (ExportType) value;
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonBase>
    {
        private final Type type;
        private final GuiSchematicManager gui;

        public ButtonListener(Type type, GuiSchematicManager gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonBase control)
        {
            DirectoryEntry entry = this.gui.widget.getSelectedEntry();

            if (entry == null)
            {
                this.gui.addMessage(InfoType.ERROR, "litematica.error.schematic_load.no_schematic_selected");
                return;
            }

            File file = entry.getFullPath();

            if (file.exists() == false || file.isFile() == false || file.canRead() == false)
            {
                this.gui.addMessage(InfoType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getName());
                return;
            }

            if (this.type == Type.EXPORT_SCHEMATIC)
            {
                if (entry.getType() == DirectoryEntryType.LITEMATICA_SCHEMATIC)
                {
                }
                else
                {
                    this.gui.addMessage(InfoType.ERROR, "litematica.error.schematic_manager.schematic_export.unsupported_type", file.getName());
                }
            }
            else if (this.type == Type.IMPORT_SCHEMATIC)
            {
                if (entry.getType() == DirectoryEntryType.SCHEMATICA_SCHEMATIC ||
                    entry.getType() == DirectoryEntryType.VANILLA_STRUCTURE)
                {
                    GuiSchematicSaveImported gui = new GuiSchematicSaveImported(entry.getType(), entry.getDirectory(), entry.getName());
                    gui.setParent(this.gui);
                    this.gui.mc.displayGuiScreen(gui);
                }
                else
                {
                    this.gui.addMessage(InfoType.ERROR, "litematica.error.schematic_manager.schematic_import.unsupported_type", file.getName());
                }
            }
            else if (this.type == Type.RENAME_SCHEMATIC)
            {
                LitematicaSchematic schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.getName(), this.gui);
                String oldName = schematic != null ? schematic.getMetadata().getName() : "";
                this.gui.mc.displayGuiScreen(new GuiTextInput(256, "litematica.gui.title.rename_schematic", oldName, this.gui, new SchematicRenamer(entry.getDirectory(), entry.getName(), this.gui)));
            }
            else if (this.type == Type.SET_PREVIEW)
            {
                previewGenerator = new PreviewGenerator(entry.getDirectory(), entry.getName());
                this.gui.mc.displayGuiScreen(null);
                StringUtils.printActionbarMessage("litematica.info.schematic_manager.preview.set_preview_by_taking_a_screenshot");
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonBase control, int mouseButton)
        {
            if (this.type == Type.SET_PREVIEW && mouseButton == 1)
            {
                previewGenerator = null;
            }
            else
            {
                this.actionPerformed(control);
            }
        }

        public enum Type
        {
            IMPORT_SCHEMATIC            ("litematica.gui.button.import"),
            EXPORT_SCHEMATIC            ("litematica.gui.button.schematic_manager.export_as"),
            RENAME_SCHEMATIC            ("litematica.gui.button.rename"),
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
                return I18n.format(this.label);
            }

            @Nullable
            public String getHoverText()
            {
                return this.hoverText != null ? I18n.format(this.hoverText) : null;
            }
        }
    }

    private static class SchematicRenamer implements IStringConsumer
    {
        private final File dir;
        private final String fileName;
        private final GuiSchematicManager gui;

        public SchematicRenamer(File dir, String fileName, GuiSchematicManager gui)
        {
            this.dir = dir;
            this.fileName = fileName;
            this.gui = gui;
        }

        @Override
        public void setString(String string)
        {
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(this.dir, this.fileName, this.gui);

            if (schematic != null)
            {
                schematic.getMetadata().setName(string);
                schematic.getMetadata().setTimeModified(System.currentTimeMillis());

                if (schematic.writeToFile(this.dir, this.fileName, true, this.gui))
                {
                    this.gui.widget.clearSchematicMetadataCache();
                }
            }
            else
            {
                this.gui.setString(I18n.format("litematica.error.schematic_rename.read_failed"));
            }
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
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(this.dir, this.fileName, InfoUtils.INFO_MESSAGE_CONSUMER);

            if (schematic != null)
            {
                Minecraft mc = Minecraft.getMinecraft();
                BufferedImage buf = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());

                int x = buf.getWidth() >= buf.getHeight() ? (buf.getWidth() - buf.getHeight()) / 2 : 0;
                int y = buf.getHeight() >= buf.getWidth() ? (buf.getHeight() - buf.getWidth()) / 2 : 0;
                int size = Math.min(buf.getWidth(), buf.getHeight());
                System.out.printf("w: %d, h: %d, x: %d, y: %d\n", buf.getWidth(), buf.getHeight(), x, y);
                //int[] pixels = buf.getRGB(x, y, size, size, null, 0, buf.getWidth());

                int previewDimensions = 140;
                BufferedImage smallBuf = new BufferedImage(previewDimensions, previewDimensions, 1);
                Graphics graphics = smallBuf.createGraphics();
                graphics.drawImage(buf, 0, 0, previewDimensions, previewDimensions, x, y, x + size, y + size, null);
                graphics.dispose();
                int[] pixels = smallBuf.getRGB(0, 0, previewDimensions, previewDimensions, null, 0, smallBuf.getWidth());

                schematic.getMetadata().setPreviewImagePixelData(pixels);
                schematic.getMetadata().setTimeModified(System.currentTimeMillis());

                schematic.writeToFile(this.dir, this.fileName, true, InfoUtils.INFO_MESSAGE_CONSUMER);

                InfoUtils.INFO_MESSAGE_CONSUMER.setString(GuiLitematicaBase.TXT_GREEN + I18n.format("litematica.info.schematic_manager.preview.success"));
            }
            else
            {
                InfoUtils.INFO_MESSAGE_CONSUMER.setString(GuiLitematicaBase.TXT_RED + I18n.format("litematica.error.schematic_rename.read_failed"));
            }
        }
    }

    public enum ExportType implements IConfigOptionListEntry
    {
        SCHEMATICA  ("Schematica"),
        VANILLA     ("Vanilla");

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

            return ExportType.SCHEMATICA;
        }
    }
}
