package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.selection.AreaSelection;

public class AreaSubRegionEditScreen extends BaseAreaSubRegionEditScreen
{
    public AreaSubRegionEditScreen(AreaSelection selection)
    {
        super(selection);

        this.setTitle("litematica.title.screen.area_editor.sub_region");
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 10;
        int y = this.y + 24;

        this.subRegionNameLabel.setPosition(x, y);
        this.subRegionNameTextField.setPosition(x, this.subRegionNameLabel.getBottom());
        this.setSubRegionNameButton.setPosition(this.subRegionNameTextField.getRight() + 2, this.subRegionNameTextField.getY() - 1);

        y = this.subRegionNameTextField.getBottom() + 4;
        this.corner1EditWidget.setPosition(x, y + 14);
        this.corner2EditWidget.setPosition(this.corner1EditWidget.getRight() + 8, y + 14);
        this.corner1Checkbox.setPosition(this.corner1EditWidget.getTextFieldStartX(), y);
        this.corner2Checkbox.setPosition(this.corner2EditWidget.getTextFieldStartX(), y);
    }
}
