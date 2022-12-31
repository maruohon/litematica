package litematica.gui;

import malilib.gui.BaseScreen;
import malilib.gui.widget.BaseTextFieldWidget;
import malilib.gui.widget.BlockPosEditWidget;
import malilib.gui.widget.CheckBoxWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import litematica.selection.AreaSelection;
import litematica.selection.BoxCorner;
import litematica.selection.SelectionBox;

public abstract class BaseAreaSubRegionEditScreen extends BaseScreen
{
    protected final AreaSelection areaSelection;
    protected final SelectionBox selectionBox;
    protected final LabelWidget selectionBoxNameLabel;
    protected final BaseTextFieldWidget selectionBoxNameTextField;
    protected final BlockPosEditWidget corner1EditWidget;
    protected final BlockPosEditWidget corner2EditWidget;
    protected final CheckBoxWidget corner1Checkbox;
    protected final CheckBoxWidget corner2Checkbox;
    protected final GenericButton setSelectionBoxNameButton;

    public BaseAreaSubRegionEditScreen(AreaSelection areaSelection, SelectionBox selectionBox)
    {
        this.areaSelection = areaSelection;
        this.selectionBox = selectionBox;

        this.selectionBoxNameLabel = new LabelWidget("litematica.label.area_editor.box_name");
        this.selectionBoxNameTextField = new BaseTextFieldWidget(200, 16, selectionBox.getName());
        this.setSelectionBoxNameButton = GenericButton.create(18, "litematica.button.misc.set", this::renameSelectionBox);

        String hover = "litematica.hover.checkmark.area_editor.select_this_element";
        this.corner1Checkbox = new CheckBoxWidget("litematica.checkmark.area_editor.corner1", hover, this::isCorner1Selected, this::setCorner1Selected);
        this.corner2Checkbox = new CheckBoxWidget("litematica.checkmark.area_editor.corner2", hover, this::isCorner2Selected, this::setCorner2Selected);

        this.corner1EditWidget = new BlockPosEditWidget(90, 80, 2, true, selectionBox.getCorner1(), selectionBox::setCorner1);
        this.corner2EditWidget = new BlockPosEditWidget(90, 80, 2, true, selectionBox.getCorner2(), selectionBox::setCorner2);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.selectionBoxNameLabel);
        this.addWidget(this.selectionBoxNameTextField);
        this.addWidget(this.setSelectionBoxNameButton);

        this.addWidget(this.corner1Checkbox);
        this.addWidget(this.corner2Checkbox);

        this.addWidget(this.corner1EditWidget);
        this.addWidget(this.corner2EditWidget);
    }

    protected boolean isCorner1Selected()
    {
        return this.selectionBox.isCornerSelected(BoxCorner.CORNER_1);
    }

    protected boolean isCorner2Selected()
    {
        return this.selectionBox.isCornerSelected(BoxCorner.CORNER_2);
    }

    protected void setCorner1Selected(boolean value)
    {
        this.updateCornerSelection(value ? BoxCorner.CORNER_1 : BoxCorner.NONE);
    }

    protected void setCorner2Selected(boolean value)
    {
        this.updateCornerSelection(value ? BoxCorner.CORNER_2 : BoxCorner.NONE);
    }

    protected void updateCornerSelection(BoxCorner corner)
    {
        this.areaSelection.setOriginSelected(false);
        this.selectionBox.setSelectedCorner(corner);
        this.updateCheckboxWidgets();
    }

    protected void updateCheckboxWidgets()
    {
        this.corner1Checkbox.updateWidgetState();
        this.corner2Checkbox.updateWidgetState();
    }

    protected void renameSelectionBox()
    {
        String oldName = this.selectionBox.getName();

        if (oldName != null)
        {
            String newName = this.selectionBoxNameTextField.getText();
            this.areaSelection.renameSelectionBox(oldName, newName);
        }
    }
}
