package fi.dy.masa.litematica.gui;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.widget.list.entry.SchematicPlacementEntryWidget;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.DataListWidget;
import fi.dy.masa.malilib.util.StringUtils;

public class SchematicPlacementsListScreen extends BaseListScreen<DataListWidget<SchematicPlacementUnloaded>>
{
    protected final HashMap<SchematicPlacementUnloaded, Boolean> modifiedCache = new HashMap<>();
    protected final SchematicPlacementManager manager;
    protected final GenericButton iconsTextToggleButton;
    protected final GenericButton loadedSchematicsListScreenButton;
    protected final GenericButton mainMenuButton;
    protected final GenericButton schematicPlacementFileBrowserButton;

    public SchematicPlacementsListScreen()
    {
        super(12, 30, 20, 64);

        this.iconsTextToggleButton               = GenericButton.create(this::getIconVsTextButtonLabel);
        this.loadedSchematicsListScreenButton    = GenericButton.create("litematica.button.change_menu.loaded_schematics", LitematicaIcons.LOADED_SCHEMATICS);
        this.mainMenuButton                      = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.schematicPlacementFileBrowserButton = GenericButton.create("litematica.button.placement_list.open_placement_browser");

        this.loadedSchematicsListScreenButton.setActionListener(() -> this.openScreen(new LoadedSchematicsListScreen()));
        //this.schematicPlacementFileBrowserButton.setActionListener(() -> this.openScreen(new GuiSchematicPlacementFileBrowser()));
        // TODO FIXME malilib refactor

        this.iconsTextToggleButton.translateAndAddHoverString("litematica.hover.button.icon_vs_text_buttons");
        this.iconsTextToggleButton.setIsRightAligned(true);
        this.iconsTextToggleButton.setActionListener(() -> {
            Configs.Internal.PLACEMENT_LIST_ICON_BUTTONS.toggleBooleanValue();
            this.initScreen();
        });

        this.manager = DataManager.getSchematicPlacementManager();
        this.setTitle("litematica.gui.title.manage_schematic_placements");
    }

    @Override
    protected void initScreen()
    {
        super.initScreen();
        this.modifiedCache.clear();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.loadedSchematicsListScreenButton);
        this.addWidget(this.schematicPlacementFileBrowserButton);
        this.addWidget(this.iconsTextToggleButton);
        this.addWidget(this.mainMenuButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.iconsTextToggleButton.setRight(this.getRight() - 22);
        this.iconsTextToggleButton.setY(this.y + 8);

        int x = this.x + 12;
        int y = this.getBottom() - 26;
        this.loadedSchematicsListScreenButton.setPosition(x, y);
        x = this.loadedSchematicsListScreenButton.getRight() + 2;

        this.schematicPlacementFileBrowserButton.setPosition(x, y);
        x = this.schematicPlacementFileBrowserButton.getRight() + 2;

        this.mainMenuButton.setPosition(x, y);
    }

    protected void openScreen(BaseScreen screen)
    {
        screen.setParent(this);
        BaseScreen.openScreen(screen);
    }

    public void onSelectionChange(@Nullable SchematicPlacementUnloaded entry)
    {
        if (entry == null || entry.isLoaded())
        {
            boolean isCurrentlySelected = entry == this.manager.getSelectedSchematicPlacement();
            this.manager.setSelectedSchematicPlacement(isCurrentlySelected ? null : (SchematicPlacement) entry);
        }
    }

    @Override
    protected DataListWidget<SchematicPlacementUnloaded> createListWidget()
    {
        Supplier<List<SchematicPlacementUnloaded>> supplier = DataManager.getSchematicPlacementManager()::getAllSchematicPlacements;
        DataListWidget<SchematicPlacementUnloaded> listWidget = new DataListWidget<>(supplier, true);

        listWidget.getEntrySelectionHandler()
                .setAllowSelection(true)
                .setSelectionListener(this::onSelectionChange);
        listWidget.setEntryFilter(SchematicPlacementEntryWidget::placementSearchFilter);
        listWidget.addDefaultSearchBar();
        listWidget.setEntryWidgetFactory((d, cd) -> new SchematicPlacementEntryWidget(d, cd, this));

        return listWidget;
    }

    public boolean getCachedWasModifiedSinceSaved(SchematicPlacementUnloaded placement)
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
            return StringUtils.translate("litematica.button.placement_list.icon_vs_text.icons");
        }
        else
        {
            return StringUtils.translate("litematica.button.placement_list.icon_vs_text.text");
        }
    }
}
