package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.widgets.WidgetListSelectionSubRegions;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.malilib.MaLiLib;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class GuiAreaSelectionEditorComplex extends GuiAreaSelectionEditorNormal
{
    protected static String defaultBoxName = "DefaultComplexBox";

    public GuiAreaSelectionEditorComplex(AreaSelection selection)
    {
        super(selection);

        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            this.title = StringUtils.translate("litematica.gui.title.area_editor_normal_schematic_projects");
        }
        else
        {
            this.title = StringUtils.translate("litematica.gui.title.area_editor_complex");
        }
    }

    @Override
    public void initGui()
    {
        super.initGui();

        if (this.selection != null)
        {
            this.createSelectionEditFields();
            this.addSubRegionFields(this.xOrigin, this.yNext);
            this.updateCheckBoxes();
        }
        else
        {
            this.addLabel(20, 30, 120, 12, 0xFFFFAA00, StringUtils.translate("litematica.error.area_editor.no_selection"));
        }
    }


    @Override
    protected int addSubRegionFields(int x, int y)
    {
        x = 12;
//        String label = StringUtils.translate("litematica.gui.label.area_editor.box_name");
//        this.addLabel(x, y, -1, 16, 0xFFFFFFFF, label);
//        y += 13;

//        boolean currentlyOn = this.selection.getExplicitOrigin() != null;
//        this.createButtonOnOff(this.xOrigin, 24, -1, currentlyOn, ButtonListener.Type.TOGGLE_ORIGIN_ENABLED);
//        x += this.createButton(x, y, -1, ButtonListener.Type.CREATE_SUB_REGION) + 4;
        int width = 202;
//        this.textFieldBoxName = new GuiTextFieldGeneric(x, y + 2, width, 16, this.textRenderer);
//        this.textFieldBoxName.setText(this.getBox().getName());
//        this.addTextField(this.textFieldBoxName, new TextFieldListenerDummy());
//        this.createButton(x + width + 4, y, -1, ButtonListener.Type.SET_BOX_NAME);
        y += 20;

        x = 12;
        width = 68;

        int nextY = 0;
        this.createCoordinateInputs(x, y, width, Corner.CORNER_1);
        x += width + 42;
        nextY = this.createCoordinateInputs(x, y, width, Corner.CORNER_2);
        this.createButton(x + 10, nextY, -1, ButtonListener.Type.ANALYZE_AREA);
        x += width + 42;

        // Manual Origin defined
//        if (this.selection.getExplicitOrigin() != null)
//        {
//            this.createCoordinateInputs(x, y, width, Corner.NONE);
//        }

        x = this.createButton(22, nextY, -1, ButtonListener.Type.CREATE_SCHEMATIC) + 26;

        x = this.createButton(22, nextY + 22, -1, ButtonListener.Type.TOGGLE_GENERATE_CIRCLE);
        nextY += 22;
        this.createButtonOnOff(22, nextY + 22, -1, circleMode, ButtonListener.Type.TOGGLE_CIRCLE_ENABLED);


//        this.createCoordinateInputs(x, y, width, Corner.CORNER_1);
//        this.createCoordinateInputs(x, y, width, Corner.CORNER_2);
//
//        this.createButton(22, y + 22, -1, ButtonListener.Type.TOGGLE_GENERATE_CIRCLE);
//        y += 22;
//        this.createButtonOnOff(22, y + 22, -1, this.circleMode, ButtonListener.Type.TOGGLE_CIRCLE_ENABLED);


        this.addRenderingDisabledWarning(250, 48);

        return y;
    }

    @Override
    protected Box getBox()
    {
        // 设置默认位置和名称
        Box selBox = this.selection.getSelectedSubRegionBox();
        this.selection.removeSelectedSubRegionBox();
        if (selBox != null) {
            selBox.setName(defaultBoxName);
        } else if (this.mc.player != null) {
            BlockPos pos = fi.dy.masa.malilib.util.PositionUtils.getEntityBlockPos(this.mc.player);
            selBox = new Box(pos, pos, defaultBoxName);
        } else {
            MaLiLib.logger.error("can not getSelectedSubRegionBox");
            selBox = new Box();
            selBox.setName(defaultBoxName);
        }
        this.selection.addSubRegionBox(selBox,false);
        this.selection.setSelectedSubRegionBox(defaultBoxName);
        return selBox;
    }

    @Override
    protected WidgetListSelectionSubRegions getListWidget()
    {
        return this.createListWidget(8, 116);
    }

    @Override
    protected void reCreateListWidget()
    {
//        super.createListWidget(8,116);
        // NO-OP
    }

    @Override
    protected WidgetListSelectionSubRegions createListWidget(int listX, int listY)
    {
        return new WidgetListSelectionSubRegions(listX, listY,
                this.getBrowserWidth(), this.getBrowserHeight(), this.selection, this);
    }
}
