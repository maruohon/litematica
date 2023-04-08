package litematica.gui;

import java.nio.file.Path;
import javax.annotation.Nullable;

import malilib.gui.BaseListScreen;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.util.FileUtils;
import malilib.util.StringUtils;
import litematica.data.DataManager;
import litematica.gui.widget.SavedSchematicPlacementInfoWidget;
import litematica.gui.widget.list.entry.SchematicPlacementBrowserEntryWidget;
import litematica.schematic.placement.SchematicPlacement;
import litematica.util.LitematicaDirectories;

public abstract class BaseSavedSchematicPlacementsBrowserScreen extends BaseListScreen<BaseFileBrowserWidget>
{
    protected final LabelWidget placementNameLabel;
    protected final SavedSchematicPlacementInfoWidget placementInfoWidget;

    protected BaseSavedSchematicPlacementsBrowserScreen(int listX, int listY, int totalListMarginX, int totalListMarginY)
    {
        super(listX, listY, totalListMarginX, totalListMarginY);

        this.placementNameLabel = new LabelWidget(100, 45);
        this.placementNameLabel.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);
        this.placementNameLabel.getBorderRenderer().getNormalSettings().setEnabled(true);
        this.placementNameLabel.getPadding().setAll(3, 3, 0, 3);

        this.placementInfoWidget = new SavedSchematicPlacementInfoWidget(170, 290);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

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

        this.placementNameLabel.setPosition(this.x + 10, this.getListWidget().getBottom() + 2);
        this.placementNameLabel.setWidth(this.screenWidth - 20);
    }

    @Override
    protected BaseFileBrowserWidget createListWidget()
    {
        Path dir = LitematicaDirectories.getPlacementSaveFilesDirectory();
        String context = "saved_placements_" + StringUtils.getWorldOrServerNameOrDefault("_fallback");
        BaseFileBrowserWidget listWidget = new BaseFileBrowserWidget(dir, dir, DataManager.INSTANCE, context);

        listWidget.setParentScreen(this.getParent());
        listWidget.setFileFilter(FileUtils.JSON_FILEFILTER);
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);
        listWidget.getEntrySelectionHandler().setAllowSelection(true);
        listWidget.setRootDirectoryDisplayName(StringUtils.translate("litematica.label.schematic_placement_browser.placements"));
        listWidget.setEntryFilter(SchematicPlacementBrowserEntryWidget::placementSearchFilter);

        return listWidget;
    }

    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        this.placementInfoWidget.onSelectionChange(entry);

        this.reAddActiveWidgets();
        this.updateInfoWidgets();
    }

    protected void updateInfoWidgets()
    {
        SchematicPlacement placement = this.placementInfoWidget.getSelectedPlacementInfo();

        if (placement != null)
        {
            Path schematicFile = placement.getSchematicFile();
            String placementName = placement.getName();
            String placementFile = placement.getSaveFile() != null ? placement.getSaveFile() : "-";
            String schematicName = schematicFile != null ? schematicFile.getFileName().toString() : "-";
            String schematicPath = schematicFile != null ? schematicFile.getParent().toAbsolutePath().toString() : "-";

            this.placementNameLabel.setWidth(this.screenWidth - 20);
            this.placementNameLabel.translateSetLines("litematica.label.saved_placement.names",
                                                      placementName, schematicName, schematicPath, placementFile);
        }
        else
        {
            this.placementNameLabel.clearText();
        }
    }
}
