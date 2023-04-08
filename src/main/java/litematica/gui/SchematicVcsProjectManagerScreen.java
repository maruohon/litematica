package litematica.gui;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import malilib.gui.BaseListScreen;
import malilib.gui.BaseScreen;
import malilib.gui.ConfirmActionScreen;
import malilib.gui.util.GuiUtils;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.DataListWidget;
import malilib.util.game.wrap.EntityWrap;
import litematica.Reference;
import litematica.data.DataManager;
import litematica.gui.widget.SchematicVcsProjectInfoWidget;
import litematica.gui.widget.list.entry.SchematicVcsVersionEntryWidget;
import litematica.schematic.projects.SchematicProject;
import litematica.schematic.projects.SchematicVersion;
import litematica.schematic.util.SchematicCreationUtils;
import litematica.selection.AreaSelectionManager;

public class SchematicVcsProjectManagerScreen extends BaseListScreen<DataListWidget<SchematicVersion>>
{
    protected final SchematicProject project;
    protected final GenericButton closeProjectButton;
    protected final GenericButton deleteAreaButton;
    protected final GenericButton mainMenuScreenButton;
    protected final GenericButton moveOriginButton;
    protected final GenericButton openAreaEditorButton;
    protected final GenericButton openProjectBrowserButton;
    protected final GenericButton placeToWorldButton;
    protected final GenericButton saveNewVersionButton;
    protected final LabelWidget projectNameLabel;
    protected final SchematicVcsProjectInfoWidget projectInfoWidget;

    public SchematicVcsProjectManagerScreen(SchematicProject project)
    {
        super(10, 35, 20 + 170 + 2, 81);

        this.project = project;

        this.projectNameLabel = new LabelWidget("litematica.label.schematic_vcs.current_project", project.getName());
        this.closeProjectButton         = GenericButton.create("litematica.button.schematic_vcs.close_project", this::closeProject);
        this.deleteAreaButton           = GenericButton.create("litematica.button.schematic_vcs.delete_area", this::deleteArea);
        this.mainMenuScreenButton       = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.moveOriginButton           = GenericButton.create("litematica.button.schematic_vcs.move_origin_to_player", this::moveOrigin);
        this.openAreaEditorButton       = GenericButton.create("litematica.button.schematic_vcs.open_area_editor", this::openAreaEditor);
        this.openProjectBrowserButton   = GenericButton.create("litematica.button.schematic_vcs.open_project_browser", this::openProjectBrowser);
        this.placeToWorldButton         = GenericButton.create("litematica.button.schematic_vcs.place_to_world", this::placeVersionToWorld);
        this.saveNewVersionButton       = GenericButton.create("litematica.button.schematic_vcs.save_version", this::saveNewVersion);
        this.projectInfoWidget = new SchematicVcsProjectInfoWidget(170, 290);
        this.projectInfoWidget.setCurrentProject(project);

        this.deleteAreaButton.translateAndAddHoverString("litematica.hover.button.schematic_vcs.delete_area");
        this.moveOriginButton.translateAndAddHoverString("litematica.hover.button.schematic_vcs.move_origin");
        this.placeToWorldButton.translateAndAddHoverString("litematica.hover.button.schematic_vcs.place_to_world");
        this.saveNewVersionButton.translateAndAddHoverString("litematica.hover.button.schematic_vcs.save_new_version");

        this.setTitle("litematica.title.screen.schematic_vcs.project_manager", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.projectNameLabel);
        this.addWidget(this.closeProjectButton);
        this.addWidget(this.deleteAreaButton);
        this.addWidget(this.mainMenuScreenButton);
        this.addWidget(this.moveOriginButton);
        this.addWidget(this.openAreaEditorButton);
        this.addWidget(this.openProjectBrowserButton);
        this.addWidget(this.placeToWorldButton);
        this.addWidget(this.saveNewVersionButton);
        this.addWidget(this.projectInfoWidget);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.projectNameLabel.setPosition(this.x + 12, this.y + 24);

        this.projectInfoWidget.setHeight(this.getListHeight());
        this.projectInfoWidget.setRight(this.getRight() - 10);
        this.projectInfoWidget.setY(this.projectNameLabel.getBottom() + 1);

        int y = this.getBottom() - 44;
        this.saveNewVersionButton.setPosition(this.x + 10, y);
        this.placeToWorldButton.setPosition(this.saveNewVersionButton.getRight() + 2, y);
        this.deleteAreaButton.setPosition(this.placeToWorldButton.getRight() + 2, y);
        this.openAreaEditorButton.setPosition(this.deleteAreaButton.getRight() + 2, y);
        this.moveOriginButton.setPosition(this.openAreaEditorButton.getRight() + 2, y);

        y += 21;
        this.closeProjectButton.setPosition(this.x + 10, y);
        this.openProjectBrowserButton.setPosition(this.closeProjectButton.getRight() + 2, y);

        this.mainMenuScreenButton.setRight(this.getRight() - 10);
        this.mainMenuScreenButton.setY(y);
    }

