package fi.dy.masa.litematica.gui;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.CachedSchematicData;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicType;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.GuiTextInputFeedback;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.FileUtils.FileDeleter;
import fi.dy.masa.malilib.util.FileUtils.FileRenamer;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicManager extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    private static PreviewGenerator previewGenerator;

    private int nextX;
    private int nextY;

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
        this.nextX = 10;
        this.nextY = this.height - 26;

        if (this.width < 520)
        {
            this.nextY -= 22;
        }

        DirectoryEntry selected = this.getListWidget().getLastSelectedEntry();
        CachedSchematicData data = selected != null ? this.getListWidget().getCachedSchematicData(selected.getFullPath()) : null;

        if (data != null)
        {
            SchematicType<?> type = data.schematic.getType();

            if (type.getHasName())
            {
                this.createButton(ButtonListener.Type.RENAME_SCHEMATIC);
            }

            this.createButton(ButtonListener.Type.RENAME_FILE);
            this.createButton(ButtonListener.Type.CONVERT_FORMAT);

            if (type == SchematicType.LITEMATICA)
            {
                this.createButton(ButtonListener.Type.SET_PREVIEW);
            }

            this.createButton(ButtonListener.Type.DELETE_FILE);
        }

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        String label = StringUtils.translate(type.getLabelKey());
        int buttonWidth = this.getStringWidth(label) + 20;
        this.addButton(new ButtonGeneric(this.width - buttonWidth - 10, this.height - 26, buttonWidth, 20, label), new ButtonListenerChangeMenu(type, null));
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        this.clearButtons();
        this.createButtons();
    }

    private int createButton(ButtonListener.Type type)
    {
        if (this.nextX >= (this.width - 180))
        {
            this.nextX = 10;
            this.nextY += 22;
        }

        ButtonGeneric button = new ButtonGeneric(this.nextX, this.nextY, -1, 20, type.getLabel());
        button.addHoverString(type.getHoverText());
        this.nextX += button.getWidth() + 2;

        this.addButton(button, new ButtonListener(type, this));

        return this.nextX;
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

            CachedSchematicData data = this.gui.getListWidget().getCachedSchematicData(file);

            if (data == null)
            {
                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_selected");
                return;
            }

            SchematicType<?> type = data.schematic.getType();

            if (this.type == Type.RENAME_SCHEMATIC)
            {
                if (type.getHasName())
                {
                    String oldName = data.schematic.getMetadata().getName();
                    GuiBase.openPopupGui(new GuiTextInputFeedback("litematica.gui.title.rename_schematic", oldName, this.gui, new SchematicRenamer(entry.getDirectory(), entry.getName(), data.schematic, this.gui)));
                }
                else
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_manager.schematic_has_no_name", type.getDisplayName());
                }
            }
            else if (this.type == Type.RENAME_FILE)
            {
                String oldName = FileUtils.getNameWithoutExtension(entry.getName());
                GuiBase.openPopupGui(new GuiTextInputFeedback("litematica.gui.title.rename_file", oldName, this.gui, new FileRenamer(entry.getDirectory(), entry.getName())));
            }
            else if (this.type == Type.DELETE_FILE)
            {
                FileDeleter deleter = new FileDeleter(entry.getFullPath());
                GuiBase.openPopupGui(new GuiConfirmAction(400, "litematica.gui.title.confirm_file_deletion", deleter, this.gui, "litematica.gui.message.confirm_file_deletion", entry.getName()));
            }
            else if (this.type == Type.CONVERT_FORMAT)
            {
                GuiSchematicSaveConvert gui = new GuiSchematicSaveConvert(data.schematic, entry.getName());
                gui.setParent(this.gui);
                GuiBase.openGui(gui);
            }
            else if (this.type == Type.SET_PREVIEW)
            {
                if (type == SchematicType.LITEMATICA)
                {
                    previewGenerator = new PreviewGenerator(entry.getDirectory(), entry.getName());
                    GuiBase.openGui(null);
                    String hotkeyName = Hotkeys.SET_SCHEMATIC_PREVIEW.getName();
                    String hotkeyValue = Hotkeys.SET_SCHEMATIC_PREVIEW.getKeybind().getKeysDisplayString();
                    InfoUtils.showGuiAndInGameMessage(MessageType.INFO, 8000, "litematica.info.schematic_manager.preview.info", hotkeyName, hotkeyValue);
                }
                else
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_manager.schematic_has_no_thumbnail", type.getDisplayName());
                }
            }
        }

        public enum Type
        {
            RENAME_SCHEMATIC    ("litematica.gui.button.rename_schematic"),
            RENAME_FILE         ("litematica.gui.button.rename_file"),
            DELETE_FILE         ("litematica.gui.button.delete"),
            CONVERT_FORMAT      ("litematica.gui.button.convert_format", "litematica.gui.button.hover.schematic_manager.convert_format"),
            SET_PREVIEW         ("litematica.gui.button.set_preview", "litematica.info.schematic_manager.preview.right_click_to_cancel");

            private final String label;
            @Nullable private final String hoverText;

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
                return this.hoverText;
            }
        }
    }

    private static class SchematicRenamer implements IStringConsumerFeedback
    {
        private final File dir;
        private final String fileName;
        private final ISchematic schematic;
        private final GuiSchematicManager gui;

        public SchematicRenamer(File dir, String fileName, ISchematic schematic, GuiSchematicManager gui)
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
                String oldName = this.schematic.getMetadata().getName();
                long currentTime = System.currentTimeMillis();
                this.schematic.getMetadata().setName(string);
                this.schematic.getMetadata().setTimeModified(currentTime);

                if (this.schematic.writeToFile(this.dir, this.fileName, true))
                {
                    List<ISchematic> list = SchematicHolder.getInstance().getAllOf(new File(this.dir, this.fileName));

                    for (ISchematic schematic : list)
                    {
                        schematic.getMetadata().setName(string);
                        schematic.getMetadata().setTimeModified(currentTime);

                        // Rename all placements that used the old schematic name (ie. were not manually renamed)
                        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllPlacementsOfSchematic(schematic))
                        {
                            if (placement.getName().equals(oldName))
                            {
                                placement.setName(string);
                            }
                        }
                    }

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
}
