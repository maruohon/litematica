package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.util.LitematicaIcons;
import fi.dy.masa.litematica.gui.widget.SavedSchematicPlacementInfoWidget;
import fi.dy.masa.litematica.gui.widget.list.entry.SchematicPlacementBrowserEntryWidget;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.icon.FileBrowserIconProvider;
import fi.dy.masa.malilib.gui.widget.LabelWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.render.text.StyledText;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class SavedSchematicPlacementsBrowserScreen extends BaseListScreen<BaseFileBrowserWidget>
{
    protected final GenericButton openMainMenuButton;
    protected final GenericButton openPlacementListScreenButton;
    protected final LabelWidget placementNameLabel;
    protected final SavedSchematicPlacementInfoWidget placementInfoWidget;

    protected SavedSchematicPlacementsBrowserScreen()
    {
        super(10, 30, 20 + 170 + 2, 102);

        this.placementNameLabel = new LabelWidget(100, 45, 0xFFFFFFFF);
        this.placementNameLabel.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);
        this.placementNameLabel.getBorderRenderer().getNormalSettings().setEnabled(true);
        this.placementNameLabel.getPadding().setAll(3, 3, 0, 3);

        this.openMainMenuButton            = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.openPlacementListScreenButton = GenericButton.create("litematica.button.change_menu.schematic_placements", LitematicaIcons.SCHEMATIC_PLACEMENTS);
        this.placementInfoWidget = new SavedSchematicPlacementInfoWidget(170, 290);

        this.openPlacementListScreenButton.setActionListener(() -> openScreen(new SchematicPlacementsListScreen()));

        this.setTitle("litematica.title.screen.saved_placements", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.openMainMenuButton);
        this.addWidget(this.openPlacementListScreenButton);
        this.addWidget(this.placementInfoWidget);

        if (this.placementInfoWidget.getSelectedPlacementInfo() != null)
        {
            this.addWidget(this.placementNameLabel);
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.placementInfoWidget.setHeight(this.getListHeight());
        this.placementInfoWidget.setRight(this.getRight() - 10);
        this.placementInfoWidget.setY(this.getListY());

        int y = this.getBottom() - 24;
        this.openPlacementListScreenButton.setPosition(this.x + 10, y);

        this.placementNameLabel.setPosition(this.x + 10, this.getListWidget().getBottom() + 2);
        this.placementNameLabel.setWidth(this.screenWidth - 20);

        this.openMainMenuButton.setRight(this.getRight() - 10);
        this.openMainMenuButton.setY(y);
    }

    @Override
    protected BaseFileBrowserWidget createListWidget()
    {
        File dir = SchematicPlacementUnloaded.getSaveDirectory();
        String context = "saved_placements_" + StringUtils.getWorldOrServerNameOrDefault("__fallback");
        BaseFileBrowserWidget listWidget = new BaseFileBrowserWidget(dir, dir, DataManager.INSTANCE, context);
        FileBrowserIconProvider iconProvider = null;

        listWidget.setListEntryWidgetFixedHeight(20);
        listWidget.setParentScreen(this.getParent());
        listWidget.setFileFilter(FileUtils.JSON_FILEFILTER);
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);
        listWidget.getEntrySelectionHandler().setAllowSelection(true);
        listWidget.setRootDirectoryDisplayName(StringUtils.translate("litematica.label.schematic_placement_browser.placements"));
        listWidget.setEntryFilter(SchematicPlacementBrowserEntryWidget::placementSearchFilter);
        listWidget.setEntryWidgetFactory((d, cd) -> new SchematicPlacementBrowserEntryWidget(d, cd, listWidget, iconProvider));

        return listWidget;
    }

    public void onSelectionChange(DirectoryEntry entry)
    {
        this.placementInfoWidget.onSelectionChange(entry);

        this.reAddActiveWidgets();
        this.updateInfoWidgets();
    }

    protected void updateInfoWidgets()
    {
        SchematicPlacementUnloaded placement = this.placementInfoWidget.getSelectedPlacementInfo();

        if (placement != null)
        {
            DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();
            File schematicFile = placement.getSchematicFile();
            String placementName = placement.getName();
            String placementFile = entry != null ? entry.getFullPath().getName() : "-";
            String schematicName = schematicFile != null ? schematicFile.getName() : "-";
            String schematicPath = schematicFile != null ? schematicFile.getParentFile().getAbsolutePath() : "-";
            StyledText text = StyledText.translate("litematica.label.saved_placement.names",
                                                   placementName, schematicName, schematicPath, placementFile);

            this.placementNameLabel.setWidth(this.screenWidth - 20);
            this.placementNameLabel.setLabelStyledText(text);
        }
        else
        {
            this.placementNameLabel.clearText();
        }
    }
}
