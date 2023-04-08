package litematica.gui;

import java.nio.file.Path;
import javax.annotation.Nullable;

import malilib.gui.BaseListScreen;
import malilib.gui.BaseScreen;
import malilib.gui.TextInputScreen;
import malilib.gui.icon.DefaultFileBrowserIconProvider;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntryType;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.FileNameUtils;
import malilib.util.FileUtils;
import malilib.util.StringUtils;
import litematica.Reference;
import litematica.data.DataManager;
import litematica.gui.util.LitematicaIcons;
import litematica.gui.widget.list.entry.AreaSelectionEntryWidget;
import litematica.schematic.placement.SchematicPlacement;
import litematica.selection.AreaSelectionManager;
import litematica.selection.AreaSelectionType;
import litematica.util.LitematicaDirectories;

public class AreaSelectionBrowserScreen extends BaseListScreen<BaseFileBrowserWidget>
{
    protected final AreaSelectionManager areaSelectionManager;
    protected final GenericButton createSelectionButton;
    protected final GenericButton openAreaEditorButton;
    protected final GenericButton selectionFromPlacementButton;
    protected final GenericButton unselectButton;
    protected final LabelWidget currentSelectionLabel;
    protected boolean simpleModeWarningShown;

    public AreaSelectionBrowserScreen()
    {
        super(10, 46, 20, 64);

        this.areaSelectionManager = DataManager.getAreaSelectionManager();

        this.createSelectionButton        = GenericButton.create("litematica.button.area_selection_browser.create_selection", this::createSelection);
        this.openAreaEditorButton         = GenericButton.create("litematica.button.change_menu.area_editor", LitematicaIcons.AREA_EDITOR);
        this.selectionFromPlacementButton = GenericButton.create("litematica.button.area_selection_browser.from_placement", this::selectionFromPlacement);
        this.unselectButton               = GenericButton.create("litematica.button.area_selection_browser.unselect", this::unselect);
        this.currentSelectionLabel = new LabelWidget();

        this.openAreaEditorButton.setActionListener(DataManager.getAreaSelectionManager()::openAreaEditorScreenWithParent);
        this.selectionFromPlacementButton.translateAndAddHoverString("litematica.info.area_browser.from_placement");
        this.unselectButton.translateAndAddHoverString("litematica.hover.button.area_selection_browser.unselect");

        this.selectionFromPlacementButton.setEnabled(DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement() != null);

        this.addPostInitListener(this::printSimpleModeWarning);
        this.setTitle("litematica.title.screen.area_selection_browser", Reference.MOD_VERSION);
        this.updateCurrentSelectionLabel();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.createSelectionButton);
        this.addWidget(this.openAreaEditorButton);
        this.addWidget(this.selectionFromPlacementButton);
        this.addWidget(this.unselectButton);
        this.addWidget(this.currentSelectionLabel);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int y = this.y + 24;
        this.openAreaEditorButton.setPosition(this.x + 10, y);
        this.unselectButton.setRight(this.getRight() - 13);
        this.selectionFromPlacementButton.setRight(this.unselectButton.getX() - 2);
        this.createSelectionButton.setRight(this.selectionFromPlacementButton.getX() - 2);

