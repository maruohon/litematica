package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetListPlacementSubRegions;
import fi.dy.masa.litematica.gui.widgets.WidgetPlacementSubRegion;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

public class GuiPlacementConfiguration  extends GuiListBase<SubRegionPlacement, WidgetPlacementSubRegion, WidgetListPlacementSubRegions>
                                        implements ISelectionListener<SubRegionPlacement>
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
        return this.width - 150;
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
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int width = Math.min(300, sr.getScaledWidth() - 200);
        int x = 10;
        int y = 28;

        this.textFieldRename = new GuiTextFieldGeneric(this.id++, this.mc.fontRenderer, x, y + 2, width, 16);
        this.textFieldRename.setMaxStringLength(256);
        this.textFieldRename.setText(this.placement.getName());
        this.addTextField(this.textFieldRename, null);
        this.createButton(x + width + 4, y, -1, ButtonListener.Type.RENAME_PLACEMENT);

        String label = I18n.format("litematica.gui.label.schematic_placement.sub_regions");
        this.addLabel(x, y + 20, -1, 20, 0xFFFFFFFF, label);

        width = 120;
        x = this.width - width - 10;

        this.createButton(x, y, width - 18, ButtonListener.Type.TOGGLE_ENABLED);
        this.createButton(x + width - 16, y + 2, 16, ButtonListener.Type.TOGGLE_ENCLOSING_BOX);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.TOGGLE_LOCKED);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.TOGGLE_ENTITIES);
        y += 22;
        x += 2;

        label = I18n.format("litematica.gui.label.placement_settings.placement_origin");
        this.addLabel(x, y, width, 20, 0xFFFFFFFF, label);
        y += 20;

        this.createCoordinateInput(x, y, 70, CoordinateType.X);
        this.addButton(new ButtonGeneric(this.id++, x + 85, y + 1, Icons.BUTTON_PLUS_MINUS_16), new ButtonListener(ButtonListener.Type.NUDGE_COORD_X, this.placement, this));
        y += 20;

        this.createCoordinateInput(x, y, 70, CoordinateType.Y);
        this.addButton(new ButtonGeneric(this.id++, x + 85, y + 1, Icons.BUTTON_PLUS_MINUS_16), new ButtonListener(ButtonListener.Type.NUDGE_COORD_Y, this.placement, this));
        y += 20;

        this.createCoordinateInput(x, y, 70, CoordinateType.Z);
        this.addButton(new ButtonGeneric(this.id++, x + 85, y + 1, Icons.BUTTON_PLUS_MINUS_16), new ButtonListener(ButtonListener.Type.NUDGE_COORD_Z, this.placement, this));
        y += 22;
        x -= 2;

        this.createButton(x, y, width, ButtonListener.Type.MOVE_HERE);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.ROTATE);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.MIRROR);
        y += 22;

        this.createButton(x, y, width, ButtonListener.Type.RESET_SUB_REGIONS);

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        int buttonWidth = this.fontRenderer.getStringWidth(label) + 20;

        // Move these buttons to the bottom (left) of the screen, if the height isn't enough for them
        // to fit below the other buttons
        if (sr.getScaledHeight() < 324)
        {
            x = 10;
            y = this.height - 32;

            x += this.createButton(x, y, -1, ButtonListener.Type.OPEN_MATERIAL_LIST_GUI) + 2;
            x += this.createButton(x, y, -1, ButtonListener.Type.OPEN_VERIFIER_GUI) + 2;

            ButtonGeneric button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
            this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
        }
        else
        {
            y = this.height - 3 * 24 - 12;
            this.createButton(x, y, width, ButtonListener.Type.OPEN_MATERIAL_LIST_GUI);
            y += 22;

            this.createButton(x, y, width, ButtonListener.Type.OPEN_VERIFIER_GUI);
            y += 26;
            x = this.width - buttonWidth - 10;

            ButtonGeneric button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
            this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
        }

        this.updateElements();
    }

    private void createCoordinateInput(int x, int y, int width, CoordinateType type)
    {
        String label = type.name() + ":";
        this.addLabel(x, y, width, 20, 0xFFFFFFFF, label);
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

    private int createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this.placement, this);
        String label = "";
        String hover = null;

        if (type == ButtonListener.Type.TOGGLE_ENCLOSING_BOX)
        {
            Icons icon = this.placement.shouldRenderEnclosingBox() ? Icons.ENCLOSING_BOX_ENABLED : Icons.ENCLOSING_BOX_DISABLED;
            boolean enabled = this.placement.shouldRenderEnclosingBox();
            String str = (enabled ? TXT_GREEN : TXT_RED) + I18n.format("litematica.message.value." + (enabled ? "on" : "off")) + TXT_RST;
            hover = I18n.format("litematica.gui.button.schematic_placement.hover.enclosing_box", str);

            this.addButton(new ButtonGeneric(this.id++, x, y, icon, hover), listener);

            return icon.getWidth();
        }

        switch (type)
        {
            case TOGGLE_ENABLED:
            {
                if (this.placement.isEnabled())
                    label = I18n.format("litematica.gui.button.disable");
                else
                    label = I18n.format("litematica.gui.button.enable");
                break;
            }

            case TOGGLE_LOCKED:
            {
                if (this.placement.isLocked())
                    label = TextFormatting.GOLD + I18n.format("litematica.gui.button.locked");
                else
                    label = I18n.format("litematica.gui.button.unlocked");

                hover = I18n.format("litematica.gui.button.schematic_placement.hover.lock");
                break;
            }

            case TOGGLE_ENTITIES:
            {
                boolean enabled = this.placement.ignoreEntities();
                String str = (enabled ? TXT_GREEN : TXT_RED) + I18n.format("litematica.message.value." + (enabled ? "on" : "off")) + TXT_RST;
                label = I18n.format("litematica.gui.button.schematic_placement.ignore_entities", str);
                break;
            }

            case ROTATE:
            {
                String value = PositionUtils.getRotationNameShort(this.placement.getRotation());
                label = type.getDisplayName(value);
                break;
            }

            case MIRROR:
            {
                String value = PositionUtils.getMirrorName(this.placement.getMirror());
                label = type.getDisplayName(value);
                break;
            }

            default:
                label = type.getDisplayName();
        }

        if (width == -1)
        {
            width = this.fontRenderer.getStringWidth(label) + 10;
        }

        ButtonGeneric button;

        if (hover != null)
        {
            button = new ButtonGeneric(this.id++, x, y, width, 20, label, hover);
        }
        else
        {
            button = new ButtonGeneric(this.id++, x, y, width, 20, label);
        }

        this.addButton(button, listener);

        if (type == ButtonListener.Type.RESET_SUB_REGIONS)
        {
            this.buttonResetPlacement = button;
        }

        return width;
    }

    private void updateElements()
    {
        String label = I18n.format("litematica.gui.button.schematic_placement.reset_sub_region_placements");;
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
    protected ISelectionListener<SubRegionPlacement> getSelectionListener()
    {
        return this;
    }

    @Override
    public void onSelectionChange(SubRegionPlacement entry)
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
            int amount = mouseButton == 1 ? -1 : 1;
            if (GuiScreen.isShiftKeyDown()) { amount *= 8; }
            if (GuiScreen.isAltKeyDown()) { amount *= 4; }
            BlockPos oldOrigin = this.placement.getOrigin();
            this.parent.setNextMessageType(MessageType.ERROR);

            switch (this.type)
            {
                case RENAME_PLACEMENT:
                    this.placement.setName(this.parent.textFieldRename.getText());
                    break;

                case ROTATE:
                {
                    boolean reverse = mouseButton == 1;
                    Rotation rotation = PositionUtils.cycleRotation(this.placement.getRotation(), reverse);
                    this.placement.setRotation(rotation, this.parent);
                    break;
                }

                case MIRROR:
                {
                    boolean reverse = mouseButton == 1;
                    Mirror mirror = PositionUtils.cycleMirror(this.placement.getMirror(), reverse);
                    this.placement.setMirror(mirror, this.parent);
                    break;
                }

                case MOVE_HERE:
                {
                    BlockPos pos = new BlockPos(mc.player.getPositionVector());
                    this.placement.setOrigin(pos, this.parent);
                    break;
                }

                case NUDGE_COORD_X:
                    this.placement.setOrigin(oldOrigin.add(amount, 0, 0), this.parent);
                    break;

                case NUDGE_COORD_Y:
                    this.placement.setOrigin(oldOrigin.add(0, amount, 0), this.parent);
                    break;

                case NUDGE_COORD_Z:
                    this.placement.setOrigin(oldOrigin.add(0, 0, amount), this.parent);
                    break;

                case TOGGLE_ENABLED:
                    this.placement.toggleEnabled();
                    break;

                case TOGGLE_LOCKED:
                    this.placement.toggleLocked();
                    break;

                case TOGGLE_ENTITIES:
                    this.placement.toggleIgnoreEntities(this.parent);
                    break;

                case TOGGLE_ENCLOSING_BOX:
                    this.placement.toggleRenderEnclosingBox();
                    break;

                case RESET_SUB_REGIONS:
                    this.placement.resetAllSubRegionsToSchematicValues(this.parent);
                    break;

                case OPEN_VERIFIER_GUI:
                {
                    GuiSchematicVerifier gui = new GuiSchematicVerifier(this.placement);
                    gui.setParent(this.parent);
                    mc.displayGuiScreen(gui);
                    break;
                }

                case OPEN_MATERIAL_LIST_GUI:
                {
                    GuiMaterialList gui = new GuiMaterialList(this.placement);
                    gui.setParent(this.parent);
                    mc.displayGuiScreen(gui);
                    break;
                }
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        public enum Type
        {
            RENAME_PLACEMENT        ("litematica.gui.button.rename"),
            ROTATE                  ("litematica.gui.button.rotation_value"),
            MIRROR                  ("litematica.gui.button.mirror_value"),
            MOVE_HERE               ("litematica.gui.button.move_here"),
            NUDGE_COORD_X           (""),
            NUDGE_COORD_Y           (""),
            NUDGE_COORD_Z           (""),
            TOGGLE_ENABLED          (""),
            TOGGLE_LOCKED           (""),
            TOGGLE_ENTITIES         (""),
            TOGGLE_ENCLOSING_BOX    (""),
            RESET_SUB_REGIONS       (""),
            OPEN_VERIFIER_GUI       ("litematica.gui.button.schematic_verifier"),
            OPEN_MATERIAL_LIST_GUI  ("litematica.gui.button.material_list");

            private final String translationKey;

            private Type(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getDisplayName(Object... args)
            {
                return I18n.format(this.translationKey, args);
            }
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
                this.parent.setNextMessageType(MessageType.ERROR);

                switch (this.type)
                {
                    case X: this.placement.setOrigin(new BlockPos(value, posOld.getY(), posOld.getZ()), this.parent); break;
                    case Y: this.placement.setOrigin(new BlockPos(posOld.getX(), value, posOld.getZ()), this.parent); break;
                    case Z: this.placement.setOrigin(new BlockPos(posOld.getX(), posOld.getY(), value), this.parent); break;
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
