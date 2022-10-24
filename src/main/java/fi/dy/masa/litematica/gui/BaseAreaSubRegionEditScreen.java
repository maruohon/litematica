package fi.dy.masa.litematica.gui;

import malilib.gui.BaseScreen;
import malilib.gui.widget.BaseTextFieldWidget;
import malilib.gui.widget.BlockPosEditWidget;
import malilib.gui.widget.CheckBoxWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;

public abstract class BaseAreaSubRegionEditScreen extends BaseScreen
{
    protected final AreaSelection selection;
    protected final LabelWidget subRegionNameLabel;
    protected final BaseTextFieldWidget subRegionNameTextField;
    protected final BlockPosEditWidget corner1EditWidget;
    protected final BlockPosEditWidget corner2EditWidget;
    protected final CheckBoxWidget corner1Checkbox;
    protected final CheckBoxWidget corner2Checkbox;
    protected final GenericButton setSubRegionNameButton;

    public BaseAreaSubRegionEditScreen(AreaSelection selection)
    {
        this.selection = selection;

        this.subRegionNameLabel = new LabelWidget("litematica.label.area_editor.sub_region_name");
        this.subRegionNameTextField = new BaseTextFieldWidget(200, 16, selection.getCurrentSubRegionBoxName());
        this.setSubRegionNameButton = GenericButton.create(18, "litematica.button.misc.set", this::renameSubRegion);

        String hover = "litematica.hover.checkmark.area_editor.select_this_element";
        this.corner1Checkbox = new CheckBoxWidget("litematica.checkmark.area_editor.corner1", hover, this::isCorner1Selected, this::setCorner1Selected);
        this.corner2Checkbox = new CheckBoxWidget("litematica.checkmark.area_editor.corner2", hover, this::isCorner2Selected, this::setCorner2Selected);

        Box box = selection.getSelectedSubRegionBox();
        this.corner1EditWidget = new BlockPosEditWidget(90, 80, 2, true, box.getPos1(), box::setPos1);
        this.corner2EditWidget = new BlockPosEditWidget(90, 80, 2, true, box.getPos2(), box::setPos2);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.subRegionNameLabel);
        this.addWidget(this.subRegionNameTextField);
        this.addWidget(this.setSubRegionNameButton);

        this.addWidget(this.corner1Checkbox);
        this.addWidget(this.corner2Checkbox);

        this.addWidget(this.corner1EditWidget);
        this.addWidget(this.corner2EditWidget);
    }

    protected boolean isCorner1Selected()
    {
        return this.selection.getSelectedSubRegionBox().getSelectedCorner() == PositionUtils.Corner.CORNER_1;
    }

    protected boolean isCorner2Selected()
    {
        return this.selection.getSelectedSubRegionBox().getSelectedCorner() == PositionUtils.Corner.CORNER_2;
    }

    protected void setCorner1Selected(boolean value)
    {
        this.updateCornerSelection(value ? PositionUtils.Corner.CORNER_1 : PositionUtils.Corner.NONE);
    }

    protected void setCorner2Selected(boolean value)
    {
        this.updateCornerSelection(value ? PositionUtils.Corner.CORNER_2 : PositionUtils.Corner.NONE);
    }

    protected void updateCornerSelection(PositionUtils.Corner corner)
    {
        this.selection.setOriginSelected(false);
        this.selection.getSelectedSubRegionBox().setSelectedCorner(corner);
        this.updateCheckboxWidgets();
    }

    protected void updateCheckboxWidgets()
    {
        this.corner1Checkbox.updateWidgetState();
        this.corner2Checkbox.updateWidgetState();
    }

    protected void renameSubRegion()
    {
        String oldName = this.selection.getCurrentSubRegionBoxName();
        String newName = this.subRegionNameTextField.getText();
        this.selection.renameSubRegionBox(oldName, newName);
    }
}
