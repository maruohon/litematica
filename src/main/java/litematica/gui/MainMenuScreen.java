package litematica.gui;

import java.util.ArrayList;
import java.util.List;

import malilib.gui.BaseScreen;
import malilib.gui.widget.InteractableWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.util.StringUtils;
import litematica.Reference;
import litematica.data.DataManager;
import litematica.gui.util.LitematicaIcons;
import litematica.schematic.projects.SchematicProject;
import litematica.selection.AreaSelectionType;

public class MainMenuScreen extends BaseScreen
{
    protected final GenericButton areaEditorScreenButton;
    protected final GenericButton areaSelectionBrowserScreenButton;
    protected final GenericButton cycleAreaSelectionModeButton;
    protected final GenericButton cycleToolModeButton;
    protected final GenericButton configScreenButton;
    protected final GenericButton loadSchematicsScreenButton;
    protected final GenericButton loadedSchematicsListScreenButton;
    protected final GenericButton schematicManagerScreenButton;
    protected final GenericButton schematicPlacementsListScreenButton;
    protected final GenericButton schematicVcsScreenButton;
    protected final GenericButton taskManagerScreenButton;
    protected int equalWidthWidgetMaxWidth = -1;

    public MainMenuScreen()
    {
        this.schematicPlacementsListScreenButton = GenericButton.create("litematica.button.change_menu.schematic_placements", LitematicaIcons.SCHEMATIC_PLACEMENTS);
        this.loadedSchematicsListScreenButton    = GenericButton.create("litematica.button.change_menu.loaded_schematics",      LitematicaIcons.LOADED_SCHEMATICS);
        this.loadSchematicsScreenButton          = GenericButton.create("litematica.button.change_menu.load_schematics",        LitematicaIcons.SCHEMATIC_BROWSER);

        this.areaEditorScreenButton              = GenericButton.create("litematica.button.change_menu.area_editor",            LitematicaIcons.AREA_EDITOR);
        this.areaSelectionBrowserScreenButton    = GenericButton.create("litematica.button.change_menu.area_selection_browser", LitematicaIcons.AREA_SELECTION);

        this.cycleAreaSelectionModeButton        = GenericButton.create(MainMenuScreen::getAreaSelectionModeButtonLabel);
        this.cycleToolModeButton                 = GenericButton.create(MainMenuScreen::getToolModeButtonLabel);

        this.configScreenButton           = GenericButton.create("litematica.button.change_menu.config_menu",       LitematicaIcons.CONFIGURATION);
        this.schematicManagerScreenButton = GenericButton.create("litematica.button.change_menu.schematic_manager", LitematicaIcons.SCHEMATIC_MANAGER);
        this.schematicVcsScreenButton     = GenericButton.create("litematica.button.change_menu.schematic_vcs",     LitematicaIcons.SCHEMATIC_VCS);
        this.taskManagerScreenButton      = GenericButton.create("litematica.button.change_menu.task_manager",      LitematicaIcons.TASK_MANAGER);

        this.schematicPlacementsListScreenButton.setActionListener(() -> openScreenWithParent(new SchematicPlacementsListScreen()));
        this.loadedSchematicsListScreenButton.setActionListener(() -> openScreenWithParent(new LoadedSchematicsListScreen()));
        this.loadSchematicsScreenButton.setActionListener(() -> openScreenWithParent(new SchematicBrowserScreen()));

        this.areaEditorScreenButton.setActionListener(DataManager.getAreaSelectionManager()::openAreaEditorScreenWithParent);
        this.areaSelectionBrowserScreenButton.setActionListener(() -> openScreenWithParent(new AreaSelectionBrowserScreen()));
        this.cycleAreaSelectionModeButton.getTextOffset().setCenterHorizontally(false);
        this.cycleAreaSelectionModeButton.setActionListener(() -> {
            DataManager.getAreaSelectionManager().switchSelectionMode();
            this.cycleAreaSelectionModeButton.updateButtonState();
        });

        //this.cycleToolModeButton.getPadding().setLeftRight(6);
        this.cycleToolModeButton.setActionListener((mouseButton) -> {
            DataManager.cycleToolMode(mouseButton == 0);
            this.cycleToolModeButton.updateButtonState();
            return true;
        });

        this.configScreenButton.setActionListener(ConfigScreen::openConfigScreen);
        this.schematicManagerScreenButton.setActionListener(() -> openScreenWithParent(new SchematicManagerScreen()));
        this.schematicVcsScreenButton.setActionListener(MainMenuScreen::openSchematicProjectsScreen);
        this.taskManagerScreenButton.setActionListener(() -> openScreenWithParent(new TaskManagerScreen()));

        this.setTitle("litematica.title.screen.main_menu", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        List<InteractableWidget> list = new ArrayList<>();
        boolean isInitial = this.equalWidthWidgetMaxWidth < 0;
        this.equalWidthWidgetMaxWidth = 0;

        this.addEqualWidthWidget(this.schematicPlacementsListScreenButton, list);
        this.addEqualWidthWidget(this.loadedSchematicsListScreenButton, list);
        this.addEqualWidthWidget(this.loadSchematicsScreenButton, list);

        this.addEqualWidthWidget(this.areaEditorScreenButton, list);
        this.addEqualWidthWidget(this.areaSelectionBrowserScreenButton, list);
        this.addEqualWidthWidget(this.cycleAreaSelectionModeButton, list);

        this.addWidget(this.cycleToolModeButton);

        this.addEqualWidthWidget(this.configScreenButton, list);
        this.addEqualWidthWidget(this.schematicManagerScreenButton, list);
        this.addEqualWidthWidget(this.schematicVcsScreenButton, list);
        this.addEqualWidthWidget(this.taskManagerScreenButton, list);

        if (isInitial)
        {
            // FIXME meh what a mess...
            String modeName1 = AreaSelectionType.SIMPLE.getDisplayName();
            String modeName2 = AreaSelectionType.MULTI_REGION.getDisplayName();
            String s1 = StringUtils.translate("litematica.button.area_editor.selection_mode", modeName1);
            String s2 = StringUtils.translate("litematica.button.area_editor.selection_mode", modeName2);
            this.equalWidthWidgetMaxWidth = Math.max(this.equalWidthWidgetMaxWidth, this.getStringWidth(s1) + 10);
            this.equalWidthWidgetMaxWidth = Math.max(this.equalWidthWidgetMaxWidth, this.getStringWidth(s2) + 10);

            int width = this.equalWidthWidgetMaxWidth + 10;

            for (InteractableWidget widget : list)
            {
                widget.setWidth(width);
            }
        }

        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            this.areaSelectionBrowserScreenButton.setEnabled(false);
            this.areaSelectionBrowserScreenButton.getHoverInfoFactory().removeAll();
            this.areaSelectionBrowserScreenButton.translateAndAddHoverString("litematica.hover.button.main_menu.area_browser_in_vcs_mode");
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 12;
        int y = this.y + 30;
        this.schematicPlacementsListScreenButton.setPosition(x, y);
        this.loadedSchematicsListScreenButton.setPosition(x, y + 22);
        this.loadSchematicsScreenButton.setPosition(x, y + 44);

        this.areaEditorScreenButton.setPosition(x, y + 88);
        this.areaSelectionBrowserScreenButton.setPosition(x, y + 110);
        this.cycleAreaSelectionModeButton.setPosition(x, y + 132);

        this.cycleToolModeButton.setPosition(x, this.getBottom() - 26);

        x = this.schematicPlacementsListScreenButton.getRight() + 20;
        this.configScreenButton.setPosition(x, y);
        this.schematicManagerScreenButton.setPosition(x, y + 88);
        this.taskManagerScreenButton.setPosition(x, y + 110);
        this.schematicVcsScreenButton.setPosition(x, y + 132);
    }

    protected void addEqualWidthWidget(InteractableWidget widget, List<InteractableWidget> widgets)
    {
        this.equalWidthWidgetMaxWidth = Math.max(this.equalWidthWidgetMaxWidth, widget.getWidth());
        widgets.add(widget);
        widget.setAutomaticWidth(false);
        this.addWidget(widget);
    }

    public static void openMainMenuScreen()
    {
        BaseScreen.openScreen(new MainMenuScreen());
    }

    public static void openSchematicProjectsScreen()
    {
        SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

        if (project != null)
        {
            BaseScreen.openScreenWithParent(new SchematicVcsProjectManagerScreen(project));
        }
        else
        {
            BaseScreen.openScreenWithParent(new SchematicVcsProjectBrowserScreen());
        }
    }

    public static String getAreaSelectionModeButtonLabel()
    {
        String modeName = DataManager.getAreaSelectionManager().getSelectionMode().getDisplayName();
        return StringUtils.translate("litematica.button.area_editor.selection_mode", modeName);
    }

    public static String getToolModeButtonLabel()
    {
        String modeName = DataManager.getToolMode().getDisplayName();
        return StringUtils.translate("litematica.button.main_menu.tool_mode", modeName);
    }
}
