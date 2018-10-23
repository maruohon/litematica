package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonIcon;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class GuiSubRegionConfiguration extends GuiBase
{
    private final SchematicPlacement schematicPlacement;
    private final SubRegionPlacement placement;
    private ButtonGeneric buttonResetPlacement;
    private int id;

    public GuiSubRegionConfiguration(SchematicPlacement schematicPlacement, SubRegionPlacement placement)
    {
        this.schematicPlacement = schematicPlacement;
        this.placement = placement;
        this.title = I18n.format("litematica.gui.title.configure_schematic_sub_region");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.id = 0;
        int width = 140;
        int x = this.width - width - 10;
        int y = 22;

        String label = I18n.format("litematica.gui.placement_sub_region.label.region_name", this.placement.getName());
        this.addLabel(20, y, -1, 16, 0xFFFFFFFF, label);

        this.createButton(x, y, width, ButtonListener.Type.TOGGLE_ENABLED);
        y += 22;

        label = I18n.format("litematica.gui.placement_sub_region.label.region_position");
        this.addLabel(x, y, width, 20, 0xFFFFFFFF, label);
        y += 20;
        x += 2;

        this.createCoordinateInput(x, y, 70, CoordinateType.X);
        this.addButton(new ButtonIcon(this.id++, x + 85, y + 1, 16, 16, Icons.BUTTON_PLUS_MINUS_16), new ButtonListener<ButtonIcon>(ButtonListener.Type.NUDGE_COORD_X, this.schematicPlacement, this.placement, this));
        y += 20;

        this.createCoordinateInput(x, y, 70, CoordinateType.Y);
        this.addButton(new ButtonIcon(this.id++, x + 85, y + 1, 16, 16, Icons.BUTTON_PLUS_MINUS_16), new ButtonListener<ButtonIcon>(ButtonListener.Type.NUDGE_COORD_Y, this.schematicPlacement, this.placement, this));
        y += 20;

        this.createCoordinateInput(x, y, 70, CoordinateType.Z);
        this.addButton(new ButtonIcon(this.id++, x + 85, y + 1, 16, 16, Icons.BUTTON_PLUS_MINUS_16), new ButtonListener<ButtonIcon>(ButtonListener.Type.NUDGE_COORD_Z, this.schematicPlacement, this.placement, this));
        y += 22;
        x -= 2;

        this.createButton(x, y, width, ButtonListener.Type.MOVE_HERE);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.ROTATE);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.MIRROR);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.RESET_PLACEMENT);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.SLICE_TYPE);

        y = this.height - 36;
        label = I18n.format("litematica.gui.placement_sub_region.button.placement_configuration");
        int buttonWidth = this.fontRenderer.getStringWidth(label) + 10;
        x = 10;
        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListener<>(ButtonListener.Type.PLACEMENT_CONFIGURATION, this.schematicPlacement, this.placement, this));

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        int menuButtonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = sr.getScaledHeight() >= 270 ? this.width - menuButtonWidth - 10 : x + buttonWidth + 4;

        button = new ButtonGeneric(this.id++, x, y, menuButtonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        this.updateElements();
    }

    private void createCoordinateInput(int x, int y, int width, CoordinateType type)
    {
        String label = type.name() + ":";
        this.addLabel(x, y, width, 20, 0xFFFFFFFF, label);
        int offset = this.mc.fontRenderer.getStringWidth(label) + 4;

        // The sub-region placements are relative
        BlockPos pos = this.placement.getPos().add(this.schematicPlacement.getOrigin());
        String text = "";

        switch (type)
        {
            case X: text = String.valueOf(pos.getX()); break;
            case Y: text = String.valueOf(pos.getY()); break;
            case Z: text = String.valueOf(pos.getZ()); break;
        }

        GuiTextFieldInteger textField = new GuiTextFieldInteger(this.id++, x + offset, y + 1, width, 16, this.mc.fontRenderer);
        textField.setText(text);
        TextFieldListener listener = new TextFieldListener(type, this.schematicPlacement, this.placement, this);
        this.addTextField(textField, listener);
    }

    private void createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener<ButtonGeneric> listener = new ButtonListener<>(type, this.schematicPlacement, this.placement, this);
        String label = "";

        switch (type)
        {
            case ROTATE:
            {
                String value = PositionUtils.getRotationNameShort(this.placement.getRotation());
                label = I18n.format("litematica.gui.button.rotation_value", value);
                break;
            }

            case MIRROR:
            {
                String value = PositionUtils.getMirrorName(this.placement.getMirror());
                label = I18n.format("litematica.gui.button.mirror_value", value);
                break;
            }

            case MOVE_HERE:
                label = I18n.format("litematica.gui.button.move_here");
                break;

            case TOGGLE_ENABLED:
                if (this.placement.isEnabled())
                    label = I18n.format("litematica.gui.button.disable");
                else
                    label = I18n.format("litematica.gui.button.enable");
                break;

            case RESET_PLACEMENT:
                break;

            case SLICE_TYPE:
            {
                String value = "todo";
                label = I18n.format("litematica.gui.placement_sub_region.button.slice_type", value);
                break;
            }

            default:
        }

        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, width, 20, label);
        this.addButton(button, listener);

        if (type == ButtonListener.Type.RESET_PLACEMENT)
        {
            this.buttonResetPlacement = button;
        }
    }

    private void updateElements()
    {
        String areaName = this.placement.getName();
        BlockPos posOriginal = this.schematicPlacement.getSchematic().getSubRegionPosition(areaName);
        String label = I18n.format("litematica.gui.placement_sub_region.button.reset_sub_region_placement");
        boolean enabled = this.placement.isRegionPlacementModified(posOriginal);

        if (enabled)
        {
            label = TXT_GOLD + label + TXT_RST;
        }

        this.buttonResetPlacement.displayString = label;
        this.buttonResetPlacement.enabled = enabled;
    }

    private static class ButtonListener<T extends ButtonBase> implements IButtonActionListener<T>
    {
        private final GuiBase parent;
        private final SchematicPlacement schematicPlacement;
        private final SubRegionPlacement placement;
        private final Type type;
        private final String subRegionName;

        public ButtonListener(Type type, SchematicPlacement schematicPlacement, SubRegionPlacement placement, GuiBase parent)
        {
            this.type = type;
            this.schematicPlacement = schematicPlacement;
            this.placement = placement;
            this.parent = parent;
            this.subRegionName = placement.getName();
        }

        @Override
        public void actionPerformed(T control)
        {
        }

        @Override
        public void actionPerformedWithButton(T control, int mouseButton)
        {
            Minecraft mc = Minecraft.getMinecraft();
            int amount = mouseButton == 1 ? -1 : 1;
            if (GuiScreen.isShiftKeyDown()) { amount *= 8; }
            if (GuiScreen.isAltKeyDown()) { amount *= 4; }

            // The sub-region placements are relative (but the setter below uses the
            // absolute position and subtracts the placement origin internally)
            BlockPos posOld = this.placement.getPos();
            posOld = PositionUtils.getTransformedBlockPos(posOld, this.schematicPlacement.getMirror(), this.schematicPlacement.getRotation());
            posOld = posOld.add(this.schematicPlacement.getOrigin());

            switch (this.type)
            {
                case PLACEMENT_CONFIGURATION:
                    mc.displayGuiScreen(new GuiPlacementConfiguration(this.schematicPlacement));
                    break;

                case ROTATE:
                {
                    boolean reverse = mouseButton == 1;
                    this.schematicPlacement.setSubRegionRotation(this.subRegionName, PositionUtils.cycleRotation(this.placement.getRotation(), reverse));
                    break;
                }

                case MIRROR:
                {
                    boolean reverse = mouseButton == 1;
                    this.schematicPlacement.setSubRegionMirror(this.subRegionName, PositionUtils.cycleMirror(this.placement.getMirror(), reverse));
                    break;
                }

                case MOVE_HERE:
                    this.schematicPlacement.moveSubRegionTo(this.subRegionName, new BlockPos(mc.player.getPositionVector()));
                    break;

                case NUDGE_COORD_X:
                    this.schematicPlacement.moveSubRegionTo(this.subRegionName, posOld.add(amount, 0, 0));
                    break;

                case NUDGE_COORD_Y:
                    this.schematicPlacement.moveSubRegionTo(this.subRegionName, posOld.add(0, amount, 0));
                    break;

                case NUDGE_COORD_Z:
                    this.schematicPlacement.moveSubRegionTo(this.subRegionName, posOld.add(0, 0, amount));
                    break;

                case TOGGLE_ENABLED:
                    this.schematicPlacement.toggleSubRegionEnabled(this.subRegionName);
                    break;

                case RESET_PLACEMENT:
                    this.schematicPlacement.resetSubRegionToSchematicValues(this.subRegionName);
                    break;

                case SLICE_TYPE:
                    break;
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        public enum Type
        {
            PLACEMENT_CONFIGURATION,
            TOGGLE_ENABLED,
            MOVE_HERE,
            NUDGE_COORD_X,
            NUDGE_COORD_Y,
            NUDGE_COORD_Z,
            ROTATE,
            MIRROR,
            RESET_PLACEMENT,
            SLICE_TYPE;
        }
    }

    private static class TextFieldListener implements ITextFieldListener<GuiTextField>
    {
        private final GuiSubRegionConfiguration parent;
        private final SchematicPlacement schematicPlacement;
        private final SubRegionPlacement placement;
        private final CoordinateType type;

        public TextFieldListener(CoordinateType type, SchematicPlacement schematicPlacement, SubRegionPlacement placement, GuiSubRegionConfiguration parent)
        {
            this.schematicPlacement = schematicPlacement;
            this.placement = placement;
            this.type = type;
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
            try
            {
                int value = Integer.parseInt(textField.getText());
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

                this.schematicPlacement.moveSubRegionTo(this.placement.getName(), pos);
                this.parent.updateElements();
            }
            catch (NumberFormatException e)
            {
            }

            return false;
        }
    }

    public enum CoordinateType
    {
        X,
        Y,
        Z
    }
}
