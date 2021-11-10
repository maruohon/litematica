package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.OnOffButton;
import fi.dy.masa.malilib.gui.widget.button.ButtonActionListener;
import fi.dy.masa.malilib.gui.widget.list.entry.SelectionListener;
import fi.dy.masa.malilib.listener.TextChangeListener;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.gui.widget.IntegerTextFieldWidget;
import fi.dy.masa.malilib.overlay.message.MessageOutput;
import fi.dy.masa.malilib.util.PositionUtils.CoordinateType;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSubRegionConfiguration extends BaseScreen
{
    private final SchematicPlacement schematicPlacement;
    private final SubRegionPlacement placement;
    private GenericButton buttonResetPlacement;

    public GuiSubRegionConfiguration(SchematicPlacement schematicPlacement, SubRegionPlacement placement)
    {
        this.schematicPlacement = schematicPlacement;
        this.placement = placement;
        this.title = StringUtils.translate("litematica.gui.title.configure_schematic_sub_region");
    }

    @Override
    protected void initScreen()
    {
        super.initScreen();

        int width = 120;
        int x = this.width - width - 10;
        int y = 32;

        String label = StringUtils.translate("litematica.gui.label.placement_sub.region_name", this.placement.getName());
        this.addLabel(20, y, 0xFFFFFFFF, label);

        y = 22;
        this.createButtonOnOff(x, y, width, this.placement.isEnabled(), ButtonListener.Type.TOGGLE_ENABLED);
        y += 21;

        this.createButtonOnOff(x, y, width, this.placement.ignoreEntities(), ButtonListener.Type.TOGGLE_ENTITIES);
        y += 22;

        label = StringUtils.translate("litematica.gui.label.placement_sub.region_position");
        this.addLabel(x, y, 0xFFFFFFFF, label);
        y += 10;
        x += 2;

        this.createCoordinateInput(x, y, 70, CoordinateType.X);
        this.createButton(x + 85, y + 1, -1, ButtonListener.Type.NUDGE_COORD_X);
        y += 18;

        this.createCoordinateInput(x, y, 70, CoordinateType.Y);
        this.createButton(x + 85, y + 1, -1, ButtonListener.Type.NUDGE_COORD_Y);
        y += 18;

        this.createCoordinateInput(x, y, 70, CoordinateType.Z);
        this.createButton(x + 85, y + 1, -1, ButtonListener.Type.NUDGE_COORD_Z);
        y += 21;
        x -= 2;

        this.createButton(x, y, width, ButtonListener.Type.MOVE_TO_PLAYER);
        y += 21;

        this.createButton(x, y, width, ButtonListener.Type.ROTATE);
        y += 21;

        this.createButton(x, y, width, ButtonListener.Type.MIRROR);
        y += 21;

        this.createButton(x, y, width, ButtonListener.Type.RESET_PLACEMENT);
        y += 21;

        this.createButton(x, y, width, ButtonListener.Type.SLICE_TYPE);

        y = this.height - 36;
        label = StringUtils.translate("litematica.gui.button.placement_sub.placement_configuration");
        int buttonWidth = this.getStringWidth(label) + 10;
        x = 10;
        GenericButton button = new GenericButton(x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListener(ButtonListener.Type.PLACEMENT_CONFIGURATION, this.schematicPlacement, this.placement, this));

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = StringUtils.translate(type.getLabelKey());
        int menuButtonWidth = this.getStringWidth(label) + 20;
        x = GuiUtils.getScaledWindowWidth() >= 270 ? this.width - menuButtonWidth - 10 : x + buttonWidth + 4;

        button = new GenericButton(x, y, menuButtonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        this.updateElements();
    }

    private void createCoordinateInput(int x, int y, int width, CoordinateType type)
    {
        String label = type.name() + ":";
        this.addLabel(x, y + 5, 0xFFFFFFFF, label);
        int offset = this.getStringWidth(label) + 4;

        // The sub-region placements are relative
        BlockPos pos = this.placement.getPos();
        pos = PositionUtils.getTransformedBlockPos(pos, this.schematicPlacement.getMirror(), this.schematicPlacement.getRotation());
        pos = pos.add(this.schematicPlacement.getOrigin());

        int value = PositionUtils.getCoordinate(pos, type);
        IntegerTextFieldWidget textField = new IntegerTextFieldWidget(x + offset, y + 1, width, 16, value);
        textField.setUpdateListenerAlways(true);
        this.addTextField(textField, new TextFieldListener(type, this.schematicPlacement, this.placement, this));

        String hover = StringUtils.translate("litematica.hud.schematic_placement.hover_info.lock_coordinate");
        x = x + offset + width + 20;
        CheckBoxWidget cb = new CheckBoxWidget(x, y + 3, LitematicaIcons.CHECKBOX_UNSELECTED, LitematicaIcons.CHECKBOX_SELECTED, "", hover);
        cb.setSelected(this.placement.isCoordinateLocked(type), false);
        cb.setListener(new CoordinateLockListener(type, this.placement));
        this.addWidget(cb);
    }

    private int createButtonOnOff(int x, int y, int width, boolean isCurrentlyOn, ButtonListener.Type type)
    {
        OnOffButton button = new OnOffButton(x, y, width, false, type.getTranslationKey(), isCurrentlyOn);
        this.addButton(button, new ButtonListener(type, this.schematicPlacement, this.placement, this));
        return button.getWidth();
    }

    private void createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this.schematicPlacement, this.placement, this);
        String label;

        switch (type)
        {
            case NUDGE_COORD_X:
            case NUDGE_COORD_Y:
            case NUDGE_COORD_Z:
            {
                String hover = StringUtils.translate("litematica.gui.button.hover.plus_minus_tip");
                GenericButton button = new GenericButton(x, y, LitematicaIcons.BUTTON_PLUS_MINUS_16, hover);
                this.addButton(button, listener);
                return;
            }

            case ROTATE:
                label = type.getDisplayName(PositionUtils.getRotationNameShort(this.placement.getRotation()));
                break;

            case MIRROR:
                label = type.getDisplayName(PositionUtils.getMirrorName(this.placement.getMirror()));
                break;

            case SLICE_TYPE:
                label = type.getDisplayName("todo");
                break;

            default:
                label = type.getDisplayName();
                break;
        }

        GenericButton button = new GenericButton(x, y, width, 20, label);
        this.addButton(button, listener);

        if (type == ButtonListener.Type.RESET_PLACEMENT)
        {
            this.buttonResetPlacement = button;
        }
    }

    private void updateElements()
    {
        String label = StringUtils.translate("litematica.gui.button.placement_sub.reset_sub_region_placement");
        boolean enabled = this.placement.isRegionPlacementModifiedFromDefault();

        if (enabled)
        {
            label = TXT_GOLD + label + TXT_RST;
        }

        this.buttonResetPlacement.setDisplayString(label);
        this.buttonResetPlacement.setEnabled(enabled);
    }

    private static class ButtonListener implements ButtonActionListener
    {
        private final BaseScreen parent;
        private final SchematicPlacementManager manager;
        private final SchematicPlacement schematicPlacement;
        private final SubRegionPlacement placement;
        private final Type type;
        private final String subRegionName;

        public ButtonListener(Type type, SchematicPlacement schematicPlacement, SubRegionPlacement placement, BaseScreen parent)
        {
            this.type = type;
            this.manager = DataManager.getSchematicPlacementManager();
            this.schematicPlacement = schematicPlacement;
            this.placement = placement;
            this.parent = parent;
            this.subRegionName = placement.getName();
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            int amount = mouseButton == 1 ? -1 : 1;
            if (BaseScreen.isShiftDown()) { amount *= 8; }
            if (BaseScreen.isAltDown()) { amount *= 4; }

            // The sub-region placements are relative (but the setter below uses the
            // absolute position and subtracts the placement origin internally)
            BlockPos posOld = this.placement.getPos();
            posOld = PositionUtils.getTransformedBlockPos(posOld, this.schematicPlacement.getMirror(), this.schematicPlacement.getRotation());
            posOld = posOld.add(this.schematicPlacement.getOrigin());
            this.parent.setNextMessageType(MessageOutput.ERROR);

            switch (this.type)
            {
                case PLACEMENT_CONFIGURATION:
                    BaseScreen.openScreen(new GuiPlacementConfiguration(this.schematicPlacement));
                    break;

                case ROTATE:
                {
                    boolean reverse = mouseButton == 1;
                    Rotation rotation = fi.dy.masa.malilib.util.PositionUtils.cycleRotation(this.placement.getRotation(), reverse);
                    this.manager.setSubRegionRotation(this.schematicPlacement, this.subRegionName, rotation, this.parent);
                    break;
                }

                case MIRROR:
                {
                    boolean reverse = mouseButton == 1;
                    Mirror mirror = fi.dy.masa.malilib.util.PositionUtils.cycleMirror(this.placement.getMirror(), reverse);
                    this.manager.setSubRegionMirror(this.schematicPlacement, this.subRegionName, mirror, this.parent);
                    break;
                }

                case MOVE_TO_PLAYER:
                    this.manager.moveSubRegionTo(this.schematicPlacement, this.subRegionName, new BlockPos(this.parent.mc.player.getPositionVector()), this.parent);
                    break;

                case NUDGE_COORD_X:
                    this.manager.moveSubRegionTo(this.schematicPlacement, this.subRegionName, posOld.add(amount, 0, 0), this.parent);
                    break;

                case NUDGE_COORD_Y:
                    this.manager.moveSubRegionTo(this.schematicPlacement, this.subRegionName, posOld.add(0, amount, 0), this.parent);
                    break;

                case NUDGE_COORD_Z:
                    this.manager.moveSubRegionTo(this.schematicPlacement, this.subRegionName, posOld.add(0, 0, amount), this.parent);
                    break;

                case TOGGLE_ENABLED:
                    this.manager.toggleSubRegionEnabled(this.schematicPlacement, this.subRegionName);
                    break;

                case TOGGLE_ENTITIES:
                    this.manager.toggleSubRegionIgnoreEntities(this.schematicPlacement, this.subRegionName);
                    break;

                case RESET_PLACEMENT:
                    this.manager.resetSubRegionToSchematicValues(this.schematicPlacement, this.subRegionName, this.parent);
                    break;

                case SLICE_TYPE:
                    break;
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        public enum Type
        {
            PLACEMENT_CONFIGURATION (""),
            TOGGLE_ENABLED          ("litematica.gui.button.schematic_placement.region_enabled"),
            TOGGLE_ENTITIES         ("litematica.gui.button.schematic_placement.ignore_entities"),
            MOVE_TO_PLAYER          ("litematica.gui.button.move_to_player"),
            NUDGE_COORD_X           (""),
            NUDGE_COORD_Y           (""),
            NUDGE_COORD_Z           (""),
            ROTATE                  ("litematica.gui.button.rotation_value"),
            MIRROR                  ("litematica.gui.button.mirror_value"),
            RESET_PLACEMENT         (""),
            SLICE_TYPE              ("litematica.gui.button.placement_sub.slice_type");

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

    private static class TextFieldListener implements TextChangeListener
    {
        private final GuiSubRegionConfiguration parent;
        private final SchematicPlacementManager manager;
        private final SchematicPlacement schematicPlacement;
        private final SubRegionPlacement placement;
        private final CoordinateType type;

        public TextFieldListener(CoordinateType type, SchematicPlacement schematicPlacement, SubRegionPlacement placement, GuiSubRegionConfiguration parent)
        {
            this.manager = DataManager.getSchematicPlacementManager();
            this.schematicPlacement = schematicPlacement;
            this.placement = placement;
            this.type = type;
            this.parent = parent;
        }

        @Override
        public void onTextChange(String newText)
        {
            try
            {
                int value = Integer.parseInt(newText);
                // The sub-region placements are relative (but the setter below uses the
                // absolute position and subtracts the placement origin internally)
                BlockPos posOld = this.placement.getPos();
                posOld = PositionUtils.getTransformedBlockPos(posOld, this.schematicPlacement.getMirror(), this.schematicPlacement.getRotation());
                posOld = posOld.add(this.schematicPlacement.getOrigin());
                BlockPos pos = posOld;

                switch (this.type)
                {
                    case X: pos = new BlockPos(value        , posOld.getY(), posOld.getZ()); break;
                    case Y: pos = new BlockPos(posOld.getX(), value        , posOld.getZ()); break;
                    case Z: pos = new BlockPos(posOld.getX(), posOld.getY(), value        ); break;
                }

                this.parent.setNextMessageType(MessageOutput.ERROR);
                this.manager.moveSubRegionTo(this.schematicPlacement, this.placement.getName(), pos, this.parent);
                this.parent.updateElements();
            }
            catch (NumberFormatException e) {}
        }
    }

    private static class CoordinateLockListener implements SelectionListener<CheckBoxWidget>
    {
        private final SubRegionPlacement placement;
        private final CoordinateType type;

        private CoordinateLockListener(CoordinateType type, SubRegionPlacement placement)
        {
            this.type = type;
            this.placement = placement;
        }

        @Override
        public void onSelectionChange(CheckBoxWidget entry)
        {
            this.placement.setCoordinateLocked(this.type, entry.isSelected());
        }
    }
}
