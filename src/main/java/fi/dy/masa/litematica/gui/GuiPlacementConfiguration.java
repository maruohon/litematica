package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetListPlacementSubRegions;
import fi.dy.masa.litematica.gui.widgets.WidgetPlacementSubRegion;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class GuiPlacementConfiguration  extends GuiListBase<Placement, WidgetPlacementSubRegion, WidgetListPlacementSubRegions>
                                        implements ISelectionListener<Placement>
{
    private final SchematicPlacement placement;
    private ButtonGeneric buttonResetPlacement;
    private GuiTextField textFieldRename;
    private int id;

    public GuiPlacementConfiguration(SchematicPlacement placement)
    {
        super(10, 60);
        this.placement = placement;
        this.title = I18n.format("litematica.gui.title.configure_schematic_placement");
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 160;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 94;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.id = 0;
        int width = 300;
        int x = 10;
        int y = 28;

        this.textFieldRename = new GuiTextFieldGeneric(this.id++, this.mc.fontRenderer, x, y + 2, width, 16);
        this.textFieldRename.setMaxStringLength(256);
        this.textFieldRename.setText(this.placement.getName());
        this.addTextField(this.textFieldRename, null);
        this.createButton(x + width + 4, y, -1, ButtonListener.Type.RENAME_PLACEMENT);

        y += 20;
        String label = I18n.format("litematica.gui.label.schematic_placement.sub_regions");
        this.addLabel(this.id++, x, y, -1, 20, 0xFFFFFFFF, label);

        width = 120;
        x = this.width - width - 10;
        this.createButton(x, y, width, ButtonListener.Type.TOGGLE_ENABLED);
        y += 32;

        label = I18n.format("litematica.gui.label.placement_settings.placement_origin");
        this.addLabel(this.id++, x, y, width, 20, 0xFFFFFFFF, label);
        y += 20;

        this.createCoordinateInput(x, y, 70, CoordinateType.X);
        y += 20;

        this.createCoordinateInput(x, y, 70, CoordinateType.Y);
        y += 20;

        this.createCoordinateInput(x, y, 70, CoordinateType.Z);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.MOVE_HERE);
        y += 44;

        this.createButton(x, y, width, ButtonListener.Type.ROTATE);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.MIRROR);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.RESET_SUB_REGIONS);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.OPEN_VERIFIER_GUI);

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        int buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        y = this.height - 36;
        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        this.updateElements();
    }

    private void createCoordinateInput(int x, int y, int width, CoordinateType type)
    {
        String label = type.name() + ":";
        this.addLabel(this.id++, x, y, width, 20, 0xFFFFFFFF, label);
        int offset = this.mc.fontRenderer.getStringWidth(label) + 4;

        BlockPos pos = this.placement.getOrigin();
        String text = "";

        switch (type)
        {
            case X: text = String.valueOf(pos.getX()); break;
            case Y: text = String.valueOf(pos.getY()); break;
            case Z: text = String.valueOf(pos.getZ()); break;
        }

        GuiTextFieldInteger textField = new GuiTextFieldInteger(this.id++, x + offset, y + 1, width, 16, this.mc.fontRenderer);
        textField.setText(text);
        TextFieldListener listener = new TextFieldListener(type, this.placement, this);
        this.addTextField(textField, listener);
    }

    private void createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this.placement, this);
        String label = "";

        switch (type)
        {
            case RENAME_PLACEMENT:
                label = I18n.format("litematica.gui.button.rename");
                break;

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

            case RESET_SUB_REGIONS:
                break;

            case OPEN_VERIFIER_GUI:
                label = I18n.format("litematica.gui.button.schematic_verifier");
                break;
        }

        if (width == -1)
        {
            width = this.fontRenderer.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, width, 20, label);
        this.addButton(button, listener);

        if (type == ButtonListener.Type.RESET_SUB_REGIONS)
        {
            this.buttonResetPlacement = button;
        }
    }

    private void updateElements()
    {
        String label = I18n.format("litematica.gui.schematic_placement.button.reset_sub_region_placements");;
        boolean enabled = true;

        if (this.placement.isRegionPlacementModified())
        {
            label = TXT_GOLD + label + TXT_RST;
        }
        else
        {
            enabled = false;
        }

        this.buttonResetPlacement.displayString = label;
        this.buttonResetPlacement.enabled = enabled;
    }

    public SchematicPlacement getSchematicPlacement()
    {
        return this.placement;
    }

    @Override
    protected ISelectionListener<Placement> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(Placement entry)
    {
        this.placement.setSelectedSubRegionName(entry != null && entry.getName().equals(this.placement.getSelectedSubRegionName()) == false ? entry.getName() : null);
    }

    @Override
    protected WidgetListPlacementSubRegions createListWidget(int listX, int listY)
    {
        return new WidgetListPlacementSubRegions(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiPlacementConfiguration parent;
        private final SchematicPlacement placement;
        private final Type type;

        public ButtonListener(Type type, SchematicPlacement placement, GuiPlacementConfiguration parent)
        {
            this.parent = parent;
            this.placement = placement;
            this.type = type;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            Minecraft mc = Minecraft.getMinecraft();

            switch (this.type)
            {
                case RENAME_PLACEMENT:
                    this.placement.setName(this.parent.textFieldRename.getText());
                    break;

                case ROTATE:
                {
                    boolean reverse = mouseButton == 1;
                    this.placement.setRotation(PositionUtils.cycleRotation(this.placement.getRotation(), reverse));
                    break;
                }

                case MIRROR:
                {
                    boolean reverse = mouseButton == 1;
                    this.placement.setMirror(PositionUtils.cycleMirror(this.placement.getMirror(), reverse));
                    break;
                }

                case MOVE_HERE:
                    BlockPos pos = new BlockPos(mc.player.getPositionVector());
                    this.placement.setOrigin(pos);
                    break;

                case TOGGLE_ENABLED:
                    this.placement.toggleEnabled();
                    break;

                case RESET_SUB_REGIONS:
                    this.placement.resetAllSubRegionsToSchematicValues();
                    break;

                case OPEN_VERIFIER_GUI:
                    GuiSchematicVerifier gui = new GuiSchematicVerifier(this.placement);
                    gui.setParent(this.parent);
                    mc.displayGuiScreen(gui);
                    break;
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        public enum Type
        {
            RENAME_PLACEMENT,
            ROTATE,
            MIRROR,
            MOVE_HERE,
            TOGGLE_ENABLED,
            RESET_SUB_REGIONS,
            OPEN_VERIFIER_GUI;
        }
    }

    private static class TextFieldListener implements ITextFieldListener<GuiTextField>
    {
        private final GuiPlacementConfiguration parent;
        private final SchematicPlacement placement;
        private final CoordinateType type;

        public TextFieldListener(CoordinateType type, SchematicPlacement placement, GuiPlacementConfiguration parent)
        {
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
                BlockPos posOld = this.placement.getOrigin();

                switch (this.type)
                {
                    case X: this.placement.setOrigin(new BlockPos(value, posOld.getY(), posOld.getZ())); break;
                    case Y: this.placement.setOrigin(new BlockPos(posOld.getX(), value, posOld.getZ())); break;
                    case Z: this.placement.setOrigin(new BlockPos(posOld.getX(), posOld.getY(), value)); break;
                }

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
