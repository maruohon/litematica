package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicProjectBrowser;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class GuiSchematicProjectsManager extends GuiListBase<DirectoryEntry, WidgetDirectoryEntry, WidgetSchematicProjectBrowser>
                                        implements ISelectionListener<DirectoryEntry>
{
    public GuiSchematicProjectsManager()
    {
        super(10, 40);

        this.title = I18n.format("litematica.gui.title.schematic_projects_manager");
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

        this.createElements();
    }

    private void createElements()
    {
        int x = 10;
        int y = this.height - 36;

        SchematicProject project = DataManager.getSchematicVersionManager().getCurrentProject();

        // There is currently a project open
        if (project != null)
        {
            String str = I18n.format("litematica.gui.label.schematic_projects.currently_open_project", project.getName());
            int w = this.mc.fontRenderer.getStringWidth(str);
            this.addLabel(x, 26, w, 14, 0xFFFFFFFF, str);

            this.createButton(this.width - 10, 16, true, ButtonListener.Type.MOVE_ORIGIN);
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
        String label = I18n.format(type.getLabelKey());
        int buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        this.addButton(new ButtonGeneric(this.width - buttonWidth - 10, y, buttonWidth, 20, label), new ButtonListenerChangeMenu(type, null));
    }

    private int createButton(int x, int y, boolean rightAlign, ButtonListener.Type type)
    {
        ButtonGeneric button = ButtonGeneric.createGeneric(x, y, -1, rightAlign, type.getTranslationKey());
        String hover = type.getHoverText();

        if (hover != null)
        {
            button.setHoverStrings(hover);
        }

        this.addButton(button, new ButtonListener(type, this));

        return button.getButtonWidth() + 4;
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
    public void onSelectionChange(DirectoryEntry entry)
    {
        this.reCreateGuiElements();
    }

    @Override
    protected WidgetSchematicProjectBrowser createListWidget(int listX, int listY)
    {
        // The width and height will be set to the actual values in initGui()
        return new WidgetSchematicProjectBrowser(listX, listY, 100, 100, this.getSelectionListener());
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final Type type;
        private final GuiSchematicProjectsManager gui;

        public ButtonListener(Type type, GuiSchematicProjectsManager gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            if (this.type == Type.LOAD_PROJECT)
            {
                DirectoryEntry entry = this.gui.getListWidget().getLastSelectedEntry();

                if (entry != null && entry.getType() == DirectoryEntryType.FILE)
                {
                    if (DataManager.getSchematicVersionManager().openProject(entry.getFullPath()))
                    {
                        SchematicProject project = DataManager.getSchematicVersionManager().getCurrentProject();
                        String name = project != null ? project.getName() : "<error>";
                        InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.message.schematic_projects.project_loaded", name);
                        this.gui.reCreateGuiElements();
                    }
                }
            }
            else if (this.type == Type.CREATE_PROJECT)
            {
                ProjectCreator creator = new ProjectCreator(this.gui.getListWidget().getCurrentDirectory(), this.gui);
                GuiTextInput gui = new GuiTextInput(256, "litematica.gui.title.create_schematic_version_project", "", this.gui.mc.currentScreen, creator);
                this.gui.mc.displayGuiScreen(gui);
            }
            else if (this.type == Type.CLOSE_PROJECT)
            {
                DataManager.getSchematicVersionManager().closeCurrentProject();
                this.gui.reCreateGuiElements();
            }
            else if (this.type == Type.MOVE_ORIGIN)
            {
                SchematicProject project = DataManager.getSchematicVersionManager().getCurrentProject();

                if (project != null)
                {
                    project.setOrigin(new BlockPos(this.gui.mc.player));
                    this.gui.reCreateGuiElements();
                }
            }
        }

        public enum Type
        {
            LOAD_PROJECT    ("litematica.gui.button.schematic_projects.load_project"),
            CREATE_PROJECT  ("litematica.gui.button.schematic_projects.create_project"),
            CLOSE_PROJECT   ("litematica.gui.button.schematic_projects.close_project"),
            MOVE_ORIGIN     ("litematica.gui.button.schematic_projects.move_origin_to_player", "litematica.gui.button.hover.schematic_projects.move_origin_to_player");

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
                return this.hoverText != null ? I18n.format(this.hoverText) : null;
            }
        }
    }

    private static class ProjectCreator implements IStringConsumerFeedback
    {
        private final File dir;
        private final GuiSchematicProjectsManager gui;

        private ProjectCreator(File dir, GuiSchematicProjectsManager gui)
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
                DataManager.getSchematicVersionManager().createNewProject(this.dir, projectName);
                // In here we need to add the message to the manager GUI, because InfoUtils.showGuiOrInGameMessage()
                // would add it to the current GUI, which here is the text input GUI, which will close immediately
                // after this method returns true.
                this.gui.getListWidget().refreshEntries();
                this.gui.addMessage(MessageType.SUCCESS, "litematica.message.schematic_projects.project_created", projectName);
                System.out.printf("plop\n");
                return true;
            }

            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.project_already_exists", projectName);

            return false;
        }
    }
}
