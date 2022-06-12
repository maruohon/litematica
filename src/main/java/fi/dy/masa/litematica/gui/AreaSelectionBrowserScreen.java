package fi.dy.masa.litematica.gui;

import java.nio.file.Path;
import javax.annotation.Nullable;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.TextInputScreen;
import fi.dy.masa.malilib.gui.widget.LabelWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.util.LitematicaIcons;
import fi.dy.masa.litematica.gui.widget.list.entry.AreaSelectionEntryWidget;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.FileType;

public class AreaSelectionBrowserScreen extends BaseListScreen<BaseFileBrowserWidget>
{
    protected final SelectionManager selectionManager;
    protected final GenericButton createSelectionButton;
    protected final GenericButton openAreaEditorButton;
    protected final GenericButton selectionFromPlacementButton;
    protected final GenericButton unselectButton;
    protected final LabelWidget currentSelectionLabel;

    public AreaSelectionBrowserScreen()
    {
        super(10, 46, 20, 64);

        this.selectionManager = DataManager.getSelectionManager();

        this.createSelectionButton        = GenericButton.create("litematica.button.area_selection_browser.create_selection", this::createSelection);
        this.openAreaEditorButton         = GenericButton.create("litematica.button.change_menu.area_editor", LitematicaIcons.AREA_EDITOR);
        this.selectionFromPlacementButton = GenericButton.create("litematica.button.area_selection_browser.from_placement", this::selectionFromPlacement);
        this.unselectButton               = GenericButton.create("litematica.button.area_selection_browser.unselect", this::unselect);
        this.currentSelectionLabel = new LabelWidget();

        this.openAreaEditorButton.setActionListener(DataManager.getSelectionManager()::openAreaEditorScreenWithParent);
        this.selectionFromPlacementButton.translateAndAddHoverString("litematica.info.area_browser.from_placement");
        this.unselectButton.translateAndAddHoverString("litematica.hover.button.area_selection_browser.unselect");

        this.selectionFromPlacementButton.setEnabled(DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement() != null);

        this.setTitle("litematica.title.screen.area_selection_browser", Reference.MOD_VERSION);
        this.updateCurrentSelectionLabel();
    }

    @Override
    protected void initScreen()
    {
        super.initScreen();

        if (this.selectionManager.getSelectionMode() == SelectionMode.SIMPLE)
        {
            MessageDispatcher.warning(5000).translate("litematica.message.warn.area_selection_browser.in_simple_mode");
        }
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
        Path dir = DataManager.getAreaSelectionsBaseDirectory();
        BaseFileBrowserWidget listWidget = new BaseFileBrowserWidget(dir, dir, DataManager.INSTANCE, "area_selections");

        listWidget.setListEntryWidgetFixedHeight(18);
        listWidget.setParentScreen(this.getParent());
        listWidget.setFileFilter(FileUtils.JSON_FILEFILTER);
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);
        listWidget.getEntrySelectionHandler().setAllowSelection(true);
        listWidget.setRootDirectoryDisplayName(StringUtils.translate("litematica.label.area_selection_browser.selections"));
        listWidget.setDataListEntryWidgetFactory((d, cd) -> new AreaSelectionEntryWidget(d, cd, listWidget, null, this.selectionManager));

        return listWidget;
    }

    protected void updateCurrentSelectionLabel()
    {
        String currentSelection = this.selectionManager.getCurrentNormalSelectionId();

        if (currentSelection != null)
        {
            int len = DataManager.getAreaSelectionsBaseDirectory().toAbsolutePath().toString().length();

            if (currentSelection.length() > len + 1)
            {
                currentSelection = FileNameUtils.getFileNameWithoutExtension(currentSelection.substring(len + 1));
                String key = "litematica.label.area_selection_browser.current_selection";
                this.currentSelectionLabel.setLabelText(key, currentSelection);
            }
        }
        else
        {
            this.currentSelectionLabel.clearText();
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
        this.selectionManager.createNewSelection(dir, name);
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

        if (placement != null && this.selectionManager.createSelectionFromPlacement(dir, placement, name))
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
        DataManager.getSelectionManager().setCurrentSelection(null);
        this.updateCurrentSelectionLabel();
        this.getListWidget().updateEntryWidgetStates();
    }

    protected void onSelectionChange(@Nullable BaseFileBrowserWidget.DirectoryEntry entry)
    {
        if (entry == null)
        {
            this.selectionManager.setCurrentSelection(null);
        }
        else if (entry.getType() == BaseFileBrowserWidget.DirectoryEntryType.FILE &&
                 FileType.fromFileName(entry.getFullPath()) == FileType.JSON)
        {
            String selectionId = entry.getFullPath().toString();

            if (selectionId.equals(this.selectionManager.getCurrentNormalSelectionId()))
            {
                this.selectionManager.setCurrentSelection(null);
            }
            else
            {
                this.selectionManager.setCurrentSelection(selectionId);
            }
        }

        this.updateCurrentSelectionLabel();
        this.getListWidget().updateEntryWidgetStates();
    }
}
