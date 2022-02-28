package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.widget.BaseTextFieldWidget;
import fi.dy.masa.malilib.gui.widget.BlockPosEditWidget;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.gui.widget.LabelWidget;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.OnOffButton;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class SimpleModeAreaEditorScreen extends BaseScreen
{
    protected final AreaSelection selection;
    protected final BaseTextFieldWidget areaNameTextField;
    protected final BaseTextFieldWidget subRegionNameTextField;
    protected final GenericButton areaAnalyzerButton;
    protected final GenericButton cornersModeButton;
    protected final GenericButton mainMenuButton;
    protected final GenericButton manualOriginButton;
    protected final GenericButton saveSchematicButton;
    protected final GenericButton selectionModeButton;
    protected final GenericButton setAreaNameButton;
    protected final GenericButton setSubRegionNameButton;
    protected final LabelWidget areaNameLabel;
    protected final LabelWidget subRegionNameLabel;
    protected final CheckBoxWidget corner1Checkbox;
    protected final CheckBoxWidget corner2Checkbox;
    protected final CheckBoxWidget originCheckbox;
    protected final BlockPosEditWidget corner1EditWidget;
    protected final BlockPosEditWidget corner2EditWidget;
    protected final BlockPosEditWidget originEditWidget;

    public SimpleModeAreaEditorScreen(AreaSelection selection)
    {
        this.selection = selection;

        this.areaNameTextField = new BaseTextFieldWidget(200, 16, selection.getName());
        this.subRegionNameTextField = new BaseTextFieldWidget(200, 16, selection.getCurrentSubRegionBoxName());

        this.areaNameLabel = new LabelWidget("litematica.label.area_editor.area_selection_name");
        this.subRegionNameLabel = new LabelWidget("litematica.label.area_editor.sub_region_name");

        this.selectionModeButton    = GenericButton.create(this::getSelectionModeButtonLabel);
        this.cornersModeButton      = GenericButton.create(this::getCornersModeButtonLabel);
        this.manualOriginButton     = OnOffButton.onOff(20, "litematica.button.area_editor.manual_origin",
                                                        this.selection::hasManualOrigin, this::toggleManualOrigin);

        this.setAreaNameButton      = GenericButton.create(18, "litematica.button.misc.set", this::renameAreaSelection);
        this.setSubRegionNameButton = GenericButton.create(18, "litematica.button.misc.set", this::renameSubRegion);

        this.areaAnalyzerButton     = GenericButton.create("litematica.button.area_editor.area_analyzer", this::openAreaAnalyzer);
        this.saveSchematicButton    = GenericButton.create("litematica.button.area_editor.save_schematic", this::openSaveSchematicScreen);
        this.mainMenuButton         = GenericButton.create("litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);

        this.selectionModeButton.setActionListener(this::cycleSelectionMode);
        this.cornersModeButton.setActionListener(this::cycleCornersMode);

        String hover = "litematica.hover.checkmark.area_editor.select_this_element";
        this.corner1Checkbox = new CheckBoxWidget(DefaultIcons.CHECKMARK_OFF, DefaultIcons.CHECKMARK_ON,
                                                  "litematica.checkmark.area_editor.corner1", hover);
        this.corner2Checkbox = new CheckBoxWidget(DefaultIcons.CHECKMARK_OFF, DefaultIcons.CHECKMARK_ON,
                                                  "litematica.checkmark.area_editor.corner2", hover);
        this.originCheckbox = new CheckBoxWidget(DefaultIcons.CHECKMARK_OFF, DefaultIcons.CHECKMARK_ON,
                                                 "litematica.checkmark.area_editor.origin", hover);

        this.corner1Checkbox.setBooleanStorage(this::isCorner1Selected, this::setCorner1Selected);
        this.corner2Checkbox.setBooleanStorage(this::isCorner2Selected, this::setCorner2Selected);
        this.originCheckbox.setBooleanStorage(selection::isOriginSelected, selection::setOriginSelected);
        this.originCheckbox.setListener(this::onOriginCheckboxClick);

        Box box = selection.getSelectedSubRegionBox();
        this.corner1EditWidget = new BlockPosEditWidget(90, 80, 2, true, box.getPos1(), box::setPos1);
        this.corner2EditWidget = new BlockPosEditWidget(90, 80, 2, true, box.getPos2(), box::setPos2);
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
        this.addWidget(this.subRegionNameLabel);
        this.addWidget(this.areaNameTextField);
        this.addWidget(this.subRegionNameTextField);

        this.addWidget(this.setAreaNameButton);
        this.addWidget(this.setSubRegionNameButton);

        this.addWidget(this.saveSchematicButton);
        this.addWidget(this.areaAnalyzerButton);
        this.addWidget(this.mainMenuButton);
        
        this.addWidget(this.corner1Checkbox);
        this.addWidget(this.corner2Checkbox);

        this.addWidget(this.corner1EditWidget);
        this.addWidget(this.corner2EditWidget);

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

        int x = this.x + 12;
        int y = this.y + 24;

        this.selectionModeButton.setPosition(x, y);
        this.cornersModeButton.setPosition(this.selectionModeButton.getRight() + 4, y);
        this.manualOriginButton.setPosition(this.cornersModeButton.getRight() + 4, y);

        y += 28;
        this.areaNameLabel.setPosition(x, y);
        this.areaNameTextField.setPosition(x, this.areaNameLabel.getBottom() + 1);
        this.subRegionNameLabel.setPosition(x, this.areaNameTextField.getBottom() + 4);
        this.subRegionNameTextField.setPosition(x, this.subRegionNameLabel.getBottom() + 1);

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
        y = this.y + 52;
        this.saveSchematicButton.setPosition(x, y);
        this.areaAnalyzerButton.setPosition(x, y + 22);

        y = this.getBottom() - 24;
        this.mainMenuButton.setRight(this.getRight() - 4);
        this.mainMenuButton.setY(y);
    }

    protected String getSelectionModeButtonLabel()
    {
        String modeName = StringUtils.translate("litematica.label.area_selection.mode.simple");
        return StringUtils.translate("litematica.button.area_editor.selection_mode", modeName);
    }

    protected String getCornersModeButtonLabel()
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

        this.corner1Checkbox.updateWidgetState();
        this.corner2Checkbox.updateWidgetState();
    }

    protected void setCorner1Selected(boolean value)
    {
        Corner corner = value ? Corner.CORNER_1 : Corner.NONE;
        this.selection.setOriginSelected(false);
        this.selection.getSelectedSubRegionBox().setSelectedCorner(corner);
        this.corner2Checkbox.updateWidgetState();
        this.originCheckbox.updateWidgetState();
    }

    protected void setCorner2Selected(boolean value)
    {
        Corner corner = value ? Corner.CORNER_2 : Corner.NONE;
        this.selection.setOriginSelected(false);
        this.selection.getSelectedSubRegionBox().setSelectedCorner(corner);
        this.corner1Checkbox.updateWidgetState();
        this.originCheckbox.updateWidgetState();
    }

    protected boolean isCorner1Selected()
    {
        return this.selection.getSelectedSubRegionBox().getSelectedCorner() == PositionUtils.Corner.CORNER_1;
    }

    protected boolean isCorner2Selected()
    {
        return this.selection.getSelectedSubRegionBox().getSelectedCorner() == PositionUtils.Corner.CORNER_2;
    }

    protected void cycleSelectionMode()
    {
        DataManager.getSelectionManager().switchSelectionMode();
        DataManager.getSelectionManager().openAreaEditorScreenWithParent();
    }

    protected void cycleCornersMode()
    {
        Configs.Generic.SELECTION_CORNERS_MODE.cycleValue(false);
        this.cornersModeButton.updateButtonState();
        this.initScreen();
    }

    protected void renameSubRegion()
    {
        String oldName = this.selection.getCurrentSubRegionBoxName();
        String newName = this.subRegionNameTextField.getText();
        this.selection.renameSubRegionBox(oldName, newName);
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
            this.selection.setExplicitOrigin(EntityUtils.getCameraEntityBlockPos());
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
    }
}
