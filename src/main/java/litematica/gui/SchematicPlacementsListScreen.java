package litematica.gui;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import malilib.gui.BaseListScreen;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.DataListWidget;
import malilib.util.StringUtils;
import litematica.Reference;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.gui.util.LitematicaIcons;
import litematica.gui.widget.list.entry.SchematicPlacementEntryWidget;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;

public class SchematicPlacementsListScreen extends BaseListScreen<DataListWidget<SchematicPlacement>>
{
    protected final HashMap<SchematicPlacement, Boolean> modifiedCache = new HashMap<>();
    protected final SchematicPlacementManager manager;
    protected final GenericButton iconsTextToggleButton;
    protected final GenericButton loadSchematicsScreenButton;
    protected final GenericButton loadedSchematicsListScreenButton;
    protected final GenericButton mainMenuButton;
    protected final GenericButton schematicPlacementFileBrowserButton;

    public SchematicPlacementsListScreen()
    {
        super(10, 30, 20, 56);

        this.manager = DataManager.getSchematicPlacementManager();

        this.iconsTextToggleButton               = GenericButton.create(16, this::getIconVsTextButtonLabel, this::toggleIconsVsText);
        this.loadSchematicsScreenButton          = GenericButton.create("litematica.button.change_menu.load_schematics", LitematicaIcons.SCHEMATIC_BROWSER);
        this.loadedSchematicsListScreenButton    = GenericButton.create("litematica.button.change_menu.loaded_schematics", LitematicaIcons.LOADED_SCHEMATICS);
        this.mainMenuButton                      = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.schematicPlacementFileBrowserButton = GenericButton.create("litematica.button.schematic_placement_list.open_placement_browser");

        this.loadSchematicsScreenButton.setActionListener(() -> openScreenWithParent(new SchematicBrowserScreen()));
        this.loadedSchematicsListScreenButton.setActionListener(() -> openScreenWithParent(new LoadedSchematicsListScreen()));
        this.schematicPlacementFileBrowserButton.setActionListener(() -> openScreenWithParent(new SavedSchematicPlacementsBrowserScreen()));

        this.iconsTextToggleButton.translateAndAddHoverString("litematica.hover.button.icon_vs_text_buttons");
        this.iconsTextToggleButton.setIsRightAligned(true);

        this.setTitle("litematica.title.screen.schematic_placements_list", Reference.MOD_VERSION);
    }

    @Override
    protected void initScreen()
    {
        super.initScreen();
        this.clearModifiedSinceSavedCache();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.iconsTextToggleButton);
        this.addWidget(this.loadedSchematicsListScreenButton);
        this.addWidget(this.loadSchematicsScreenButton);
        this.addWidget(this.mainMenuButton);
        this.addWidget(this.schematicPlacementFileBrowserButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.iconsTextToggleButton.setRight(this.getRight() - 10);
        this.iconsTextToggleButton.setBottom(this.getListY() - 1);

        int y = this.getBottom() - 24;
        this.loadSchematicsScreenButton.setPosition(this.x + 10, y);
        this.loadedSchematicsListScreenButton.setPosition(this.loadSchematicsScreenButton.getRight() + 2, y);
        this.schematicPlacementFileBrowserButton.setPosition(this.loadedSchematicsListScreenButton.getRight() + 2, y);

        this.mainMenuButton.setRight(this.getRight() - 10);
        this.mainMenuButton.setY(y);
    }

    public void onSelectionChange(@Nullable SchematicPlacement entry)
    {
        boolean selected = (entry != null) && (entry == this.manager.getSelectedSchematicPlacement());
        this.manager.setSelectedSchematicPlacement(selected ? null : entry);
    }

    @Override
    protected DataListWidget<SchematicPlacement> createListWidget()
    {
        Supplier<List<SchematicPlacement>> supplier = DataManager.getSchematicPlacementManager()::getAllSchematicPlacements;
        DataListWidget<SchematicPlacement> listWidget = new DataListWidget<>(supplier, true);

        listWidget.getEntrySelectionHandler()
                .setAllowSelection(true)
                .setSelectionListener(this::onSelectionChange);
        listWidget.setEntryFilter(SchematicPlacementEntryWidget::placementSearchFilter);
        listWidget.addDefaultSearchBar();
        listWidget.setDataListEntryWidgetFactory((d, cd) -> new SchematicPlacementEntryWidget(d, cd, this));

        return listWidget;
    }

    protected void toggleIconsVsText()
    {
        Configs.Internal.PLACEMENT_LIST_ICON_BUTTONS.toggleBooleanValue();
        this.initScreen();
    }

    public void clearModifiedSinceSavedCache()
    {
        this.modifiedCache.clear();
    }

    public boolean getCachedWasModifiedSinceSaved(SchematicPlacement placement)
    {
        Boolean modified = this.modifiedCache.get(placement);

        if (modified == null)
        {
            modified = placement.wasModifiedSinceSaved();
            this.modifiedCache.put(placement, modified);
        }

        return modified.booleanValue();
    }

    protected String getIconVsTextButtonLabel()
    {
        if (Configs.Internal.PLACEMENT_LIST_ICON_BUTTONS.getBooleanValue())
        {
            return StringUtils.translate("litematica.button.schematic_placement_list.icon_vs_text.icons");
        }
        else
        {
            return StringUtils.translate("litematica.button.schematic_placement_list.icon_vs_text.text");
        }
    }
}
