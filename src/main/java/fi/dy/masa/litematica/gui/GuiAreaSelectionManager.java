package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetAreaSelectionBrowser;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiAreaSelectionManager extends GuiListBase<DirectoryEntry, WidgetDirectoryEntry, WidgetAreaSelectionBrowser> implements ISelectionListener<DirectoryEntry>
{
    private SelectionManager selectionManager;

    public GuiAreaSelectionManager()
    {
        super(10, 50);

        this.title = StringUtils.translate("litematica.gui.title.area_selection_manager");
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
        return this.height - 68;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.reCreateGuiElements();

        if (this.selectionManager.getSelectionMode() == SelectionMode.SIMPLE)
        {
            InfoUtils.showGuiMessage(MessageType.WARNING, "litematica.message.warn.area_selection.browser_open_in_simple_mode");
        }
    }

    protected void reCreateGuiElements()
    {
        this.clearButtons();
        this.clearWidgets();

        int x = this.width - 13;
        int y = 24;

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.AREA_EDITOR;
        ButtonGeneric button = new ButtonGeneric(10, y, -1, 20, StringUtils.translate(type.getLabelKey()), type.getIcon());
        this.addButton(button, new ButtonListenerChangeMenu(type, this));

        // These are placed from right to left
        x = this.createButton(x, y, ButtonListener.ButtonType.UNSELECT);
        x = this.createButton(x, y, ButtonListener.ButtonType.FROM_PLACEMENT);
        x = this.createButton(x, y, ButtonListener.ButtonType.NEW_SELECTION);

        String currentSelection = this.selectionManager.getCurrentNormalSelectionId();

        if (currentSelection != null)
        {
            int len = DataManager.getAreaSelectionsBaseDirectory().getAbsolutePath().length();

            if (currentSelection.length() > len + 1)
            {
                currentSelection = FileUtils.getNameWithoutExtension(currentSelection.substring(len + 1));
                String str = StringUtils.translate("litematica.gui.label.area_selection_manager.current_selection", currentSelection);
                int w = this.getStringWidth(str);
                this.addLabel(10, this.height - 15, w, 14, 0xFFFFFFFF, str);
            }
        }
    }

    private int createButton(int x, int y, ButtonListener.ButtonType type)
    {
        String label = StringUtils.translate(type.getLabelKey());
        int len = this.getStringWidth(label) + 10;
        x -= (len + 2);

        ButtonGeneric button = new ButtonGeneric(x, y, len, 20, label);

        if (type == ButtonListener.ButtonType.UNSELECT)
        {
            button.setHoverStrings("litematica.gui.button.hover.area_selections.unselect");
        }

        this.addButton(button, new ButtonListener(type, this));

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

            if (selectionId.equals(this.selectionManager.getCurrentNormalSelectionId()))
            {
                this.selectionManager.setCurrentSelection(null);
            }
            else
            {
                this.selectionManager.setCurrentSelection(selectionId);
            }

            this.reCreateGuiElements();
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
        return new WidgetAreaSelectionBrowser(listX, listY, 100, 100, this, this.getSelectionListener());
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final GuiAreaSelectionManager gui;
        private final ButtonType type;

        public ButtonListener(ButtonType type, GuiAreaSelectionManager gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == ButtonType.NEW_SELECTION)
            {
                File dir = this.gui.getListWidget().getCurrentDirectory();
                String title = "litematica.gui.title.create_area_selection";
                GuiBase.openGui(new GuiTextInput(256, title, "", this.gui, new SelectionCreator(dir, this.gui)));
            }
            else if (this.type == ButtonType.FROM_PLACEMENT)
            {
                SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                if (placement != null)
                {
                    File dir = this.gui.getListWidget().getCurrentDirectory();
                    String title = "litematica.gui.title.create_area_selection_from_placement";
                    GuiBase.openGui(new GuiTextInput(256, title, placement.getName(), this.gui, new SelectionCreatorPlacement(placement, dir, this.gui)));
                }
                else
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.area_selection.no_placement_selected");
                }
            }
            else if (this.type == ButtonType.UNSELECT)
            {
                DataManager.getSelectionManager().setCurrentSelection(null);
                this.gui.reCreateGuiElements();
            }
        }

        public enum ButtonType
        {
            NEW_SELECTION       ("litematica.gui.button.area_selections.create_new_selection"),
            FROM_PLACEMENT      ("litematica.gui.button.area_selections.create_selection_from_placement"),
            UNSELECT            ("litematica.gui.button.area_selections.unselect");

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

    public static class SelectionCreatorPlacement implements IStringConsumerFeedback
    {
        private final SchematicPlacement placement;
        private final File dir;
        private final GuiAreaSelectionManager gui;

        public SelectionCreatorPlacement(SchematicPlacement placement, File dir, GuiAreaSelectionManager gui)
        {
            this.placement = placement;
            this.dir = dir;
            this.gui = gui;
        }

        @Override
        public boolean setString(String name)
        {
            if (this.gui.getSelectionManager().createSelectionFromPlacement(this.dir, this.placement, name, this.gui))
            {
                this.gui.addMessage(MessageType.SUCCESS, "litematica.message.area_selections.selection_created_from_placement", name);
                this.gui.getListWidget().refreshEntries();
                return true;
            }

            return false;
        }
    }
}
