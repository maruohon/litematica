package fi.dy.masa.litematica.gui;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.GridSettings;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.IntBoxCoordType;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.BaseDialogScreen;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.OnOffButton;
import fi.dy.masa.malilib.gui.widget.button.ButtonActionListener;
import fi.dy.masa.malilib.listener.TextChangeListener;
import fi.dy.masa.malilib.gui.widget.BaseTextFieldWidget;
import fi.dy.masa.malilib.gui.widget.IntegerTextFieldWidget;
import fi.dy.masa.malilib.util.position.IntBoundingBox;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiPlacementGridSettings extends BaseScreen
{
    private final SchematicPlacementManager manager;
    private final SchematicPlacement placement;
    private final GridSettings cachedSettings = new GridSettings();
    private int repeatElementsWidth;

    public GuiPlacementGridSettings(SchematicPlacement placement, GuiScreen parent)
    {
        this.setParent(parent);

        this.manager = DataManager.getSchematicPlacementManager();
        this.placement = placement;
        GridSettings settings = placement.getGridSettings();

        this.cachedSettings.copyFrom(settings);
        this.title = StringUtils.translate("litematica.gui.title.schematic_placement_grid_settings", placement.getName());
        this.useTitleHierarchy = false;
        this.zLevel = 1f;

        this.setScreenWidthAndHeight(360, 200);
        this.centerOnScreen();

        this.setWorldAndResolution(this.mc, this.screenWidth, this.screenHeight);
    }

    @Override
    protected void initScreen()
    {
        this.clearElements();

        int x = this.x + 10;
        int y = this.y + 20;
        int width = 50;

        boolean on = this.placement.getGridSettings().isEnabled();
        OnOffButton buttonOnOff = new OnOffButton(x, y, -1, false, "litematica.gui.button.schematic_placement.grid_settings", on);
        this.addButton(buttonOnOff, (btn, mb) -> {
            this.placement.getGridSettings().toggleEnabled();
            this.updatePlacementManager();
            this.initScreen();
        });

        GenericButton button = new GenericButton(x + buttonOnOff.getWidth() + 4, y, -1, 20, "litematica.gui.button.schematic_placement.reset_grid_size");
        this.addButton(button, (btn, mb) -> {
            this.placement.getGridSettings().resetSize();
            this.updatePlacementManager();
            this.initScreen();
        });

        y += 30;
        this.addLabel(x, y + 5, 0xFFFFFFFF, "litematica.gui.label.placement_grid.grid_size");
        y += 14;
        this.addSizeInputElements(x, y     , width, CoordinateType.X);
        this.addSizeInputElements(x, y + 16, width, CoordinateType.Y);
        this.addSizeInputElements(x, y + 32, width, CoordinateType.Z);

        y += 60;
        this.addLabel(x, y + 5, 0xFFFFFFFF, "litematica.gui.label.placement_grid.repeat_count");
        y += 14;
        this.addRepeatInputElements(x, y     , width, IntBoxCoordType.MIN_X);
        this.addRepeatInputElements(x, y + 16, width, IntBoxCoordType.MIN_Y);
        this.addRepeatInputElements(x, y + 32, width, IntBoxCoordType.MIN_Z);

        x += this.repeatElementsWidth + 20;
        this.addRepeatInputElements(x, y     , width, IntBoxCoordType.MAX_X);
        this.addRepeatInputElements(x, y + 16, width, IntBoxCoordType.MAX_Y);
        this.addRepeatInputElements(x, y + 32, width, IntBoxCoordType.MAX_Z);
    }

    private void updatePlacementManager()
    {
        GridSettings currentSettings = this.placement.getGridSettings();

        if (this.cachedSettings.equals(currentSettings) == false)
        {
            this.manager.updateGridPlacementsFor(this.placement);
            this.cachedSettings.copyFrom(currentSettings);
        }
    }

    private String getRepeatTranslationKey(IntBoxCoordType type)
    {
        switch (type)
        {
            case MIN_X: return "litematica.gui.label.placement_grid_settings.min_x";
            case MIN_Y: return "litematica.gui.label.placement_grid_settings.min_y";
            case MIN_Z: return "litematica.gui.label.placement_grid_settings.min_z";
            case MAX_X: return "litematica.gui.label.placement_grid_settings.max_x";
            case MAX_Y: return "litematica.gui.label.placement_grid_settings.max_y";
            case MAX_Z: return "litematica.gui.label.placement_grid_settings.max_z";
        }

        return "";
    }

    private String getSizeTranslationKey(CoordinateType type)
    {
        switch (type)
        {
            case X: return "litematica.gui.label.placement_grid_settings.size_x";
            case Y: return "litematica.gui.label.placement_grid_settings.size_y";
            case Z: return "litematica.gui.label.placement_grid_settings.size_z";
        }

        return "";
    }

    private void addRepeatInputElements(int x, int y, int width, IntBoxCoordType type)
    {
        String label = StringUtils.translate(this.getRepeatTranslationKey(type)) + ":";
        this.addLabel(x, y + 5, 0xFFFFFFFF, label);
        int labelWidth = this.getStringWidth(label) + 4;

        IntBoundingBox repeat = this.placement.getGridSettings().getRepeatCounts();
        String text = String.valueOf(PositionUtils.getIntBoxValue(repeat, type));

        x += labelWidth;
        BaseTextFieldWidget textField = new BaseTextFieldWidget(x, y + 2, width, 14, text);
        textField.setTextValidator(new IntegerTextFieldWidget.IntValidator(0, Integer.MAX_VALUE));
        textField.setUpdateListenerAlways(true);
        this.addTextField(textField, new TextFieldListenerRepeat(type, this.placement, this));

        x += width + 4;
        String hover = StringUtils.translate("litematica.gui.button.hover.plus_minus_tip");
        GenericButton button = new GenericButton(x, y + 1, LitematicaIcons.BUTTON_PLUS_MINUS_16, hover);

        this.addButton(button, new ButtonListenerRepeat(type, this.placement, this));

        this.repeatElementsWidth = labelWidth + width + button.getWidth() + 6;
    }

    private void addSizeInputElements(int x, int y, int width, CoordinateType type)
    {
        String label = StringUtils.translate(this.getSizeTranslationKey(type)) + ":";
        this.addLabel(x, y + 5, 0xFFFFFFFF, label);
        int offset = this.getStringWidth(label) + 4;

        Vec3i size = this.placement.getGridSettings().getSize();
        String text = String.valueOf(PositionUtils.getCoordinate(size, type));

        x += offset;
        int defaultSize = PositionUtils.getCoordinate(this.placement.getGridSettings().getDefaultSize(), type);
        BaseTextFieldWidget textField = new BaseTextFieldWidget(x, y + 2, width, 14, text);
        textField.setTextValidator(new IntegerTextFieldWidget.IntValidator(defaultSize, Integer.MAX_VALUE));
        textField.setUpdateListenerAlways(true);
        this.addTextField(textField, new TextFieldListenerSize(type, this.placement, this));

        x += width + 4;
        String hover = StringUtils.translate("litematica.gui.button.hover.plus_minus_tip");
        GenericButton button = new GenericButton(x, y + 1, LitematicaIcons.BUTTON_PLUS_MINUS_16, hover);

        this.addButton(button, new ButtonListenerSize(type, this.placement, this));
    }

    private static class TextFieldListenerRepeat implements Consumer<String>
    {
        private final GuiPlacementGridSettings parent;
        private final SchematicPlacement placement;
        private final IntBoxCoordType type;

        public TextFieldListenerRepeat(IntBoxCoordType type, SchematicPlacement placement, GuiPlacementGridSettings parent)
        {
            this.parent = parent;
            this.placement = placement;
            this.type = type;
        }

        @Override
        public void accept(String newText)
        {
            try
            {
                int value = Math.max(0, Integer.parseInt(newText));
                IntBoundingBox old = this.placement.getGridSettings().getRepeatCounts();
                this.placement.getGridSettings().setRepeatCounts(PositionUtils.setIntBoxValue(old, this.type, value));
                this.parent.updatePlacementManager();
            }
            catch (NumberFormatException ignore) {}
        }
    }

    private static class ButtonListenerRepeat implements ButtonActionListener
    {
        private final GuiPlacementGridSettings parent;
        private final SchematicPlacement placement;
        private final IntBoxCoordType type;

        public ButtonListenerRepeat(IntBoxCoordType type, SchematicPlacement placement, GuiPlacementGridSettings parent)
        {
            this.parent = parent;
            this.placement = placement;
            this.type = type;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            int amount = mouseButton == 1 ? -1 : 1;
            if (BaseScreen.isShiftDown()) { amount *= 8; }
            if (BaseScreen.isAltDown()) { amount *= 4; }

            IntBoundingBox old = this.placement.getGridSettings().getRepeatCounts();
            int newValue = Math.max(0, PositionUtils.getIntBoxValue(old, this.type) + amount);
            this.placement.getGridSettings().setRepeatCounts(PositionUtils.setIntBoxValue(old, this.type, newValue));
            this.parent.updatePlacementManager();

            this.parent.initGui(); // Re-create buttons/text fields
        }
    }

    private static class TextFieldListenerSize implements Consumer<String>
    {
        private final GuiPlacementGridSettings parent;
        private final SchematicPlacement placement;
        private final CoordinateType type;

        public TextFieldListenerSize(CoordinateType type, SchematicPlacement placement, GuiPlacementGridSettings parent)
        {
            this.parent = parent;
            this.placement = placement;
            this.type = type;
        }

        @Override
        public void accept(String newText)
        {
            try
            {
                int value = Math.max(0, Integer.parseInt(newText));
                this.placement.getGridSettings().setSize(this.type, value);
                this.parent.updatePlacementManager();
            }
            catch (NumberFormatException ignore) {}
        }
    }

    private static class ButtonListenerSize implements ButtonActionListener
    {
        private final GuiPlacementGridSettings parent;
        private final SchematicPlacement placement;
        private final CoordinateType type;

        public ButtonListenerSize(CoordinateType type, SchematicPlacement placement, GuiPlacementGridSettings parent)
        {
            this.parent = parent;
            this.placement = placement;
            this.type = type;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            int amount = mouseButton == 1 ? -1 : 1;
            if (BaseScreen.isShiftDown()) { amount *= 8; }
            if (BaseScreen.isAltDown()) { amount *= 4; }

            this.placement.getGridSettings().modifySize(this.type, amount);
            this.parent.updatePlacementManager();

            this.parent.initGui(); // Re-create buttons/text fields
        }
    }
}
