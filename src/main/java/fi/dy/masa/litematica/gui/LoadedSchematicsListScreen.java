package fi.dy.masa.litematica.gui;

import java.util.List;
import java.util.function.Supplier;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.util.LitematicaIcons;
import fi.dy.masa.litematica.gui.widget.list.entry.SchematicEntryWidget;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.DataListWidget;
import fi.dy.masa.malilib.util.StringUtils;

public class LoadedSchematicsListScreen extends BaseListScreen<DataListWidget<ISchematic>>
{
    protected final GenericButton iconsTextToggleButton;
    protected final GenericButton loadSchematicsScreenButton;
    protected final GenericButton mainMenuScreenButton;
    protected final GenericButton schematicPlacementsListScreenButton;

    public LoadedSchematicsListScreen()
    {
        super(12, 30, 20, 68);

        this.iconsTextToggleButton               = GenericButton.create(this::getIconVsTextButtonLabel);
        this.loadSchematicsScreenButton          = GenericButton.create("litematica.button.change_menu.load_schematics", LitematicaIcons.SCHEMATIC_BROWSER);
        this.mainMenuScreenButton                = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.schematicPlacementsListScreenButton = GenericButton.create("litematica.button.change_menu.schematic_placements", LitematicaIcons.SCHEMATIC_PLACEMENTS);

        this.iconsTextToggleButton.translateAndAddHoverString("litematica.hover.button.icon_vs_text_buttons");
        this.iconsTextToggleButton.setIsRightAligned(true);
        this.iconsTextToggleButton.setActionListener(() -> {
            Configs.Internal.SCHEMATIC_LIST_ICON_BUTTONS.toggleBooleanValue();
            this.initScreen();
        });

        this.loadSchematicsScreenButton.setActionListener(() -> this.openScreen(new SchematicBrowserScreen()));
        this.schematicPlacementsListScreenButton.setActionListener(() -> this.openScreen(new SchematicPlacementsListScreen()));

        this.setTitle("litematica.title.screen.loaded_schematics", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.iconsTextToggleButton);
        this.addWidget(this.loadSchematicsScreenButton);
        this.addWidget(this.schematicPlacementsListScreenButton);
        this.addWidget(this.mainMenuScreenButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.iconsTextToggleButton.setRight(this.getRight() - 22);
        this.iconsTextToggleButton.setY(this.y + 8);

        int y = this.getBottom() - 26;
        this.loadSchematicsScreenButton.setPosition(this.x + 12, y);
        this.schematicPlacementsListScreenButton.setPosition(this.loadSchematicsScreenButton.getRight() + 4, y);
        this.mainMenuScreenButton.setRight(this.getRight() - 10);
        this.mainMenuScreenButton.setY(y);
    }

    @Override
    protected DataListWidget<ISchematic> createListWidget()
    {
        Supplier<List<ISchematic>> supplier = SchematicHolder.getInstance()::getAllSchematics;
        DataListWidget<ISchematic> listWidget = new DataListWidget<>(supplier, true);
        listWidget.addDefaultSearchBar();
        listWidget.setEntryFilter(SchematicEntryWidget::schematicSearchFilter);
        listWidget.setEntryWidgetFactory(SchematicEntryWidget::new);
        return listWidget;
    }

    protected void openScreen(BaseScreen screen)
    {
        screen.setParent(this);
        BaseScreen.openScreen(screen);
    }

    protected String getIconVsTextButtonLabel()
    {
        if (Configs.Internal.SCHEMATIC_LIST_ICON_BUTTONS.getBooleanValue())
        {
            return StringUtils.translate("litematica.button.placement_list.icon_vs_text.icons");
        }
        else
        {
            return StringUtils.translate("litematica.button.placement_list.icon_vs_text.text");
        }
    }
}
