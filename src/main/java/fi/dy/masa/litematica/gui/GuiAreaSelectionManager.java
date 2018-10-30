package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicSaveBase.DirectoryCreator;
import fi.dy.masa.litematica.gui.widgets.WidgetAreaSelectionBrowser;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiAreaSelectionManager extends GuiListBase<DirectoryEntry, WidgetDirectoryEntry, WidgetAreaSelectionBrowser> implements ISelectionListener<DirectoryEntry>
{
    private SelectionManager selectionManager;
    private int id;

    public GuiAreaSelectionManager()
    {
        super(10, 50);

        this.title = I18n.format("litematica.gui.title.area_selection_manager");
        this.mc = Minecraft.getMinecraft();
        this.selectionManager = DataManager.getSelectionManager();
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 80;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.id = 0;
        int x = this.mc.currentScreen.width - 13;
        int y = 24;

        x = this.createButton(x, y, ButtonListener.ButtonType.CREATE_DIRECTORY);
        x = this.createButton(x, y, ButtonListener.ButtonType.CREATE_SELECTION);
    }

    private int createButton(int x, int y, ButtonListener.ButtonType type)
    {
        String label = I18n.format(type.getLabelKey());
        int len = this.mc.fontRenderer.getStringWidth(label) + 10;
        x -= (len + 2);
        this.addButton(new ButtonGeneric(this.id++, x, y, len, 20, label), new ButtonListener(type, this));

        return x;
    }

    /**
     * This is the string the DataManager uses for saving/loading/storing the last used directory
     * for each browser GUI type/contet.
     * @return
     */
    public String getBrowserContext()
    {
        return "area_selections";
    }

    public File getDefaultDirectory()
    {
        return DataManager.getAreaSelectionsBaseDirectory();
    }

    @Override
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(DirectoryEntry entry)
    {
        if (entry.getType() == DirectoryEntryType.FILE && FileType.fromFile(entry.getFullPath()) == FileType.JSON)
        {
            String selectionId = entry.getFullPath().getAbsolutePath();

            if (selectionId.equals(this.selectionManager.getCurrentSelectionId()))
            {
                this.selectionManager.setCurrentSelection(null);
            }
            else
            {
                this.selectionManager.setCurrentSelection(selectionId);
            }
        }
    }

    public SelectionManager getSelectionManager()
    {
        return this.selectionManager;
    }

    @Override
    protected WidgetAreaSelectionBrowser createListWidget(int listX, int listY)
    {
        // The width and height will be set to the actual values in initGui()
        WidgetAreaSelectionBrowser widget = new WidgetAreaSelectionBrowser(listX, listY, 100, 100, this, this.getSelectionListener());
        widget.setParent(this);
        return widget;
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiAreaSelectionManager gui;
        private final ButtonType type;

        public ButtonListener(ButtonType type, GuiAreaSelectionManager gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == ButtonType.CREATE_DIRECTORY)
            {
                File dir = this.gui.getListWidget().getCurrentDirectory();
                String title = "litematica.gui.title.create_directory";
                this.gui.mc.displayGuiScreen(new GuiTextInput(256, title, "", this.gui, new DirectoryCreator(dir, this.gui, this.gui.getListWidget())));
            }
            else if (this.type == ButtonType.CREATE_SELECTION)
            {
                File dir = this.gui.getListWidget().getCurrentDirectory();
                String title = "litematica.gui.title.create_area_selection";
                this.gui.mc.displayGuiScreen(new GuiTextInput(256, title, "", this.gui, new SelectionCreator(dir, this.gui)));
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum ButtonType
        {
            CREATE_DIRECTORY    ("litematica.gui.button.area_selections.create_directory"),
            CREATE_SELECTION    ("litematica.gui.button.area_selections.create_selection");

            private final String labelKey;

            private ButtonType(String labelKey)
            {
                this.labelKey = labelKey;
            }

            public String getLabelKey()
            {
                return this.labelKey;
            }
        }
    }

    public static class SelectedBoxRenamer implements IStringConsumer
    {
        private final SelectionManager selectionManager;

        public SelectedBoxRenamer(SelectionManager selectionManager)
        {
            this.selectionManager = selectionManager;
        }

        @Override
        public void setString(String string)
        {
            this.selectionManager.renameSelectedSubRegionBox(string);
        }
    }

    public static class SelectionCreator implements IStringConsumer
    {
        private final File dir;
        private final GuiAreaSelectionManager gui;

        public SelectionCreator(File dir, GuiAreaSelectionManager gui)
        {
            this.dir = dir;
            this.gui = gui;
        }

        @Override
        public void setString(String string)
        {
            this.gui.selectionManager.createNewSelection(this.dir, string);
            this.gui.getListWidget().refreshEntries();
        }
    }
}
