package litematica.gui;

import malilib.gui.icon.DefaultFileBrowserIconProvider;
import malilib.gui.icon.FileBrowserIconProvider;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import litematica.Reference;
import litematica.gui.util.LitematicaIcons;
import litematica.gui.widget.list.entry.SchematicPlacementBrowserEntryWidget;

public class SavedSchematicPlacementsBrowserScreen extends BaseSavedSchematicPlacementsBrowserScreen
{
    protected final GenericButton openMainMenuButton;
    protected final GenericButton openPlacementListScreenButton;

    public SavedSchematicPlacementsBrowserScreen()
    {
        super(10, 30, 20 + 2 + 170, 102);

        this.openMainMenuButton            = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.openPlacementListScreenButton = GenericButton.create("litematica.button.change_menu.schematic_placements", LitematicaIcons.SCHEMATIC_PLACEMENTS);
        this.openPlacementListScreenButton.setActionListener(() -> openScreen(new SchematicPlacementsListScreen()));

        this.setTitle("litematica.title.screen.saved_placements", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.openMainMenuButton);
        this.addWidget(this.openPlacementListScreenButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int y = this.getBottom() - 24;
        this.openPlacementListScreenButton.setPosition(this.x + 10, y);

        this.openMainMenuButton.setRight(this.getRight() - 10);
        this.openMainMenuButton.setY(y);
    }

    @Override
    protected BaseFileBrowserWidget createListWidget()
    {
        BaseFileBrowserWidget listWidget = super.createListWidget();
        FileBrowserIconProvider iconProvider = new DefaultFileBrowserIconProvider();
        listWidget.setAreEntriesFixedHeight(false);
        listWidget.setListEntryWidgetFixedHeight(20);
        listWidget.setDataListEntryWidgetFactory((d, cd) -> new SchematicPlacementBrowserEntryWidget(d, cd, listWidget, iconProvider));
        return listWidget;
    }
}
