package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.base.ButtonEntry;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.gui.button.ButtonBase;
import fi.dy.masa.litematica.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.gui.widgets.base.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

public class WidgetSchematicPlacement extends WidgetBase
{
    private final WidgetSchematicPlacements parent;
    private final SchematicPlacement placement;
    private final Minecraft mc;
    private final List<ButtonEntry<?>> buttons = new ArrayList<>();
    private int id;

    public WidgetSchematicPlacement(int x, int y, int width, int height, float zLevel,
            SchematicPlacement placement, WidgetSchematicPlacements parent, Minecraft mc)
    {
        super(x, y, width, height, zLevel);

        this.parent = parent;
        this.placement = placement;
        this.mc = mc;

        this.id = 0;
        int posX = x + width;
        int posY = y + 1;

        // Note: These are placed from right to left

        posX = this.createButton(posX, posY, ButtonListener.ButtonType.REMOVE);

        String labelEn = I18n.format("litematica.button.schematic_placements.render_enable");
        String labelDis = I18n.format("litematica.button.schematic_placements.render_disable");
        String label = this.placement.getRenderSchematic() ? labelDis : labelEn;
        int len = Math.max(mc.fontRenderer.getStringWidth(labelEn), mc.fontRenderer.getStringWidth(labelEn)) + 10;
        posX -= (len + 4);
        ButtonListener listener = new ButtonListener(ButtonListener.ButtonType.TOGGLE_RENDER, this);
        this.addButton(new ButtonGeneric(this.id++, posX, posY, len, 20, label), listener);

        labelEn = I18n.format("litematica.button.schematic_placements.enable");
        labelDis = I18n.format("litematica.button.schematic_placements.disable");
        label = this.placement.isEnabled() ? labelDis : labelEn;
        len = Math.max(mc.fontRenderer.getStringWidth(labelEn), mc.fontRenderer.getStringWidth(labelEn)) + 10;
        posX -= (len + 4);
        listener = new ButtonListener(ButtonListener.ButtonType.TOGGLE_ENABLED, this);
        this.addButton(new ButtonGeneric(this.id++, posX, posY, len, 20, label), listener);

        posX = this.createButton(posX, posY, ButtonListener.ButtonType.SELECT);
        posX = this.createButton(posX, posY, ButtonListener.ButtonType.CONFIGURE);
    }

    private int createButton(int x, int y, ButtonListener.ButtonType type)
    {
        String label = I18n.format(type.getLabelKey());
        int len = mc.fontRenderer.getStringWidth(label) + 10;
        x -= (len + 4);
        this.addButton(new ButtonGeneric(this.id++, x, y, len, 20, label), new ButtonListener(type, this));

        return x;
    }

    private <T extends ButtonBase> void addButton(T button, IButtonActionListener<T> listener)
    {
        this.buttons.add(new ButtonEntry<>(button, listener));
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        for (ButtonEntry<?> entry : this.buttons)
        {
            if (entry.mousePressed(this.mc, mouseX, mouseY, mouseButton))
            {
                // Don't call super if the button press got handled
                return true;
            }
        }

        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        Minecraft mc = Minecraft.getMinecraft();
        int dimension = mc.world.provider.getDimensionType().getId();

        if (DataManager.getInstance(dimension).getSchematicPlacementManager().getSelectedSchematicPlacement() == this.placement)
        {
            GuiLitematicaBase.drawOutline(this.x, this.y, this.width, this.height, 0xD0FFFFFF);
        }

        String name = this.placement.getName();
        String pre = this.placement.isEnabled() ? TextFormatting.GREEN.toString() : TextFormatting.RED.toString();
        this.mc.fontRenderer.drawString(pre + name, this.x + 4, this.y + 7, 0xFFFFFFFF);

        for (int i = 0; i < this.buttons.size(); ++i)
        {
            this.buttons.get(i).draw(this.mc, mouseX, mouseY, 0);
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final ButtonType type;
        private final WidgetSchematicPlacement widget;

        public ButtonListener(ButtonType type, WidgetSchematicPlacement widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            Minecraft mc = Minecraft.getMinecraft();
            int dimension = mc.world.provider.getDimensionType().getId();

            if (this.type == ButtonType.CONFIGURE)
            {
                GuiPlacementConfiguration gui = new GuiPlacementConfiguration(this.widget.placement);
                gui.setParent(this.widget.parent);
                mc.displayGuiScreen(gui);
            }
            else if (this.type == ButtonType.SELECT)
            {
                DataManager.getInstance(dimension).getSchematicPlacementManager().setSelectedSchematicPlacement(this.widget.placement);
            }
            else if (this.type == ButtonType.REMOVE)
            {
                DataManager.getInstance(dimension).getSchematicPlacementManager().removeSchematicPlacement(this.widget.placement);
                this.widget.parent.refreshEntries();
            }
            else if (this.type == ButtonType.TOGGLE_ENABLED)
            {
                this.widget.placement.setEnabled(! this.widget.placement.isEnabled());
                this.widget.parent.refreshEntries();
            }
            else if (this.type == ButtonType.TOGGLE_RENDER)
            {
                this.widget.placement.setRenderSchematic(! this.widget.placement.getRenderSchematic());
                this.widget.parent.refreshEntries();
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum ButtonType
        {
            CONFIGURE       ("litematica.button.schematic_placements.configure"),
            SELECT          ("litematica.button.schematic_placements.select"),
            REMOVE          ("litematica.button.schematic_placements.remove"),
            TOGGLE_ENABLED  (""),
            TOGGLE_RENDER   ("");

            private final String labelKey;

            private ButtonType(String labelKey)
            {
                this.labelKey = labelKey;
            }

            public String getLabelKey()
            {
                return this.labelKey;
            }
        }
    }
}
