package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.button.ButtonOnOff;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.CoordinateType;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class GuiAreaSelectionEditorSimple extends GuiBase
{
    protected final AreaSelection selection;
    protected GuiTextFieldGeneric textFieldSelectionName;
    protected GuiTextFieldGeneric textFieldBoxName;

    public GuiAreaSelectionEditorSimple()
    {
        this.selection = DataManager.getSimpleArea();
        this.title = I18n.format("litematica.gui.title.area_editor_simple");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 20;
        int y = 24;

        int width = 202;
        this.createButton(x, y, -1, ButtonListener.Type.CHANGE_MODE);
        boolean currentlyOn = this.selection.getExplicitOrigin() != null;
        this.createButtonOnOff(x + width + 4, y, -1, currentlyOn, ButtonListener.Type.TOGGLE_ORIGIN_ENABLED);
        y += 20;

        String label = I18n.format("litematica.gui.label.area_editor.selection_name");
        this.addLabel(x, y, -1, 16, 0xFFFFFFFF, label);
        y += 13;

        TextFieldListenerDummy listener = new TextFieldListenerDummy();
        this.textFieldSelectionName = new GuiTextFieldGeneric(0, this.mc.fontRenderer, x, y + 2, width, 16);
        this.textFieldSelectionName.setText(this.selection.getName());
        this.addTextField(this.textFieldSelectionName, listener);
        this.createButton(x + width + 4, y, -1, ButtonListener.Type.SET_SELECTION_NAME);
        y += 20;

        label = I18n.format("litematica.gui.label.area_editor.box_name");
        this.addLabel(x, y, -1, 16, 0xFFFFFFFF, label);
        y += 13;
        this.textFieldBoxName = new GuiTextFieldGeneric(0, this.mc.fontRenderer, x, y + 2, width, 16);
        this.textFieldBoxName.setText(this.getBox().getName());
        this.addTextField(this.textFieldBoxName, listener);
        this.createButton(x + width + 4, y, -1, ButtonListener.Type.SET_BOX_NAME);

        y += 20;
        x = 20;
        width = 68;

        this.createCoordinateInputs(x, y, width, Corner.CORNER_1);
        x += width + 42;
        this.createCoordinateInputs(x, y, width, Corner.CORNER_2);
        x += width + 42;

        // Manual Origin defined
        if (this.selection.getExplicitOrigin() != null)
        {
            this.createCoordinateInputs(x, y, width, Corner.NONE);
        }
    }

    protected void createCoordinateInputs(int x, int y, int width, Corner corner)
    {
        String label = "";

        switch (corner)
        {
            case CORNER_1: label = I18n.format("litematica.gui.label.area_editor.corner_1"); break;
            case CORNER_2: label = I18n.format("litematica.gui.label.area_editor.corner_2"); break;
            case NONE: label = I18n.format("litematica.gui.label.area_editor.origin"); break;
        }

        this.addLabel(x, y, -1, 16, 0xFFFFFFFF, label);
        y += 14;

        this.createCoordinateInput(x, y, width, CoordinateType.X, corner);
        y += 20;

        this.createCoordinateInput(x, y, width, CoordinateType.Y, corner);
        y += 20;

        this.createCoordinateInput(x, y, width, CoordinateType.Z, corner);
        y += 22;

        this.createButton(x + 10, y, -1, corner, ButtonListener.Type.MOVE_TO_PLAYER);
    }

    private void createCoordinateInput(int x, int y, int width, CoordinateType coordType, Corner corner)
    {
        String label = coordType.name() + ":";
        this.addLabel(x, y, 20, 20, 0xFFFFFFFF, label);
        int offset = 12;

        y += 2;
        BlockPos pos = corner == Corner.NONE ? this.selection.getEffectiveOrigin() : this.getBox().getPosition(corner);
        String text = "";
        ButtonListener.Type type = null;

        switch (coordType)
        {
            case X:
                text = String.valueOf(pos.getX());
                type = ButtonListener.Type.NUDGE_COORD_X;
                break;
            case Y:
                text = String.valueOf(pos.getY());
                type = ButtonListener.Type.NUDGE_COORD_Y;
                break;
            case Z:
                text = String.valueOf(pos.getZ());
                type = ButtonListener.Type.NUDGE_COORD_Z;
                break;
        }

        GuiTextFieldInteger textField = new GuiTextFieldInteger(0, x + offset, y, width, 16, this.mc.fontRenderer);
        TextFieldListener listener = new TextFieldListener(coordType, corner, this);
        textField.setText(text);
        this.addTextField(textField, listener);

        this.createCoordinateButton(x + offset + width + 4, y, corner, coordType, type);
    }

    private int createButtonOnOff(int x, int y, int width, boolean isCurrentlyOn, ButtonListener.Type type)
    {
        ButtonOnOff button = ButtonOnOff.create(x, y, width, false, type.getTranslationKey(), isCurrentlyOn);
        this.addButton(button, new ButtonListener(type, null, null, this));
        return button.getButtonWidth();
    }

    private void createButton(int x, int y, int width,ButtonListener.Type type)
    {
        this.createButton(x, y, width, null, type);
    }

    private void createButton(int x, int y, int width, @Nullable Corner corner, ButtonListener.Type type)
    {
        String label;

        if (type == ButtonListener.Type.CHANGE_MODE)
        {
            label = type.getDisplayName(SelectionMode.SIMPLE.getDisplayName());
        }
        else
        {
            label = type.getDisplayName();
        }

        if (width == -1)
        {
            width = this.mc.fontRenderer.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(0, x, y, width, 20, label);
        ButtonListener listener = new ButtonListener(type, corner, null, this);
        this.addButton(button, listener);
    }

    private void createCoordinateButton(int x, int y, Corner corner, CoordinateType coordType, ButtonListener.Type type)
    {
        String hover = I18n.format("litematica.gui.button.hover.plus_minus_tip_ctrl_alt_shift");
        ButtonGeneric button = new ButtonGeneric(0, x, y, Icons.BUTTON_PLUS_MINUS_16, hover);
        ButtonListener listener = new ButtonListener(type, corner, coordType, this);
        this.addButton(button, listener);
    }

    protected Box getBox()
    {
        return this.selection.getSelectedSubRegionBox();
    }

    protected void updatePosition(String numberString, Corner corner, CoordinateType type)
    {
        try
        {
            int value = Integer.parseInt(numberString);
            this.selection.setCoordinate(this.getBox(), corner, type, value);
        }
        catch (NumberFormatException e)
        {
        }
    }

    protected void moveCoordinate(int amount, Corner corner, CoordinateType type)
    {
        int oldValue;

        if (corner == Corner.NONE)
        {
            oldValue = PositionUtils.getCoordinate(this.selection.getEffectiveOrigin(), type);
        }
        else
        {
            oldValue = this.getBox().getCoordinate(corner, type);
        }

        this.selection.setCoordinate(this.getBox(), corner, type, oldValue + amount);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiAreaSelectionEditorSimple parent;
        private final Type type;
        @Nullable private final Corner corner;
        @Nullable private final CoordinateType coordinateType;

        public ButtonListener(Type type, @Nullable Corner corner, @Nullable CoordinateType coordinateType, GuiAreaSelectionEditorSimple parent)
        {
            this.type = type;
            this.corner = corner;
            this.coordinateType = coordinateType;
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            int amount = mouseButton == 1 ? -1 : 1;
            if (GuiScreen.isCtrlKeyDown()) { amount *= 100; }
            if (GuiScreen.isShiftKeyDown()) { amount *= 10; }
            if (GuiScreen.isAltKeyDown()) { amount *= 5; }

            this.parent.setNextMessageType(MessageType.ERROR);

            switch (this.type)
            {
                case NUDGE_COORD_X:
                    this.parent.moveCoordinate(amount, this.corner, this.coordinateType);
                    break;

                case NUDGE_COORD_Y:
                    this.parent.moveCoordinate(amount, this.corner, this.coordinateType);
                    break;

                case NUDGE_COORD_Z:
                    this.parent.moveCoordinate(amount, this.corner, this.coordinateType);
                    break;

                case CHANGE_MODE:
                    SelectionManager manager = DataManager.getSelectionManager();
                    manager.setMode(manager.getSelectionMode().cycle(true));
                    manager.openEditGui(null);
                    break;

                case SET_SELECTION_NAME:
                {
                    String oldSelectionName = this.parent.selection.getName();
                    String oldBoxName = this.parent.selection.getCurrentSubRegionBoxName();
                    String newSelectionName = this.parent.textFieldSelectionName.getText();
                    this.parent.selection.setName(newSelectionName);

                    if (oldSelectionName.equals(oldBoxName))
                    {
                        this.parent.selection.renameSubRegionBox(oldBoxName, newSelectionName);
                    }
                    break;
                }

                case SET_BOX_NAME:
                {
                    String oldName = this.parent.selection.getCurrentSubRegionBoxName();
                    String newName = this.parent.textFieldBoxName.getText();
                    this.parent.selection.renameSubRegionBox(oldName, newName);
                    break;
                }

                case MOVE_TO_PLAYER:
                    if (this.parent.mc.player != null)
                    {
                        BlockPos pos = new BlockPos(this.parent.mc.player);

                        if (this.corner == Corner.NONE)
                        {
                            this.parent.selection.setExplicitOrigin(pos);
                        }
                        else
                        {
                            this.parent.selection.setSelectedSubRegionCornerPos(pos, this.corner);
                        }
                    }
                    break;

                case TOGGLE_ORIGIN_ENABLED:
                    BlockPos origin = this.parent.selection.getExplicitOrigin();

                    if (origin == null)
                    {
                        BlockPos pos1 = this.parent.getBox().getPos1();
                        BlockPos pos2 = this.parent.getBox().getPos2();
                        origin = PositionUtils.getMinCorner(pos1, pos2);
                        this.parent.selection.setExplicitOrigin(origin);
                    }
                    else
                    {
                        this.parent.selection.setExplicitOrigin(null);
                    }
                    this.parent.initGui();
                    break;
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        public enum Type
        {
            SET_SELECTION_NAME      ("litematica.gui.button.area_editor.set_selection_name"),
            SET_BOX_NAME            ("litematica.gui.button.area_editor.set_box_name"),
            TOGGLE_ORIGIN_ENABLED   ("litematica.gui.button.area_editor.origin_enabled"),
            CHANGE_MODE             ("litematica.gui.button.area_editor.change_mode"),
            MOVE_TO_PLAYER          ("litematica.gui.button.move_to_player"),
            NUDGE_COORD_X           (""),
            NUDGE_COORD_Y           (""),
            NUDGE_COORD_Z           ("");

            private final String translationKey;
            @Nullable private final String hoverText;

            private Type(String translationKey)
            {
                this(translationKey, null);
            }

            private Type(String translationKey, @Nullable String hoverText)
            {
                this.translationKey = translationKey;
                this.hoverText = hoverText;
            }

            public String getTranslationKey()
            {
                return this.translationKey;
            }

            public String getDisplayName(Object... args)
            {
                return I18n.format(this.translationKey, args);
            }
        }
    }

    protected static class TextFieldListener implements ITextFieldListener<GuiTextField>
    {
        private final GuiAreaSelectionEditorSimple parent;
        private final CoordinateType type;
        private final Corner corner;

        public TextFieldListener(CoordinateType type, Corner corner, GuiAreaSelectionEditorSimple parent)
        {
            this.type = type;
            this.corner = corner;
            this.parent = parent;
        }

        @Override
        public boolean onGuiClosed(GuiTextField textField)
        {
            return this.onTextChange(textField);
        }

        @Override
        public boolean onTextChange(GuiTextField textField)
        {
            this.parent.updatePosition(textField.getText(), this.corner, this.type);
            return false;
        }
    }

    protected static class TextFieldListenerDummy implements ITextFieldListener<GuiTextField>
    {
        @Override
        public boolean onGuiClosed(GuiTextField textField)
        {
            return false;
        }

        @Override
        public boolean onTextChange(GuiTextField textField)
        {
            return false;
        }
    }
}
