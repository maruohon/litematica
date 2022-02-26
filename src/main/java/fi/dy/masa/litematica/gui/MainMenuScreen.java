package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.InteractableWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.util.StringUtils;

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
        this.schematicPlacementsListScreenButton = GenericButton.create("litematica.button.change_menu.schematic_placements",   LitematicaIcons.SCHEMATIC_PLACEMENTS);
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

        this.schematicPlacementsListScreenButton.setActionListener(() -> this.openScreen(new SchematicPlacementsListScreen()));
        this.loadedSchematicsListScreenButton.setActionListener(() -> this.openScreen(new LoadedSchematicsListScreen()));
        // TODO FIXME malilib refactor
        //this.loadSchematicsScreenButton.setActionListener(() -> this.openScreen(new GuiSchematicLoad()));

        this.areaEditorScreenButton.setActionListener(DataManager.getSelectionManager()::openEditGuiWithParent);
        // TODO FIXME malilib refactor
        //this.areaSelectionBrowserScreenButton.setActionListener(() -> this.openScreen(new GuiAreaSelectionManager()));
        this.cycleAreaSelectionModeButton.setActionListener(() -> {
            DataManager.getSelectionManager().switchSelectionMode();
            this.cycleAreaSelectionModeButton.updateButtonState();
        });

        //this.cycleToolModeButton.getPadding().setLeftRight(6);
        this.cycleToolModeButton.setActionListener((mouseButton) -> {
            DataManager.cycleToolMode(mouseButton == 0);
            this.cycleToolModeButton.updateButtonState();
            return true;
        });

        this.configScreenButton.setActionListener(ConfigScreen::openConfigScreen);
        // TODO FIXME malilib refactor
        //this.schematicManagerScreenButton.setActionListener(() -> this.openScreen(new GuiSchematicManager()));
        this.schematicVcsScreenButton.setActionListener(DataManager.getSchematicProjectsManager()::openSchematicProjectsGui);
        this.taskManagerScreenButton.setActionListener(() -> this.openScreen(new TaskManagerScreen()));

        String version = String.format("v%s", Reference.MOD_VERSION);
        this.setTitle("litematica.gui.title.litematica_main_menu", version);
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
            int width = this.equalWidthWidgetMaxWidth + 10;

            for (InteractableWidget widget : list)
            {
                widget.setWidth(width);
            }
        }

        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            this.areaSelectionBrowserScreenButton.setEnabled(false);
            this.areaSelectionBrowserScreenButton.translateAndAddHoverString("litematica.gui.button.hover.schematic_projects.area_browser_disabled_currently_in_projects_mode");
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
        this.schematicManagerScreenButton.setPosition(x, y + 44);
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

    protected void openScreen(BaseScreen screen)
    {
        screen.setParent(this);
        BaseScreen.openScreen(screen);
    }

    public static void openMainMenuScreen()
    {
        MainMenuScreen screen = new MainMenuScreen();
        screen.setParent(GuiUtils.getCurrentScreen());
        BaseScreen.openScreen(screen);
    }

    public static String getAreaSelectionModeButtonLabel()
    {
        String modeName = DataManager.getSelectionManager().getSelectionMode().getDisplayName();
        return StringUtils.translate("litematica.button.main_menu.area_selection_mode", modeName);
    }

    public static String getToolModeButtonLabel()
    {
        String modeName = DataManager.getToolMode().getName();
        return StringUtils.translate("litematica.button.main_menu.tool_mode", modeName);
    }
}
