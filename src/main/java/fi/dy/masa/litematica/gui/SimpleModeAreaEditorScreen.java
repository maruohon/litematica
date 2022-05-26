package fi.dy.masa.litematica.gui;

import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.BaseTextFieldWidget;
import fi.dy.masa.malilib.gui.widget.BlockPosEditWidget;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.gui.widget.LabelWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.OnOffButton;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.wrap.EntityWrap;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;

public class SimpleModeAreaEditorScreen extends BaseAreaSubRegionEditScreen
{
    protected final BaseTextFieldWidget areaNameTextField;
    protected final GenericButton areaAnalyzerButton;
    protected final GenericButton cornersModeButton;
    protected final GenericButton mainMenuButton;
    protected final GenericButton manualOriginButton;
    protected final GenericButton saveSchematicButton;
    protected final GenericButton selectionModeButton;
    protected final GenericButton setAreaNameButton;
    protected final LabelWidget areaNameLabel;
    protected final CheckBoxWidget originCheckbox;
    protected final BlockPosEditWidget originEditWidget;

    public SimpleModeAreaEditorScreen(AreaSelection selection)
    {
        super(selection);

        this.areaNameLabel = new LabelWidget("litematica.label.area_editor.area_selection_name");
        this.areaNameTextField = new BaseTextFieldWidget(200, 16, selection.getName());

        this.selectionModeButton    = GenericButton.create(18, SimpleModeAreaEditorScreen::getSelectionModeButtonLabel, SimpleModeAreaEditorScreen::cycleSelectionMode);
        this.cornersModeButton      = GenericButton.create(18, SimpleModeAreaEditorScreen::getCornersModeButtonLabel, this::cycleCornersMode);
        this.manualOriginButton     = OnOffButton.onOff(18, "litematica.button.area_editor.manual_origin",
                                                        this.selection::hasManualOrigin, this::toggleManualOrigin);

        this.setAreaNameButton      = GenericButton.create(18, "litematica.button.misc.set", this::renameAreaSelection);
        this.areaAnalyzerButton     = GenericButton.create(18, "litematica.button.area_editor.area_analyzer", this::openAreaAnalyzer);
        this.saveSchematicButton    = GenericButton.create(18, "litematica.button.area_editor.save_schematic", this::openSaveSchematicScreen);
        this.mainMenuButton         = GenericButton.create(18, "litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);

        this.manualOriginButton.translateAndAddHoverString("litematica.hover.button.area_editor.manual_origin");
        this.originCheckbox = new CheckBoxWidget("litematica.checkmark.area_editor.origin",
                                                 "litematica.hover.checkmark.area_editor.select_this_element",
                                                 selection::isOriginSelected, selection::setOriginSelected);
        this.originCheckbox.setListener(this::onOriginCheckboxClick);

        Box box = selection.getSelectedSubRegionBox();
        this.originEditWidget  = new BlockPosEditWidget(90, 80, 2, true, box.getPos1(), selection::setExplicitOrigin);

        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            this.setTitle("litematica.title.screen.area_editor.schematic_vcs");
        }
        else
        {
            this.setTitle("litematica.title.screen.area_editor.simple");
        }
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.selectionModeButton);
        this.addWidget(this.cornersModeButton);
        this.addWidget(this.manualOriginButton);

        this.addWidget(this.areaNameLabel);
        this.addWidget(this.areaNameTextField);
        this.addWidget(this.setAreaNameButton);

        this.addWidget(this.saveSchematicButton);
        this.addWidget(this.areaAnalyzerButton);
        this.addWidget(this.mainMenuButton);
        
        if (this.selection.hasManualOrigin())
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
        this.manualOriginButton.setPosition(this.cornersModeButton.getRight() + 4, y);

        y = this.selectionModeButton.getBottom() + 4;
        this.areaNameLabel.setPosition(x, y);
        this.areaNameTextField.setPosition(x, this.areaNameLabel.getBottom());
        this.subRegionNameLabel.setPosition(x, this.areaNameTextField.getBottom() + 4);
        this.subRegionNameTextField.setPosition(x, this.subRegionNameLabel.getBottom());

        this.setAreaNameButton.setPosition(this.areaNameTextField.getRight() + 2, this.areaNameTextField.getY() - 1);
        this.setSubRegionNameButton.setPosition(this.subRegionNameTextField.getRight() + 2, this.subRegionNameTextField.getY() - 1);

        y = this.subRegionNameTextField.getBottom() + 4;
        this.corner1EditWidget.setPosition(x, y + 14);
        this.corner2EditWidget.setPosition(this.corner1EditWidget.getRight() + 8, y + 14);
        this.originEditWidget.setPosition(this.corner2EditWidget.getRight() + 8, y + 14);
        this.corner1Checkbox.setPosition(this.corner1EditWidget.getTextFieldStartX(), y);
        this.corner2Checkbox.setPosition(this.corner2EditWidget.getTextFieldStartX(), y);
        this.originCheckbox.setPosition(this.originEditWidget.getTextFieldStartX(), y);

        x = this.setAreaNameButton.getRight() + 8;
        this.saveSchematicButton.setPosition(x, this.setAreaNameButton.getY());
        this.areaAnalyzerButton.setPosition(x, this.setSubRegionNameButton.getY());

        this.mainMenuButton.setRight(this.getRight() - 10);
        this.mainMenuButton.setBottom(this.getBottom() - 3);
    }

    protected static String getSelectionModeButtonLabel()
    {
        SelectionMode mode = DataManager.getSelectionManager().getSelectionMode();
        String key = mode == SelectionMode.SIMPLE ? "litematica.label.area_selection.mode.simple" :
                                                    "litematica.label.area_selection.mode.multi_region";
        return StringUtils.translate("litematica.button.area_editor.selection_mode", StringUtils.translate(key));
    }

    protected static String getCornersModeButtonLabel()
    {
        String modeName = Configs.Generic.SELECTION_CORNERS_MODE.getValue().getDisplayName();
        return StringUtils.translate("litematica.button.area_editor.corners_mode", modeName);
    }

    protected void onOriginCheckboxClick(boolean value)
    {
        if (value)
        {
            this.selection.clearCurrentSelectedCorner();
        }

        this.updateCheckboxWidgets();
    }

    @Override
    protected void updateCheckboxWidgets()
    {
        super.updateCheckboxWidgets();
        this.originCheckbox.updateWidgetState();
    }

    protected static void cycleSelectionMode()
    {
        DataManager.getSelectionManager().switchSelectionMode();
        DataManager.getSelectionManager().openEditGui(null);
    }

    protected void cycleCornersMode()
    {
        Configs.Generic.SELECTION_CORNERS_MODE.cycleValue(false);
        this.cornersModeButton.updateButtonState();
        this.initScreen();
    }

    protected void renameAreaSelection()
    {
        String newName = this.areaNameTextField.getText();
        SelectionManager.renameSubRegionBoxIfSingle(this.selection, newName);
        this.selection.setName(newName);
        this.subRegionNameTextField.setText(this.selection.getCurrentSubRegionBoxName());
    }

    protected void toggleManualOrigin()
    {
        if (this.selection.hasManualOrigin())
        {
            this.selection.setExplicitOrigin(null);
        }
        else
        {
            this.selection.setExplicitOrigin(EntityWrap.getCameraEntityBlockPos());
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
        SaveSchematicFromAreaScreen screen = new SaveSchematicFromAreaScreen(this.selection);
        BaseScreen.openScreen(screen);
    }
}