        this.unselectButton.setY(y);
        this.selectionFromPlacementButton.setY(y);
        this.createSelectionButton.setY(y);
        this.currentSelectionLabel.setPosition(this.x + 12, this.getBottom() - 14);
    }

    @Override
    protected BaseFileBrowserWidget createListWidget()
    {
        Path dir = LitematicaDirectories.getAreaSelectionsBaseDirectory();
        BaseFileBrowserWidget listWidget = new BaseFileBrowserWidget(dir, dir, DataManager.INSTANCE, "area_selections");

        listWidget.setListEntryWidgetFixedHeight(18);
        listWidget.setParentScreen(this.getParent());
        listWidget.setFileFilter(FileUtils.JSON_FILEFILTER);
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);
        listWidget.getEntrySelectionHandler().setAllowSelection(true);
        listWidget.setRootDirectoryDisplayName(StringUtils.translate("litematica.label.area_selection_browser.selections"));
        listWidget.setDataListEntryWidgetFactory((d, cd) -> new AreaSelectionEntryWidget(d, cd, listWidget, new DefaultFileBrowserIconProvider(), this.areaSelectionManager));

        return listWidget;
    }

    protected void updateCurrentSelectionLabel()
    {
        String currentSelection = this.areaSelectionManager.getCurrentMultiRegionSelectionId();

        if (currentSelection != null)
        {
            int len = LitematicaDirectories.getAreaSelectionsBaseDirectory().toAbsolutePath().toString().length();

            if (currentSelection.length() > len + 1)
            {
                currentSelection = FileNameUtils.getFileNameWithoutExtension(currentSelection.substring(len + 1));
                String key = "litematica.label.area_selection_browser.current_selection";
                this.currentSelectionLabel.translateSetLines(key, currentSelection);
            }
        }
        else
        {
            this.currentSelectionLabel.clearText();
        }
    }

    protected void printSimpleModeWarning()
    {
        if (this.simpleModeWarningShown == false &&
            this.areaSelectionManager.getSelectionMode() == AreaSelectionType.SIMPLE)
        {
            MessageDispatcher.warning(5000).translate("litematica.message.warn.area_selection_browser.in_simple_mode");
            this.simpleModeWarningShown = true;
        }
    }

    protected void createSelection()
    {
        String title = "litematica.title.screen.create_area_selection";
        TextInputScreen screen = new TextInputScreen(title, "", this::createSelectionByName);
        screen.setParent(this);
        BaseScreen.openPopupScreen(screen);
    }

    protected boolean createSelectionByName(String name)
    {
        Path dir = this.getListWidget().getCurrentDirectory();
        this.areaSelectionManager.createNewSelection(dir, name);
        this.getListWidget().refreshEntries();
        this.updateCurrentSelectionLabel();
        return true;
    }

    protected void selectionFromPlacement()
    {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null)
        {
            String title = "litematica.title.screen.area_selection_browser.selection_from_placement";
            TextInputScreen screen = new TextInputScreen(title, placement.getName(),
                                                         this::selectionFromPlacementByName);
            screen.setInfoText("litematica.info.area_browser.from_placement");
            screen.setParent(this);
            BaseScreen.openPopupScreen(screen);
        }
        else
        {
            MessageDispatcher.error("litematica.message.error.area_selection_browser.no_placement_selected");
        }
    }

    protected boolean selectionFromPlacementByName(String name)
    {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        Path dir = this.getListWidget().getCurrentDirectory();

        if (placement != null && this.areaSelectionManager.createSelectionFromPlacement(dir, placement, name))
        {
            String key = "litematica.message.area_selections.selection_created_from_placement";
            MessageDispatcher.success().translate(key, name);
            this.getListWidget().refreshEntries();
            this.updateCurrentSelectionLabel();
            return true;
        }

        return false;
    }

    protected void unselect()
    {
        DataManager.getAreaSelectionManager().setCurrentSelection(null);
        this.getListWidget().clearSelection();
        this.getListWidget().updateEntryWidgetStates();
        this.updateCurrentSelectionLabel();
    }

    protected void onSelectionChange(@Nullable BaseFileBrowserWidget.DirectoryEntry entry)
    {
        if (entry == null)
        {
            this.areaSelectionManager.setCurrentSelection(null);
        }
        else if (entry.getType() == DirectoryEntryType.FILE && entry.getName().endsWith(".json"))
        {
            String selectionId = entry.getFullPath().toString();

            if (selectionId.equals(this.areaSelectionManager.getCurrentMultiRegionSelectionId()))
            {
                this.areaSelectionManager.setCurrentSelection(null);
            }
            else
            {
                this.areaSelectionManager.setCurrentSelection(selectionId);
            }
        }

        this.updateCurrentSelectionLabel();
        this.getListWidget().updateEntryWidgetStates();
    }
}
