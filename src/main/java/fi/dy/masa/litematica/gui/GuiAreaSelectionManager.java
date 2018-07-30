package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicSaveBase.DirectoryCreator;
import fi.dy.masa.litematica.gui.base.GuiListBase;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.WidgetAreaSelectionBrowser;
import fi.dy.masa.litematica.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
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
        this.selectionManager = DataManager.getInstance().getSelectionManager();
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
        return DataManager.ROOT_AREA_SELECTIONS_DIRECTORY;
    }

    @Override
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(DirectoryEntry entry)
    {
        if (entry.getType() == DirectoryEntryType.JSON)
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
        widget.setParent(this.getParent());
        return widget;
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiAreaSelectionManager gui;
        private final SelectionManager selectionManager;
        private final ButtonType type;

        public ButtonListener(ButtonType type, GuiAreaSelectionManager gui)
        {
            this.type = type;
            this.gui = gui;
            this.selectionManager = gui.selectionManager;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == ButtonType.CREATE_DIRECTORY)
            {
                File dir = this.gui.widget.getCurrentDirectory();
                String title = "litematica.gui.title.create_directory";
                this.gui.mc.displayGuiScreen(new GuiTextInput(256, title, "", this.gui, new DirectoryCreator(dir, this.gui)));
            }
            else if (this.type == ButtonType.CREATE_SELECTION)
            {
                this.selectionManager.createNewSelection(this.gui.widget.getCurrentDirectory());
                this.gui.widget.refreshEntries();
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
}
