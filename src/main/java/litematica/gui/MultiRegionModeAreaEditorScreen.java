package litematica.gui;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import malilib.gui.BaseListScreen;
import malilib.gui.BaseScreen;
import malilib.gui.TextInputScreen;
import malilib.gui.widget.BaseTextFieldWidget;
import malilib.gui.widget.BlockPosEditWidget;
import malilib.gui.widget.CheckBoxWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.gui.widget.list.DataListWidget;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.position.BlockPos;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.gui.util.LitematicaIcons;
import litematica.gui.widget.list.entry.AreaSubRegionEntryWidget;
import litematica.selection.AreaSelection;
import litematica.selection.AreaSelectionManager;

public class MultiRegionModeAreaEditorScreen extends BaseListScreen<DataListWidget<String>>
{
    protected final AreaSelection areaSelection;
    protected final LabelWidget areaNameLabel;
    protected final BaseTextFieldWidget areaNameTextField;
    protected final GenericButton areaAnalyzerButton;
    protected final GenericButton areaSelectionBrowserButton;
    protected final GenericButton cornersModeButton;
    protected final GenericButton createSubRegionButton;
    protected final GenericButton mainMenuButton;
    protected final GenericButton manualOriginButton;
    protected final GenericButton saveSchematicButton;
    protected final GenericButton selectionModeButton;
    protected final GenericButton setAreaNameButton;
    protected final BlockPosEditWidget originEditWidget;
    protected final CheckBoxWidget originCheckbox;

    public MultiRegionModeAreaEditorScreen(AreaSelection areaSelection)
    {
        super(10, 106, 20, 129);

        this.areaSelection = areaSelection;
        this.areaNameTextField = new BaseTextFieldWidget(200, 16, areaSelection.getName());

        this.areaNameLabel = new LabelWidget("litematica.label.area_editor.area_selection_name");

        this.selectionModeButton    = GenericButton.create(18, SimpleModeAreaEditorScreen::getSelectionModeButtonLabel, SimpleModeAreaEditorScreen::cycleSelectionMode);
        this.cornersModeButton      = GenericButton.create(18, SimpleModeAreaEditorScreen::getToolBehaviorModeButtonLabel, this::cycleCornersMode);
        this.createSubRegionButton  = GenericButton.create(18, "litematica.button.area_editor.create_selection_box", this::createSubRegion);
        this.manualOriginButton     = OnOffButton.onOff(18, "litematica.button.area_editor.manual_origin",
                                                        this.areaSelection::hasManualOrigin, this::toggleManualOrigin);

        this.setAreaNameButton      = GenericButton.create(18, "litematica.button.misc.set", this::renameAreaSelection);

        this.areaAnalyzerButton     = GenericButton.create(18, "litematica.button.area_editor.area_analyzer", this::openAreaAnalyzer);
        this.saveSchematicButton    = GenericButton.create(18, "litematica.button.area_editor.save_schematic", this::openSaveSchematicScreen);
        this.mainMenuButton         = GenericButton.create(18, "litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);
        this.areaSelectionBrowserButton = GenericButton.create(18, "litematica.button.change_menu.area_selection_browser", LitematicaIcons.AREA_SELECTION);

        this.areaSelectionBrowserButton.setActionListener(() -> openScreenWithParent(new AreaSelectionBrowserScreen()));

        String hover = "litematica.hover.checkmark.area_editor.select_this_element";
        this.originCheckbox = new CheckBoxWidget("litematica.checkmark.area_editor.origin", hover, areaSelection::isOriginSelected, areaSelection::setOriginSelected);
        this.originCheckbox.setListener(this::onOriginCheckboxClick);
        this.manualOriginButton.translateAndAddHoverString("litematica.hover.button.area_editor.manual_origin");

        BlockPos origin = this.areaSelection.hasManualOrigin() ? this.areaSelection.getManualOrigin() : this.areaSelection.getEffectiveOrigin();
        this.originEditWidget  = new BlockPosEditWidget(90, 80, 2, true, origin, areaSelection::setManualOrigin);

        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            this.setTitle("litematica.title.screen.area_editor.schematic_vcs");
        }
        else
        {
            this.setTitle("litematica.title.screen.area_editor.multi_region");
        }
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.areaAnalyzerButton);
        this.addWidget(this.areaSelectionBrowserButton);
        this.addWidget(this.cornersModeButton);
        this.addWidget(this.createSubRegionButton);
        this.addWidget(this.mainMenuButton);
        this.addWidget(this.manualOriginButton);
        this.addWidget(this.saveSchematicButton);
        this.addWidget(this.selectionModeButton);