    @Override
    protected DataListWidget<SchematicVersion> createListWidget()
    {
        DataListWidget<SchematicVersion> listWidget = new DataListWidget<>(this.project::getAllVersions, true);

        listWidget.setListEntryWidgetFixedHeight(14);
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);
        listWidget.getEntrySelectionHandler().setAllowSelection(true);
        listWidget.setAllowKeyboardNavigation(true);
        listWidget.setEntryFilterStringFunction((v) -> ImmutableList.of(v.getName(), String.valueOf(v.getVersion())));
        listWidget.setDataListEntryWidgetFactory(SchematicVcsVersionEntryWidget::new);
        listWidget.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);

        return listWidget;
    }

    public void onSelectionChange(@Nullable SchematicVersion entry)
    {
        if (entry != null)
        {
            this.project.switchVersion(entry, true);
        }

        this.getListWidget().updateEntryWidgetStates();
        this.projectInfoWidget.updateProjectInfo();
    }

    protected void closeProject()
    {
        DataManager.getSchematicProjectsManager().closeCurrentProject();
        BaseScreen.openScreen(new SchematicVcsProjectBrowserScreen());
    }

    protected void moveOrigin()
    {
        SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

        if (project != null)
        {
            project.setOrigin(EntityWrap.getCameraEntityBlockPos());
            this.projectInfoWidget.updateProjectInfo();
        }
    }

    protected void openAreaEditor()
    {
        AreaSelectionManager manager = DataManager.getAreaSelectionManager();

        if (manager.getCurrentSelection() != null)
        {
            manager.openEditGui(GuiUtils.getCurrentScreen());
        }
    }

    protected void openProjectBrowser()
    {
        BaseScreen.openScreen(new SchematicVcsProjectBrowserScreen());
    }

    protected void saveNewVersion()
    {
        SchematicCreationUtils.saveSchematic(false);
    }

    protected void deleteArea()
    {
        String title = "litematica.title.screen.schematic_vcs.confirm_delete_area";
        String msg = "litematica.info.schematic_vcs.confirm_area_deletion";
        ConfirmActionScreen screen = new ConfirmActionScreen(320, title, this::executeDeleteArea, msg);
        screen.setParent(this);
        BaseScreen.openScreen(screen);
    }

    protected void placeVersionToWorld()
    {
        String title = "litematica.title.screen.schematic_vcs.confirm_place_to_world";
        String msg = "litematica.info.schematic_vcs.confirm_place_to_world";
        ConfirmActionScreen screen = new ConfirmActionScreen(320, title, this::executePlaceVersionToWorld, msg);
        screen.setParent(this);
        BaseScreen.openScreen(screen);
    }

    protected void executeDeleteArea()
    {
        DataManager.getSchematicProjectsManager().deleteLastSeenArea();
    }

    protected void executePlaceVersionToWorld()
    {
        DataManager.getSchematicProjectsManager().pasteCurrentVersionToWorld();
    }
}
