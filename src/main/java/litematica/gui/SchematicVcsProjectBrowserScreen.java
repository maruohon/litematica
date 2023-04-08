package litematica.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import javax.annotation.Nullable;

import malilib.gui.BaseListScreen;
import malilib.gui.BaseScreen;
import malilib.gui.ConfirmActionScreen;
import malilib.gui.TextInputScreen;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.FileUtils;
import malilib.util.StringUtils;
import litematica.Reference;
import litematica.data.DataManager;
import litematica.gui.widget.SchematicVcsProjectInfoWidget;
import litematica.schematic.projects.SchematicProject;
import litematica.util.LitematicaDirectories;

public class SchematicVcsProjectBrowserScreen extends BaseListScreen<BaseFileBrowserWidget>
{
    protected final GenericButton closeProjectButton;
    protected final GenericButton createProjectButton;
    protected final GenericButton deleteProjectButton;
    protected final GenericButton loadProjectButton;
    protected final GenericButton mainMenuScreenButton;
    protected final GenericButton openManagerButton;
    protected final LabelWidget projectNameLabel;
    protected final SchematicVcsProjectInfoWidget projectInfoWidget;

    public SchematicVcsProjectBrowserScreen()
    {
        super(10, 35, 20 + 170 + 2, 81);

        this.projectNameLabel = new LabelWidget();
        this.closeProjectButton     = GenericButton.create("litematica.button.schematic_vcs.close_project", this::closeProject);
        this.createProjectButton    = GenericButton.create("litematica.button.schematic_vcs.create_project", this::createProject);
        this.deleteProjectButton    = GenericButton.create("litematica.button.schematic_vcs.delete_project", this::deleteProject);
        this.loadProjectButton      = GenericButton.create("litematica.button.schematic_vcs.load_project", this::loadProject);
        this.mainMenuScreenButton   = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.openManagerButton      = GenericButton.create("litematica.button.schematic_vcs.open_manager", this::openManagerScreen);
        this.projectInfoWidget = new SchematicVcsProjectInfoWidget(170, 290);

        this.setTitle("litematica.title.screen.schematic_vcs.project_browser", Reference.MOD_VERSION);
    }


    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.closeProjectButton);
        this.addWidget(this.createProjectButton);
        this.addWidget(this.deleteProjectButton);
        this.addWidget(this.loadProjectButton);
        this.addWidget(this.mainMenuScreenButton);
        this.addWidget(this.openManagerButton);
        this.addWidget(this.projectInfoWidget);

        DirectoryEntry selected = this.getListWidget().getLastSelectedEntry();
        SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();
        boolean projectOpen = project != null;
        boolean projectSelected = selected != null && selected.getName().endsWith(".json");

        this.deleteProjectButton.setEnabled(projectSelected);
        this.loadProjectButton.setEnabled(projectSelected);
        this.closeProjectButton.setEnabled(projectOpen);
        this.openManagerButton.setEnabled(projectOpen);

        if (projectOpen)
        {
            this.addWidget(this.projectNameLabel);
            this.projectNameLabel.translateSetLines("litematica.label.schematic_vcs.current_project", project.getName());
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.projectNameLabel.setPosition(this.x + 12, this.y + 24);

        this.projectInfoWidget.setHeight(this.getListHeight());
        this.projectInfoWidget.setRight(this.getRight() - 10);
        this.projectInfoWidget.setY(this.getListY());

        int y = this.getBottom() - 44;
        this.createProjectButton.setPosition(this.x + 10, y);
        this.loadProjectButton.setPosition(this.createProjectButton.getRight() + 2, y);
        this.deleteProjectButton.setPosition(this.loadProjectButton.getRight() + 2, y);

        y += 21;
        this.closeProjectButton.setPosition(this.x + 10, y);
        this.openManagerButton.setPosition(this.closeProjectButton.getRight() + 2, y);

        this.mainMenuScreenButton.setRight(this.getRight() - 10);
        this.mainMenuScreenButton.setY(y);
    }

    @Override
    protected BaseFileBrowserWidget createListWidget()
    {
        Path dir = LitematicaDirectories.getVCSProjectsBaseDirectory();
        BaseFileBrowserWidget listWidget = new BaseFileBrowserWidget(dir, dir, DataManager.INSTANCE, "vcs_projects");

        listWidget.setParentScreen(this.getParent());
        listWidget.setFileFilter(FileUtils.JSON_FILEFILTER);
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);
        listWidget.getEntrySelectionHandler().setAllowSelection(true);
        listWidget.setRootDirectoryDisplayName(StringUtils.translate("litematica.label.schematic_vcs.browser_root_dir_name"));

        return listWidget;
    }

    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        this.projectInfoWidget.onSelectionChange(entry);
        this.reAddActiveWidgets();
    }

    protected void closeProject()
    {
        DataManager.getSchematicProjectsManager().closeCurrentProject();
        this.reAddActiveWidgets();
    }

    protected void createProject()
    {
        String titleKey = "litematica.title.screen.schematic_vcs.create_new_project";
        TextInputScreen screen = new TextInputScreen(titleKey, this::createProjectByName);
        screen.setParent(this);
        BaseScreen.openPopupScreen(screen);
    }

    protected boolean createProjectByName(String projectName)
    {
        Path dir = this.getListWidget().getCurrentDirectory();
        Path file = dir.resolve(projectName + ".json");

        if (Files.exists(file) == false)
        {
            DataManager.getSchematicProjectsManager().createAndOpenProject(dir, projectName);
            SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();
            BaseScreen.openScreen(new SchematicVcsProjectManagerScreen(project));
            MessageDispatcher.generic("litematica.message.info.schematic_vcs.project_created", projectName);

            return true;
        }

        MessageDispatcher.error("litematica.message.error.schematic_vcs.project_already_exists", projectName);

        return false;
    }

    protected void deleteProject()
    {
        SchematicProject project = this.projectInfoWidget.getSelectedSchematicProject();

        if (project != null)
        {
            String titleKey = "litematica.title.screen.schematic_vcs.confirm_delete_project";
            String infoKey = "litematica.info.schematic_vcs.confirm_project_removal";
            String projectName = project.getName();
            ConfirmActionScreen screen = new ConfirmActionScreen(320, titleKey, this::executeDeleteProject,
                                                                 infoKey, projectName);
            screen.setParent(this);
            BaseScreen.openPopupScreen(screen);
        }
    }

    protected void executeDeleteProject()
    {
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry != null && entry.getType() == DirectoryEntryType.FILE)
        {
            Path file = entry.getFullPath();
            FileUtils.deleteFiles(Collections.singletonList(file), MessageDispatcher.error()::send);
            this.getListWidget().clearSelection();
            this.getListWidget().refreshEntries();
            this.reAddActiveWidgets();
        }
    }

    protected void loadProject()
    {
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry != null && entry.getType() == DirectoryEntryType.FILE)
        {
            SchematicProject project = DataManager.getSchematicProjectsManager().openProject(entry.getFullPath());

            if (project != null)
            {
                BaseScreen.openScreen(new SchematicVcsProjectManagerScreen(project));
                MessageDispatcher.success("litematica.message.info.schematic_vcs.project_loaded", project.getName());
            }
            else
            {
                MessageDispatcher.error("litematica.message.error.schematic_vcs.failed_to_load_project", entry.getName());
            }
        }
    }

    protected void openManagerScreen()
    {
        SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

        if (project != null)
        {
            BaseScreen.openScreen(new SchematicVcsProjectManagerScreen(project));
        }
    }
}
