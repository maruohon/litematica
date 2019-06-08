package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.widgets.WidgetListSelectionSubRegions;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiAreaSelectionEditorSubRegion extends GuiAreaSelectionEditorSimple
{
    protected final Box box;

    public GuiAreaSelectionEditorSubRegion(AreaSelection selection, Box box)
    {
        super(selection);

        this.box = box;
        this.title = StringUtils.translate("litematica.gui.title.area_editor_sub_region");
    }

    @Override
    protected void createSelectionEditFields()
    {
        // NO-OP
    }

    @Override
    protected int addSubRegionFields(int x, int y)
    {
        x = 12;
        y = 24;
        String label = StringUtils.translate("litematica.gui.label.area_editor.box_name");
        this.addLabel(x, y, -1, 16, 0xFFFFFFFF, label);
        y += 13;

        int width = 202;
        this.textFieldBoxName = new GuiTextFieldGeneric(x, y + 2, width, 16, this.textRenderer);
        this.textFieldBoxName.setText(this.getBox().getName());
        this.addTextField(this.textFieldBoxName, new TextFieldListenerDummy());
        this.createButton(x + width + 4, y, -1, ButtonListener.Type.SET_BOX_NAME);
        y += 20;

        x = 12;
        width = 68;

        this.createCoordinateInputs(x, y, width, Corner.CORNER_1);
        x += width + 42;
        this.createCoordinateInputs(x, y, width, Corner.CORNER_2);
        x += width + 42;

        return y;
    }

    @Override
    @Nullable
    protected Box getBox()
    {
        return this.box;
    }

    @Override
    protected void renameSubRegion()
    {
        String oldName = this.box.getName();
        String newName = this.textFieldBoxName.getText();
        this.selection.renameSubRegionBox(oldName, newName);
    }

    @Override
    protected void createOrigin()
    {
    }

    @Override
    protected WidgetListSelectionSubRegions getListWidget()
    {
        return null;
    }

    @Override
    protected void reCreateListWidget()
    {
        // NO-OP
    }
}
