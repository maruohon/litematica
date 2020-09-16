package fi.dy.masa.litematica.gui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetListSelectionSubRegions;
import fi.dy.masa.litematica.gui.widgets.WidgetSelectionSubRegion;
import fi.dy.masa.litematica.materials.MaterialListAreaAnalyzer;
import fi.dy.masa.litematica.schematic.util.SchematicUtils;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.CornerSelectionMode;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.malilib.config.option.HotkeyConfig;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.TextInputScreen;
import fi.dy.masa.malilib.gui.widget.BaseTextFieldWidget;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.gui.widget.IntegerTextFieldWidget;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.ButtonActionListener;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.OnOffButton;
import fi.dy.masa.malilib.gui.widget.list.entry.SelectionListener;
import fi.dy.masa.malilib.render.message.MessageType;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.consumer.StringConsumer;

public class GuiAreaSelectionEditorNormal extends BaseListScreen<String, WidgetSelectionSubRegion, WidgetListSelectionSubRegions>
                                          implements SelectionListener<String>
{
    protected final AreaSelection selection;
    protected BaseTextFieldWidget textFieldSelectionName;
    protected CheckBoxWidget checkBoxOrigin;
    protected CheckBoxWidget checkBoxCorner1;
    protected CheckBoxWidget checkBoxCorner2;
    protected int xNext;
    protected int yNext;
    protected int xOrigin;
    @Nullable protected String selectionId;

    public GuiAreaSelectionEditorNormal(AreaSelection selection)
    {
        super(8, 116, 20, 146);

        this.selection = selection;
        this.selectionId = DataManager.getSelectionManager().getCurrentSelectionId();
        this.useTitleHierarchy = false;

        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            this.title = StringUtils.translate("litematica.gui.title.area_editor_normal_schematic_projects");
        }
        else
        {
            this.title = StringUtils.translate("litematica.gui.title.area_editor_normal");
        }
    }

    public void setSelectionId(@Nullable String selectionId)
    {
        this.selectionId = selectionId;
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
            this.addLabel(20, 30, 0xFFFFAA00, StringUtils.translate("litematica.error.area_editor.no_selection"));
        }
    }

    protected void createSelectionEditFields()
    {
        int xLeft = 12;
        int x = xLeft - 2;
        int y = 24;

        x += this.createButton(x, y, -1, ButtonListener.Type.CHANGE_SELECTION_MODE) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.CHANGE_CORNER_MODE) + 4;
        this.xOrigin = x;

        x = xLeft;
        y += 23;

        this.addLabel(x, y, 0xFFFFFFFF, StringUtils.translate("litematica.gui.label.area_editor.selection_name"));
        y += 10;

        int width = 202;
        this.textFieldSelectionName = new BaseTextFieldWidget(x, y + 1, width, 18, this.selection.getName());
        this.textFieldSelectionName.setFocused(true);
        this.addWidget(this.textFieldSelectionName);
        x += width + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.SET_SELECTION_NAME) + 10;
        y += 20;

        this.yNext = y;
    }

    protected int addSubRegionFields(int x, int y)
    {
        int width = 68;
        int xSave = 10;
        int ySave = y + 4;

        xSave += this.createButton(xSave, ySave, -1, ButtonListener.Type.CREATE_SUB_REGION) + 4;

        boolean currentlyOn = this.selection.getExplicitOrigin() != null;
        xSave += this.createButtonOnOff(xSave, ySave, -1, currentlyOn, ButtonListener.Type.TOGGLE_ORIGIN_ENABLED) + 4;
        xSave += this.createButton(xSave, ySave, -1, ButtonListener.Type.CREATE_SCHEMATIC) + 4;

        // Manual Origin defined
        if (this.selection.getExplicitOrigin() != null)
        {
            x = Math.max(xSave, this.xOrigin);
            this.createCoordinateInputs(x, 5, width, Corner.NONE);
        }

        x = 12;
        y = this.getListY() - 12;
        String str = String.valueOf(this.selection.getAllSubRegionNames().size());
        x += this.addLabel(x, y + 3, 0xFFFFFFFF, BaseScreen.TXT_BOLD + StringUtils.translate("litematica.gui.label.area_editor.sub_regions", str)).getWidth() + 16;

        this.addRenderingDisabledWarning(x, y);

        x = 12;
        y = this.height - 26;

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.AREA_SELECTION_BROWSER;
        String label = StringUtils.translate(type.getLabelKey());
        GenericButton button = new GenericButton(x, y, -1, 20, label, type.getIcon());

        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            button.setEnabled(false);
            button.addHoverString("litematica.gui.button.hover.schematic_projects.area_browser_disabled_currently_in_projects_mode");
        }

        x += this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent())).getWidth() + 4;

        this.createButton(x, y, -1, ButtonListener.Type.ANALYZE_AREA);

        type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = StringUtils.translate(type.getLabelKey());
        int buttonWidth = this.getStringWidth(label) + 10;
        x = this.width - buttonWidth - 10;
        button = new GenericButton(x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        return y;
    }

    protected void addRenderingDisabledWarning(int x, int y)
    {
        if (Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING.getBooleanValue() == false)
        {
            HotkeyConfig hotkey = Hotkeys.TOGGLE_AREA_SELECTION_RENDERING;
            String configName = Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING.getName();
            String hotkeyName = hotkey.getName();
            String hotkeyVal = hotkey.getKeyBind().getKeysDisplayString();
            String str = StringUtils.translate("litematica.warning.area_editor.area_rendering_disabled", configName, hotkeyName, hotkeyVal);
            List<String> lines = new ArrayList<>();
            int maxLineLength = this.width - x - 20;
            StringUtils.splitTextToLines(lines, str, maxLineLength);
            this.addLabel(x, y, 0xFFFFAA00, lines);
        }
    }

    protected void renameSubRegion()
    {
    }

    protected void createOrigin()
    {
        BlockPos origin = new BlockPos(this.mc.player);
        this.selection.setExplicitOrigin(origin);
    }

    protected int createCoordinateInputs(int x, int y, int width, Corner corner)
    {
        String label = "";
        CheckBoxWidget widget = null;

        switch (corner)
        {
            case CORNER_1:
                label = StringUtils.translate("litematica.gui.label.area_editor.corner_1");
                widget = new CheckBoxWidget(x, y + 3, LitematicaIcons.CHECKBOX_UNSELECTED, LitematicaIcons.CHECKBOX_SELECTED, label);
                this.checkBoxCorner1 = widget;
                break;
            case CORNER_2:
                label = StringUtils.translate("litematica.gui.label.area_editor.corner_2");
                widget = new CheckBoxWidget(x, y + 3, LitematicaIcons.CHECKBOX_UNSELECTED, LitematicaIcons.CHECKBOX_SELECTED, label);
                this.checkBoxCorner2 = widget;
                break;
            case NONE:
                label = StringUtils.translate("litematica.gui.label.area_editor.origin");
                widget = new CheckBoxWidget(x, y + 3, LitematicaIcons.CHECKBOX_UNSELECTED, LitematicaIcons.CHECKBOX_SELECTED, label);
                this.checkBoxOrigin = widget;
                break;
        }

        if (widget != null)
        {
            widget.setListener(new CheckBoxListener(corner, this));
            this.addWidget(widget);
        }
        y += 14;

        this.createCoordinateInput(x, y, width, CoordinateType.X, corner);
        y += 20;

        this.createCoordinateInput(x, y, width, CoordinateType.Y, corner);
        y += 20;

        this.createCoordinateInput(x, y, width, CoordinateType.Z, corner);
        y += 22;

        this.createButton(x + 10, y, -1, corner, ButtonListener.Type.MOVE_TO_PLAYER);
        y += 22;

        return y;
    }

    protected void createCoordinateInput(int x, int y, int width, CoordinateType coordType, Corner corner)
    {
        String label = coordType.name() + ":";
        this.addLabel(x, y + 6, 0xFFFFFFFF, label);
        int offset = 12;

        y += 2;
        BlockPos pos = corner == Corner.NONE ? this.selection.getEffectiveOrigin() : this.getBox().getPosition(corner);
        int value = 0;
        ButtonListener.Type type = null;

        switch (coordType)
        {
            case X:
                value = pos.getX();
                type = ButtonListener.Type.NUDGE_COORD_X;
                break;
            case Y:
                value = pos.getY();
                type = ButtonListener.Type.NUDGE_COORD_Y;
                break;
            case Z:
                value = pos.getZ();
                type = ButtonListener.Type.NUDGE_COORD_Z;
                break;
        }

        IntegerTextFieldWidget textField = new IntegerTextFieldWidget(x + offset, y, width, 16, value);
        textField.setUpdateListenerAlways(true);
        this.addTextField(textField, (newText) -> this.updatePosition(textField.getText(), corner, coordType));

        this.createCoordinateButton(x + offset + width + 4, y, corner, coordType, type);
    }

    protected int createButtonOnOff(int x, int y, int width, boolean isCurrentlyOn, ButtonListener.Type type)
    {
        OnOffButton button = new OnOffButton(x, y, width, false, type.getTranslationKey(), isCurrentlyOn);
        this.addButton(button, new ButtonListener(type, null, null, this));
        return button.getWidth();
    }

    protected int createButton(int x, int y, int width, ButtonListener.Type type)
    {
        return this.createButton(x, y, width, null, type);
    }

    protected int createButton(int x, int y, int width, @Nullable Corner corner, ButtonListener.Type type)
    {
        String label;
        String hover = null;
        boolean projectsMode = DataManager.getSchematicProjectsManager().hasProjectOpen();

        if (type == ButtonListener.Type.CHANGE_SELECTION_MODE)
        {
            SelectionMode mode = DataManager.getSelectionManager().getSelectionMode();
            label = type.getDisplayName(mode.getDisplayName());
        }
        else if (type == ButtonListener.Type.CHANGE_CORNER_MODE)
        {
            CornerSelectionMode mode = Configs.Generic.SELECTION_CORNERS_MODE.getOptionListValue();
            label = type.getDisplayName(mode.getDisplayName());

            if (mode == CornerSelectionMode.EXPAND)
            {
                hover = "litematica.gui.button.hover.area_editor.corner_mode.expand";
            }
            else
            {
                hover = "litematica.gui.button.hover.area_editor.corner_mode.corners";
            }
        }
        else if (type == ButtonListener.Type.CREATE_SCHEMATIC && projectsMode)
        {
            label = StringUtils.translate("litematica.gui.button.save_new_schematic_version");
        }
        else
        {
            label = type.getDisplayName();
        }

        if (type == ButtonListener.Type.CREATE_SCHEMATIC && projectsMode == false)
        {
            hover = "litematica.gui.button.hover.area_editor.shift_for_in_memory";
        }
        else if (type == ButtonListener.Type.ANALYZE_AREA)
        {
            hover = "litematica.gui.button.hover.area_editor.analyze_area";
        }

        GenericButton button = new GenericButton(x, y, width, 20, label);
        button.addHoverString(hover);

        this.addButton(button, new ButtonListener(type, corner, null, this));

        return button.getWidth();
    }

    protected void createCoordinateButton(int x, int y, Corner corner, CoordinateType coordType, ButtonListener.Type type)
    {
        String hover = StringUtils.translate("litematica.gui.button.hover.plus_minus_tip_ctrl_alt_shift");
        GenericButton button = new GenericButton(x, y, LitematicaIcons.BUTTON_PLUS_MINUS_16, hover);
        ButtonListener listener = new ButtonListener(type, corner, coordType, this);
        this.addButton(button, listener);
    }

    protected void updateCheckBoxes()
    {
        if (this.checkBoxOrigin != null)
        {
            this.checkBoxOrigin.setChecked(this.selection.isOriginSelected(), false);
        }

        if (this.checkBoxCorner1 != null)
        {
            boolean checked = this.selection.getSelectedSubRegionBox() != null && this.selection.getSelectedSubRegionBox().getSelectedCorner() == Corner.CORNER_1;
            this.checkBoxCorner1.setChecked(checked, false);
        }

        if (this.checkBoxCorner2 != null)
        {
            boolean checked = this.selection.getSelectedSubRegionBox() != null && this.selection.getSelectedSubRegionBox().getSelectedCorner() == Corner.CORNER_2;
            this.checkBoxCorner2.setChecked(checked, false);
        }
    }

    @Nullable
    protected SelectionBox getBox()
    {
        return null;
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
        int oldValue = 0;

        if (corner == Corner.NONE)
        {
            oldValue = PositionUtils.getCoordinate(this.selection.getEffectiveOrigin(), type);
        }
        else if (this.getBox() != null)
        {
            oldValue = this.getBox().getCoordinate(corner, type);
        }

        this.selection.setCoordinate(this.getBox(), corner, type, oldValue + amount);
    }

    protected void renameSelection()
    {
        String newName = this.textFieldSelectionName.getText();

        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            SelectionManager.renameSubRegionBoxIfSingle(this.selection, newName);
            this.selection.setName(newName);
        }
        else
        {
            this.renameSelection(newName);
        }
    }

    protected void renameSelection(String newName)
    {
        if (this.selectionId != null)
        {
            DataManager.getSelectionManager().renameSelection(this.selectionId, newName, this);
            this.selectionId = DataManager.getSelectionManager().getCurrentSelectionId();
        }
    }

    protected static class ButtonListener implements ButtonActionListener
    {
        private final GuiAreaSelectionEditorNormal parent;
        private final Type type;
        @Nullable private final Corner corner;
        @Nullable private final CoordinateType coordinateType;

        public ButtonListener(Type type, @Nullable Corner corner, @Nullable CoordinateType coordinateType, GuiAreaSelectionEditorNormal parent)
        {
            this.type = type;
            this.corner = corner;
            this.coordinateType = coordinateType;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            int amount = mouseButton == 1 ? -1 : 1;
            if (BaseScreen.isCtrlDown()) { amount *= 100; }
            if (BaseScreen.isShiftDown()) { amount *= 10; }
            if (BaseScreen.isAltDown()) { amount *= 5; }

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

                case CHANGE_SELECTION_MODE:
                    SelectionManager manager = DataManager.getSelectionManager();
                    SelectionMode newMode = manager.getSelectionMode().cycle(true);

                    if (newMode == SelectionMode.NORMAL && manager.hasNormalSelection() == false)
                    {
                        this.parent.addMessage(MessageType.WARNING, "litematica.error.area_editor.switch_mode.no_selection");
                    }
                    else
                    {
                        manager.switchSelectionMode();
                        manager.openEditGui(null);
                        return;
                    }

                    break;

                case CHANGE_CORNER_MODE:
                    Configs.Generic.SELECTION_CORNERS_MODE.cycleValue(false);
                    break;

                case CREATE_SCHEMATIC:
                    SchematicUtils.saveSchematic(BaseScreen.isShiftDown());
                    break;

                case ANALYZE_AREA:
                {
                    MaterialListAreaAnalyzer list = new MaterialListAreaAnalyzer(this.parent.selection);
                    DataManager.setMaterialList(list);
                    GuiMaterialList gui = new GuiMaterialList(list);
                    BaseScreen.openGui(gui);
                    list.reCreateMaterialList(); // This is after changing the GUI, so that the task message goes to the new GUI
                    return;
                }

                case CREATE_SUB_REGION:
                {
                    TextInputScreen gui = new TextInputScreen("litematica.gui.title.area_editor.sub_region_name", "", null, new SubRegionCreator(this.parent));
                    gui.setParent(this.parent);
                    BaseScreen.openPopupGui(gui);
                    break;
                }

                case SET_SELECTION_NAME:
                {
                    this.parent.renameSelection();
                    break;
                }

                case SET_BOX_NAME:
                {
                    this.parent.renameSubRegion();
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
                        this.parent.createOrigin();
                    }
                    else
                    {
                        this.parent.selection.setExplicitOrigin(null);
                    }
                    break;
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        public enum Type
        {
            SET_SELECTION_NAME      ("litematica.gui.button.area_editor.set_selection_name"),
            SET_BOX_NAME            ("litematica.gui.button.area_editor.set_box_name"),
            TOGGLE_ORIGIN_ENABLED   ("litematica.gui.button.area_editor.origin_enabled"),
            CREATE_SUB_REGION       ("litematica.gui.button.area_editor.create_sub_region"),
            CREATE_SCHEMATIC        ("litematica.gui.button.area_editor.create_schematic"),
            ANALYZE_AREA            ("litematica.gui.button.area_editor.analyze_area"),
            CHANGE_SELECTION_MODE   ("litematica.gui.button.area_editor.change_selection_mode"),
            CHANGE_CORNER_MODE      ("litematica.gui.button.area_editor.change_corner_mode"),
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
                return StringUtils.translate(this.translationKey, args);
            }
        }
    }

    protected static class SubRegionCreator implements StringConsumer
    {
        private final GuiAreaSelectionEditorNormal gui;

        private SubRegionCreator(GuiAreaSelectionEditorNormal gui)
        {
            this.gui = gui;
        }

        @Override
        public boolean consumeString(String string)
        {
            return DataManager.getSelectionManager().createNewSubRegionIfDoesntExist(string, this.gui.mc, this.gui);
        }
    }

    protected static class CheckBoxListener implements SelectionListener<CheckBoxWidget>
    {
        private final GuiAreaSelectionEditorNormal gui;
        private final Corner corner;

        public CheckBoxListener(Corner corner, GuiAreaSelectionEditorNormal gui)
        {
            this.gui = gui;
            this.corner = corner;
        }

        @Override
        public void onSelectionChange(CheckBoxWidget entry)
        {
            if (entry.isChecked())
            {
                // Origin
                if (this.corner == Corner.NONE)
                {
                    this.gui.selection.setOriginSelected(true);
                    this.gui.selection.clearCurrentSelectedCorner();
                }
                else
                {
                    this.gui.selection.setOriginSelected(false);

                    if (this.gui.getBox() != null)
                    {
                        this.gui.selection.setSelectedSubRegionBox(this.gui.getBox().getName());
                    }

                    this.gui.selection.setCurrentSelectedCorner(this.corner);
                }
            }
            else
            {
                // Origin
                if (this.corner == Corner.NONE)
                {
                    this.gui.selection.setOriginSelected(false);
                }
                else
                {
                    this.gui.selection.clearCurrentSelectedCorner();
                }
            }

            this.gui.updateCheckBoxes();
        }
    }

    @Override
    protected SelectionListener<String> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(String entry)
    {
        if (entry != null && entry.equals(this.selection.getCurrentSubRegionBoxName()))
        {
            this.selection.setSelectedSubRegionBox(null);
        }
        else
        {
            this.selection.setSelectedSubRegionBox(entry);
        }
    }

    @Override
    protected WidgetListSelectionSubRegions createListWidget(int listX, int listY)
    {
        return new WidgetListSelectionSubRegions(listX, listY,
                                                 this.getListWidth(), this.getListHeight(), this.selection, this);
    }
}
