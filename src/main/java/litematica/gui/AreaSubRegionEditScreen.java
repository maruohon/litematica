package litematica.gui;

import litematica.selection.AreaSelection;
import litematica.selection.SelectionBox;

public class AreaSubRegionEditScreen extends BaseAreaSubRegionEditScreen
{
    public AreaSubRegionEditScreen(AreaSelection selection, SelectionBox selectionBox)
    {
        super(selection, selectionBox);

        this.setTitle("litematica.title.screen.area_editor.selection_box", selectionBox.getName());
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 10;
        int y = this.y + 24;

        this.selectionBoxNameLabel.setPosition(x, y);
        this.selectionBoxNameTextField.setPosition(x, this.selectionBoxNameLabel.getBottom());
        this.setSelectionBoxNameButton.setPosition(this.selectionBoxNameTextField.getRight() + 2, this.selectionBoxNameTextField.getY() - 1);

        y = this.selectionBoxNameTextField.getBottom() + 4;
        this.corner1EditWidget.setPosition(x, y + 14);
        this.corner2EditWidget.setPosition(this.corner1EditWidget.getRight() + 8, y + 14);
        this.corner1Checkbox.setPosition(this.corner1EditWidget.getTextFieldStartX(), y);
        this.corner2Checkbox.setPosition(this.corner2EditWidget.getTextFieldStartX(), y);
    }
}