        this.addWidget(this.areaNameLabel);
        this.addWidget(this.areaNameTextField);
        this.addWidget(this.setAreaNameButton);

        if (this.areaSelection.hasManualOrigin())
        {
            this.addWidget(this.originEditWidget);
            this.addWidget(this.originCheckbox);
        }
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 10;
        int y = this.y + 17;

        this.selectionModeButton.setPosition(x, y);
        this.cornersModeButton.setPosition(this.selectionModeButton.getRight() + 4, y);

        this.areaNameLabel.setPosition(x, this.selectionModeButton.getBottom() + 4);
        this.areaNameTextField.setPosition(x, this.areaNameLabel.getBottom());
        this.setAreaNameButton.setPosition(this.areaNameTextField.getRight() + 2, this.areaNameTextField.getY() - 1);

        y = this.areaNameTextField.getBottom() + 2;
        this.saveSchematicButton.setPosition(x, y);
        this.areaAnalyzerButton.setPosition(this.saveSchematicButton.getRight() + 4, y);

        y = this.areaAnalyzerButton.getBottom() + 1;
        this.createSubRegionButton.setPosition(x, y);
        this.manualOriginButton.setPosition(this.createSubRegionButton.getRight() + 4, y);

        this.originEditWidget.setPosition(this.cornersModeButton.getRight() + 8, this.y + 26);
        this.originCheckbox.setPosition(this.originEditWidget.getTextFieldStartX(), this.originEditWidget.getY() - 14);

        this.areaSelectionBrowserButton.setX(x);
        this.areaSelectionBrowserButton.setBottom(this.getBottom() - 3);
        this.mainMenuButton.setRight(this.getRight() - 10);
        this.mainMenuButton.setBottom(this.getBottom() - 3);
    }

    @Override
    protected DataListWidget<String> createListWidget()
    {
        Supplier<List<String>> supplier = this.areaSelection::getAllSelectionBoxNames;
        DataListWidget<String> listWidget = new DataListWidget<>(supplier, true);
        listWidget.setListEntryWidgetFixedHeight(18);
        listWidget.addDefaultSearchBar();
        listWidget.getEntrySelectionHandler().setSelectionListener(this::onSelectionChange);
        listWidget.getEntrySelectionHandler().setAllowSelection(true);
        listWidget.setDataListEntryWidgetFactory((d, cd) -> new AreaSubRegionEntryWidget(d, cd, this.areaSelection));
        return listWidget;
    }

    protected void onSelectionChange(@Nullable String regionName)
    {
        if (regionName != null && regionName.equals(this.areaSelection.getSelectedSelectionBoxName()))
        {
            regionName = null;
        }

        this.areaSelection.setSelectedSelectionBox(regionName);
    }

    protected void onOriginCheckboxClick(boolean value)
    {
        if (value)
        {
            this.areaSelection.clearCornerSelectionOfSelectedBox();
        }

        this.originCheckbox.updateWidgetState();
    }

    protected void createSubRegion()
    {
        String title = "litematica.title.screen.area_editor.create_selection_box";
        TextInputScreen screen = new TextInputScreen(title, "", this::createSubRegionByName);
        screen.setParent(this);
        BaseScreen.openPopupScreen(screen);
    }

    protected boolean createSubRegionByName(String name)
    {
        return DataManager.getAreaSelectionManager().createNewSubRegionIfNotExists(name);
    }

    protected void cycleCornersMode()
    {
        Configs.Generic.TOOL_SELECTION_MODE.cycleValue(false);
        this.cornersModeButton.updateButtonState();
        this.initScreen();
    }

    protected void renameAreaSelection()
    {
        String newName = this.areaNameTextField.getText();
        AreaSelectionManager.renameSubRegionBoxIfSingle(this.areaSelection, newName);
        this.areaSelection.setName(newName);
    }

    protected void toggleManualOrigin()
    {
        if (this.areaSelection.hasManualOrigin())
        {
            this.areaSelection.setManualOrigin(null);
        }
        else
        {
            this.areaSelection.setManualOrigin(EntityWrap.getCameraEntityBlockPos());
        }

        this.manualOriginButton.updateButtonState();
        this.reAddActiveWidgets();
        this.updateWidgetPositions();
    }

    protected void openAreaAnalyzer()
    {
    }

    protected void openSaveSchematicScreen()
    {
        BaseScreen.openScreen(new SaveSchematicFromAreaScreen(this.areaSelection));
    }
}
