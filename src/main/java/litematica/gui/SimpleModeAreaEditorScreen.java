package litematica.gui;

import net.minecraft.util.math.BlockPos;

import malilib.gui.widget.BaseTextFieldWidget;
import malilib.gui.widget.BlockPosEditWidget;
import malilib.gui.widget.CheckBoxWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.util.StringUtils;
import malilib.util.game.wrap.EntityWrap;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.selection.AreaSelection;
import litematica.selection.AreaSelectionManager;
import litematica.selection.AreaSelectionType;

public class SimpleModeAreaEditorScreen extends BaseAreaSubRegionEditScreen
{
    protected final BaseTextFieldWidget areaNameTextField;
    protected final GenericButton areaAnalyzerButton;
    protected final GenericButton mainMenuButton;
    protected final GenericButton manualOriginButton;
    protected final GenericButton saveSchematicButton;
    protected final GenericButton selectionModeButton;
    protected final GenericButton setAreaNameButton;
    protected final GenericButton toolBehaviorModeButton;
    protected final LabelWidget areaNameLabel;
    protected final CheckBoxWidget originCheckbox;
    protected final BlockPosEditWidget originEditWidget;

    public SimpleModeAreaEditorScreen(AreaSelection selection)
    {
        super(selection, selection.getSelectedSelectionBox());

        this.areaNameLabel = new LabelWidget("litematica.label.area_editor.area_selection_name");
        this.areaNameTextField = new BaseTextFieldWidget(200, 16, selection.getName());

        this.selectionModeButton    = GenericButton.create(18, SimpleModeAreaEditorScreen::getSelectionModeButtonLabel, SimpleModeAreaEditorScreen::cycleSelectionMode);
        this.toolBehaviorModeButton = GenericButton.create(18, SimpleModeAreaEditorScreen::getToolBehaviorModeButtonLabel, this::cycleToolBehaviorMode);
        this.manualOriginButton     = OnOffButton.onOff(18, "litematica.button.area_editor.manual_origin",
                                                        this.areaSelection::hasManualOrigin, this::toggleManualOrigin);

        this.setAreaNameButton      = GenericButton.create(18, "litematica.button.misc.set", this::renameAreaSelection);
        this.areaAnalyzerButton     = GenericButton.create(18, "litematica.button.area_editor.area_analyzer", this::openAreaAnalyzer);
        this.saveSchematicButton    = GenericButton.create(18, "litematica.button.area_editor.save_schematic", this::openSaveSchematicScreen);
        this.mainMenuButton         = GenericButton.create(18, "litematica.button.change_menu.main_menu", MainMenuScreen::openMainMenuScreen);

        this.manualOriginButton.translateAndAddHoverString("litematica.hover.button.area_editor.manual_origin");
        this.originCheckbox = new CheckBoxWidget("litematica.checkmark.area_editor.origin",
                                                 "litematica.hover.checkmark.area_editor.select_this_element",
                                                 selection::isOriginSelected, selection::setOriginSelected);
        this.originCheckbox.setListener(this::onOriginCheckboxClick);

        BlockPos origin = this.areaSelection.hasManualOrigin() ? this.areaSelection.getManualOrigin() : this.areaSelection.getEffectiveOrigin();
        this.originEditWidget  = new BlockPosEditWidget(90, 80, 2, true, origin, selection::setManualOrigin);

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
        this.addWidget(this.toolBehaviorModeButton);
        this.addWidget(this.manualOriginButton);

        this.addWidget(this.areaNameLabel);
        this.addWidget(this.areaNameTextField);
        this.addWidget(this.setAreaNameButton);

        this.addWidget(this.saveSchematicButton);
        this.addWidget(this.areaAnalyzerButton);
        this.addWidget(this.mainMenuButton);
        
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
        this.toolBehaviorModeButton.setPosition(this.selectionModeButton.getRight() + 4, y);
        this.manualOriginButton.setPosition(this.toolBehaviorModeButton.getRight() + 4, y);

        y = this.selectionModeButton.getBottom() + 4;
        this.areaNameLabel.setPosition(x, y);
        this.areaNameTextField.setPosition(x, this.areaNameLabel.getBottom());
        this.selectionBoxNameLabel.setPosition(x, this.areaNameTextField.getBottom() + 4);
        this.selectionBoxNameTextField.setPosition(x, this.selectionBoxNameLabel.getBottom());

        this.setAreaNameButton.setPosition(this.areaNameTextField.getRight() + 2, this.areaNameTextField.getY() - 1);
        this.setSelectionBoxNameButton.setPosition(this.selectionBoxNameTextField.getRight() + 2, this.selectionBoxNameTextField.getY() - 1);

        y = this.selectionBoxNameTextField.getBottom() + 4;
        this.corner1EditWidget.setPosition(x, y + 14);
        this.corner2EditWidget.setPosition(this.corner1EditWidget.getRight() + 8, y + 14);
        this.originEditWidget.setPosition(this.corner2EditWidget.getRight() + 8, y + 14);
        this.corner1Checkbox.setPosition(this.corner1EditWidget.getTextFieldStartX(), y);
        this.corner2Checkbox.setPosition(this.corner2EditWidget.getTextFieldStartX(), y);
        this.originCheckbox.setPosition(this.originEditWidget.getTextFieldStartX(), y);

        x = this.setAreaNameButton.getRight() + 8;
        this.saveSchematicButton.setPosition(x, this.setAreaNameButton.getY());
        this.areaAnalyzerButton.setPosition(x, this.setSelectionBoxNameButton.getY());

        this.mainMenuButton.setRight(this.getRight() - 10);
        this.mainMenuButton.setBottom(this.getBottom() - 3);
    }

    protected static String getSelectionModeButtonLabel()
    {
        AreaSelectionType mode = DataManager.getAreaSelectionManager().getSelectionMode();
        return StringUtils.translate("litematica.button.area_editor.selection_mode", mode.getDisplayName());
    }

    protected static String getToolBehaviorModeButtonLabel()
    {
        String modeName = Configs.Generic.TOOL_SELECTION_MODE.getValue().getDisplayName();
        return StringUtils.translate("litematica.button.area_editor.corners_mode", modeName);
    }

    protected void onOriginCheckboxClick(boolean value)
    {
        if (value)
        {
            this.areaSelection.clearCornerSelectionOfSelectedBox();
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
        DataManager.getAreaSelectionManager().switchSelectionMode();
        DataManager.getAreaSelectionManager().openEditGui(null);
    }

    protected void cycleToolBehaviorMode()
    {
        Configs.Generic.TOOL_SELECTION_MODE.cycleValue(false);
        this.toolBehaviorModeButton.updateButtonState();
        this.initScreen();
    }

    protected void renameAreaSelection()
    {
        String newName = this.areaNameTextField.getText();
        AreaSelectionManager.renameSubRegionBoxIfSingle(this.areaSelection, newName);
        this.areaSelection.setName(newName);
        this.selectionBoxNameTextField.setText(this.areaSelection.getSelectedSelectionBoxName());
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
        openScreen(new SaveSchematicFromAreaScreen(this.areaSelection));
    }
}
