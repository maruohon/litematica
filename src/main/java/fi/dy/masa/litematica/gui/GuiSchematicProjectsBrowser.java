package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicProjectBrowser;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
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
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicProjectsBrowser extends GuiListBase<DirectoryEntry, WidgetDirectoryEntry, WidgetSchematicProjectBrowser>
                                        implements ISelectionListener<DirectoryEntry>
{
    public GuiSchematicProjectsBrowser()
    {
        super(10, 30);

        this.title = StringUtils.translate("litematica.gui.title.schematic_projects_browser");
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 58;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.createElements();
    }

    private void createElements()
    {
        int x = 10;
        int y = this.height - 24;

        SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

        if (project != null)
        {
            x += this.createButton(x, y, false, ButtonListener.Type.OPEN_MANAGER_GUI);
        }

        x += this.createButton(x, y, false, ButtonListener.Type.CREATE_PROJECT);

        DirectoryEntry selected = this.getListWidget().getLastSelectedEntry();

        if (selected != null && FileType.fromFile(selected.getFullPath()) == FileType.JSON)
        {
            x += this.createButton(x, y, false, ButtonListener.Type.LOAD_PROJECT);
        }

        if (project != null)
        {
            x += this.createButton(x, y, false, ButtonListener.Type.CLOSE_PROJECT);
        }

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        String label = StringUtils.translate(type.getLabelKey());
        int buttonWidth = this.getStringWidth(label) + 20;
        this.addButton(new ButtonGeneric(this.width - buttonWidth - 10, y, buttonWidth, 20, label), new ButtonListenerChangeMenu(type, null));
    }

    private int createButton(int x, int y, boolean rightAlign, ButtonListener.Type type)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, rightAlign, type.getTranslationKey());
        String hover = type.getHoverText();

        if (hover != null)
        {
            button.setHoverStrings(hover);
        }

        this.addButton(button, new ButtonListener(type, this));

        return button.getWidth() + 2;
    }

    private void reCreateGuiElements()
    {
        this.clearButtons();
        this.clearWidgets();

        this.createElements();
    }

    @Override
    @Nullable
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        this.reCreateGuiElements();
    }

    @Override
    protected WidgetSchematicProjectBrowser createListWidget(int listX, int listY)
    {
        // The width and height will be set to the actual values in initGui()
        return new WidgetSchematicProjectBrowser(listX, listY, 100, 100, this.getSelectionListener());
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final Type type;
        private final GuiSchematicProjectsBrowser gui;

        public ButtonListener(Type type, GuiSchematicProjectsBrowser gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == Type.LOAD_PROJECT)
            {
                DirectoryEntry entry = this.gui.getListWidget().getLastSelectedEntry();

                if (entry != null && entry.getType() == DirectoryEntryType.FILE)
                {
                    if (DataManager.getSchematicProjectsManager().openProject(entry.getFullPath()))
                    {
                        SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

                        if (project != null)
                        {
                            GuiSchematicProjectManager gui = new GuiSchematicProjectManager(project);
                            GuiBase.openGui(gui);
                            String name = project.getName();
                            InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.message.schematic_projects.project_loaded", name);
                        }
                        else
                        {
                            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.failed_to_load_project");
                        }
                    }
                }
            }
            else if (this.type == Type.CREATE_PROJECT)
            {
                ProjectCreator creator = new ProjectCreator(this.gui.getListWidget().getCurrentDirectory(), this.gui);
                GuiTextInput gui = new GuiTextInput(256, "litematica.gui.title.create_schematic_project", "", GuiUtils.getCurrentScreen(), creator);
                GuiBase.openGui(gui);
            }
            else if (this.type == Type.CLOSE_PROJECT)
            {
                DataManager.getSchematicProjectsManager().closeCurrentProject();
                this.gui.reCreateGuiElements();
            }
            else if (this.type == Type.OPEN_MANAGER_GUI)
            {
                SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

                if (project != null)
                {
                    GuiSchematicProjectManager gui = new GuiSchematicProjectManager(project);
                    GuiBase.openGui(gui);
                }
            }
        }

        public enum Type
        {
            OPEN_MANAGER_GUI    ("litematica.gui.button.schematic_projects.open_manager_gui"),
            LOAD_PROJECT        ("litematica.gui.button.schematic_projects.load_project"),
            CREATE_PROJECT      ("litematica.gui.button.schematic_projects.create_project"),
            CLOSE_PROJECT       ("litematica.gui.button.schematic_projects.close_project");

            private final String translationKey;
            @Nullable
            private final String hoverText;

            private Type(String label)
            {
                this(label, null);
            }

            private Type(String translationKey, String hoverText)
            {
                this.translationKey = translationKey;
                this.hoverText = hoverText;
            }

            public String getTranslationKey()
            {
                return this.translationKey;
            }

            @Nullable
            public String getHoverText()
            {
                return this.hoverText != null ? StringUtils.translate(this.hoverText) : null;
            }
        }
    }

    private static class ProjectCreator implements IStringConsumerFeedback
    {
        private final File dir;
        private final GuiSchematicProjectsBrowser gui;

        private ProjectCreator(File dir, GuiSchematicProjectsBrowser gui)
        {
            this.dir = dir;
            this.gui = gui;
        }

        @Override
        public boolean setString(String projectName)
        {
            File file = new File(this.dir, projectName + ".json");

            if (file.exists() == false)
            {
                DataManager.getSchematicProjectsManager().createNewProject(this.dir, projectName);
                // In here we need to add the message to the manager GUI, because InfoUtils.showGuiOrInGameMessage()
                // would add it to the current GUI, which here is the text input GUI, which will close immediately
                // after this method returns true.
                this.gui.getListWidget().refreshEntries();
                this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_projects.project_created", projectName);
                return true;
            }

            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.project_already_exists", projectName);

            return false;
        }
    }
}
